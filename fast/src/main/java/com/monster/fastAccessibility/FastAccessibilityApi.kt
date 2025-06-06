package cn.coderpig.cp_fast_accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.monster.fastAccessibility.Const.Companion.multipleX
import com.monster.fastAccessibility.Const.Companion.multipleY
import com.monster.fastAccessibility.FastAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.random.Random


/**
 * 跳转其它APP
 * @param packageName 跳转APP包名
 * @param activityName 跳转APP的Activity名
 * @param errorTips 跳转页面不存在时的提示
 * */
fun Context.startApp(packageName: String, activityName: String, errorTips: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(packageName, activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: ActivityNotFoundException) {
        shortToast(errorTips)
    } catch (e: Exception) {
        e.message?.let { logD(it) }
    }
}

/**
 * 无障碍服务是否可用
 * */
val isAccessibilityEnable = FastAccessibilityService.isServiceEnable

/**
 * 请求无障碍服务
 * */
fun requireAccessibility() = FastAccessibilityService.requireAccessibility()

/**
 * 全局操作快速调用
 * */
fun performAction(action: Int) = FastAccessibilityService.require?.performGlobalAction(action)

// 返回
fun back() = performAction(AccessibilityService.GLOBAL_ACTION_BACK)

// Home键
fun home() = performAction(AccessibilityService.GLOBAL_ACTION_HOME)

// 最近任务
fun recent() = performAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

// 电源菜单
fun powerDialog() = performAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

// 通知栏
fun notificationBar() = performAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

// 通知栏 → 快捷设置
fun quickSettings() = performAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

// 锁屏
@RequiresApi(Build.VERSION_CODES.P)
fun lockScreen() = performAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

// 应用分屏
fun splitScreen() = performAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

// 休眠
fun sleep(millis: Long) = Thread.sleep(millis)

/**
 * 手势模拟相关
 * */
// 快速生成GestureDescription的方法
fun fastGestureDescription(
    operate: (Path) -> Unit,
    startTime: Long = 0L,
    duration: Long = 50L
): GestureDescription =
    GestureDescription.Builder().apply {
        addStroke(GestureDescription.StrokeDescription(Path().apply {
            operate.invoke(this)
        }, startTime, duration))
    }.build()

// 快速生成GestureResultCallback的方法
fun fastGestureCallback() = object : AccessibilityService.GestureResultCallback() {
    override fun onCompleted(gestureDescription: GestureDescription?) {
        super.onCompleted(gestureDescription)
        // 手势执行完成回调
    }
}

/**
 * 使用手势模拟点击，长按的话，传入的Duration大一些就好，比如1000(1s)
 *
 * @param x 点击坐标点的x坐标
 * @param y 点击坐标点的y坐标
 * @param delayTime 延迟多久进行本次点击，单位毫秒
 * @param duration 模拟触摸屏幕的时长(按下到抬起)，太短会导致部分应用下点击无效，单位毫秒
 * @param repeatCount 本次点击重复多少次，必须大于0
 * @param randomPosition 点击位置随机偏移距离，用于反检测
 * @param randomTime 在随机参数上加减延时时长，有助于防止点击器检测，单位毫秒
 *
 * */
fun click(
    x: Int,
    y: Int,
    delayTime: Long = 0,
    duration: Long = 200,
    repeatCount: Int = 1,
    randomPosition: Int = 5,
    randomTime: Long = 50
) {
    repeat(repeatCount) {
        // 生成(-randomPosition，randomPosition]间的随机数
        val tempX = x + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempY = y + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
        FastAccessibilityService.require?.dispatchGesture(
            fastGestureDescription({
                it.moveTo(
                    if (tempX < 0) x.toFloat() else tempX.toFloat(),
                    if (tempY < 0) y.toFloat() else tempY.toFloat()
                )
            }, delayTime, tempDuration), fastGestureCallback(), null
        )
    }
}

/**
 * 使用手势模拟点击，长按的话，传入的Duration大一些就好，比如1000(1s)
 *
 * @param x 点击坐标点的x坐标
 * @param y 点击坐标点的y坐标
 * @param delayTime 延迟多久进行本次点击，单位毫秒
 * @param duration 模拟触摸屏幕的时长(按下到抬起)，太短会导致部分应用下点击无效，单位毫秒
 * @param repeatCount 本次点击重复多少次，必须大于0
 * @param randomTime 在随机参数上加减延时时长，有助于防止点击器检测，单位毫秒
 *
 * */
/*fun clickA(
    minX: Int,
    minY: Int,
    randomPosition: Int = 10,
    delayTime: Long = 0,
    duration: Long = 200,
    repeatCount: Int = 1,
    randomTime: Long = 100
) {
    repeat(repeatCount) {
        val tempX = minX + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempY = minY + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
        FastAccessibilityService.require?.dispatchGesture(
            fastGestureDescription({
                it.moveTo(
                    tempX.toFloat(),
                    tempY.toFloat()
                )
            }, delayTime, tempDuration), fastGestureCallback(), null
        )
    }
}*/


fun clickA(
    minX: Int,
    minY: Int,
    maxX: Int,
    maxY: Int,
    delayTime: Long = 0,
    duration: Long = 200,
    repeatCount: Int = 1,
    randomTime: Long = 0
) {
    repeat(repeatCount) {
        val tempX = (minX..maxX).random()
        val tempY = (minY..maxY).random()
        val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
        FastAccessibilityService.require?.dispatchGesture(
            fastGestureDescription({
                it.moveTo(
                    tempX * multipleX.toFloat(),
                    tempY * multipleY.toFloat()
                )
            }, delayTime, tempDuration), fastGestureCallback(), null
        )
    }
}

fun clickA(
    minX: Int,
    minY: Int,

    delayTime: Long = 0,
    duration: Long = 351,
    repeatCount: Int = 1,
    randomTime: Long = 0
) {
    Log.d("#####MONSTER#####", "clickA x:$minX, y:$minY")
    repeat(repeatCount) {
        val tempX = minX * multipleX
        val tempY = minY * multipleY
        val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
        FastAccessibilityService.require?.dispatchGesture(
            fastGestureDescription({
                it.moveTo(
                    tempX.toFloat(),
                    tempY.toFloat()
                )
            }, delayTime, duration), fastGestureCallback(), null
        )
    }
}


var isSliding = false // 全局控制滑动状态
// 添加类变量用于记录上次滑动终点位置
private var lastEndX: Int = 0
private var lastEndY: Int = 0

suspend fun startSliding(
    direction: String,   // 滑动方向：上下 ("vertical") 或 左右 ("horizontal")
    startX: Int,         // 滑动起点 X
    startY: Int,         // 滑动起点 Y
    minSwipeValue: Int,  // 滑动最小值（左侧边界）
    maxSwipeValue: Int,  // 滑动最大值（右侧边界）
    duration: Long       // 每次滑动的持续时间
) {
    var scrollType = 0
    val swipeDuration: Long = 100L
    val endTime = System.currentTimeMillis() + duration
    var currentX = startX
    var currentY = startY

    if (isSliding) {
        Log.d("Slide", "Sliding is already running!")
        return
    }

    isSliding = true

    // 初始化终点记录（首次使用传入起点）
    if (lastEndX == 0 && lastEndY == 0) {
        lastEndX = startX
        lastEndY = startY
    }

    while (System.currentTimeMillis() < endTime) {
        FastAccessibilityService.require?.dispatchGesture(
            fastGestureDescription({
                when(scrollType) {
                    0 -> {
                        // 首次滑动使用初始起点，后续使用记录的终点
                        val actualStartX = if (scrollType == 0) startX else lastEndX
                        val actualStartY = if (scrollType == 0) startY else lastEndY

                        it.moveTo(actualStartX.toFloat(), actualStartY.toFloat())
                        if (direction == "horizontal") {
                            it.lineTo(minSwipeValue.toFloat(), actualStartY.toFloat())
                            lastEndX = minSwipeValue
                            lastEndY = actualStartY
                        } else {
                            it.lineTo(actualStartX.toFloat(), minSwipeValue.toFloat())
                            lastEndX = actualStartX
                            lastEndY = minSwipeValue
                        }
                        scrollType = 1
                    }
                    1 -> {
                        // 使用上次记录的终点作为本次起点
                        it.moveTo(lastEndX.toFloat(), lastEndY.toFloat())
                        if (direction == "horizontal") {
                            it.lineTo(maxSwipeValue.toFloat(), lastEndY.toFloat())
                            lastEndX = maxSwipeValue
                        } else {
                            it.lineTo(lastEndX.toFloat(), maxSwipeValue.toFloat())
                            lastEndY = maxSwipeValue
                        }
                        scrollType = 2
                    }
                    2 -> {
                        // 使用上次记录的终点作为本次起点
                        it.moveTo(lastEndX.toFloat(), lastEndY.toFloat())
                        if (direction == "horizontal") {
                            val endX = minSwipeValue.toFloat() + Random.nextInt(-10,10)
                            it.lineTo(endX, lastEndY.toFloat())
                            lastEndX = endX.toInt()
                        } else {
                            val endY = minSwipeValue.toFloat() + Random.nextInt(-10,10)
                            it.lineTo(lastEndX.toFloat(),endY)
                            lastEndY = endY.toInt()
                        }
                        scrollType = 1
                    }
                }
            }, 0, swipeDuration),
            fastGestureCallback(),
            null
        )
        delay(swipeDuration + 30)
    }
    isSliding = false
}



fun stopSliding() {
    isSliding = false
    Log.d("Slide", "Sliding stopped.")
}


/**
 * 使用手势模拟滑动
 *
 * @param startX 滑动起始坐标点的x坐标
 * @param startY 滑动起始坐标点的y坐标
 * @param endX 滑动终点坐标点的x坐标
 * @param endY 滑动终点坐标点的y坐标
 * @param duration 滑动持续时间，一般在300~500ms效果会好一些，太快会导致滑动不可用
 * @param repeatCount 滑动重复次数
 * @param randomPosition 点击位置随机偏移距离，用于反检测
 * @param randomTime 在随机参数上加减延时时长，有助于防止点击器检测，单位毫秒
 * */
fun swipe(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    duration: Long = 1000L,
    repeatCount: Int = 1,
    randomPosition: Int = 0,
    randomTime: Long = 0
) {
    repeat(repeatCount) {
        // 生成(-randomPosition，randomPosition]间的随机数
        val tempStartX = startX + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempStartY = startY + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempEndX = endX + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempEndY = endY + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
        FastAccessibilityService.require?.dispatchGesture(fastGestureDescription({
            it.moveTo(
                if (tempStartX < 0) startX.toFloat() else tempStartX.toFloat(),
                if (tempStartY < 0) startY.toFloat() else tempStartY.toFloat()
            )
            it.lineTo(
                if (tempEndX < 0) endX.toFloat() else tempEndX.toFloat(),
                if (tempEndY < 0) endY.toFloat() else tempEndY.toFloat()
            )
        }, tempDuration), fastGestureCallback(), null)
    }
}

/**
 * 结点操作相关
 * */

/**
 * 快速查找到操作结点并执行操作的方法
 *
 * @param condition 结点判断表达式
 * @param action 执行的操作
 * */
fun AccessibilityNodeInfo?.fastFindAction(
    condition: (AccessibilityNodeInfo) -> Boolean,
    action: (AccessibilityNodeInfo) -> Unit
) {
    if (this == null) return
    var depthCount = 0  // 查找深度
    var tempNode: AccessibilityNodeInfo? = this // 临时结点
    while (true) {
        if (depthCount < 10 && tempNode != null) {
            if (condition.invoke(tempNode)) {
                action.invoke(tempNode)
            } else {
                tempNode = tempNode.parent
                depthCount++
            }
        } else break
    }
}


/**
 * 结点点击，不过现在很多APP都屏蔽了结点点击，所以默认使用手势模拟
 *
 * @param gestureClick 是否使用手势模拟点击，默认true
 * @param duration 点击时长，默认200ms
 * */
fun NodeWrapper?.click(gestureClick: Boolean = true, duration: Long = 200L) {
    if (this == null) return
    if (gestureClick) {
        bounds?.let {
            val centerX = (it.left + it.right) / 2
            val centerY = (it.left + it.right) / 2
            logD("click- x:$centerX y:$centerY")
            if (centerX >= 0 && centerY >= 0) click(centerX, centerY, 0, duration)
        }
    } else {
        nodeInfo.fastFindAction({ it.isClickable }, {
            it.performAction(if (duration >= 1000L) AccessibilityNodeInfo.ACTION_LONG_CLICK else AccessibilityNodeInfo.ACTION_CLICK)
        })
    }
}

/**
 * 手势模拟
 *
 * @param duration 点击时长，默认200ms
 * */
fun NodeWrapper?.clickX(minX: Int, minY: Int, maxX: Int, maxY: Int, duration: Long = 200L) {
    if (this == null) return
//    clickA(minX, minY, maxX)
}

/**
 * 结点长按，不过现在很多APP都屏蔽了结点点击，所以默认使用手势模拟
 *
 * @param gestureClick 是否使用手势模拟点击，默认true
 * @param duration 点击时长，默认时长1000ms
 * */
fun NodeWrapper?.longClick(gestureClick: Boolean = true, duration: Long = 1000L) {
    if (this == null) return
    click(gestureClick, duration)
}

/**
 * 向前滑动
 * */
fun NodeWrapper?.forward(isForward: Boolean = true) {
    if (this == null) return
    nodeInfo.fastFindAction({ it.isScrollable }, {
        it.performAction(if (isForward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    })
}

/**
 * 向后滑动
 * */
fun NodeWrapper?.backward() = forward(false)


/**
 * 文本填充
 *
 * @param content 要填充的文本
 * */
fun NodeWrapper?.input(content: String) {
    if (this == null) return
    nodeInfo.fastFindAction({ it.isEditable }, {
        it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content)
        })
    })
}


/**
 * 结点解析结果快速调用
 * */

/**
 * 根据文本查找结点
 *
 * @param text 匹配的文本
 * @param textAllMatch 文本全匹配
 * @param includeDesc 同时匹配desc
 * @param descAllMatch desc全匹配
 * @param enableRegular 是否启用正则
 * */
fun AnalyzeSourceResult.findNodeByText(
    text: String,
    textAllMatch: Boolean = false,
    includeDesc: Boolean = false,
    descAllMatch: Boolean = false,
    enableRegular: Boolean = false,
): NodeWrapper? {
    if (enableRegular) {
        val regex = Regex(text)
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (regex.find(node.text!!) != null) return node
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (regex.find(node.description!!) != null) return node
            }
        }
    } else {
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (textAllMatch) {
                    if (text == node.text) return node
                } else {
                    if (node.text!!.contains(text)) return node
                }
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (descAllMatch) {
                    if (text == node.description) return node
                } else {
                    if (node.description!!.contains(text)) return node
                }
            }
        }
    }
    return null
}

/**
 * 根据文本查找结点列表
 *
 * @param text 匹配的文本
 * @param textAllMatch 文本全匹配
 * @param includeDesc 同时匹配desc
 * @param descAllMatch desc全匹配
 * @param enableRegular 是否启用正则
 * */
fun AnalyzeSourceResult.findNodesByText(
    text: String,
    textAllMatch: Boolean = false,
    includeDesc: Boolean = false,
    descAllMatch: Boolean = false,
    enableRegular: Boolean = false,
): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    if (enableRegular) {
        val regex = Regex(text)
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (regex.find(node.text!!) != null) {
                    result.nodes.add(node)
                    return@forEach
                }
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (regex.find(node.description!!) != null) {
                    result.nodes.add(node)
                    return@forEach
                }
            }
        }
    } else {
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (textAllMatch) {
                    if (text == node.text) {
                        result.nodes.add(node)
                        return@forEach
                    }
                } else {
                    if (node.text!!.contains(text)) {
                        result.nodes.add(node)
                        return@forEach
                    }
                }
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (descAllMatch) {
                    if (text == node.description) {
                        result.nodes.add(node)
                        return@forEach
                    }
                } else {
                    if (node.description!!.contains(text)) {
                        result.nodes.add(node)
                        return@forEach
                    }
                }
            }
        }
    }
    return result
}

/**
 * 根据id查找结点 (模糊匹配)
 *
 * @param ids 结点id，可传入多个
 * */
fun AnalyzeSourceResult.findNodeById(vararg ids: String): NodeWrapper? {
    nodes.forEach { node ->
        if (!node.id.isNullOrBlank()) {
            ids.forEach { id -> if (node.id!!.contains(id)) return node }
        }
    }
    return null
}

/**
 * 根据id查找结点列表 (模糊匹配)
 *
 * @param ids 结点id, 可传入多个
 * */
fun AnalyzeSourceResult.findNodesById(vararg ids: String): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    nodes.forEach { node ->
        if (!node.id.isNullOrBlank()) {
            ids.forEach { id -> if (node.id!!.contains(id)) result.nodes.add(node) }
        }
    }
    return result
}

/**
 * 根据传入的表达式结果查找结点
 *
 * @param expression 匹配条件表达式
 * */
fun AnalyzeSourceResult.findNodeByExpression(expression: (NodeWrapper) -> Boolean): NodeWrapper? {
    nodes.forEach { node ->
        if (expression.invoke(node)) return node
    }
    return null
}

/**
 * 根据传入的表达式结果查找结点列表
 *
 * @param expression 匹配条件表达式
 * */
fun AnalyzeSourceResult.findNodesByExpression(expression: (NodeWrapper) -> Boolean): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    nodes.forEach { node ->
        if (expression.invoke(node)) result.nodes.add(node)
    }
    return result
}

/**
 * 查找所有文本不为空的结点
 * */
fun AnalyzeSourceResult.findAllTextNode(includeDesc: Boolean = false): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    nodes.forEach { node ->
        if (!node.text.isNullOrBlank()) {
            result.nodes.add(node)
            return@forEach
        }
        if (includeDesc && !node.description.isNullOrBlank()) {
            result.nodes.add(node)
            return@forEach
        }
    }
    return result
}


/**
 * 打印出所有的结点，打印出有用的特征点
 * */
fun AccessibilityNodeInfo?.printAllNode(level: Int = 0) {
    if (this == null) return
    logD(StringBuilder().apply {
        repeat(level) { append(" ") }
        val bounds = Rect()
        this@printAllNode.getBoundsInScreen(bounds)
        append("${this@printAllNode.className}")
        this@printAllNode.viewIdResourceName.expressionResult(
            { it.isNotBlank() },
            { append(" → $it") })
        this@printAllNode.text.expressionResult({ it.isNotBlank() }, { append(" → $it") })
        this@printAllNode.contentDescription.expressionResult(
            { it.isNotBlank() },
            { append(" → $it") })
        append(" → $bounds")
        this@printAllNode.isClickable.expressionResult({ it }, { append(" → 可点击") })
        this@printAllNode.isScrollable.expressionResult({ it }, { append(" → 可滚动") })
        this@printAllNode.isEditable.expressionResult({ it }, { append(" → 可编辑") })
    }.toString())
    if (this.childCount > 0) {
        for (index in 0 until this.childCount) this.getChild(index).printAllNode(level + 1)
    }
}


