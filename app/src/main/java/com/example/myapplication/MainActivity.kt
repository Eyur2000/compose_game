package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.random.Random

data class InData(
    val type: String,
    val difficulty: String,
    val category: String,
    val question: String,
    val correct_answer: String,
    val incorrect_answers: List<String>
)

data class OutData(
    val response_code: Int,
    val results: List<InData>
)

class MainActivity : ComponentActivity() {
    private val Client = OkHttpClient.Builder().build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var isLoading by remember { mutableStateOf(true) }
                var post by remember { mutableStateOf<List<InData>>(emptyList()) }
                LaunchedEffect(Unit) {
                    val result = getData(Client)
                    if (result != null) {
                        post = result.results
                    }
                    isLoading = false
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Main(
                        isLoading = isLoading,
                        post = post,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Main(isLoading: Boolean, post: List<InData>, modifier: Modifier = Modifier) {
    var hp by remember { mutableIntStateOf(3) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var time by remember { mutableIntStateOf(10) }
    if (hp > 0 || currentIndex < post.size) {
        LaunchedEffect(currentIndex) {
            time = 10
        }
    }
    if (hp > 0 || currentIndex < post.size) {
        LaunchedEffect(hp, currentIndex) {
            while (time > 0) {
                delay(1000L)
                time--
            }
            // 時間到：扣血並自動跳下一題
            hp--
            currentIndex++
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(),contentAlignment = Alignment.Center){
                Text("Loading....")
            }
        } else if (hp <= 0) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("game Over")
                Button({
                    hp=3
                    score=0
                    currentIndex=0
                    time=10
                }) {
                    Text("Again")
                }
            }

        } else {
            val nowData = post[currentIndex]
            val choose = remember(nowData) {
                (nowData.incorrect_answers + nowData.correct_answer).shuffled()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("題目${currentIndex + 1}:${nowData.question}")
                    Spacer(Modifier.height(50.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(choose) { index ->
                            Button({
                                if (index == nowData.correct_answer) {
                                    score += 10
                                    currentIndex++
                                } else {
                                    hp--
                                    currentIndex++
                                }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(index)

                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }

        }
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Text("Score:$score")
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Text("Hp:$hp")
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Text("time:$time")
    }
}

suspend fun getData(client: OkHttpClient): OutData? = withContext(Dispatchers.IO) {
    val request = Request.Builder().url("https://opentdb.com/api.php?amount=10").build()
    try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val jsonString = response.body.string()
                Gson().fromJson(jsonString, OutData::class.java)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        print(e)
        null
    }
}

/*
@Composable
fun Main(isLoading: Boolean, post: List<InData>, modifier: Modifier = Modifier) {
    var hp by remember { mutableIntStateOf(3) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var time by remember { mutableIntStateOf(10) }
    var score by remember { mutableIntStateOf(0) }

    // 遊戲主計時器
    LaunchedEffect(key1 = hp, key2 = currentIndex) {
        if (hp > 0 && !isLoading && post.isNotEmpty()) {
            time = 10 // 切換題目或扣血後重置時間
            while (time > 0) {
                delay(1000L)
                time--
            }
            hp-- // 時間到扣血
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            Text("載入中...")
        } else if (hp <= 0) {
            Text("遊戲結束！最終得分：$score")
            Button(onClick = { /* 這裡可以實作重開邏輯 */ }) {
                Text("重新開始")
            }
        } else if (post.isNotEmpty()) {
            val currentQuestion = post[currentIndex]

            // 顯示狀態
            Row {
                Text("生命值: $hp  ")
                Text("剩餘時間: $time  ")
                Text("得分: $score")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 顯示題目 (注意：OpenTDB 的文字通常包含 HTML 實體編碼，實務上需處理)
            Text(text = "題目: ${currentQuestion.question}")

            // 這裡可以加入選項按鈕
            val options = remember(currentQuestion) {
                (currentQuestion.incorrect_answers + currentQuestion.correct_answer).shuffled()
            }

            options.forEach { answer ->
                Button(
                    onClick = {
                        if (answer == currentQuestion.correct_answer) {
                            score += 10
                        } else {
                            hp--
                        }
                        // 進入下一題
                        if (currentIndex < post.size - 1) {
                            currentIndex++
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(answer)
                }
            }
        }
    }
}
 */