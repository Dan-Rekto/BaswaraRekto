package com.example.worm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.os.Handler
import android.os.Looper
import android.text.TextUtils.replace
import android.widget.Toast
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.worm.HomeFragment.DahJalan
import com.example.worm.ui.theme.RunningService.Companion.jalan
import com.example.worm.ui.theme.RunningService
import com.example.worm.HomeFragment
import com.example.worm.ui.theme.WormTheme
import com.google.android.material.navigation.NavigationView

var sw: Boolean = true
var ab: Boolean = false
var lg: Boolean = false
var warnabtn = Color.parseColor("#2b5f56")
class HomeFragment : Fragment(R.layout.activity_main_uji) {
    override fun onViewCreated(v: View, b: Bundle?) {
        super.onViewCreated(v, b)
        v.findViewById<Button>(R.id.matadalam).setOnClickListener {
            sw = false
            parentFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, DahJalan())
                .commit()
            (activity as? MainActivity)?.updateToolbarColor()
            // 3) Mulai service
            (activity as? MainActivity)?.requestScreenCapturePermission()

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
                (activity as? MainActivity)?.stopScreenCapture()
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val PREFS_NAME = "app_prefs"
    private val KEY_SW = "sw"
    private var doubleBackToExitPressedOnce = false
    private var doubleBackToExitPressedOnce1 = false  // For DahJalan
    private val handler = Handler(Looper.getMainLooper())
    private val resetBackPress = Runnable { doubleBackToExitPressedOnce = false }
    private val resetBackPress1 = Runnable { doubleBackToExitPressedOnce1 = false }
    lateinit var toolbar: Toolbar
    private lateinit var drawer: DrawerLayout // Tambahkan variabel drawer sebagai property

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private fun handleNavigationIntent(intent: Intent) {
        when (intent.getStringExtra("navigate_to")) {
            "stop_and_home" -> {
                // 1) Stop service
                Intent(this, RunningService::class.java).also { svc ->
                    svc.action = RunningService.ACTION_STOP
                    startService(svc)
                }
                // 2) Navigate ke HomeFragment
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, HomeFragment())
                    .commitAllowingStateLoss()
                sw = true
                // 3) Update toolbar
                updateToolbarColor()
                Toast.makeText(this, "Notifikasi Dimatikan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }


    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            // Izin diberikan! Sekarang kita mulai service DAN perbarui UI.
            sw = false

            val serviceIntent = Intent(this, RunningService::class.java).apply {
                action = RunningService.ACTION_START
                putExtra(RunningService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RunningService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)

            supportFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, HomeFragment.DahJalan())
                .commit()
            updateToolbarColor()

        } else {
            Toast.makeText(this, "Izin merekam layar ditolak!", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestScreenCapturePermission() {
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    fun stopScreenCapture() {
        sw = true

        val intent = Intent(this, RunningService::class.java)
        intent.action = RunningService.ACTION_STOP
        startService(intent)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_fragment, HomeFragment())
            .commit()
        updateToolbarColor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
        sw = true
        enableEdgeToEdge()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val newsid: String = "a895c9b8cc274495a67cdaec6922fe8c"
        val gnews: String = "bd93c4e2fcfe19b6239972ea95dd0512"
        val news: String = "13b9c989205c4629861ae9d6e3ceb728"
        val AI: String = "AIzaSyC8wJ_8GNj33Xp-pGC6vD6S0JlYB5eg07Y"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        fun Context.dpToPx(dp: Int): Int =
            (dp * resources.displayMetrics.density).toInt()

        if (RunningService.jalan){
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, HomeFragment.DahJalan())
                .commit()
        }else{
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, HomeFragment())
                .commit()
        }

        // 1. Root DrawerLayout
        drawer = DrawerLayout(this).apply { // Ubah val menjadi assignment ke property
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

            val icon = ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_sort_by_size)
            icon?.setTint(ContextCompat.getColor(this@MainActivity, R.color.tabmenu))

// 2. Buat button
            var btnMenu = Button(this@MainActivity).apply {
                setTitleTextColor(Color.TRANSPARENT)
                setBackgroundColor(Color.TRANSPARENT)
                // Set icon di kiri teks tombol
                setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
                compoundDrawablePadding = 16 // jarak antara icon dan text
                setPadding(dpToPx(5), dpToPx(24), paddingRight, paddingBottom)
                setOnClickListener {
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        drawer.closeDrawer(GravityCompat.START)
                    } else {
                        drawer.openDrawer(GravityCompat.START)
                    }
                }
            }

// 3. Atur posisi tombol
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            btnMenu.layoutParams = params

// 4. Tambahkan ke layout
            content.addView(btnMenu)

            // Tambahkan padding untuk menurunkan icon
            setPadding(paddingLeft, dpToPx(18), paddingRight, paddingBottom) // 8dp padding top

            val tv = TypedValue()
            this@MainActivity.theme.resolveAttribute(
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

        // 5. Navigation drawer
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
            val home = (R.drawable.home_svgrepo_com)
            addHeaderView(header)
            menu.add(Menu.NONE, 3, Menu.NONE, "Home")
                .setIcon(R.drawable.home_svgrepo_com)
            menu.add(Menu.NONE, 1, Menu.NONE, "Log Pengecekan")
                .icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_search)
            menu.add(Menu.NONE, 2, Menu.NONE, "Tentang Kami")
                .icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_info_details)


            setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    3 -> if (sw) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container_fragment, HomeFragment())
                            .commit()
                        updateToolbarColor()
                    } else {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container_fragment, HomeFragment.DahJalan())
                            .commit()
                        updateToolbarColor()
                    }

                    1 -> {
                        lg = false
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container_fragment, LogFragment())
                            .addToBackStack(null)
                            .commit()
                        updateToolbarColorlg()
                    }

                    2 -> {
                        ab = false
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container_fragment, AboutFragment())
                            .addToBackStack(null)
                            .commit()
                        updateToolbarColorab()
                    }
                }
                drawer.closeDrawer(GravityCompat.START)
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
        if (savedInstanceState == null)
        {
            if (sw) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, HomeFragment())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, HomeFragment.DahJalan())
                    .commit()
            }
            handleNavigationIntent(intent)
        }
    }

    fun updateToolbarColor() {
        toolbar.setBackgroundColor(if (sw) Color.parseColor("#2b5f56") else Color.WHITE)
        if (sw){
            warnabtn = Color.parseColor("#2b5f56")
        }else{
            warnabtn = Color.WHITE
        }
    }
    fun updateToolbarColorlg() {
        if (!lg){
            toolbar.setBackgroundColor (Color.parseColor("#2b5f56"))
            warnabtn = Color.parseColor("#2b5f56")
        }else if (lg && sw){
            toolbar.setBackgroundColor (Color.parseColor("#2b5f56"))
            warnabtn = Color.parseColor("#2b5f56")
        }else{
            toolbar.setBackgroundColor(Color.WHITE)
            warnabtn = Color.WHITE
        }
    }
    fun updateToolbarColorab() {
        if (!ab){
            toolbar.setBackgroundColor (Color.parseColor("#2b5f56"))
            warnabtn = Color.parseColor("#2b5f56")
        }else if (ab && sw){
            toolbar.setBackgroundColor (Color.parseColor("#2b5f56"))
            warnabtn = Color.parseColor("#2b5f56")
        }else{
            toolbar.setBackgroundColor(Color.WHITE)
            warnabtn = Color.WHITE
        }
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(R.id.container_fragment)

        when (current) {
            is LogFragment -> {
                if (sw) {
                    lg = true
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, HomeFragment())
                        .commit()
                    updateToolbarColorlg()
                } else {
                    lg = true
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, HomeFragment.DahJalan())
                        .commit()
                    updateToolbarColorlg()
                }
            }
            is AboutFragment -> {
                if (sw) {
                    ab = true
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, HomeFragment())
                        .commit()
                    updateToolbarColorab()
                } else {
                    ab = true
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container_fragment, HomeFragment.DahJalan())
                        .commit()
                    updateToolbarColorab()
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
                Toast.makeText(this, "Ketuk Sekali Lagi Untuk Keluar", Toast.LENGTH_SHORT).show()
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
        stopScreenCapture()
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