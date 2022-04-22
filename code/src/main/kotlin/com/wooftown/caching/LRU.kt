package com.wooftown.caching

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random.Default.nextInt

open class LRU<Key, Value>(private val size: Int) {

    private val linkedQueue = ConcurrentLinkedQueue<Key>()
    private val hashMap = ConcurrentHashMap<Key, Value>()

    operator fun get(key: Key): Value? {
        val value = hashMap[key]
        if (value != null) {
            linkedQueue.remove(key)
            linkedQueue.add(key)
        }
        return value
    }

    @Synchronized
    operator fun set(key: Key, value: Value) {

        if (hashMap.contains(key)) {
            linkedQueue.remove(key)
        }

        while (linkedQueue.size >= size) {
            val oldestKey = linkedQueue.poll()
            if (oldestKey != null) {
                hashMap.remove(oldestKey)
            }
        }
        linkedQueue.add(key)
        hashMap[key] = value
    }

    @Synchronized
    fun remove(key: Key) {
        if (linkedQueue.contains(key)) {
            linkedQueue.remove(key)
            if (key != null) {
                hashMap.remove(key)
            }
        }

    }

    // For debug
    fun iterator(): MutableIterator<MutableMap.MutableEntry<Key, Value>> {
        return hashMap.iterator()
    }

    fun getInfo(): String {
        return hashMap.toString()
    }

}

object Cache : LRU<Int, Int>(100)


fun main() {
    val cyclicBarrier = CyclicBarrier(2)
    val a = AtomicInteger(0)

    val th1 = Thread {
        cyclicBarrier.await()
        var i = 0
        var j = 0

        for (x in 0..5) {
            i = nextInt(5)
            j = nextInt(100)
            Cache[i] = j
            println("[${a.incrementAndGet()}]Added $i=$j,cache=${Cache.getInfo()} by ${Thread.currentThread()} ")
            i = nextInt(5)
            println("[${a.incrementAndGet()}]Get $i=${Cache[i]},cache=${Cache.getInfo()} by ${Thread.currentThread()} ")

        }

        cyclicBarrier.await()
        println(Cache.getInfo())
        cyclicBarrier.await()
    }
    val th2 = Thread {
        cyclicBarrier.await()
        var i = 0
        var j = 0
        for (x in 0..5) {
            i = nextInt(4)
            j = nextInt(100)
            Cache[i] = j
            println("[${a.incrementAndGet()}]Added $i=$j,cache=${Cache.getInfo()} by ${Thread.currentThread()}")
            i = nextInt(5)
            println("[${a.incrementAndGet()}]Get $i=${Cache[i]},cache=${Cache.getInfo()} by ${Thread.currentThread()} ")
        }
        cyclicBarrier.await()
        cyclicBarrier.await()
    }
    th1.start()
    th2.start()


}