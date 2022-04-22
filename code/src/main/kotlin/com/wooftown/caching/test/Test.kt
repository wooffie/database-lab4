package com.wooftown.caching.test

import com.wooftown.caching.CachingDataBaseConnector
import com.wooftown.caching.test.threads.*
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

fun main() {

    CachingDataBaseConnector.cachingEnabled = true

     val iterations = 1000

    val threadsMap = mutableMapOf(
        "Check_Menu" to ViewMenuThread(iterations),
        "Check_Positions" to ViewPositionsThread(iterations),
        "Check_Order" to ViewOrdersThread(iterations),
        "Add_Pizza" to AddPizzaThread(iterations),
        "Change_Amount" to ChangePizzaAmountThread(iterations),
        "Change_Order_Type" to ChangeTypeThread(iterations),
        "Change_Restaurant" to ChangeRestaurantThread(iterations),
        "Change_Delivery_Address" to ChangeDeliveryAddressThread(iterations),
        "Remove_Pizza" to RemovePizzaThread(iterations)
    )

    threadsMap.forEach { it.value.start() }

    threadsMap.forEach { it.value.join() }

    println(CachingDataBaseConnector.Cache.getInfo())

    for ((key, value) in threadsMap) {
        println("$key - ${value.time.sum() / value.iterations}")
    }

    val directory = "results/" + if (CachingDataBaseConnector.cachingEnabled) "cache/" else "default/"
    for ((key, value) in threadsMap) {
        val filename = directory + "$key${value.iterations}"
        try {
            val fos = FileOutputStream(filename)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(value.time)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}


