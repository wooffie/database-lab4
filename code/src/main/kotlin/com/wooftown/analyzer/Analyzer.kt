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

fun makeTable(experimentDataList: List<ExperimentData>, transactionLevel: String?) {
    System.out.format("%-35s%-12s%-35s\n", "=".repeat(35), transactionLevel, "=".repeat(35))
    System.out.format("%-35s|%-19s|%-20s|%-20s\n", "OPERATION", "AVERAGE", "SD", "MEDIAN")
    for (experimentData in experimentDataList) {
        val operation = experimentData.name
        val average = java.lang.String.format(
            "%.3f ±%.3f",
            experimentData.mean / 1000000.0,
            experimentData.confidence / 1000000.0
        )
        val deviation = java.lang.String.format("%.3f", experimentData.SD / 1000000.0)
        val median = java.lang.String.format("%.3f", experimentData.median / 1000000.0)
        System.out.format("%-35s|%-20s|%-20s|%-20s\n", operation, average, deviation, median)
    }
    System.out.format("%-35s%-12s%-35s\n", "=".repeat(35), transactionLevel, "=".repeat(35))
}