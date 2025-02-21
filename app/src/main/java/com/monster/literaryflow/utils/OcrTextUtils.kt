package com.monster.literaryflow.utils

import android.graphics.Rect
import com.benjaminwan.ocrlibrary.Point
import com.benjaminwan.ocrlibrary.TextBlock

object OcrTextUtils {
    fun findTextCenter(textBlock: TextBlock):Pair<Int,Int>{
        val maxX = textBlock.boxPoint.maxBy { it.x }
        val maxY = textBlock.boxPoint.maxBy { it.y }
        val minX = textBlock.boxPoint.minBy { it.x }
        val minY = textBlock.boxPoint.minBy { it.y }
        return Pair((maxX.x + minX.x) / 2, (maxY.y + minY.y) / 2)
    }
    fun pointsToRect(points: ArrayList<Point>): Rect {
        if (points.isEmpty()) {
            return Rect()
        }

        var left = points[0].x
        var top = points[0].y
        var right = points[0].x
        var bottom = points[0].y

        // 遍历所有点，更新 left, top, right, bottom
        for (point in points) {
            if (point.x < left) left = point.x
            if (point.x > right) right = point.x
            if (point.y < top) top = point.y
            if (point.y > bottom) bottom = point.y
        }

        return Rect(left, top, right, bottom)
    }
}