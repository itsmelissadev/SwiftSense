package io.github.itsmelissadev.swiftsense.feature.cachecleaner

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itsmelissadev.swiftsense.R
import io.github.itsmelissadev.swiftsense.service.shizuku.ShizukuShellRunner
import io.github.itsmelissadev.swiftsense.ui.components.ShizukuStatusWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            initialStorage = withContext(Dispatchers.IO) { getAvailableStorage() }
            withContext(Dispatchers.IO) {
                ShizukuShellRunner.runCommand("pm trim-caches 4096G")
            }
            delay(2000)
            finalStorage = withContext(Dispatchers.IO) { getAvailableStorage() }
            isRunning = false
            isComplete = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_cache_cleaner).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShizukuStatusWidget()

            Spacer(modifier = Modifier.height(32.dp))
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (isRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.clearing_cache),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isComplete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DoneAll,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.optimization_complete),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        val freedMb = (finalStorage - initialStorage).coerceAtLeast(0L) / (1024 * 1024)
                        if (freedMb > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Text(
                                    stringResource(R.string.freed_space, freedMb),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { isComplete = false },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp).width(160.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                stringResource(R.string.granted),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.size(120.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CleaningServices,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            stringResource(R.string.feature_cache_cleaner_desc),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }

            if (!isRunning && !isComplete) {
                Button(
                    onClick = { startCleaning() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .height(52.dp),
                    enabled = isShizukuReady,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.clean_now).uppercase(),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
