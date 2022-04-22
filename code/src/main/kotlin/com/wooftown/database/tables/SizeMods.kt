package com.wooftown.database.tables

import org.jetbrains.exposed.sql.Table

object SizeMods : Table("size_mods") {
    val sizeId = integer("size_id").autoIncrement().uniqueIndex()
    val sizeCm = integer("size_cm").uniqueIndex().check { it greater 0 }
    val sizeMod = decimal("size_mod", 4, 2).check { it greater 0 }

    override val primaryKey: PrimaryKey = PrimaryKey(sizeId)
}