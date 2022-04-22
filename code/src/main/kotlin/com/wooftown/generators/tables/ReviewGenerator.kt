package com.wooftown.generators.tables

import com.github.javafaker.Faker
import com.wooftown.database.tables.PizzaInOrder
import com.wooftown.database.tables.Review
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random.Default.nextInt

class ReviewGenerator : Generator {

    override fun generate(num: Int) {
        val pizzaToOrder = transaction {
            return@transaction listOf(
                PizzaInOrder.slice(PizzaInOrder.pizzaId, PizzaInOrder.orderId).selectAll()
                    .map { it[PizzaInOrder.pizzaId] to it[PizzaInOrder.orderId] }).first().shuffled().take(num)

        }
        val faker = Faker.instance()

        for ((newPizza, newOrder) in pizzaToOrder) {
            transaction {
                Review.insert { row ->
                    row[pizzaId] = newPizza
                    row[orderId] = newOrder
                    row[reviewRate] = (0..5).random()
                    row[reviewText] = if (nextInt() % 3 == 0) faker.lorem().sentence((1..70).random()) else null
                }
            }
        }

    }
}