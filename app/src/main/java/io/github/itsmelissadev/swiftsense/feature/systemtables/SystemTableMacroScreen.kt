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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.data.PreferenceManager
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.FeatureCard
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialog
import io.github.itsmelissadev.swiftsense.ui.components.ShadcnDialogButton
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseOutlinedButton
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSectionHeader
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseSelectionDialog
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTextField
import io.github.itsmelissadev.swiftsense.ui.components.SwiftSenseTopAppBar
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
    val autoApplyInterval by preferenceManager.autoApplyMacroInterval.collectAsState(initial = 30)

    var showAutoApplyMacroDialog by remember { mutableStateOf(false) }
    var showAutoApplyIntervalDialog by remember { mutableStateOf(false) }

    var tableData by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val refreshTable: () -> Unit = {
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
                        SwiftSenseTopAppBar(
                            title = stringResource(R.string.feature_system_tables),
                            onNavigateBack = onNavigateBack,
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
                                    height = 2.dp
                                )
                            },
                            divider = {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
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
                                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.6f
                                    )
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ShizukuStatusWidget()
                                    Spacer(modifier = Modifier.height(12.dp))
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
                                            SwiftSenseTextField(
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
                                            val intervalLabel = when (autoApplyInterval) {
                                                5 -> stringResource(R.string.interval_5s)
                                                10 -> stringResource(R.string.interval_10s)
                                                30 -> stringResource(R.string.interval_30s)
                                                60 -> stringResource(R.string.interval_1m)
                                                120 -> stringResource(R.string.interval_2m)
                                                300 -> stringResource(R.string.interval_5m)
                                                600 -> stringResource(R.string.interval_10m)
                                                else -> "$autoApplyInterval"
                                            }
                                            SwiftSenseTextField(
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        modifier = Modifier.padding(bottom = 16.dp)
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
                                                                ShizukuShellRunner.runCommand(cmd)
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
                                                                ShizukuShellRunner.runCommand(cmd)
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

                        1 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                var showTableDialog by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    SwiftSenseTextField(
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
                                    SwiftSenseSelectionDialog(
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

                                SwiftSenseTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = stringResource(R.string.search_table),
                                    leadingIcon = Icons.Default.Search,
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

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
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            ShizukuStatusWidget()
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                        items(filteredData) { (key, value) ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth()
                                                    .clickable {
                                                        editingSetting = key to value
                                                    },
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 12.dp
                                                    ),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f)
                                                            .padding(end = 16.dp)
                                                    ) {
                                                        Text(
                                                            text = key,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.08f
                                                        ),
                                                        contentColor = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = value,
                                                            modifier = Modifier.padding(
                                                                horizontal = 8.dp,
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
        ShadcnDialog(
            onDismissRequest = { showImportDialog = false },
            title = stringResource(R.string.import_macros),
            content = {
                SwiftSenseTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = stringResource(R.string.import_macros),
                    placeholder = stringResource(R.string.import_json_hint),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                ShadcnDialogButton(
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
                ShadcnDialogButton(
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

        ShadcnDialog(
            onDismissRequest = { showExportDialog = false },
            title = stringResource(R.string.export_macros),
            content = {
                SwiftSenseTextField(
                    value = allMacrosJson,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.export_macros),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                ShadcnDialogButton(
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
                ShadcnDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { showExportDialog = false }
                )
            }
        )
    }

    editingSetting?.let { (key, value) ->
        var newValue by remember { mutableStateOf(value) }
        ShadcnDialog(
            onDismissRequest = { editingSetting = null },
            title = stringResource(R.string.edit_setting),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        key,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SwiftSenseTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = stringResource(R.string.new_value)
                    )
                }
            },
            confirmButton = {
                ShadcnDialogButton(
                    text = stringResource(R.string.action_apply),
                    onClick = {
                        scope.launch {
                            val cmd = "settings put ${selectedTable.key} $key $newValue"
                            val result = withContext(Dispatchers.IO) {
                                ShizukuShellRunner.runCommand(cmd)
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
                ShadcnDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { editingSetting = null }
                )
            }
        )
    }

    if (showAutoApplyMacroDialog) {
        val macroOptions = macros.map { it.toJson() to it.name }
        SwiftSenseSelectionDialog(
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
            5 to stringResource(R.string.interval_5s),
            10 to stringResource(R.string.interval_10s),
            30 to stringResource(R.string.interval_30s),
            60 to stringResource(R.string.interval_1m),
            120 to stringResource(R.string.interval_2m),
            300 to stringResource(R.string.interval_5m),
            600 to stringResource(R.string.interval_10m)
        )
        SwiftSenseSelectionDialog(
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
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
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
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(
                            R.string.macro_settings_count,
                            macro.settings.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                ),
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
                                    shape = RoundedCornerShape(4.dp)
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
                                            horizontal = 6.dp,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SwiftSenseButton(
                    text = stringResource(R.string.action_apply),
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                )
                SwiftSenseOutlinedButton(
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

    Scaffold(
        topBar = {
            SwiftSenseTopAppBar(
                title = if (macro == null) stringResource(R.string.add_macro) else stringResource(R.string.edit_macro),
                onNavigateBack = onDismiss,
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
                Spacer(modifier = Modifier.height(12.dp))
                SwiftSenseTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.macro_name)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(settings) { setting ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        val idx = settings.indexOf(setting)
                        if (idx != -1) {
                            editingSetting = idx to setting
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
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
                                    shape = RoundedCornerShape(4.dp)
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
                                            horizontal = 6.dp,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.add_new_setting),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        var showTableDialog by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SwiftSenseTextField(
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
                            SwiftSenseSelectionDialog(
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

                        SwiftSenseTextField(
                            value = currentKey,
                            onValueChange = { currentKey = it },
                            label = stringResource(R.string.setting_key)
                        )
                        SwiftSenseTextField(
                            value = currentDefaultValue,
                            onValueChange = { currentDefaultValue = it },
                            label = stringResource(R.string.default_value)
                        )
                        SwiftSenseTextField(
                            value = currentTargetValue,
                            onValueChange = { currentTargetValue = it },
                            label = stringResource(R.string.target_value)
                        )

                        SwiftSenseButton(
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

        ShadcnDialog(
            onDismissRequest = { editingSetting = null },
            title = stringResource(R.string.edit_setting),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var showTableDialog by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwiftSenseTextField(
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
                        SwiftSenseSelectionDialog(
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

                    SwiftSenseTextField(
                        value = editKey,
                        onValueChange = { editKey = it },
                        label = stringResource(R.string.setting_key)
                    )

                    SwiftSenseTextField(
                        value = editDefaultValue,
                        onValueChange = { editDefaultValue = it },
                        label = stringResource(R.string.default_value)
                    )

                    SwiftSenseTextField(
                        value = editTargetValue,
                        onValueChange = { editTargetValue = it },
                        label = stringResource(R.string.target_value)
                    )
                }
            },
            confirmButton = {
                ShadcnDialogButton(
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
                ShadcnDialogButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = { editingSetting = null }
                )
            }
        )
    }
}
