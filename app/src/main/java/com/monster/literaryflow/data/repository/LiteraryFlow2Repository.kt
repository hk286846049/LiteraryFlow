package com.monster.literaryflow.data.repository

import android.content.Context
import com.google.gson.Gson
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.AppProfileModel
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.ConditionSpec
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.core.result.FlowResult
import com.monster.literaryflow.data.db2.ActionEntity2
import com.monster.literaryflow.data.db2.AppProfileEntity2
import com.monster.literaryflow.data.db2.ConditionEntity2
import com.monster.literaryflow.data.db2.LiteraryFlow2Database
import com.monster.literaryflow.data.db2.StepEntity2
import com.monster.literaryflow.data.db2.TaskEntity2
import com.monster.literaryflow.data.importexport.AutomationImportExport
import com.monster.literaryflow.data.importexport.ImportReport
import com.monster.literaryflow.data.importexport.LegacyAutoInfoMapper
import com.monster.literaryflow.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiteraryFlow2Repository(private val context: Context) {
    private val gson = Gson()
    private val dao by lazy { LiteraryFlow2Database.getDatabase(context).dao() }
    private val legacyDao by lazy { AppDatabase.getDatabase(context).autoInfoDao() }

    suspend fun loadDocument(): AutomationDocument = withContext(Dispatchers.IO) {
        val existing = dao.getTasks()
        if (existing.isNotEmpty()) {
            readDocumentFromNewDatabase()
        } else {
            val legacyTasks = legacyDao.getAll()
            val document = LegacyAutoInfoMapper.convertAll(legacyTasks)
            if (document.tasks.isNotEmpty()) {
                replaceDocument(document)
            }
            document
        }
    }

    suspend fun importJson(json: String): FlowResult<ImportReport> = withContext(Dispatchers.IO) {
        when (val result = AutomationImportExport.importJson(json)) {
            is FlowResult.Success -> {
                replaceDocument(result.value.document)
                result
            }
            is FlowResult.Failure -> result
        }
    }

    suspend fun replaceDocument(document: AutomationDocument) = withContext(Dispatchers.IO) {
        val tasks = document.tasks.map { it.toEntity() }
        val steps = document.tasks.flatMap { task -> task.steps.map { it.toEntity() } }
        val actions = document.tasks.flatMap { task ->
            task.steps.flatMap { step ->
                step.actions.mapIndexed { index, action -> action.toEntity(step.id, step.id * 100L + index + 1L) }
            }
        }
        val conditions = document.tasks.flatMap { task ->
            task.steps.flatMap { step ->
                step.conditions.mapIndexed { index, condition ->
                    condition.toEntity(step.id, step.id * 100L + index + 51L)
                }
            }
        }
        val appProfiles = document.appProfiles.map { it.toEntity() }
        dao.replaceImportedData(tasks, steps, actions, conditions, appProfiles)
    }

    private fun readDocumentFromNewDatabase(): AutomationDocument {
        val tasks = dao.getTasks().map { taskEntity ->
            val steps = dao.getSteps(taskEntity.id).map { stepEntity ->
                stepEntity.toModel(
                    actions = dao.getActions(stepEntity.id).map { it.toModel() },
                    conditions = dao.getConditions(stepEntity.id).map { it.toModel() }
                )
            }
            taskEntity.toModel(steps)
        }
        val appProfiles = dao.getAppProfiles().map {
            AppProfileModel(
                id = it.id,
                packageName = it.packageName,
                appName = it.appName,
                defaultRecognitionSource = it.defaultRecognitionSource,
                defaultRoi = it.defaultRoiJson?.let { json -> gson.fromJson(json, com.monster.literaryflow.core.model.NormalizedRect::class.java) },
                landscape = it.landscape,
                allowShizuku = it.allowShizuku,
                favorite = it.favorite
            )
        }
        return AutomationDocument(tasks = tasks, appProfiles = appProfiles)
    }

    private fun TaskModel.toEntity(): TaskEntity2 = TaskEntity2(
        id = id,
        title = title,
        enabled = enabled,
        targetPackageName = targetPackageName,
        targetAppName = targetAppName,
        allowAutoOpenApp = allowAutoOpenApp,
        scheduleType = scheduleType,
        startTime = startTime,
        endTime = endTime,
        weeklyDays = weeklyDays,
        runTimes = runTimes,
        intervalMs = intervalMs,
        createdAt = createdAt,
        updatedAt = updatedAt,
        schemaVersion = schemaVersion,
        favorite = favorite,
        category = category,
        estimatedDurationMs = estimatedDurationMs,
        todayRunCount = todayRunCount
    )

    private fun StepModel.toEntity(): StepEntity2 = StepEntity2(
        id = id,
        taskId = taskId,
        orderIndex = orderIndex,
        type = type,
        title = title,
        timeoutMs = timeoutMs,
        delayAfterMs = delayAfterMs,
        failurePolicy = failurePolicy,
        description = description,
        waitBeforeMs = waitBeforeMs,
        retryCount = retryCount
    )

    private fun ActionSpec.toEntity(stepId: Long, id: Long): ActionEntity2 = ActionEntity2(
        id = id,
        stepId = stepId,
        type = type,
        x = x,
        y = y,
        text = text,
        matchType = matchType,
        recognitionSource = recognitionSource,
        swipeJson = swipe?.let { gson.toJson(it) },
        inputText = inputText,
        childTaskId = childTaskId,
        childTaskTitle = childTaskTitle,
        targetPackageName = targetPackageName,
        targetAppName = targetAppName,
        roiJson = roi?.let { gson.toJson(it) },
        timeoutMs = timeoutMs,
        repeatCount = repeatCount,
        legacyNote = legacyNote
    )

    private fun ConditionSpec.toEntity(stepId: Long, id: Long): ConditionEntity2 = ConditionEntity2(
        id = id,
        stepId = stepId,
        type = type,
        text = text,
        excludedText = excludedText,
        matchType = matchType,
        recognitionSource = recognitionSource,
        startTime = startTime,
        endTime = endTime,
        timeoutMs = timeoutMs,
        checkIntervalMs = checkIntervalMs,
        onTrueActionsJson = gson.toJson(onTrueActions),
        onFalseActionsJson = gson.toJson(onFalseActions),
        legacyNote = legacyNote
    )

    private fun AppProfileModel.toEntity(): AppProfileEntity2 = AppProfileEntity2(
        id = id,
        packageName = packageName,
        appName = appName,
        defaultRecognitionSource = defaultRecognitionSource,
        defaultRoiJson = defaultRoi?.let { gson.toJson(it) },
        landscape = landscape,
        allowShizuku = allowShizuku,
        favorite = favorite
    )

    private fun TaskEntity2.toModel(steps: List<StepModel>): TaskModel = TaskModel(
        id = id,
        title = title,
        enabled = enabled,
        targetPackageName = targetPackageName,
        targetAppName = targetAppName,
        allowAutoOpenApp = allowAutoOpenApp,
        scheduleType = scheduleType,
        startTime = startTime,
        endTime = endTime,
        weeklyDays = weeklyDays,
        runTimes = runTimes,
        intervalMs = intervalMs,
        createdAt = createdAt,
        updatedAt = updatedAt,
        schemaVersion = schemaVersion,
        steps = steps,
        favorite = favorite,
        category = category,
        estimatedDurationMs = estimatedDurationMs,
        todayRunCount = todayRunCount
    )

    private fun StepEntity2.toModel(actions: List<ActionSpec>, conditions: List<ConditionSpec>): StepModel =
        StepModel(
            id = id,
            taskId = taskId,
            orderIndex = orderIndex,
            type = type,
            title = title,
            timeoutMs = timeoutMs,
            delayAfterMs = delayAfterMs,
            failurePolicy = failurePolicy,
            actions = actions,
            conditions = conditions,
            description = description,
            waitBeforeMs = waitBeforeMs,
            retryCount = retryCount
        )

    private fun ActionEntity2.toModel(): ActionSpec = ActionSpec(
        type = type,
        x = x,
        y = y,
        text = text,
        matchType = matchType,
        recognitionSource = recognitionSource,
        swipe = swipeJson?.let {
            gson.fromJson(it, com.monster.literaryflow.core.model.SwipeSpec::class.java)
        },
        inputText = inputText,
        childTaskId = childTaskId,
        childTaskTitle = childTaskTitle,
        targetPackageName = targetPackageName,
        targetAppName = targetAppName,
        roi = roiJson?.let {
            gson.fromJson(it, com.monster.literaryflow.core.model.NormalizedRect::class.java)
        },
        timeoutMs = timeoutMs,
        repeatCount = repeatCount,
        legacyNote = legacyNote
    )

    private fun ConditionEntity2.toModel(): ConditionSpec = ConditionSpec(
        type = type,
        text = text,
        excludedText = excludedText,
        matchType = matchType,
        recognitionSource = recognitionSource,
        startTime = startTime,
        endTime = endTime,
        timeoutMs = timeoutMs,
        checkIntervalMs = checkIntervalMs,
        onTrueActions = readActions(onTrueActionsJson),
        onFalseActions = readActions(onFalseActionsJson),
        legacyNote = legacyNote
    )

    private fun readActions(json: String): List<ActionSpec> {
        val type = object : com.google.gson.reflect.TypeToken<List<ActionSpec>>() {}.type
        return gson.fromJson<List<ActionSpec>>(json, type).orEmpty()
    }
}
