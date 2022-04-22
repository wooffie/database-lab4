package com.wooftown.database.tables

import org.jetbrains.exposed.sql.Table

object Post : Table("post") {
    val postId = integer("post_id").autoIncrement().uniqueIndex()
    val postName = varchar("post_name", 50).uniqueIndex()
    val postCook = bool("post_cook")
    val postCashbox = bool("post_cashbox")
    val postDelivery = bool("post_delivery")
}