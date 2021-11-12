package io.docrozza.normalization

import com.stardog.stark.IRI
import com.stardog.stark.Resource
import com.stardog.stark.Value
import com.stardog.stark.Values
import com.stardog.stark.vocabs.XSD
import io.docrozza.normalization.StatementUtils.writeLine
import io.docrozza.normalization.StatementUtils.writeString
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test as test

class StatementUtilsTest {

    companion object {

        private val iri1 = Values.iri("urn:test:thing1")
        private val iri2 = Values.iri("urn:test:thing2")
        private val iri3 = Values.iri("urn:test:thing3")
        private val iri4 = Values.iri("urn:test:thing4")

        private val bnode1 = Values.bnode("c14n1")
        private val bnode2 = Values.bnode("c14n2")
        private val bnode3 = Values.bnode("c14n3")

        private val lit1 = Values.literal("1")
        private val lit2 = Values.literal("1", XSD.INTEGER)
        private val lit3 = Values.literal("1", iri3)
    }

    @test fun write() {
        assertStatement("<urn:test:thing1> <urn:test:thing2> <urn:test:thing3> .",
            iri1, iri2, iri3)
        assertStatement("<urn:test:thing1> <urn:test:thing2> <urn:test:thing3> <urn:test:thing4> .",
            iri1, iri2, iri3, iri4)
        assertStatement("<urn:test:thing1> <urn:test:thing2> _:c14n1 .",
            iri1, iri2, bnode1)
        assertStatement("<urn:test:thing1> <urn:test:thing2> <urn:test:thing3> _:c14n1 .",
            iri1, iri2, iri3, bnode1)
        assertStatement("_:c14n1 <urn:test:thing2> <urn:test:thing3> _:c14n2 .",
            bnode1, iri2, iri3, bnode2)
        assertStatement("<urn:test:thing1> <urn:test:thing2> \"1\" .",
            iri1, iri2, lit1)
        assertStatement("<urn:test:thing1> <urn:test:thing2> \"1\"^^<http://www.w3.org/2001/XMLSchema#integer> .",
            iri1, iri2, lit2)
        assertStatement("<urn:test:thing1> <urn:test:thing2> \"1\"^^<urn:test:thing3> .",
            iri1, iri2, lit3)
    }

    private fun assertStatement(expected: String, s: Resource, p: IRI, o: Value, c: Resource? = null) {
        val statement = if (c == null) Values.statement(s, p, o) else Values.statement(s, p, o, c)
        assertEquals(expected, statement.writeString())
        assertEquals("$expected${System.lineSeparator()}", statement.writeLine())
    }
}