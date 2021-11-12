package io.docrozza.normalization.function

import com.complexible.stardog.index.statistics.Cardinality
import com.complexible.stardog.plan.*
import com.google.common.collect.ImmutableSet
import com.complexible.stardog.plan.QueryDataset.Scope
import com.google.common.collect.ImmutableList

class GraphDigestPlan(
    parent: PlanNode,
    subjects: List<QueryTerm>,
    objects: List<QueryTerm>,
    context: QueryTerm?,
    scope: Scope,
    cost: Double,
    cardinality: Cardinality,
    subjVars: ImmutableSet<Int>,
    predVars: ImmutableSet<Int>,
    objVars: ImmutableSet<Int>,
    contextVars: ImmutableSet<Int>,
    assuredVars: ImmutableSet<Int>,
    allVars: ImmutableSet<Int>): AbstractPropertyFunctionPlanNode(parent, subjects, objects, context, scope, cost,
        cardinality, subjVars, predVars, objVars, contextVars, assuredVars, allVars) {

    private val inputs = subjects.asSequence()
        .filter { it.isVariable }
        .fold(ImmutableList.builder<QueryTerm>()) { list, v -> list.add(v) }
        .build()

    override fun getURI() = GraphDigest.iri

    override fun getInputs() : ImmutableList<QueryTerm> = inputs

    override fun canEquals(other: Any) = other is GraphDigestPlan

    override fun createBuilder() = GraphDigestPlanBuilder()

    override fun copy() = GraphDigestPlan(arg.copy(), subjects, objects, context, scope, cost, cardinality, subjectVars,
        predicateVars, objectVars, contextVars, assuredVars, allVars)
}