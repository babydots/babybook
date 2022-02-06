package com.serwylo.babybook.editbook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.serwylo.babybook.databinding.EditBookPageItemBinding
import com.serwylo.babybook.db.entities.PageEditingData
import com.squareup.picasso.Picasso

class EditBookPagesAdapter: RecyclerView.Adapter<EditBookPagesAdapter.ViewHolder>() {

    private var values: List<PageEditingData> = listOf()

    private var pageSelectedListener: ((page: PageEditingData) -> Unit)? = null

    fun setPageSelectedListener(listener: (page: PageEditingData) -> Unit) {
        this.pageSelectedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(EditBookPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = values[position]
        holder.titleView.text = page.title()
        if (page.image == null) {
            holder.imageView.visibility = View.GONE
        } else {
            holder.imageView.visibility = View.VISIBLE
            Picasso.get()
                .load(page.image.filename)
                .fit()
                .centerCrop()
                .into(holder.imageView)
        }
        holder.root.setOnClickListener { pageSelectedListener?.invoke(page) }
    }

    override fun getItemCount(): Int = values.size

    fun setData(pages: List<PageEditingData>?) {
        values = pages ?: listOf()
    }

    inner class ViewHolder(binding: EditBookPageItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val root: View = binding.root
        val titleView: TextView = binding.title
        val imageView: AppCompatImageView = binding.image
    }

}
