package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AutoInfo

class FloatAdapter (private var dataList: List<AutoInfo>, private val onSwitchChange: (autoInfo:AutoInfo) -> Unit) : RecyclerView.Adapter<FloatAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val btOpen: Switch = itemView.findViewById(R.id.bt_open)
    }

    fun updateList(dataList: List<AutoInfo>){
        this.dataList = dataList
        notifyDataSetChanged()
    }

    fun getList():List<AutoInfo>{
        return dataList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_float_auto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTitle.text ="【${dataList[position].runAppName}】${dataList[position].title}"
        holder.btOpen.isChecked = dataList[position].isRun
        holder.btOpen.setOnCheckedChangeListener { _, isChecked ->
            dataList[position].isRun = isChecked
            onSwitchChange(dataList[position])
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}
