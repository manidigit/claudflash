package com.claudemani.data.mapper

import com.claudemani.database.entity.CategoryEntity
import com.claudemani.domain.model.Category

object CategoryMapper {
    fun toDomain(e: CategoryEntity): Category = Category(id = e.id, name = e.name)
    fun toEntity(d: Category): CategoryEntity = CategoryEntity(id = d.id, name = d.name)
}
