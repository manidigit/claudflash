package com.flashlearn.data.repository

import com.flashlearn.data.mapper.CategoryMapper
import com.flashlearn.data.mapper.ConceptTagMapper
import com.flashlearn.data.mapper.TagMapper
import com.flashlearn.database.dao.CategoryDao
import com.flashlearn.database.dao.ConceptTagDao
import com.flashlearn.database.dao.TagDao
import com.flashlearn.domain.model.Category
import com.flashlearn.domain.model.ConceptTag
import com.flashlearn.domain.model.Tag
import com.flashlearn.domain.repository.CategoryRepository
import com.flashlearn.domain.repository.ConceptTagRepository
import com.flashlearn.domain.repository.TagRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {
    override suspend fun insert(category: Category) = dao.insert(CategoryMapper.toEntity(category))
    override suspend fun update(category: Category) = dao.update(CategoryMapper.toEntity(category))
    override suspend fun getById(id: UUID): Category? = dao.getById(id)?.let(CategoryMapper::toDomain)
    override suspend fun getAll(): List<Category> = dao.getAll().map(CategoryMapper::toDomain)
}

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val dao: TagDao
) : TagRepository {
    override suspend fun insert(tag: Tag) = dao.insert(TagMapper.toEntity(tag))
    override suspend fun update(tag: Tag) = dao.update(TagMapper.toEntity(tag))
    override suspend fun getById(id: UUID): Tag? = dao.getById(id)?.let(TagMapper::toDomain)
    override suspend fun getAll(): List<Tag> = dao.getAll().map(TagMapper::toDomain)
}

@Singleton
class ConceptTagRepositoryImpl @Inject constructor(
    private val dao: ConceptTagDao
) : ConceptTagRepository {
    override suspend fun insert(conceptTag: ConceptTag) = dao.insert(ConceptTagMapper.toEntity(conceptTag))
    override suspend fun delete(conceptTag: ConceptTag) = dao.delete(ConceptTagMapper.toEntity(conceptTag))
    override suspend fun getTagIdsForConcept(conceptId: UUID): List<UUID> = dao.getTagIdsForConcept(conceptId)
    override suspend fun getConceptIdsForTag(tagId: UUID): List<UUID> = dao.getConceptIdsForTag(tagId)
    override suspend fun getAll(): List<ConceptTag> = dao.getAll().map(ConceptTagMapper::toDomain)
}
