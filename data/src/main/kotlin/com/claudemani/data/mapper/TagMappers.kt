package com.claudemani.data.mapper

import com.claudemani.database.entity.ConceptTagEntity
import com.claudemani.database.entity.TagEntity
import com.claudemani.domain.model.ConceptTag
import com.claudemani.domain.model.Tag

object TagMapper {
    fun toDomain(e: TagEntity): Tag = Tag(id = e.id, name = e.name)
    fun toEntity(d: Tag): TagEntity = TagEntity(id = d.id, name = d.name)
}

object ConceptTagMapper {
    fun toDomain(e: ConceptTagEntity): ConceptTag = ConceptTag(conceptId = e.conceptId, tagId = e.tagId)
    fun toEntity(d: ConceptTag): ConceptTagEntity = ConceptTagEntity(conceptId = d.conceptId, tagId = d.tagId)
}
