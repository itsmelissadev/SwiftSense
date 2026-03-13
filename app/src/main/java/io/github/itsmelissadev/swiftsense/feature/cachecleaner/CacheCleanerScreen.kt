package io.github.itsmelissadev.swiftsense.feature.cachecleaner

import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheCleanerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var initialStorage by remember { mutableLongStateOf(0L) }
    var finalStorage by remember { mutableLongStateOf(0L) }

    val isShizukuReady = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun getAvailableStorage(): Long {
        return context.cacheDir.freeSpace + context.filesDir.freeSpace
    }

    fun startCleaning() {
        scope.launch {
            isRunning = true
            isComplete = false
            initialStorage = getAvailableStorage()
            
            ShizukuShellRunner.runCommand("pm trim-caches 4096G")
            delay(2000)

            finalStorage = getAvailableStorage()
            isRunning = false
            isComplete = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_cache_cleaner), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShizukuStatusWidget()
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (isRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(stringResource(R.string.clearing_cache), style = MaterialTheme.typography.titleMedium)
                    }
                } else if (isComplete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.optimization_complete), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        
                        val freedMb = (finalStorage - initialStorage).coerceAtLeast(0L) / (1024 * 1024)
                        if (freedMb > 0) {
                            Text(stringResource(R.string.freed_space, freedMb), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.feature_cache_cleaner_desc), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
            }
            Button(
                onClick = { startCleaning() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isShizukuReady && !isRunning,
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.clean_now), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
