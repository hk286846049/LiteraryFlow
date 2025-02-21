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
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType

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
            val view = inflater.inflate(R.layout.item_auto_trigger, parent, false)
            TriggerViewHolder(view)
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
            layout.setOnClickListener {
                listener.onItemClick(position)
            }
            tvTask.text = taskStr
            tvTime.text = "然后等待${clickBean.sleepTime ?: 0}秒，循环${clickBean.loopTimes ?: 1}次"
        }
    }

    inner class TriggerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvTask: TextView = itemView.findViewById(R.id.tv_trigger)
        private val tvTrueRun: TextView = itemView.findViewById(R.id.tv_true_run)
        private val tvFalseRun: TextView = itemView.findViewById(R.id.tv_false_run)
        private val layout: LinearLayout = itemView.findViewById(R.id.layout)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)

        fun bind(runBean: RunBean) {
            val triggerBean = runBean.triggerBean ?: return
            // 根据需要补充逻辑
            val triggerStr = if (triggerBean.triggerType == RuleType.TIME){
                "【条件】区间[${triggerBean.runTime?.first?.first}:${triggerBean.runTime?.first?.second}-${triggerBean.runTime?.second?.first}:${triggerBean.runTime?.second?.second}]"
            }else{
                "【条件】查找文字[${triggerBean.findText}] ${triggerBean.runScanTime}秒"
            }
            val trueText = if (triggerBean.runTrueAuto!=null){
                "【TRUE】执行任务【${triggerBean.runTrueAuto?.first}】"
            }else if(triggerBean.runTrueTask!=null){
                val clickBean = triggerBean.runTrueTask!!
                val taskStr = when (clickBean.clickType) {
                    RunType.CLICK_XY -> "点击 x:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}"
                    RunType.LONG_HOR -> "左右滑动 X:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}，MIN:${clickBean.scrollMinMax?.first} MAX:${clickBean.scrollMinMax?.second},持续${clickBean.scrollTime}秒"
                    RunType.LONG_VEH -> "上下滑动 X:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}，MIN:${clickBean.scrollMinMax?.first} MAX:${clickBean.scrollMinMax?.second},持续${clickBean.scrollTime}秒"
                    RunType.SCROLL_LEFT -> "左滑"
                    RunType.SCROLL_RIGHT -> "右滑"
                    RunType.SCROLL_TOP -> "上滑"
                    RunType.SCROLL_BOTTOM -> "下滑"
                    RunType.ENTER_TEXT -> "输入文字${clickBean.enterText}"
                    RunType.CLICK_TEXT -> "点击文字：${clickBean.text} 寻找${clickBean.findTextTime}秒"
                    else -> "未知任务类型"
                }
                "【TRUE】执行操作 $taskStr"
            }else{
                "【TRUE】无操作"
            }
            val falseText = if (triggerBean.runFalseAuto!=null){
                "【FALSE】执行任务【${triggerBean.runFalseAuto?.first}】"
            }else if (triggerBean.runFalseTask!=null){
                val clickBean = triggerBean.runFalseTask!!
                val taskStr = when (clickBean.clickType) {
                    RunType.CLICK_XY -> "点击 x:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}"
                    RunType.LONG_HOR -> "左右滑动 X:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}，MIN:${clickBean.scrollMinMax?.first} MAX:${clickBean.scrollMinMax?.second},持续${clickBean.scrollTime}秒"
                    RunType.LONG_VEH -> "上下滑动 X:${clickBean.clickXy?.first} y:${clickBean.clickXy?.second}，MIN:${clickBean.scrollMinMax?.first} MAX:${clickBean.scrollMinMax?.second},持续${clickBean.scrollTime}秒"
                    RunType.SCROLL_LEFT -> "左滑"
                    RunType.SCROLL_RIGHT -> "右滑"
                    RunType.SCROLL_TOP -> "上滑"
                    RunType.SCROLL_BOTTOM -> "下滑"
                    RunType.ENTER_TEXT -> "输入文字${clickBean.enterText}"
                    RunType.CLICK_TEXT -> "点击文字：${clickBean.text} 寻找${clickBean.findTextTime}秒"
                    else -> "未知任务类型"
                }
                "【FALSE】执行操作 $taskStr"
            }else{
                "【FALSE】无操作"
            }
            tvTask.text = triggerStr
            tvTrueRun.text = trueText
            tvFalseRun.text = falseText
            ivDelete.setOnClickListener {
                listener.onItemDelete(position)
            }

            layout.setOnClickListener {
                listener.onItemClick(position)
            }

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

}
