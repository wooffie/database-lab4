package com.wooftown.caching

import com.wooftown.database.tables.ORDERTYPE
import org.jetbrains.exposed.sql.ResultRow

abstract class DatabaseConnector {

    abstract fun checkMenu() : List<ResultRow>?

    abstract fun checkOrderPositions(orderId : Int): List<ResultRow>?

    abstract fun checkOrder(orderId : Int): List<ResultRow>?

    abstract fun addPizza(orderId: Int, pizzaId : Int, amount: Int)

    abstract fun changePizzaAmount(orderId: Int, pizzaId: Int, newAmount : Int)

    abstract fun changeType(orderId: Int, newType : ORDERTYPE)

    abstract fun changeRestaurant(orderId: Int, newRestaurantId : Int)

    abstract fun changeDeliveryAddress(orderId: Int, newAddress :String)

    abstract fun removePizza(orderId: Int, pizzaId: Int)

}

