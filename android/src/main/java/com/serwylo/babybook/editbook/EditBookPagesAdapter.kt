package com.serwylo.babybook.editbook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.serwylo.babybook.databinding.EditBookPageItemBinding
import com.serwylo.babybook.db.entities.BookPage

class EditBookPagesAdapter: RecyclerView.Adapter<EditBookPagesAdapter.ViewHolder>() {

    private var values: List<BookPage> = listOf()

    private var pageSelectedListener: ((page: BookPage) -> Unit)? = null

    fun setPageSelectedListener(listener: (page: BookPage) -> Unit) {
        this.pageSelectedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(EditBookPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = values[position]
        holder.titleView.text = page.title
        holder.root.setOnClickListener { pageSelectedListener?.invoke(page) }
    }

    override fun getItemCount(): Int = values.size

    fun setData(pages: List<BookPage>?) {
        values = pages ?: listOf()
    }

    inner class ViewHolder(binding: EditBookPageItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val root: View = binding.root
        val titleView: TextView = binding.bookPageTitle
    }

}
