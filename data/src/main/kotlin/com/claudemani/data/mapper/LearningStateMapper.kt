package com.claudemani.data.mapper

import com.claudemani.database.entity.LearningStateEntity
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.Stage

object LearningStateMapper {
    fun toDomain(e: LearningStateEntity): LearningState = LearningState(
        id = e.id,
        conceptId = e.conceptId,
        stage = Stage.valueOf(e.stage),
        nextReviewAt = e.nextReviewAt,
        monthlyWrongCount = e.monthlyWrongCount,
        hasPathFailure = e.hasPathFailure,
        totalCorrect = e.totalCorrect,
        totalWrong = e.totalWrong,
        lastReviewedAt = e.lastReviewedAt
    )

    fun toEntity(d: LearningState): LearningStateEntity = LearningStateEntity(
        id = d.id,
        conceptId = d.conceptId,
        stage = d.stage.name,
        nextReviewAt = d.nextReviewAt,
        monthlyWrongCount = d.monthlyWrongCount,
        hasPathFailure = d.hasPathFailure,
        totalCorrect = d.totalCorrect,
        totalWrong = d.totalWrong,
        lastReviewedAt = d.lastReviewedAt
    )
}
