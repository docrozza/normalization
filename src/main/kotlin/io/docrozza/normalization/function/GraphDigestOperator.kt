package io.docrozza.normalization.function

import com.complexible.stardog.index.IndexOrder
import com.complexible.stardog.index.IteratorBuilder
import com.complexible.stardog.index.IteratorBuilder.CountsRequired
import com.complexible.stardog.index.Quad
import com.complexible.stardog.plan.SortType
import com.complexible.stardog.plan.eval.ExecutionContext
import com.complexible.stardog.plan.eval.operator.*
import com.complexible.stardog.plan.eval.operator.impl.AbstractOperator
import com.stardog.stark.*
import com.stardog.stark.vocabs.XSD
import io.docrozza.normalization.canonize.GraphNormalization
import io.docrozza.normalization.StatementUtils.writeLine
import java.security.MessageDigest
import java.util.*


class GraphDigestOperator(
    context: ExecutionContext,
    private val node: GraphDigestPlan,
    private val op : Operator) : AbstractOperator(context, SortType.UNSORTED), PropertyFunctionOperator {

    private val extender = context.solutionFactory.createSolutionExtender(node.allVars)

    private val digester = MessageDigest.getInstance("SHA-256")

    override fun getVars() : MutableSet<Int> = node.allVars

    override fun accept(visitor: OperatorVisitor) = visitor.visit(this)

    override fun performReset() = digester.reset()

    override fun computeNext(): Solution? {
        val objVar = node.objects.first().name

        while (op.hasNext()) {
            checkCanceled()
            digester.reset()

            val current = op.next()
            var count = 0

            IteratorBuilder.create<Quad>(reader, IndexOrder.SPO, CountsRequired.No, executionContext.cancellationPoint)
                .context(context(current))
                .iterator().also { results ->
                    results.asSequence()
                        .onEach { checkCanceled() }
                        .map { Values.statement(it.x.value(), it.y.value(), it.z.value()) }
                        .let { GraphNormalization.canonize(it) }
                        .fold(digester) { digest, s ->
                            count++
                            digest.update(s.writeLine().encodeToByteArray())
                            digest
                        }
                }
                .close()

            if (count > 0) {
                return extender.extend(current).apply {
                    val hash = Base64.getEncoder().encodeToString(digester.digest())
                    set(objVar, mappings.add(Values.literal(hash, XSD.BASE64BINARY)))
                }
            }
        }

        return endOfData()
    }

    private fun context(solution: Solution) : Long {
        val term = node.subjects[0]
        return if (term.isVariable) solution.get(term.name) else term.index
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Value> Long.value() = mappings.getValue(this) as V
}