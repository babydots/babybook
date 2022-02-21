package com.serwylo.babybook.contentwarning

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.serwylo.babybook.databinding.ActivityContentWarningBinding

class ContentWarningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContentWarningBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityContentWarningBinding.inflate(layoutInflater)

        binding.iUnderstand.setOnClickListener {
            finish()
        }

        setContentView(binding.root)
    }

}
