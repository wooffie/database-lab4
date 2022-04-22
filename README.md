# Лабораторная работа 4: Кэширование

## Цели работы

Знакомство студентов с алгоритмами кэширования.
В рамках данной работы необходимо разработать кэширующий SQL-proxy - программу, которая принимала бы запросы к БД, отправляла эти запросы в БД, сохраняла бы результаты в хранилище. Если приходит повторный запрос на чтение - выдавала запросы из хранилища, если приходит запрос на изменение - сбрасывала бы значения всех запросов, результаты которых станут неактуальными после внесенных изменений.

## Программа работы

1. Выбор понравившегося способа кэширования:
    - **в памяти программы**
    - с использованием внешних хранилищ
    - во внешней памяти
2. Реализация выбранного способа
    - преобразование входных запросов
    - выбор ключа для хранения результатов
    - реализация алгоритма поиска сохраненных результатов, которые надо сбросить после внесения изменений в БД
3. Снятие показательных характеристик
    - реализация дополнительной программы для формирования потока запросов к БД на чтение/изменение/удаление с возможностью настройки соотношения запросов, количества запросов разных типов в потоке и измерения временных характеристик: среднее/минимальное/максимальное время выполнения запроса по типу
4. Анализ полученных результатов и сравнение реализаций с кэшем и без между собой.
5. Демонстрация результатов преподавателю.

## Ход работы:

### **Выбор области запросов и кэширования**

Для данной работы я решил моделировать следующий тип взаимодействия с БД: оформление онлайн заказов пиццы. У посетителя есть список пицц, корзина, и статус его заказа. В основном работа будет происходить с этими таблицами.

Поясню мой выбор: т.к. **описание работы написано довольно расплывчато, то я посчитал, что делать кэширование всей БД будет слишком сложной работой (учитывая когерентность можно назвать её выпуской работой), поэтому я взял частные случаи работы с БД, где можно наглядно посмотрел некоторые алгоритмы и решить проблему актуальности кэшированных данных**. 

Примерный список операций для пользователя (в скобках указываю какие данные могут терять актуальность):
- просмотр меню пицц (I)
- просмотр содержимого заказа (II)
- просмотр заказа (III)
- добавление пиццы (изменение II, III)
- изменение количество пицц в одной позиции (II, III)
- изменение способа доставки (III)
- изменение ресторана для приготовления (II)
- изменение адреса доставки (III)
- удаление пиццы из заказа (II, III)

Кэширование будет происходить в памяти JVM, как алгоритм я использую самый простой LRU.

### **Изменение схемы БД**

В прошлых работах обсуждалось, что заказ не может меняться и поэтому триггер стоял только на добавление позиций. Теперь всё иначе, поэтому я сделал следующую функцию, что позволило изменять данные о заказе, без особых трудностей:

```sql
CREATE OR REPLACE FUNCTION recalc_order(target_id int) RETURNS numeric(10, 2)
AS
$recalc_pizza$
BEGIN
    RETURN (
        SELECT SUM(p.pizza_price * pio.amount)
        FROM "order" o
                 JOIN public.pizza_in_order pio ON o.order_id = pio.order_id
                 JOIN pizza p ON pio.pizza_id = p.pizza_id
        WHERE o.order_id = target_id
    );
END;

$recalc_pizza$ LANGUAGE plpgsql;
```

### **Алгоритм кэширования**

Перед началом работы с БД, надо написать класс кэширования и проверить его в действии:

```kotlin
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
    fun iterator() : MutableIterator<MutableMap.MutableEntry<Key, Value>> {
        return hashMap.iterator()
    }

    // For debug
    fun getInfo() : String{
        return hashMap.toString()
    }

}
```

Функция `get` выдаём нам информацию по запросу, если ключ в ассоциативном словаре присутсвует, то получаем данные, иначе `null`. Также при доступе к данным, надо перезаписать их в очередь, это нужно для организации LRU.

Функция `set` добавляет новые данные в словарь. В этой части также нужно позаботиться о используемой памяти кэша и очереди..

Функция `remove` будет использоваться для удаления неактуальной информации. Сам механизм перехвата данных будет реализован в специальном классе.

Перед началом работы с кэшем проверим его потокобезопасность на простом примере:

```kotlin
fun main(){
    val cyclicBarrier = CyclicBarrier(2)
    val a = AtomicInteger(0)

    val th1 = Thread {
        cyclicBarrier.await()
        var i = 0
        var j = 0

        for (x in 0..5){
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
        for (x in 0..5){
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
```

Вызываем 2 потока, которые будут работать с нашим кэшем. Также добавим переменную `AtomicInteger` для отслеживания порядка работы. Можно видеть, что потоки видят один и тот же кэш, и работают с ним правильно.

```
[1]Added 0=8,cache={0=8} by Thread[Thread-1,5,main]
[2]Added 0=8,cache={0=8} by Thread[Thread-0,5,main] 
[3]Get 1=null,cache={0=8} by Thread[Thread-1,5,main] 
[4]Get 3=null,cache={0=8} by Thread[Thread-0,5,main] 
[6]Added 2=64,cache={0=30, 2=64} by Thread[Thread-0,5,main] 
[5]Added 0=30,cache={0=30, 2=64} by Thread[Thread-1,5,main]
[7]Get 4=null,cache={0=30, 2=64} by Thread[Thread-0,5,main] 
[8]Get 1=null,cache={0=30, 2=64} by Thread[Thread-1,5,main] 
[9]Added 0=41,cache={0=41, 2=64} by Thread[Thread-0,5,main] 
[10]Get 1=null,cache={0=41, 2=64} by Thread[Thread-0,5,main] 
[11]Added 3=85,cache={2=64, 3=85} by Thread[Thread-1,5,main]
[12]Added 3=49,cache={2=64, 3=49} by Thread[Thread-0,5,main] 
[13]Get 0=null,cache={2=64, 3=49} by Thread[Thread-1,5,main] 
[14]Get 0=null,cache={2=64, 3=49} by Thread[Thread-0,5,main] 
[15]Added 0=92,cache={0=92, 2=64, 3=49} by Thread[Thread-1,5,main]
[16]Added 4=72,cache={0=92, 3=49, 4=72} by Thread[Thread-0,5,main] 
[17]Get 3=49,cache={0=92, 3=49, 4=72} by Thread[Thread-1,5,main] 
[19]Added 1=9,cache={1=9, 3=49, 4=72} by Thread[Thread-1,5,main]
[18]Get 4=72,cache={1=9, 3=49, 4=72} by Thread[Thread-0,5,main] 
[20]Get 2=null,cache={1=9, 3=49, 4=72} by Thread[Thread-1,5,main] 
[21]Added 1=21,cache={1=21, 4=72} by Thread[Thread-0,5,main] 
[22]Added 1=64,cache={1=64, 4=72} by Thread[Thread-1,5,main]
[23]Get 4=72,cache={1=64, 4=72} by Thread[Thread-0,5,main] 
[24]Get 4=72,cache={1=64, 4=72} by Thread[Thread-1,5,main] 
{1=64, 4=72}
```

Наглядно видно, что, например в 17 шаге, поток 2 получил данные внесённые потоком 1 в 12 шаге.

### **Связь с БД**

Выше описывались основные операция при работе с БД, давайте реализуем их в виде отдельного класса.

- просмотр меню пицц (I)
- просмотр содержимого заказа (II)
- просмотр заказа (III)
- добавление пиццы (изменение II, III)
- изменение количество пицц в одной позиции (II, III)
- изменение способа доставки (III)
- изменение ресторана для приготовления (II)
- изменение адреса доставки (III)
- удаление пиццы из заказа (II, III)

В кэше необходимо хранить меню пицц, содержимое заказа и информацию о заказе.

Как данные будем использовать `List<ResultRow>`, пусть программисты на фронтенде сами думают, что с этим сделать. А ключ будет характеризовать информацию значения.

Дефолтный коннектор будет выглядеть следующим образом:

```kotlin
object UsualDatabaseConnector : DatabaseConnector() {

    override fun checkMenu(): List<ResultRow>? {
        return transaction {
            Pizza.selectAll().map { it }
        }
    }

    override fun checkOrderPositions(orderId: Int): List<ResultRow>? {
        return transaction {
            PizzaInOrder.select { PizzaInOrder.orderId eq orderId }.map { it }
        }
    }

    override fun checkOrder(orderId: Int): List<ResultRow>? {
        return transaction {
            Order.join(Delivery, joinType = JoinType.FULL).select { Order.orderId eq orderId }.map { it }
        }
    }

    override fun addPizza(orderId: Int, pizzaId: Int, amount: Int) {

        val restaurantId = getRestaurantByOrder(orderId)
        val employee = getProbablyEmployees(restaurantId, Post.postCook eq true).random()

        transaction {
            PizzaInOrder.insert { row ->
                row[PizzaInOrder.pizzaId] = pizzaId
                row[PizzaInOrder.amount] = amount
                row[PizzaInOrder.orderId] = orderId
                row[employeeId] = employee
            }
        }
    }

    override fun changePizzaAmount(orderId: Int, pizzaId: Int, newAmount: Int) {
        transaction {
            PizzaInOrder.update({ (PizzaInOrder.orderId eq orderId) and (PizzaInOrder.pizzaId eq pizzaId) }) { row ->
                row[PizzaInOrder.amount] = newAmount
            }
        }
    }

    override fun changeType(orderId: Int, newType: ORDERTYPE) {
        val order = transaction {
            Order.select { Order.orderId eq orderId }.map { it }
        }.first()

        val oldType = order[Order.orderType]

        if (oldType == newType) {
            return
        }
        if (oldType == ORDERTYPE.Delivery) {
            transaction {
                Delivery.deleteWhere { Delivery.orderId eq orderId }
            }
        }
        if (newType == ORDERTYPE.Delivery) {
            val restaurantId = getRestaurantByOrder(orderId)

            val employee = getProbablyEmployees(restaurantId, Post.postDelivery eq true).random()

            transaction {
                Delivery.insert { row ->
                    row[Delivery.orderId] = orderId
                    row[employeeId] = employee
                    row[deliveryAddress] = Faker.instance().address().streetAddress()
                }
            }
        }

        transaction {
            Order.update({ Order.orderId eq orderId }) { row ->
                row[Order.orderType] = newType
            }
        }

    }

    override fun changeRestaurant(orderId: Int, newRestaurantId: Int) {
        val cooker = getProbablyEmployees(newRestaurantId, Op.build { Post.postCook eq true }).random()
        val cashier = getProbablyEmployees(newRestaurantId, Op.build { Post.postCashbox eq true }).random()
        val deliveryman = getProbablyEmployees(newRestaurantId, Op.build { Post.postDelivery eq true }).random()
        transaction {
            Order.update({ Order.orderId eq orderId }) { row ->
                row[Order.restaurantId] = newRestaurantId
                row[Order.employeeId] = cashier
            }
            PizzaInOrder.update({ PizzaInOrder.orderId eq orderId }) { row ->
                row[PizzaInOrder.employeeId] = cooker
            }
            Delivery.update({ Delivery.orderId eq orderId }) { row ->
                row[Delivery.employeeId] = deliveryman
            }
        }

    }

    override fun changeDeliveryAddress(orderId: Int, newAddress: String) {
        transaction {
            Delivery.update({ Delivery.orderId eq orderId }) { row ->
                row[Delivery.deliveryAddress] = newAddress
            }
        }
    }

    override fun removePizza(orderId: Int, pizzaId: Int) {
        transaction {
            PizzaInOrder.deleteWhere { (PizzaInOrder.orderId eq orderId) and (PizzaInOrder.pizzaId eq pizzaId) }
        }
    }

    private fun getRestaurantByOrder(orderId: Int): Int {
        return transaction {
            Order.select { Order.orderId eq orderId }.map { it }
        }.first()[Order.restaurantId]
    }

}

private fun getProbablyEmployees(restaurantId: Int, op: Op<Boolean>): List<Int> {
    return transaction {
        Employee.join(
            Restaurant,
            JoinType.INNER,
            additionalConstraint = { Employee.restaurantId eq Restaurant.restaurantId })
            .join(Post, JoinType.INNER, additionalConstraint = { Employee.postId eq Post.postId })
            .slice(Employee.employeeId)
            .select { Restaurant.restaurantId eq restaurantId }.andWhere { op }.map { it[Employee.employeeId] }
    }

}
```

В нём уже, как я говорил, есть все функции для работы с БД.

Далее сделаем объект, для кэширования:

```kotlin
object CachingDataBaseConnector : DatabaseConnector() {

    var cachingEnabled = true

    object Cache : LRU<CacheKey, List<ResultRow>>(1000)

    override fun checkMenu(): List<ResultRow>? {
        if (cachingEnabled) {
            val key = MenuKey
            if (Cache[key] != null) {
                return Cache[key]!!
            } else {
                val result = UsualDatabaseConnector.checkMenu()
                if (result != null) {
                    Cache[key] = result
                }
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
                if (result != null) {
                    Cache[key] = result
                }
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
                if (result != null) {
                    Cache[key] = result
                }
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
```

Тут всё просто, в методах, чья информация хранится в кэше пробуем взять её оттуда. В методах, где кэш изменяется, для когерентности удаляем неактуальные данные. Тут используем приём перехвата данных, как например это сделано между ОП и МП в некоторых архитектурах.

Как ключ, используем специальный класс, который хранит индекс к чему мы хотим обратиться.
```kotlin
abstract class CacheKey

class OrderKey(val orderId : Int) : CacheKey() {

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

object MenuKey : CacheKey(){

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
```

Теперь, когда мы имеем весь АПИ, можно приступать к созданию бенчмарка. Первое — это потоки. Для простоты было решено наследоваться от `Thread` и изменять метод `run()`. Так, например, поток для выборки заказа выглядит так:
```kotlin
class ViewOrdersThread(iterations: Int) : CountingThread(iterations) {

    override fun run() {
        DatabaseFactory.connect()
        CYCLICBARRIER.await()
        val cartOrders = transaction {
            Order.select { Order.orderStatus eq ORDERSTATUS.Cart }.map { it[Order.orderId] }
        }
        while (CYCLICBARRIER.numberWaiting < 6) {
            val start = System.nanoTime()
            CachingDataBaseConnector.checkOrder(cartOrders.random())
            time.add(System.nanoTime() - start)
        }
        println("3 ended")
        CYCLICBARRIER.await()
    }
}
```

Все потоки имеют параметр iterations, для удобной параметризации. Также было решено, что потоки выборки будут работать пока не закончат работать другие потоки, поэтому имеет условие по `CyclicBarrier`. Думаю, не очень интересно разжёвывать каждую строчку, поэтому перейдём к главному классу(ну или методу, компилируется всё равно в класс).

```kotlin
fun main() {

    CachingDataBaseConnector.cachingEnabled = false

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
```

Определяем 9 потоков и запускаем их, не забывая про `join`, чтобы главный поток программы не закончил выполнения раньше срока. После того как потоки отработали, выводится содержимое кэша (для интереса) и происходит запись в файлы. 


Сами файлы будем обрабатывать как в `Lab04_2021`, поэтому код и пояснения можно посмотреть там. Результаты при 1000 итераций для каждого потока, анализируем их уже в выводе.

```
===================================/cache      ===================================
OPERATION                          |AVERAGE            |SD                  |MEDIAN     
Check_Menu1000                     |0,007 ±0,000        |0,625               |0,006               
Check_Order1000                    |0,006 ±0,000        |0,062               |0,005               
Check_Positions1000                |0,006 ±0,000        |0,061               |0,005        

Add_Pizza1000                      |2,462 ±0,004        |2,220               |2,149               
Change_Amount1000                  |0,595 ±0,001        |0,715               |0,489               
Change_Delivery_Address1000        |12,684 ±0,036       |17,966              |9,757               
Change_Order_Type1000              |5,594 ±0,038        |19,010              |1,165               
Change_Restaurant1000              |3,313 ±0,005        |2,620               |2,910                      
Remove_Pizza1000                   |1,725 ±0,004        |2,006               |1,486               
===================================/cache      ===================================
===================================/default    ===================================
OPERATION                          |AVERAGE            |SD                  |MEDIAN              
Check_Menu1000                     |0,483 ±0,002        |5,065               |0,399               
Check_Order1000                    |0,240 ±0,000        |0,195               |0,211               
Check_Positions1000                |0,450 ±0,000        |0,239               |0,398 

Add_Pizza1000                      |4,180 ±0,074        |37,100              |2,295               
Change_Amount1000                  |0,607 ±0,002        |0,892               |0,455               
Change_Delivery_Address1000        |12,742 ±0,034       |17,215              |10,163              
Change_Order_Type1000              |6,186 ±0,043        |21,815              |1,219               
Change_Restaurant1000              |5,319 ±0,090        |45,347              |2,952                             
Remove_Pizza1000                   |3,698 ±0,090        |45,561              |1,539               
===================================/default    ===================================

Process finished with exit code 0
```

## Выводы

В ходе данной лабораторной работы был спроектирован LRU-кэш для ускорения выполнения запросов из нашей БД. Я решил использоваться метод хранения данных в программе, потому что у меня не так много данных в таблицах и прирост был бы заметен только при таком способе. 

Что касается LRU - классический алгоритм, который показал на практике свою работоспособность. Как говорили на занятиях по ЭВМ: это не самый лучший алгоритм, но лучше остальных.

Пул соединений Hikari позволил упростить процесс подключения к БД. Для каждого потока - своё подключение.

Результаты: я считаю, что результаты можно назвать успешными, т.к. время выполнения запросов отличается на 2 порядка. Конечно остальные запросы я вывел просто так, для наглядности их значения постоянно меняются из-за невозможности проведения теста в ваакуме, но отличие на порядок, действительно вселяет оптимизм насчёт этой программы.

Я считаю что можно ещё сильней повысить быстродействие, но тогда надо  бесконечно играться с количеством итераций, размером БД, размером кэша. Ведь сама концепция кэша - компромисс между большой и медленной памятью (Postgre в нашем случае) и быстрой но маленькой (JVM).

Также может показаться, что добавления меню в кэш это какой-то бред. Я согласен с этим, на практике я думаю, в кэше бы хранились слайсы от меню. Но я решил попробовать это и посмотреть за быстродействием.