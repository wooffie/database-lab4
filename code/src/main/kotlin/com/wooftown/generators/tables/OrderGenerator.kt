package com.wooftown.generators.tables


import com.github.javafaker.Faker
import com.wooftown.database.tables.*
import com.wooftown.database.tables.PizzaInOrder.amount
import com.wooftown.database.tables.PizzaInOrder.employeeId
import com.wooftown.database.tables.PizzaInOrder.orderId
import com.wooftown.database.tables.PizzaInOrder.pizzaId
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class OrderGenerator : Generator {

    private lateinit var pizzaIds: List<Int>

    override fun generate(num: Int) {
        val restaurantIds = transaction {
            Restaurant.selectAll().map { it[Restaurant.restaurantId] }
        }

        pizzaIds = transaction {
            Pizza.selectAll().map { it[Pizza.pizzaId] }
        }
        val oldStart = Instant.now() - Duration.ofDays(365)
        val oldEnd = Instant.now() - Duration.ofHours(5)
        val oldNumber = ((num / 365.0) * 364.0).toInt()
        val newNumber = num - oldNumber
        var counter = 0
        while (counter != num) {
            val orderRestaurant = restaurantIds.random()
            val orderCashbox = getProbablyEmployees(orderRestaurant, Op.build { Post.postCashbox eq true })
            if (orderCashbox.isEmpty()) continue
            val orderCooker = getProbablyEmployees(orderRestaurant, Op.build { Post.postCook eq true })
            if (orderCooker.isEmpty()) continue
            val orderDelivery = getProbablyEmployees(orderRestaurant, Op.build { Post.postDelivery eq true })
            val orderStatus = if (counter < newNumber) ORDERSTATUS.Completed else ORDERSTATUS.values().random()
            val orderTime =
                if (counter < newNumber) generateBetween(oldStart, oldEnd) else generateBetween(oldEnd, Instant.now())
            var orderType = ORDERTYPE.values().random()
            if (orderType == ORDERTYPE.Delivery) {
                if (orderDelivery.isEmpty()) orderType = ORDERTYPE.Pickup
            }
            transaction {
                val newOrderId = Order.insert { row ->
                    row[restaurantId] = orderRestaurant
                    row[employeeId] = orderCashbox.random()
                    row[orderDate] = orderTime
                    row[Order.orderType] = orderType
                    row[Order.orderStatus] = orderStatus
                } get Order.orderId
                generatePositions(orderCooker.random(), newOrderId)
                if (orderType == ORDERTYPE.Delivery) {
                    generateDelivery(orderDelivery.random(), newOrderId)
                }
            }
            counter++
        }
    }

    private fun generateBetween(startInclusive: Instant, endExclusive: Instant): Instant {
        return Instant.ofEpochSecond(Random.nextLong(startInclusive.epochSecond, endExclusive.epochSecond))
    }

    private fun getProbablyEmployees(restaurantId: Int, op: Op<Boolean>): List<Int> {
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

    private fun generatePositions(positionEmployee: Int, positionOrder: Int) {
        val orderSize = (1..5).random()
        val pizzas = pizzaIds.shuffled().take(orderSize)
        transaction {
            PizzaInOrder.batchInsert(pizzas) { batch ->
                this[pizzaId] = batch
                this[orderId] = positionOrder
                this[amount] = (1..3).random()
                this[employeeId] = positionEmployee
            }
        }
    }

    private fun generateDelivery(deliveryEmployee: Int, deliveryOrder: Int) {
        transaction {
            Delivery.insert { row ->
                row[orderId] = deliveryOrder
                row[employeeId] = deliveryEmployee
                row[deliveryAddress] = Faker.instance().address().streetAddress()
            }

        }
    }

}