package com.flashlearn.app.ui.screens.addword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashlearn.app.presentation.addword.AddWordMode
import com.flashlearn.app.presentation.addword.AddWordUiState
import com.flashlearn.app.presentation.addword.AddWordViewModel
import com.flashlearn.app.presentation.addword.ImportSummary
import com.flashlearn.domain.parser.ParsedEntry

/**
 * Two modes: manual single-entry add, and Paste Text bulk import
 * (Descriptions §8, Algorithms v4.20 Parser P0). A genuine Conflict from
 * [com.flashlearn.domain.usecase.ImportEntryResult.NeedsUserChoice] is
 * reported in the import summary rather than resolved automatically or
 * with a per-conflict picker UI — the Algorithm's own rule is "کاربر
 * یکی از Conceptهای موجود را انتخاب کند یا لغو کند", and building that
 * full multi-Concept picker is deferred past this phase (see the
 * Phase 24 decision log); for now the user can re-paste just that one
 * entry manually once they've checked the existing word.
 */
@Composable
fun AddWordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddWordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onNavigateBack) { Text("بازگشت") }
            Text("افزودن کلمه", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))

        val selectedTabIndex = if (uiState.mode == AddWordMode.MANUAL) 0 else 1
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = uiState.mode == AddWordMode.MANUAL,
                onClick = { viewModel.onModeSelected(AddWordMode.MANUAL) },
                text = { Text("افزودن دستی") }
            )
            Tab(
                selected = uiState.mode == AddWordMode.PASTE,
                onClick = { viewModel.onModeSelected(AddWordMode.PASTE) },
                text = { Text("چسباندن متن") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.mode == AddWordMode.MANUAL) {
            ManualAddSection(uiState, viewModel)
        } else {
            PasteImportSection(uiState, viewModel)
        }
    }
}

@Composable
private fun ManualAddSection(uiState: AddWordUiState, viewModel: AddWordViewModel) {
    Column {
        OutlinedTextField(
            value = uiState.manualSourceText,
            onValueChange = viewModel::onManualSourceTextChanged,
            label = { Text("اسپانیایی") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.manualTargetText,
            onValueChange = viewModel::onManualTargetTextChanged,
            label = { Text("فارسی") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.manualNotes,
            onValueChange = viewModel::onManualNotesChanged,
            label = { Text("یادداشت (اختیاری)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = viewModel::saveManualEntry, modifier = Modifier.fillMaxWidth()) {
            Text("ذخیره")
        }

        uiState.manualSaveError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        if (uiState.manualSavedSuccessfully) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("ذخیره شد", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PasteImportSection(uiState: AddWordUiState, viewModel: AddWordViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.pasteText,
            onValueChange = viewModel::onPasteTextChanged,
            label = { Text("متن را اینجا بچسبانید") },
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = viewModel::parsePastedText,
            enabled = uiState.pasteText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تحلیل متن")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.parsedEntries.isNotEmpty()) {
            ParsePreviewSummary(uiState)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.parsedEntries) { entry -> ParsedEntryRow(entry) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isImporting) {
                CircularProgressIndicator()
            } else {
                Button(onClick = viewModel::importParsedEntries, modifier = Modifier.fillMaxWidth()) {
                    Text("وارد کردن ${uiState.parsedEntries.size} کلمه")
                }
            }
        }

        uiState.importSummary?.let { summary -> ImportSummaryCard(summary) }
    }
}

@Composable
private fun ParsePreviewSummary(uiState: AddWordUiState) {
    Column {
        Text("${uiState.parsedEntries.size} کلمه پیدا شد", style = MaterialTheme.typography.titleMedium)
        if (uiState.duplicateGroupCount > 0) {
            Text("${uiState.duplicateGroupCount} مورد تکراری", style = MaterialTheme.typography.bodyMedium)
        }
        if (uiState.orphanCount > 0) {
            Text("${uiState.orphanCount} خط بدون ترجمه (نیازمند بررسی)", style = MaterialTheme.typography.bodyMedium)
        }
        if (uiState.commentCount > 0) {
            Text("${uiState.commentCount} خط توضیحی نادیده گرفته شد", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ParsedEntryRow(entry: ParsedEntry) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(entry.sourceText, style = MaterialTheme.typography.bodyLarge)
            Text(entry.translationText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ImportSummaryCard(summary: ImportSummary) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("نتیجه وارد کردن", style = MaterialTheme.typography.titleMedium)
            Text("${summary.created} کلمه جدید")
            Text("${summary.merged} ترجمه به کلمه‌های موجود اضافه شد")
            Text("${summary.alreadyExists} مورد از قبل وجود داشت")
            if (summary.conflicts > 0) {
                Text("${summary.conflicts} مورد نیازمند بررسی دستی (تناقض)", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
