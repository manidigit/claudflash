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
    fun `an older backup without lastReviewedAt and with schemaVersion 2 still decodes`() {
        val legacy = """
            {"schemaVersion":2,"exportedAt":"2026-09-18T21:05:50.152599Z","backupType":"FULL",
             "concepts":[{"id":"9fc5aff4-61e1-45d9-885c-f245f9a18e5c","entryType":"WORD","categoryId":null,
               "favorite":false,"active":true,"createdAt":"2026-09-18T21:05:41.609Z","updatedAt":"2026-09-18T21:05:41.609Z"}],
             "contents":[{"id":"00058302-12fb-4930-a338-feac9e8b8dc0","conceptId":"9fc5aff4-61e1-45d9-885c-f245f9a18e5c",
               "languageCode":"es","text":"hola","canonicalKey":"hola","translationIndex":0}],
             "learningStates":[{"id":"e0d55a53-cf30-4cc8-803d-ebe242680871","conceptId":"9fc5aff4-61e1-45d9-885c-f245f9a18e5c",
               "stage":"DAILY","nextReviewAt":"2026-09-18T21:05:41.635Z","monthlyWrongCount":0,"hasPathFailure":false,
               "totalCorrect":0,"totalWrong":0}],
             "difficultyStates":[{"id":"415363f9-319c-42f2-8603-94e51bb57971","conceptId":"9fc5aff4-61e1-45d9-885c-f245f9a18e5c",
               "current":"EASY","consecutiveCorrect":0,"consecutiveWrong":0,"hasReachedVeryHard":false}],
             "parserMetadata":[],"relations":[],"variants":[],"reviewQueue":[]}
        """.trimIndent()

        val decoded = BackupJsonCodec.decode(legacy)

        assertEquals(2, decoded.schemaVersion)
        assertEquals(1, decoded.learningStates.size)
        assertEquals(null, decoded.learningStates[0].lastReviewedAt)
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
