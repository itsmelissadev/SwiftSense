package io.github.itsmelissadev.swiftsense.feature.systemtables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shell.ShellBridge
import io.github.itsmelissadev.swiftsense.ui.components.ExecutionStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveHeader
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveOutlinedButton
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSearchBar
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.ExpressiveTextField
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSectionHeader
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

    var showEditor by remember { mutableStateOf(false) }
    var macroToEdit by remember { mutableStateOf<SystemMacro?>(null) }
    var originalMacroJson by remember { mutableStateOf<String?>(null) }

    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var editingSetting by remember { mutableStateOf<Pair<String, String>?>(null) }

    val macrosJson by preferenceManager.systemMacros.collectAsStateWithLifecycle(initialValue = emptySet())
    val macros = remember(macrosJson) { macrosJson.map { SystemMacro.fromJson(it) } }

    val isMacroServiceRunning by SystemMacroService.isRunning.collectAsState()
    val autoApplyJson by preferenceManager.autoApplyMacroJson.collectAsState(initial = null)
    val autoApplyInterval by preferenceManager.autoApplyMacroInterval.collectAsState(initial = 1)

    var macroToDelete by remember { mutableStateOf<SystemMacro?>(null) }
    var showAutoApplyMacroDialog by remember { mutableStateOf(false) }
    var showAutoApplyIntervalDialog by remember { mutableStateOf(false) }

    val isReady by ShellBridge.ready.collectAsState()

    var tableData by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val refreshTable: () -> Unit = {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val result = ShellBridge.run("settings list ${selectedTable.key}")
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
                macroToEdit = null
                originalMacroJson = null
                showEditor = false
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
                        macroToEdit = null
                        originalMacroJson = null
                        showEditor = false
                    }
                }
            )
        } else {
            Scaffold(
                topBar = {
                    Column {
                        ExpressiveHeader(
                            title = stringResource(R.string.feature_system_tables),
                            onBackClick = onNavigateBack,
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
                        SecondaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            },
                            divider = {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    when (selectedTabIndex) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!isReady) {
                                    item {
                                        ExecutionStatusWidget(bottomSpacing = 6.dp)
                                    }
                                }

                                item {
                                    SwiftSenseSectionHeader(
                                        title = stringResource(R.string.auto_apply_title),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    val activeAutoMacro = remember(autoApplyJson) {
                                        autoApplyJson?.let {
                                            try {
                                                SystemMacro.fromJson(it)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                    }

                                    FeatureCard(
                                        title = stringResource(R.string.service_status),
                                        description = stringResource(
                                            if (isMacroServiceRunning) R.string.macro_service_running else R.string.macro_service_stopped
                                        ),
                                        icon = Icons.Default.Refresh,
                                        checked = isMacroServiceRunning,
                                        enabled = activeAutoMacro != null,
                                        onCheckedChange = { active ->
                                            scope.launch {
                                                preferenceManager.setMacroServiceRunning(active)
                                                val intent =
                                                    Intent(context, SystemMacroService::class.java)
                                                if (active) {
                                                    context.startForegroundService(intent)
                                                } else {
                                                    context.stopService(intent)
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ExpressiveTextField(
                                                value = activeAutoMacro?.name
                                                    ?: stringResource(R.string.audio_disabled),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = stringResource(R.string.select_macro_to_apply),
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.Default.ArrowDropDown,
                                                        null
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .clickable(enabled = !isMacroServiceRunning) {
                                                        showAutoApplyMacroDialog = true
                                                    }
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f)) {
                                            val intervalLabel = if (autoApplyInterval == 500) {
                                                stringResource(R.string.interval_500ms)
                                            } else {
                                                stringResource(R.string.macro_interval_seconds, autoApplyInterval)
                                            }
                                            ExpressiveTextField(
                                                value = intervalLabel,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = stringResource(R.string.select_interval),
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.Default.ArrowDropDown,
                                                        null
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .clickable(enabled = !isMacroServiceRunning) {
                                                        showAutoApplyIntervalDialog = true
                                                    }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.auto_apply_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                                if (macros.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stringResource(R.string.no_macros),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(macros) { macro ->
                                        MacroItem(
                                            macro = macro,
                                            onApply = {
                                                scope.launch {
                                                    var success = true
                                                    withContext(Dispatchers.IO) {
                                                        macro.settings.forEach { setting ->
                                                            val cmd =
                                                                "settings put ${setting.table.key} ${setting.key} ${setting.targetValue}"
                                                            val result =
                                                                ShellBridge.run(cmd)
                                                            if (!result.isSuccess) success = false
                                                        }
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
                                                    withContext(Dispatchers.IO) {
                                                        macro.settings.forEach { setting ->
                                                            val cmd =
                                                                "settings put ${setting.table.key} ${setting.key} ${setting.defaultValue}"
                                                            val result =
                                                                ShellBridge.run(cmd)
                                                            if (!result.isSuccess) success = false
                                                        }
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
                                            onDuplicate = {
                                                val copyName = context.getString(R.string.macro_copy_suffix, macro.name)
                                                val cloned = macro.copy(name = copyName)
                                                scope.launch {
                                                    preferenceManager.addSystemMacro(cloned.toJson())
                                                    Toast.makeText(
                                                        context,
                                                        R.string.toast_macro_duplicated,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onEdit = {
                                                macroToEdit = macro
                                                originalMacroJson = macro.toJson()
                                                showEditor = true
                                            },
                                            onDelete = {
                                                macroToDelete = macro
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                var showTableDialog by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExpressiveTextField(
                                        value = stringResource(
                                            when (selectedTable) {
                                                SystemTable.SYSTEM -> R.string.table_system
                                                SystemTable.SECURE -> R.string.table_secure
                                                SystemTable.GLOBAL -> R.string.table_global
                                            }
                                        ),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = stringResource(R.string.select_table),
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showTableDialog = true }
                                    )
                                }

                                if (showTableDialog) {
                                    ExpressiveSelectionDialog(
                                        title = stringResource(R.string.select_table),
                                        options = SystemTable.entries.map { table ->
                                            table to stringResource(
                                                when (table) {
                                                    SystemTable.SYSTEM -> R.string.table_system
                                                    SystemTable.SECURE -> R.string.table_secure
                                                    SystemTable.GLOBAL -> R.string.table_global
                                                }
                                            )
                                        },
                                        selected = selectedTable,
                                        onSelectedChange = { selectedTable = it },
                                        onDismissRequest = { showTableDialog = false }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                ExpressiveSearchBar(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    placeholder = stringResource(R.string.search_table)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

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
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!isReady) {
                                            item {
                                                ExecutionStatusWidget(bottomSpacing = 8.dp)
                                            }
                                        }
                                        items(filteredData) { (key, value) ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth()
                                                    .clickable {
                                                        editingSetting = key to value
                                                    },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainer
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 12.dp
                                                    ),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = key,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = value,
                                                            modifier = Modifier.padding(
                                                                horizontal = 10.dp,
                                                                vertical = 4.dp
                                                            ),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold
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
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        ExpressiveDialog(
            onDismissRequest = { showImportDialog = false },
            title = stringResource(R.string.import_macros),
            content = {
                ExpressiveTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = stringResource(R.string.import_macros),
                    placeholder = stringResource(R.string.import_json_hint),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_import),
                    onClick = {
                        try {
                            val array = JSONArray(jsonInput)
                            scope.launch {
                                for (i in 0 until array.length()) {
                                    val macroObj = array.getJSONObject(i)
                                    val macroString = macroObj.toString()
                                    SystemMacro.fromJson(macroString)
                                    preferenceManager.addSystemMacro(macroString)
                                }
                                showImportDialog = false
                                Toast.makeText(
                                    context,
                                    R.string.toast_import_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (_: Exception) {
                            try {
                                val obj = JSONObject(jsonInput)
                                val macroString = obj.toString()
                                SystemMacro.fromJson(macroString)
                                scope.launch {
                                    preferenceManager.addSystemMacro(macroString)
                                    showImportDialog = false
                                    Toast.makeText(
                                        context,
                                        R.string.toast_import_success,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    R.string.toast_import_invalid,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showImportDialog = false }
                )
            }
        )
    }

    val macros_clipboard_label = stringResource(R.string.macros_clipboard_label)
    if (showExportDialog) {
        val allMacrosJson = remember(macrosJson) {
            val jsonArray = JSONArray()
            macrosJson.forEach { jsonStr ->
                try {
                    jsonArray.put(JSONObject(jsonStr))
                } catch (_: Exception) {
                }
            }
            jsonArray.toString(4)
        }

        ExpressiveDialog(
            onDismissRequest = { showExportDialog = false },
            title = stringResource(R.string.export_macros),
            content = {
                ExpressiveTextField(
                    value = allMacrosJson,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.export_macros),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.copy_to_clipboard),
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(macros_clipboard_label, allMacrosJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showExportDialog = false }
                )
            }
        )
    }

    editingSetting?.let { (key, value) ->
        var newValue by remember { mutableStateOf(value) }
        ExpressiveDialog(
            onDismissRequest = { editingSetting = null },
            title = stringResource(R.string.edit_setting),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        key,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ExpressiveTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = stringResource(R.string.new_value)
                    )
                }
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_apply),
                    onClick = {
                        scope.launch {
                            val cmd = "settings put ${selectedTable.key} $key $newValue"
                            val result = withContext(Dispatchers.IO) {
                                ShellBridge.run(cmd)
                            }
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    R.string.toast_setting_updated,
                                    Toast.LENGTH_SHORT
                                ).show()
                                refreshTable()
                            }
                            editingSetting = null
                        }
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { editingSetting = null }
                )
            }
        )
    }

    if (showAutoApplyMacroDialog) {
        val macroOptions = macros.map { it.toJson() to it.name }
        ExpressiveSelectionDialog(
            title = stringResource(R.string.select_macro_to_apply),
            options = macroOptions,
            selected = autoApplyJson ?: "",
            onSelectedChange = { json ->
                scope.launch {
                    preferenceManager.setAutoApplyMacroJson(json)
                }
            },
            onDismissRequest = { showAutoApplyMacroDialog = false }
        )
    }

    if (showAutoApplyIntervalDialog) {
        val intervalOptions = listOf(
            500 to stringResource(R.string.interval_500ms),
            1 to stringResource(R.string.macro_interval_seconds, 1),
            2 to stringResource(R.string.macro_interval_seconds, 2),
            3 to stringResource(R.string.macro_interval_seconds, 3),
            4 to stringResource(R.string.macro_interval_seconds, 4),
            5 to stringResource(R.string.macro_interval_seconds, 5)
        )
        ExpressiveSelectionDialog(
            title = stringResource(R.string.select_interval),
            options = intervalOptions,
            selected = autoApplyInterval,
            onSelectedChange = { interval ->
                scope.launch {
                    preferenceManager.setAutoApplyMacroInterval(interval)
                }
            },
            onDismissRequest = { showAutoApplyIntervalDialog = false }
        )
    }

    macroToDelete?.let { targetMacro ->
        ExpressiveDialog(
            onDismissRequest = { macroToDelete = null },
            title = stringResource(R.string.delete_macro_title),
            content = {
                Text(
                    text = stringResource(R.string.delete_macro_confirm, targetMacro.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_delete),
                    onClick = {
                        scope.launch {
                            preferenceManager.removeSystemMacro(targetMacro.toJson())
                        }
                        macroToDelete = null
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { macroToDelete = null }
                )
            }
        )
    }
}

@Composable
fun MacroItem(
    macro: SystemMacro,
    onApply: () -> Unit,
    onRevert: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        macro.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(
                            R.string.macro_settings_count,
                            macro.settings.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onDuplicate) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    macro.settings.forEach { setting ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            when (setting.table) {
                                                SystemTable.SYSTEM -> R.string.table_system
                                                SystemTable.SECURE -> R.string.table_secure
                                                SystemTable.GLOBAL -> R.string.table_global
                                            }
                                        ),
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = setting.key,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = setting.defaultValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = setting.targetValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpressiveButton(
                    text = stringResource(R.string.action_apply),
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                )
                ExpressiveOutlinedButton(
                    text = stringResource(R.string.action_revert_macro),
                    onClick = onRevert,
                    modifier = Modifier.weight(1f)
                )
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

    var editingSetting by remember { mutableStateOf<Pair<Int, MacroSetting>?>(null) }

    var currentTable by remember { mutableStateOf(SystemTable.SYSTEM) }
    var currentKey by remember { mutableStateOf("") }
    var currentDefaultValue by remember { mutableStateOf("") }
    var currentTargetValue by remember { mutableStateOf("") }

    var cachedTableMap by remember { mutableStateOf<Map<SystemTable, List<Pair<String, String>>>>(emptyMap()) }

    LaunchedEffect(currentTable) {
        if (!cachedTableMap.containsKey(currentTable)) {
            val res = withContext(Dispatchers.IO) {
                ShellBridge.run("settings list ${currentTable.key}")
            }
            if (res.isSuccess) {
                val list = (res.getOrNull() ?: "").lines()
                    .filter { it.contains("=") }
                    .map {
                        val parts = it.split("=", limit = 2)
                        parts[0].trim() to parts[1].trim()
                    }
                cachedTableMap = cachedTableMap + (currentTable to list)
            }
        }
    }

    val currentSuggestions = remember(currentKey, currentTable, cachedTableMap) {
        val trimmed = currentKey.trim()
        if (trimmed.length < 2) emptyList()
        else cachedTableMap[currentTable]?.filter {
            it.first.contains(trimmed, ignoreCase = true) && !it.first.equals(trimmed, ignoreCase = true)
        }?.take(5) ?: emptyList()
    }

    Scaffold(
        topBar = {
            ExpressiveHeader(
                title = if (macro == null) stringResource(R.string.add_macro) else stringResource(R.string.edit_macro),
                onBackClick = onDismiss,
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank() && settings.isNotEmpty()) {
                                onSave(SystemMacro(name, settings.toList()))
                            }
                        },
                        enabled = name.isNotBlank() && settings.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (name.isNotBlank() && settings.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                ExpressiveTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.macro_name)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(settings) { setting ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        val idx = settings.indexOf(setting)
                        if (idx != -1) {
                            editingSetting = idx to setting
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            when (setting.table) {
                                                SystemTable.SYSTEM -> R.string.table_system
                                                SystemTable.SECURE -> R.string.table_secure
                                                SystemTable.GLOBAL -> R.string.table_global
                                            }
                                        ),
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = setting.key,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = setting.defaultValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = setting.targetValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { settings.remove(setting) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.add_new_setting),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        var showTableDialog by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExpressiveTextField(
                                value = stringResource(
                                    when (currentTable) {
                                        SystemTable.SYSTEM -> R.string.table_system
                                        SystemTable.SECURE -> R.string.table_secure
                                        SystemTable.GLOBAL -> R.string.table_global
                                    }
                                ),
                                onValueChange = {},
                                readOnly = true,
                                label = stringResource(R.string.select_table),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showTableDialog = true }
                            )
                        }

                        if (showTableDialog) {
                            ExpressiveSelectionDialog(
                                title = stringResource(R.string.select_table),
                                options = SystemTable.entries.map { t ->
                                    t to stringResource(
                                        when (t) {
                                            SystemTable.SYSTEM -> R.string.table_system
                                            SystemTable.SECURE -> R.string.table_secure
                                            SystemTable.GLOBAL -> R.string.table_global
                                        }
                                    )
                                },
                                selected = currentTable,
                                onSelectedChange = { currentTable = it },
                                onDismissRequest = { showTableDialog = false }
                            )
                        }

                        ExpressiveTextField(
                            value = currentKey,
                            onValueChange = { currentKey = it },
                            label = stringResource(R.string.setting_key)
                        )

                        if (currentSuggestions.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    currentSuggestions.forEach { (sugKey, sugVal) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    currentKey = sugKey
                                                    currentDefaultValue = sugVal
                                                    currentTargetValue = sugVal
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sugKey,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = sugVal,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ExpressiveTextField(
                            value = currentDefaultValue,
                            onValueChange = { currentDefaultValue = it },
                            label = stringResource(R.string.default_value)
                        )
                        ExpressiveTextField(
                            value = currentTargetValue,
                            onValueChange = { currentTargetValue = it },
                            label = stringResource(R.string.target_value)
                        )

                        ExpressiveButton(
                            text = stringResource(R.string.add_new_setting),
                            onClick = {
                                if (currentKey.isNotBlank()) {
                                    settings.add(
                                        MacroSetting(
                                            currentTable,
                                            currentKey,
                                            currentDefaultValue,
                                            currentTargetValue
                                        )
                                    )
                                    currentKey = ""
                                    currentDefaultValue = ""
                                    currentTargetValue = ""
                                }
                            },
                            icon = Icons.Default.Add,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    editingSetting?.let { (index, setting) ->
        var editTable by remember { mutableStateOf(setting.table) }
        var editKey by remember { mutableStateOf(setting.key) }
        var editDefaultValue by remember { mutableStateOf(setting.defaultValue) }
        var editTargetValue by remember { mutableStateOf(setting.targetValue) }

        LaunchedEffect(editTable) {
            if (!cachedTableMap.containsKey(editTable)) {
                val res = withContext(Dispatchers.IO) {
                    ShellBridge.run("settings list ${editTable.key}")
                }
                if (res.isSuccess) {
                    val list = (res.getOrNull() ?: "").lines()
                        .filter { it.contains("=") }
                        .map {
                            val parts = it.split("=", limit = 2)
                            parts[0].trim() to parts[1].trim()
                        }
                    cachedTableMap = cachedTableMap + (editTable to list)
                }
            }
        }

        val editSuggestions = remember(editKey, editTable, cachedTableMap) {
            val trimmed = editKey.trim()
            if (trimmed.length < 2) emptyList()
            else cachedTableMap[editTable]?.filter {
                it.first.contains(trimmed, ignoreCase = true) && !it.first.equals(trimmed, ignoreCase = true)
            }?.take(5) ?: emptyList()
        }

        ExpressiveDialog(
            onDismissRequest = { editingSetting = null },
            title = stringResource(R.string.edit_setting),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var showTableDialog by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExpressiveTextField(
                            value = stringResource(
                                when (editTable) {
                                    SystemTable.SYSTEM -> R.string.table_system
                                    SystemTable.SECURE -> R.string.table_secure
                                    SystemTable.GLOBAL -> R.string.table_global
                                }
                            ),
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.select_table),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTableDialog = true }
                        )
                    }

                    if (showTableDialog) {
                        ExpressiveSelectionDialog(
                            title = stringResource(R.string.select_table),
                            options = SystemTable.entries.map { t ->
                                t to stringResource(
                                    when (t) {
                                        SystemTable.SYSTEM -> R.string.table_system
                                        SystemTable.SECURE -> R.string.table_secure
                                        SystemTable.GLOBAL -> R.string.table_global
                                    }
                                )
                            },
                            selected = editTable,
                            onSelectedChange = { editTable = it },
                            onDismissRequest = { showTableDialog = false }
                        )
                    }

                    ExpressiveTextField(
                        value = editKey,
                        onValueChange = { editKey = it },
                        label = stringResource(R.string.setting_key)
                    )

                    if (editSuggestions.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                editSuggestions.forEach { (sugKey, sugVal) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editKey = sugKey
                                                editDefaultValue = sugVal
                                                editTargetValue = sugVal
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sugKey,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = sugVal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ExpressiveTextField(
                        value = editDefaultValue,
                        onValueChange = { editDefaultValue = it },
                        label = stringResource(R.string.default_value)
                    )

                    ExpressiveTextField(
                        value = editTargetValue,
                        onValueChange = { editTargetValue = it },
                        label = stringResource(R.string.target_value)
                    )
                }
            },
            confirmButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_apply),
                    onClick = {
                        if (editKey.isNotBlank()) {
                            settings[index] = MacroSetting(
                                editTable,
                                editKey,
                                editDefaultValue,
                                editTargetValue
                            )
                        }
                        editingSetting = null
                    }
                )
            },
            dismissButton = {
                ExpressiveDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { editingSetting = null }
                )
            }
        )
    }
}
