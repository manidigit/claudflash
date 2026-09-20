package com.claudemani.domain.algorithm

import com.claudemani.domain.model.Achievement
import com.claudemani.domain.model.AchievementType
import com.claudemani.domain.model.AppSetting
import com.claudemani.domain.model.BackupType
import com.claudemani.domain.model.Category
import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.ConceptTag
import com.claudemani.domain.model.Content
import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.model.EntryType
import com.claudemani.domain.model.ExportData
import com.claudemani.domain.model.Language
import com.claudemani.domain.model.LanguagePair
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.ReviewHistory
import com.claudemani.domain.model.ReviewSession
import com.claudemani.domain.model.ReviewType
import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.Tag
import com.claudemani.domain.model.VocabularyDifficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class BackupValidationAlgorithmTest {

    private val now = Instant.parse("2026-09-14T12:00:00Z")

    // --- minimal valid builders — each test assembles its own ExportData from these,
    // never by partially overriding another test's fixture, so errors never leak in
    // from unrelated fields. ---

    private fun language(code: String, id: UUID = UUID.randomUUID()) = Language(id, code, code.uppercase())

    private fun languagePair(source: String, target: String) =
        LanguagePair(UUID.randomUUID(), source, target, isActive = true)

    private fun concept(id: UUID = UUID.randomUUID()) = Concept(
        id = id, entryType = EntryType.WORD, categoryId = null, favorite = false,
        active = true, createdAt = now, updatedAt = now
    )

    private fun content(conceptId: UUID, languageCode: String = "es", text: String = "hola") = Content(
        id = UUID.randomUUID(), conceptId = conceptId, languageCode = languageCode,
        text = text, canonicalKey = text
    )

    private fun learningState(conceptId: UUID, stage: Stage = Stage.DAILY) = LearningState(
        id = UUID.randomUUID(), conceptId = conceptId, stage = stage, nextReviewAt = null,
        monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = 0, totalWrong = 0, lastReviewedAt = null
    )

    private fun difficultyState(conceptId: UUID) = DifficultyState(
        id = UUID.randomUUID(), conceptId = conceptId, current = VocabularyDifficulty.EASY,
        consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = false
    )

    private fun reviewSession(id: UUID = UUID.randomUUID()) = ReviewSession(id, now, null, ReviewType.DAILY)

    private fun reviewHistory(conceptId: UUID, sessionId: UUID) = ReviewHistory(
        id = UUID.randomUUID(), sessionId = sessionId, reviewAttemptId = UUID.randomUUID(),
        conceptId = conceptId, reviewedAt = now, isCorrect = true, reviewType = ReviewType.DAILY
    )

    private fun vocabulary(
        languages: List<Language> = emptyList(),
        categories: List<Category> = emptyList(),
        tags: List<Tag> = emptyList(),
        languagePairs: List<LanguagePair> = emptyList(),
        concepts: List<Concept> = emptyList(),
        contents: List<Content> = emptyList(),
        conceptTags: List<ConceptTag> = emptyList()
    ) = ExportData(
        schemaVersion = 1, exportedAt = now, backupType = BackupType.VOCABULARY,
        languages = languages, categories = categories, tags = tags, languagePairs = languagePairs,
        concepts = concepts, contents = contents, conceptTags = conceptTags
    )

    private fun progress(
        reviewSessions: List<ReviewSession> = emptyList(),
        reviewHistory: List<ReviewHistory> = emptyList(),
        learningStates: List<LearningState> = emptyList(),
        difficultyStates: List<DifficultyState> = emptyList(),
        settings: List<AppSetting> = emptyList(),
        achievements: List<Achievement> = emptyList()
    ) = ExportData(
        schemaVersion = 1, exportedAt = now, backupType = BackupType.PROGRESS,
        reviewSessions = reviewSessions, reviewHistory = reviewHistory, learningStates = learningStates,
        difficultyStates = difficultyStates, settings = settings, achievements = achievements
    )

    private fun validProgressExport(): ExportData {
        val externalConceptId = UUID.randomUUID() // legitimately not part of this backup
        val session = reviewSession()
        return progress(
            reviewSessions = listOf(session),
            reviewHistory = listOf(reviewHistory(externalConceptId, session.id)),
            learningStates = listOf(learningState(externalConceptId)),
            difficultyStates = listOf(difficultyState(externalConceptId)),
            settings = listOf(AppSetting("threshold_difficulty", "3", now)),
            achievements = listOf(Achievement(AchievementType.FIRST_TEN_WORDS, true, now))
        )
    }

    private fun validVocabularyExport(): ExportData {
        val lang = language("es")
        val pair = languagePair("es", "fa")
        val cat = Category(UUID.randomUUID(), "Animals")
        val tag = Tag(UUID.randomUUID(), "Important")
        val c = concept()
        return vocabulary(
            languages = listOf(lang, language("fa")),
            categories = listOf(cat),
            tags = listOf(tag),
            languagePairs = listOf(pair),
            concepts = listOf(c),
            contents = listOf(content(c.id)),
            conceptTags = listOf(ConceptTag(c.id, tag.id))
        )
    }

    private fun validFullExport(): ExportData {
        val c = concept()
        val session = reviewSession()
        return ExportData(
            schemaVersion = 1, exportedAt = now, backupType = BackupType.FULL,
            concepts = listOf(c),
            contents = listOf(content(c.id)),
            reviewSessions = listOf(session),
            reviewHistory = listOf(reviewHistory(c.id, session.id)),
            learningStates = listOf(learningState(c.id)),
            difficultyStates = listOf(difficultyState(c.id))
        )
    }

    // --- tests ---

    @Test
    fun `valid VOCABULARY backup passes`() {
        assertEquals(BackupValidationResult.Valid, validateBackup(validVocabularyExport(), currentSchemaVersion = 1))
    }

    @Test
    fun `valid PROGRESS backup passes with external concept references`() {
        assertEquals(BackupValidationResult.Valid, validateBackup(validProgressExport(), currentSchemaVersion = 1))
    }

    @Test
    fun `valid FULL backup passes`() {
        assertEquals(BackupValidationResult.Valid, validateBackup(validFullExport(), currentSchemaVersion = 1))
    }

    @Test
    fun `schemaVersion above supported range is rejected`() {
        val data = validFullExport().copy(schemaVersion = 5)
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `schemaVersion below 1 is rejected`() {
        val data = validFullExport().copy(schemaVersion = 0)
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `VOCABULARY backup with progress data populated is rejected`() {
        val data = validVocabularyExport().copy(settings = listOf(AppSetting("k", "v", now)))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `PROGRESS backup with vocabulary data populated is rejected`() {
        val data = validProgressExport().copy(concepts = listOf(concept()))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `duplicate Concept id within backup is rejected`() {
        val c = concept()
        val data = vocabulary(concepts = listOf(c, c.copy(favorite = true)))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `duplicate Content (conceptId,languageCode) within backup is rejected`() {
        val c = concept()
        val data = vocabulary(
            concepts = listOf(c),
            contents = listOf(content(c.id, "es", "hola"), content(c.id, "es", "otra"))
        )
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `duplicate LearningState conceptId within backup is rejected`() {
        val c = concept()
        val data = ExportData(
            schemaVersion = 1, exportedAt = now, backupType = BackupType.FULL,
            concepts = listOf(c),
            learningStates = listOf(learningState(c.id), learningState(c.id))
        )
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `Content referencing a Concept not in this backup is rejected`() {
        val data = vocabulary(contents = listOf(content(UUID.randomUUID())))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `ConceptTag referencing an unknown Tag is rejected`() {
        val c = concept()
        val data = vocabulary(concepts = listOf(c), conceptTags = listOf(ConceptTag(c.id, UUID.randomUUID())))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `LanguagePair referencing an unknown language code is rejected`() {
        val data = vocabulary(languages = listOf(language("es")), languagePairs = listOf(languagePair("es", "de")))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `ReviewHistory referencing an unknown session is rejected`() {
        val data = progress(reviewHistory = listOf(reviewHistory(UUID.randomUUID(), UUID.randomUUID())))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `FULL backup with a LearningState referencing a missing Concept is rejected`() {
        val c = concept()
        val session = reviewSession()
        val data = ExportData(
            schemaVersion = 1, exportedAt = now, backupType = BackupType.FULL,
            concepts = listOf(c),
            reviewSessions = listOf(session),
            learningStates = listOf(learningState(UUID.randomUUID()))
        )
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `PROGRESS backup's external concept references are never flagged`() {
        // Structurally the same "LearningState points to a Concept absent from
        // this backup" shape as the FULL case above, but for PROGRESS that
        // absence is exactly what conceptReferences models — must stay Valid.
        assertEquals(BackupValidationResult.Valid, validateBackup(validProgressExport(), currentSchemaVersion = 1))
    }

    @Test
    fun `blank required field is rejected`() {
        val data = vocabulary(categories = listOf(Category(UUID.randomUUID(), "  ")))
        assertTrue(validateBackup(data, currentSchemaVersion = 1) is BackupValidationResult.Invalid)
    }

    @Test
    fun `multiple problems all accumulate rather than short-circuiting on the first`() {
        val c = concept()
        val data = ExportData(
            schemaVersion = 99, exportedAt = now, backupType = BackupType.FULL,
            concepts = listOf(c),
            learningStates = listOf(learningState(UUID.randomUUID()))
        )
        val result = validateBackup(data, currentSchemaVersion = 1) as BackupValidationResult.Invalid
        assertTrue(result.errors.size >= 2)
    }

    @Test
    fun `conceptReferences is derived from reviewHistory, learningStates and difficultyStates`() {
        val export = validProgressExport()
        val expectedIds = export.reviewHistory.map { it.conceptId }.toSet() +
            export.learningStates.map { it.conceptId }.toSet() +
            export.difficultyStates.map { it.conceptId }.toSet()
        assertEquals(expectedIds, export.conceptReferences)
    }
}
