package com.example.worm

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.worm.ui.theme.SecondActivity
import com.example.worm.ui.theme.WormTheme
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.delay

var sw:Boolean = true

class HomeFragment : Fragment(R.layout.activity_main_uji) {
    override fun onViewCreated(v: View, b: Bundle?) {
        super.onViewCreated(v, b)
        v.findViewById<Button>(R.id.matadalam).setOnClickListener {
            sw = false
            parentFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, DahJalan())
                .commit()
            (activity as? MainActivity)?.updateToolbarColor()
            (activity as? MainActivity)?.updateToolbarColor()
        }
    }
    class DahJalan : Fragment(R.layout.dahjalan) {
        override fun onViewCreated(v: View, b: Bundle?) {
            super.onViewCreated(v, b)
            v.findViewById<Button>(R.id.silang).setOnClickListener {
                sw = true
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, HomeFragment())
                    .commit()
                (activity as? MainActivity)?.updateToolbarColor()
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val PREFS_NAME = "app_prefs"
    private val KEY_SW    = "sw"
    private var doubleBackToExitPressedOnce = false
    private var doubleBackToExitPressedOnce1 = false  // For DahJalan
    private val handler = Handler(Looper.getMainLooper())
    private val resetBackPress = Runnable { doubleBackToExitPressedOnce = false }
    private val resetBackPress1 = Runnable { doubleBackToExitPressedOnce1 = false }
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
        sw = true
        enableEdgeToEdge()
        val newsid: String = "a895c9b8cc274495a67cdaec6922fe8c"
        val gnews: String = "bd93c4e2fcfe19b6239972ea95dd0512"
        val news: String = "13b9c989205c4629861ae9d6e3ceb728"
        val AI: String = "AIzaSyC8wJ_8GNj33Xp-pGC6vD6S0JlYB5eg07Y"

        fun Context.dpToPx(dp: Int): Int =
            (dp * resources.displayMetrics.density).toInt()

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_fragment, HomeFragment())
            .commit()

        // 1. Root DrawerLayout
        val drawer = DrawerLayout(this).apply {
            id = R.id.drawerdrawer
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 2. Content container
        val content = FrameLayout(this).apply {
            id = R.id.framedrawer
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT,
                DrawerLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 3. Fragment container (ADD THIS BEFORE TOOLBAR)
        val fragmentContainer = FrameLayout(this).apply {
            id = R.id.container_fragment
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        content.addView(fragmentContainer)

        // 4. Toolbar setup (now accessible as property)
        toolbar = Toolbar(this).apply {
            id = R.id.toolbardrawer
            setBackgroundColor(if (sw) Color.parseColor("#2b5f56") else Color.WHITE)
            setTitleTextColor(Color.TRANSPARENT)

            navigationIcon = ContextCompat.getDrawable(
                context, android.R.drawable.ic_menu_sort_by_size
            )
            navigationIcon?.setTint(ContextCompat.getColor(context, R.color.tabmenu))
            setNavigationOnClickListener { drawer.openDrawer(Gravity.START) }

            val tv = TypedValue()
            context.theme.resolveAttribute(
                androidx.appcompat.R.attr.actionBarSize, tv, true
            )
            val actionBarHeightPx = TypedValue.complexToDimensionPixelSize(
                tv.data, resources.displayMetrics
            )

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                actionBarHeightPx,
                Gravity.TOP
            )
        }

        setSupportActionBar(toolbar)
        content.addView(toolbar)

        // 5. Navigation drawer (keeping your existing code)
        val navView = NavigationView(this).apply {
            id = R.id.navdrawer
            val width = (resources.displayMetrics.widthPixels * 0.75).toInt()
            layoutParams = DrawerLayout.LayoutParams(
                width, DrawerLayout.LayoutParams.MATCH_PARENT, Gravity.START
            )
            setBackgroundColor(Color.parseColor("#5E7D76"))

            val header = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 80, 48, 32)
                addView(TextView(context).apply {
                    text = "Tab Menu"
                    setTextColor(Color.WHITE)
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                })
            }
            addHeaderView(header)

            menu.add(Menu.NONE, 1, Menu.NONE, "Log Pengecekan")
                .icon = getDrawable(android.R.drawable.ic_menu_search)
            menu.add(Menu.NONE, 2, Menu.NONE, "Tentang Kami")
                .icon = getDrawable(android.R.drawable.ic_menu_info_details)
            menu.add(Menu.NONE, 3, Menu.NONE, "Home")
                .icon = getDrawable(android.R.drawable.ic_media_next)

            setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    3 -> if (sw){
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container_fragment, HomeFragment())
                            .commit()
                    }else{
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container_fragment, HomeFragment.DahJalan())
                            .commit()
                    }
                    1 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, LogFragment())
                        .addToBackStack(null)
                        .commit()
                    2 -> supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, AboutFragment())
                        .addToBackStack(null)
                        .commit()
                }
                drawer.closeDrawer(Gravity.START)
                true
            }
        }

        // 6. Add views to drawer
        drawer.addView(content)
        drawer.addView(navView)
        val swSaved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_SW, true)
        sw = swSaved
        // 7. Set content view ONCE
        setContentView(drawer)

        // 8. Initial fragment
        if (savedInstanceState == null) {
            if (sw) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, HomeFragment())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, HomeFragment.DahJalan())
                    .commit()
            }
        }
    }

    fun updateToolbarColor() {
        toolbar.setBackgroundColor(if (sw) Color.parseColor("#2b5f56") else Color.WHITE)
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(R.id.container_fragment)

        when (current) {
            is LogFragment, is AboutFragment -> {
                if (sw) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, HomeFragment())
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, HomeFragment.DahJalan())
                        .commit()
                }
            }
            is HomeFragment.DahJalan -> {
                if (doubleBackToExitPressedOnce1) {
                    handler.removeCallbacks(resetBackPress1)
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                        .putBoolean("sw", false)
                        .apply()
                    finish()
                    return
                }

                doubleBackToExitPressedOnce1 = true
                Toast.makeText(this, "Jika Anda Keluar, Notifikasi Akan Mati", Toast.LENGTH_SHORT).show()
                handler.postDelayed(resetBackPress1, 2000)
            }
            is HomeFragment -> {
                if (doubleBackToExitPressedOnce) {
                    handler.removeCallbacks(resetBackPress)
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                        .putBoolean("sw", true)
                        .apply()
                    finish()
                    return
                }

                doubleBackToExitPressedOnce = true
                Toast.makeText(this, "Jika Anda Keluar, Notifikasi Akan Mati", Toast.LENGTH_SHORT).show()
                handler.postDelayed(resetBackPress, 2000)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove both callbacks
        handler.removeCallbacks(resetBackPress)
        handler.removeCallbacks(resetBackPress1)
    }
}

class AboutFragment : Fragment(R.layout.aboutab) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}

class LogFragment : Fragment(R.layout.log) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}