package com.serwylo.babybook.editbookpage

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.serwylo.babybook.databinding.DialogPageImageSelectorBinding
import com.serwylo.babybook.databinding.DialogPageImageSelectorItemBinding
import com.serwylo.babybook.databinding.EditBookPageItemBinding
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.entities.WikiImage
import com.squareup.picasso.Picasso
import java.io.File

class SelectImageAdapter(private val values: List<WikiImage>): RecyclerView.Adapter<SelectImageAdapter.ViewHolder>() {

    private var imageSelectedListener: ((image: WikiImage) -> Unit)? = null

    fun setImageSelectedListener(listener: (image: WikiImage) -> Unit) {
        this.imageSelectedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DialogPageImageSelectorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = values[position]

        Log.d(TAG, "onBindViewHolder: Loading image ${image.filename}")
        Picasso.get()
            .load(image.filename)
            .fit()
            .centerCrop()
            .into(holder.image)

        holder.image.setOnClickListener {
            imageSelectedListener?.invoke(image)
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: DialogPageImageSelectorItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val image: AppCompatImageView = binding.image
    }

    companion object {
        const val TAG = "SelectImageAdapter"
    }

}
