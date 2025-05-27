package com.monster.literaryflow.autoRun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TriggerBean

class MonitorAdapter(private var dataList: MutableList<TriggerBean>, private val onSelected: (triggerBean: Pair<TriggerBean,Int>) -> Unit) : RecyclerView.Adapter<MonitorAdapter.ViewHolder>() {
    private lateinit var listener: AutoListListener

    fun setListener(listener: AutoListListener){
        this.listener = listener
    }
    fun setList(dataList: MutableList<TriggerBean>){
        this.dataList = dataList
        notifyDataSetChanged()
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
        val layout: LinearLayout = itemView.findViewById(R.id.layout)
        val tvTask: TextView = itemView.findViewById(R.id.tv_trigger)
        val tvTrueRun: TextView = itemView.findViewById(R.id.tv_true_run)
        val ivInsert: ImageView = itemView.findViewById(R.id.iv_insert)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitor, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.layout.setOnClickListener {
            onSelected(Pair(dataList[position],position))
        }
        val triggerBean = dataList[position]
        val triggerStr = if (triggerBean.triggerType == RuleType.TIME){
            "【监听】时间区间[${triggerBean.runTime?.first?.first}:${triggerBean.runTime?.first?.second}-${triggerBean.runTime?.second?.first}:${triggerBean.runTime?.second?.second}]"
        }else{
            "【监听】出现文字[${triggerBean.findText}]"
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
                RunType.OPEN_APP ->"打开APP[${clickBean.openAppData?.first}]"
                RunType.LONG_CLICK -> "长按${clickBean.longClickTime}秒"
                RunType.GO_BACK ->"侧滑返回"
                else -> "未知任务类型"
            }
            "【TRUE】执行操作 $taskStr"
        }else{
            "【TRUE】无操作"
        }
        holder.tvTask.text = triggerStr
        holder.tvTrueRun.text = trueText
        holder.ivDelete.setOnClickListener {
            dataList.removeAt(position)
            listener.onItemDelete(position)
            notifyDataSetChanged()
        }
        holder.ivInsert.setOnClickListener {
            listener.onInsert(position)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}