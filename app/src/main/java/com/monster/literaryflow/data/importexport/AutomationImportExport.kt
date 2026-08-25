package com.monster.literaryflow.data.importexport

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.TaskValidator
import com.monster.literaryflow.core.result.ErrorCode
import com.monster.literaryflow.core.result.FlowError
import com.monster.literaryflow.core.result.FlowResult

object AutomationImportExport {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun importJson(json: String): FlowResult<ImportReport> {
        return try {
            val root = JsonParser.parseString(json)
            when {
                root.isNewDocument() -> importNewDocument(root)
                else -> importLegacyDocument(root)
            }
        } catch (error: Exception) {
            FlowResult.Failure(
                FlowError(
                    code = ErrorCode.INVALID_IMPORT_FORMAT,
                    message = "无法识别导入 JSON：${error.message}",
                    cause = error
                )
            )
        }
    }

    fun exportDocument(document: AutomationDocument): String = gson.toJson(document)

    private fun importNewDocument(root: JsonElement): FlowResult<ImportReport> {
        val document = gson.fromJson(root, AutomationDocument::class.java)
        if (document.schemaVersion > AutomationDocument.CURRENT_SCHEMA_VERSION) {
            return FlowResult.Failure(
                FlowError(
                    code = ErrorCode.UNSUPPORTED_SCHEMA,
                    message = "导入数据版本 ${document.schemaVersion} 高于当前支持版本 ${AutomationDocument.CURRENT_SCHEMA_VERSION}"
                )
            )
        }
        val issues = TaskValidator.validate(document)
        if (issues.isNotEmpty()) {
            return FlowResult.Failure(
                FlowError(
                    code = ErrorCode.INVALID_IMPORT_FORMAT,
                    message = "新版任务校验失败：${issues.take(3).joinToString { "${it.path} ${it.message}" }}"
                )
            )
        }
        return FlowResult.Success(
            ImportReport(
                source = ImportSource.NEW_SCHEMA,
                document = document,
                convertedTaskCount = document.tasks.size,
                warnings = emptyList()
            )
        )
    }

    private fun importLegacyDocument(root: JsonElement): FlowResult<ImportReport> {
        val legacyTasks = readLegacyTasks(root)
        if (legacyTasks.isEmpty()) {
            return FlowResult.Failure(
                FlowError(
                    code = ErrorCode.INVALID_IMPORT_FORMAT,
                    message = "没有找到旧版 AutoInfo 任务数据"
                )
            )
        }

        val document = LegacyAutoInfoMapper.convertAll(legacyTasks)
        val warnings = listOf(
            "旧版绝对坐标已按 1080x1920 归一化，导入后建议重新拾取坐标/ROI。",
            "旧版子任务只能保留标题和动作语义，导入后需要在新版任务中重新绑定。"
        )
        return FlowResult.Success(
            ImportReport(
                source = ImportSource.LEGACY_AUTO_INFO,
                document = document,
                convertedTaskCount = document.tasks.size,
                warnings = warnings
            )
        )
    }

    private fun readLegacyTasks(root: JsonElement): List<AutoInfo> {
        return when {
            root.isJsonArray -> parseLegacyArray(root.asJsonArray)
            root.isJsonObject -> parseLegacyObject(root.asJsonObject)
            else -> emptyList()
        }
    }

    private fun parseLegacyArray(array: JsonArray): List<AutoInfo> {
        val type = object : TypeToken<List<AutoInfo>>() {}.type
        val normalized = JsonArray()
        array.forEach { element ->
            normalized.add(normalizeLegacyElement(element))
        }
        return gson.fromJson<List<AutoInfo>>(normalized, type).orEmpty()
    }

    private fun parseLegacyObject(obj: JsonObject): List<AutoInfo> {
        val candidateKeys = listOf("autoInfoList", "autoInfos", "tasks", "data", "list")
        candidateKeys.forEach { key ->
            val value = obj.get(key)
            if (value != null && value.isJsonArray) {
                return parseLegacyArray(value.asJsonArray)
            }
        }

        val looksLikeAutoInfo = obj.has("runInfo") || obj.has("run_info") || obj.has("runPackageName") ||
            obj.has("run_package_name") || obj.has("loopType") || obj.has("loop_type")
        return if (looksLikeAutoInfo) {
            listOf(gson.fromJson(normalizeLegacyObject(obj), AutoInfo::class.java))
        } else {
            emptyList()
        }
    }

    private fun normalizeLegacyElement(element: JsonElement): JsonElement {
        return if (element.isJsonObject) normalizeLegacyObject(element.asJsonObject) else element
    }

    private fun normalizeLegacyObject(obj: JsonObject): JsonObject {
        val aliases = mapOf(
            "run_state" to "runState",
            "run_app_name" to "runAppName",
            "run_package_name" to "runPackageName",
            "run_times" to "runTimes",
            "run_time" to "runTime",
            "app_level" to "appLevel",
            "run_info" to "runInfo",
            "is_run" to "isRun",
            "today_run_time" to "todayRunTime",
            "monitor_list" to "monitorList",
            "loop_type" to "loopType",
            "week_data" to "weekData",
            "sleep_time" to "sleepTime",
            "click_bean" to "clickBean",
            "trigger_bean" to "triggerBean",
            "click_type" to "clickType",
            "click_xy" to "clickXy",
            "scroll_min_max" to "scrollMinMax",
            "find_text_time" to "findTextTime",
            "loop_times" to "loopTimes",
            "scroll_time" to "scrollTime",
            "find_text_type" to "findTextType",
            "enter_text" to "enterText",
            "run_task" to "runTask",
            "open_app_data" to "openAppData",
            "is_find_text4_node" to "isFindText4Node",
            "long_click_time" to "longClickTime",
            "trigger_type" to "triggerType",
            "find_text" to "findText",
            "run_scan_time" to "runScanTime",
            "run_true_auto" to "runTrueAuto",
            "run_true_task" to "runTrueTask",
            "run_false_auto" to "runFalseAuto",
            "run_false_task" to "runFalseTask"
        )
        val normalized = JsonObject()
        obj.entrySet().forEach { (key, rawValue) ->
            val normalizedKey = aliases[key] ?: key
            normalized.add(normalizedKey, normalizeLegacyValue(normalizedKey, rawValue))
        }
        return normalized
    }

    private fun normalizeLegacyValue(key: String, value: JsonElement): JsonElement {
        if (value is JsonPrimitive && value.isString && key in jsonEncodedLegacyKeys) {
            val text = value.asString
            if (text.isBlank() || text.equals("null", ignoreCase = true)) {
                return value
            }
            return try {
                normalizeLegacyValue(key, JsonParser.parseString(text))
            } catch (_: Exception) {
                value
            }
        }
        return when {
            value.isJsonObject -> normalizeLegacyObject(value.asJsonObject)
            value.isJsonArray -> JsonArray().also { array ->
                value.asJsonArray.forEach { array.add(normalizeLegacyElement(it)) }
            }
            else -> value
        }
    }

    private fun JsonElement.isNewDocument(): Boolean {
        if (!isJsonObject) return false
        val obj = asJsonObject
        return obj.has("schemaVersion") && obj.has("tasks")
    }

    private val jsonEncodedLegacyKeys = setOf(
        "runTime",
        "runInfo",
        "todayRunTime",
        "monitorList",
        "weekData",
        "clickXy",
        "scrollMinMax",
        "runTask",
        "openAppData",
        "runTrueAuto",
        "runFalseAuto"
    )
}

data class ImportReport(
    val source: ImportSource,
    val document: AutomationDocument,
    val convertedTaskCount: Int,
    val warnings: List<String>
)

enum class ImportSource {
    NEW_SCHEMA,
    LEGACY_AUTO_INFO
}
