package io.github.itsmelissadev.swiftsense.feature.systemtables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class SystemTable(val key: String) {
    SYSTEM("system"),
    SECURE("secure"),
    GLOBAL("global")
}

data class MacroSetting(
    val table: SystemTable,
    val key: String,
    val defaultValue: String,
    val targetValue: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("table", table.name)
            put("key", key)
            put("defaultValue", defaultValue)
            put("targetValue", targetValue)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): MacroSetting {
            return MacroSetting(
                SystemTable.valueOf(obj.getString("table")),
                obj.getString("key"),
                obj.getString("defaultValue"),
                obj.getString("targetValue")
            )
        }
    }
}

data class SystemMacro(
    val name: String,
    val settings: List<MacroSetting>
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("name", name)
            val settingsArray = JSONArray()
            settings.forEach { settingsArray.put(it.toJsonObject()) }
            put("settings", settingsArray)
        }.toString()
    }

    companion object {
        fun fromJson(json: String): SystemMacro {
            val obj = JSONObject(json)
            val settingsList = mutableListOf<MacroSetting>()
            val settingsArray = obj.optJSONArray("settings")
            if (settingsArray != null) {
                for (i in 0 until settingsArray.length()) {
                    settingsList.add(MacroSetting.fromJsonObject(settingsArray.getJSONObject(i)))
                }
            } else {
                // Migration for old single setting macros
                settingsList.add(
                    MacroSetting(
                        SystemTable.valueOf(obj.getString("table")),
                        obj.getString("key"),
                        obj.getString("defaultValue"),
                        obj.getString("targetValue")
                    )
                )
            }
            return SystemMacro(obj.getString("name"), settingsList)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemTableMacroScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.macros), stringResource(R.string.tab_viewer))
    
    var selectedTable by remember { mutableStateOf(SystemTable.SYSTEM) }
    var searchQuery by remember { mutableStateOf("") }

    // Editor State
    var showEditor by remember { mutableStateOf(false) }
    var macroToEdit by remember { mutableStateOf<SystemMacro?>(null) }
    var originalMacroJson by remember { mutableStateOf<String?>(null) }

    // UI states
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    var editingSetting by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    val macrosJson by preferenceManager.systemMacros.collectAsState(initial = emptySet())
    val macros = remember(macrosJson) { macrosJson.map { SystemMacro.fromJson(it) } }

    var tableData by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val refreshTable = {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val result = ShizukuShellRunner.runCommand("settings list ${selectedTable.key}")
            if (result.isSuccess) {
                val output = result.getOrNull() ?: ""
                tableData = output.lines()
                    .filter { it.contains("=") }
                    .map { 
                        val parts = it.split("=", limit = 2)
                        parts[0] to parts[1]
                    }
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTable) {
        refreshTable()
    }

    AnimatedContent(targetState = showEditor, label = "ScreenTransition") { isEditing ->
        if (isEditing) {
            BackHandler {
                showEditor = false
                macroToEdit = null
                originalMacroJson = null
            }
            MacroEditorPage(
                macro = macroToEdit,
                onDismiss = {
                    showEditor = false
                    macroToEdit = null
                    originalMacroJson = null
                },
                onSave = { newMacro ->
                    scope.launch {
                        if (originalMacroJson != null) {
                            preferenceManager.updateSystemMacro(
                                originalMacroJson!!,
                                newMacro.toJson()
                            )
                        } else {
                            preferenceManager.addSystemMacro(newMacro.toJson())
                        }
                        showEditor = false
                        macroToEdit = null
                        originalMacroJson = null
                    }
                }
            )
        } else {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.feature_system_tables),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            ),
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                            },
                            actions = {
                                if (selectedTabIndex == 0) {
                                    IconButton(onClick = {
                                        macroToEdit = null
                                        originalMacroJson = null
                                        showEditor = true
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }

                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = null)
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.import_macros)) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.FileUpload,
                                                        null
                                                    )
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    showImportDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.export_macros)) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.FileDownload,
                                                        null
                                                    )
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    showExportDialog = true
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    IconButton(onClick = { refreshTable() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                    }
                                }
                            }
                        )
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ShizukuStatusWidget()
                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedTabIndex) {
                        0 -> { // Macros Tab
                            if (macros.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.no_macros),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(macros) { macro ->
                                        MacroItem(
                                            macro = macro,
                                            onApply = {
                                                scope.launch {
                                                    var success = true
                                                    macro.settings.forEach { setting ->
                                                        val cmd =
                                                            "settings put ${setting.table.key} ${setting.key} ${setting.targetValue}"
                                                        val result =
                                                            ShizukuShellRunner.runCommand(cmd)
                                                        if (!result.isSuccess) success = false
                                                    }
                                                    if (success) {
                                                        Toast.makeText(
                                                            context,
                                                            R.string.toast_macro_applied,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            },
                                            onRevert = {
                                                scope.launch {
                                                    var success = true
                                                    macro.settings.forEach { setting ->
                                                        val cmd =
                                                            "settings put ${setting.table.key} ${setting.key} ${setting.defaultValue}"
                                                        val result =
                                                            ShizukuShellRunner.runCommand(cmd)
                                                        if (!result.isSuccess) success = false
                                                    }
                                                    if (success) {
                                                        Toast.makeText(
                                                            context,
                                                            R.string.toast_macro_reverted,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            },
                                            onEdit = {
                                                macroToEdit = macro
                                                originalMacroJson = macro.toJson()
                                                showEditor = true
                                            },
                                            onDelete = {
                                                scope.launch {
                                                    preferenceManager.removeSystemMacro(
                                                        macro.toJson()
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        1 -> { // Table Viewer Tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                var expanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedTable.name,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.select_table)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expanded
                                            )
                                        },
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        SystemTable.entries.forEach { table ->
                                            DropdownMenuItem(
                                                text = { Text(table.name) },
                                                onClick = {
                                                    selectedTable = table
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(stringResource(R.string.search_table)) },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else {
                                    val filteredData = tableData.filter {
                                        it.first.contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ) || it.second.contains(searchQuery, ignoreCase = true)
                                    }
                                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        items(filteredData) { (key, value) ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(vertical = 4.dp).clickable {
                                                        editingSetting = key to value
                                                    },
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.3f
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        key,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        value,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
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
        }
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_macros)) },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text(stringResource(R.string.import_json_hint)) },
                    shape = MaterialTheme.shapes.medium
                )
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val array = JSONArray(jsonInput)
                        scope.launch {
                            for (i in 0 until array.length()) {
                                val macroObj = array.getJSONObject(i)
                                val macroString = macroObj.toString()
                                SystemMacro.fromJson(macroString) // Validate
                                preferenceManager.addSystemMacro(macroString)
                            }
                            showImportDialog = false
                            Toast.makeText(
                                context,
                                R.string.toast_import_success,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        try {
                            val obj = JSONObject(jsonInput)
                            val macroString = obj.toString()
                            SystemMacro.fromJson(macroString) // Validate
                            scope.launch {
                                preferenceManager.addSystemMacro(macroString)
                                showImportDialog = false
                                Toast.makeText(
                                    context,
                                    R.string.toast_import_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e2: Exception) {
                            Toast.makeText(
                                context,
                                R.string.toast_import_invalid,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.action_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showExportDialog) {
        val allMacrosJson = remember(macrosJson) {
            val jsonArray = JSONArray()
            macrosJson.forEach { jsonStr ->
                try {
                    jsonArray.put(JSONObject(jsonStr))
                } catch (e: Exception) {
                }
            }
            jsonArray.toString(4)
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_macros)) },
            text = {
                OutlinedTextField(
                    value = allMacrosJson,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = MaterialTheme.shapes.medium
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Macros JSON", allMacrosJson)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text(stringResource(R.string.copy_to_clipboard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    editingSetting?.let { (key, value) ->
        var newValue by remember { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { editingSetting = null },
            title = { Text(stringResource(R.string.edit_setting)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(key, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text(stringResource(R.string.new_value)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val cmd = "settings put ${selectedTable.key} $key $newValue"
                        val result = ShizukuShellRunner.runCommand(cmd)
                        if (result.isSuccess) {
                            Toast.makeText(context, R.string.toast_setting_updated, Toast.LENGTH_SHORT).show()
                            refreshTable()
                        }
                        editingSetting = null
                    }
                }) {
                    Text(stringResource(R.string.action_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSetting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun MacroItem(
    macro: SystemMacro,
    onApply: () -> Unit,
    onRevert: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(macro.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.macro_settings_count, macro.settings.size),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            macro.settings.forEach { setting ->
                Text(
                    "${setting.table.name}: ${setting.key} \u2192 ${setting.targetValue}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(100)
                ) {
                    Text(stringResource(R.string.action_apply), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRevert,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(100)
                ) {
                    Text(stringResource(R.string.action_revert_macro), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroEditorPage(
    macro: SystemMacro? = null,
    onDismiss: () -> Unit,
    onSave: (SystemMacro) -> Unit
) {
    var name by remember { mutableStateOf(macro?.name ?: "") }
    val settings =
        remember { mutableStateListOf<MacroSetting>().apply { macro?.let { addAll(it.settings) } } }
    
    var currentTable by remember { mutableStateOf(SystemTable.SYSTEM) }
    var currentKey by remember { mutableStateOf("") }
    var currentDefaultValue by remember { mutableStateOf("") }
    var currentTargetValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (macro == null) stringResource(R.string.add_macro) else stringResource(
                            R.string.edit_macro
                        ), fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank() && settings.isNotEmpty()) {
                                onSave(SystemMacro(name, settings.toList()))
                            }
                        },
                        enabled = name.isNotBlank() && settings.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.macro_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(settings) { setting ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                setting.key,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${setting.table.name}: ${setting.defaultValue} \u2192 ${setting.targetValue}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = { settings.remove(setting) }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.add_new_setting),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentTable.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.select_table)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                SystemTable.entries.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.name) },
                                        onClick = {
                                            currentTable = t
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(value = currentKey, onValueChange = { currentKey = it }, label = { Text(stringResource(R.string.setting_key)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = currentDefaultValue, onValueChange = { currentDefaultValue = it }, label = { Text(stringResource(R.string.default_value)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = currentTargetValue, onValueChange = { currentTargetValue = it }, label = { Text(stringResource(R.string.target_value)) }, modifier = Modifier.fillMaxWidth())
                        
                        Button(
                            onClick = {
                                if (currentKey.isNotBlank()) {
                                    settings.add(MacroSetting(currentTable, currentKey, currentDefaultValue, currentTargetValue))
                                    currentKey = ""
                                    currentDefaultValue = ""
                                    currentTargetValue = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(100)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.add_new_setting),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
