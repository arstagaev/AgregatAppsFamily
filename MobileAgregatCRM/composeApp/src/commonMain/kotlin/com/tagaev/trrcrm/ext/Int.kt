package com.tagaev.trrcrm.ext

fun String.toIntSafe(): Int? =
    replace(" ", "", true)      // remove spaces
        .split(",")[0]          // take part before comma
        .toIntOrNull()