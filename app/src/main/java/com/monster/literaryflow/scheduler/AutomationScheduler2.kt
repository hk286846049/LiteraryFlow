package com.monster.literaryflow.scheduler

import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.core.result.FlowResult

interface AutomationScheduler2 {
    suspend fun schedule(task: TaskModel): FlowResult<Unit>
    suspend fun cancel(taskId: Long): FlowResult<Unit>
}

class InMemoryAutomationScheduler2 : AutomationScheduler2 {
    private val scheduledTasks = linkedMapOf<Long, TaskModel>()

    override suspend fun schedule(task: TaskModel): FlowResult<Unit> {
        scheduledTasks[task.id] = task
        return FlowResult.Success(Unit)
    }

    override suspend fun cancel(taskId: Long): FlowResult<Unit> {
        scheduledTasks.remove(taskId)
        return FlowResult.Success(Unit)
    }

    fun snapshot(): List<TaskModel> = scheduledTasks.values.toList()
}
