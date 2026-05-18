package com.zero.zero_tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zero.zero_tools.ui.theme.ZerotoolsTheme
import com.zero.zero_tools.zeroui.host.ZeroUiHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZerotoolsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ZeroUiHost(
                        startPage = "showcase",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ZeroUiHostPreview() {
    ZerotoolsTheme {
        ZeroUiHost(startPage = "showcase")
    }
}
