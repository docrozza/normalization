package io.docrozza.normalization.function

import com.complexible.stardog.plan.PropertyFunction
import com.complexible.stardog.plan.PropertyFunctionPlanNode
import com.complexible.stardog.plan.eval.ExecutionContext
import com.complexible.stardog.plan.eval.TranslateException
import com.complexible.stardog.plan.eval.operator.Operator
import com.google.common.collect.ImmutableList
import com.stardog.stark.IRI
import com.stardog.stark.Values

class GraphDigest : PropertyFunction {

    companion object {
        val iri = Values.iri("urn:docrozza:stardog:normalization:graphDigest")
    }

    override fun getURIs() : ImmutableList<IRI> = ImmutableList.of(iri)

    override fun newBuilder() = GraphDigestPlanBuilder()

    override fun translate(context: ExecutionContext, planNode: PropertyFunctionPlanNode, op: Operator) : Operator {
        return if (planNode is GraphDigestPlan) {
            GraphDigestOperator(context, planNode, op)
        } else {
            throw TranslateException("Incorrect type of PlanNode supplied to this PropertyFunction, cannot translate")
        }
    }
}