package com.claudemani.app.ui.screens.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.claudemani.app.presentation.backup.BackupViewModel
import com.claudemani.app.presentation.backup.VocabularyCsvViewModel
import com.claudemani.app.presentation.settings.SettingsViewModel
import com.claudemani.domain.model.AppTheme
import com.claudemani.domain.model.QuizDifficulty
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings screen (Descriptions §6/Backlog F): Max Review Cards, Quiz
 * Difficulty, Theme, read-only threshold display, Backup/Restore
 * (optionally PIN-encrypted, §16.2), CSV Import/Export, and links to
 * Category management and About.
 *
 * [currentTheme]/[onThemeChange] come from the Activity-scoped
 * `ThemeViewModel` via `ClaudemaniNavGraph`, not from [SettingsViewModel]
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

        Text("سختی Quiz", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "مستقل از سختی خودِ کلمه — فقط تعیین می‌کند گزینه‌های غلط چقدر شبیه باشند",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.quizDifficulty == QuizDifficulty.EASY,
                onClick = { viewModel.onQuizDifficultySelected(QuizDifficulty.EASY) },
                label = { Text("آسان") }
            )
            FilterChip(
                selected = uiState.quizDifficulty == QuizDifficulty.MEDIUM,
                onClick = { viewModel.onQuizDifficultySelected(QuizDifficulty.MEDIUM) },
                label = { Text("متوسط") }
            )
            FilterChip(
                selected = uiState.quizDifficulty == QuizDifficulty.HARD,
                onClick = { viewModel.onQuizDifficultySelected(QuizDifficulty.HARD) },
                label = { Text("سخت") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("رمزنگاری Backup")
                    Text("فایل خروجی با یک PIN رمزنگاری می‌شود", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = uiState.backupEncryptionEnabled, onCheckedChange = viewModel::onBackupEncryptionToggled)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BackupRestoreSection(backupViewModel, encryptionEnabled = uiState.backupEncryptionEnabled)

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
 * Export writes a file (plain JSON, or PIN-encrypted bytes when
 * [encryptionEnabled]) to wherever the user picks via Android's Storage
 * Access Framework; Restore reads one back the same way and
 * auto-detects encryption from the file's own header regardless of the
 * current [encryptionEnabled] toggle (a file encrypted yesterday must
 * still restore today even if the user turned the setting back off).
 * All actual file/Uri access happens right here, not in [BackupViewModel]
 * — see that class's KDoc for why.
 */
@Composable
private fun BackupRestoreSection(viewModel: BackupViewModel, encryptionEnabled: Boolean) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExportPinDialog by remember { mutableStateOf(false) }
    // Captured right before launching the SAF picker, read back once the
    // picker returns a Uri — rememberLauncherForActivityResult must be
    // created unconditionally at composition time, so the encrypted vs.
    // plain choice can't be two different launchers; it has to be state
    // the one launcher's callback reads.
    var pendingExportPin by remember { mutableStateOf<String?>(null) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pin = pendingExportPin
        scope.launch {
            val bytes = viewModel.exportBackup(pin)
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) return@launch
            if (viewModel.isEncryptedBackup(bytes)) {
                pendingRestoreBytes = bytes
            } else {
                viewModel.restoreBackup(bytes, pin = null)
            }
        }
    }

    if (showExportPinDialog) {
        PinEntryDialog(
            title = "PIN برای رمزنگاری",
            confirmLabel = "پشتیبان‌گیری",
            onDismiss = { showExportPinDialog = false },
            onConfirm = { pin ->
                showExportPinDialog = false
                pendingExportPin = pin
                exportLauncher.launch("claudemani_backup_${Instant.now().epochSecond}.json.enc")
            }
        )
    }

    pendingRestoreBytes?.let { bytes ->
        PinEntryDialog(
            title = "این فایل رمزنگاری‌شده — PIN را وارد کنید",
            confirmLabel = "بازیابی",
            onDismiss = { pendingRestoreBytes = null },
            onConfirm = { pin ->
                pendingRestoreBytes = null
                scope.launch { viewModel.restoreBackup(bytes, pin) }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (encryptionEnabled) {
                            showExportPinDialog = true
                        } else {
                            pendingExportPin = null
                            exportLauncher.launch("claudemani_backup_${Instant.now().epochSecond}.json")
                        }
                    },
                    enabled = !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text("پشتیبان‌گیری")
                }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
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
 * A dedicated small dialog for entering a 4+ digit PIN, used for both
 * encrypted export (choosing a new PIN) and encrypted restore (re-entering
 * the one used at export time). Digits only, no minimum enforced beyond
 * non-empty — Descriptions/Algorithms never specify PIN length rules for
 * this Backlog item, so this doesn't invent one either.
 */
@Composable
private fun PinEntryDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (pin.isNotEmpty()) onConfirm(pin) }, enabled = pin.isNotEmpty()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
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
                    onClick = { exportLauncher.launch("claudemani_vocabulary_${Instant.now().epochSecond}.csv") },
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
