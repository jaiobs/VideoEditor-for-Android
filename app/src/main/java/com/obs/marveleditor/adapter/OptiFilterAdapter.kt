/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.obs.marveleditor.R
import com.obs.marveleditor.interfaces.OptiFilterListener

class OptiFilterAdapter(filterList: ArrayList<String>, bitmap: Bitmap, val context: Context, optiFilterListener: OptiFilterListener) :
    RecyclerView.Adapter<OptiFilterAdapter.MyPostViewHolder>() {

    private var tagName: String = OptiFilterAdapter::class.java.simpleName
    private var myFilterList = filterList
    private var myBitmap = bitmap
    private var myFilterListener = optiFilterListener
    private var selectedPosition: Int = -1
    private var selectedFilter: String? = null

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyPostViewHolder {
        return MyPostViewHolder(LayoutInflater.from(context).inflate(R.layout.opti_filter_view, p0, false))
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        holder.tvFilter.text = myFilterList[position]

        if (selectedPosition == position) {
            holder.clFilter.setBackgroundColor(Color.WHITE)
            holder.tvFilter.setTextColor(Color.BLACK)
        } else {
            holder.clFilter.setBackgroundColor(Color.BLACK)
            holder.tvFilter.setTextColor(Color.WHITE)
        }

        holder.ivFilter.setImageBitmap(myBitmap)

        holder.clFilter.setOnClickListener {
            //selected filter will be saved here
            selectedPosition = holder.adapterPosition
            selectedFilter = myFilterList[holder.adapterPosition]
            notifyDataSetChanged()
        }
    }

    fun setFilter() {
        if (selectedFilter != null) {
            Log.v(tagName, "selectedFilter: $selectedFilter")
            myFilterListener.selectedFilter(selectedFilter!!)
        }
    }

    class MyPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvFilter: TextView = itemView.findViewById(R.id.tvFilter)
        var ivFilter: SimpleDraweeView = itemView.findViewById(R.id.ivFilter)
        var clFilter: ConstraintLayout = itemView.findViewById(R.id.clFilter)
    }

    override fun getItemCount(): Int {
        return myFilterList.size
    }
}