package jp.komagata.tweektext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TweekTextTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EditorApp()
                }
            }
        }
    }
}

private enum class LineEnding(val value: String) {
    LF("\n"),
    CRLF("\r\n"),
    CR("\r"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val editorState = rememberTextFieldState()

    var currentUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var fileName by rememberSaveable { mutableStateOf("Untitled.txt") }
    var savedText by rememberSaveable { mutableStateOf("") }
    var lineEnding by rememberSaveable { mutableStateOf(LineEnding.LF) }
    var menuOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var replaceOpen by remember { mutableStateOf(false) }
    var pendingSaveAfterCreate by remember { mutableStateOf(false) }

    fun currentText(): String = editorState.text.toString()

    fun setEditorText(text: String) {
        editorState.edit {
            replace(0, length, text)
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun saveTo(uri: Uri) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(currentText().normalizeLineEndings(lineEnding).toByteArray(Charsets.UTF_8))
            } ?: error("Cannot open output stream")
        }.onSuccess {
            currentUri = uri
            savedText = currentText()
            fileName = context.displayName(uri) ?: fileName
            showMessage("Saved")
        }.onFailure {
            showMessage("Save failed: ${it.localizedMessage ?: "unknown error"}")
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val rawText = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: ""
            val detectedLineEnding = rawText.detectLineEnding()
            val normalizedText = rawText.replace("\r\n", "\n").replace("\r", "\n")
            setEditorText(normalizedText)
            savedText = normalizedText
            lineEnding = detectedLineEnding
            currentUri = uri
            fileName = context.displayName(uri) ?: "Untitled.txt"
        }.onSuccess {
            showMessage("Opened")
        }.onFailure {
            showMessage("Open failed: ${it.localizedMessage ?: "unknown error"}")
        }
    }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        currentUri = uri
        fileName = context.displayName(uri) ?: fileName
        if (pendingSaveAfterCreate) {
            pendingSaveAfterCreate = false
            saveTo(uri)
        }
    }

    fun createDocument() {
        createLauncher.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, fileName.ifBlank { "Untitled.txt" })
            },
        )
    }

    fun save() {
        currentUri?.let(::saveTo) ?: run {
            pendingSaveAfterCreate = true
            createDocument()
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fileName)
                        if (currentText() != savedText) {
                            Text("Unsaved changes", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        setEditorText("")
                        savedText = ""
                        currentUri = null
                        fileName = "Untitled.txt"
                        lineEnding = LineEnding.LF
                    }) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "New")
                    }
                },
                actions = {
                    IconButton(onClick = { openLauncher.launch(arrayOf("text/*", "application/json", "application/xml", "*/*")) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open")
                    }
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Save as") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                pendingSaveAfterCreate = true
                                createDocument()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Replace") },
                            leadingIcon = { Icon(Icons.Filled.FindReplace, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                replaceOpen = true
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        OutlinedTextField(
            value = currentText(),
            onValueChange = { setEditorText(it) },
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp)
                .fillMaxSize()
                .imePadding(),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text("Plain text") },
            singleLine = false,
        )
    }

    if (searchOpen) {
        SearchDialog(
            text = currentText(),
            onDismiss = { searchOpen = false },
        )
    }

    if (replaceOpen) {
        ReplaceDialog(
            text = currentText(),
            onReplace = { search, replacement ->
                val updated = currentText().replace(search, replacement)
                setEditorText(updated)
            },
            onDismiss = { replaceOpen = false },
        )
    }
}

@Composable
private fun SearchDialog(
    text: String,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val count = remember(text, query) {
        if (query.isBlank()) 0 else Regex.escape(query).toRegex().findAll(text).count()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
        title = { Text("Search") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Text") },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                )
                Text("$count matches")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun ReplaceDialog(
    text: String,
    onReplace: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    val count = remember(text, search) {
        if (search.isBlank()) 0 else Regex.escape(search).toRegex().findAll(text).count()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.FindReplace, contentDescription = null) },
        title = { Text("Replace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Find") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text("Replace with") },
                    singleLine = true,
                )
                Text("$count matches")
            }
        },
        confirmButton = {
            Button(
                enabled = search.isNotEmpty(),
                onClick = {
                    onReplace(search, replacement)
                    onDismiss()
                },
            ) {
                Text("Replace all")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TweekTextTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.compose.foundation.isSystemInDarkTheme()) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
        },
        content = content,
    )
}

private fun String.detectLineEnding(): LineEnding =
    when {
        contains("\r\n") -> LineEnding.CRLF
        contains("\r") -> LineEnding.CR
        else -> LineEnding.LF
    }

private fun String.normalizeLineEndings(lineEnding: LineEnding): String =
    replace("\r\n", "\n").replace("\r", "\n").replace("\n", lineEnding.value)

private fun Context.displayName(uri: Uri): String? {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }
    return uri.lastPathSegment
}
