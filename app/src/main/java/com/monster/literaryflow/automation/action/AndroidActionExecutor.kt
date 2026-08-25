package com.monster.literaryflow.automation.action

import android.content.Context
import cn.coderpig.cp_fast_accessibility.back
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.swipe
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.autoRun.AutoRunManager
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.result.ErrorCode
import com.monster.literaryflow.core.result.FlowError
import com.monster.literaryflow.core.result.FlowResult
import com.monster.literaryflow.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AndroidActionExecutor(
    private val appContext: Context
) : ActionExecutor {
    override suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit> {
        return try {
            when (action.type) {
                ActionType.TAP_COORDINATE -> tapCoordinate(action, context)
                ActionType.TAP_TEXT -> tapText(action)
                ActionType.SWIPE -> swipeAction(action, context)
                ActionType.INPUT_TEXT -> inputText(action)
                ActionType.BACK -> globalBack()
                ActionType.OPEN_APP -> openApp(action)
                ActionType.LONG_PRESS -> longPress(action, context)
                ActionType.RUN_SUBTASK -> FlowResult.Failure(
                    FlowError(
                        ErrorCode.ACTION_FAILED,
                        "子任务 ${action.childTaskTitle ?: action.childTaskId ?: ""} 尚未绑定为新版任务"
                    )
                )
                ActionType.WAIT -> {
                    delay(action.timeoutMs.coerceAtLeast(0L))
                    FlowResult.Success(Unit)
                }
                ActionType.WAIT_FOR_TEXT -> waitForText(action)
                ActionType.WAIT_FOR_SCREEN -> waitForScreen(action)
            }
        } catch (error: Exception) {
            FlowResult.Failure(
                FlowError(
                    ErrorCode.ACTION_FAILED,
                    error.message ?: "动作执行失败：${action.type}",
                    error
                )
            )
        }
    }

    private suspend fun tapCoordinate(
        action: ActionSpec,
        context: ActionExecutionContext
    ): FlowResult<Unit> = withContext(Dispatchers.Main) {
        val x = action.x?.toPixels(context.screenWidth)
            ?: return@withContext missing("缺少点击坐标 x")
        val y = action.y?.toPixels(context.screenHeight)
            ?: return@withContext missing("缺少点击坐标 y")
        click(x, y, duration = 160L, repeatCount = action.repeatCount.coerceAtLeast(1))
        FlowResult.Success(Unit)
    }

    private suspend fun longPress(
        action: ActionSpec,
        context: ActionExecutionContext
    ): FlowResult<Unit> = withContext(Dispatchers.Main) {
        val x = action.x?.toPixels(context.screenWidth)
            ?: return@withContext missing("缺少长按坐标 x")
        val y = action.y?.toPixels(context.screenHeight)
            ?: return@withContext missing("缺少长按坐标 y")
        click(
            x = x,
            y = y,
            duration = action.timeoutMs.coerceAtLeast(500L),
            repeatCount = action.repeatCount.coerceAtLeast(1)
        )
        FlowResult.Success(Unit)
    }

    private suspend fun swipeAction(
        action: ActionSpec,
        context: ActionExecutionContext
    ): FlowResult<Unit> = withContext(Dispatchers.Main) {
        val spec = action.swipe ?: return@withContext missing("缺少滑动参数")
        swipe(
            startX = spec.startX.toPixels(context.screenWidth),
            startY = spec.startY.toPixels(context.screenHeight),
            endX = spec.endX.toPixels(context.screenWidth),
            endY = spec.endY.toPixels(context.screenHeight),
            duration = spec.durationMs,
            repeatCount = action.repeatCount.coerceAtLeast(1)
        )
        FlowResult.Success(Unit)
    }

    private suspend fun tapText(action: ActionSpec): FlowResult<Unit> {
        val target = action.text?.takeIf { it.isNotBlank() }
            ?: return missing("缺少目标文字")
        val matchType = action.matchType.toLegacyTextPickType()
        val timeoutSec = action.timeoutMs.toTimeoutSeconds()
        return when (action.recognitionSource ?: RecognitionSource.AUTO) {
            RecognitionSource.ACCESSIBILITY,
            RecognitionSource.AUTO -> {
                val service = accessibilityService()
                    ?: return permissionMissing("无障碍服务未连接")
                if (withContext(Dispatchers.Main) { service.clickText(target, matchType, timeoutSec) }) {
                    FlowResult.Success(Unit)
                } else {
                    FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "未找到可点击文字：$target"))
                }
            }
            RecognitionSource.OCR,
            RecognitionSource.TEMPLATE -> tapTextByOcr(target, matchType, timeoutSec)
        }
    }

    private suspend fun waitForText(action: ActionSpec): FlowResult<Unit> {
        val target = action.text?.takeIf { it.isNotBlank() }
            ?: return missing("缺少等待文字")
        val matchType = action.matchType.toLegacyTextPickType()
        val timeoutSec = action.timeoutMs.toTimeoutSeconds()
        return when (action.recognitionSource ?: RecognitionSource.AUTO) {
            RecognitionSource.ACCESSIBILITY,
            RecognitionSource.AUTO -> {
                val service = accessibilityService()
                    ?: return permissionMissing("无障碍服务未连接")
                if (withContext(Dispatchers.Main) { service.findText(target, matchType, timeoutSec) }) {
                    FlowResult.Success(Unit)
                } else {
                    FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "等待文字超时：$target"))
                }
            }
            RecognitionSource.OCR,
            RecognitionSource.TEMPLATE -> {
                val (found, _) = AutoRunManager.findText(target, matchType, timeoutSec)
                if (found) {
                    FlowResult.Success(Unit)
                } else {
                    FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "OCR 等待文字超时：$target"))
                }
            }
        }
    }

    private suspend fun waitForScreen(action: ActionSpec): FlowResult<Unit> {
        if (action.text.isNullOrBlank()) {
            val deadline = System.currentTimeMillis() + action.timeoutMs.coerceAtLeast(1000L)
            while (System.currentTimeMillis() < deadline) {
                if (MyApp.image.value != null) return FlowResult.Success(Unit)
                delay(100L)
            }
            return FlowResult.Failure(FlowError(ErrorCode.SCREENSHOT_FAILED, "等待屏幕捕获超时"))
        }
        return waitForText(action.copy(type = ActionType.WAIT_FOR_TEXT))
    }

    private suspend fun tapTextByOcr(
        target: String,
        matchType: TextPickType,
        timeoutSec: Int
    ): FlowResult<Unit> {
        val (_, block) = AutoRunManager.findText(target, matchType, timeoutSec)
        val box = block?.boundingBox
            ?: return FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "OCR 未找到文字：$target"))
        withContext(Dispatchers.Main) {
            click(box.centerX(), box.centerY())
        }
        return FlowResult.Success(Unit)
    }

    private suspend fun inputText(action: ActionSpec): FlowResult<Unit> {
        val input = action.inputText ?: action.text
            ?: return missing("缺少输入内容")
        val service = accessibilityService()
            ?: return permissionMissing("无障碍服务未连接")
        return if (withContext(Dispatchers.Main) { service.enterText(input) }) {
            FlowResult.Success(Unit)
        } else {
            FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "未找到可输入控件"))
        }
    }

    private suspend fun globalBack(): FlowResult<Unit> = withContext(Dispatchers.Main) {
        back()
        FlowResult.Success(Unit)
    }

    private suspend fun openApp(action: ActionSpec): FlowResult<Unit> {
        val packageName = action.targetPackageName?.takeIf { it.isNotBlank() }
            ?: return missing("缺少目标应用包名")
        AppUtils.openApp(appContext, packageName)
        return FlowResult.Success(Unit)
    }

    private fun accessibilityService(): MyAccessibilityService? {
        return FastAccessibilityService.instance as? MyAccessibilityService
    }

    private fun Float.toPixels(size: Int): Int {
        return (coerceIn(0f, 1f) * size).toInt().coerceIn(0, size.coerceAtLeast(1) - 1)
    }

    private fun Long.toTimeoutSeconds(): Int {
        return (coerceAtLeast(1000L) / 1000L).toInt().coerceAtLeast(1)
    }

    private fun MatchType?.toLegacyTextPickType(): TextPickType = when (this) {
        MatchType.EXACT, null -> TextPickType.EXACT_MATCH
        MatchType.FUZZY -> TextPickType.FUZZY_MATCH
        MatchType.ANY_KEYWORD -> TextPickType.MULTIPLE_FUZZY_WORDS
    }

    private fun missing(message: String): FlowResult.Failure {
        return FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, message))
    }

    private fun permissionMissing(message: String): FlowResult.Failure {
        return FlowResult.Failure(FlowError(ErrorCode.PERMISSION_MISSING, message))
    }
}
