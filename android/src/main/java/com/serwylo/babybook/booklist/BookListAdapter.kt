package com.serwylo.babybook.booklist

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.serwylo.babybook.databinding.FragmentBookListItemBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.BookWithCoverPage
import com.squareup.picasso.Picasso

class BookListAdapter(context: Context): RecyclerView.Adapter<BookListAdapter.ViewHolder>() {

    private var values: List<BookWithCoverPage> = listOf()
    private var bookSelectedListener: ((book: Book) -> Unit)? = null

    fun setBookSelectedListener(listener: (book: Book) -> Unit) {
        this.bookSelectedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentBookListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val b = values[position]
        holder.titleView.text = b.book.title
        holder.root.setOnClickListener { bookSelectedListener?.invoke(b.book) }
        if (b.coverPageImagePath == null) {
            holder.imageView.visibility = View.GONE
        } else {
            holder.imageView.visibility = View.VISIBLE
            Picasso.get()
                .load(b.coverPageImagePath)
                .fit()
                .centerCrop()
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int = values.size

    fun setData(books: List<BookWithCoverPage>?) {
        values = books ?: listOf()
    }

    inner class ViewHolder(binding: FragmentBookListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val root = binding.root
        val titleView = binding.title
        val imageView  = binding.image
    }

}