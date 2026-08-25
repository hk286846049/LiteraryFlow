package com.monster.literaryflow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)
    }


    fun getLogcatLogs(): String {
        val processBuilder = ProcessBuilder("logcat", "-d")
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        val log = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            log.append(line).append("\n")
        }

        reader.close()
        return log.toString()
    }

}