package com.wooftown.generators.tables

import com.github.javafaker.Faker
import com.wooftown.database.tables.Employee
import com.wooftown.database.tables.Post
import com.wooftown.database.tables.Restaurant
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class EmployeeGenerator : Generator {


    override fun generate(num: Int) {
        val restaurantIds = transaction {
            Restaurant.slice(Restaurant.restaurantId).selectAll().map { it[Restaurant.restaurantId] }
        }
        val postIds = transaction {
            Post.slice(Post.postId).selectAll().map { it[Post.postId] }
        }
        val faker = Faker()
        for (i in 1..num) {
            transaction {
                Employee.insert { row ->
                    row[employeeName] = faker.name().firstName()
                    row[employeeSurname] = faker.name().lastName()
                    row[employeeFathersName] = faker.name().firstName()
                    row[restaurantId] = restaurantIds.random()
                    row[postId] = postIds.random()
                }
            }
        }
    }

}