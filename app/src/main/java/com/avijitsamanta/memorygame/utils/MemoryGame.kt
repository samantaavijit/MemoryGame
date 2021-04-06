package com.avijitsamanta.memorygame.utils

import com.avijitsamanta.memorygame.models.BoardSize
import com.avijitsamanta.memorygame.models.MemoryCard


class MemoryGame(private val boardSize: BoardSize) {
    val cards: List<MemoryCard>
    val numPairsFound = 0

    init {
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()
        cards = randomizedImages.map { MemoryCard(it) }
    }
}