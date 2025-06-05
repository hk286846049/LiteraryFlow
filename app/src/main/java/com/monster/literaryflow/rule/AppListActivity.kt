package com.monster.literaryflow.rule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.monster.literaryflow.R
import com.monster.literaryflow.bean.AppData
import com.monster.literaryflow.databinding.ActivityAppListBinding
import com.monster.literaryflow.databinding.AppItemBinding
import com.monster.literaryflow.utils.AppUtils
import taylor.com.util.Preference

class AppListActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAppListBinding.inflate(layoutInflater) }
    private lateinit var appAdapter: AppAdapter
    private lateinit var appDataList: MutableList<AppData>
    private lateinit var allAppDataList: List<AppData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // 获取所有安装的应用
        val apps = AppUtils.getAllInstalledApps(this@AppListActivity)

        // 初始化 SharedPreferences 和收藏列表
        val sharedPreferences = getSharedPreferences("appList", MODE_PRIVATE)
        val preferences = Preference(sharedPreferences)
        val collectList: Set<String> = preferences["appList", emptySet()]

        // 初始化横屏设置 SharedPreferences
        val landscapePrefs = getSharedPreferences("landscape_apps", MODE_PRIVATE)
        val landscapePreferences = Preference(landscapePrefs)
        val landscapeApps: Set<String> = landscapePreferences["landscape_apps", emptySet()]

        // 转换为包含收藏状态和横屏状态的列表
        allAppDataList = apps.map { app ->
            AppData(
                appName = app.appName,
                packageName = app.packageName,
                isGame = app.isGame,
                isCollect = collectList.contains(app.appName),
                isLandscape = landscapeApps.contains(app.packageName)
            )
        }.sortedByDescending { it.isCollect }
        appDataList = allAppDataList.toMutableList()

        // 宫格布局
        binding.mRecyclerView.layoutManager = GridLayoutManager(this, 4)
        appAdapter = AppAdapter(this, appDataList, preferences, landscapePreferences) { appInfo ->
            val resultIntent = Intent().apply {
                putExtra("selectedApp", appInfo)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        binding.mRecyclerView.adapter = appAdapter

        // 搜索功能
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAppList(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 返回按钮
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun filterAppList(query: String) {
        val lowerQuery = query.trim().lowercase()
        appDataList.clear()
        if (lowerQuery.isEmpty()) {
            appDataList.addAll(allAppDataList)
        } else {
            appDataList.addAll(allAppDataList.filter {
                it.appName.lowercase().contains(lowerQuery)
            })
        }
        appAdapter.notifyDataSetChanged()
    }
}

class AppAdapter(
    private val context: Context,
    private val apps: MutableList<AppData>,
    private val preferences: Preference,
    private val landscapePreferences: Preference,
    private val onItemClick: (AppData) -> Unit
) : RecyclerView.Adapter<AppAdapter.MyViewHolder>() {

    class MyViewHolder(val binding: AppItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AppItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Log.d("AppAdapter", "Binding item at position: $position")
        val app = apps[position]
        val binding = holder.binding

        // 设置应用名称和图标
        binding.tvName.text = app.appName
        val appIcon = AppUtils.getAppIcon(context, app.packageName)
        Glide.with(holder.itemView.context).load(appIcon).into(binding.ivIcon)

        // 设置收藏图标
        setCollectIcon(binding.ivCollect, app.isCollect)
        // 设置横屏图标
        setOrientationIcon(binding.ivOrientation, app.isLandscape)

        // 收藏点击事件
        binding.ivCollect.setOnClickListener {
            app.isCollect = !app.isCollect
            updateCollectStatus(app.appName, app.isCollect)
            setCollectIcon(binding.ivCollect, app.isCollect)
        }
        // 横竖屏点击事件
        binding.ivOrientation.setOnClickListener {
            app.isLandscape = !app.isLandscape
            updateLandscapeStatus(app.packageName, app.isLandscape)
            setOrientationIcon(binding.ivOrientation, app.isLandscape)
        }
        // 卡片点击事件
        binding.root.setOnClickListener {
            onItemClick(app)
        }
    }

    override fun getItemCount(): Int = apps.size

    private fun setCollectIcon(imageView: ImageView, isCollect: Boolean) {
        val drawableId = if (isCollect) R.drawable.collect else R.drawable.un_collect
        imageView.setImageDrawable(context.getDrawable(drawableId))
    }

    private fun setOrientationIcon(imageView: ImageView, isLandscape: Boolean) {
        val drawableId = if (isLandscape) R.drawable.hor else R.drawable.vor
        imageView.setImageDrawable(context.getDrawable(drawableId))
    }

    private fun updateCollectStatus(appName: String, isCollect: Boolean) {
        val collectList = preferences["appList", emptySet<String>()].toMutableSet()
        if (isCollect) {
            collectList.add(appName)
        } else {
            collectList.remove(appName)
        }
        preferences["appList"] = collectList
    }

    private fun updateLandscapeStatus(packageName: String, isLandscape: Boolean) {
        val landscapeList = landscapePreferences["landscape_apps", emptySet<String>()].toMutableSet()
        if (isLandscape) {
            landscapeList.add(packageName)
        } else {
            landscapeList.remove(packageName)
        }
        landscapePreferences["landscape_apps"] = landscapeList
    }
}