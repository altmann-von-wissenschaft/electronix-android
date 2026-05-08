package com.pnzgu.electronix.util

fun contentImageUrl(contentBaseUrl: String, objectPath: String?): String? {
    if (objectPath.isNullOrBlank()) return null
    val base = contentBaseUrl.trimEnd('/')
    val path = objectPath.trimStart('/')
    return "$base/$path"
}
