package com.monster.literaryflow.autoRun.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.databinding.ItemAutoTriggerBinding

class AutoListAdapter(private var dataList: List<RunBean>,val listener: AutoListListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_TASK = 1 //任务
        const val TYPE_TRIGGER = 2 //条件
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TASK) {
            val view = inflater.inflate(R.layout.item_auto_task, parent, false)
            TaskViewHolder(view)
        } else {
            TriggerViewHolder(ItemAutoTriggerBinding.inflate(inflater, parent, false))
        }
    }

    fun setList(dataList: List<RunBean>) {
        this.dataList = dataList
        Log.d("AutoListAdapter", "setList: dataList size = ${dataList.size}")
        notifyDataSetChanged() 
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataList.getOrNull(position) ?: return
        when (holder) {
            is TaskViewHolder -> holder.bind(item)
            is TriggerViewHolder -> holder.bind(item)
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvTask: TextView = itemView.findViewById(R.id.tv_task)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
        private val ivInsert: ImageView = itemView.findViewById(R.id.iv_insert)
        private val layout: LinearLayout = itemView.findViewById(R.id.layout)

        fun bind(runBean: RunBean) {
            val clickBean = runBean.clickBean ?: return
            val taskStr = when (clickBean.clickType) {
                RunType.CLICK_XY -> "点击 x:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}"
                RunType.LONG_HOR -> "左右滑动 X:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}，MIN:${clickBean.scrollMinMax?.first} MAX:${clickBean.scrollMinMax?.second},持续${clickBean.scrollTime}秒"
                RunType.LONG_VEH -> "上下滑动 X:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}，MIN:${clickBean.scrollMinMax?.first} MAX:${clickBean.scrollMinMax?.second},持续${clickBean.scrollTime}秒"
                RunType.SCROLL_LEFT -> "左滑"
                RunType.SCROLL_RIGHT -> "右滑"
                RunType.SCROLL_TOP -> "上滑"
                RunType.SCROLL_BOTTOM -> "下滑"
                RunType.CLICK_TEXT -> "点击文字：${clickBean.text} 寻找${clickBean.findTextTime}秒"
                RunType.ENTER_TEXT -> "输入文字${clickBean.enterText}"
                RunType.TASK -> "执行任务 ${clickBean.runTask?.first} ${clickBean.runTask?.second?.size}次 "
                else -> "未知任务类型"
            }
            ivDelete.setOnClickListener {
                listener.onItemDelete(position)
            }
            ivInsert.setOnClickListener {
                listener.onInsert(position)
            }
            layout.setOnClickListener {
                listener.onItemClick(position)
            }
            tvTask.text = taskStr
            tvTime.text = "然后等待${clickBean.sleepTime ?: 0}秒，循环${clickBean.loopTimes ?: 1}次"
        }
    }

    inner class TriggerViewHolder(private val holderView: ItemAutoTriggerBinding) : RecyclerView.ViewHolder(holderView.root) {

        fun bind(runBean: RunBean) {
            holderView.ivDelete.setOnClickListener {
                listener.onItemDelete(position)
            }
            holderView.layout.setOnClickListener {
                listener.onItemClick(position)
            }
            val layoutManager = LinearLayoutManager(MyApp.instance)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL
            holderView.recyclerView.layoutManager = layoutManager
            holderView.recyclerView.adapter = TriggerListAdapter(runBean.triggerBean?: mutableListOf(),object :TriggerListAdapter.TriggerListListener{
                override fun onItemClick(triggerPosition: Int) {
                    listener.onTriggerItemClick(position,triggerPosition)
                }

                override fun addItem() {
                    listener.addItem(position,runBean.triggerBean?.size?:0)
                }

            })
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (dataList.getOrNull(position)?.clickBean != null) {
            TYPE_TASK
        } else {
            TYPE_TRIGGER
        }
    }

    override fun getItemCount(): Int = dataList.size
}
interface AutoListListener{
    fun onItemClick(position: Int)
    fun onItemDelete(position: Int)
    fun onStateChange(position: Int)
    fun onInsert(position: Int)
    fun addItem(position: Int,triggerPosition: Int){}
    fun onTriggerItemClick(position: Int,triggerPosition: Int){}

}
