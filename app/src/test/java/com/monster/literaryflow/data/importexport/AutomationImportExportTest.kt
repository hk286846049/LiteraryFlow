package com.monster.literaryflow.data.importexport

import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.result.FlowResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationImportExportTest {
    @Test
    fun importsLegacyCamelCaseArray() {
        val json = """
            [
              {
                "id": 7,
                "title": "每日签到",
                "runState": true,
                "runAppName": "测试游戏",
                "runPackageName": "com.example.game",
                "runTimes": 2,
                "loopType": "DAY_LOOP",
                "runTime": {"first": {"first": 8, "second": 30}, "second": {"first": 23, "second": 0}},
                "runInfo": [
                  {
                    "clickBean": {
                      "clickType": "CLICK_TEXT",
                      "text": "签到",
                      "findTextType": "FUZZY_MATCH",
                      "isFindText4Node": false,
                      "findTextTime": 5,
                      "loopTimes": 2
                    }
                  }
                ]
              }
            ]
        """.trimIndent()

        val result = AutomationImportExport.importJson(json)

        assertTrue(result is FlowResult.Success)
        val report = (result as FlowResult.Success).value
        val task = report.document.tasks.single()
        val action = task.steps.single().actions.single()
        assertEquals(ImportSource.LEGACY_AUTO_INFO, report.source)
        assertEquals("每日签到", task.title)
        assertEquals(ScheduleType.DAILY, task.scheduleType)
        assertEquals("08:30", task.startTime)
        assertEquals(ActionType.TAP_TEXT, action.type)
        assertEquals("签到", action.text)
        assertEquals(MatchType.FUZZY, action.matchType)
        assertEquals(RecognitionSource.OCR, action.recognitionSource)
        assertEquals(2, action.repeatCount)
    }

    @Test
    fun importsLegacyDatabaseStyleObjectWithJsonStrings() {
        val runInfo = """
            [
              {
                "click_bean": {
                  "click_type": "CLICK_XY",
                  "click_xy": {"first": 540, "second": 960},
                  "find_text_time": 3,
                  "sleep_time": 1
                }
              },
              {
                "trigger_bean": [
                  {
                    "trigger_type": "FIND_TEXT",
                    "find_text": "领取",
                    "find_text_type": "EXACT_MATCH",
                    "is_find_text4_node": true,
                    "run_scan_time": 6
                  }
                ]
              }
            ]
        """.trimIndent().replace("\"", "\\\"").replace("\n", "")
        val json = """
            {
              "id": 9,
              "title": "旧库任务",
              "run_state": true,
              "run_app_name": "广告 App",
              "run_package_name": "com.example.ad",
              "run_times": 1,
              "loop_type": "LOOP",
              "sleep_time": 30,
              "week_data": "[1,2,3]",
              "run_info": "$runInfo"
            }
        """.trimIndent()

        val result = AutomationImportExport.importJson(json)

        assertTrue(result is FlowResult.Success)
        val task = (result as FlowResult.Success).value.document.tasks.single()
        val coordinateAction = task.steps[0].actions.single()
        val textCondition = task.steps[1].conditions.single()
        assertEquals("com.example.ad", task.targetPackageName)
        assertEquals(ScheduleType.LOOP, task.scheduleType)
        assertEquals(30_000L, task.intervalMs)
        assertEquals(ActionType.TAP_COORDINATE, coordinateAction.type)
        assertEquals(0.5f, coordinateAction.x ?: -1f, 0.0001f)
        assertEquals(0.5f, coordinateAction.y ?: -1f, 0.0001f)
        assertEquals("领取", textCondition.text)
        assertEquals(RecognitionSource.ACCESSIBILITY, textCondition.recognitionSource)
    }

    @Test
    fun importsNewSchemaWithoutLegacyWarnings() {
        val json = """
            {
              "schemaVersion": 2,
              "exportedAt": 1,
              "tasks": []
            }
        """.trimIndent()

        val result = AutomationImportExport.importJson(json)

        assertTrue(result is FlowResult.Success)
        val report = (result as FlowResult.Success).value
        assertEquals(ImportSource.NEW_SCHEMA, report.source)
        assertEquals(0, report.convertedTaskCount)
        assertTrue(report.warnings.isEmpty())
    }
}
