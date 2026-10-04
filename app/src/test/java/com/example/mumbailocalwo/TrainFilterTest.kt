package com.example.mumbailocalwo

import com.example.mumbailocalwo.data.model.Train
import com.example.mumbailocalwo.presentation.components.TrainFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainFilterTest {

    private fun createTrain(trainType: String, isAC: Boolean): Train {
        return Train(
            id = 1,
            trainNumber = "90001",
            trainName = null,
            line = "WR",
            lineCode = "W",
            direction = "UP",
            sourceStationId = 1,
            destStationId = 10,
            trainType = trainType,
            isAC = isAC,
            is15Car = false,
            isLadiesSpecial = false,
            operatingDays = "Daily",
            departureTime = "10:00",
            arrivalTime = "11:00",
            departureMinutes = 600,
            arrivalMinutes = 660,
            sourceStationName = "Churchgate",
            destStationName = "Borivali"
        )
    }

    @Test
    fun testSlowFilter() {
        val slowRegular = createTrain("SLOW", isAC = false)
        val slowAC = createTrain("SLOW", isAC = true)
        val fastRegular = createTrain("FAST", isAC = false)
        val fastAC = createTrain("FAST", isAC = true)
        val semiFast = createTrain("SEMI_FAST", isAC = false)

        assertTrue(TrainFilter.SLOW.matches(slowRegular))
        assertTrue(TrainFilter.SLOW.matches(slowAC))
        assertFalse(TrainFilter.SLOW.matches(fastRegular))
        assertFalse(TrainFilter.SLOW.matches(fastAC))
        assertFalse(TrainFilter.SLOW.matches(semiFast))
    }

    @Test
    fun testFastFilter() {
        val slowRegular = createTrain("SLOW", isAC = false)
        val slowAC = createTrain("SLOW", isAC = true)
        val fastRegular = createTrain("FAST", isAC = false)
        val fastAC = createTrain("FAST", isAC = true)
        val semiFast = createTrain("SEMI_FAST", isAC = false)
        val express = createTrain("EXPRESS", isAC = false)

        assertFalse(TrainFilter.FAST.matches(slowRegular))
        assertFalse(TrainFilter.FAST.matches(slowAC))
        assertTrue(TrainFilter.FAST.matches(fastRegular))
        assertTrue(TrainFilter.FAST.matches(fastAC))
        assertTrue(TrainFilter.FAST.matches(semiFast))
        assertTrue(TrainFilter.FAST.matches(express))
    }

    @Test
    fun testACFilter() {
        val slowRegular = createTrain("SLOW", isAC = false)
        val slowAC = createTrain("SLOW", isAC = true)
        val fastRegular = createTrain("FAST", isAC = false)
        val fastAC = createTrain("FAST", isAC = true)

        assertFalse(TrainFilter.AC.matches(slowRegular))
        assertTrue(TrainFilter.AC.matches(slowAC))
        assertFalse(TrainFilter.AC.matches(fastRegular))
        assertTrue(TrainFilter.AC.matches(fastAC))
    }
}
