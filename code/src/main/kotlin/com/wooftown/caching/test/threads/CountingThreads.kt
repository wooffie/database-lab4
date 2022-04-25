package com.wooftown.caching.test.threads

import com.github.javafaker.Faker
import com.wooftown.caching.CachingDataBaseConnector
import com.wooftown.database.DatabaseFactory
import com.wooftown.database.tables.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.CyclicBarrier


val CYCLICBARRIER = CyclicBarrier(9)


open class CountingThread(val iterations: Int) : Thread() {

    val time = mutableListOf<Long>()
}

class ViewMenuThread(iterations: Int) : CountingThread(iterations) {


    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        while (CYCLICBARRIER.numberWaiting < 6) {
            val start = System.nanoTime()
            CachingDataBaseConnector.checkMenu()
            time.add(System.nanoTime() - start)
        }

        println("1 ended")
        CYCLICBARRIER.await()
    }


}

class ViewPositionsThread(iterations: Int) : CountingThread(iterations) {


    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }

        while (CYCLICBARRIER.numberWaiting < 6) {
            val start = System.nanoTime()
            CachingDataBaseConnector.checkOrderPositions(cartOrders.random())
            time.add(System.nanoTime() - start)
        }

        println("2 ended")
        CYCLICBARRIER.await()
    }
}

class ViewOrdersThread(iterations: Int) : CountingThread(iterations) {


    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }
        while (CYCLICBARRIER.numberWaiting < 6) {
            val start = System.nanoTime()
            CachingDataBaseConnector.checkOrder(cartOrders.random())
            time.add(System.nanoTime() - start)
        }
        println("3 ended")
        CYCLICBARRIER.await()
    }
}

class AddPizzaThread(iterations: Int) : CountingThread(iterations) {
    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }
        val pizzaMenus = transaction {
            Pizza.selectAll().map { it[Pizza.pizzaId] }
        }.toSet()

        for (i in 0..iterations) {
            val order = cartOrders.random()
            val pizzaInOrder = transaction {
                PizzaInOrder.select { PizzaInOrder.orderId eq order }.map { it[PizzaInOrder.pizzaId] }
            }.toSet()
            val newPizza = (pizzaMenus - pizzaInOrder).random()
            val start = System.nanoTime()
            CachingDataBaseConnector.addPizza(order, newPizza, (1..5).random())
            time.add(System.nanoTime() - start)

        }
        println("4 ended")
        CYCLICBARRIER.await()
    }
}

class ChangePizzaAmountThread(iterations: Int) : CountingThread(iterations) {
    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }
        val pizzas = transaction {
            Pizza.selectAll().map { it[Pizza.pizzaId] }
        }
        var i = 0
        while (i != iterations) {
            var success = false
            while (!success) {
                try {
                    val start = System.nanoTime()
                    CachingDataBaseConnector.changePizzaAmount(cartOrders.random(), pizzas.random(), (1..5).random())
                    time.add(System.nanoTime() - start)
                    success = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            i++
        }
        println("5 ended")
        CYCLICBARRIER.await()
    }
}

class ChangeTypeThread(iterations: Int) : CountingThread(iterations) {
    override fun run() {

        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }

        for (i in 0..iterations) {
            val start = System.nanoTime()
            CachingDataBaseConnector.changeType(cartOrders.random(), ORDERTYPE.values().random())
            time.add(System.nanoTime() - start)

        }
        println("6 ended")
        CYCLICBARRIER.await()
    }
}

class ChangeRestaurantThread(iterations: Int) : CountingThread(iterations) {
    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }
        val restaurants = transaction {
            Restaurant.selectAll().map { it[Restaurant.restaurantId] }
        }
        for (i in 0..iterations) {
            val start = System.nanoTime()
            CachingDataBaseConnector.changeRestaurant(cartOrders.random(), restaurants.random())
            time.add(System.nanoTime() - start)
        }
        println("7 ended")
        CYCLICBARRIER.await()
    }
}

class ChangeDeliveryAddressThread(iterations: Int) : CountingThread(iterations) {
    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        for (i in 0..iterations) {
            val cartOrders = transaction {
                Order.select { Order.orderType eq ORDERTYPE.Delivery }.map { it[Order.orderId] }
            }
            val start = System.nanoTime()
            CachingDataBaseConnector.changeDeliveryAddress(
                cartOrders.random(),
                Faker.instance().address().streetAddress()
            )
            time.add(System.nanoTime() - start)
        }
        println("8 ended")
        CYCLICBARRIER.await()
    }
}

class RemovePizzaThread(iterations: Int) : CountingThread(iterations) {
    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }


        var i = 0
        while (i != iterations) {
            var success = false
            while (!success) {
                try {
                    var order = cartOrders.random()
                    var pizzaToRemove = transaction {
                        PizzaInOrder.select { PizzaInOrder.orderId eq order }.map { it[PizzaInOrder.pizzaId] }
                    }
                    while (pizzaToRemove.isEmpty()) {
                        order = cartOrders.random()
                        pizzaToRemove = transaction {
                            PizzaInOrder.select { PizzaInOrder.orderId eq order }.map { it[PizzaInOrder.pizzaId] }
                        }
                    }
                    val start = System.nanoTime()
                    CachingDataBaseConnector.removePizza(order, pizzaToRemove.random())
                    time.add(System.nanoTime() - start)
                    success = true
                } catch (e: org.postgresql.util.PSQLException) {

                }
            }
            i++
        }
        println("9 ended")
        CYCLICBARRIER.await()
    }
}
