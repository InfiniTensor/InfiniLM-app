package org.infinitensor.lm

import android.app.Activity
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
import android.content.Context
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Face
import androidx.documentfile.provider.DocumentFile

object ServiceManager {
    private var isInitialized = false

    fun initialize(modelPath: String) {
        if (!isInitialized) {
            Native.init(modelPath)
            isInitialized = true
        }
    }
}


data class Message(var text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {
    companion object {
        init {
            try {
                System.loadLibrary("android")
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        titleBar(name = "Chat")
                    },
                    content = {paddingValues ->
                        mainAPP(Modifier.padding(paddingValues))
                    }
                )
            }
        }

        if (!Environment.isExternalStorageManager()) {
            Toast.makeText(this, "存储权限未被授予", Toast.LENGTH_SHORT).show()
            var intent = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:$packageName"));
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show()
                }
            }.launch(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun titleBar(name: String) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text("九格大模型")
        }
    )
}

@Composable
fun chatScreen(modifier: Modifier){
    var message by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(emptyList<Message>()) }
    var isButtonEnabled by remember { mutableStateOf(true) }
    fun onSubmit() {
        // 开始异步任务
        isButtonEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            if (message.isNotEmpty()) {
                messages += Message(message,true)
                Native.start(message)
                message = ""
            }
            var botMessage = Message("", false)
            messages += botMessage
            while (true) {
                val text = Native.decode()
                if (text.isEmpty()) {
                    break
                }else{
                    withContext(Dispatchers.Main) {
                        botMessage = botMessage.copy(text = botMessage.text + text)
                        // 更新消息列表中的最后一条消息
                        messages = messages.dropLast(1) + botMessage
                    }
                }
            }

            // 任务完成，恢复按钮可点击状态
            withContext(Dispatchers.Main) {
                isButtonEnabled = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 历史聊天记录
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
                .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(12.dp))
        ) {
            messages.forEach { msg ->
                val icon: ImageVector = if (msg.isUser) {
                    Icons.Default.Person  // 用户图标
                } else {
                    Icons.Default.Face  // 机器人图标
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        // 输入框和发送按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.LightGray, shape = MaterialTheme.shapes.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                textStyle = TextStyle(fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {onSubmit()},
                enabled = message.isNotEmpty() && isButtonEnabled
            ) {
                Text("发送")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun chatPreview() {
    LmAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                titleBar(name = "Chat")
            },
            content = {paddingValues ->
                chatScreen(Modifier.padding(paddingValues))
            }
        )
    }
}

@Composable
fun mainAPP(modifier: Modifier) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    var showDialog by remember { mutableStateOf(false) }
    var modelFolderPath by remember { mutableStateOf<String>("") }

    val savedFolderPath = sharedPreferences.getString("defaultModelFolder", "")

    if (savedFolderPath != "" && isValidFolder(savedFolderPath)) {
        Log.i("model path",savedFolderPath.toString() )
        modelFolderPath = savedFolderPath.toString()
    } else {
        showDialog = true
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                modelFolderPath = getAbsolutePathFromUri(context, uri)
                Log.i("model path",modelFolderPath )
                sharedPreferences.edit().putString("defaultModelFolder", modelFolderPath).apply()
                showDialog = false
            }
        }else {
            Toast.makeText(context, "您选择的文件夹无效，请选择一个有效的文件夹。", Toast.LENGTH_SHORT).show()
        }
    }

    if (showDialog) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        FolderSelectionDialog(onSelectFolder = { folderPicker.launch(intent) })
    } else {
        ServiceManager.initialize(modelFolderPath)
        Toast.makeText(context, "模型加载成功", Toast.LENGTH_SHORT).show()
        chatScreen(modifier)
    }

}

@Composable
fun FolderSelectionDialog(onSelectFolder: () -> Unit) {
    Dialog(onDismissRequest = { /* 禁止关闭 */ }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally // 水平居中
                ) {
                    Text("请选择模型文件夹",
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    Button(onClick = onSelectFolder) {
                        Text("选择模型文件夹")
                    }
                }
            }
        }
    }
}

fun isValidFolder(path:String?): Boolean {
    if (path != ""){
        return true;
    }else{
        return false;
    }
}

fun getAbsolutePathFromUri(context: Context, uri: Uri): String {
    val documentFile = DocumentFile.fromTreeUri(context, uri)
    documentFile?.let {
        // 获取文件名
        val displayName = it.name ?: return ""

        // 假设目标路径为 /storage/emulated/0/Download/
        val basePath = "/storage/emulated/0/"

        // 返回拼接好的绝对路径
        return "$basePath$displayName"
    }
    return ""
}