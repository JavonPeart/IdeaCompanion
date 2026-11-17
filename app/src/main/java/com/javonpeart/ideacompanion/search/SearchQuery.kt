package com.javonpeart.ideacompanion.search

object SearchQuery {
    // Quote each token so punctuation cannot become FTS syntax.
    fun fromInput(input: String): String = Regex("[\\p{L}\\p{N}]+").findAll(input)
        .map { "\"${it.value}\"*" }.joinToString(" AND ")
}
