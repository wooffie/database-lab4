package com.wooftown.database.tables

import org.jetbrains.exposed.sql.Table

object Restaurant : Table("restaurant") {
    val restaurantId = integer("restaurant_id").autoIncrement().uniqueIndex()
    val restaurantAddress = varchar("restaurant_address", 100).uniqueIndex()
}