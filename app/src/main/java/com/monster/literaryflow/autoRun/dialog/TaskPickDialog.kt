package com.monster.literaryflow.autoRun.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.autoRun.adapter.TaskPickAdapter

class TaskPickDialog(
    context: Context,
    private val list: MutableList<String>,
    private val onSelected: (position: Int) -> Unit
) : Dialog(context) {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_task_picker)

        window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            setWindowAnimations(R.style.DialogAnimation)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        }

        val adapter = TaskPickAdapter(list) {
            onSelected(it)
            dismiss()
        }
        val layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        recyclerView = findViewById(R.id.mRecyclerView)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }
}
