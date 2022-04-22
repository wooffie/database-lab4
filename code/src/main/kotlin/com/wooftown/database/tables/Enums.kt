package com.wooftown.database.tables

import org.postgresql.util.PGobject

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}

enum class ORDERTYPE {
    Restaurant, Delivery, Pickup
}

enum class ORDERSTATUS {
    Cart ,Accepted, Cooking, Ready, Completed
}