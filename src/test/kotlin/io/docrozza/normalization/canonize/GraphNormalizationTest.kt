package io.docrozza.normalization.canonize

import com.stardog.stark.Statement
import com.stardog.stark.io.RDFFormat
import com.stardog.stark.io.RDFFormats
import com.stardog.stark.io.RDFParsers
import io.docrozza.normalization.StatementUtils.writeString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Paths
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.io.path.readText
import kotlin.test.assertEquals

class GraphNormalizationTest {

    @Suppress("unused")
    companion object {
        @JvmStatic fun testNTriples() : Stream<String> {
            return IntStream.range(1, 63)
                .filter { it !in 57..59 } // skip the quad related tests
                .mapToObj{ it.toString().padStart(3, '0') }
        }

        @JvmStatic fun testNQuads() : Stream<String> {
            return IntStream.range(57, 59)
                .mapToObj{ it.toString().padStart(3, '0') }
        }
    }

    @ParameterizedTest(name = "Running canonization over test file test{0}-in.nq")
    @MethodSource("testNTriples")
    fun checkTriplesCanonization(testId: String) {
        val canonical = GraphNormalization.canonize(statements(testId, RDFFormats.NTRIPLES))
            .map { it.writeString() }
            .joinToString(System.lineSeparator())
        assertEquals(lines(testId), canonical, "should canonize the RDF to the expected statements")
    }

    @ParameterizedTest(name = "Running canonization over test file test{0}-in.nq")
    @MethodSource("testNQuads")
    fun checkQuadsCanonization(testId: String) {
        val canonical = GraphNormalization.canonize(statements(testId, RDFFormats.NQUADS), true)
            .map { it.writeString() }
            .joinToString(System.lineSeparator())
        assertEquals(lines(testId), canonical, "should canonize the RDF to the expected statements")
    }

    private fun statements(testId: String, format: RDFFormat) : Sequence<Statement> {
        val path = Paths.get("src/test/resources/normalization", "test$testId-in.nq")
        return RDFParsers.read(path, format).asSequence()
    }

    private fun lines(testId: String) : String {
        return Paths.get("src/test/resources/normalization", "test$testId-urdna2015.nq")
            .readText()
            .replace(Regex("\n\r"), System.lineSeparator())
            .trim()
    }
}