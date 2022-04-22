package com.wooftown.database.tables

import org.jetbrains.exposed.sql.Table

object PizzaInOrder : Table("pizza_in_order") {
    val pizzaId = integer("pizza_id").references(Pizza.pizzaId)
    val orderId = integer("order_id").references(Order.orderId)

    init {
        index(true, pizzaId, orderId)
    }

    val amount = integer("amount")
    val employeeId = integer("employee_id").references(Employee.employeeId)
}