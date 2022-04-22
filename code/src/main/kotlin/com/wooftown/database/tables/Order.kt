package com.wooftown.database.tables


import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp


object Order : Table("\"order\"") {
    val orderId = integer("order_id").autoIncrement().uniqueIndex()
    val orderPrice = decimal("order_price", 4, 2).default(0.0.toBigDecimal())
    val restaurantId = integer("restaurant_id").references(Restaurant.restaurantId, onDelete = ReferenceOption.CASCADE)
    val employeeId = integer("employee_id").references(Employee.employeeId, onDelete = ReferenceOption.CASCADE)
    val orderDate = timestamp("order_date")
    val orderType = customEnumeration(
        "order_type",
        "type_order",
        { value -> ORDERTYPE.valueOf(value as String) },
        { PGEnum("type_order", it) })
    val orderStatus = customEnumeration(
        "order_status",
        "status_order",
        { value -> ORDERSTATUS.valueOf(value as String) },
        { PGEnum("status_order", it) })

}



