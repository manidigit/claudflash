package com.flashlearn.data.mapper

import com.flashlearn.database.entity.ConceptTagEntity
import com.flashlearn.database.entity.TagEntity
import com.flashlearn.domain.model.ConceptTag
import com.flashlearn.domain.model.Tag

object TagMapper {
    fun toDomain(e: TagEntity): Tag = Tag(id = e.id, name = e.name)
    fun toEntity(d: Tag): TagEntity = TagEntity(id = d.id, name = d.name)
}

object ConceptTagMapper {
    fun toDomain(e: ConceptTagEntity): ConceptTag = ConceptTag(conceptId = e.conceptId, tagId = e.tagId)
    fun toEntity(d: ConceptTag): ConceptTagEntity = ConceptTagEntity(conceptId = d.conceptId, tagId = d.tagId)
}
