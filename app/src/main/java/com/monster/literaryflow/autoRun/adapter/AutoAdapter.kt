package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.utils.AppUtils

class AutoAdapter (private var dataList: MutableList<AutoInfo>,private val onSelected: (autoInfo: AutoInfo) -> Unit) : RecyclerView.Adapter<AutoAdapter.ViewHolder>() {
    private lateinit var listener: AutoListListener


    fun update(dataList: MutableList<AutoInfo>){
        this.dataList = dataList
        notifyDataSetChanged()
    }

    fun getList():MutableList<AutoInfo>{
        return dataList
    }
    fun setListener(listener: AutoListListener){
        this.listener = listener
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
        val layout: RelativeLayout = itemView.findViewById(R.id.layout)
        val ivState: ImageView = itemView.findViewById(R.id.iv_state)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auto_list, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTitle.text ="${dataList[position].title}"
        holder.layout.setOnClickListener {
            onSelected(dataList[position])
        }
        holder.ivDelete.setOnClickListener {
            listener.onItemDelete(position)
        }
        if (!dataList[position].runState){
            holder.ivState.setImageDrawable( MyApp.instance.getDrawable(R.drawable.play))
        }else{
            holder.ivState.setImageDrawable( MyApp.instance.getDrawable(R.drawable.stop))
        }
        holder.ivState.setOnClickListener {
           listener.onStateChange(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}