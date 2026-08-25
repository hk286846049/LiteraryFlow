package taylor.com

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.monster.literaryflow.ui.floatwindow.DimensionUtil
import taylor.com.floatwindow.FloatWindow
import taylor.com.util.Preference
import taylor.com.util.View
import taylor.com.util.background_color
import taylor.com.util.layout_height
import taylor.com.util.layout_width
import taylor.com.util.match_parent
import taylor.com.util.nightMode

/**
 * base activity for the sake of dark mode
 */
open class BaseActivity : AppCompatActivity() {

    private val preference by lazy { Preference(getSharedPreferences("dark-mode", MODE_PRIVATE)) }

    companion object {
        val maskHandler by lazy { Handler(Looper.getMainLooper()) }
    }

    // this way is not so good to implement dark mode
    private fun showMaskWindow() {
        val view = View {
            layout_width = match_parent
            layout_height = match_parent
            background_color = "#c8000000"
        }
        val windowInfo = FloatWindow.WindowInfo(view).apply {
            width = DimensionUtil.getScreenWidth(this@BaseActivity)
            height = DimensionUtil.getScreenHeight(this@BaseActivity)
        }
        FloatWindow.show(this, "mask", windowInfo, 0, 100, false, false, true)
    }

    override fun onStart() {
        nightMode(preference["dark-mode", false])
        super.onStart()
    }
}


