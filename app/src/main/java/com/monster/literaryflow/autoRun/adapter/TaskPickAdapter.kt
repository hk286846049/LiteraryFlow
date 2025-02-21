package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AutoInfo

class TaskPickAdapter(private val dataList: List<String>,private val onSelected: (position: Int) -> Unit) : RecyclerView.Adapter<TaskPickAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val layout: RelativeLayout = itemView.findViewById(R.id.layout)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_picker, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTitle.text = dataList[position]
        holder.layout.setOnClickListener {
            onSelected(position)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}
