package io.docrozza.normalization.canonize

import com.complexible.stardog.plan.filter.LiteralComparator
import com.stardog.stark.*
import com.stardog.stark.vocabs.XSD

class StatementComparator(private val checkContext: Boolean = true) : Comparator<Statement> {

    private val comparator = LiteralComparator.SPEC

    override fun compare(s1: Statement, s2: Statement) : Int {
        var result = compare(s1.subject(), s2.subject())
        if (result == 0) {
            result = compare(s1.predicate(), s2.predicate())
        }

        if (result == 0) {
            result = compare(s1.`object`(), s2.`object`())
        }

        if (result == 0 && checkContext) {
            result = compare(s1.context(), s2.context())
        }

        return result
    }

    private fun compare(r1: Resource, r2: Resource) : Int {
        return if (r1 is BNode) {
            if (r2 is BNode) r1.id().compareTo(r2.id()) else 1
        } else {
            if (r2 is BNode) -1 else r1.toString().compareTo(r2.toString())
        }
    }

    private fun compare(v1: Value, v2: Value) : Int {
        return when {
            v1 is Literal && v2 is Literal -> {
                // need to go long as Stardog won't do comparisons of non-XSD literals
                val d1 = v1.datatypeIRI()
                val d2 = v2.datatypeIRI()
                if (d1.namespace() == XSD.NAMESPACE) {
                    if (d2.namespace() == XSD.NAMESPACE) {
                        comparator.compare(v1, v2).intValue()
                    } else {
                        -1
                    }
                } else {
                    if (d2.namespace() == XSD.NAMESPACE) {
                        1
                    } else {
                        val res = compare(d1, d2)
                        if (res == 0) v1.label().compareTo(v2.label()) else res
                    }
                }

            }
            v1 is Literal && v2 is Resource -> -1
            v1 is Resource && v2 is Literal -> 1
            v1 is Resource && v2 is Resource -> compare(v1, v2)
            else -> error("Not a Resource or Literal")
        }
    }
}