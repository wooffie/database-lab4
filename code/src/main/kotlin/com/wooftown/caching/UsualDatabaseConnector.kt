package com.wooftown.caching

import com.github.javafaker.Faker
import com.wooftown.database.DatabaseFactory
import com.wooftown.database.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object UsualDatabaseConnector : DatabaseConnector() {

    override fun checkMenu(): List<ResultRow> {
        return transaction {
            Pizza.selectAll().map { it }
        }
    }

    override fun checkOrderPositions(orderId: Int): List<ResultRow> {
        return transaction {
            PizzaInOrder.select { PizzaInOrder.orderId eq orderId }.map { it }
        }
    }

    override fun checkOrder(orderId: Int): List<ResultRow> {
        return transaction {
            Order.join(Delivery, joinType = JoinType.FULL).select { Order.orderId eq orderId }.map { it }
        }
    }

    override fun addPizza(orderId: Int, pizzaId: Int, amount: Int) {


        val restaurantId = getRestaurantByOrder(orderId)
        val employee = getProbablyEmployees(restaurantId, Post.postCook eq true).random()

        transaction {
            PizzaInOrder.insert { row ->
                row[PizzaInOrder.pizzaId] = pizzaId
                row[PizzaInOrder.amount] = amount
                row[PizzaInOrder.orderId] = orderId
                row[employeeId] = employee
            }
        }
    }

    override fun changePizzaAmount(orderId: Int, pizzaId: Int, newAmount: Int) {
        transaction {
            PizzaInOrder.update({ (PizzaInOrder.orderId eq orderId) and (PizzaInOrder.pizzaId eq pizzaId) }) { row ->
                row[amount] = newAmount
            }
        }
    }

    override fun changeType(orderId: Int, newType: ORDERTYPE) {
        val order = transaction {
            Order.select { Order.orderId eq orderId }.map { it }
        }.first()

        val oldType = order[Order.orderType]

        if (oldType == newType) {
            return
        }
        if (oldType == ORDERTYPE.Delivery) {
            transaction {
                Delivery.deleteWhere { Delivery.orderId eq orderId }
            }
        }
        if (newType == ORDERTYPE.Delivery) {
            val restaurantId = getRestaurantByOrder(orderId)

            val employee = getProbablyEmployees(restaurantId, Post.postDelivery eq true).random()

            transaction {
                Delivery.insert { row ->
                    row[Delivery.orderId] = orderId
                    row[employeeId] = employee
                    row[deliveryAddress] = Faker.instance().address().streetAddress()
                }
            }
        }

        transaction {
            Order.update({ Order.orderId eq orderId }) { row ->
                row[orderType] = newType
            }
        }

    }

    override fun changeRestaurant(orderId: Int, newRestaurantId: Int) {
        val cooker = getProbablyEmployees(newRestaurantId, Op.build { Post.postCook eq true }).random()
        val cashier = getProbablyEmployees(newRestaurantId, Op.build { Post.postCashbox eq true }).random()
        val deliveryman = getProbablyEmployees(newRestaurantId, Op.build { Post.postDelivery eq true }).random()
        transaction {
            Order.update({ Order.orderId eq orderId }) { row ->
                row[restaurantId] = newRestaurantId
                row[employeeId] = cashier
            }
            PizzaInOrder.update({ PizzaInOrder.orderId eq orderId }) { row ->
                row[employeeId] = cooker
            }
            Delivery.update({ Delivery.orderId eq orderId }) { row ->
                row[employeeId] = deliveryman
            }
        }

    }

    override fun changeDeliveryAddress(orderId: Int, newAddress: String) {
        transaction {
            Delivery.update({ Delivery.orderId eq orderId }) { row ->
                row[deliveryAddress] = newAddress
            }
        }
    }

    override fun removePizza(orderId: Int, pizzaId: Int) {
        transaction {
            PizzaInOrder.deleteWhere { (PizzaInOrder.orderId eq orderId) and (PizzaInOrder.pizzaId eq pizzaId) }
        }
    }


    private fun getRestaurantByOrder(orderId: Int): Int {
        return transaction {
            Order.select { Order.orderId eq orderId }.map { it }
        }.first()[Order.restaurantId]
    }


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


fun main() {
    DatabaseFactory.connect()
    //println(UsualDatabaseConnector.checkMenu())
    println(UsualDatabaseConnector.checkOrderPositions(7019))
    println(UsualDatabaseConnector.checkOrder(7019))
    transaction {
        PizzaInOrder.deleteWhere { (PizzaInOrder.pizzaId eq 515) and (PizzaInOrder.orderId eq 7019) }
    }
    UsualDatabaseConnector.addPizza(7019, 515, 3)
    println(UsualDatabaseConnector.checkOrderPositions(7019))
    println(UsualDatabaseConnector.checkOrder(7019))
    UsualDatabaseConnector.changePizzaAmount(7019, 515, 4)
    println(UsualDatabaseConnector.checkOrderPositions(7019))
    println(UsualDatabaseConnector.checkOrder(7019))
    UsualDatabaseConnector.changeType(7019, ORDERTYPE.Delivery)
    println(UsualDatabaseConnector.checkOrderPositions(7019))
    println(UsualDatabaseConnector.checkOrder(7019))
    UsualDatabaseConnector.changeType(7019, ORDERTYPE.Pickup)
    println(UsualDatabaseConnector.checkOrderPositions(7019))
    println(UsualDatabaseConnector.checkOrder(7019))
}