package com.monster.literaryflow.rule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AppData
import com.monster.literaryflow.databinding.ActivityAppListBinding
import com.monster.literaryflow.utils.AppUtils
import taylor.com.util.Preference


class AppListActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAppListBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // 获取所有安装的应用
        val apps = AppUtils.getAllInstalledApps(this@AppListActivity)

        // 初始化 SharedPreferences 和收藏列表
        val sharedPreferences = getSharedPreferences("appList", MODE_PRIVATE)
        val preferences = Preference(sharedPreferences)
        val collectList: Set<String> = preferences["appList", emptySet()]
        Log.d("collectAppList","${collectList.size},${collectList}")
        // 转换为包含收藏状态的列表
        val appDataList = apps.map { app ->
            AppData(
                appName = app.appName,
                packageName = app.packageName,
                isGame = app.isGame,
                isCollect = collectList.contains(app.appName)
            )
        }.toMutableList()
        appDataList.sortByDescending { it.isCollect }
        binding.mRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.mRecyclerView.adapter = AppAdapter(this, appDataList, preferences){ appInfo ->
            val resultIntent = Intent().apply {
                putExtra("selectedApp", appInfo)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        binding.btnBack.setOnClickListener { finish() }
    }
}

class AppAdapter(
    private val context: Context,
    private val apps: MutableList<AppData>,
    private val preferences: Preference,
    private val onItemClick: (AppData) -> Unit
) : RecyclerView.Adapter<AppAdapter.MyViewHolder>() {

    class MyViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val appIcon: ImageView = v.findViewById(R.id.iv_icon)
        val appName: TextView = v.findViewById(R.id.tv_name)
        val ivCollect: ImageView = v.findViewById(R.id.iv_collect)
        val layout: ConstraintLayout = v.findViewById(R.id.layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val app = apps[position]

        // 设置应用名称和图标
        if (app.isGame){
            holder.appName.setTextColor(context.getColor(R.color.material_red_700))
        }else{
            holder.appName.setTextColor(context.getColor(R.color.black))
        }
        holder.appName.text = app.appName
        val appIcon = AppUtils.getAppIcon(context, app.packageName)
        Glide.with(holder.itemView.context).load(appIcon).into(holder.appIcon)

        // 设置收藏图标
        setCollectIcon(holder.ivCollect, app.isCollect)

        // 点击事件：切换收藏状态并同步到 SharedPreferences
        holder.ivCollect.setOnClickListener {
            app.isCollect = !app.isCollect
            updateCollectStatus(app.appName, app.isCollect)
            setCollectIcon(holder.ivCollect, app.isCollect)
        }
        holder.layout.setOnClickListener {
            onItemClick(app)
        }
    }

    override fun getItemCount(): Int = apps.size

    private fun setCollectIcon(imageView: ImageView, isCollect: Boolean) {
        val drawableId = if (isCollect) R.drawable.collect else R.drawable.un_collect
        imageView.setImageDrawable(context.getDrawable(drawableId))
    }

    private fun updateCollectStatus(appName: String, isCollect: Boolean) {
        val collectList: MutableSet<String> = preferences["appList", mutableSetOf()]
        if (isCollect) {
            collectList.add(appName)
        } else {
            collectList.remove(appName)
        }
        Log.d("collectAppList","${collectList.size},${collectList}")
        preferences["appList"] = collectList
    }
}
