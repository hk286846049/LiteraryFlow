package com.monster.literaryflow

import android.annotation.SuppressLint
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

class MyAccessibilityService : FastAccessibilityService(), ServiceInterface {
    companion object {
        @SuppressLint("WrongConstant")
        suspend fun clickNode(ocrBlock: ArrayList<TextBlock>) {
            ocrBlock.forEach { block ->
                Log.d("MyAccessibilityService", "TextBlock: ${block.text}")
            }
        }

        var sourceResult: AnalyzeSourceResult? = null

        var tPackageName: String? = null
    }

    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        tPackageName = wrapper?.className
        sourceResult = result
        Log.d("MyAccessibilityService", "result：${result.toString()}")
        if (TextFloatingWindowService.isRunning) {
            if (sourceResult?.nodes != null) {
                val list = sourceResult?.nodes!!.filter {
                    it.text != null && it.text != ""
                            && it.bounds!!.left >= 0 && it.bounds!!.top >= 0 && it.bounds!!.right >= 0 && it.bounds!!.bottom >= 0
                }
                val listB: List<TextData> = list.map { a ->
                    TextData(
                        text = a.text!!,
                        bounds = a.bounds!!
                    )
                }
                /*
                                TextFloatingWindowService.instance?.let { service ->
                                    Handler(Looper.getMainLooper()).post {
                                        service.updateView(listB)
                                    }
                                }
                */
            }


        }
    }

    override fun noAnalyzeCallBack(wrapper: EventWrapper?, node: AccessibilityNodeInfo?) {
        wrapper?.let { logD(it.toString()) }
    }

    override fun findText(text: String): Pair<Boolean, ArrayList<NodeWrapper>?> {
        return if (sourceResult != null) {
            val nodeInfo = sourceResult!!.nodes.find { it.text?.contains(text) == true }
            Pair(true, sourceResult?.nodes)
        } else {
            Pair(false, null)
        }

    }

    override fun clickText(text: String, type: TextPickType): Boolean {
        return if (sourceResult != null) {
            val nodeInfo = when (type) {
                TextPickType.EXACT_MATCH -> sourceResult!!.nodes.find { it.text == text }
                TextPickType.FUZZY_MATCH -> sourceResult!!.nodes.find { it.text!!.contains(text) }
                TextPickType.MULTIPLE_FUZZY_WORDS -> {
                    val texts = text.split("#")
                    sourceResult!!.nodes.find { node ->
                        texts.any { fuzzyText ->
                            node.text!!.contains(fuzzyText, ignoreCase = true)
                        }
                    }
                }
            }
            if (nodeInfo != null) {
                if (nodeInfo.nodeInfo != null) {
                    click(nodeInfo.bounds?.centerX()!!, nodeInfo.bounds?.centerY()!!)
                    true
                } else {
                    false
                }
            } else {
                false
            }
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

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val queue: Queue<AccessibilityNodeInfo> = LinkedList()
        queue.add(node)

        while (queue.isNotEmpty()) {
            val currentNode = queue.poll()

            if (currentNode.text?.toString()?.contains(text, true) == true) {
                return currentNode
            }

            for (i in 0 until currentNode.childCount) {
                currentNode.getChild(i)?.let { queue.add(it) }
            }
        }

        return null
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
}
