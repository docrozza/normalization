package io.docrozza.normalization.canonize

import com.stardog.stark.Values
import com.stardog.stark.vocabs.XSD
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test as test

class StatementComparatorTest {

    companion object {

        private val iri1 = Values.iri("urn:test:thing1")
        private val iri2 = Values.iri("urn:test:thing2")
        private val iri3 = Values.iri("urn:test:thing3")
        private val iri4 = Values.iri("urn:test:thing4")
        private val iri5 = Values.iri("urn:test:thing5")

        private val bnode1 = Values.bnode("c14n1")
        private val bnode2 = Values.bnode("c14n2")
        private val bnode3 = Values.bnode("c14n3")

        private val lit1 = Values.literal("1", XSD.INTEGER)
        private val lit2 = Values.literal("2", XSD.INTEGER)
        private val lit3 = Values.literal("c", iri1)
        private val lit4 = Values.literal("c", iri2)
        private val lit5 = Values.literal("d", iri2)
        private val lit6 = Values.literal("11", XSD.INTEGER)
    }

    @test fun bnode() {
        val sorter = StatementComparator(false)

        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, bnode1),
            Values.statement(iri1, iri2, bnode2)))
        assertEquals(0, sorter.compare(
            Values.statement(iri1, iri2, bnode1),
            Values.statement(iri1, iri2, bnode1)))
        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, bnode2),
            Values.statement(iri1, iri2, bnode1)))
    }

    @test fun iri() {
        val sorter = StatementComparator(false)

        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, iri3),
            Values.statement(iri1, iri2, iri4)))
        assertEquals(0, sorter.compare(
            Values.statement(iri1, iri2, iri3),
            Values.statement(iri1, iri2, iri3)))
        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, iri4),
            Values.statement(iri1, iri2, iri3)))
    }

    @test fun bnodeAfterIRI() {
        val sorter = StatementComparator(false)

        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, bnode1),
            Values.statement(iri1, iri2, iri2)))
        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, iri2),
            Values.statement(iri1, iri2, bnode2)))
    }

    @test fun literalsBeforeResource() {
        val sorter = StatementComparator(false)

        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, lit1),
            Values.statement(iri1, iri2, iri3)))
        assertEquals(0, sorter.compare(
            Values.statement(iri1, iri2, lit1),
            Values.statement(iri1, iri2, lit1)))
        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, iri3),
            Values.statement(iri1, iri2, lit1)))
    }

    @test fun nonXSDliterals() {
        val sorter = StatementComparator(false)

        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, lit1),
            Values.statement(iri1, iri2, lit3)))
        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, lit3),
            Values.statement(iri1, iri2, lit2)))

        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, lit3),
            Values.statement(iri1, iri2, lit4)))

        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, lit5),
            Values.statement(iri1, iri2, lit4)))
    }

    @test fun literalNaturalOrdering() {
        // stardog's LiteralComparator.SPEC should cover this but quick check here for completeness
        val sorter = StatementComparator(false)

        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, lit1),
            Values.statement(iri1, iri2, lit2)))

        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, lit6),
            Values.statement(iri1, iri2, lit2)))
    }

    @test fun statementsNaturalOrdering() {
        // by subject  then by object then by context
        val sorter = StatementComparator(true)

        // by subject
        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, iri3),
            Values.statement(iri2, iri2, iri3)))
        assertEquals(0, sorter.compare(
            Values.statement(iri1, iri2, iri3),
            Values.statement(iri1, iri2, iri3)))
        assertEquals(1, sorter.compare(
            Values.statement(iri2, iri2, iri3),
            Values.statement(iri1, iri2, iri3)))

        // then by predicate
        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, iri4),
            Values.statement(iri1, iri3, iri4)))

        // then by object
        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, iri3),
            Values.statement(iri1, iri2, iri4)))

        // then by context
        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, iri3, iri4),
            Values.statement(iri1, iri2, iri3, iri5)))
        assertEquals(-1, sorter.compare(
            Values.statement(iri1, iri2, iri3, bnode1),
            Values.statement(iri1, iri2, iri3, bnode2)))
        assertEquals(1, sorter.compare(
            Values.statement(iri1, iri2, iri3, bnode3),
            Values.statement(iri1, iri2, iri3, bnode2)))
    }
}