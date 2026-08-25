package com.monster.literaryflow.helper

import com.monster.literaryflow.service.CaptureService
import java.lang.ref.WeakReference

object CaptureManager {
    private var serviceRef: WeakReference<CaptureService>? = null

    fun registerService(service: CaptureService) {
        serviceRef = WeakReference(service)
    }

    private suspend fun requireService(): CaptureService {
        return serviceRef?.get() ?: throw IllegalStateException("CaptureService not registered")
    }

    suspend fun <R> withService(block: suspend (CaptureService) -> R): R {
        return block(requireService())
    }
}