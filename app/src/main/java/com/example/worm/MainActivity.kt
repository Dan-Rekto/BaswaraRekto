package com.example.worm
import android.Manifest

import android.content.Context

import android.content.Intent
import android.content.SharedPreferences

import com.example.worm.R

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
import android.system.Os.remove

import android.text.TextUtils.replace
import android.util.Log

import android.widget.Toast

import android.widget.Button
import android.widget.EditText

import android.widget.FrameLayout

import android.widget.ImageButton
import android.widget.ImageView

import android.widget.LinearLayout

import android.widget.TextView
import androidx.activity.OnBackPressedCallback

import androidx.appcompat.app.AppCompatActivity

import androidx.appcompat.widget.Toolbar

import androidx.activity.enableEdgeToEdge

import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat

import androidx.core.content.ContentProviderCompat.requireContext

import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf

import androidx.core.view.GravityCompat

import androidx.drawerlayout.widget.DrawerLayout

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

import com.example.worm.HomeFragment.DahJalan

import com.example.worm.ui.theme.RunningService.Companion.jalan

import com.example.worm.ui.theme.RunningService

import com.example.worm.HomeFragment

import com.example.worm.ui.theme.OCRTextKeMain

import com.example.worm.ui.theme.WormTheme
import com.example.worm.ui.theme.answerTextKMain
import com.example.worm.ui.theme.boti

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.google.android.material.navigation.NavigationView

import com.google.firebase.FirebaseApp
import com.skydoves.expandablelayout.ExpandableLayout
import com.skydoves.expandablelayout.OnExpandListener


var sw: Boolean = true

var ab: Boolean = false

var sett:Boolean = false

var lg: Boolean = false

var warn: Boolean = true

var warnabtn = Color.parseColor("#2b5f56")

var bush:Boolean = false

var APIkeRunning = ""
var ModelKeRunning = ""
var stopit:Boolean = false

// Add companion object to store log texts

class LogDataManager {

    companion object {

        var logText1: String = ""

        var logText2: String = ""

        var logText3: String = ""



        fun updateLogText(logNumber: Int, text: String) {

            when(logNumber) {

                1 -> logText1 = text

                2 -> logText2 = text

                3 -> logText3 = text

            }

        }



        fun getLogText(logNumber: Int): String {

            return when(logNumber) {

                1 -> logText1

                2 -> logText2

                3 -> logText3

                else -> ""

            }

        }

    }

}



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

            if (warn){

                showUpdateWarning(requireContext())

            }else if (!warn){

                print("hi")

            }

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



    private fun showUpdateWarning(context: Context) {

        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_AlertDialog)

            .setIcon(R.drawable.logo)

            .setTitle("Petunjuk Baswara")

            .setMessage("""

Untuk menggunakan "Scan", Tekan dulu scannya dan langsung tutup Jendela Notifikasi. Karna Aplikasi ini tidak puntya kuasa untuk langsung menutup jendela Notifikasi.

""".trimIndent())

            .setNegativeButton ("Jangan Tampilkan Lagi"){ dialog, _ -> dialog.dismiss(); warn = false }

            .setPositiveButton("Oke") { dialog, _ ->

// your update logic here

                dialog.dismiss()

            }

            .show()

    }

}



class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "app_prefs"

    private val KEY_SW = "sw"
    private var cameFromNotification = false

    var responseTextOnResume: String? = null
    private var doubleBackToExitPressedOnce = false

    private var doubleBackToExitPressedOnce1 = false // For DahJalan
    private var backPressCount = 0
    private val handlerBPC = Handler(Looper.getMainLooper())
    private val resetBackPressCount = Runnable { backPressCount = 0 }
    private val handler = Handler(Looper.getMainLooper())

    private val resetBackPress = Runnable { doubleBackToExitPressedOnce = false }

    private val resetBackPress1 = Runnable { doubleBackToExitPressedOnce1 = false }

    lateinit var toolbar: Toolbar

    private lateinit var drawer: DrawerLayout //Tambahkan variabel drawer sebagai property



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

    override fun onResume(){

        super.onResume()

        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)

        val savedName = sharedPreferences.getBoolean("user_pref", warn)

        warn = savedName
        responseTextOnResume = null
    }



    override fun onPause() {

        super.onPause()

// Storing data in SharedPreferences

        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)

        val editor = sharedPreferences.edit()



// Retrieving user input and saving it

        editor.putBoolean("user_pref", warn)

        editor.apply()
        if(sw){
            supportFragmentManager.beginTransaction()

                .replace(R.id.container_fragment, HomeFragment())

                .commit()

            updateToolbarColor()
        }else if (!sw){
            supportFragmentManager.beginTransaction()

                .replace(R.id.container_fragment, HomeFragment.DahJalan())

                .commit()

            updateToolbarColor()
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleNavigationIntent(intent)
        when (intent.getStringExtra("navigate_to")) {
            RunningService.ACTION_SCREEN -> {
                val scanSvc = Intent(this, RunningService::class.java).apply {
                    action = RunningService.ACTION_SCREEN
                }
                // for Android O+ use startForegroundService
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(this, scanSvc)

                } else {
                    startService(scanSvc)
                }
            }

            "GO_TO_LOG1" -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, LogFragment.Log1())
                    .commitAllowingStateLoss()
                cameFromNotification = true
            }
        }
        when (intent.getStringExtra("mbot")) {
            "langsung" -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_fragment, LogFragment.Log1())
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
                cameFromNotification = true
                updateToolbarColor()
            }
        }

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

        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val newsid: String = "a895c9b8cc274495a67cdaec6922fe8c"

        val gnews: String = "bd93c4e2fcfe19b6239972ea95dd0512"

        val news: String = "13b9c989205c4629861ae9d6e3ceb728"



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            ActivityCompat.requestPermissions(

                this,

                arrayOf(Manifest.permission.POST_NOTIFICATIONS),

                0

            )

        }



        fun Context.dpToPx(dp: Int): Int =

            (dp * resources.displayMetrics.density).toInt()

        if (intent.getBooleanExtra("from_notification", false)) {
            Handler(Looper.getMainLooper()).post {
            }
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



            var btnMenu = Button(this@MainActivity).apply {

                setTitleTextColor(Color.TRANSPARENT)

                setBackgroundColor(Color.TRANSPARENT)

                setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)

                compoundDrawablePadding = 16

                setPadding(dpToPx(5), dpToPx(24), paddingRight, paddingBottom)

                setOnClickListener {

                    if (drawer.isDrawerOpen(GravityCompat.START)) {

                        drawer.closeDrawer(GravityCompat.START)

                    } else {

                        drawer.openDrawer(GravityCompat.START)

                    }

                }

            }



            val params = FrameLayout.LayoutParams(

                FrameLayout.LayoutParams.WRAP_CONTENT,

                FrameLayout.LayoutParams.WRAP_CONTENT

            )

            params.gravity = Gravity.TOP or Gravity.START

            btnMenu.layoutParams = params



            content.addView(btnMenu)



            setPadding(paddingLeft, dpToPx(18), paddingRight, paddingBottom)



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
            menu.add(Menu.NONE, 4, Menu.NONE, "Setting")

                .setIcon(R.drawable.gear_svgrepo_com)
            menu.add(Menu.NONE, 5, Menu.NONE, "Tutorial")

                .setIcon(R.drawable.question)




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

                            .commit()

                        updateToolbarColorlg()

                    }



                    2 -> {

                        ab = false

                        supportFragmentManager.beginTransaction()

                            .replace(R.id.container_fragment, AboutFragment())

                            .commit()

                        updateToolbarColorab()

                    }
                    4 -> {
                        supportFragmentManager.beginTransaction()

                            .replace(R.id.container_fragment, SettingsFragment())

                            .commit()

                        updateToolbarColorsett()
                        sett = false
                    }
                    5-> {

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



// 4️⃣ Only now handle your notification-tap intent

        handleNavigationIntent(intent)

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

    fun updateToolbarColorsett() {

        if (!sett){

            toolbar.setBackgroundColor (Color.WHITE)

            warnabtn = Color.WHITE

        }else if (sett && sw){

            toolbar.setBackgroundColor (Color.WHITE)

            warnabtn = Color.WHITE

        }else{

            toolbar.setBackgroundColor(Color.WHITE)

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


        val currentFragment = supportFragmentManager.findFragmentById(R.id.container_fragment)


        when (current) {

            is LogFragment -> {

                if (sw) {

                    lg = true

                    supportFragmentManager.beginTransaction()

                        .replace(R.id.container_fragment, HomeFragment())

                        .commit()

                    updateToolbarColorlg()

                } else if (!sw){
                    lg = true

                    supportFragmentManager.beginTransaction()

                        .replace(R.id.container_fragment, HomeFragment.DahJalan())

                        .commit()

                    updateToolbarColorlg()

                }

            }
            is SettingsFragment -> {
                if (sw) {
                    sett = true
                    supportFragmentManager.beginTransaction()

                        .replace(R.id.container_fragment, HomeFragment())

                        .commit()
                    toolbar.setBackgroundColor(if (sw) Color.parseColor("#2b5f56") else Color.WHITE)

                } else if (!sw){
                    sett = true
                    supportFragmentManager.beginTransaction()

                        .replace(R.id.container_fragment, HomeFragment.DahJalan())

                        .commit()
                    toolbar.setBackgroundColor(if (sw) Color.parseColor("#2b5f56") else Color.WHITE)

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

        handler.removeCallbacks {resetBackPressCount}
        Intent(this, RunningService::class.java).also { svc ->
            svc.action = RunningService.ACTION_STOP
            startService(svc)
            sw = true
            updateToolbarColor()
            boti = false


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
        view.findViewById<TextView>(R.id.textlog1).setText("${OCRTextKeMain.take(10)}...")

// Set up click listeners for log buttons

        view.findViewById<ImageButton>(R.id.Log1)?.setOnClickListener {

            parentFragmentManager.beginTransaction()

                .replace(R.id.container_fragment, Log1())

                .addToBackStack(null)

                .commit()

            lg = true

            (activity as? MainActivity)?.toolbar?.setBackgroundColor( Color.WHITE)

        }



        view.findViewById<ImageButton>(R.id.Log2)?.setOnClickListener {

            parentFragmentManager.beginTransaction()

                .replace(R.id.container_fragment, Log2())

                .addToBackStack(null)

                .commit()

            lg = true

            (activity as? MainActivity)?.updateToolbarColorlg()

        }



        view.findViewById<ImageButton>(R.id.Log3)?.setOnClickListener {

            parentFragmentManager.beginTransaction()

                .replace(R.id.container_fragment, Log3())

                .addToBackStack(null)

                .commit()

            lg = true

            (activity as? MainActivity)?.updateToolbarColorlg()

        }

    }



    class Log1 : Fragment(R.layout.log1) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            boti = true
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("DELETENOTIF", "UDAH TERDELETE")
            NotificationManagerCompat
                .from(requireContext())
                .cancel(100009)
            }, 60000L)
            // Populate your views
            view.findViewById<TextView>(R.id.jdulLOG1BL)
                .text = answerTextKMain.take(6)
            view.findViewById<TextView>(R.id.penjelasanLOG1BL)
                .text = answerTextKMain
            // === BACK PRESS HANDLER FOR *THIS* FRAGMENT ===
            requireActivity()
                .onBackPressedDispatcher
                .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // Redirect to DahJalan (always, regardless of cameFromNotification)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.container_fragment, HomeFragment.DahJalan())
                            .commitAllowingStateLoss()

                        // Update the toolbar color via your activity
                        (activity as? MainActivity)?.updateToolbarColor()

                        // Clear any flags if needed
                        (activity as? MainActivity)?.cameFromNotification = false
                        sw = false
                        boti = false
                    }
                })
        }
    }
}


    class Log2 : Fragment(R.layout.log2) {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

            super.onViewCreated(view, savedInstanceState)


// Set the text from LogDataManager

            view.findViewById<TextView>(R.id.textlog2)?.text = LogDataManager.getLogText(2)

        }

    }


    class Log3 : Fragment(R.layout.log3) {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

            super.onViewCreated(view, savedInstanceState)


// Set the text from LogDataManager

            view.findViewById<TextView>(R.id.textlog3)?.text = LogDataManager.getLogText(3)

        }

    }
}


class SettingsFragment : Fragment(R.layout.setting) {

    private val PREFS_NAME      = "settings_prefs"
    private val KEY_MODEL       = "selected_model"
    private val KEY_API_KEY     = "api_key"
    private var selectedModel   = "gemini-1.5"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // --- 0) load saved values ---
        selectedModel = prefs.getString(KEY_MODEL, selectedModel) ?: selectedModel
        val savedApiKey = prefs.getString(KEY_API_KEY, "") ?: ""

        // --- API key UI wiring ---
        val editText = view.findViewById<EditText>(R.id.editText)
        val button   = view.findViewById<Button>(R.id.button)
        editText.setText(savedApiKey)

        button.setOnClickListener {
            val newKey = editText.text.toString().trim()
            APIkeRunning = newKey
            prefs.edit()
                .putString(KEY_API_KEY, newKey)
                .apply()
            Toast.makeText(requireContext(), "API key saved", Toast.LENGTH_SHORT).show()
            Log.d("Jancok", APIkeRunning)
        }

        // --- expandable layout wiring ---
        val expandable     = view.findViewById<ExpandableLayout>(R.id.expandableModels)
        val parentText     = view.findViewById<TextView>(R.id.parentText)
        val parentSpinner  = view.findViewById<ImageView>(R.id.parentSpinner)

        // toggle on header tap
        view.findViewById<View>(R.id.parentLayout).setOnClickListener {
            expandable.toggleLayout()
        }

        // rotate arrow on expand/collapse
        expandable.setOnExpandListener { isExpanded ->
            parentSpinner.rotation = if (isExpanded) 180f else 0f
        }

        // wire up each choice
        view.findViewById<TextView>(R.id.opt15).setOnClickListener {
            onModelSelected("gemini-1.5", parentText, expandable, prefs)
            selectedModel   = "gemini-1.5"
        }
        view.findViewById<TextView>(R.id.opt20lite).setOnClickListener {
            onModelSelected("gemini-2.0-flash-lite", parentText, expandable, prefs)
            selectedModel   = "gemini-2.0-flash-lite"
        }
        view.findViewById<TextView>(R.id.opt20).setOnClickListener {
            onModelSelected("gemini-2.0", parentText, expandable, prefs)
            selectedModel   = "gemini-2.0"
        }
        view.findViewById<TextView>(R.id.opt25).setOnClickListener {
            onModelSelected("gemini-2.5-flash", parentText, expandable, prefs)
            selectedModel   = "gemini-2.5-flash"
        }

        // initialize header text
        parentText.text = "Model: $selectedModel"
    }

    private fun onModelSelected(
        model: String,
        header: TextView,
        expandable: ExpandableLayout,
        prefs: SharedPreferences
    ) {
        ModelKeRunning = selectedModel
        // save to prefs
        prefs.edit()
            .putString(KEY_MODEL, selectedModel)
            .apply()

        header.text = "Model: $selectedModel"
        expandable.collapse()
        Toast.makeText(requireContext(), "Selected $ModelKeRunning", Toast.LENGTH_SHORT).show()
    }
}




