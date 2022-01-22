package com.serwylo.babybook.booklist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serwylo.babybook.editbook.EditBookActivity
import com.serwylo.babybook.R
import com.serwylo.babybook.db.AppDatabase

class BookListFragment : Fragment() {

    private var columnCount = 2

    private lateinit var viewModel: BookListViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(BookListViewModel::class.java)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT, 2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_list_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }

                adapter = BookListAdapter(context).also { adapter ->

                    adapter.setBookSelectedListener { book ->
                        startActivity(Intent(context, EditBookActivity::class.java).apply {
                            putExtra(EditBookActivity.EXTRA_BOOK_ID, book.id)
                        })
                    }

                    viewModel.allBooks.observe(viewLifecycleOwner) { books ->
                        adapter.setData(books)
                        adapter.notifyDataSetChanged()
                    }

                }
            }
        }
        return view
    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int) =
            BookListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}