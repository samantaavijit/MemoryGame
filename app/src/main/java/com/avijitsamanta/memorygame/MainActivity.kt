package com.avijitsamanta.memorygame

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avijitsamanta.memorygame.adapter.MemoryBoardAdapter
import com.avijitsamanta.memorygame.models.BoardSize
import com.avijitsamanta.memorygame.models.UserImageList
import com.avijitsamanta.memorygame.utils.COLLECTION_PATH
import com.avijitsamanta.memorygame.utils.EXTRA_BOARD_SIZE
import com.avijitsamanta.memorygame.utils.EXTRA_GAME_NAME
import com.avijitsamanta.memorygame.utils.MemoryGame
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), MemoryBoardAdapter.CardClickListener {

    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var clRoot: ConstraintLayout

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private val db = Firebase.firestore
    private var gameName: String? = null
    private var boardSize: BoardSize = BoardSize.EASY
    private var customGameImages: List<String>? = null

    companion object {
        const val CREATE_REQUEST_CODE = 248
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        clRoot = findViewById(R.id.clRoot)

        setupBoard()
    }

    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = getString(R.string.easy)
                tvNumPairs.text = getString(R.string.e_pair)
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = getString(R.string.medium)
                tvNumPairs.text = getString(R.string.m_pair)
            }
            BoardSize.HARD -> {
                tvNumMoves.text = getString(R.string.hard)
                tvNumPairs.text = getString(R.string.h_pair)
            }
        }
        memoryGame = MemoryGame(boardSize, customGameImages)
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, this)
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
    }

    override fun onCardClicked(position: Int) {
        updateGameWithFlip(position)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ui_refresh -> {
                // setup the game again
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your current game?", null) {
                        setupBoard()
                    }
                } else {
                    setupBoard()
                }
                return true
            }
            R.id.ui_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.ui_custom -> {
                showCreationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /***
     * it is custom
     * game create dialog
     */
    @SuppressLint("InflateParams")
    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        showAlertDialog("Create your own memory board", boardSizeView) {
            // set a new value for the board size
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            // navigate to a new screen
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        }
    }

    /**
     * show new game dialog
     */
    @SuppressLint("InflateParams")
    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        // show previous selected item
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }

        showAlertDialog("Choose new size", boardSizeView) {
            // set a new value for the board size
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()
        }
    }

    /**
     * It show
     * custom alert dialog
     */
    private fun showAlertDialog(
        title: String,
        view: View?,
        positiveClickListener: View.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    /**
     * update the data set
     * and check win or not
     */
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST_CODE && resultCode == RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /***
     * Download the
     * all image with
     * specific game name
     */
    private fun downloadGame(customGameName: String) {
        db.collection(COLLECTION_PATH).document(customGameName).get()
            .addOnSuccessListener { document ->
                val userImageList = document.toObject(UserImageList::class.java)
                if (userImageList?.images == null) {
                    Log.e(TAG, "Invalid custom game data from Firestore")
                    Snackbar.make(
                        clRoot,
                        "Sorry, we couldn't find any such game, '$customGameName'",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }
                val numCards = userImageList.images.size * 2
                boardSize = BoardSize.getByValue(numCards)
                customGameImages = userImageList.images
                setupBoard()
                gameName = customGameName
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Exception with retrieving game", exception)
            }
    }
}