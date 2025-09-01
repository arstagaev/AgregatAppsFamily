package org.agregatcrm.ext

//import java.net.URLDecoder
//import java.nio.charset.StandardCharsets

//fun String.urlDecode(): String =
//    URLDecoder.decode(this, StandardCharsets.UTF_8.name())

// for ios
fun String.urlDecodeUtf8(): String {
    val out = StringBuilder()
    var i = 0
    while (i < this.length) {
        val c = this[i]
        if (c == '%' && i + 2 < this.length) {
            val hex = this.substring(i + 1, i + 3)
            val code = hex.toInt(16)
            out.append(code.toChar())
            i += 3
        } else if (c == '+') {
            out.append(' ')
            i++
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}
