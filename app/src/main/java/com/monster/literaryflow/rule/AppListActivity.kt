package com.monster.literaryflow.rule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AppData
import com.monster.literaryflow.databinding.ActivityAppListBinding
import com.monster.literaryflow.utils.AppUtils


class AppListActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAppListBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val apps = AppUtils.getAllInstalledApps(this@AppListActivity)
        binding.mRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.mRecyclerView.adapter =AppAdapter(apps)
    }
}
class AppAdapter(apps: List<AppData>) :
    RecyclerView.Adapter<AppAdapter.MyViewHolder?>() {
    private val apps: List<AppData>

    class MyViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var appIcon: ImageView
        var appName: TextView

        init {
            appIcon = v.findViewById<ImageView>(R.id.iv_icon)
            appName = v.findViewById<TextView>(R.id.tv_name)
        }
    }

    init {
        this.apps = apps
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v: View =
            LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val app: AppData = apps[position]
        holder.appName.text = app.appName
        Glide.with(holder.itemView.context)
            .load(app.appIcon)
            .into(holder.appIcon)
    }

    override fun getItemCount(): Int {
        return apps.size
    }
}
