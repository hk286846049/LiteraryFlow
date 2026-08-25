package com.monster.literaryflow.autoRun.autoInterface

import com.monster.literaryflow.bean.TriggerBean

interface IMonitorInterface {
    suspend fun monitor(triggerBean: TriggerBean)
}