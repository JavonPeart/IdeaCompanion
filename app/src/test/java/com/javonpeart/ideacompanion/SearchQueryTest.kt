package com.javonpeart.ideacompanion

import com.javonpeart.ideacompanion.search.SearchQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryTest {
    @Test fun punctuationCannotInjectFtsOperators() {
        assertEquals("\"hello\"* AND \"world\"*", SearchQuery.fromInput("hello: world"))
    }
    @Test fun blankQueryIsEmpty() { assertEquals("", SearchQuery.fromInput("?!")) }
}
