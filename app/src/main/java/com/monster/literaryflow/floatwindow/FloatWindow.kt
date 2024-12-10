package taylor.com.floatwindow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import taylor.com.animation_dsl.Anim
import taylor.com.animation_dsl.AnimSet
import taylor.com.animation_dsl.animSet


/**
 * A window shows above an activity. It has the following feature:
 * 1. clickable: it could react to click event
 * 2. draggable and sticky: it could be dragged by finger. If finger leave the screen, it will stick to the nearest border of the screen
 * 3. show with animation: you could use Animation to define how window shows
 * 4. dismiss automatically with delay: you could specify a time to make it show like a Toast
 * 5. show in common position: there are 12 predefined position, make it easy to show window in common position
 * 6. show overall window: this is a kind of window which is visible during the whole app lifecycle
 */
object FloatWindow : View.OnTouchListener {

    /**
     * flags of float window's position
     */
    const val FLAG_TOP = 0x00000010
    const val FLAG_LEFT = 0x00000020
    const val FLAG_RIGHT = 0x00000040
    const val FLAG_BOTTOM = 0x00000080
    const val FLAG_START = 0x00000001
    const val FLAG_MID = 0x00000002
    const val FLAG_END = 0x00000004

    /**
     * several window content stored by String tag ;
     */
    private var windowInfoMap: HashMap<String, WindowInfo?> = HashMap()
    var windowInfo: WindowInfo? = null
    private var lastTouchX: Int = 0
    private var lastTouchY: Int = 0
    private val lastWeltX: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var context: Context? = null
    private var gestureDetector: GestureDetector = GestureDetector(context, GestureListener())
    private var dragEnable: Boolean = false

    /**
     * this list records the activities which shows this window
     */
    private var whiteList: List<Class<Any>>? = mutableListOf()

    /**
     * if true,whiteList will be used to depend which activity could show window
     * if false,all activities in app is allow to show window
     */
    private var enableWhileList: Boolean = false

    /**
     * the animation make window stick to the left or right side of screen
     */
    private var weltAnimator: ValueAnimator? = null

    private var inAndOutAnim: Anim? = null

    val layoutParam: WindowManager.LayoutParams?
        get() = windowInfo?.layoutParams

    val isShowing: Boolean
        get() = windowInfo?.view?.parent != null

    /**
     * listener invoked when touch outside of [FloatWindow]
     */
    private var onTouchOutside: (() -> Unit)? = null
    var onWindowShow: (() -> Unit)? = null
    var onWindowDismiss: (() -> Unit)? = null
    var onWindowMove: ((Float, Float, Int, Int, WindowManager.LayoutParams?) -> WindowManager.LayoutParams)? = null
    var onFling: (() -> Unit)? = null
    var onWindowClick: ((View, WindowInfo?) -> Boolean)? = null

    private var handler = android.os.Handler(Looper.getMainLooper())

    private fun getNavigationBarHeight(context: Context?): Int {
        val rid = context?.resources?.getIdentifier("config_showNavigationBar", "bool", "android")
        return if (rid != 0) {
            context?.resources?.getIdentifier("navigation_bar_height", "dimen", "android")?.let {
                context.resources?.getDimensionPixelSize(it)
            } ?: 0
        } else {
            0
        }
    }

    /**
     * update window position on the screen
     * @param windowInfo which window to update
     */
    fun updateWindowView(
        windowInfo: WindowInfo? = this.windowInfo,
        x: Int = windowInfo?.layoutParams?.x.value(),
        y: Int = windowInfo?.layoutParams?.y.value()
    ) {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (windowInfo?.hasParent().value()) {
            windowInfo?.layoutParams?.apply {
                this.x = x
                this.y = y
            }
            windowManager.updateViewLayout(windowInfo?.view, windowInfo?.layoutParams)
        }
    }

    fun setEnable(enable: Boolean, tag: String) {
        val windowInfo = windowInfoMap[tag]
            ?: throw RuntimeException("no such window view,please invoke setView() first")
        windowInfo.enable = enable
    }

    /**
     * show float window, every window has a tag
     * @param tag a unique tag for a window, if showing the previous window without providing [windowInfo], we will looking for it in [windowInfoMap]
     * @param windowInfo the necessary information for showing float window, it will be kept in [windowInfoMap] with the key [tag]
     * @param x the horizontal position of float window according to the left top of screen
     * @param y the vertical position of float window according to the left top of screen
     * @param dragEnable whether window could be dragged by finger
     * @param overall whether window is visible during the whole app lifecycle
     * @param penetrate whether touch event could penetrate this window
     */
    fun show(
        context: Context,
        tag: String,
        windowInfo: WindowInfo? = windowInfoMap[tag],
        x: Int = windowInfo?.layoutParams?.x.value(),
        y: Int = windowInfo?.layoutParams?.y.value(),
        dragEnable: Boolean = false,
        overall: Boolean = false,
        penetrate: Boolean = false
    ) {
        if (windowInfo == null) {
            Log.v("ttaylor", "there is no view to show,please creating the right WindowInfo object")
            return
        }
        if (!windowInfo.enable) {
            return
        }
        if (windowInfo.view == null) {
            return
        }
        this.windowInfo = windowInfo
        this.dragEnable = dragEnable
        windowInfoMap[tag] = windowInfo
        windowInfo.view?.setOnTouchListener(this)
        if (penetrate) {
            windowInfo.view?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        this.context = context
        windowInfo.layoutParams = createLayoutParam(x, y, overall, penetrate)
        //in case of "IllegalStateException :has already been added to the window manager."
        if (!windowInfo.hasParent().value()) {
            val windowManager = this.context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            prepareScreenDimension(windowManager)
            windowManager?.addView(windowInfo.view, windowInfo.layoutParams)
            updateWindowViewSize()
            onWindowShow?.invoke()
        }
    }

    /**
     * show float window according to specific gravity with animation defined in [onAnimateWindow]
     *
     * @param tag a unique tag for a window, if showing the previous window without providing [windowInfo], we will looking for it in [windowInfoMap]
     * @param windowInfo the necessary information for showing float window, it will be kept in [windowInfoMap] with the key [tag]
     * @param flag which position to show float window
     * @param offset the offset value in pixel of position set by [flag]
     * @param onAnimateWindow the animation which will be played to window after shown
     */
    fun show(
        context: Context,
        tag: String,
        windowInfo: WindowInfo? = windowInfoMap[tag],
        flag: Int,
        offset: Int = 0,
        onAnimateWindow: ((WindowInfo) -> Anim)?
    ) {
        this.context = context
        getShowPoint(flag, windowInfo, offset).let { show(context, tag, windowInfo, it.x, it.y, false, overall = true) }
        windowInfo?.view?.post { inAndOutAnim = onAnimateWindow?.invoke(windowInfo) }
    }

    /**
     * show float window according to specific gravity with default animation and dismiss automatically
     * @param tag a unique tag for a window, if showing the previous window without providing [windowInfo], we will looking for it in [windowInfoMap]
     * @param windowInfo the necessary information for showing float window, it will be kept in [windowInfoMap] with the key [tag]
     * @param flag which position to show float window
     * @param gravityOffset the offset value in pixel in gravity
     * @param positionOffset the offset value in pixel in position
     * @param duration the time span of window show animation
     * @param stayTime [stayTime] milliseconds after it shows, window will dismiss itself
     */
    fun show(
        context: Context,
        tag: String,
        windowInfo: WindowInfo? = windowInfoMap[tag],
        flag: Int,
        gravityOffset: Int = 0,
        positionOffset: Int = 0,
        duration: Long = 250L,
        stayTime: Long = 1500L
    ) {
        this.context = context
        getShowPoint(flag, windowInfo, gravityOffset).let { show(context, tag, windowInfo, it.x, it.y, false, overall = true) }
        windowInfo?.view?.post {
            inAndOutAnim = getShowAnim(flag, windowInfo, duration, positionOffset)?.also { anim ->
                anim.start()
                //dismiss itself by delay
                handler.postDelayed({
                    anim.reverse()
                    anim.onEnd = { dismiss(windowInfo) }
                }, stayTime)
            }
        }
    }

    /**
     * get the coordinate of window's left-top point on the screen
     */
    private fun getShowPoint(flag: Int, windowInfo: WindowInfo?, offset: Int): Point {
        val windowManager = this.context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        prepareScreenDimension(windowManager)
        return when {
            flag.and(FLAG_TOP) != 0 -> {
                val y = -windowInfo?.height.value() - getStatusBarHeight()
                val x = getValueByGravity(flag, screenWidth, windowInfo?.width.value()) + offset
                Point(x, y)
            }
            flag.and(FLAG_BOTTOM) != 0 -> {
                val y = screenHeight - getStatusBarHeight()
                val x = getValueByGravity(flag, screenWidth, windowInfo?.width.value()) + offset
                Point(x, y)
            }
            flag.and(FLAG_LEFT) != 0 -> {
                val x = -windowInfo?.width.value()
                val y = getValueByGravity(flag, screenHeight, windowInfo?.height.value()) + offset
                Point(x, y)
            }
            flag.and(FLAG_RIGHT) != 0 -> {
                val x = screenWidth
                val y = getValueByGravity(flag, screenHeight, windowInfo?.height.value()) + offset
                Point(x, y)
            }
            else -> Point(0, 0)
        }
    }

    /**
     * get the predefine animation used to show float window
     */
    private fun getShowAnim(
        flag: Int,
        windowInfo: WindowInfo?,
        duration: Long,
        positionOffset: Int
    ): AnimSet? = when {
        flag.and(FLAG_TOP) != 0 -> {
            animSet {
                anim {
                    values = intArrayOf(windowInfo?.layoutParams?.y.value(), positionOffset)
                    this.duration = duration
                    interpolator = LinearOutSlowInInterpolator()
                    action = { value -> updateWindowView(windowInfo = windowInfo, y = value as Int) }
                }
            }
        }
        flag.and(FLAG_BOTTOM) != 0 -> {
            animSet {
                anim {
                    values = intArrayOf(
                        windowInfo?.layoutParams?.y.value(),
                        windowInfo?.layoutParams?.y.value() - windowInfo?.height.value() - positionOffset
                    )
                    this.duration = duration
                    interpolator = LinearOutSlowInInterpolator()
                    action = { value -> updateWindowView(windowInfo = windowInfo, y = value as Int) }
                }
            }
        }
        flag.and(FLAG_LEFT) != 0 -> {
            animSet {
                anim {
                    values = intArrayOf(windowInfo?.layoutParams?.x.value(), positionOffset)
                    this.duration = duration
                    interpolator = LinearOutSlowInInterpolator()
                    action = { value -> updateWindowView(windowInfo = windowInfo, x = value as Int) }
                }
            }

        }
        flag.and(FLAG_RIGHT) != 0 -> {
            animSet {
                anim {
                    values =
                        intArrayOf(
                            windowInfo?.layoutParams?.x.value(),
                            windowInfo?.layoutParams?.x.value() - windowInfo?.layoutParams?.width.value() - positionOffset
                        )
                    this.duration = duration
                    interpolator = LinearOutSlowInInterpolator()
                    action = { value -> updateWindowView(windowInfo = windowInfo, x = value as Int) }
                }
            }
        }
        else -> null
    }

    private fun updateWindowViewSize() {
        windowInfo?.view?.post {
            windowInfo?.apply {
                width = view?.width.value()
                height = view?.height.value()
            }
        }
    }

    /**
     * get the x or y value of float window according to flag
     */
    private fun getValueByGravity(flag: Int, total: Int, actual: Int): Int = when {
        flag.and(FLAG_START) != 0 -> 0
        flag.and(FLAG_MID) != 0 -> (total - actual) / 2
        flag.and(FLAG_END) != 0 -> (total - actual)
        else -> 0
    }

    /**
     * create layout params for window
     * @param overall whether window is showing during whole app lifecycle
     */
    private fun createLayoutParam(x: Int, y: Int, overall: Boolean, penetrate: Boolean): WindowManager.LayoutParams {
        if (context == null) {
            return WindowManager.LayoutParams()
        }

        return WindowManager.LayoutParams().apply {
            type = if (overall) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
            } else {
                WindowManager.LayoutParams.TYPE_APPLICATION
            }
            format = PixelFormat.TRANSLUCENT
            flags = if (!penetrate) {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_FULLSCREEN
            }
            dimAmount = 0f
            this.gravity = Gravity.START or Gravity.TOP
            width = windowInfo?.width.value()
            height = windowInfo?.height.value()
            this.x = x
            this.y = y
        }
    }

    fun setDimAmount(amount: Float) {
        windowInfo?.layoutParams?.let {
            it.dimAmount = amount
        }
    }

    private fun dismiss(windowInfo: WindowInfo? = this.windowInfo) {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        //in case of "IllegalStateException :not attached to window manager."
        if (windowManager != null && windowInfo?.hasParent().value()) {
            windowManager.removeViewImmediate(windowInfo?.view)
            onWindowDismiss?.invoke()
        }
    }

    fun dismiss(tag:String){
        windowInfoMap[tag]?.let { dismiss(it) }
    }

    fun setWhiteList(whiteList: List<Class<Any>>) {
        enableWhileList = true
        this.whiteList = whiteList
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        //handle outside touch event
        if (event.action == MotionEvent.ACTION_OUTSIDE) {
            onTouchOutside?.invoke()
            return true
        }

        //let GestureDetector take care of touch event,in order to parsing touch event into different gesture
        gestureDetector.onTouchEvent(event).takeIf { !it && dragEnable }?.also {
            //there is no ACTION_UP event in GestureDetector
            val action = event.action
            when (action) {
                MotionEvent.ACTION_UP -> onActionUp(event, screenWidth, windowInfo?.width ?: 0)
                else -> {
                }
            }
        }
        return true
    }

    /**
     * set whether outside touch event could be detected
     */
    fun setOutsideTouchable(enable: Boolean, onTouchOutside: (() -> Unit)? = null) {
        if (enable) windowInfo?.layoutParams?.let { layoutParams ->
            layoutParams.flags =
                layoutParams.flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            this.onTouchOutside = onTouchOutside
        }
    }

    private fun onActionUp(event: MotionEvent, screenWidth: Int, width: Int) {
        if (!windowInfo?.hasView().value()) {
            return
        }
        val upX = event.rawX.toInt()
        val endX = if (upX > screenWidth / 2) {
            screenWidth - width
        } else {
            0
        }

        if (weltAnimator == null) {
            weltAnimator = ValueAnimator.ofInt(windowInfo?.layoutParams!!.x, endX).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = WELT_ANIMATION_DURATION
                addUpdateListener { animation ->
                    val x = animation.animatedValue as Int
                    if (windowInfo?.layoutParams != null) {
                        windowInfo?.layoutParams!!.x = x
                    }
                    val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    if (windowInfo?.hasParent().value()) {
                        windowManager?.updateViewLayout(windowInfo?.view, windowInfo?.layoutParams)
                    }
                }
            }
        }
        weltAnimator?.setIntValues(windowInfo?.layoutParams!!.x, endX)
        weltAnimator?.start()
    }


    private fun onActionMove(event: MotionEvent) {
        val currentX = event.rawX.toInt()
        val currentY = event.rawY.toInt()
        val dx = currentX - lastTouchX
        val dy = currentY - lastTouchY

        windowInfo?.layoutParams!!.x += dx
        windowInfo?.layoutParams!!.y += dy
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        var rightMost = screenWidth - windowInfo?.layoutParams!!.width
        var leftMost = 0
        val topMost = 0
        val bottomMost =
            screenHeight - windowInfo?.layoutParams!!.height - getNavigationBarHeight(context)
        var partnerParam: WindowManager.LayoutParams? = null
        partnerParam = onWindowMove?.invoke(dx.toFloat(), dy.toFloat(), screenWidth, screenHeight, windowInfo?.layoutParams)
        //adjust move area according to partner window
        if (partnerParam != null) {
            if (partnerParam.x < windowInfo?.layoutParams!!.x) {
                leftMost = partnerParam.width - windowInfo?.layoutParams!!.width / 2
            } else if (partnerParam.x > windowInfo?.layoutParams!!.x) {
                rightMost =
                    screenWidth - (windowInfo?.layoutParams!!.width / 2 + partnerParam.width)
            }
        }

        //make window float inside screen
        if (windowInfo?.layoutParams!!.x < leftMost) {
            windowInfo?.layoutParams!!.x = leftMost
        }
        if (windowInfo?.layoutParams!!.x > rightMost) {
            windowInfo?.layoutParams!!.x = rightMost
        }
        if (windowInfo?.layoutParams!!.y < topMost) {
            windowInfo?.layoutParams!!.y = topMost
        }
        if (windowInfo?.layoutParams!!.y > bottomMost) {
            windowInfo?.layoutParams!!.y = bottomMost
        }
        windowManager?.updateViewLayout(windowInfo?.view, windowInfo?.layoutParams)
        lastTouchX = currentX
        lastTouchY = currentY
    }

    private fun onActionDown(event: MotionEvent) {
        lastTouchX = event.rawX.toInt()
        lastTouchY = event.rawY.toInt()
    }

    private fun prepareScreenDimension(windowManager: WindowManager?) {
        if (screenWidth != 0 && screenHeight != 0) {
            return
        }
        if (windowManager != null) {
            val dm = DisplayMetrics()
            val display = windowManager.defaultDisplay
            if (display != null) {
                windowManager.defaultDisplay.getMetrics(dm)
                screenWidth = dm.widthPixels
                screenHeight = dm.heightPixels
            }
        }
    }

    fun getStatusBarHeight(): Int {
        return context?.resources?.let {
            it.getDimensionPixelSize(it.getIdentifier("status_bar_height", "dimen", "android"))
        } ?: 0
    }


    private class GestureListener : GestureDetector.OnGestureListener {

        private var touchFrame: Rect? = null

        override fun onDown(e: MotionEvent): Boolean {
            onActionDown(e)
            return false
        }

        override fun onShowPress(e: MotionEvent) {}

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            //find clicked child view first. If there is none, we consider the root of window view is clicked
            return if (findClickableChild(e.x.toInt(), e.y.toInt())) {
                true
            } else {
                windowInfo?.view?.let { onWindowClick?.invoke(it, windowInfo) } ?: false
            }
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (dragEnable) {
                onActionMove(e2)
                return true
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {}

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            onFling?.invoke()
            inAndOutAnim?.let { anim ->
                anim.reverse()
                return true
            }
            return false
        }


        /**
         * find clickable child according to the touch position
         */
        fun findClickableChild(x: Int, y: Int): Boolean {
            if (touchFrame == null) {
                touchFrame = Rect()
            }

            windowInfo?.view?.takeIf { it is ViewGroup }?.let { rootView ->
                rootView as ViewGroup
                (0 until rootView.childCount).map { index -> rootView.getChildAt(index) }
                    .forEach { child ->
                        if (child.visibility == View.VISIBLE) {
                            child.getHitRect(touchFrame)
                            if (touchFrame?.contains(x, y).value()) {
                                return onWindowClick?.invoke(child, windowInfo).value()
                            }
                        }
                    }
            }
            return false
        }

    }

    fun onDestroy() {
        windowInfoMap.clear()
        context = null
    }

    class WindowInfo(var view: View?) {
        /**
         * the layout param of window content view
         */
        var layoutParams: WindowManager.LayoutParams? = null

        /**
         * whether this window content is allow to show
         */
        var enable = true

        /**
         * the width of window content
         */
        var width: Int = 0

        /**
         * the height of window content
         */
        var height: Int = 0

        fun hasView() = view != null && layoutParams != null

        fun hasParent() = hasView() && view?.parent != null

    }

    private val WELT_ANIMATION_DURATION: Long = 150

    fun Boolean?.value() = this ?: false
    fun Int?.value() = this ?: 0
}
