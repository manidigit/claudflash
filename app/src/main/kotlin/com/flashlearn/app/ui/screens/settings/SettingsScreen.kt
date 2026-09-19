package com.flashlearn.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashlearn.app.presentation.backup.BackupViewModel
import com.flashlearn.app.presentation.backup.VocabularyCsvViewModel
import com.flashlearn.app.presentation.settings.SettingsViewModel
import com.flashlearn.domain.model.AppTheme
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings screen (Descriptions §6/Backlog F): Max Review Cards, Theme,
 * read-only threshold display, Backup/Restore, and links to Category
 * management and About.
 *
 * [currentTheme]/[onThemeChange] come from the Activity-scoped
 * `ThemeViewModel` via `FlashLearnNavGraph`, not from [SettingsViewModel]
 * — see `SettingsViewModel`'s KDoc.
 */
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    csvViewModel: VocabularyCsvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تنظیمات", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onNavigateBack) { Text("بازگشت") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("ظاهر برنامه", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = currentTheme == AppTheme.SYSTEM,
                onClick = { onThemeChange(AppTheme.SYSTEM) },
                label = { Text("سیستم") }
            )
            FilterChip(
                selected = currentTheme == AppTheme.LIGHT,
                onClick = { onThemeChange(AppTheme.LIGHT) },
                label = { Text("روشن") }
            )
            FilterChip(
                selected = currentTheme == AppTheme.DARK,
                onClick = { onThemeChange(AppTheme.DARK) },
                label = { Text("تیره") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("مرور", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("آستانه تغییر سختی", style = MaterialTheme.typography.bodyMedium)
                    Text("${uiState.thresholdDifficulty} (غیرقابل تغییر)", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.maxReviewCardsText,
                    onValueChange = viewModel::onMaxReviewCardsTextChanged,
                    label = { Text("حداکثر تعداد کارت هر مرور (خالی = نامحدود)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.maxReviewCardsError != null,
                    supportingText = uiState.maxReviewCardsError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = viewModel::saveMaxReviewCards, modifier = Modifier.fillMaxWidth()) {
                    Text("ذخیره")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        BackupRestoreSection(backupViewModel)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Import/Export واژگان (CSV)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        VocabularyCsvSection(csvViewModel)

        Spacer(modifier = Modifier.height(24.dp))

        Text("واژگان", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(onClick = onNavigateToCategories, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("مدیریت دسته‌بندی‌ها")
                Text("›")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(onClick = onNavigateToAbout, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("درباره برنامه")
                Text("›")
            }
        }
    }
}

/**
 * Export writes a JSON file to wherever the user picks via Android's
 * Storage Access Framework; Restore reads one back the same way. All
 * actual file/Uri access happens right here, not in [BackupViewModel] —
 * see that class's KDoc for why. `CreateBackupUseCase`/`RestoreBackupUseCase`
 * (Phase 17) existed long before this UI did; this closes the
 * README/tracker gap "UI برای Backup/Restore وجود ندارد".
 */
@Composable
private fun BackupRestoreSection(viewModel: BackupViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportBackup()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (text != null) viewModel.restoreBackup(text)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportLauncher.launch("flashlearn_backup_${Instant.now().epochSecond}.json") },
                    enabled = !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text("پشتیبان‌گیری")
                }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                    enabled = !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text("بازیابی")
                }
            }

            if (uiState.isWorking) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }

            uiState.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    color = if (uiState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * CSV half of README/tracker gap #5 (Algorithms §9). Same "no Uri/Context
 * in the ViewModel" split as [BackupRestoreSection] just above.
 */
@Composable
private fun VocabularyCsvSection(viewModel: VocabularyCsvViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csv = viewModel.exportCsv()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (text != null) viewModel.importCsv(text)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportLauncher.launch("flashlearn_vocabulary_${Instant.now().epochSecond}.csv") },
                    enabled = !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text("خروجی CSV")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },
                    enabled = !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text("ورودی CSV")
                }
            }

            if (uiState.isWorking) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }

            uiState.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    color = if (uiState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
