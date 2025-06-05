package com.monster.literaryflow.autoRun.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.utils.AppUtils
class AutoAppAdapter(
    val context: Context,
    private val dataList: List<AutoInfo>,
    private var selectPos: Int = 0,
    private val onItemSelected: (position: Int) -> Unit
) : RecyclerView.Adapter<AutoAppAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivApp: ImageView = itemView.findViewById(R.id.iv_app)
    }

    fun setSelectPos(position: Int){
        this.selectPos = position
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auto_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appIcon = AppUtils.getAppIcon(context, dataList[position].runPackageName!!)

        // 选中的icon放大
        if (position == selectPos) {
            // 使用ObjectAnimator或ScaleAnimation来放大选中的图标
            holder.ivApp.scaleX = 1f  // X轴放大1.2倍
            holder.ivApp.scaleY = 1f  // Y轴放大1.2倍
            Glide.with(context)
                .load(appIcon)
                .transform(CircleCrop())
                .into(holder.ivApp)
        } else {
            // 非选中的图标保持原始大小
            holder.ivApp.scaleX = 0.6f
            holder.ivApp.scaleY = 0.6f
            Glide.with(context)
                .load(appIcon)
                .transform(CircleCrop())
                .into(holder.ivApp)
        }

        holder.ivApp.setOnClickListener {
            selectPos = position
            onItemSelected(position)
            notifyDataSetChanged()  // 更新数据，刷新选中项
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}

