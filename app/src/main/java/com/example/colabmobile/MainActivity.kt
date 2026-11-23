package com.example.colabmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val bg = Color(0xFF10131A)
private val panel = Color(0xFF1B2230)
private val accent = Color(0xFF8BB8FF)

data class NotebookCell(val id: String = UUID.randomUUID().toString(), val type: String = "code", val source: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ColabApp(
            openColab = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://colab.research.google.com/"))) },
            readFile = { uri -> contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("無法讀取檔案") },
            writeFile = { uri, text -> contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) } ?: error("無法寫入檔案") }
        ) }
    }
}

private fun fromJson(raw: String): List<NotebookCell> {
    val root = JSONObject(raw)
    val cells = root.getJSONArray("cells")
    return (0 until cells.length()).map { index ->
        val obj = cells.getJSONObject(index)
        val src = obj.opt("source")
        val value = when (src) {
            is JSONArray -> (0 until src.length()).joinToString("") { src.optString(it) }
            is String -> src
            else -> ""
        }
        NotebookCell(type = obj.optString("cell_type", "code").let { if (it == "markdown") "markdown" else "code" }, source = value)
    }
}

private fun toJson(cells: List<NotebookCell>): String {
    val array = JSONArray()
    cells.forEach { cell ->
        val lines = JSONArray()
        cell.source.splitToSequence("\n").toList().forEachIndexed { i, line ->
            lines.put(if (i < cell.source.count { it == '\n' }) "$line\n" else line)
        }
        val obj = JSONObject().put("cell_type", cell.type).put("metadata", JSONObject()).put("source", lines)
        if (cell.type == "code") obj.put("execution_count", JSONObject.NULL).put("outputs", JSONArray())
        array.put(obj)
    }
    return JSONObject().put("cells", array).put("metadata", JSONObject().put("kernelspec", JSONObject().put("display_name", "Python 3").put("language", "python").put("name", "python3"))).put("nbformat", 4).put("nbformat_minor", 5).toString(2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColabApp(openColab: () -> Unit, readFile: (Uri) -> String, writeFile: (Uri, String) -> Unit) {
    val cells = remember { mutableStateListOf(NotebookCell(source = "print('Hello, Colab Mobile!')")) }
    var filename by remember { mutableStateOf("Untitled.ipynb") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("離線編輯模式：執行請使用官方 Colab") }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val parsed = fromJson(readFile(uri))
            cells.clear(); cells.addAll(parsed); fileUri = uri
            filename = uri.lastPathSegment?.substringAfterLast('/') ?: "Notebook.ipynb"
            message = "已載入 ${cells.size} 個儲存格"
        } catch (e: Exception) { message = "開啟失敗：${e.message}" }
    }
    val saveAs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-ipynb+json")) { uri ->
        if (uri != null) try { writeFile(uri, toJson(cells)); fileUri = uri; message = "已儲存筆記本" }
        catch (e: Exception) { message = "儲存失敗：${e.message}" }
    }
    fun save() {
        val uri = fileUri
        if (uri == null) saveAs.launch(filename)
        else try { writeFile(uri, toJson(cells)); message = "已儲存筆記本" }
        catch (e: Exception) { message = "儲存失敗：${e.message}，請使用另存新檔" }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = panel, onSurface = Color.White)) {
        Scaffold(containerColor = bg, topBar = {
            TopAppBar(title = { Column { Text("Colab Mobile", fontWeight = FontWeight.Bold); Text(filename, style = MaterialTheme.typography.labelSmall, color = Color.LightGray) } },
                actions = {
                    IconButton(onClick = { open.launch(arrayOf("application/json", "application/x-ipynb+json", "*/*")) }) { Icon(Icons.Default.FolderOpen, "開啟") }
                    IconButton(onClick = { save() }) { Icon(Icons.Default.Save, "儲存") }
                    IconButton(onClick = { saveAs.launch(filename) }) { Icon(Icons.Default.SaveAs, "另存新檔") }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = panel))
        }, bottomBar = {
            Surface(color = panel) { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(message, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { cells.add(NotebookCell()) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("程式碼") }
                    OutlinedButton(onClick = { cells.add(NotebookCell(type = "markdown")) }, modifier = Modifier.weight(1f)) { Text("+ Markdown") }
                    OutlinedButton(onClick = openColab) { Icon(Icons.Default.OpenInBrowser, "開啟 Colab") }
                }
            } }
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Jupyter Notebook 編輯器", style = MaterialTheme.typography.titleMedium, color = Color.White); Text("本機編輯與儲存；雲端執行請開啟 Colab 並上傳筆記本。", color = Color.LightGray, style = MaterialTheme.typography.bodySmall) }
                itemsIndexed(cells, key = { _, cell -> cell.id }) { index, cell ->
                    Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${index + 1} · ${if (cell.type == "code") "Python" else "Markdown"}", modifier = Modifier.weight(1f), color = accent, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { if (index > 0) { val temp = cells[index - 1]; cells[index - 1] = cell; cells[index] = temp } }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ArrowUpward, "上移", tint = Color.LightGray) }
                                IconButton(onClick = { if (index < cells.lastIndex) { val temp = cells[index + 1]; cells[index + 1] = cell; cells[index] = temp } }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ArrowDownward, "下移", tint = Color.LightGray) }
                                IconButton(onClick = { cells[index] = cell.copy(type = if (cell.type == "code") "markdown" else "code") }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.SwapHoriz, "切換類型", tint = Color.LightGray) }
                                IconButton(onClick = { cells.removeAt(index) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.DeleteOutline, "刪除", tint = Color.LightGray) }
                            }
                            Spacer(Modifier.height(8.dp))
                            BasicTextField(value = cell.source, onValueChange = { cells[index] = cell.copy(source = it) }, modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).background(bg, RoundedCornerShape(8.dp)).padding(12.dp), textStyle = TextStyle(color = Color(0xFFE5F0FF), fontFamily = FontFamily.Monospace), cursorBrush = androidx.compose.ui.graphics.SolidColor(accent), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), decorationBox = { inner -> Box { if (cell.source.isEmpty()) Text("在此輸入 ${cell.type} ...", color = Color.Gray); inner() } })
                            if (cell.type == "code") {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("    ", "()", "[]", ":", "=", "\n").forEach { token ->
                                        AssistChip(onClick = { cells[index] = cell.copy(source = cell.source + token) }, label = { Text(if (token == "\n") "↵" else if (token == "    ") "Tab" else token) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
