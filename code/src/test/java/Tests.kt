import com.wooftown.database.DatabaseFactory
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier

class Tests {

    @Test
    fun multiplyConnections(){
        val cyclicBarrier = CyclicBarrier(2)

        val t1 = Thread{

            val x = DatabaseFactory.connect()
            x.useNestedTransactions = true
            cyclicBarrier.await()
            println(x.useNestedTransactions)
            cyclicBarrier.await()
        }

        val t2 = Thread{
            val x = DatabaseFactory.connect()
            x.useNestedTransactions = false
            cyclicBarrier.await()
            println(x.useNestedTransactions)
            cyclicBarrier.await()
        }
        t1.start()
        t2.start()
        t1.join()
        t2.join()
    }
}