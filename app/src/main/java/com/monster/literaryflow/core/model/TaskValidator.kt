package com.monster.literaryflow.core.model

data class ValidationIssue(
    val path: String,
    val message: String
)

object TaskValidator {
    fun validate(document: AutomationDocument): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val taskIds = mutableSetOf<Long>()
        document.tasks.forEachIndexed { taskIndex, task ->
            val taskPath = "tasks[$taskIndex]"
            if (task.id == 0L) issues += ValidationIssue("$taskPath.id", "任务 id 不能为 0")
            if (!taskIds.add(task.id)) issues += ValidationIssue("$taskPath.id", "任务 id 重复")
            if (task.title.isBlank()) issues += ValidationIssue("$taskPath.title", "任务名称不能为空")
            val stepIds = mutableSetOf<Long>()
            task.steps.forEachIndexed { stepIndex, step ->
                val stepPath = "$taskPath.steps[$stepIndex]"
                if (step.id == 0L) issues += ValidationIssue("$stepPath.id", "步骤 id 不能为 0")
                if (!stepIds.add(step.id)) issues += ValidationIssue("$stepPath.id", "步骤 id 重复")
                if (step.taskId != task.id) issues += ValidationIssue("$stepPath.taskId", "步骤不属于当前任务")
                if (step.timeoutMs < 0L || step.delayAfterMs < 0L) {
                    issues += ValidationIssue(stepPath, "超时和延迟不能为负数")
                }
                step.actions.forEachIndexed { actionIndex, action ->
                    val actionPath = "$stepPath.actions[$actionIndex]"
                    if (action.timeoutMs < 0L || action.repeatCount < 1) {
                        issues += ValidationIssue(actionPath, "动作超时不能为负数，重复次数至少为 1")
                    }
                    if (action.x != null && action.x !in 0f..1f) issues += ValidationIssue("$actionPath.x", "x 必须在 0..1")
                    if (action.y != null && action.y !in 0f..1f) issues += ValidationIssue("$actionPath.y", "y 必须在 0..1")
                }
            }
        }
        return issues
    }
}
