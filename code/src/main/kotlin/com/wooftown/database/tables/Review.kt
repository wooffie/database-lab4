package com.wooftown.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Review : Table("Review") {
    val pizzaId = integer("pizza_id").references(Pizza.pizzaId, onDelete = ReferenceOption.CASCADE)
    val orderId = integer("order_id").references(Order.orderId, onDelete = ReferenceOption.CASCADE)

    init {
        index(true, pizzaId, orderId)
    }

    val reviewRate = integer("review_rate").check { it greaterEq 0 }.check { it lessEq 0 }
    val reviewText = varchar("review_text", 2000).nullable()
}