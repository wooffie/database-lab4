package com.wooftown.database


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {


    fun connect() : Database {
        return Database.connect(hikari())
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/lab4"
        config.username = "postgres"
        config.password = ""
        config.maximumPoolSize = 10
        config.isAutoCommit = false
        // !!!!!!!!!
        config.addDataSourceProperty("cachePrepStmts", "false")
        // !!!!!!!!!
        config.transactionIsolation = "TRANSACTION_READ_COMMITTED"

        config.validate()
        return HikariDataSource(config)
    }

}

