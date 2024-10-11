/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.videoeditor.utils

import android.app.Activity
import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import com.obs.videoeditor.R
import com.obs.videoeditor.databinding.CustomLoadingDialogBinding

class CustomLoadingDialog {

    private var dialog: AlertDialog? = null

    fun show(
        activity: Activity,
        msg: String? = activity.getString(R.string.loading),
        isCancellable: Boolean = false
    ): Dialog? {
        if (dialog != null) {   // do not create new dialog instance everytime
            dialog?.show()
            return dialog
        }

        val viewBinding = CustomLoadingDialogBinding.inflate(activity.layoutInflater)
        viewBinding.tvMsg.text = msg


        val builder = AlertDialog.Builder(activity)
        builder.setView(viewBinding.root)
        builder.setCancelable(isCancellable)
        dialog = builder.create()
        dialog?.show()
        return dialog
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

}