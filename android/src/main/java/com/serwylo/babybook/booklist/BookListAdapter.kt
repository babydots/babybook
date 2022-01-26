package com.serwylo.babybook.booklist

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.serwylo.babybook.databinding.FragmentBookListBinding
import com.serwylo.babybook.databinding.FragmentBookListItemBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.Book
import com.squareup.picasso.Picasso

class BookListAdapter(context: Context): RecyclerView.Adapter<BookListAdapter.ViewHolder>() {

    private var values: List<Book> = listOf()
    private var bookSelectedListener: ((book: Book) -> Unit)? = null
    private var bookLongPressedListener: ((book: Book) -> Unit)? = null
    private var handler: Handler = Handler(context.mainLooper)
    private var dao = AppDatabase.getInstance(context).bookDao()

    fun setBookSelectedListener(listener: (book: Book) -> Unit) {
        this.bookSelectedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentBookListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = values[position]
        holder.titleView.text = book.title
        holder.root.setOnClickListener { bookSelectedListener?.invoke(book) }

        holder.root.isLongClickable = true
        holder.root.setOnLongClickListener {
            bookLongPressedListener?.invoke(book)
            true
        }

        AppDatabase.executor.execute {
            val coverImage = dao.getBookCoverImage(book.id)

            handler.post {
                if (coverImage == null) {
                    holder.imageView.visibility = View.GONE
                } else {
                    holder.imageView.visibility = View.VISIBLE
                    Picasso.get()
                        .load(coverImage)
                        .fit()
                        .centerCrop()
                        .into(holder.imageView)
                }
            }
        }
    }

    override fun getItemCount(): Int = values.size

    fun setData(books: List<Book>?) {
        values = books ?: listOf()
    }

    inner class ViewHolder(binding: FragmentBookListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val root = binding.root
        val titleView = binding.title
        val imageView  = binding.image
    }

}