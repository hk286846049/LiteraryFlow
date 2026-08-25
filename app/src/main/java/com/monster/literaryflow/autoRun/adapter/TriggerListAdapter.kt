package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.databinding.ItemTriggerAddBinding
import com.monster.literaryflow.databinding.ItemTriggerListBinding

class TriggerListAdapter(
    private val triggerBean: MutableList<TriggerBean>,
    val listener: TriggerListListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_ADD = 1 //添加模式
        const val TYPE_NORMAL = 2 //常规模式
    }
    inner class TriggerViewHolder(private val holderView: ItemTriggerListBinding) :
        RecyclerView.ViewHolder(holderView.root) {


        fun bind(item: TriggerBean, position: Int) {

            // 根据需要补充逻辑
            val triggerStr = if (item.triggerType == RuleType.TIME){
                "【条件】区间[${item.runTime?.first?.first}:${item.runTime?.first?.second}-${item.runTime?.second?.first}:${item.runTime?.second?.second}]"
            }else{
                "【条件】查找文字[${item.findText}] ${item.runScanTime}秒"
            }
            val trueText = if (item.runTrueAuto != null){
                "【TRUE】执行任务【${item.runTrueAuto?.first}】"
            }else if(item.runTrueTask != null){
                val clickBean = item.runTrueTask!!
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
            val falseText = if (item.runFalseAuto != null){
                "【FALSE】执行任务【${item.runFalseAuto?.first}】"
            }else if (item.runFalseTask != null){
                val clickBean = item.runFalseTask!!
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
            holderView.tvTriggerName.text = triggerStr
            holderView.tvTrue.text = trueText
            holderView.tvFalse.text = falseText
            holderView.layout.setOnClickListener{
                listener.onItemClick(position)
            }
        }

    }

    inner class AddViewHolder(private val holderView: ItemTriggerAddBinding) :   RecyclerView.ViewHolder(holderView.root) {
        fun bind() {
            holderView.layout.setOnClickListener {
                listener.addItem()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD) {
            AddViewHolder(ItemTriggerAddBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            TriggerViewHolder(ItemTriggerListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TriggerViewHolder) {
            val triggerBean: TriggerBean = triggerBean[position]
            holder.bind(triggerBean, position)
        } else if (holder is AddViewHolder) {
            holder.bind()
        }
    }

    override fun getItemCount(): Int {
        return triggerBean.size + 1
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == (itemCount - 1)) TYPE_ADD else TYPE_NORMAL
    }

    interface TriggerListListener {
        fun onItemClick(position: Int)
        fun addItem()
    }

}
