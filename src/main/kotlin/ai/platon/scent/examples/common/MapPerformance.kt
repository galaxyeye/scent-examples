package ai.platon.scent.examples.common

import org.apache.commons.lang3.RandomStringUtils
import java.util.*
import kotlin.random.Random

object MapPerformance {
    val usedMemory = usedMemory()

    fun run() {
        val numRecords = 1_000_000
        val reportRound = numRecords / 20
        val varcharSize = 50
        val map = TreeMap<Int, String>()
        val indexes = mutableListOf<TreeMap<Int, Int>>()
        IntRange(1, numRecords).forEach {
            var text = ""
            IntRange(1, 20).forEach {
                text += "$it "
            }
            IntRange(1, 20).forEach {
                text += RandomStringUtils.randomAlphanumeric(varcharSize)
                text += " "
            }
//            IntRange(1, 5).forEach {
//                text += RandomStringUtils.randomAlphanumeric(500)
//                text += " "
//            }
            map[it] = text
            if (it % reportRound == 0) {
                reportMemory(it)
                System.gc()
            }
        }

        val dataUsedMemory = usedMemory()
        IntRange(1, 30).forEach {
            val index = TreeMap<Int, Int>()
            IntRange(1, numRecords).forEach { index[it] = Random(20_000_000).nextInt() }
            indexes.add(index)

            if (it % 5 == 0) {
                reportMemory(it)
                System.gc()
            }
        }

        val totalUsedMemory = usedMemory()
        val totalIndexMemory = totalUsedMemory - dataUsedMemory
        val averageIndexMemory = totalIndexMemory / 30
        val indexSpaceRate = 1.0 * averageIndexMemory / dataUsedMemory

        println("Size: " + map.size)
        println("Record sample: " + map[1000])

        println("Index used memory:\t$totalUsedMemory - $dataUsedMemory = $totalIndexMemory")
        println("totalUsedMemory: $totalUsedMemory " +
                "dataUsedMemory: $dataUsedMemory " +
                "averageIndexMemory: $averageIndexMemory " +
                "indexSpaceRate: $indexSpaceRate " +
                "totalIndexMemory: $totalIndexMemory ")
        reportMemory("Total used memory:\t")
    }

    fun reportMemory(round: Int) {
        val usedMemory2 = usedMemory()
        println("$round.\t$usedMemory2 - $usedMemory = ${usedMemory2 - usedMemory}")
    }

    fun reportMemory(prefix: String) {
        val usedMemory2 = usedMemory()
        println("$prefix$usedMemory2 - $usedMemory = ${usedMemory2 - usedMemory}")
    }

    fun usedMemory(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }
}

fun main() {
    MapPerformance.run()
}
