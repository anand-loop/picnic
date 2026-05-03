// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.post

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.anandj.picnic.data.db.entity.MediaEntity
import java.io.File

fun shareMedia(context: Context, media: MediaEntity) {
    shareFile(
        context = context,
        path = media.uri,
        mimeType = if (media.mediaType == "video") "video/*" else "image/*"
    )
}

fun shareFile(context: Context, path: String, mimeType: String) {
    val file = File(path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, null))
}
