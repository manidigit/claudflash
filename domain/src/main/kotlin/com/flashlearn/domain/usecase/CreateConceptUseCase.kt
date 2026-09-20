package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.InvalidConceptInputException
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.ConceptTag
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.ConceptTagRepository
import com.flashlearn.domain.repository.ContentRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import com.flashlearn.domain.repository.FlashLearnDatabase
import com.flashlearn.domain.repository.LearningStateRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * V1 default language pair is Spanish→Persian; either side can be
 * overridden per call (e.g. Persian→Spanish direction, or a future
 * language pair) without changing this UseCase.
 */
data class CreateConceptCommand(
    val sourceText: String,
    val targetText: String,
    val sourceLanguage: String = "es",
    val targetLanguage: String = "fa",
    val categoryId: UUID? = null,
    val notes: String? = null,
    val pronunciation: String? = null,
    val example: String? = null,
    val entryType: EntryType = EntryType.WORD,
    val tags: List<UUID> = emptyList()
)

/**
 * Contract: creates Concept + Content(s) + initial LearningState (DAILY)
 * + initial DifficultyState (EASY) inside a single transaction (Data
 * Model Canonical v1.1, Integrity Rule). If any step fails, everything
 * rolls back — an active Concept without both states must never exist.
 */
class CreateConceptUseCase @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val contentRepository: ContentRepository,
    private val learningStateRepository: LearningStateRepository,
    private val difficultyStateRepository: DifficultyStateRepository,
    private val conceptTagRepository: ConceptTagRepository,
    private val database: FlashLearnDatabase
) {
    suspend operator fun invoke(command: CreateConceptCommand): UUID {
        // Edge Case §8: at least one (in practice both) translation is required.
        if (command.sourceText.isBlank()) {
            throw InvalidConceptInputException("sourceText must not be blank")
        }
        if (command.targetText.isBlank()) {
            throw InvalidConceptInputException("targetText must not be blank")
        }

        return database.withTransaction {
            val conceptId = UUID.randomUUID()
            val now = Instant.now()

            // 1. Concept
            conceptRepository.insert(
                Concept(
                    id = conceptId,
                    entryType = command.entryType,
                    categoryId = command.categoryId,
                    favorite = false,
                    active = true,
                    createdAt = now,
                    updatedAt = now
                )
            )

            // 2. Contents (source + target)
            contentRepository.upsert(
                Content(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    languageCode = command.sourceLanguage,
                    text = command.sourceText,
                    canonicalKey = computeCanonicalKey(command.sourceText),
                    notes = command.notes,
                    pronunciation = command.pronunciation,
                    example = command.example
                )
            )
            contentRepository.upsert(
                Content(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    languageCode = command.targetLanguage,
                    text = command.targetText,
                    canonicalKey = computeCanonicalKey(command.targetText)
                )
            )

            // 3. LearningState — initial DAILY, due right now
            learningStateRepository.upsert(
                LearningState(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    stage = Stage.DAILY,
                    // A brand-new word must be due immediately: the Scheduler's due query
                    // requires nextReviewAt IS NOT NULL for DAILY/WEEKLY/MONTHLY, so a null
                    // here would hide the word from every review queue forever (v1.4.5 fix).
                    nextReviewAt = now,
                    monthlyWrongCount = 0,
                    hasPathFailure = false,
                    totalCorrect = 0,
                    totalWrong = 0,
                    lastReviewedAt = null
                )
            )

            // 4. DifficultyState — initial EASY
            difficultyStateRepository.upsert(
                DifficultyState(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    current = VocabularyDifficulty.EASY,
                    consecutiveCorrect = 0,
                    consecutiveWrong = 0,
                    hasReachedVeryHard = false
                )
            )

            // 5. Optional tags (idempotent insert)
            command.tags.forEach { tagId ->
                conceptTagRepository.insert(ConceptTag(conceptId = conceptId, tagId = tagId))
            }

            conceptId
        }
    }
}
