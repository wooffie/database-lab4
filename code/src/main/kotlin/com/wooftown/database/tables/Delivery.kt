package com.wooftown.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Delivery : Table("delivery") {
    val orderId = integer("order_id").references(Order.orderId, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val employeeId = integer("employee_id").references(Employee.employeeId, onDelete = ReferenceOption.RESTRICT)
    val deliveryAddress = varchar("delivery_address", 100)
}