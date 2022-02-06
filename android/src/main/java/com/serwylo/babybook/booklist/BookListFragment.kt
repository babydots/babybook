package com.serwylo.babybook.booklist

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.serwylo.babybook.bookviewer.BookViewerActivity
import com.serwylo.babybook.databinding.FragmentBookListBinding
import com.serwylo.babybook.editbook.EditBookActivity

class BookListFragment : Fragment() {

    private var columnCount = 2

    private val viewModel: BookListViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT, 2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBookListBinding.inflate(inflater, container, false)

        viewModel.isInEditMode.observe(this) { isInEditMode ->
            binding.editMode.visibility = if (isInEditMode) View.VISIBLE else View.GONE
            binding.addBookButton.visibility = if (isInEditMode) View.VISIBLE else View.GONE
        }

        binding.addBookButton.setOnClickListener {
            onAddBook()
        }

        binding.list.layoutManager = when {
            columnCount <= 1 -> LinearLayoutManager(context)
            else -> GridLayoutManager(context, columnCount)
        }

        binding.list.adapter = BookListAdapter(context!!).also { adapter ->

            adapter.setBookSelectedListener { book ->

                if (viewModel.isInEditMode.value == true) {
                    startActivity(Intent(context, EditBookActivity::class.java).apply {
                        putExtra(EditBookActivity.EXTRA_BOOK_ID, book.id)
                    })
                } else {
                    startActivity(Intent(context, BookViewerActivity::class.java).apply {
                        putExtra(BookViewerActivity.EXTRA_BOOK_ID, book.id)
                    })
                }

            }

            viewModel.allBooks.observe(viewLifecycleOwner) { books ->
                adapter.setData(books)
                adapter.notifyDataSetChanged()
            }

        }

        return binding.root
    }

    private fun onAddBook() {
        startActivity(Intent(context, EditBookActivity::class.java))
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