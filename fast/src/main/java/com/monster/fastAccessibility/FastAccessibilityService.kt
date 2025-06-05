package com.monster.fastAccessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.EventWrapper
import cn.coderpig.cp_fast_accessibility.NodeWrapper
import cn.coderpig.cp_fast_accessibility.blankOrThis
import cn.coderpig.cp_fast_accessibility.jumpAccessibilityServiceSettings
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


abstract class FastAccessibilityService : AccessibilityService() {
    companion object {
        var instance: FastAccessibilityService? = null
        val isServiceEnable: Boolean get() = instance != null
        private var _appContext: Context? = null
        //前台app包名
        var foregroundAppPackageName: String? = null
        val appContext
            get() = _appContext
                ?: throw NullPointerException("需要在Application的onCreate()中调用init()")
        lateinit var specificServiceClass: Class<*> // 具体无障碍服务实现类的类类型
        private var mListenEventTypeList = arrayListOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.WINDOWS_CHANGE_BOUNDS,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        ) // 监听的event类型列表
        private var mListenPackageList = arrayListOf<String>()  // 要监听的event的包名列表，不传默认监听所有应用的包名
        var touchListener:AccessTouchListener? = null // 触摸事件监听

        /**
         * 库初始化方法，必须在Application的OnCreate()中调用
         *
         * @param context Context上下文
         * @param clazz 无障碍服务的类类型
         * @param typeList 监听的事件类型列表，不传默认只监听TYPE_WINDOW_STATE_CHANGED类型
         * @param packageList 要监听的应用包名
         * */
        fun init(
            context: Context,
            clazz: Class<*>,
            typeList: ArrayList<Int>? = null,
            packageList: ArrayList<String>? = null
        ) {
            _appContext = context.applicationContext
            specificServiceClass = clazz
            typeList?.let { mListenEventTypeList = it }
            packageList?.let { mListenPackageList = it }
        }

        /**
         * 请求无障碍服务权限，即跳转无障碍设置页
         * */
        fun requireAccessibility() {
            if (!isServiceEnable) jumpAccessibilityServiceSettings()
        }

        /**
         * 无障碍服务Action套一层，没权限直接跳设置
         * */
        val require get() = run { requireAccessibility(); instance }
    }

    private var executor: ExecutorService = Executors.newFixedThreadPool(4) // 执行任务的线程池

    fun setTouchListener(listener:AccessTouchListener){
        touchListener = listener
    }
    fun removeTouchListener(){
        touchListener = null
    }

    override fun onServiceConnected() {
        if (this::class.java == specificServiceClass) instance = this
    }

    override fun onDestroy() {
        if (this::class.java == specificServiceClass) instance = null
        executor.shutdown()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                foregroundAppPackageName = it.packageName?.toString()
            }
        }

        if ( event == null) return
        if (mListenEventTypeList.isNotEmpty()) {
            if (event.eventType in mListenEventTypeList) {
                touchListener?.onTouch()
             /*   val className = event.className.blankOrThis()
                val packageName = event.packageName.blankOrThis()
                val eventType = event.eventType
                // 监听列表为空，或者要监听的package列表里有此package的event才分发
                if (mListenPackageList.isEmpty() || (mListenPackageList.isNotEmpty() && packageName in mListenPackageList)) {
                    analyzeSource(
                        EventWrapper(packageName, className, eventType),
                        noAnalyzeCallback = ::noAnalyzeCallBack,
                        analyzeCallback = ::analyzeCallBack
                    )
                }*/
            }
        }
    }

    /**
     * 解析当前页面结点
     *
     * @param wrapper Event包装类
     * @param waitTime 延迟获取结点Source的时间
     * @param getWindow 通过getWindows()获得窗口，然后过滤返回所需窗口的方法
     * @param callback 处理结点信息的回调
     * */
    fun analyzeSource(
        wrapper: EventWrapper? = null,
        waitTime: Long = 100,
        getWindow: ((List<AccessibilityWindowInfo>) -> AccessibilityWindowInfo)? = null,
        noAnalyzeCallback: ((EventWrapper?, AccessibilityNodeInfo?) -> Unit)? = null,
        analyzeCallback: ((EventWrapper?, AnalyzeSourceResult) -> Unit)? = null
    ) {
        executor.execute {
            Thread.sleep(waitTime)   // 休眠100毫秒避免获取到错误的source
            // 遍历解析获得结点列表
            AnalyzeSourceResult(arrayListOf()).let { result ->
                getWindow?.invoke(windows)?.root ?: rootInActiveWindow?.let {
                    analyzeNode(it, 0, result.nodes)
                    noAnalyzeCallback?.invoke(wrapper, it)
                    analyzeCallback?.invoke(wrapper, result)
                }
            }
        }
    }


    /**
     * 递归遍历结点的方法
     * */
    private fun analyzeNode(
        node: AccessibilityNodeInfo?,
        nodeIndex: Int,
        list: ArrayList<NodeWrapper>,
        depth: Int = 0, // 添加递归深度参数
        maxDepth: Int = 50 // 设定最大递归深度
    ) {
        if (node == null || depth > maxDepth) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        list.add(
            NodeWrapper(
                index = nodeIndex,
                text = node.text.blankOrThis(),
                id = node.viewIdResourceName.blankOrThis(),
                bounds = bounds,
                className = node.className.blankOrThis(),
                description = node.contentDescription.blankOrThis(),
                clickable = node.isClickable,
                scrollable = node.isScrollable,
                editable = node.isEditable,
                nodeInfo = node
            )
        )

        if (node.childCount > 0) {
            for (index in 0 until node.childCount) {
                analyzeNode(node.getChild(index), index, list, depth + 1, maxDepth)
            }
        }
    }


    // 监听Event的自定义回调，可按需重写
    open fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {}

    // 不解析结点的自定义回调，可按需重写
    open fun noAnalyzeCallBack(wrapper: EventWrapper?, node: AccessibilityNodeInfo?) {}

    override fun onInterrupt() {}

    // 重写获取当前页面节点信息，异常的话返回Null
    override fun getRootInActiveWindow() = try {
        super.getRootInActiveWindow()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }



}
interface AccessTouchListener{
    fun onTouch():Boolean
}