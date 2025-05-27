package com.monster.literaryflow

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import cn.coderpig.cp_fast_accessibility.*
import com.benjaminwan.ocrlibrary.TextBlock
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.rule.ui.TextData
import com.monster.literaryflow.service.TextFloatingWindowService
import com.monster.literaryflow.serviceInterface.ServiceInterface
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MyAccessibilityService : FastAccessibilityService(), ServiceInterface {
    companion object {
        @SuppressLint("WrongConstant")

        var sourceResult: AnalyzeSourceResult? = null

        var tPackageName: String? = null
    }

    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        tPackageName = wrapper?.className
        Log.d("MyAccessibilityService", "analyzeCallBack result：【${result.nodes}】")
        sourceResult = result


    }

    var fetchHandler: Handler? = null
    var fetchRunnable: Runnable? = null
    var fetchScheduler: ScheduledExecutorService? = null
    private val activeTasks = ConcurrentHashMap<String, TaskContext>()

    data class TaskContext(
        val taskId: String,
        val isRunning: AtomicBoolean = AtomicBoolean(true),
        var fetchHandler: Handler? = null,
        var fetchRunnable: Runnable? = null,
        var fetchScheduler: ScheduledExecutorService? = null
    )
    /**
     * 开始获取节点信息
     * @param interval 间隔时间(ms)，<=0 表示尽可能快连续获取
     * @param findText   查找条件
     * @param timeoutMillis 超时时间(ms)，null 表示不超时
     */
    fun startFetchingNodes(
        taskId: String = UUID.randomUUID().toString(),
        interval: Long = 0,
        findText: Pair<String, TextPickType>? = null,
        timeoutMillis: Long? = null,
        isUpdateWindow: Boolean = false
    ): TextData? {
        if (activeTasks.containsKey(taskId)) {
            Log.w("MyAccessibilityService", "Task $taskId is already running")
            return null
        }
        val context = TaskContext(taskId)
        activeTasks[taskId] = context
        Log.d("MyAccessibilityService", "startFetchingNodes")
        val latch = CountDownLatch(1)
        var foundNode: TextData? = null
        val start = System.currentTimeMillis()

        if (interval <= 0) {
            context.fetchHandler = Handler(Looper.getMainLooper())
            context.fetchRunnable = object : Runnable {
                override fun run() {
                    if (!context.isRunning.get()) return
                    val node = getAllVisibleText(findText,isUpdateWindow)
                    if (node != null) {
                        foundNode = node
                        stopFetchingNodes(taskId)
                        latch.countDown()
                    } else if (timeoutMillis != null && System.currentTimeMillis() - start >= timeoutMillis) {
                        stopFetchingNodes(taskId)
                        latch.countDown()
                    } else {
                        context.fetchHandler?.post(this)
                    }
                }
            }
            context.fetchHandler?.post(context.fetchRunnable!!)
        } else {
            // 定时获取
            context.fetchScheduler = Executors.newSingleThreadScheduledExecutor().apply {
                scheduleWithFixedDelay({
                    if (!context.isRunning.get()) return@scheduleWithFixedDelay
                    val node = getAllVisibleText(findText, isUpdateWindow)
                    if (node != null) {
                        foundNode = node
                        stopFetchingNodes(taskId)
                        latch.countDown()
                    } else if (timeoutMillis != null && System.currentTimeMillis() - start >= timeoutMillis) {
                        stopFetchingNodes(taskId)
                        latch.countDown()
                    }
                }, 0, interval, TimeUnit.MILLISECONDS)
            }
        }

        if (findText != null) {
            try {
                if (timeoutMillis != null) {
                    latch.await(timeoutMillis, TimeUnit.MILLISECONDS)
                } else {
                    latch.await()
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        } else {
            if (!isUpdateWindow) {
                Log.d("MyAccessibilityService", "startFetchingNodes 没有findText")
                stopFetchingNodes(taskId)
            }
        }
        return foundNode
    }


    private fun getAllVisibleText(
        findText: Pair<String, TextPickType>? = null,
        isUpdateWindow: Boolean = false
    ): TextData? {

        var textList = listOf<TextData>()
        AnalyzeSourceResult(arrayListOf()).let { result ->
            analyzeNode(rootInActiveWindow, 0, result.nodes)
             val nodes = result.nodes.filter { it.text!= null && it.text!= "" }

            val list = nodes.filter {
                it.text != null && it.text != ""
                        && it.bounds!!.left >= 0 && it.bounds!!.top >= 0 && it.bounds!!.right >= 0 && it.bounds!!.bottom >= 0 && it.bounds!!.right>it.bounds!!.left
            }
            textList = list.map { a ->
                TextData(
                    text = a.text!!,
                    bounds = a.bounds!!
                )
            }

            if (isUpdateWindow) {
                Log.d("MyAccessibilityService", "updateView：【${textList}】")
                if (textList.isEmpty()) {
                    return null
                }
                TextFloatingWindowService.instance?.let { service ->
                    Handler(Looper.getMainLooper()).post {
                        service.updateView(textList)
                    }
                }
            }

        }

/*
        Log.d("MyAccessibilityService", "getAllVisibleText isUpdateWindow：【${isUpdateWindow}】")
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow

        analyzeNode(rootNode, 0, result.nodes)

        if (rootNode == null) {
            nodeTextList = null
            return null
        }
        val queue: Queue<AccessibilityNodeInfo> = LinkedList()
        queue.add(rootNode)*/
/*
        while (queue.isNotEmpty()) {
            val currentNode = queue.poll()
            // 获取文字内容
            val text = currentNode.text?.toString()?.trim()
            val contentDesc = currentNode.contentDescription?.toString()?.trim()
            val hintText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentNode.hintText?.toString()?.trim()
            } else null

            // 获取坐标
            val bounds = Rect()
            currentNode.getBoundsInScreen(bounds)

            // 添加有效文字信息
            if (!text.isNullOrEmpty()) {
                textList.add(TextData(text, bounds))
            }
            if (!contentDesc.isNullOrEmpty()) {
                textList.add(TextData(contentDesc, bounds))
            }
            if (!hintText.isNullOrEmpty()) {
                textList.add(TextData(hintText, bounds))
            }

            // 添加子节点到队列
            for (i in 0 until currentNode.childCount) {
                currentNode.getChild(i)?.let { queue.add(it) }
            }
        }
*/


        if (findText != null) {
            when (findText.second) {
                TextPickType.EXACT_MATCH -> {
                    return textList.find { it.text == findText.first }
                }

                TextPickType.FUZZY_MATCH -> {
                    return textList.find { it.text.contains(findText.first) }
                }

                TextPickType.MULTIPLE_FUZZY_WORDS -> {
                    val texts = findText.first.split("#")
                    return textList.find { node ->
                        texts.any { fuzzyText ->
                            node.text.contains(fuzzyText, ignoreCase = true)
                        }
                    }
                }
            }
        }
        return null

    }


    /**
     * 停止获取节点信息
     */
    fun stopFetchingNodes(taskId: String) {
        val context = activeTasks[taskId] ?: run {
            Log.w("MyAccessibilityService", "Task $taskId not found")
            return
        }
        context.isRunning.set(false)
        context.fetchHandler?.removeCallbacksAndMessages(null)
        context.fetchScheduler?.shutdownNow()

        activeTasks.remove(taskId)
        Log.d("MyAccessibilityService", "Task $taskId stopped")

    }

    fun stopAllFetchingNodes() {
        activeTasks.forEach { (taskId, context) ->
            context.isRunning.set(false)
            context.fetchHandler?.removeCallbacksAndMessages(null)
            context.fetchScheduler?.shutdownNow()
        }
        activeTasks.clear()
        Log.d("MyAccessibilityService", "All tasks stopped")
    }

    fun isAppForeground(packageName: String): Boolean {
        Log.d("MyAccessibilityService", "runApp：【${packageName}】 current：【${tPackageName}】")
        return packageName == foregroundAppPackageName
    }


    override fun noAnalyzeCallBack(wrapper: EventWrapper?, node: AccessibilityNodeInfo?) {
        wrapper?.let { logD(it.toString()) }
    }


    override fun findText(text: String, type: TextPickType, findTime: Int): Boolean {
        val timeout = findTime * 1000L
        val id ="find_text"+ UUID.randomUUID().toString()
        val found =
            startFetchingNodes(    taskId = id,interval = 200L, findText = text to type, timeoutMillis = timeout)
        stopFetchingNodes(id)
        return found != null
    }

    override fun clickText(text: String, type: TextPickType, findTime: Int): Boolean {
        val timeout = findTime * 1000L
        val id ="click_text"+ UUID.randomUUID().toString()

        val node =
            startFetchingNodes(taskId =id,interval = 200L, findText = text to type, timeoutMillis = timeout)
        stopFetchingNodes(id)
        return if (node != null) {
            click(node.bounds.centerX(), node.bounds.centerY())
            Log.d("MyAccessibilityService", "点击【${text}】：【${node.bounds}】")
            true
        } else {
            false
        }
    }

    override fun enterText(input: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val inputNode = findEditableNode(rootNode) ?: return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, input)
        }
        return inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }


    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue: Queue<AccessibilityNodeInfo> = LinkedList()
        queue.add(node)

        while (queue.isNotEmpty()) {
            val currentNode = queue.poll()

            if (currentNode.isEditable) {
                return currentNode
            }

            for (i in 0 until currentNode.childCount) {
                currentNode.getChild(i)?.let { queue.add(it) }
            }
        }

        return null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Accessibility", "Service connected")
        if (this::class.java == specificServiceClass) instance = this
    }

    override fun onDestroy() {
        Log.d("Accessibility", "Service destroyed")
        super.onDestroy()
    }


    private fun analyzeNode(
        node: AccessibilityNodeInfo?,
        nodeIndex: Int,
        list: ArrayList<NodeWrapper>,
        depth: Int = 0, // 添加递归深度参数
        maxDepth: Int = 50 // 设定最大递归深度
    ) {
        if (node == null || depth > maxDepth) return


        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        list.add(
            NodeWrapper(
                index = nodeIndex,
                text = node.text.blankOrThis(),
                id = node.viewIdResourceName.blankOrThis(),
                bounds = bounds,
                className = node.className.blankOrThis(),
                description = node.contentDescription.blankOrThis(),
                clickable = node.isClickable,
                scrollable = node.isScrollable,
                editable = node.isEditable,
                nodeInfo = node
            )
        )

        if (node.childCount > 0) {
            for (index in 0 until node.childCount) {
                analyzeNode(node.getChild(index), index, list, depth + 1, maxDepth)
            }
        }
    }

}
