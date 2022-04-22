package com.wooftown.analyzer

import org.apache.commons.math3.distribution.TDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

class ExperimentData(private val data: List<Long>, val name : String) {

    fun getDataInMillis() : List<Double> = data.map { it / 1_000_000.0 }

    val statistics = DescriptiveStatistics();

    val mean: Double
    val SD: Double
    val median: Double
    val confidence: Double

    init {
        data.forEach { statistics.addValue(it.toDouble()) }
        this.mean = statistics.mean
        this.SD = statistics.standardDeviation
        this.median = statistics.getPercentile(50.0)
        this.confidence = getConfidenceIntervalWidth()
    }

    private fun getConfidenceIntervalWidth(): Double {
        val significance = 0.95
        val tDist = TDistribution((statistics.n - 1).toDouble())
        val a = tDist.inverseCumulativeProbability(1.0 - significance / 2)
        return a * statistics.standardDeviation / Math.sqrt(statistics.n.toDouble())
    }

}