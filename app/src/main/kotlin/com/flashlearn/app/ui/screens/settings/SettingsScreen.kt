package com.flashlearn.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashlearn.app.presentation.settings.SettingsViewModel
import com.flashlearn.domain.model.AppTheme

/**
 * Settings screen (Descriptions §6/Backlog F): Max Review Cards, Theme,
 * read-only threshold display, and links to Category management and
 * About. Backup/Restore has no UI anywhere in this project yet — Phase
 * 17's decision log explains that its safety-backup I/O was deliberately
 * left to "a not-yet-numbered phase", and this phase's own title
 * ("Settings + About + Category UI") does not include it either, so it
 * stays a known gap rather than being invented here.
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
    viewModel: SettingsViewModel = hiltViewModel()
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
