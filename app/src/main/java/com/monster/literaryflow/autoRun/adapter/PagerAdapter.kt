package com.monster.literaryflow.autoRun.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.databinding.ItemPagerBinding

class PagerAdapter (private val context: Context, private var pagerList:MutableList<MutableList<AutoInfo>>, val listener:FloatListListener): RecyclerView.Adapter<PagerAdapter.FloatTabHolder>() {
    fun updateAppList(newList: MutableList<MutableList<AutoInfo>> ) {
        pagerList = newList
        notifyDataSetChanged()
    }
    inner class FloatTabHolder(private val holder: ItemPagerBinding) : RecyclerView.ViewHolder(holder.root) {
        fun bind(autoList: MutableList<AutoInfo>, position: Int) {
            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            holder.rvPager.layoutManager = layoutManager
            val adapter = FloatAdapter(autoList, object : FloatListListener {
                override fun onSwitchChange(autoInfo: AutoInfo) {
                    listener.onSwitchChange(autoInfo)
                }

                override fun onItemLongClick(autoInfo: AutoInfo) {
                    listener.onItemLongClick(autoInfo)
                }

            })
            holder.rvPager.adapter = adapter

        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloatTabHolder {
        val binding = ItemPagerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        return FloatTabHolder(binding)
    }

    override fun onBindViewHolder(holder: FloatTabHolder, position: Int) {
        holder.bind(pagerList[position], position)
    }


    override fun getItemCount(): Int {
        return pagerList.size
    }

}
