package com.wooftown.utils


import com.wooftown.database.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

fun readFileAsLinesUsingGetResourceAsStream(fileName: String) =
    { }::class.java.getResourceAsStream(fileName)!!.bufferedReader().readLines()

fun fileFromResource(fileName: String): String = {}::class.java.classLoader.getResource(fileName)!!.file

private val log = LoggerFactory.getLogger({ }.javaClass)

fun warn(string: String) {
    log.warn("GENERATOR: $string")
}


fun existInTable(table: Table, op: Op<Boolean>): Boolean = transaction {
    val existsOp = exists(table.select { op })
    val result = Table.Dual.slice(existsOp).selectAll().first()
    return@transaction result[existsOp]
}

fun getRestaurantByOrder(orderId: Int): Int {
    return transaction {
        Order.select { Order.orderId eq orderId }.map { it }
    }.first()[Order.restaurantId]
}


fun getProbablyEmployees(restaurantId: Int, op: Op<Boolean>): List<Int> {
    return transaction {
        Employee.join(
            Restaurant,
            JoinType.INNER,
            additionalConstraint = { Employee.restaurantId eq Restaurant.restaurantId })
            .join(Post, JoinType.INNER, additionalConstraint = { Employee.postId eq Post.postId })
            .slice(Employee.employeeId)
            .select { Restaurant.restaurantId eq restaurantId }.andWhere { op }.map { it[Employee.employeeId] }
    }
}

fun clearAll() {
    transaction {
        Review.deleteAll()
        Delivery.deleteAll()
        Order.deleteAll()
        PizzaInOrder.deleteAll()
        Pizza.deleteAll()
        IngredientsToPizza.deleteAll()
        Ingredient.deleteAll()
        SizeMods.deleteAll()
        Employee.deleteAll()
        Post.deleteAll()
        Restaurant.deleteAll()
    }
}
