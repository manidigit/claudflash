package com.flashlearn.data.mapper

import com.flashlearn.database.entity.DifficultyStateEntity
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.VocabularyDifficulty

object DifficultyStateMapper {
    fun toDomain(e: DifficultyStateEntity): DifficultyState = DifficultyState(
        id = e.id,
        conceptId = e.conceptId,
        current = VocabularyDifficulty.valueOf(e.current),
        consecutiveCorrect = e.consecutiveCorrect,
        consecutiveWrong = e.consecutiveWrong,
        hasReachedVeryHard = e.hasReachedVeryHard
    )

    fun toEntity(d: DifficultyState): DifficultyStateEntity = DifficultyStateEntity(
        id = d.id,
        conceptId = d.conceptId,
        current = d.current.name,
        consecutiveCorrect = d.consecutiveCorrect,
        consecutiveWrong = d.consecutiveWrong,
        hasReachedVeryHard = d.hasReachedVeryHard
    )
}
