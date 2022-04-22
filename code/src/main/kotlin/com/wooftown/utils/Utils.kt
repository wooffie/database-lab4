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