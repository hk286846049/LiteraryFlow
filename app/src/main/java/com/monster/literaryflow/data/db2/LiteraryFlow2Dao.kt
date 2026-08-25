package com.monster.literaryflow.data.db2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LiteraryFlow2Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTasks(tasks: List<TaskEntity2>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSteps(steps: List<StepEntity2>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertActions(actions: List<ActionEntity2>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertConditions(conditions: List<ConditionEntity2>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAppProfiles(appProfiles: List<AppProfileEntity2>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertRunRecord(record: RunRecordEntity2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertStepRecord(record: StepRecordEntity2)

    @Query("SELECT * FROM lf2_run_records ORDER BY started_at DESC LIMIT :limit")
    fun getRecentRunRecords(limit: Int = 50): List<RunRecordEntity2>

    @Query("DELETE FROM lf2_run_records")
    fun clearRunRecords()

    @Query("DELETE FROM lf2_step_records")
    fun clearStepRecords()

    @Query("UPDATE lf2_run_records SET ended_at = :endedAt, status = :status, message = :message WHERE id = :id")
    fun finishRunRecord(id: Long, endedAt: Long, status: String, message: String?)

    @Query("SELECT * FROM lf2_step_records WHERE run_record_id = :runRecordId ORDER BY started_at ASC")
    fun getStepRecords(runRecordId: Long): List<StepRecordEntity2>

    @Query("UPDATE lf2_step_records SET ended_at = :endedAt, status = :status, message = :message WHERE id = :id")
    fun finishStepRecord(id: Long, endedAt: Long, status: String, message: String?)

    @Query("SELECT * FROM lf2_tasks ORDER BY updated_at DESC")
    fun getTasks(): List<TaskEntity2>

    @Query("SELECT * FROM lf2_steps WHERE task_id = :taskId ORDER BY order_index ASC")
    fun getSteps(taskId: Long): List<StepEntity2>

    @Query("SELECT * FROM lf2_actions WHERE step_id = :stepId")
    fun getActions(stepId: Long): List<ActionEntity2>

    @Query("SELECT * FROM lf2_conditions WHERE step_id = :stepId")
    fun getConditions(stepId: Long): List<ConditionEntity2>

    @Query("SELECT * FROM lf2_app_profiles ORDER BY app_name ASC")
    fun getAppProfiles(): List<AppProfileEntity2>

    @Query("DELETE FROM lf2_tasks")
    fun clearTasks()

    @Transaction
    fun replaceImportedData(
        tasks: List<TaskEntity2>,
        steps: List<StepEntity2>,
        actions: List<ActionEntity2>,
        conditions: List<ConditionEntity2>,
        appProfiles: List<AppProfileEntity2>
    ) {
        clearTasks()
        upsertTasks(tasks)
        upsertSteps(steps)
        upsertActions(actions)
        upsertConditions(conditions)
        upsertAppProfiles(appProfiles)
    }
}
