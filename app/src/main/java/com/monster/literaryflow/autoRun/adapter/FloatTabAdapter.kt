package com.monster.literaryflow.autoRun.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.monster.literaryflow.R
import com.monster.literaryflow.databinding.ItemFloatTabBinding

class FloatTabAdapter(private val tabList:MutableList<String>,private val itemSelect:(Int) -> Unit): RecyclerView.Adapter<FloatTabAdapter.FloatTabHolder>() {

    var currentPosition = 0

    inner class FloatTabHolder(private val holderView: ItemFloatTabBinding) : RecyclerView.ViewHolder(holderView.root) {
        fun bind(item: String, position: Int) {
            Log.d("FloatTabAdapter", "position: $position")
            holderView.tvName.text = item
            if (currentPosition == position){
                holderView.tvName.setTextColor(holderView.tvName.context.getColor(R.color.white))
                holderView.tvName.background = holderView.tvName.context.getDrawable(R.drawable.shape_float_tab_a)
            }else{
                holderView.tvName.setTextColor(holderView.tvName.context.getColor(R.color.black))
                holderView.tvName.background = null
            }
            holderView.tvName.setOnClickListener {
                if (currentPosition == position){
                    return@setOnClickListener
                }
                currentPosition = position
                notifyDataSetChanged()
                itemSelect(position)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloatTabHolder {
        return FloatTabHolder(ItemFloatTabBinding.inflate( LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: FloatTabHolder, position: Int) {
        holder.bind(tabList[position], position)
    }


    override fun getItemCount(): Int {
        return tabList.size
    }

}
