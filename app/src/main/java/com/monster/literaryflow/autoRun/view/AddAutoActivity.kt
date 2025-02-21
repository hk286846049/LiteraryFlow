package com.monster.literaryflow.autoRun.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
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
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.databinding.ActivityAddAutoBinding
import com.monster.literaryflow.databinding.ActivityAutoListBinding
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
        adapter = AutoListAdapter(runList, object : AutoListListener {
            override fun onItemClick(position: Int) {
                if (runList[position].clickBean != null) {
                    val intent = Intent(this@AddAutoActivity, AddTaskActivity::class.java)
                    intent.putExtra("clickData", runList[position].clickBean)
                    intent.putExtra("index", position)
                    startActivityForResult(intent, REQUEST_ADD_TASK)
                } else {
                    val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
                    intent.putExtra("actionData", runList[position].triggerBean)
                    intent.putExtra("index", position)
                    startActivityForResult(intent, REQUEST_ADD_ACTION)
                }
            }

            override fun onItemDelete(position: Int) {
                runList.removeAt(position)
                adapter!!.setList(runList)
            }

            override fun onStateChange(position: Int) {
            }
        })
        monitorAdapter = MonitorAdapter(monitorList) {
            val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
            intent.putExtra("actionData", it.first)
            intent.putExtra("index", it.second)
            intent.putExtra("isMonitor", true)
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
                    if (data.runInfo != null) {
                        runList = data.runInfo ?: mutableListOf()
                        adapter!!.setList(runList)
                        monitorList = data.monitorList ?: mutableListOf()
                        monitorAdapter!!.setList(monitorList)
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
            startActivityForResult(
                Intent(this@AddAutoActivity, AddActionActivity::class.java),
                REQUEST_ADD_ACTION
            )
        }
        binding.tvAddTask.setOnClickListener {
            startActivityForResult(
                Intent(this@AddAutoActivity, AddTaskActivity::class.java),
                REQUEST_ADD_TASK
            )
        }
        binding.tvMonitor.setOnClickListener {
            val intent = Intent(this@AddAutoActivity, AddActionActivity::class.java)
            intent.putExtra("isMonitor", true)
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
            autoInfo.title = binding.etName.text.toString()
            autoInfo.runState = binding.swtichOpenApp.isChecked
            autoInfo.runTime = Pair(startTimePair, endTimePair)
            autoInfo.runTimes = times


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

            REQUEST_ADD_ACTION -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                val triggerBean = data?.getSerializableExtra("triggerBean") as TriggerBean
                val index = data.getIntExtra("index", -1)
                val isMonitor = data.getBooleanExtra("isMonitor", false)

                if (isMonitor) {
                    if (index == -1) {
                        monitorList.add(triggerBean)
                    } else {
                        monitorList[index] = triggerBean
                    }
                    monitorAdapter!!.setList(monitorList)
                    autoInfo.monitorList = monitorList
                } else {
                    val runBean = RunBean()
                    runBean.triggerBean = triggerBean
                    if (index == -1) {
                        runList.add(runBean)
                    } else {
                        runList[index] = runBean
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

    override fun onDestroy() {
        super.onDestroy()
        KeyboardUtil.hideKeyboard(this)
    }
}