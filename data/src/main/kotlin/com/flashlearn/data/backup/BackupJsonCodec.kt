package com.flashlearn.data.backup

import com.flashlearn.domain.model.Achievement
import com.flashlearn.domain.model.AchievementType
import com.flashlearn.domain.model.AppSetting
import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.model.Category
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.ConceptTag
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.ExportData
import com.flashlearn.domain.model.Language
import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewHistory
import com.flashlearn.domain.model.ReviewSession
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.Tag
import com.flashlearn.domain.model.VocabularyDifficulty
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * JSON file format for Backup/Restore (README/tracker gap #1 — until now
 * [com.flashlearn.domain.usecase.CreateBackupUseCase]/[com.flashlearn.domain.usecase.RestoreBackupUseCase]
 * only ever produced/consumed an in-memory [ExportData], with nothing
 * anywhere reading or writing an actual file).
 *
 * Deliberately kept out of `domain`: [ExportData] and its nested models
 * stay free of any serialization-library annotation (Descriptions §3.3 —
 * domain has no framework dependency beyond plain Kotlin). This whole
 * file is a `data`-module concern instead, matching that module's own
 * stated responsibility ("Import/Export، Backup/Restore" per the
 * architecture table) — a plain mirror-DTO layer (UUID/Instant as
 * String, enums as their `.name`) plus mapping functions, not a
 * `@Serializable` annotation anywhere near a domain class.
 *
 * `CSV`/`XLSX`/`SQLite` (Algorithms §9's other listed formats) are NOT
 * implemented here — see the tracker for that decision; this only closes
 * the "no file I/O at all" gap using the one format ([ExportData] itself)
 * every UseCase already speaks.
 */
object BackupJsonCodec {
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    fun encode(data: ExportData): String = json.encodeToString(ExportDataDto.serializer(), data.toDto())

    /** @throws BackupFileFormatException if [text] isn't valid JSON in this shape at all (corrupt/foreign file). */
    fun decode(text: String): ExportData {
        val dto = try {
            json.decodeFromString(ExportDataDto.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFileFormatException("Not a valid FlashLearn backup file", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFileFormatException("Not a valid FlashLearn backup file", e)
        }
        return try {
            dto.toDomain()
        } catch (e: IllegalArgumentException) {
            // A bad enum name (Stage/ReviewType/...) or an unparsable UUID/Instant surfaces here.
            throw BackupFileFormatException("Backup file contains an invalid value", e)
        }
    }
}

/** A file that isn't valid JSON in the expected shape, or has a value invalid for its domain type. */
class BackupFileFormatException(message: String, cause: Throwable) : Exception(message, cause)

@Serializable
private data class ExportDataDto(
    val schemaVersion: Int,
    val exportedAt: String,
    val backupType: String,
    val languages: List<LanguageDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val languagePairs: List<LanguagePairDto> = emptyList(),
    val concepts: List<ConceptDto> = emptyList(),
    val contents: List<ContentDto> = emptyList(),
    val conceptTags: List<ConceptTagDto> = emptyList(),
    val reviewSessions: List<ReviewSessionDto> = emptyList(),
    val reviewHistory: List<ReviewHistoryDto> = emptyList(),
    val learningStates: List<LearningStateDto> = emptyList(),
    val difficultyStates: List<DifficultyStateDto> = emptyList(),
    val settings: List<AppSettingDto> = emptyList(),
    val achievements: List<AchievementDto> = emptyList()
)

@Serializable
private data class LanguageDto(val id: String, val code: String, val name: String)

@Serializable
private data class LanguagePairDto(val id: String, val sourceLanguage: String, val targetLanguage: String, val isActive: Boolean)

@Serializable
private data class CategoryDto(val id: String, val name: String)

@Serializable
private data class TagDto(val id: String, val name: String)

@Serializable
private data class ConceptDto(
    val id: String, val entryType: String, val categoryId: String?, val favorite: Boolean,
    val active: Boolean, val createdAt: String, val updatedAt: String, val dataVersion: Int = 0
)

@Serializable
private data class ContentDto(
    val id: String, val conceptId: String, val languageCode: String, val text: String,
    val canonicalKey: String, val notes: String? = null, val pronunciation: String? = null,
    val example: String? = null, val dataVersion: Int = 0
)

@Serializable
private data class ConceptTagDto(val conceptId: String, val tagId: String)

@Serializable
private data class ReviewSessionDto(val id: String, val startedAt: String, val endedAt: String?, val reviewType: String)

@Serializable
private data class ReviewHistoryDto(
    val id: String, val sessionId: String, val reviewAttemptId: String, val conceptId: String,
    val reviewedAt: String, val isCorrect: Boolean, val reviewType: String
)

@Serializable
private data class LearningStateDto(
    val id: String, val conceptId: String, val stage: String, val nextReviewAt: String?,
    val monthlyWrongCount: Int, val hasPathFailure: Boolean, val totalCorrect: Int,
    val totalWrong: Int, val lastReviewedAt: String?
)

@Serializable
private data class DifficultyStateDto(
    val id: String, val conceptId: String, val current: String,
    val consecutiveCorrect: Int, val consecutiveWrong: Int, val hasReachedVeryHard: Boolean
)

@Serializable
private data class AppSettingDto(val key: String, val value: String, val updatedAt: String)

@Serializable
private data class AchievementDto(val type: String, val isUnlocked: Boolean, val unlockedAt: String?)

// ------------------------------------------------------------------
// domain -> dto
// ------------------------------------------------------------------

private fun ExportData.toDto() = ExportDataDto(
    schemaVersion = schemaVersion,
    exportedAt = exportedAt.toString(),
    backupType = backupType.name,
    languages = languages.map { LanguageDto(it.id.toString(), it.code, it.name) },
    categories = categories.map { CategoryDto(it.id.toString(), it.name) },
    tags = tags.map { TagDto(it.id.toString(), it.name) },
    languagePairs = languagePairs.map {
        LanguagePairDto(it.id.toString(), it.sourceLanguage, it.targetLanguage, it.isActive)
    },
    concepts = concepts.map {
        ConceptDto(
            it.id.toString(), it.entryType.name, it.categoryId?.toString(), it.favorite,
            it.active, it.createdAt.toString(), it.updatedAt.toString(), it.dataVersion
        )
    },
    contents = contents.map {
        ContentDto(
            it.id.toString(), it.conceptId.toString(), it.languageCode, it.text, it.canonicalKey,
            it.notes, it.pronunciation, it.example, it.dataVersion
        )
    },
    conceptTags = conceptTags.map { ConceptTagDto(it.conceptId.toString(), it.tagId.toString()) },
    reviewSessions = reviewSessions.map {
        ReviewSessionDto(it.id.toString(), it.startedAt.toString(), it.endedAt?.toString(), it.reviewType.name)
    },
    reviewHistory = reviewHistory.map {
        ReviewHistoryDto(
            it.id.toString(), it.sessionId.toString(), it.reviewAttemptId.toString(), it.conceptId.toString(),
            it.reviewedAt.toString(), it.isCorrect, it.reviewType.name
        )
    },
    learningStates = learningStates.map {
        LearningStateDto(
            it.id.toString(), it.conceptId.toString(), it.stage.name, it.nextReviewAt?.toString(),
            it.monthlyWrongCount, it.hasPathFailure, it.totalCorrect, it.totalWrong, it.lastReviewedAt?.toString()
        )
    },
    difficultyStates = difficultyStates.map {
        DifficultyStateDto(
            it.id.toString(), it.conceptId.toString(), it.current.name,
            it.consecutiveCorrect, it.consecutiveWrong, it.hasReachedVeryHard
        )
    },
    settings = settings.map { AppSettingDto(it.key, it.value, it.updatedAt.toString()) },
    achievements = achievements.map { AchievementDto(it.type.name, it.isUnlocked, it.unlockedAt?.toString()) }
)

// ------------------------------------------------------------------
// dto -> domain
// ------------------------------------------------------------------

private fun ExportDataDto.toDomain() = ExportData(
    schemaVersion = schemaVersion,
    exportedAt = Instant.parse(exportedAt),
    backupType = BackupType.valueOf(backupType),
    languages = languages.map { Language(UUID.fromString(it.id), it.code, it.name) },
    categories = categories.map { Category(UUID.fromString(it.id), it.name) },
    tags = tags.map { Tag(UUID.fromString(it.id), it.name) },
    languagePairs = languagePairs.map {
        LanguagePair(UUID.fromString(it.id), it.sourceLanguage, it.targetLanguage, it.isActive)
    },
    concepts = concepts.map {
        Concept(
            UUID.fromString(it.id), EntryType.valueOf(it.entryType), it.categoryId?.let(UUID::fromString),
            it.favorite, it.active, Instant.parse(it.createdAt), Instant.parse(it.updatedAt), it.dataVersion
        )
    },
    contents = contents.map {
        Content(
            UUID.fromString(it.id), UUID.fromString(it.conceptId), it.languageCode, it.text, it.canonicalKey,
            it.notes, it.pronunciation, it.example, it.dataVersion
        )
    },
    conceptTags = conceptTags.map { ConceptTag(UUID.fromString(it.conceptId), UUID.fromString(it.tagId)) },
    reviewSessions = reviewSessions.map {
        ReviewSession(
            UUID.fromString(it.id), Instant.parse(it.startedAt), it.endedAt?.let(Instant::parse), ReviewType.valueOf(it.reviewType)
        )
    },
    reviewHistory = reviewHistory.map {
        ReviewHistory(
            UUID.fromString(it.id), UUID.fromString(it.sessionId), UUID.fromString(it.reviewAttemptId),
            UUID.fromString(it.conceptId), Instant.parse(it.reviewedAt), it.isCorrect, ReviewType.valueOf(it.reviewType)
        )
    },
    learningStates = learningStates.map {
        LearningState(
            UUID.fromString(it.id), UUID.fromString(it.conceptId), Stage.valueOf(it.stage),
            it.nextReviewAt?.let(Instant::parse), it.monthlyWrongCount, it.hasPathFailure,
            it.totalCorrect, it.totalWrong, it.lastReviewedAt?.let(Instant::parse)
        )
    },
    difficultyStates = difficultyStates.map {
        DifficultyState(
            UUID.fromString(it.id), UUID.fromString(it.conceptId), VocabularyDifficulty.valueOf(it.current),
            it.consecutiveCorrect, it.consecutiveWrong, it.hasReachedVeryHard
        )
    },
    settings = settings.map { AppSetting(it.key, it.value, Instant.parse(it.updatedAt)) },
    achievements = achievements.map {
        Achievement(AchievementType.valueOf(it.type), it.isUnlocked, it.unlockedAt?.let(Instant::parse))
    }
)
