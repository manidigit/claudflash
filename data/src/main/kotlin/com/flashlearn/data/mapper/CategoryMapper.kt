package com.flashlearn.data.mapper

import com.flashlearn.database.entity.CategoryEntity
import com.flashlearn.domain.model.Category

object CategoryMapper {
    fun toDomain(e: CategoryEntity): Category = Category(id = e.id, name = e.name)
    fun toEntity(d: Category): CategoryEntity = CategoryEntity(id = d.id, name = d.name)
}
