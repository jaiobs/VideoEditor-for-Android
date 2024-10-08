/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.adapter

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.obs.marveleditor.R
import com.obs.marveleditor.interfaces.OptiClipArtListener
import java.io.File

class OptiClipArtAdapter(clipArtList: ArrayList<String>, val context: Context, optiClipArtListener: OptiClipArtListener) :
    RecyclerView.Adapter<OptiClipArtAdapter.MyPostViewHolder>() {

    private var tagName: String = OptiClipArtAdapter::class.java.simpleName
    private var myClipArtList = clipArtList
    private var myClipArtListener = optiClipArtListener
    private var selectedPosition: Int = -1
    private var selectedFilePath: String? = null

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyPostViewHolder {
        return MyPostViewHolder(LayoutInflater.from(context).inflate(R.layout.opti_clipart_view, p0, false))
    }

    override fun getItemCount(): Int {
        return myClipArtList.size
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {

        val uri = Uri.fromFile(File(myClipArtList[position]))
        holder.tvClipArt.setImageURI(uri)

        if (selectedPosition == position) {
            holder.tvClipArt.setBackgroundColor(Color.WHITE)
        } else {
            holder.tvClipArt.setBackgroundColor(Color.BLACK)
        }

        holder.tvClipArt.setOnClickListener {
            //selected clip art will be saved here
            selectedPosition = holder.adapterPosition
            selectedFilePath = myClipArtList[holder.adapterPosition]
            notifyDataSetChanged()
        }
    }

    fun setClipArt() {
        if (selectedFilePath != null) {
            Log.v(tagName, "selectedFilePath: $selectedFilePath")
            myClipArtListener.selectedClipArt(selectedFilePath!!)
        }
    }

    class MyPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvClipArt: ImageView = itemView.findViewById(R.id.tv_clip_art)
    }
}