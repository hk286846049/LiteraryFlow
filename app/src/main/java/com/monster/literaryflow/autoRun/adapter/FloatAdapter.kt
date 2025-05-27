package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.AutoRunType
import com.monster.literaryflow.databinding.ItemAutoOverBinding
import com.monster.literaryflow.databinding.ItemFloatAutoBinding
import com.monster.literaryflow.databinding.ItemFloatListBinding
import com.monster.literaryflow.utils.TimeUtils
class FloatAdapter(
    private var dataList: List<AutoInfo>,
    private val listener: FloatListListener,
    private var isOverMode: Boolean = false // 新增参数，用于区分模式
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FLOAT = 0
        private const val VIEW_TYPE_OVER = 1
    }

    fun updateList(dataList: List<AutoInfo>,isOverMode: Boolean = false) {
        this.dataList = dataList
        this.isOverMode = isOverMode
        notifyDataSetChanged()
    }

    fun getList(): List<AutoInfo> = dataList // 新增方法，用于获取数据列表

    override fun getItemViewType(position: Int): Int {
        return if (isOverMode) VIEW_TYPE_OVER else VIEW_TYPE_FLOAT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_OVER -> {
                val binding = ItemAutoOverBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AutoOverHolder(binding)
            }
            else -> {
                val binding = ItemFloatAutoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FloatAdapterHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FloatAdapterHolder -> holder.bind(dataList[position], position)
            is AutoOverHolder -> holder.bind(dataList[position], position)
        }
    }

    override fun getItemCount(): Int = dataList.size

    // 原 FloatAdapter 的 ViewHolder
    inner class FloatAdapterHolder(private val holder: ItemFloatAutoBinding) :
        RecyclerView.ViewHolder(holder.root) {

        fun bind(data: AutoInfo, position: Int) {
            var tip = ""
            if (data.loopType == AutoRunType.DAY_LOOP) {
                val time = if (TimeUtils.isToday(data.todayRunTime.first)) {
                    data.todayRunTime.second
                } else {
                    0
                }
                tip = "【${time}/${data.runTimes}】"
            }
            holder.tvTitle.text = "${data.title} $tip"
            holder.btOpen.setOnClickListener {
                listener.onSwitchChange(data)
            }
            holder.layout.setOnLongClickListener {
                listener.onItemLongClick(data)
                true
            }
        }
    }

    inner class AutoOverHolder(private val holder: ItemAutoOverBinding) :
        RecyclerView.ViewHolder(holder.root) {

        private var times: Int = 1

        fun bind(data: AutoInfo, position: Int) {
            if (data.loopType == AutoRunType.DAY_LOOP) {
                val time = if (TimeUtils.isToday(data.todayRunTime.first)) {
                    data.todayRunTime.second
                } else {
                    0
                }
            }
            holder.tvTitle.text = "${data.title}"
            times = 1
            holder.tvCount.text = times.toString()

            holder.ivMinus.setOnClickListener {
                if (times > 0) {
                    times--
                    holder.tvCount.text = times.toString()
                }
            }

            holder.ivAdd.setOnClickListener {
                times++
                holder.tvCount.text = times.toString()
            }

            holder.ivRestart.setOnClickListener {
                holder.ivRestart.rotation = 0f
                holder.ivRestart.animate().rotationBy(360f).duration = 2000
                listener.onItemRestart(position,times)
            }

            holder.layout.setOnLongClickListener {
                listener.onItemLongClick(data)
                true
            }
        }
    }
}

interface FloatListListener {
    fun onSwitchChange(autoInfo: AutoInfo)
    fun onItemLongClick(autoInfo: AutoInfo)
    fun onItemRestart(position: Int,times: Int) {} // 默认空实现
}