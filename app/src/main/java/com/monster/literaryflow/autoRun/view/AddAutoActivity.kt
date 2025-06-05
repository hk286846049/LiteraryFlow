package com.monster.literaryflow.autoRun.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.monster.literaryflow.Const
import com.monster.literaryflow.MainActivity
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.autoRun.adapter.AutoListAdapter
import com.monster.literaryflow.autoRun.adapter.AutoListListener
import com.monster.literaryflow.autoRun.adapter.MonitorAdapter
import com.monster.literaryflow.autoRun.dialog.TimeDialog
import com.monster.literaryflow.bean.AppData
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.AutoRunType
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.databinding.ActivityAddAutoBinding
import com.monster.literaryflow.room.AppDatabase
import com.monster.literaryflow.room.AutoInfoDao
import com.monster.literaryflow.rule.AppListActivity
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.KeyboardUtil
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ClickableViewAccessibility")
class AddAutoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAddAutoBinding.inflate(layoutInflater) }
    private var isIncrementing = false // 是否正在自增
    private var isDecrementing = false // 是否正在自减
    private var times = 1
    private var startTimePair = Pair(0, 0)
    private var endTimePair = Pair(23, 59)
    private val handler = Handler(Looper.getMainLooper())
    private var runList: MutableList<RunBean> = mutableListOf()
    private var monitorList: MutableList<TriggerBean> = mutableListOf()
    private var adapter: AutoListAdapter? = null
    private var monitorAdapter: MonitorAdapter? = null
    private var autoInfo: AutoInfo = AutoInfo()
    private var isUpdate: Boolean = false
    private lateinit var database: AppDatabase
    private lateinit var autoInfoDao: AutoInfoDao

    companion object {
        const val REQUEST_APP_LIST = 1
        const val REQUEST_ADD_TASK = 2
        const val REQUEST_ADD_ACTION = 3
        const val REQUEST_INSERT_TASK = 4
    }

    private val incrementRunnable = object : Runnable {
        override fun run() {
            if (isIncrementing) {
                timesChange(true)
                handler.postDelayed(this, 100)
            }
        }
    }

    private val decrementRunnable = object : Runnable {
        override fun run() {
            if (isDecrementing) {
                timesChange(false)
                handler.postDelayed(this, 100)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        database = AppDatabase.getDatabase(this)
        autoInfoDao = database.autoInfoDao()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        val monitorManager = LinearLayoutManager(this)
        monitorManager.orientation = LinearLayoutManager.VERTICAL
        binding.mRecyclerView.layoutManager = layoutManager
        binding.monitorRecyclerView.layoutManager = monitorManager
        binding.rgLoopType.setOnCheckedChangeListener { _, checkedId ->
            binding.layoutWeeklyDays.visibility = View.GONE
            binding.layoutTime.visibility = View.GONE
            binding.layoutDailyTime.visibility = View.VISIBLE
            binding
            when (checkedId) {
                binding.rbWeekly.id -> {
                    binding.layoutWeeklyDays.visibility = View.VISIBLE
                    binding.layoutTime.visibility = View.VISIBLE
                    autoInfo.loopType = AutoRunType.WEEK_LOOP
                }

                binding.rbDaily.id -> {
                    autoInfo.loopType = AutoRunType.DAY_LOOP
                    binding.layoutTime.visibility = View.VISIBLE
                }

                binding.rbInfinite.id -> {
                    autoInfo.loopType = AutoRunType.LOOP
                    binding.layoutDailyTime.visibility = View.GONE
                }
            }
        }
        adapter = AutoListAdapter(runList, object : AutoListListener {
            override fun onItemClick(position: Int) {
                if (runList[position].clickBean != null) {
                    val intent = Intent(this@AddAutoActivity, AddTaskActivity::class.java)
                    intent.putExtra("clickData", runList[position].clickBean)
                    intent.putExtra("index", position)
                    intent.putExtra("runApp", autoInfo.runPackageName)
                    startActivityForResult(intent, REQUEST_ADD_TASK)
                }
            }

            override fun onTriggerItemClick(position: Int, triggerPosition: Int) {
                super.onTriggerItemClick(position, triggerPosition)
                val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
                intent.putExtra("actionData", runList[position].triggerBean!![triggerPosition])
                intent.putExtra("index", position)
                intent.putExtra("triggerIndex", triggerPosition)
                intent.putExtra("runApp", autoInfo.runPackageName)
                startActivityForResult(intent, REQUEST_ADD_ACTION)
            }

            override fun addItem(position: Int, triggerPosition: Int) {
                super.addItem(position, triggerPosition)
                val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
                intent.putExtra("index", position)
                intent.putExtra("triggerIndex", triggerPosition+1)
                intent.putExtra("runApp", autoInfo.runPackageName)
                startActivityForResult(intent, REQUEST_ADD_ACTION)
            }

            override fun onItemDelete(position: Int) {
                runList.removeAt(position)
                adapter!!.setList(runList)
            }


            override fun onInsert(position: Int) {
                val intent = Intent(this@AddAutoActivity, AddTaskActivity::class.java)
                intent.putExtra("index", position)
                intent.putExtra("isInsert", true)
                intent.putExtra("runApp", autoInfo.runPackageName)
                startActivityForResult(intent, REQUEST_INSERT_TASK)
            }

            override fun onStateChange(position: Int) {
            }
        })
        monitorAdapter = MonitorAdapter(monitorList) {
            val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
            intent.putExtra("actionData", it.first)
            intent.putExtra("index", it.second)
            intent.putExtra("isMonitor", true)
            intent.putExtra("runApp", autoInfo.runPackageName)
            startActivityForResult(intent, REQUEST_ADD_ACTION)
        }
        monitorAdapter?.setListener(object : AutoListListener {
            override fun onItemClick(position: Int) {
            }

            override fun onItemDelete(position: Int) {
                monitorList.removeAt(position)
                monitorAdapter!!.setList(monitorList)
            }

            override fun onStateChange(position: Int) {

            }

            override fun onInsert(position: Int) {
                if (autoInfo.runAppName == null) {
                    AppUtils.showToast(this@AddAutoActivity, "请选择要运行的应用")
                    return
                }
                val intent = Intent(this@AddAutoActivity, AddTaskActivity::class.java)
                intent.putExtra("index", position)
                intent.putExtra("isInsert", true)
                intent.putExtra("runApp", autoInfo.runPackageName)

                startActivityForResult(intent, REQUEST_INSERT_TASK)
            }

        })
        binding.mRecyclerView.adapter = adapter
        binding.monitorRecyclerView.adapter = monitorAdapter
        setOnClickListener()

        lifecycleScope.launch(Dispatchers.IO) {
            var data = intent.getSerializableExtra("autoInfo") as AutoInfo?
            if (intent.getBooleanExtra("isVirtual", false)) {
                val title = Const.VIRTUAL
                data = autoInfoDao.findByTitle(title)
                withContext(Dispatchers.Main) {
                    binding.tvTitle.text = "设置屏幕捕获配置"
                    if (data == null) {
                        binding.etName.setText(title)
                    }
                    binding.etTimes.isEnabled = false
                    binding.ivAdd.isEnabled = false
                    binding.ivMinus.isEnabled = false
                }
            }
            withContext(Dispatchers.Main) {
                if (data != null) {
                    isUpdate = true
                    autoInfo = data
                    binding.etName.setText(data.title)
                    binding.btApp.text = data.runAppName
                    binding.etTimes.setText(data.runTimes.toString())
                    times = data.runTimes
                    binding.swtichOpenApp.isChecked = data.runState
                    startTimePair = data.runTime?.first!!
                    endTimePair = data.runTime?.second!!
                    binding.btStartTime.text = TimeUtils.formatTime(startTimePair)
                    binding.btEndTime.text = TimeUtils.formatTime(endTimePair)
                    binding.etInterval.setText(data.sleepTime.toString())
                    if (data.runInfo != null) {
                        runList = data.runInfo ?: mutableListOf()
                        adapter!!.setList(runList)
                        monitorList = data.monitorList ?: mutableListOf()
                        monitorAdapter!!.setList(monitorList)
                    }
                    when (autoInfo.loopType) {
                        AutoRunType.WEEK_LOOP -> {
                            data.weekData.forEach {
                                when (it) {
                                    1 -> binding.cbMonday.isChecked = true
                                    2 -> binding.cbTuesday.isChecked = true
                                    3 -> binding.cbWednesday.isChecked = true
                                    4 -> binding.cbThursday.isChecked = true
                                    5 -> binding.cbFriday.isChecked = true
                                    6 -> binding.cbSaturday.isChecked = true
                                    7 -> binding.cbSunday.isChecked = true
                                }
                            }

                            binding.rbWeekly.isChecked = true
                            binding.layoutWeeklyDays.visibility = View.VISIBLE
                            binding.layoutTime.visibility = View.VISIBLE
                        }

                        AutoRunType.DAY_LOOP -> {
                            binding.rbDaily.isChecked = true
                            binding.layoutTime.visibility = View.VISIBLE
                        }

                        AutoRunType.LOOP -> {
                            binding.rbInfinite.isChecked = true
                            autoInfo.loopType = AutoRunType.LOOP
                            binding.layoutDailyTime.visibility = View.GONE
                        }

                        else -> {}
                    }


                }
            }
        }
    }

    private fun setOnClickListener() {
        binding.ivMinus.setOnClickListener { timesChange(false) }
        binding.ivAdd.setOnClickListener { timesChange(true) }
        binding.ivAdd.setOnLongClickListener {
            isIncrementing = true
            handler.post(incrementRunnable)
            true
        }

        binding.ivMinus.setOnLongClickListener {
            isDecrementing = true
            handler.post(decrementRunnable)
            true
        }
        binding.ivAdd.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isIncrementing = false
                    handler.removeCallbacks(incrementRunnable)
                }
            }
            false
        }
        binding.ivMinus.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDecrementing = false
                    handler.removeCallbacks(decrementRunnable)
                }
            }
            false
        }
        binding.btApp.setOnClickListener {
            startActivityForResult(
                Intent(this@AddAutoActivity, AppListActivity::class.java),
                REQUEST_APP_LIST
            )
        }
        binding.layoutTime.setOnClickListener {
            TimeDialog(this@AddAutoActivity, startTimePair, endTimePair) { startTime, endTime ->
                startTimePair = startTime
                endTimePair = endTime
                binding.btStartTime.text = TimeUtils.formatTime(startTime)
                binding.btEndTime.text = TimeUtils.formatTime(endTime)
            }.show()

        }
        binding.tvAddCondition.setOnClickListener {
            if (autoInfo.runAppName == null) {
                AppUtils.showToast(this@AddAutoActivity, "请选择要运行的应用")
                return@setOnClickListener
            }
            val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
            intent.putExtra("runApp", autoInfo.runPackageName)
            startActivityForResult(intent, REQUEST_ADD_ACTION)

        }
        binding.tvAddTask.setOnClickListener {
            if (autoInfo.runAppName == null) {
                AppUtils.showToast(this@AddAutoActivity, "请选择要运行的应用")
                return@setOnClickListener
            }
            val intent = Intent(this@AddAutoActivity, AddTaskActivity::class.java)
            intent.putExtra("runApp", autoInfo.runPackageName)
            startActivityForResult(
                intent,
                REQUEST_ADD_TASK
            )
        }
        binding.tvMonitor.setOnClickListener {

            if (autoInfo.runAppName == null) {
                AppUtils.showToast(this@AddAutoActivity, "请选择要运行的应用")
                return@setOnClickListener
            }
            val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
            intent.putExtra("isMonitor", true)
            intent.putExtra("runApp", autoInfo.runPackageName)
            startActivityForResult(intent, REQUEST_ADD_ACTION)
        }
        binding.btBack.setOnClickListener { finish() }

        binding.btStartTime.setOnClickListener {
            TimeDialog(this@AddAutoActivity, startTimePair, endTimePair) { startTime, endTime ->
                startTimePair = startTime
                endTimePair = endTime
                binding.btStartTime.text = TimeUtils.formatTime(startTime)
                binding.btEndTime.text = TimeUtils.formatTime(endTime)
            }.show()
        }
        binding.btEndTime.setOnClickListener {
            TimeDialog(this@AddAutoActivity, startTimePair, endTimePair) { startTime, endTime ->
                startTimePair = startTime
                endTimePair = endTime
                binding.btStartTime.text = TimeUtils.formatTime(startTime)
                binding.btEndTime.text = TimeUtils.formatTime(endTime)
            }.show()
        }
        binding.btSave.setOnClickListener {
            //做非空判断
            if (binding.etName.text.toString().isEmpty()) {
                AppUtils.showToast(this@AddAutoActivity, "请输入任务名称")
                return@setOnClickListener
            }
            if (autoInfo.runAppName==null) {
                AppUtils.showToast(this@AddAutoActivity, "请选择要运行的应用")
                return@setOnClickListener
            }
            if (autoInfo.loopType == AutoRunType.WEEK_LOOP) {
                autoInfo.weekData = mutableListOf()
                if (binding.cbMonday.isChecked) {
                    autoInfo.weekData.add(1)
                }
                if (binding.cbTuesday.isChecked) {
                    autoInfo.weekData.add(2)
                }
                if (binding.cbWednesday.isChecked) {
                    autoInfo.weekData.add(3)
                }
                if (binding.cbThursday.isChecked) {
                    autoInfo.weekData.add(4)
                }
                if (binding.cbFriday.isChecked) {
                    autoInfo.weekData.add(5)
                }
                if (binding.cbSaturday.isChecked) {
                    autoInfo.weekData.add(6)
                }
                if (binding.cbSunday.isChecked) {
                    autoInfo.weekData.add(7)
                }
                if (autoInfo.weekData.isEmpty()) {
                    AppUtils.showToast(this@AddAutoActivity, "请选择至少一个星期")
                    return@setOnClickListener
                }
            }

            autoInfo.title = binding.etName.text.toString()
            autoInfo.runState = binding.swtichOpenApp.isChecked
            autoInfo.runTime = Pair(startTimePair, endTimePair)
            autoInfo.runTimes = binding.etTimes.text.toString().toInt()
            autoInfo.sleepTime = binding.etInterval.text.toString().toInt()

            CoroutineScope(Dispatchers.IO).launch {
                if (isUpdate) {
                    autoInfoDao.update(autoInfo)
                } else {
                    autoInfoDao.insert(autoInfo)
                }
                withContext(Dispatchers.Main) {
                    MyApp.isUpdateData.value = true
                    finish()
                }
            }
        }
    }

    private fun timesChange(isAdd: Boolean) {
        if (isAdd) {
            times++
            binding.etTimes.setText(times.toString())
        } else {
            times--
            if (times > 0) {
                binding.etTimes.setText(times.toString())
            } else {
                times = -1
                binding.etTimes.setText("无限制")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_APP_LIST -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                val selectedApp = data?.getSerializableExtra("selectedApp") as? AppData
                binding.btApp.text = selectedApp?.appName
                autoInfo.runAppName = selectedApp?.appName
                autoInfo.runPackageName = selectedApp?.packageName
            }

            REQUEST_ADD_TASK -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                val clickBean = data?.getSerializableExtra("clickBean") as ClickBean
                val index = data.getIntExtra("index", -1)

                val runBean = RunBean()
                runBean.clickBean = clickBean
                if (index == -1) {
                    runList.add(runBean)
                } else {
                    runList[index] = runBean
                }
                adapter!!.setList(runList)
                autoInfo.runInfo = runList
            }
            REQUEST_INSERT_TASK -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                val clickBean = data?.getSerializableExtra("clickBean") as ClickBean
                val index = data.getIntExtra("index", -1)

                val runBean = RunBean()
                runBean.clickBean = clickBean
                if (index == -1) {
                    runList.add(runBean)
                } else {
                    runList.add(index,runBean)
                }
                adapter!!.setList(runList)
                autoInfo.runInfo = runList
            }

            REQUEST_ADD_ACTION -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                val triggerBean = data?.getSerializableExtra("triggerBean") as TriggerBean
                val index = data.getIntExtra("index", -1)
                val isMonitor = data.getBooleanExtra("isMonitor", false)
                val dataPosition = data.getIntExtra("triggerIndex", -1)
                if (isMonitor) {
                    if (index == -1) {
                        monitorList.add(triggerBean)
                    } else {
                        monitorList[index] = triggerBean
                    }
                    monitorAdapter!!.setList(monitorList)
                    autoInfo.monitorList = monitorList
                } else {
                    Log.d("AddAutoActivity", "onActivityResult: $dataPosition")
                    if (index == -1) {
                        val runBean = RunBean()
                        runBean.triggerBean.add(triggerBean)
                        runList.add(runBean)
                    } else {
                        if (dataPosition>runList[index].triggerBean.size){
                            runList[index].triggerBean.add(triggerBean)
                        }else{
                            runList[index].triggerBean[dataPosition] = triggerBean
                        }
                    }
                    adapter!!.setList(runList)
                    autoInfo.runInfo = runList
                }
            }


            MainActivity.REQUEST_CODE_OVERLAY_PERMISSION -> {
                startService(Intent(this, FloatingWindowService::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //隐藏软键盘
        KeyboardUtil.hideKeyboard(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        KeyboardUtil.hideKeyboard(this)
    }
}