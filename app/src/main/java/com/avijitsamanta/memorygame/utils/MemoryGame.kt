package com.avijitsamanta.memorygame.utils

import com.avijitsamanta.memorygame.models.BoardSize
import com.avijitsamanta.memorygame.models.MemoryCard


class MemoryGame(private val boardSize: BoardSize, customImages: List<String>?) {

    val cards: List<MemoryCard>
    var numPairsFound = 0

    private var indexOfSingleSelectedCard: Int? = null
    private var numCardFlips = 0

    init {
        if (customImages == null) {
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it) }
        } else {
            val randomizedImages = (customImages + customImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it.hashCode(), it) }
        }
    }

    fun flipCard(position: Int): Boolean {
        numCardFlips++
        val card = cards[position]
        // Three cases:
        /*
        0 cards previously flipped over => restore cards + flip over the selected card
        1 cards previously flipped over => flip over the selected card + check if the images match
        2 cards previously flipped over => restore cards + flip over the selected card
         */
        var foundMatch = false
        if (indexOfSingleSelectedCard == null) {
            // 0 or 2 cards previously flipped over
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            // exactly 1 card previously flipped over
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) {
            return false
        }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame() = numPairsFound == boardSize.getNumPairs()

    fun isCardFaceUp(position: Int) = cards[position].isFaceUp

    fun getNumMoves() = numCardFlips / 2

}