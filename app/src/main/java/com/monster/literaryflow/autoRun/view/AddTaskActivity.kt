package com.monster.literaryflow.autoRun.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.autoRun.dialog.TaskPickDialog
import com.monster.literaryflow.bean.AppData
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.databinding.ActivityAddTaskBinding
import com.monster.literaryflow.room.AppDatabase
import com.monster.literaryflow.rule.AppListActivity
import com.monster.literaryflow.service.ScreenFloatingWindowsService
import com.monster.literaryflow.service.SharedData
import com.monster.literaryflow.service.TextFloatingWindowService
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.KeyboardUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTaskActivity : AppCompatActivity() {
    private var clickType: RunType = RunType.CLICK_XY
    private val binding by lazy { ActivityAddTaskBinding.inflate(layoutInflater) }
    private var position = -1
    private var isInsert = false
    private var pickType = TextPickType.EXACT_MATCH
    private lateinit var autoList: List<AutoInfo>
    private var clickTask: Pair<String, MutableList<RunBean>?>? = null
    private var openAppInfo: Pair<String, String>? = null
    private var isFindText4Node = true
    private lateinit var runApp:String
    companion object {
        const val REQUEST_APP_LIST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val database = AppDatabase.getDatabase(this)

        val autoInfoDao = database.autoInfoDao()

        val taskList = listOf(
            "点击坐标",
            "点击文字",
            "左右滑动",
            "上下滑动",
            "左滑",
            "右滑",
            "上滑",
            "下滑",
            "输入文字",
            "侧滑返回",
            "打开APP",
            "长按"
        )
        val pickTypeList = listOf("完全匹配", "模糊匹配", "多个模糊词")
        val data = intent.getSerializableExtra("clickData") as ClickBean?
        position = intent.getIntExtra("index", -1)
        isInsert = intent.getBooleanExtra("isInsert", false)
        runApp = intent.getStringExtra("runApp")!!

        binding.apply {
            CoroutineScope(Dispatchers.IO).launch {
                autoList = autoInfoDao.getAll()
                withContext(Dispatchers.Main) {
                    data?.let { setUpData(it) }
                    setUpTaskPickDialog(taskList.toMutableList())
                    setUpTextPickDialog(pickTypeList.toMutableList())
                    // 从数据库加载任务列表
                    btBack.setOnClickListener { finish() }
                    btSave.setOnClickListener { saveTaskData() }
                    tabSearch.setOnClickListener {
                        isFindText4Node = true
                        updateTextType()
                    }
                    tabScreenshot.setOnClickListener {
                        isFindText4Node = false
                        updateTextType()
                    }
                    if (autoList.isNotEmpty()) {
                        val list = autoList.map { it.title!! }.toMutableList()
                        setUpChoseTaskDialog(list.toMutableList())
                    } else {
                        tvChoseTask.visibility = View.GONE
                    }
                    updateTextType()
                    if (data == null) {
                        TaskPickDialog(this@AddTaskActivity, taskList.toMutableList()) { position ->
                            handleTaskPick(taskList.toMutableList(), position)
                        }.show()
                    }
                }
            }
        }
    }

    private fun setUpData(data: ClickBean) {
        when (data.clickType) {
            RunType.CLICK_XY -> configureClickXY(data)
            RunType.ENTER_TEXT -> configureEnterText()
            RunType.CLICK_TEXT -> configureClickText(data)
            RunType.GO_BACK, RunType.SCROLL_LEFT, RunType.SCROLL_RIGHT, RunType.SCROLL_TOP, RunType.SCROLL_BOTTOM -> configureScroll(
                data
            )

            RunType.LONG_HOR, RunType.LONG_VEH -> configureLongSwipe(data)
            RunType.TASK -> configTask(data)
            RunType.OPEN_APP -> configOpenApp(data)
            RunType.LONG_CLICK -> configureLongClick(data)
            else -> {}
        }
        binding.etLoopTimes.setText(data.loopTimes.toString())
        binding.etTime.setText(data.sleepTime.toString())
        pickType = data.findTextType
    }

    private fun configureClickXY() {
        binding.tvTask.text = "点击坐标"
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutXy.visibility = View.VISIBLE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = RunType.CLICK_XY
    }
    private fun configureClickXY(data: ClickBean) {
        binding.tvTask.text = "点击坐标"
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutXy.visibility = View.VISIBLE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = RunType.CLICK_XY
        binding.etClickX.setText(data.clickXy.first.toString())
        binding.etClickY.setText(data.clickXy.second.toString())
    }

    private fun configTask(data: ClickBean) {
        binding.tvTask.text = data.runTask?.first
        clickTask = data.runTask
    }

    private fun configOpenApp(data: ClickBean) {
        binding.tvTask.text = "打开APP[${data.openAppData?.first}] "
        openAppInfo = data.openAppData
        clickType = RunType.OPEN_APP
    }

    private fun configureEnterText() {
        binding.tvTask.text = "输入文字"
        binding.layoutEnter.visibility = View.VISIBLE
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutXy.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = RunType.ENTER_TEXT
    }



    private fun configureClickText(data: ClickBean) {
        isFindText4Node = data.isFindText4Node
        binding.tvTask.text = "点击文字"
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutClickText.visibility = View.VISIBLE
        binding.layoutXy.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = RunType.CLICK_TEXT
        binding.etFindText.setText(data.text)
        binding.tvTextPick.text = getPickTypeText(data.findTextType)
        binding.etFindTextTimes.setText(data.findTextTime.toString())
    }

    private fun getPickTypeText(type: TextPickType): String {
        return when (type) {
            TextPickType.EXACT_MATCH -> "完全匹配"
            TextPickType.FUZZY_MATCH -> "模糊匹配"
            TextPickType.MULTIPLE_FUZZY_WORDS -> "多个模糊词"
            else -> ""
        }
    }

    private fun configureLongClick(data: ClickBean) {
        binding.etLongClickTime.setText(data.longClickTime.toString())
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutXy.visibility = View.VISIBLE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        clickType = RunType.LONG_CLICK
        binding.layoutLongClick.visibility = View.VISIBLE
    }

    private fun configureScroll(data: ClickBean) {
        binding.tvTask.text = when (clickType) {
            RunType.SCROLL_LEFT -> "左滑"
            RunType.SCROLL_RIGHT -> "右滑"
            RunType.SCROLL_TOP -> "上滑"
            RunType.SCROLL_BOTTOM -> "下滑"
            RunType.GO_BACK -> "侧滑返回"
            else -> ""
        }
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutXy.visibility = View.GONE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = data.clickType!!
    }

    private fun updateTextType() {
        binding.tabSearch.isSelected = isFindText4Node
        binding.tabScreenshot.isSelected = !isFindText4Node
    }

    private fun configureLongSwipe(data: ClickBean) {
        binding.tvTask.text = when (clickType) {
            RunType.LONG_HOR -> "左右滑动"
            RunType.LONG_VEH -> "上下滑动"
            else -> ""
        }
        binding.layoutMinMax.visibility = View.VISIBLE
        binding.layoutXy.visibility = View.VISIBLE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        binding.tvMax.text = if (clickType == RunType.LONG_HOR) "x最大值" else "y最大值"
        binding.tvMin.text = if (clickType == RunType.LONG_HOR) "x最小值" else "y最小值"
        clickType = data.clickType!!
        binding.etClickX.setText(data.clickXy.first.toString())
        binding.etClickY.setText(data.clickXy.second.toString())
        binding.etMin.setText(data.scrollMinMax.first.toString())
        binding.etMax.setText(data.scrollMinMax.second.toString())
    }

    private fun setUpTaskPickDialog(taskList: MutableList<String>) {
        binding.tvClickTask.setOnClickListener {
            TaskPickDialog(this@AddTaskActivity, taskList) { position ->
                handleTaskPick(taskList, position)
            }.show()
        }
        binding.tvTask.setOnClickListener {
            TaskPickDialog(this@AddTaskActivity, taskList) { position ->
                handleTaskPick(taskList, position)
            }.show()
        }
    }

    private fun setUpChoseTaskDialog(taskList: MutableList<String>) {
        binding.tvChoseTask.setOnClickListener {
            TaskPickDialog(this@AddTaskActivity, taskList) { position ->
                handleTaskChose(taskList, position)
            }.show()
        }
    }

    private fun handleTaskPick(taskList: MutableList<String>, position: Int) {
        when (position) {
            0 -> configureClickXYLayout()
            1 -> configureClickTextLayout()
            2 -> configureLongSwipeLayout(RunType.LONG_HOR)
            3 -> configureLongSwipeLayout(RunType.LONG_VEH)
            4 -> configureScrollLayout(RunType.SCROLL_LEFT)
            5 -> configureScrollLayout(RunType.SCROLL_RIGHT)
            6 -> configureScrollLayout(RunType.SCROLL_TOP)
            7 -> configureScrollLayout(RunType.SCROLL_BOTTOM)
            8 -> configureEnterTextLayout()
            9 -> configureScrollLayout(RunType.GO_BACK)
            10 -> configureOpenApp()
            11 -> configureLongClickLayout()
        }
        when (position) {
            0,2,3-> {
                ScreenFloatingWindowsService.startService(this)
                CoroutineScope(Dispatchers.Main).launch {
                    AppUtils.openApp(this@AddTaskActivity, runApp)
                }

            }
            1 ->{
                TextFloatingWindowService.startService(this)
                CoroutineScope(Dispatchers.Main).launch {
                    AppUtils.openApp(this@AddTaskActivity, runApp)
                }
            }
        }
        binding.tvTask.text = taskList[position]
    }

    override fun onResume() {
        super.onResume()
        SharedData.rect.observe(this){
            if (it != null) {
                when (clickType) {
                    RunType.CLICK_XY -> {
                        binding.etClickX.setText(it.centerX().toString())
                        binding.etClickY.setText(it.centerY().toString())
                    }
                    RunType.LONG_HOR -> {
                        binding.etClickX.setText(it.centerX().toString())
                        binding.etClickY.setText(it.centerY().toString())
                        binding.etMin.setText(it.left.toString())
                        binding.etMax.setText(it.right.toString())
                    }
                    RunType.LONG_VEH -> {
                        binding.etClickX.setText(it.centerX().toString())
                        binding.etClickY.setText(it.centerY().toString())
                        binding.etMin.setText(it.top.toString())
                        binding.etMax.setText(it.bottom.toString())
                    }
                    RunType.CLICK_TEXT ->{
                        configureClickXY()
                        binding.etClickX.setText(it.centerX().toString())
                        binding.etClickY.setText(it.centerY().toString())
                    }

                    else -> {}
                }
                SharedData.rect.postValue(null)
            }
        }
        SharedData.textRect.observe(this){
            if (it!= null) {
                binding.etFindText.setText(it)
                SharedData.textRect.postValue(null)
            }
        }
    }


    private fun handleTaskChose(taskList: MutableList<String>, position: Int) {
        clickType = RunType.TASK
        clickTask = autoList[position].toAutoPair()
        binding.tvTask.text = taskList[position]
    }

    private fun configureOpenApp() {
        startActivityForResult(
            Intent(this@AddTaskActivity, AppListActivity::class.java),
            REQUEST_APP_LIST
        )
    }

    private fun configureClickXYLayout() {
        binding.layoutXy.visibility = View.VISIBLE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = RunType.CLICK_XY
    }

    private fun configureClickTextLayout() {
        binding.layoutClickText.visibility = View.VISIBLE
        binding.layoutXy.visibility = View.GONE
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = RunType.CLICK_TEXT
    }

    private fun configureLongSwipeLayout(type: RunType) {
        binding.layoutMinMax.visibility = View.VISIBLE
        binding.layoutXy.visibility = View.VISIBLE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = type
    }

    private fun configureScrollLayout(type: RunType) {
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutXy.visibility = View.GONE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutEnter.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = type
    }

    private fun configureEnterTextLayout() {
        binding.layoutEnter.visibility = View.VISIBLE
        binding.layoutXy.visibility = View.GONE
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutLongClick.visibility = View.GONE
        clickType = RunType.ENTER_TEXT
    }

    private fun configureLongClickLayout() {
        binding.layoutEnter.visibility = View.GONE
        binding.layoutXy.visibility = View.VISIBLE
        binding.layoutMinMax.visibility = View.GONE
        binding.layoutClickText.visibility = View.GONE
        binding.layoutLongClick.visibility = View.VISIBLE
        clickType = RunType.LONG_CLICK
    }

    private fun setUpTextPickDialog(pickTypeList: MutableList<String>) {
        binding.tvTextPick.setOnClickListener {
            TaskPickDialog(this@AddTaskActivity, pickTypeList) { position ->
                handleTextPick(pickTypeList, position)
            }.show()
        }
    }

    private fun handleTextPick(pickTypeList: List<String>, position: Int) {
        binding.etFindText.hint = ""
        binding.tvTextPick.text = pickTypeList[position]
        pickType = when (position) {
            0 -> TextPickType.EXACT_MATCH
            1 -> TextPickType.FUZZY_MATCH
            2 -> {
                binding.etFindText.hint = "#号隔开(例:领取#领取成#成功)"
                TextPickType.MULTIPLE_FUZZY_WORDS
            }

            else -> pickType
        }
    }

    private fun saveTaskData() {
        val clickBean = ClickBean().apply {
            clickType = this@AddTaskActivity.clickType
            loopTimes = binding.etLoopTimes.text.toString().toInt()
            sleepTime = binding.etTime.text.toString().toInt()
            scrollTime = binding.etScrollTime.text.toString().toInt()

            when (clickType) {
                RunType.CLICK_XY -> clickXy = Pair(
                    binding.etClickX.text.toString().toInt(),
                    binding.etClickY.text.toString().toInt()
                )

                RunType.LONG_VEH, RunType.LONG_HOR -> {
                    clickXy = Pair(
                        binding.etClickX.text.toString().toInt(),
                        binding.etClickY.text.toString().toInt()
                    )
                    scrollMinMax = Pair(
                        binding.etMin.text.toString().toInt(),
                        binding.etMax.text.toString().toInt()
                    )
                }

                RunType.CLICK_TEXT -> {
                    isFindText4Node = this@AddTaskActivity.isFindText4Node
                    text = binding.etFindText.text.toString()
                    findTextType = pickType
                    findTextTime = binding.etFindTextTimes.text.toString().toInt()
                }

                RunType.ENTER_TEXT -> enterText = binding.etEnterText.text.toString()
                RunType.TASK -> runTask = clickTask
                RunType.OPEN_APP -> openAppData = openAppInfo
                RunType.LONG_CLICK ->{
                    clickXy = Pair(
                        binding.etClickX.text.toString().toInt(),
                        binding.etClickY.text.toString().toInt()
                    )
                    longClickTime =
                        binding.etLongClickTime.text.toString().toInt()
                }

                else -> {}
            }
        }
        val intent = Intent().apply {
            putExtra("clickBean", clickBean)
            putExtra("index", position)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        KeyboardUtil.hideKeyboard(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AddAutoActivity.REQUEST_APP_LIST -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                binding.layoutMinMax.visibility = View.GONE
                binding.layoutXy.visibility = View.GONE
                binding.layoutClickText.visibility = View.GONE
                binding.layoutEnter.visibility = View.GONE
                binding.layoutLongClick.visibility = View.GONE
                val selectedApp = data?.getSerializableExtra("selectedApp") as? AppData
                openAppInfo = Pair(selectedApp?.appName!!, selectedApp.packageName)
                clickType = RunType.OPEN_APP
                binding.tvTask.text = "打开APP[${openAppInfo?.first}] "
            }
        }
    }
}
