package com.monster.literaryflow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.monster.literaryflow.databinding.ActivityLogBinding
import com.monster.literaryflow.utils.SPUtils

class LogActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLogBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val gson = Gson()
        val novelist= SPUtils[this, "orderList", ""] ?:""
        if (novelist!=""){
            val type = object : TypeToken<MutableList<String>>() {}.type
            val list:MutableList<String> = gson.fromJson(novelist as String, type) ?: mutableListOf()
            val adapter = SimpleAdapter(list)
            binding.mRecyclerView.adapter = adapter
            binding.mRecyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        }
    }
}
class SimpleAdapter(private val dataList: MutableList<String>) :
    RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]
        holder.bind(data)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textView)
        fun bind(data: String) {
            textView.text = data
        }
    }
}