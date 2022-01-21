package com.serwylo.babybook.booklist

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.serwylo.babybook.databinding.FragmentBookListBinding
import com.serwylo.babybook.db.entities.Book

class BookListAdapter: RecyclerView.Adapter<BookListAdapter.ViewHolder>() {

    private var values: List<Book> = listOf()

    private var bookSelectedListener: ((book: Book) -> Unit)? = null

    fun setBookSelectedListener(listener: (book: Book) -> Unit) {
        this.bookSelectedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentBookListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = values[position]
        holder.titleView.text = book.title
        holder.root.setOnClickListener { bookSelectedListener?.invoke(book) }
    }

    override fun getItemCount(): Int = values.size

    fun setData(books: List<Book>?) {
        values = books ?: listOf()
    }

    inner class ViewHolder(binding: FragmentBookListBinding) : RecyclerView.ViewHolder(binding.root) {
        val root: View = binding.root
        val titleView: TextView = binding.bookTitle
    }

}