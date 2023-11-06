package com.example.logsstoragedemo.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.logsstoragedemo.databinding.FilePreviewerBinding
import com.example.logsstoragedemo.utils.FileLogger
import java.io.BufferedReader
import java.io.InputStreamReader

class FilePreviewer : ConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)


    private lateinit var binding: FilePreviewerBinding
    private var view: View? = null
    private val layoutInflater = LayoutInflater.from(context)
    private var stringBuilderLog: StringBuilder = FileLogger.saveLog()


    internal fun initFilePreviewer(view: View) {
        view.let {
            this.view = it
            binding = FilePreviewerBinding.inflate(layoutInflater, it as ViewGroup, false)
            it.addView(binding.root)

            binding.constraintLayoutFilePreviewerProgressBar.visibility = View.GONE

            binding.textViewFilePreviewerLog.text = stringBuilderLog.toString()
        }
    }

    internal fun clearView() {
        (view as ViewGroup).removeView(binding.root)
    }


    override fun onDetachedFromWindow() {
        clearView()
        super.onDetachedFromWindow()
    }

}