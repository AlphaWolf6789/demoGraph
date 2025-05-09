package com.example.finalcuafinal // << QUAN TRỌNG: Đảm bảo đây là package đúng của bạn

/**
 * Escapes special HTML characters in a string.
 *
 * Converts characters like '&', '<', '>', '"', and '''
 * into their corresponding HTML entities to prevent them from
 * being interpreted as HTML markup.
 *
 * @return The string with HTML characters escaped.
 */
fun String.escapeHtml(): String {
    return this
        .replace("&", "&")
        .replace("<", "<")
        .replace(">", ">")
            .replace("'", "'")
}


fun String.unescapeHtml(): String {
    return this
        .replace("<", "<")
        .replace(">", ">")
            .replace("'", "'")
            .replace("&", "&")
}