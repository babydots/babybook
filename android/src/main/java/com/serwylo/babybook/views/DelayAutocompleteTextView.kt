package com.serwylo.babybook.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.serwylo.babybook.R
import kotlinx.coroutines.*

/**
 * Adapted from http://makovkastar.github.io/blog/2014/04/12/android-autocompletetextview-with-suggestions-from-a-web-service/
 * and switched from handler to coroutine based implementation.
 *
 * Doesn't use [JvmOverloads] due to this issue: https://medium.com/@mmlodawski/https-medium-com-mmlodawski-do-not-always-trust-jvmoverloads-5251f1ad2cfe
 */
class DelayAutoCompleteTextView: MaterialAutoCompleteTextView {

    constructor(context: Context) : super(context) {
        autoCompleteDelay = DEFAULT_AUTOCOMPLETE_DELAY
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        autoCompleteDelay = fetchDelayAttr(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        autoCompleteDelay = fetchDelayAttr(context, attrs)
    }

    companion object {
        private const val DEFAULT_AUTOCOMPLETE_DELAY = 750

        private fun fetchDelayAttr(context: Context, attrs: AttributeSet?): Int {
            context.theme.obtainStyledAttributes(attrs, R.styleable.DelayAutoCompleteTextView, 0, 0).apply {
                try {
                    return getInteger(R.styleable.DelayAutoCompleteTextView_delay, DEFAULT_AUTOCOMPLETE_DELAY)
                } finally {
                    recycle()
                }
            }
        }
    }

    private val autoCompleteDelay: Int

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var delayedFilterJob: Job? = null

    override fun performFiltering(text: CharSequence?, keyCode: Int) {
        delayedFilterJob?.cancel()
        delayedFilterJob = scope.launch {
            delay(autoCompleteDelay.toLong())
            super.performFiltering(text, keyCode)
        }
    }

}