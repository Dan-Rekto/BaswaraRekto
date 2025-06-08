package com.example.worm

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.worm.ui.theme.SecondActivity
import com.example.worm.ui.theme.WormTheme
import com.google.android.material.navigation.NavigationView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        //API kita
        val newsid: String = "a895c9b8cc274495a67cdaec6922fe8c"
        val gnews: String = "bd93c4e2fcfe19b6239972ea95dd0512"
        val news: String = "13b9c989205c4629861ae9d6e3ceb728"
        val AI: String = "AIzaSyC8wJ_8GNj33Xp-pGC6vD6S0JlYB5eg07Y"

        val ctx = this


        // 1. Root DrawerLayout
        val drawer = DrawerLayout(this).apply {
            id = R.id.drawerdrawer
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 2. Konten utama
        val content = FrameLayout(this).apply {
            id = R.id.framedrawer
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT,
                DrawerLayout.LayoutParams.MATCH_PARENT
            )
        }
        val rootView = layoutInflater.inflate(R.layout.activity_main, content, false)
        // 3. Toolbar beserta tombol menu
        val toolbar = Toolbar(this).apply {
            id = R.id.toolbardrawer
            setBackgroundColor(Color.parseColor("#2F5C54"))
            setTitleTextColor(Color.WHITE)

            // tombol hamburger
// pakai icon bawaan dan tint yang benar
            navigationIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_sort_by_size)
            navigationIcon?.setTint(ContextCompat.getColor(context, R.color.tabmenu))
            setNavigationOnClickListener { drawer.openDrawer(Gravity.START) }

        }

        content.addView(toolbar)
        content.addView(rootView)
        // 4. NavigationView
        val navView = NavigationView(this).apply {
            id = R.id.navdrawer
            val width = (resources.displayMetrics.widthPixels * 0.75).toInt()
            layoutParams = DrawerLayout.LayoutParams(width,
                DrawerLayout.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
            setBackgroundColor(Color.parseColor("#5E7D76"))

            // header
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

            // menu item
            menu.add(Menu.NONE, 1, Menu.NONE, "Log Pengecekan")
                .icon = getDrawable(android.R.drawable.ic_menu_search)
            menu.add(Menu.NONE, 2, Menu.NONE, "Tentang Kami")
                .icon = getDrawable(android.R.drawable.ic_menu_info_details)
            menu.add(Menu.NONE, 3, Menu.NONE, "Home")
                .icon = getDrawable(android.R.drawable.ic_media_next)

            setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    3 -> {
                            startActivity(Intent(ctx, MainActivity::class.java))
                        }
                    1 -> { /* Mengenai Log Pengecekan */ }
                    2 -> { /* Mengenai Tentang Kami */ }
                }
                drawer.closeDrawer(Gravity.START)
                true
            }
        }

        // 5. Rangkap ke DrawerLayout
        drawer.addView(content)
        drawer.addView(navView)

        // 6. Tampilkan
        setContentView(drawer)

        val button = findViewById<Button>(R.id.matadalam)
        var laman:Boolean = true
        button.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            var laman:Boolean = true
            startActivity(intent)
        }

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
@Preview(showBackground = true)
@Composable

fun GreetingPreview() {
    WormTheme {
        Greeting("Sinister")
    }
}
