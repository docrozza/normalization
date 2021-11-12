package io.docrozza.normalization

import com.stardog.stark.Statement
import com.stardog.stark.Values
import com.stardog.stark.io.ntriples.NTWriter
import java.io.StringWriter

object StatementUtils {

    fun Statement.writeString() = write(false)

    fun Statement.writeLine() = write(true)

    private fun Statement.write(appendLine: Boolean) : String {
        return StringWriter().use {
            NTWriter.serialize(subject(), true, it)
            it.append(' ')

            NTWriter.serialize(predicate(), true, it)
            it.append(' ')

            NTWriter.serialize(`object`(), true, it)
            it.append(' ')

            if (!Values.isDefaultGraph(context())) {
                NTWriter.serialize(context(), true, it)
                it.append(' ')
            }

            if (appendLine) {
                it.appendLine('.')
            } else {
                it.append('.')
            }

            it.toString()
        }
    }
}