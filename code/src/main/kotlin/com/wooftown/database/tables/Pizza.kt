package com.wooftown.database.tables

import org.jetbrains.exposed.sql.Table

object Pizza : Table("pizza") {
    val pizzaId = integer("pizza_id").autoIncrement().uniqueIndex()
    val pizzaName = varchar("pizza_name", 50)
    val sizeId = integer("size_id").references(SizeMods.sizeId)
    val pizzaPrice = decimal("pizza_price", 10, 2).default(0.0.toBigDecimal()).check { it greaterEq 0.0 }

    init {
        index(true, pizzaName, sizeId)
    }

}