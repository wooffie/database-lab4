package com.wooftown.caching

import com.wooftown.database.DatabaseFactory
import com.wooftown.database.tables.*
import com.wooftown.utils.getProbablyEmployees
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object CachingDataBaseConnector : DatabaseConnector() {

    var cachingEnabled = true

    object Cache : LRU<CacheKey, List<ResultRow>>(100)

    override fun checkMenu(): List<ResultRow>? {
        if (cachingEnabled) {
            val key = MenuKey
            if (Cache[key] != null) {
                return Cache[key]!!
            } else {
                val result = UsualDatabaseConnector.checkMenu()
                Cache[key] = result
                return result
            }
        } else {
            return UsualDatabaseConnector.checkMenu()
        }
    }

    override fun checkOrderPositions(orderId: Int): List<ResultRow>? {
        if (cachingEnabled) {
            val key = PositionsKey(orderId)
            if (Cache[key] != null) {
                return Cache[key]
            } else {
                val result = UsualDatabaseConnector.checkOrderPositions(orderId)
                Cache[key] = result
                return result
            }
        } else {
            return UsualDatabaseConnector.checkOrderPositions(orderId)
        }
    }

    override fun checkOrder(orderId: Int): List<ResultRow>? {
        if (cachingEnabled) {
            val key = OrderKey(orderId)
            if (Cache[key] != null) {
                return Cache[key]
            } else {
                val result = UsualDatabaseConnector.checkOrder(orderId)
                Cache[key] = result
                return result
            }
        }
        return UsualDatabaseConnector.checkOrder(orderId)


    }

    override fun addPizza(orderId: Int, pizzaId: Int, amount: Int) {
        UsualDatabaseConnector.addPizza(orderId, pizzaId, amount)
        if (cachingEnabled) {
            Cache.remove(OrderKey(orderId))
            Cache.remove(PositionsKey(orderId))
        }
    }

    override fun changePizzaAmount(orderId: Int, pizzaId: Int, newAmount: Int) {
        UsualDatabaseConnector.changePizzaAmount(orderId, pizzaId, newAmount)
        if (cachingEnabled) {
            Cache.remove(OrderKey(orderId))
            Cache.remove(PositionsKey(orderId))
        }
    }

    override fun changeType(orderId: Int, newType: ORDERTYPE) {
        UsualDatabaseConnector.changeType(orderId, newType)
        if (cachingEnabled) {
            Cache.remove(OrderKey(orderId))
        }
    }

    override fun changeRestaurant(orderId: Int, newRestaurantId: Int) {
        UsualDatabaseConnector.changeRestaurant(orderId, newRestaurantId)
        if (cachingEnabled) {
            Cache.remove(OrderKey(orderId))
        }
    }

    override fun changeDeliveryAddress(orderId: Int, newAddress: String) {
        UsualDatabaseConnector.changeDeliveryAddress(orderId, newAddress)
        if (cachingEnabled) {
            Cache.remove(OrderKey(orderId))
        }
    }

    override fun removePizza(orderId: Int, pizzaId: Int) {
        UsualDatabaseConnector.removePizza(orderId, pizzaId)
        if (cachingEnabled) {
            Cache.remove(OrderKey(orderId))
            Cache.remove(PositionsKey(orderId))
        }
    }

}

