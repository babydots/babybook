package com.serwylo.babybook.attribution

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serwylo.babybook.bookviewer.BookViewerActivity
import com.serwylo.babybook.databinding.ActivityAttributionBinding
import com.serwylo.babybook.databinding.ImageAttributionItemBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.PageEditingData
import com.serwylo.babybook.db.repositories.BookRepository
import com.squareup.picasso.Picasso

class AttributionActivity: AppCompatActivity() {

    private lateinit var viewModel: AttributionViewModel
    private lateinit var binding: ActivityAttributionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAttributionBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val dao = AppDatabase.getInstance(this).bookDao()
        val bookId = intent.extras?.getLong(BookViewerActivity.EXTRA_BOOK_ID) ?: error("Can't view book attribution, expected to find the ID of the book in the Intent $EXTRA_BOOK_ID but not found.")

        viewModel = ViewModelProvider(this, AttributionViewModelFactory(BookRepository(dao), bookId)).get(AttributionViewModel::class.java)

        binding.pages.adapter = Adapter().also { adapter ->
            viewModel.pages.observe(this) { pages ->
                adapter.setData(pages)
            }
        }

        binding.pages.layoutManager = LinearLayoutManager(this)

        viewModel.book.observe(this) { book ->
            supportActionBar?.title = book.title
        }

        binding.textAttribution.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://simple.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License")))
        }
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        private var pages: List<PageEditingData> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ImageAttributionItemBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = pages[position]

            // We know that page.image is not null, because we filtered as such when calling setData().
            holder.binding.root.visibility = View.VISIBLE
            holder.binding.textImageName.text = page.image!!.title
            holder.binding.textAuthorName.text = page.image.author
            holder.binding.textLicense.text = page.image.license

            holder.binding.root.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://commons.wikimedia.org/wiki/File:${page.image.name}")))
            }

            holder.binding.image.visibility = View.VISIBLE
            Picasso.get()
                .load(page.image.filename)
                .fit()
                .centerCrop()
                .into(holder.binding.image)

        }

        override fun getItemCount(): Int {
            return pages.size
        }

        fun setData(value: List<PageEditingData>) {
            pages = value.filter { it.image != null }
            notifyDataSetChanged()
        }

    }

    private class ViewHolder(val binding: ImageAttributionItemBinding): RecyclerView.ViewHolder(binding.root)

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
    }

}