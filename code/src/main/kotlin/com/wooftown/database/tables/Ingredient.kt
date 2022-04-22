package com.wooftown.database.tables

import org.jetbrains.exposed.sql.Table

object Ingredient : Table("ingredient") {
    val ingredientId = integer("ingredient_id").autoIncrement().uniqueIndex()
    val ingredientName = varchar("ingredient_name", 50).uniqueIndex()
    val ingredientPrice = decimal("ingredient_price", 10, 2).check { it greater 0 }
    override val primaryKey: PrimaryKey = PrimaryKey(ingredientId)
}