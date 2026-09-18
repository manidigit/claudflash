package com.flashlearn.data.mapper

import com.flashlearn.database.entity.ConceptEntity
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.EntryType

object ConceptMapper {
    fun toDomain(e: ConceptEntity): Concept = Concept(
        id = e.id,
        entryType = EntryType.valueOf(e.entryType),
        categoryId = e.categoryId,
        favorite = e.favorite,
        active = e.active,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
        dataVersion = e.dataVersion
    )

    fun toEntity(d: Concept): ConceptEntity = ConceptEntity(
        id = d.id,
        entryType = d.entryType.name,
        categoryId = d.categoryId,
        favorite = d.favorite,
        active = d.active,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt,
        dataVersion = d.dataVersion
    )
}
