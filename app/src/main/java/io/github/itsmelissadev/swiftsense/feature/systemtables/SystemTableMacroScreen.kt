package io.github.itsmelissadev.swiftsense.feature.systemtables

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.withContext
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
    var showAddDialog by remember { mutableStateOf(false) }
    
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.feature_system_tables), fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        if (selectedTabIndex == 0) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_macros), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(macros) { macro ->
                                MacroItem(
                                    macro = macro,
                                    onApply = {
                                        scope.launch {
                                            var success = true
                                            macro.settings.forEach { setting ->
                                                val cmd = "settings put ${setting.table.key} ${setting.key} ${setting.targetValue}"
                                                val result = ShizukuShellRunner.runCommand(cmd)
                                                if (!result.isSuccess) success = false
                                            }
                                            if (success) {
                                                Toast.makeText(context, R.string.toast_macro_applied, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onRevert = {
                                        scope.launch {
                                            var success = true
                                            macro.settings.forEach { setting ->
                                                val cmd = "settings put ${setting.table.key} ${setting.key} ${setting.defaultValue}"
                                                val result = ShizukuShellRunner.runCommand(cmd)
                                                if (!result.isSuccess) success = false
                                            }
                                            if (success) {
                                                Toast.makeText(context, R.string.toast_macro_reverted, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onDelete = {
                                        scope.launch { preferenceManager.removeSystemMacro(macro.toJson()) }
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                SystemTable.values().forEach { table ->
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
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val filteredData = tableData.filter { 
                                it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
                            }
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                items(filteredData) { (key, value) ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                                            editingSetting = key to value
                                        },
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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

    if (showAddDialog) {
        AddMacroDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { macro ->
                scope.launch {
                    preferenceManager.addSystemMacro(macro.toJson())
                    showAddDialog = false
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
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(macro.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${macro.settings.size} Settings", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            macro.settings.forEach { setting ->
                Text("${setting.table.name}: ${setting.key} → ${setting.targetValue}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(R.string.action_apply))
                }
                OutlinedButton(
                    onClick = onRevert,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(R.string.action_revert_macro))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMacroDialog(
    onDismiss: () -> Unit,
    onAdd: (SystemMacro) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val settings = remember { mutableStateListOf<MacroSetting>() }
    
    var currentTable by remember { mutableStateOf(SystemTable.SYSTEM) }
    var currentKey by remember { mutableStateOf("") }
    var currentDefaultValue by remember { mutableStateOf("") }
    var currentTargetValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_macro)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.macro_name)) }, modifier = Modifier.fillMaxWidth())
                }
                
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                
                items(settings) { setting ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${setting.table.name}: ${setting.key}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = { settings.remove(setting) }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.add_new_setting), style = MaterialTheme.typography.labelLarge)
                        
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
                                label = { Text("Table") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                SystemTable.values().forEach { t ->
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
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text(stringResource(R.string.add_new_setting))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && settings.isNotEmpty()) {
                        onAdd(SystemMacro(name, settings.toList()))
                    }
                }
            ) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
