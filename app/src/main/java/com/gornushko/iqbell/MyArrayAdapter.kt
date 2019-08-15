package com.gornushko.iqbell

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat

class MyArrayAdapter(context: Context, layoutResource: Int, array: Array<String>):
    ArrayAdapter<String>(context, layoutResource, array) {
    var selectedItem: Int? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.setBackgroundColor(
            if (position == selectedItem) ContextCompat.getColor(context, R.color.selected_item)
            else Color.TRANSPARENT
        )
        return view
    }
}