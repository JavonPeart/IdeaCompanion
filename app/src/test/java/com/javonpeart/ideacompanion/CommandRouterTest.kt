package com.javonpeart.ideacompanion

import com.javonpeart.ideacompanion.conversation.*
import org.junit.Assert.*
import org.junit.Test

class CommandRouterTest {
    @Test fun requiresPhraseAtStart() {
        assertNull(CommandRouter.parse("Please, Hello Companion, show my recent ideas", "Hello Companion"))
        assertNull(CommandRouter.parse("show my recent ideas", "Hello Companion"))
    }
    @Test fun handlesPunctuationAndCase() {
        assertEquals(Command.Recent, CommandRouter.parse("HELLO companion, show my recent ideas!", "Hello Companion"))
        assertEquals(Command.Find("gardening"), CommandRouter.parse("Hello Companion, find ideas about gardening", "Hello Companion"))
    }
    @Test fun unsupportedRequestIsExplicit() {
        assertEquals(Command.Unsupported, CommandRouter.parse("Hello Companion, book a flight", "Hello Companion"))
    }
}
