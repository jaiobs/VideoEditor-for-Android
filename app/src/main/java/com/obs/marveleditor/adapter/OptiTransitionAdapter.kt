/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.obs.marveleditor.R
import com.obs.marveleditor.interfaces.OptiFilterListener

class OptiTransitionAdapter (transitionList: ArrayList<String>, val context: Context, optiFilterListener: OptiFilterListener) :
    RecyclerView.Adapter<OptiTransitionAdapter.MyPostViewHolder>() {

    private var tagName: String = OptiTransitionAdapter::class.java.simpleName
    private var myTransitionList = transitionList
    private var myFilterListener = optiFilterListener
    private var selectedPosition: Int = -1
    private var selectedTransition: String? = null

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyPostViewHolder {
        return MyPostViewHolder(LayoutInflater.from(context).inflate(R.layout.opti_playback_view, p0, false))
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        holder.tvSpeed.text = myTransitionList[position]

        if (selectedPosition == position) {
            holder.tvSpeed.setBackgroundColor(Color.WHITE)
            holder.tvSpeed.setTextColor(Color.BLACK)
        } else {
            holder.tvSpeed.setBackgroundColor(Color.BLACK)
            holder.tvSpeed.setTextColor(Color.WHITE)
        }

        holder.tvSpeed.setOnClickListener {
            //selected transition will be saved here
            selectedPosition = holder.adapterPosition
            selectedTransition = myTransitionList[holder.adapterPosition]
            notifyDataSetChanged()
        }
    }

    fun setTransition() {
        if (selectedTransition != null) {
            Log.v(tagName, "selectedTransition: $selectedTransition")
            myFilterListener.selectedFilter(selectedTransition!!)
        }
    }

    class MyPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvSpeed: TextView = itemView.findViewById(R.id.tv_speed)
    }

    override fun getItemCount(): Int {
        return myTransitionList.size
    }
}