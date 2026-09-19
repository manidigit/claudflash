package com.flashlearn.data.backup

import com.flashlearn.domain.model.Achievement
import com.flashlearn.domain.model.AchievementType
import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.ExportData
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupJsonCodecTest {

    @Test
    fun `encode then decode reproduces the exact same ExportData`() {
        val conceptId = UUID.randomUUID()
        val original = ExportData(
            schemaVersion = 1,
            exportedAt = Instant.parse("2026-09-18T10:00:00Z"),
            backupType = BackupType.FULL,
            concepts = listOf(
                Concept(
                    id = conceptId, entryType = EntryType.WORD, categoryId = null, favorite = false,
                    active = true, createdAt = Instant.parse("2026-09-01T00:00:00Z"),
                    updatedAt = Instant.parse("2026-09-01T00:00:00Z"), dataVersion = 0
                )
            ),
            contents = listOf(
                Content(
                    id = UUID.randomUUID(), conceptId = conceptId, languageCode = "es", text = "hola",
                    canonicalKey = "hola", notes = null, pronunciation = null, example = null, dataVersion = 0
                )
            ),
            learningStates = listOf(
                LearningState(
                    id = UUID.randomUUID(), conceptId = conceptId, stage = Stage.DAILY, nextReviewAt = null,
                    monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = 0, totalWrong = 0, lastReviewedAt = null
                )
            ),
            difficultyStates = listOf(
                DifficultyState(
                    id = UUID.randomUUID(), conceptId = conceptId, current = VocabularyDifficulty.EASY,
                    consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = false
                )
            ),
            achievements = listOf(
                Achievement(AchievementType.FIRST_TEN_WORDS, isUnlocked = true, unlockedAt = Instant.parse("2026-09-10T00:00:00Z"))
            )
        )

        val json = BackupJsonCodec.encode(original)
        val decoded = BackupJsonCodec.decode(json)

        assertEquals(original, decoded)
    }

    @Test
    fun `an empty FULL export round-trips too`() {
        val original = ExportData(schemaVersion = 1, exportedAt = Instant.parse("2026-09-18T10:00:00Z"), backupType = BackupType.FULL)

        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun `garbage text is reported as an invalid backup file, not a raw parser crash`() {
        assertThrows(BackupFileFormatException::class.java) {
            BackupJsonCodec.decode("this is not json at all")
        }
    }

    @Test
    fun `valid json in the wrong shape is also reported as an invalid backup file`() {
        assertThrows(BackupFileFormatException::class.java) {
            BackupJsonCodec.decode("""{"someOtherApp": true}""")
        }
    }

    @Test
    fun `a corrupted enum value is reported as an invalid backup file`() {
        val badJson = BackupJsonCodec.encode(
            ExportData(schemaVersion = 1, exportedAt = Instant.now(), backupType = BackupType.FULL)
        ).replace("\"FULL\"", "\"NOT_A_REAL_BACKUP_TYPE\"")

        assertThrows(BackupFileFormatException::class.java) {
            BackupJsonCodec.decode(badJson)
        }
    }
}
