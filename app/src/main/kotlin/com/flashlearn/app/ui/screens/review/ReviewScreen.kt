package com.flashlearn.app.ui.screens.review

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashlearn.app.presentation.review.AnswerFeedback
import com.flashlearn.app.presentation.review.CardPresentation
import com.flashlearn.app.presentation.review.ReviewMode
import com.flashlearn.app.presentation.review.ReviewUiState
import com.flashlearn.app.presentation.review.ReviewViewModel
import com.flashlearn.app.ui.theme.AppColors
import com.flashlearn.domain.model.ReviewType

/**
 * Review screen (Phase 25 Flashcard + Phase 26 Quiz/type-selector).
 * [reviewTypeArg] is the nav argument from
 * [com.flashlearn.app.navigation.Routes.REVIEW_TYPE_ARG] — when null
 * (user reached Review without picking a type on Home) this falls back
 * to [ReviewType.DAILY] as the picker's starting value; the user can
 * still change it before tapping "شروع مرور".
 */
@Composable
fun ReviewScreen(
    reviewTypeArg: String?,
    onFinished: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        val reviewType = reviewTypeArg
            ?.let { arg -> ReviewType.entries.firstOrNull { it.name == arg } }
            ?: ReviewType.DAILY
        viewModel.initialize(reviewType)
    }

    when (val state = uiState) {
        is ReviewUiState.SelectingOptions -> SelectingOptionsContent(
            state = state,
            onReviewTypeChange = viewModel::changeReviewType,
            onModeChange = viewModel::changeMode,
            onStart = viewModel::start
        )
        is ReviewUiState.Loading -> LoadingContent()
        is ReviewUiState.Empty -> EmptyContent(onBack = onFinished)
        is ReviewUiState.InProgress -> InProgressContent(
            state = state,
            onFlip = viewModel::flip,
            onAnswerFlashcard = viewModel::answerFlashcard,
            onSelectQuizOption = viewModel::selectQuizOption
        )
        is ReviewUiState.Finished -> FinishedContent(state = state, onBack = onFinished)
    }
}

private fun reviewTypeLabel(reviewType: ReviewType): String = when (reviewType) {
    ReviewType.DAILY -> "روزانه"
    ReviewType.WEEKLY -> "هفتگی"
    ReviewType.MONTHLY -> "ماهانه"
    ReviewType.RANDOM -> "تصادفی"
    ReviewType.LEARNED -> "یادگرفته‌شده"
}

@Composable
private fun SelectingOptionsContent(
    state: ReviewUiState.SelectingOptions,
    onReviewTypeChange: (ReviewType) -> Unit,
    onModeChange: (ReviewMode) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("نوع مرور", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReviewType.entries.forEach { type ->
                FilterChip(
                    selected = state.reviewType == type,
                    onClick = { onReviewTypeChange(type) },
                    label = { Text(reviewTypeLabel(type)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("حالت مرور", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == ReviewMode.FLASHCARD,
                onClick = { onModeChange(ReviewMode.FLASHCARD) },
                label = { Text("فلش‌کارت") }
            )
            FilterChip(
                selected = state.mode == ReviewMode.QUIZ,
                onClick = { onModeChange(ReviewMode.QUIZ) },
                label = { Text("چهارگزینه‌ای") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text("شروع مرور")
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("چیزی برای مرور نیست", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) { Text("بازگشت") }
    }
}

@Composable
private fun FinishedContent(state: ReviewUiState.Finished, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("پایان مرور", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${state.correctCount} از ${state.totalCount} درست", style = MaterialTheme.typography.bodyLarge)
        if (state.newlyUnlockedCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("🏆 ${state.newlyUnlockedCount} دستاورد جدید باز شد!", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) { Text("بازگشت به خانه") }
    }
}

@Composable
private fun InProgressContent(
    state: ReviewUiState.InProgress,
    onFlip: () -> Unit,
    onAnswerFlashcard: (Boolean) -> Unit,
    onSelectQuizOption: (String) -> Unit
) {
    val successColor = if (isSystemInDarkTheme()) AppColors.DarkSuccess else AppColors.LightSuccess

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "${state.currentIndex + 1} از ${state.totalCount}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val presentation = state.presentation) {
            is CardPresentation.Flashcard -> FlashcardContent(
                frontText = state.frontText,
                presentation = presentation,
                feedback = state.feedback,
                successColor = successColor,
                onFlip = onFlip,
                onAnswer = onAnswerFlashcard
            )
            is CardPresentation.Quiz -> QuizContent(
                frontText = state.frontText,
                presentation = presentation,
                feedback = state.feedback,
                successColor = successColor,
                onSelectOption = onSelectQuizOption
            )
        }
    }
}

@Composable
private fun ColumnScope.FlashcardContent(
    frontText: String,
    presentation: CardPresentation.Flashcard,
    feedback: AnswerFeedback?,
    successColor: Color,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit
) {
    val cardColor = when (feedback) {
        AnswerFeedback.CORRECT -> successColor
        AnswerFeedback.WRONG -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(frontText, style = MaterialTheme.typography.headlineSmall)
            if (presentation.isFlipped) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(presentation.backText, style = MaterialTheme.typography.headlineSmall)
                if (!presentation.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(presentation.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (!presentation.isFlipped) {
        Button(onClick = onFlip, modifier = Modifier.fillMaxWidth()) {
            Text("نمایش پاسخ")
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onAnswer(false) },
                enabled = feedback == null,
                modifier = Modifier.weight(1f)
            ) { Text("❌ بلد نیستم") }
            Button(
                onClick = { onAnswer(true) },
                enabled = feedback == null,
                modifier = Modifier.weight(1f)
            ) { Text("✅ بلدم") }
        }
    }
}

@Composable
private fun QuizContent(
    frontText: String,
    presentation: CardPresentation.Quiz,
    feedback: AnswerFeedback?,
    successColor: Color,
    onSelectOption: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(frontText, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    presentation.options.forEach { option ->
        val isCorrectOption = option == presentation.correctAnswerText
        val isSelected = option == presentation.selectedOption
        val containerColor = when {
            feedback == null -> MaterialTheme.colorScheme.surfaceVariant
            isCorrectOption -> successColor
            isSelected -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        Card(
            onClick = { onSelectOption(option) },
            enabled = feedback == null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Text(
                option,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
