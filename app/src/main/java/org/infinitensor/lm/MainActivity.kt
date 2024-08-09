package org.infinitensor.lm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.infinitensor.lm.ui.theme.LmAndroidTheme

class MainActivity : ComponentActivity() {
    companion object {
        init {
            try {
                System.loadLibrary("infinilm_chat")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("Native library not found", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LmAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        if (!Environment.isExternalStorageManager()) {
            Toast.makeText(this, "存储权限未被授予", Toast.LENGTH_SHORT).show()
            var intent = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:$packageName"));
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                when (result.resultCode) {
                    RESULT_OK -> Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this, "授权失败", Toast.LENGTH_LONG).show()
                }
            }.launch(intent)
        }

        Native.init("/storage/emulated/0/Download/tinyllama_F16/")
        Log.i("Native", "loaded")
        Toast.makeText(this, "模型已加载", Toast.LENGTH_SHORT).show()
        Native.start("Tell a story")
        Log.i("Native", "started")
        while (true) {
            val text = Native.decode()
            Log.i("Native", "ans = $text")
            if (text.isEmpty()) {
                break
            }
        }
        Log.i("Native", "over")
        Toast.makeText(this, "生成结束", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello, $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LmAndroidTheme {
        Greeting("Android")
    }
}
