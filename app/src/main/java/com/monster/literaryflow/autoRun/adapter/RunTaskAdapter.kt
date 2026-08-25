package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AutoInfo

class RunTaskAdapter(private val dataList: List<AutoInfo>) : RecyclerView.Adapter<RunTaskAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val floatSwitch: Switch = itemView.findViewById(R.id.float_switch)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_run_task, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.floatSwitch.text = dataList[position].title
        holder.floatSwitch.isChecked = dataList[position].runState
        holder.floatSwitch.setOnCheckedChangeListener { _, isChecked ->
            dataList[position].runState = isChecked
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}
