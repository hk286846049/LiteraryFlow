package com.monster.literaryflow.autoRun.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.monster.literaryflow.databinding.ItemFloatAppBinding
import com.monster.literaryflow.utils.AppUtils

class FloatAppAdapter (private val context: Context, private var appList:List<String>, private val itemSelect:(Int) -> Unit): RecyclerView.Adapter<FloatAppAdapter.FloatTabHolder>() {
    var currentPosition = 0

    fun updateAppList(newList: List<String>) {
        appList = newList
        notifyDataSetChanged()
    }
    fun getList(): List<String> {
        return appList
    }
    inner class FloatTabHolder(private val holder: ItemFloatAppBinding) : RecyclerView.ViewHolder(holder.root) {
        fun bind(packageName: String, position: Int) {
            val appIcon = AppUtils.getAppIcon(context, packageName)
            // 选中的icon放大
            if (position == currentPosition) {
                // 使用ObjectAnimator或ScaleAnimation来放大选中的图标
                holder.ivIcon.scaleX = 1f  // X轴放大1.2倍
                holder.ivIcon.scaleY = 1f  // Y轴放大1.2倍
                Glide.with(context)
                    .load(appIcon)
                    .transform(CircleCrop())
                    .into(holder.ivIcon)
            } else {
                // 非选中的图标保持原始大小
                holder.ivIcon.scaleX = 0.7f
                holder.ivIcon.scaleY = 0.7f
                Glide.with(context)
                    .load(appIcon)
                    .transform(CircleCrop())
                    .into(holder.ivIcon)
            }

            holder.ivIcon.setOnClickListener {
                if (currentPosition == position) {
                    return@setOnClickListener
                }
                currentPosition = position
                itemSelect(position)
                notifyDataSetChanged()
            }

        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloatTabHolder {
        return FloatTabHolder(ItemFloatAppBinding.inflate( LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: FloatTabHolder, position: Int) {
        holder.bind(appList[position], position)
    }


    override fun getItemCount(): Int {
        return appList.size
    }

}
