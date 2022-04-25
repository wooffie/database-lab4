package com.wooftown.analyzer

import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream


fun main() {
    for (i in listOf("/cache", "/default")) {
        val filedir = File("results" + i)
        val dataFiles = filedir.listFiles()

        val experimentDataList = mutableListOf<ExperimentData>()

        for (file in dataFiles) {
            try {
                val fis = FileInputStream(file)
                val ois = ObjectInputStream(fis)
                val time = ois.readObject() as List<Long>
                experimentDataList.add(ExperimentData(time, file.name))
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        makeTable(experimentDataList, i)

    }


}

fun makeTable(experimentDataList: List<ExperimentData>, type: String?) {
    println("${"=".repeat(56)} [$type] ${"=".repeat(56)} ")
    val format = "%-30s|\t%-20s|\t%-12s|\t%-12s|\t%-15s|\t%-15s|"
    val numberFormat = "%.3f"
    println(format.format("Operation", "Average time [ms]", "SD [ms]", "Median [ms]", "Min [ns]", "Max [ns]"))

    for (experimentData in experimentDataList.sortedBy { it.name }) {
        val op = experimentData.name
        val meanInMillis = experimentData.mean / 1_000_000
        val standardDeviationInMillis = experimentData.SD / 1_000_000
        val medianInMillis = experimentData.median / 1_000_000
        val confidenceInMillis = experimentData.confidence / 1_000_000
        val minInNanos = experimentData.min
        val maxInNanos = experimentData.max
        println(
            format.format(
                op,
                "%s±%s".format(numberFormat.format(meanInMillis), numberFormat.format(confidenceInMillis)),
                numberFormat.format(standardDeviationInMillis),
                numberFormat.format(medianInMillis),
                numberFormat.format(minInNanos),
                numberFormat.format(maxInNanos)
            )
        )
    }

}