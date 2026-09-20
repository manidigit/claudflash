package com.claudemani.domain.repository

import com.claudemani.domain.model.AppSetting
import com.claudemani.domain.model.Achievement
import com.claudemani.domain.model.AchievementType
import com.claudemani.domain.model.Category
import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.ConceptTag
import com.claudemani.domain.model.Content
import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.model.Language
import com.claudemani.domain.model.LanguagePair
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.ReviewHistory
import com.claudemani.domain.model.ReviewSession
import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.Tag
import java.time.Instant
import java.util.UUID

/**
 * Minimal in-memory fakes for the repository interfaces, used only by
 * tests in this module to confirm the contracts are actually usable end
 * to end before Room implementations exist (Phase 9). Not shipped in the
 * app — these live under src/test only.
 */
class FakeConceptRepository : ConceptRepository {
    private val store = mutableMapOf<UUID, Concept>()

    override suspend fun insert(concept: Concept) {
        store[concept.id] = concept
    }

    override suspend fun update(concept: Concept) {
        store[concept.id] = concept
    }

    override suspend fun getById(id: UUID): Concept? =
        store[id]?.takeIf { it.active }

    override suspend fun findAnyById(id: UUID): Concept? = store[id]

    override suspend fun getAllActive(): List<Concept> =
        store.values.filter { it.active }

    override suspend fun getAll(): List<Concept> = store.values.toList()

    override suspend fun softDelete(id: UUID, now: Instant) {
        store[id]?.let { store[id] = it.copy(active = false, updatedAt = now) }
    }
}

class FakeLearningStateRepository : LearningStateRepository {
    private val byConcept = mutableMapOf<UUID, LearningState>()

    override suspend fun get(conceptId: UUID): LearningState? = byConcept[conceptId]

    override suspend fun upsert(state: LearningState) {
        byConcept[state.conceptId] = state
    }

    override suspend fun getAllByStage(stage: Stage): List<LearningState> =
        byConcept.values.filter { it.stage == stage }

    override suspend fun getDueByStage(stage: Stage, now: Instant): List<LearningState> =
        byConcept.values
            .filter {
                // Local val, not `it.nextReviewAt` inline: a nullable property
                // declared in a different module (domain/main) can't be
                // smart-cast from domain/test — "Smart cast to 'Instant' is
                // impossible" (a real compiler error, found only once this
                // was actually compiled for the first time by real Gradle).
                val dueAt = it.nextReviewAt
                it.stage == stage && dueAt != null && !dueAt.isAfter(now)
            }
            .sortedWith(compareBy({ it.nextReviewAt }, { it.conceptId }))

    override suspend fun getAllDueNonLearned(now: Instant): List<LearningState> =
        byConcept.values
            .filter {
                val dueAt = it.nextReviewAt
                it.stage != Stage.LEARNED && dueAt != null && !dueAt.isAfter(now)
            }

    override suspend fun getAll(): List<LearningState> = byConcept.values.toList()
}

class FakeDifficultyStateRepository : DifficultyStateRepository {
    private val byConcept = mutableMapOf<UUID, DifficultyState>()

    override suspend fun get(conceptId: UUID): DifficultyState? = byConcept[conceptId]

    override suspend fun upsert(state: DifficultyState) {
        byConcept[state.conceptId] = state
    }

    override suspend fun delete(conceptId: UUID) {
        byConcept.remove(conceptId)
    }

    override suspend fun getAll(): List<DifficultyState> = byConcept.values.toList()
}

class FakeReviewHistoryRepository : ReviewHistoryRepository {
    private val store = mutableMapOf<UUID, ReviewHistory>()

    override suspend fun insert(history: ReviewHistory) {
        store[history.id] = history
    }

    override suspend fun getById(id: UUID): ReviewHistory? = store[id]

    override suspend fun getByConceptId(conceptId: UUID): List<ReviewHistory> =
        store.values.filter { it.conceptId == conceptId }

    override suspend fun getBySessionId(sessionId: UUID): List<ReviewHistory> =
        store.values.filter { it.sessionId == sessionId }

    override suspend fun existsByAttemptId(sessionId: UUID, attemptId: UUID): Boolean =
        store.values.any { it.sessionId == sessionId && it.reviewAttemptId == attemptId }

    override suspend fun getDistinctConceptIds(): List<UUID> =
        store.values.map { it.conceptId }.distinct()

    override suspend fun getAll(): List<ReviewHistory> = store.values.toList()
}

class FakeContentRepository : ContentRepository {
    private val store = mutableMapOf<UUID, Content>()

    override suspend fun upsert(content: Content) {
        store[content.id] = content
    }

    override suspend fun getById(id: UUID): Content? = store[id]

    override suspend fun getByConceptIdAndLanguage(conceptId: UUID, languageCode: String): Content? =
        store.values.find { it.conceptId == conceptId && it.languageCode == languageCode }

    override suspend fun getAllByConceptId(conceptId: UUID): List<Content> =
        store.values.filter { it.conceptId == conceptId }

    override suspend fun findByCanonicalKey(languageCode: String, canonicalKey: String): List<Content> =
        store.values.filter { it.languageCode == languageCode && it.canonicalKey == canonicalKey }

    override suspend fun getAllByLanguageCode(languageCode: String): List<Content> =
        store.values.filter { it.languageCode == languageCode }

    override suspend fun getAll(): List<Content> = store.values.toList()
}

class FakeConceptTagRepository : ConceptTagRepository {
    private val store = mutableSetOf<ConceptTag>()

    override suspend fun insert(conceptTag: ConceptTag) {
        store.add(conceptTag)
    }

    override suspend fun delete(conceptTag: ConceptTag) {
        store.remove(conceptTag)
    }

    override suspend fun getTagIdsForConcept(conceptId: UUID): List<UUID> =
        store.filter { it.conceptId == conceptId }.map { it.tagId }

    override suspend fun getConceptIdsForTag(tagId: UUID): List<UUID> =
        store.filter { it.tagId == tagId }.map { it.conceptId }

    override suspend fun getAll(): List<ConceptTag> = store.toList()
}

class FakeSettingsRepository : SettingsRepository {
    private val store = mutableMapOf<String, AppSetting>()

    override suspend fun findByKey(key: String): AppSetting? = store[key]

    override suspend fun put(setting: AppSetting) {
        store[setting.key] = setting
    }

    override suspend fun delete(key: String) {
        store.remove(key)
    }

    override suspend fun getInt(key: String, default: Int): Int =
        store[key]?.value?.toIntOrNull() ?: default

    override suspend fun getBoolean(key: String, default: Boolean): Boolean =
        store[key]?.value?.toBooleanStrictOrNull() ?: default

    override suspend fun getAll(): List<AppSetting> = store.values.toList()
}

class FakeReviewSessionRepository : ReviewSessionRepository {
    private val store = mutableMapOf<UUID, ReviewSession>()

    override suspend fun insert(session: ReviewSession) {
        store[session.id] = session
    }

    override suspend fun update(session: ReviewSession) {
        store[session.id] = session
    }

    override suspend fun getById(id: UUID): ReviewSession? = store[id]

    override suspend fun getAll(): List<ReviewSession> = store.values.toList()
}

class FakeCategoryRepository : CategoryRepository {
    private val store = mutableMapOf<UUID, Category>()

    override suspend fun insert(category: Category) {
        store[category.id] = category
    }

    override suspend fun update(category: Category) {
        store[category.id] = category
    }

    override suspend fun getById(id: UUID): Category? = store[id]

    override suspend fun getAll(): List<Category> = store.values.toList()
}

class FakeTagRepository : TagRepository {
    private val store = mutableMapOf<UUID, Tag>()

    override suspend fun insert(tag: Tag) {
        store[tag.id] = tag
    }

    override suspend fun update(tag: Tag) {
        store[tag.id] = tag
    }

    override suspend fun getById(id: UUID): Tag? = store[id]

    override suspend fun getAll(): List<Tag> = store.values.toList()
}

class FakeLanguageRepository : LanguageRepository {
    private val store = mutableMapOf<UUID, Language>()

    override suspend fun insert(language: Language) {
        store[language.id] = language
    }

    override suspend fun update(language: Language) {
        store[language.id] = language
    }

    override suspend fun getById(id: UUID): Language? = store[id]

    override suspend fun getByCode(code: String): Language? = store.values.find { it.code == code }

    override suspend fun getAll(): List<Language> = store.values.toList()
}

class FakeLanguagePairRepository : LanguagePairRepository {
    private val store = mutableMapOf<UUID, LanguagePair>()

    override suspend fun insert(pair: LanguagePair) {
        store[pair.id] = pair
    }

    override suspend fun update(pair: LanguagePair) {
        store[pair.id] = pair
    }

    override suspend fun getById(id: UUID): LanguagePair? = store[id]

    override suspend fun getActive(): LanguagePair? = store.values.find { it.isActive }

    override suspend fun getAll(): List<LanguagePair> = store.values.toList()
}

class FakeAchievementRepository : AchievementRepository {
    private val store = mutableMapOf<AchievementType, Achievement>()

    override suspend fun findByType(type: AchievementType): Achievement? = store[type]

    override suspend fun upsert(achievement: Achievement) {
        store[achievement.type] = achievement
    }

    override suspend fun getAll(): List<Achievement> = store.values.toList()
}

/** In-memory "transaction" — just runs the block; no real rollback semantics (Room provides those in Phase 9). */
class FakeClaudemaniDatabase : ClaudemaniDatabase {
    override suspend fun <T> withTransaction(block: suspend () -> T): T = block()
}
