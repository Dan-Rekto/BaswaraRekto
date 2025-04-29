package com.example.worm
import android.os.Bundle

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.worm.ui.theme.WormTheme
val newsid: String = "a895c9b8cc274495a67cdaec6922fe8c"
val gnews: String = "bd93c4e2fcfe19b6239972ea95dd0512"
val news: String = "13b9c989205c4629861ae9d6e3ceb728"
val AI: String = "AIzaSyC8wJ_8GNj33Xp-pGC6vD6S0JlYB5eg07Y"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R. layout. activity_main)
        val button: Button=findViewById(R.id.Button1)
        val text: TextView=findViewById(R.id.text1)
        text.visibility = View.INVISIBLE
        button.setOnClickListener(){

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
