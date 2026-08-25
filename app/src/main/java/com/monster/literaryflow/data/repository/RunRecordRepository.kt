package com.monster.literaryflow.data.repository

import android.content.Context
import com.monster.literaryflow.data.db2.LiteraryFlow2Database
import com.monster.literaryflow.data.db2.RunRecordEntity2
import com.monster.literaryflow.data.db2.StepRecordEntity2

class RunRecordRepository(context: Context) {
    private val dao = LiteraryFlow2Database.getDatabase(context).dao()

    fun start(taskId: Long): Long {
        val id = System.currentTimeMillis()
        dao.upsertRunRecord(
            RunRecordEntity2(
                id = id,
                taskId = taskId,
                startedAt = id,
                endedAt = null,
                status = "RUNNING",
                message = "任务开始"
            )
        )
        return id
    }

    fun finish(id: Long, status: String, message: String?) {
        dao.finishRunRecord(id, System.currentTimeMillis(), status, message)
    }

    fun startStep(runRecordId: Long, stepId: Long): Long {
        val id = System.nanoTime()
        dao.upsertStepRecord(StepRecordEntity2(id, runRecordId, stepId, System.currentTimeMillis(), null, "RUNNING", null))
        return id
    }

    fun finishStep(id: Long, status: String, message: String?) {
        dao.finishStepRecord(id, System.currentTimeMillis(), status, message)
    }

    fun steps(runRecordId: Long): List<StepRecordEntity2> = dao.getStepRecords(runRecordId)

    fun recent(limit: Int = 50): List<RunRecordEntity2> = dao.getRecentRunRecords(limit)

    fun clear() {
        dao.clearStepRecords()
        dao.clearRunRecords()
    }
}
