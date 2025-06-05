package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AutoInfo

class AutoAdapter(
    private var dataList: MutableList<AutoInfo>,
    private val onSelected: (autoInfo: AutoInfo) -> Unit
) : RecyclerView.Adapter<AutoAdapter.ViewHolder>() {
    
    private lateinit var listener: AutoListListener

    fun update(dataList: MutableList<AutoInfo>) {
        this.dataList = dataList
        notifyDataSetChanged()
    }

    fun getList(): MutableList<AutoInfo> {
        return dataList
    }

    fun setListener(listener: AutoListListener) {
        this.listener = listener
    }

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val ivDelete: MaterialButton = itemView.findViewById(R.id.iv_delete)
        val layout: RelativeLayout = itemView.findViewById(R.id.layout)
        val ivState: MaterialButton = itemView.findViewById(R.id.iv_state)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auto_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        
        // 设置标题
        holder.tvTitle.text = item.title
        
        // 设置点击事件
        holder.layout.setOnClickListener {
            onSelected(item)
        }
        
        // 设置删除按钮
        holder.ivDelete.setOnClickListener {
            listener.onItemDelete(position)
        }
        
        // 设置状态按钮
        if (!item.runState) {
            holder.ivState.setIconResource(R.drawable.play)
            holder.ivState.iconTint = MyApp.instance.getColorStateList(R.color.material_green_500)
        } else {
            holder.ivState.setIconResource(R.drawable.stop)
            holder.ivState.iconTint = MyApp.instance.getColorStateList(R.color.material_red_500)
        }
        
        holder.ivState.setOnClickListener {
            listener.onStateChange(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = dataList.size
}