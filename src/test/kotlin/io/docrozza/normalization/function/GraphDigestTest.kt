package io.docrozza.normalization.function

import com.stardog.stark.IRI
import com.stardog.stark.Literal
import com.stardog.stark.Statement
import com.stardog.stark.Values
import com.stardog.stark.query.*
import com.stardog.stark.vocabs.XSD
import io.docrozza.normalization.StardogTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test as test

class GraphDigestTest : StardogTest() {

    companion object {
        val iri1 = Values.iri("urn:test:iri1")
        val iri2 = Values.iri("urn:test:iri2")
        val iri3 = Values.iri("urn:test:iri3")
        val iri4 = Values.iri("urn:test:iri4")
        val iri5 = Values.iri("urn:test:iri5")
        val iri6 = Values.iri("urn:test:iri6")
    }

    @test fun digestEmptyGraph() {
        val query = """
            PREFIX gd: <urn:docrozza:stardog:normalization:>
            SELECT ?hash FROM <tag:stardog:api:context:all> WHERE {
                (<urn:this:is:missing>) gd:graphDigest (?hash)
            }
        """.trimIndent()

        query(query) { assertFalse(it.hasNext(), "should return no digest") }
    }

    @test fun digestStaysConstant() {
        createData(
            Values.statement(iri1, iri2, iri3, iri4),
            Values.statement(iri2, iri3, iri1, iri4),
            Values.statement(iri3, iri1, iri2, iri4))

        val query = """
            PREFIX gd: <urn:docrozza:stardog:normalization:>
            SELECT ?hash FROM <tag:stardog:api:context:all> WHERE {
                (<$iri4>) gd:graphDigest (?hash)
            }
        """.trimIndent()

        val hash = "qGZvwUk4k2v3Eu1vqahBjIESVfEq5wPxSYkleCXjXzE="
        val check = { results: SelectQueryResult ->
            assertTrue(results.hasNext())
            assertEquals(hash, results.next().hash("hash"))
            assertFalse(results.hasNext())
        }

        query(query, check)
        query(query, check)
    }

    @test fun digestForMultipleContextsAllDifferentGraphs() {
        createData(
            Values.statement(iri1, iri2, iri3, iri4),
            Values.statement(iri2, iri3, iri1, iri4),
            Values.statement(iri3, iri1, iri2, iri4),

            Values.statement(iri3, iri2, iri1, iri5),
            Values.statement(iri2, iri1, iri3, iri5),
            Values.statement(iri1, iri3, iri2, iri5))

        val query = """
            PREFIX gd: <urn:docrozza:stardog:normalization:>
            SELECT ?context ?hash FROM NAMED <tag:stardog:api:context:all> WHERE {
                GRAPH ?context {}
                (?context) gd:graphDigest (?hash)
            }
        """.trimIndent()

        val hash1 = "qGZvwUk4k2v3Eu1vqahBjIESVfEq5wPxSYkleCXjXzE="
        val hash2 = "q1XNB/1SJs6RqaprS+Q1ShP89X14XIOe5p6c+8qya6w="
        val actual = mutableSetOf<Pair<IRI, String>>()

        query(query) { results ->
            assertTrue(results.hasNext())
            actual.add(results.next().let { Pair(it.context("context"), it.hash("hash")) })
            assertTrue(results.hasNext())
            actual.add(results.next().let { Pair(it.context("context"), it.hash("hash")) })
            assertFalse(results.hasNext())
        }

        assertEquals(setOf(Pair(iri4, hash1), Pair(iri5, hash2)), actual)
    }

    @test fun digestForMultipleContextsWithSomeMatchingGraphs() {
        createData(
            Values.statement(iri1, iri2, iri3, iri4),
            Values.statement(iri2, iri3, iri1, iri4),
            Values.statement(iri3, iri1, iri2, iri4),

            Values.statement(iri3, iri2, iri1, iri5),
            Values.statement(iri2, iri1, iri3, iri5),
            Values.statement(iri1, iri3, iri2, iri5),

            Values.statement(iri1, iri2, iri3, iri6),
            Values.statement(iri2, iri3, iri1, iri6),
            Values.statement(iri3, iri1, iri2, iri6),)

        val query = """
            PREFIX gd: <urn:docrozza:stardog:normalization:>
            SELECT ?context ?hash FROM NAMED <tag:stardog:api:context:all> WHERE {
                GRAPH ?context {}
                (?context) gd:graphDigest (?hash)
            }
        """.trimIndent()

        val sameHash = "qGZvwUk4k2v3Eu1vqahBjIESVfEq5wPxSYkleCXjXzE="
        val otherHash = "q1XNB/1SJs6RqaprS+Q1ShP89X14XIOe5p6c+8qya6w="
        val actual = mutableSetOf<Pair<IRI, String>>()

        query(query) { results ->
            assertTrue(results.hasNext())
            actual.add(results.next().let { Pair(it.context("context"), it.hash("hash")) })
            assertTrue(results.hasNext())
            actual.add(results.next().let { Pair(it.context("context"), it.hash("hash")) })
            assertTrue(results.hasNext())
            actual.add(results.next().let { Pair(it.context("context"), it.hash("hash")) })
            assertFalse(results.hasNext())
        }

        assertEquals(setOf(Pair(iri4, sameHash), Pair(iri5, otherHash), Pair(iri6, sameHash)), actual)
    }

    private fun createData(vararg statements: Statement) {
        connectionFactory.use { it.add().graph(statements.toSet()) }
    }

    private fun query(query: String, check: (SelectQueryResult) -> Unit) {
        connectionFactory.use { connection ->
            connection.select(query)
                .execute()
                .also { check(it) }
                .close()
        }
    }

    private fun BindingSet.hash(field: String) : String {
        val value = this[field]
        assertTrue(value is Literal)
        assertEquals(XSD.BASE64BINARY, value.datatypeIRI())
        return value.label()
    }

    private fun BindingSet.context(field: String) : IRI {
        val value = this[field]
        assertTrue(value is IRI)
        return value
    }
}