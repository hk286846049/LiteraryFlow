package com.monster.literaryflow.autoRun.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.R
import com.monster.literaryflow.autoRun.adapter.AutoAdapter
import com.monster.literaryflow.autoRun.adapter.AutoAppAdapter
import com.monster.literaryflow.autoRun.adapter.AutoListListener
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.databinding.ActivityAutoListBinding
import com.monster.literaryflow.room.AppDatabase
import com.monster.literaryflow.room.AutoInfoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AutoListActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAutoListBinding.inflate(layoutInflater) }
    private lateinit var database: AppDatabase
    private lateinit var autoInfoDao: AutoInfoDao

    // 使用新的 ActivityResult API 定义导入与导出回调
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Uri?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 初始化数据库
        database = AppDatabase.getDatabase(this)
        autoInfoDao = database.autoInfoDao()

        // 设置 RecyclerView 布局管理器
        binding.mRecyclerView.layoutManager = LinearLayoutManager(this)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.recyclerViewApp.layoutManager = layoutManager

        // 返回与新增按钮监听
        binding.btBack.setOnClickListener { finish() }
        binding.btAdd.setOnClickListener {
            startActivity(Intent(this, AddAutoActivity::class.java))
        }

        // 注册导入 JSON 文件的 ActivityResult 回调
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    importDataFromUri(it)
                }
            }
        }

        // 注册导出目录选择的 ActivityResult 回调
        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri: Uri? ->
            treeUri?.let { uri ->
                // 持久化 URI 权限
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val pickedDir = DocumentFile.fromTreeUri(this, uri)
                if (pickedDir == null || !pickedDir.canWrite()) {
                    Log.e(TAG, "无法写入该目录")
                    return@registerForActivityResult
                }
                // 在 IO 线程中进行文件操作
                lifecycleScope.launch(Dispatchers.IO) {
                    // 若目标文件已存在，则删除
                    pickedDir.findFile("1A_自动任务备份.json")?.delete()
                    val newFile = pickedDir.createFile("application/json", "1A_自动任务备份.json")
                    if (newFile != null) {
                        val autoInfoList = autoInfoDao.getAll()
                        val json = Gson().toJson(autoInfoList)
                        contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AutoListActivity, "导出成功", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e(TAG, "创建文件失败")
                    }
                }
            }
        }

        // 点击“导入”按钮，打开 JSON 文件选择器
        binding.tvInto.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/json"))
        }
        // 点击“导出”按钮，打开目录选择器
        binding.tvDerive.setOnClickListener {
            folderPickerLauncher.launch(null)
        }
    }

    override fun onResume() {
        super.onResume()
        // 从数据库中加载数据并刷新列表
        lifecycleScope.launch(Dispatchers.IO) {
            val autoInfoList = autoInfoDao.getAll()
            withContext(Dispatchers.Main) {
                loadList(autoInfoList.toMutableList())
            }
        }
    }

    /**
     * 更新 RecyclerView 数据
     */
    private fun loadList(autoInfoList: MutableList<AutoInfo>) {
        val appList = autoInfoList.distinctBy { it.runPackageName }
        val autoList = autoInfoList.filter { it.runPackageName == appList[0].runPackageName }
        val autoAdapter = AutoAdapter(autoList.toMutableList()) { autoInfo ->
            val intent = Intent(this, AddAutoActivity::class.java).apply {
                putExtra("autoInfo", autoInfo)
            }
            startActivity(intent)
        }
        autoAdapter.setListener(object : AutoListListener {
            override fun onItemClick(position: Int) {
            }

            override fun onItemDelete(position: Int) {
                lifecycleScope.launch(Dispatchers.IO) {
                    autoInfoDao.delete(autoAdapter.getList()[position])
                    withContext(Dispatchers.Main) {
                        MyApp.isUpdateData.value = true
                        autoAdapter.getList().removeAt(position)
                        autoAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onStateChange(position: Int) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val autoInfo = autoAdapter.getList()[position]
                    autoInfo.runState = !autoInfo.runState
                    autoInfoDao.update(autoInfo)
                    withContext(Dispatchers.Main) {
                        MyApp.isUpdateData.value = true
                    }
                }
            }
        })
        val autoAppAdapter = AutoAppAdapter(this,appList){ position ->
            val autoList =  autoInfoList.filter { it.runPackageName == appList[position].runPackageName }
            autoAdapter.update(autoList.toMutableList())
        }
        binding.recyclerViewApp.adapter = autoAppAdapter
        binding.mRecyclerView.adapter = autoAdapter
    }

    /**
     * 从指定 URI 导入数据（后台线程中执行）
     */
    private suspend fun importDataFromUri(uri: Uri) {
        val json = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: return
        val type = object : TypeToken<List<AutoInfo>>() {}.type
        val autoInfoList: List<AutoInfo> = Gson().fromJson(json, type)
        withContext(Dispatchers.Main) {
            loadList(autoInfoList.toMutableList())
        }
        // 将数据写入数据库
        autoInfoList.forEach {
            autoInfoDao.insert(it)
        }
    }

    /**
     * 旧版导出方法（不建议直接调用，仅做参考），对文件目录做了版本适配
     */
    private fun saveAutoInfoListToFile() {
        // 注意：该方法应在后台线程中调用
        val autoInfoList = autoInfoDao.getAll()
        val json = Gson().toJson(autoInfoList)
        // Android 10 及以上使用 app 内部目录，否则使用外部存储根目录
        val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(getExternalFilesDir(null), "互传")
        } else {
            File(Environment.getExternalStorageDirectory(), "互传")
        }
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "1A_自动任务备份.json")
        file.writeText(json)
    }

    /**
     * 旧版导入方法（不建议直接调用，仅作参考），对文件目录做了版本适配
     */
    private fun loadAutoInfoListFromFile(): List<AutoInfo>? {
        val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(getExternalFilesDir(null), "互传")
        } else {
            File(Environment.getExternalStorageDirectory(), "互传")
        }
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "1A_自动任务备份.json")
        if (!file.exists()) return null
        val json = file.readText()
        val type = object : TypeToken<List<AutoInfo>>() {}.type
        return Gson().fromJson(json, type)
    }

    companion object {
        private const val TAG = "AutoListActivity"
    }
}
