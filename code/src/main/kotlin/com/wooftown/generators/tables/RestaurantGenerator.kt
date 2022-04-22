package com.wooftown.generators.tables

import com.github.javafaker.Faker
import com.wooftown.database.tables.Restaurant
import com.wooftown.utils.existInTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction


class RestaurantGenerator : Generator {

    override fun generate(num: Int) {
        val faker = Faker().address()
        for (i in 1..num) {

            var address = faker.streetAddress()
            while (existInTable(Restaurant, Op.build { Restaurant.restaurantAddress eq address })) {
                address = faker.streetAddress()
            }

            transaction {
                Restaurant.insert { row ->
                    row[restaurantAddress] = address
                }
            }
        }
    }


}