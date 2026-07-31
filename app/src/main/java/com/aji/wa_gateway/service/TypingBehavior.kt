package com.aji.wa_gateway.service

import java.util.Random

object TypingBehavior {
    private val gaussianRandom = Random(System.currentTimeMillis())

    fun calculateDelayPerChar(minDelay: Int, maxDelay: Int): Long {
        val mean = (minDelay + maxDelay) / 2.0
        val stddev = (maxDelay - minDelay) / 4.0
        val gaussian = gaussianRandom.nextGaussian()
        val delay = mean + gaussian * stddev
        return delay.coerceIn(minDelay.toDouble(), maxDelay.toDouble()).toLong()
    }

    fun shouldPause(charIndex: Int, pauseFreq: Int): Boolean {
        if (pauseFreq <= 0) return false
        return charIndex > 0 && charIndex % pauseFreq == 0
    }

    fun getPauseDuration(minPause: Int, maxPause: Int): Long {
        if (minPause >= maxPause) return minPause.toLong()
        val range = maxPause - minPause + 1
        return (minPause + gaussianRandom.nextInt(range)).toLong()
    }

    fun getPresendDelay(baseDelayMs: Int): Long {
        val variation = (baseDelayMs * 0.3).toInt()
        val min = (baseDelayMs - variation).coerceAtLeast(200)
        val range = baseDelayMs + variation - min + 1
        return (min + gaussianRandom.nextInt(range)).toLong()
    }
}
