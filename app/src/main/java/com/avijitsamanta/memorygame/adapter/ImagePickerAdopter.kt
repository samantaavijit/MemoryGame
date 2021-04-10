package com.avijitsamanta.memorygame.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.avijitsamanta.memorygame.R
import com.avijitsamanta.memorygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdopter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize
) : RecyclerView.Adapter<ImagePickerAdopter.ImagePickerHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePickerHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardWidth, cardHeight)
        val layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.height = cardSideLength
        layoutParams.width = cardSideLength
        return ImagePickerHolder(view)
    }

    override fun onBindViewHolder(holder: ImagePickerHolder, position: Int) {
        if (position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    override fun getItemCount() = boardSize.getNumPairs()

    inner class ImagePickerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind() {
            ivCustomImage.setOnClickListener {
                // Launch intent for user to select photo

            }
        }

    }
}
