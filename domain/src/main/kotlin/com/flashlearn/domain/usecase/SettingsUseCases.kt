package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.AppSetting
import com.flashlearn.domain.model.AppTheme
import com.flashlearn.domain.model.QuizDifficulty
import com.flashlearn.domain.model.SettingKeys
import com.flashlearn.domain.repository.SettingsRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Read-only accessor for `threshold_difficulty` (Descriptions §6.3/D:
 * V1 value is 3; "در UI قابل ویرایش نیست"). There is deliberately no
 * matching Set-UseCase — SubmitReviewAnswerUseCase reads the same
 * setting directly for its own internal use, but any UI-facing display
 * of the current value (e.g. a future Settings screen) should go
 * through this UseCase rather than reading the repository directly, so
 * the "no UI to change it in V1" rule stays enforced in one place.
 */
class GetThresholdDifficultyUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Int =
        settingsRepository.getInt(SettingKeys.THRESHOLD_DIFFICULTY, SettingKeys.DEFAULT_THRESHOLD_DIFFICULTY)
}

/**
 * Optional cap on cards per Review Session (Descriptions §6/Backlog F).
 * `null` means "no limit" — the doc requires this setting to exist but
 * never gives a concrete default number, so V1 defaults to unlimited
 * rather than inventing one (Phase 20 decision log).
 */
class GetMaxReviewCardsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Int? =
        settingsRepository.findByKey(SettingKeys.MAX_REVIEW_CARDS)?.value?.toIntOrNull()
}

/**
 * Sets or clears the Max Review Cards cap. This setting only limits
 * Session *size* — it never changes Due/Eligibility (Descriptions §6:
 * "فقط اندازه Session را محدود می‌کند").
 */
class SetMaxReviewCardsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(maxCards: Int?, now: Instant) {
        require(maxCards == null || maxCards > 0) { "maxCards must be null (no limit) or a positive number" }
        if (maxCards == null) {
            settingsRepository.delete(SettingKeys.MAX_REVIEW_CARDS)
        } else {
            settingsRepository.put(AppSetting(SettingKeys.MAX_REVIEW_CARDS, maxCards.toString(), now))
        }
    }
}

/** Theme/Color Scheme (Descriptions §6/Backlog F). Falls back to [AppTheme.SYSTEM] when unset or unparsable. */
class GetThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): AppTheme {
        val raw = settingsRepository.findByKey(SettingKeys.THEME)?.value
        return AppTheme.entries.firstOrNull { it.name == raw } ?: AppTheme.SYSTEM
    }
}

class SetThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(theme: AppTheme, now: Instant) {
        settingsRepository.put(AppSetting(SettingKeys.THEME, theme.name, now))
    }
}

/** Falls back to [QuizDifficulty.MEDIUM] when unset or unparsable — same default [GenerateQuizQuestionUseCase] itself uses. */
class GetQuizDifficultyUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): QuizDifficulty {
        val raw = settingsRepository.findByKey(SettingKeys.QUIZ_DIFFICULTY)?.value
        return QuizDifficulty.entries.firstOrNull { it.name == raw } ?: QuizDifficulty.MEDIUM
    }
}

class SetQuizDifficultyUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(quizDifficulty: QuizDifficulty, now: Instant) {
        settingsRepository.put(AppSetting(SettingKeys.QUIZ_DIFFICULTY, quizDifficulty.name, now))
    }
}

/** Descriptions §16.2: "کاربر می‌تواند در تنظیمات، گزینه «رمزنگاری Backup» را فعال کند" — default false. */
class GetBackupEncryptionEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Boolean =
        settingsRepository.getBoolean(SettingKeys.BACKUP_ENCRYPTION_ENABLED, false)
}

class SetBackupEncryptionEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean, now: Instant) {
        settingsRepository.put(AppSetting(SettingKeys.BACKUP_ENCRYPTION_ENABLED, enabled.toString(), now))
    }
}
