package com.avijitsamanta.memorygame

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avijitsamanta.memorygame.models.BoardSize
import com.avijitsamanta.memorygame.utils.MemoryGame
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), MemoryBoardAdapter.CardClickListener {

    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var clRoot: ConstraintLayout

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    private var boardSize: BoardSize = BoardSize.EASY

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        clRoot = findViewById(R.id.clRoot)

        memoryGame = MemoryGame(boardSize)
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, this)
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
    }

    override fun onCardClicked(position: Int) {
        updateGameWithFlip(position)
    }

    @SuppressLint("SetTextI18n")
    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()) {
            Snackbar.make(clRoot, "You already won!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (memoryGame.flipCard(position)) {
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full),
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot, "You won! Congratulation.", Snackbar.LENGTH_LONG).show()
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}