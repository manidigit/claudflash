package com.flashlearn.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.flashlearn.app.presentation.home.HomeUiState
import com.flashlearn.app.presentation.home.HomeViewModel
import com.flashlearn.domain.model.ReviewType

/**
 * Home screen — the "personal vocabulary coach" landing page (Descriptions
 * §1.1). Deliberately follows the §12.3 UI/UX rules from the start rather
 * than needing later correction:
 * - no welcome text (rule #2), no duplicate "words waiting" flag/number
 *   anywhere (rules #1/#5 — there is exactly one due-count per review
 *   type, sourced once each from [HomeUiState])
 * - no "+" decoration on numbers (rule #4)
 * - the streak+progress summary is a single compact card, not two
 *   separate two-line cards (rule #6)
 * - every number comes from [HomeUiState], never hardcoded (rule #3)
 */
@Composable
fun HomeScreen(
    onStartReview: (ReviewType) -> Unit,
    onNavigateToAddWord: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Home's back-stack entry survives navigating away and back (e.g. after
    // finishing a Review session), so counts must refresh on resume, not
    // just once when this Composable first enters composition.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onNavigateToProgress) { Text("پیشرفت") }
            TextButton(onClick = onNavigateToSettings) { Text("تنظیمات") }
        }

        SummaryCard(uiState)

        Spacer(modifier = Modifier.height(16.dp))

        ReviewOptionCard(title = "روزانه", count = uiState.dueDaily, onClick = { onStartReview(ReviewType.DAILY) })
        ReviewOptionCard(title = "هفتگی", count = uiState.dueWeekly, onClick = { onStartReview(ReviewType.WEEKLY) })
        ReviewOptionCard(title = "ماهانه", count = uiState.dueMonthly, onClick = { onStartReview(ReviewType.MONTHLY) })
        ReviewOptionCard(title = "تصادفی", count = uiState.dueRandom, onClick = { onStartReview(ReviewType.RANDOM) })
        ReviewOptionCard(
            title = "یادگرفته‌شده (اختیاری)",
            count = uiState.learnedAvailable,
            onClick = { onStartReview(ReviewType.LEARNED) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToAddWord, modifier = Modifier.fillMaxWidth()) {
            Text("افزودن کلمه")
        }
    }
}

@Composable
private fun SummaryCard(uiState: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("پیشرفت: ${uiState.progressPercentage}٪", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${uiState.learnedWords} از ${uiState.totalActiveWords} کلمه یادگرفته‌شده",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text("🔥 ${uiState.streak}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ReviewOptionCard(title: String, count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text("$count", style = MaterialTheme.typography.titleMedium)
        }
    }
}
