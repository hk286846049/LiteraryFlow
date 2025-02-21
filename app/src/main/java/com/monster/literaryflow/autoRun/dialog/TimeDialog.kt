package com.monster.literaryflow.autoRun.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.monster.literaryflow.R

class TimeDialog(
    context: Context,
    val startTime: Pair<Int, Int>,
    val endTime: Pair<Int, Int>,
    private val onTimeSelected: (startTime: Pair<Int, Int>, endTime: Pair<Int, Int>) -> Unit
) : Dialog(context) {

    private lateinit var startHourPicker: NumberPicker
    private lateinit var startMinutePicker: NumberPicker
    private lateinit var endHourPicker: NumberPicker
    private lateinit var endMinutePicker: NumberPicker
    private lateinit var btnConfirm: AppCompatButton
    private lateinit var btnCancel: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_timer_picker)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        startHourPicker = findViewById(R.id.start_hour_picker)
        startMinutePicker = findViewById(R.id.start_minute_picker)
        endHourPicker = findViewById(R.id.end_hour_picker)
        endMinutePicker = findViewById(R.id.end_minute_picker)
        btnConfirm = findViewById(R.id.btn_confirm)
        btnCancel = findViewById(R.id.btn_cancel)

        startHourPicker.minValue = 0
        startHourPicker.maxValue = 23
        startMinutePicker.minValue = 0
        startMinutePicker.maxValue = 59
        endHourPicker.minValue = 0
        endHourPicker.maxValue = 23
        endMinutePicker.minValue = 0
        endMinutePicker.maxValue = 59
        startHourPicker.value = startTime.first
        startMinutePicker.value = startTime.second
        endHourPicker.value = endTime.first
        endMinutePicker.value = endTime.second

        btnConfirm.setOnClickListener {
            val startHour = startHourPicker.value
            val startMinute = startMinutePicker.value
            val endHour = endHourPicker.value
            val endMinute = endMinutePicker.value
            onTimeSelected(Pair(startHour,startMinute),Pair(endHour,endMinute))
            dismiss()
        }

        //设置分割线颜色为透明
        for (pickerField in btnConfirm.javaClass.declaredFields) {
            if (pickerField.name.equals("mSelectionDivider")) {
                pickerField.isAccessible = true
                val color = context.getDrawable(R.color.transparent)
                pickerField.set(btnConfirm, color)
                btnConfirm.invalidate()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}