package com.flashlearn.app.ui.screens.progress

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashlearn.app.presentation.progress.AchievementUiItem
import com.flashlearn.app.presentation.progress.ProgressUiState
import com.flashlearn.app.presentation.progress.ProgressViewModel

/**
 * Progress screen: basic statistics (§11.1), progress percentage (§11.2),
 * streak (§11.3), and the full Achievements list with unlock state
 * (§11.4). Every number comes from [ProgressUiState] (Descriptions §12.3
 * UI rule #3) — same discipline as Home.
 */
@Composable
fun ProgressScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("پیشرفت", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onNavigateBack) { Text("بازگشت") }
            }
        }

        if (uiState.newlyUnlockedTypes.isNotEmpty()) {
            item {
                NewlyUnlockedBanner(
                    count = uiState.newlyUnlockedTypes.size,
                    onDismiss = viewModel::dismissNewlyUnlocked
                )
            }
        }

        item { OverviewCard(uiState) }
        item { StatisticsCard(uiState) }

        item {
            Text(
                "دستاوردها",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(uiState.achievements) { achievement ->
            AchievementRow(achievement)
        }
    }
}

@Composable
private fun NewlyUnlockedBanner(count: Int, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏆 $count دستاورد جدید باز شد!")
            TextButton(onClick = onDismiss) { Text("باشه") }
        }
    }
}

@Composable
private fun OverviewCard(uiState: ProgressUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("پیشرفت کلی", style = MaterialTheme.typography.titleMedium)
                Text("🔥 ${uiState.streak}", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = uiState.progressPercentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("${uiState.progressPercentage}٪", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatisticsCard(uiState: ProgressUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatRow("کل کلمات فعال", uiState.totalActiveWords)
            StatRow("تمرین‌شده", uiState.practicedWords)
            StatRow("تمرین‌نشده", uiState.unpracticedWords)
            StatRow("یادگرفته‌شده", uiState.learnedWords)
            StatRow("منتظر مرور", uiState.dueCount)
            StatRow("پاسخ صحیح", uiState.totalCorrect)
            StatRow("پاسخ غلط", uiState.totalWrong)
            StatRow("دقت", uiState.accuracyPercentage, suffix = "٪")
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int, suffix: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$value$suffix", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AchievementRow(achievement: AchievementUiItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (achievement.isUnlocked) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (achievement.isUnlocked) FontWeight.Bold else FontWeight.Normal
                )
                if (!achievement.isUnlocked) {
                    Text("قفل", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(if (achievement.isUnlocked) "🏆" else "🔒")
        }
    }
}
