package com.jetbrains.greetingkmp

import kotlin.test.Test
import kotlin.test.assertTrue

class SharedLogicCommonTest {
    @Test
    fun sayHello_includesName() {
        assertTrue(sayHello("KMP").contains("KMP"))
    }
}
