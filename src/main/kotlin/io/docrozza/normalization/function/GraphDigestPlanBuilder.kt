package io.docrozza.normalization.function

import com.complexible.stardog.plan.AbstractPropertyFunctionNodeBuilder
import com.complexible.stardog.plan.filter.expr.Constant
import com.google.common.collect.ImmutableSet

class GraphDigestPlanBuilder : AbstractPropertyFunctionNodeBuilder<GraphDigestPlan>() {

    override fun hasInputs() = mSubjects.any { it.isVariable }

    override fun getInputs() = mSubjects.filter { it.isVariable }

    override fun validate() {
        super.validate()

        require(subjects.size == 1) {
            "GraphDigestPlan requires a single subject argument for the graph to digest"
        }

        require(subjects[0].isVariable || (subjects[0] as Constant).isResource) {
            "GraphDigestPlan requires a single subject variable or an RDF Resource as the graph to digest"
        }

        require(objects.size == 1 && objects[0].isVariable) {
            "GraphDigestPlan requires a single object variable to bind the digest to"
        }
    }

    override fun createNode(subjectVars: ImmutableSet<Int>, objectVars: ImmutableSet<Int>, contextVars: ImmutableSet<Int>,
                            allVars: ImmutableSet<Int>): GraphDigestPlan {
        val assured = ImmutableSet.builder<Int>()
            .addAll(mArg.assuredVars)
            .addAll(objectVars)
            .build()

        return GraphDigestPlan(mArg, mSubjects, mObjects, mContext, mScope, mCost, mCardinality, subjectVars,
            mArg.predicateVars, objectVars, contextVars, assured, allVars)
    }
}