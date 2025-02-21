package com.monster.literaryflow.autoRun

import android.util.Log
import com.benjaminwan.ocrlibrary.TextBlock
import com.monster.literaryflow.bean.TextPickType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

object AutoRunManager {
    private val _bitmapNodesFlow = MutableSharedFlow<List<TextBlock>>(replay = 1)
    val bitmapNodesFlow: SharedFlow<List<TextBlock>> = _bitmapNodesFlow

    suspend fun updateBitmapNodes(newBitmapNodes: List<TextBlock>) {
        _bitmapNodesFlow.emit(newBitmapNodes)
    }

    suspend fun findText(text: String, findType: TextPickType, time: Int): Pair<Boolean, TextBlock?> {
        val result = withTimeoutOrNull(time*1000L) {
            bitmapNodesFlow
                .first { bitmapNodes ->
                    when (findType) {
                        TextPickType.EXACT_MATCH -> bitmapNodes.any { it.text == text }
                        TextPickType.FUZZY_MATCH -> bitmapNodes.any { it.text.contains(text) }
                        TextPickType.MULTIPLE_FUZZY_WORDS -> {
                            val texts = text.split("#")
                            bitmapNodes.any { node ->
                                texts.any { fuzzyText ->
                                    node.text.contains(fuzzyText, ignoreCase = true)
                                }
                            }
                        }
                    }
                }
        }

        return if (result != null) {
            Log.d("AutoRunManager", "result:${result}")
            Pair(true, result.firstOrNull {
                when (findType) {
                    TextPickType.EXACT_MATCH -> it.text == text
                    TextPickType.FUZZY_MATCH -> it.text.contains(text)
                    TextPickType.MULTIPLE_FUZZY_WORDS -> {
                        val texts = text.split("#")
                        texts.any { fuzzyText -> it.text.contains(fuzzyText, ignoreCase = true) }
                    }
                }
            })
        } else {
            Pair(false, null)
        }
    }
}
