package com.monster.literaryflow.autoRun.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.monster.literaryflow.autoRun.dialog.TaskPickDialog
import com.monster.literaryflow.autoRun.dialog.TimeDialog
import com.monster.literaryflow.databinding.ActivityAddActionBinding
import com.monster.literaryflow.room.AppDatabase
import com.monster.literaryflow.bean.*
import com.monster.literaryflow.service.SharedData
import com.monster.literaryflow.service.TextFloatingWindowService
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.KeyboardUtil
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddActionActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAddActionBinding.inflate(layoutInflater) }
    private var ruleType: RuleType = RuleType.FIND_TEXT
    private var startTimePair = Pair(0, 0)
    private var endTimePair = Pair(23, 59)
    private var position = -1
    private var triggerIndex = -1
    private var triggerBean: TriggerBean? = TriggerBean()
    private lateinit var autoList: List<AutoInfo>
    private var isMonitor = false
    private val REQUEST_ADD_TRUE = 0
    private val REQUEST_ADD_FALSE = 1
    private var isFindText4Node = true
    private var pickType = TextPickType.EXACT_MATCH
    private lateinit var runApp:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val taskList = mutableListOf("创建任务")
        val database = AppDatabase.getDatabase(this)
        val autoInfoDao = database.autoInfoDao()
        val pickTypeList = listOf("完全匹配", "模糊匹配", "多个模糊词")

        // 获取传递过来的数据
        val data = intent.getSerializableExtra("actionData") as TriggerBean?
        position = intent.getIntExtra("index", -1)
        runApp = intent.getStringExtra("runApp")!!
        triggerIndex = intent.getIntExtra("triggerIndex", 0)
        isMonitor = intent.getBooleanExtra("isMonitor", false)

        // 从数据库加载任务列表
        CoroutineScope(Dispatchers.IO).launch {
            autoList = autoInfoDao.getAll()
            val list = autoList.map { it.title!! }.toMutableList()
            taskList.addAll(list)
            withContext(Dispatchers.Main) { setupUI(data, taskList) }
        }

        binding.apply {
            btBack.setOnClickListener { finish() }
            tvText.setOnClickListener { switchToTextRule() }
            tvTime.setOnClickListener { switchToTimeRule() }
            layoutTime.setOnClickListener { showTimeDialog() }
            btSave.setOnClickListener { saveTriggerData() }
            tvActionTrue.setOnClickListener { showTaskPickDialog(taskList, true) }
            tvActionFalse.setOnClickListener { showTaskPickDialog(taskList, false) }
            binding.tabSearch.setOnClickListener {
                isFindText4Node = true
                updateTextType()
            }
            binding.tabScreenshot.setOnClickListener {
                isFindText4Node = false
                updateTextType()
            }
            setUpTextPickDialog(pickTypeList.toMutableList())
        }
    }

    // 设置UI
    private fun setupUI(data: TriggerBean?, taskList: MutableList<String>) {
        if (isMonitor) {
            binding.tvTitle.text = "添加监听"
            binding.layoutFalse.visibility = View.GONE
        }
        binding.tvText.isSelected = true
        binding.tvTime.isSelected = false
        if (data != null) {
            triggerBean = data
            binding.etScanTime.setText(data.runScanTime.toString())
            pickType = data.findTextType
            setupRule(data)
            setupActions(data)
            updateTextType()
        }
    }
    private fun setUpTextPickDialog(pickTypeList: MutableList<String>) {
        binding.tvTextPick.setOnClickListener {
            TaskPickDialog(this@AddActionActivity, pickTypeList) { position ->
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
    // 设置触发规则
    private fun setupRule(data: TriggerBean) {
        when (data.triggerType) {
            RuleType.FIND_TEXT -> {
                binding.tvText.isSelected = true
                binding.tvTime.isSelected = false
                ruleType = RuleType.FIND_TEXT
                pickType = data.findTextType
                binding.layoutTime.visibility = View.GONE
                binding.layoutText.visibility = View.VISIBLE
                binding.etFindText.setText(data.findText)
                isFindText4Node = data.isFindText4Node
            }
            RuleType.TIME -> {
                binding.tvText.isSelected = false
                binding.tvTime.isSelected = true
                ruleType = RuleType.TIME
                binding.layoutTime.visibility = View.VISIBLE
                binding.layoutText.visibility = View.GONE
                startTimePair = Pair(data.runTime?.first?.first!!, data.runTime?.first?.second!!)
                endTimePair = Pair(data.runTime?.second?.first!!, data.runTime?.second?.second!!)
                binding.btStartTime.text = "${startTimePair.first}:${startTimePair.second}"
                binding.btEndTime.text = "${endTimePair.first}:${endTimePair.second}"
            }

            else -> {}
        }
    }

    // 设置执行任务
    private fun setupActions(data: TriggerBean) {
        binding.tvActionTrue.text = getTaskString(data.runTrueAuto, data.runTrueTask)
        binding.tvActionFalse.text = getTaskString(data.runFalseAuto, data.runFalseTask)
    }

    // 根据任务类型设置字符串
    private fun getTaskString(auto: Pair<String, MutableList<RunBean>?>?, task: ClickBean?): String {
        return when {
            auto != null -> auto.first
            task != null -> task.toTaskString()
            else -> "未知任务类型"
        }
    }
    private fun updateTextType(){
        binding.tabSearch.isSelected = isFindText4Node
        binding.tabScreenshot.isSelected = !isFindText4Node
    }

    // 点击任务类型按钮时切换到文本规则
    private fun switchToTextRule() {
        if (!binding.tvText.isSelected) {
            ruleType = RuleType.FIND_TEXT
            binding.tvText.isSelected = true
            binding.tvTime.isSelected = false
            binding.layoutTime.visibility = View.GONE
            binding.layoutText.visibility = View.VISIBLE
            TextFloatingWindowService.startService(this)
            CoroutineScope(Dispatchers.Main).launch {
                AppUtils.openApp(this@AddActionActivity, runApp)
            }
        }
    }

    // 点击任务类型按钮时切换到时间规则
    private fun switchToTimeRule() {
        if (!binding.tvTime.isSelected) {
            ruleType = RuleType.TIME
            binding.tvText.isSelected = false
            binding.tvTime.isSelected = true
            binding.layoutTime.visibility = View.VISIBLE
            binding.layoutText.visibility = View.GONE
        }
    }

    // 弹出时间选择对话框
    private fun showTimeDialog() {
        TimeDialog(this, startTimePair, endTimePair) { startTime, endTime ->
            startTimePair = startTime
            endTimePair = endTime
            binding.btStartTime.text = TimeUtils.formatTime(startTime)
            binding.btEndTime.text = TimeUtils.formatTime(endTime)
        }.show()
    }

    // 保存触发器数据
    private fun saveTriggerData() {
        triggerBean?.apply {
            triggerType = ruleType
            runScanTime = binding.etScanTime.text.toString().toInt()
            when (ruleType) {
                RuleType.TIME -> runTime = Pair(startTimePair, endTimePair)
                RuleType.FIND_TEXT -> {
                    findText = binding.etFindText.text.toString()
                    findTextType = pickType
                    isFindText4Node = this@AddActionActivity.isFindText4Node
                }
            }
        }
        val intent = Intent().apply {
            putExtra("triggerBean", triggerBean)
            putExtra("index", position)
            putExtra("triggerIndex", triggerIndex)
            putExtra("isMonitor", isMonitor)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    // 显示任务选择对话框
    private fun showTaskPickDialog(taskList: MutableList<String>, isTrueTask: Boolean) {
        TaskPickDialog(this, taskList) { index ->
            if (index == 0) {
                val requestCode = if (isTrueTask) REQUEST_ADD_TRUE else REQUEST_ADD_FALSE
                startActivityForResult(Intent(this, AddTaskActivity::class.java), requestCode)
            } else {
                val task = autoList[index - 1]
                if (isTrueTask) {
                    binding.tvActionTrue.text = task.title
                    triggerBean?.runTrueTask = null
                    triggerBean?.runTrueAuto = task.toAutoPair()
                } else {
                    binding.tvActionFalse.text = task.title
                    triggerBean?.runFalseTask = null
                    triggerBean?.runFalseAuto = task.toAutoPair()
                }
            }
        }.show()
    }

    override fun onResume() {
        super.onResume()
        SharedData.textRect.observe(this){
            if (it!= null) {
                binding.etFindText.setText(it)
                SharedData.rect.postValue(null)
            }
        }
    }

    // 处理返回结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val clickBean = data?.getSerializableExtra("clickBean") as ClickBean
            if (requestCode == REQUEST_ADD_TRUE) {
                triggerBean?.runTrueTask = clickBean
                triggerBean?.runTrueAuto = null
                binding.tvActionTrue.text = clickBean.toTaskString()
            } else {
                triggerBean?.runFalseTask = clickBean
                triggerBean?.runFalseAuto = null
                binding.tvActionFalse.text = clickBean.toTaskString()
            }
        }
    }

    // 释放资源
    override fun onDestroy() {
        super.onDestroy()
        KeyboardUtil.hideKeyboard(this)
    }
}

// 扩展函数：将 ClickBean 转换为任务字符串
fun ClickBean.toTaskString(): String {
    return when (clickType) {
        RunType.CLICK_XY -> "点击 x:$clickXy?.first y:$clickXy?.second"
        RunType.LONG_HOR -> "左右滑动 X:$clickXy?.first y:$clickXy?.second，MIN:${scrollMinMax?.first} MAX:${scrollMinMax?.second},持续$scrollTime 秒"
        RunType.LONG_VEH -> "上下滑动 X:$clickXy?.first y:$clickXy?.second，MIN:${scrollMinMax?.first} MAX:${scrollMinMax?.second},持续$scrollTime 秒"
        RunType.SCROLL_LEFT -> "左滑"
        RunType.SCROLL_RIGHT -> "右滑"
        RunType.SCROLL_TOP -> "上滑"
        RunType.SCROLL_BOTTOM -> "下滑"
        RunType.CLICK_TEXT -> "点击文字：$text 寻找$findTextTime 秒"
        RunType.ENTER_TEXT -> "输入文字$text"
        RunType.GO_BACK -> "侧滑返回"
        RunType.OPEN_APP ->"打开APP[${openAppData?.first}]"
        else -> "未知任务类型"
    }
}

// 扩展函数：将 AutoInfo 转换为 Pair
fun AutoInfo.toAutoPair(): Pair<String, MutableList<RunBean>?> {
    return Pair(title!!, runInfo!!)
}
