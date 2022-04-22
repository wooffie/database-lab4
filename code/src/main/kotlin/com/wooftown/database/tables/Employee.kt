package com.wooftown.database.tables

import org.jetbrains.exposed.sql.Table

object Employee : Table("employee") {
    val employeeId = integer("employee_id").autoIncrement().uniqueIndex()
    val restaurantId = integer("restaurant_id").references(Restaurant.restaurantId)
    val postId = integer("post_id").references(Post.postId)
    val employeeName = varchar("employee_name", 50)
    val employeeSurname = varchar("employee_surname", 50)
    val employeeFathersName = varchar("employee_fathers_name", 50)
}