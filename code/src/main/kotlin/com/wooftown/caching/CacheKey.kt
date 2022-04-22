package com.wooftown.caching


abstract class CacheKey

class OrderKey(val orderId: Int) : CacheKey() {

    override fun hashCode(): Int {
        return orderId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderKey

        if (orderId != other.orderId) return false

        return true
    }
}

object MenuKey : CacheKey() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }
}

class PositionsKey(val orderId: Int) : CacheKey() {
    override fun hashCode(): Int {
        return orderId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PositionsKey

        if (orderId != other.orderId) return false

        return true
    }


}