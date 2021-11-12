package io.docrozza.normalization.canonize

import com.stardog.stark.BNode
import com.stardog.stark.Values
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test as test

class PermutationsTest {

    @test fun emptyInitialList() {
        val items = emptyList<BNode>()
        val iterator = Permutations(items)
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertEquals(items, iterator.next())
        assertFalse(iterator.hasNext())

        assertFails { iterator.next() }
    }

    @test fun singleItem() {
        val items = listOf(Values.bnode("n1"))
        val iterator = Permutations(items)
        assertTrue(iterator.hasNext())
        assertEquals(items, iterator.next())
        assertFalse(iterator.hasNext())
    }

    @test fun multipleItems() {
        val b1 = Values.bnode("n1")
        val b2 = Values.bnode("n2")
        val b3 = Values.bnode("n3")
        val iterator = Permutations(listOf(b1, b2, b3))

        assertTrue(iterator.hasNext())
        assertEquals(listOf(b1, b2, b3), iterator.next())
        assertEquals(listOf(b1, b3, b2), iterator.next())
        assertEquals(listOf(b3, b1, b2), iterator.next())
        assertEquals(listOf(b3, b2, b1), iterator.next())
        assertEquals(listOf(b2, b3, b1), iterator.next())
        assertEquals(listOf(b2, b1, b3), iterator.next())
        assertFalse(iterator.hasNext())
    }
}