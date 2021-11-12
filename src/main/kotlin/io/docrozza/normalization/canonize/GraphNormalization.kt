package io.docrozza.normalization.canonize

import com.stardog.stark.BNode
import com.stardog.stark.Statement
import com.stardog.stark.Value
import com.stardog.stark.Values
import io.docrozza.normalization.StatementUtils.writeLine
import java.security.MessageDigest

class GraphNormalization(private val includeContext: Boolean) {

    companion object {

        private val hexCodes = "0123456789abcdef".toCharArray()

        private val aNode = Values.bnode("a")
        private val zNode = Values.bnode("z")

        /**
         * Run the normalisation algorithm over the provided [statements]
         */
        fun normalize(statements: Sequence<Statement>, includeContext: Boolean = false) : Sequence<Statement> {
            return GraphNormalization(includeContext).run(statements)
        }

        /**
         * Normalizes the provided [statements] and then sorts the result of the algorithm
         */
        fun canonize(statements: Sequence<Statement>, includeContext: Boolean = false)  : Sequence<Statement> {
            return normalize(statements, includeContext).sortedWith(StatementComparator(includeContext))
        }

        private fun MessageDigest.update(text: String) : MessageDigest {
            update(text.encodeToByteArray())
            return this
        }

        private fun ByteArray.toHex() : String {
            val r = StringBuilder(this.size * 2)
            for (b in this) {
                val i = b.toInt()
                r.append(hexCodes[(i shr 4) and 0xF]).append(hexCodes[i and 0xF])
            }
            return r.toString()
        }
    }

    private data class QuadsInfo(val quads: MutableList<Statement> = mutableListOf(), var hash: String? = null)

    private data class BNodeTracker(
        private val prefix: String = "c14n",
        private var counter: Int = 0,
        private val identifiers: MutableMap<BNode, String> = linkedMapOf()) {

        operator fun get(node: BNode) : BNode {
            return if (node.id().startsWith(prefix)) node else Values.bnode(mapped(node))
//            return if (node.id.startsWith(prefix)) node else BNode(track(node) + "<-" + node.id)
        }

        fun tracked() : Sequence<BNode> = identifiers.keys.asSequence()

        fun track(existingId: BNode) {
            mapped(existingId)
        }

        fun trackAndGet(existingId: BNode) = "_:${mapped(existingId)}"

        fun contains(existingId: BNode) = identifiers.containsKey(existingId)

        fun clone() = copy(identifiers = linkedMapOf<BNode, String>().apply { putAll(identifiers) })

        private fun mapped(existingId: BNode) = identifiers.getOrPut(existingId) { "$prefix${counter++}" }
    }

    private enum class Position(val label: String) {
        Subject("s"),
        Object("o"),
        Context("g");
    }

    private val blankNodeToQuads = mutableMapOf<BNode, QuadsInfo>()
    private val canonicalTracker = BNodeTracker()

    /**
     * Perform the normalization algorithm as detailed in [https://json-ld.github.io/normalization/spec/index.html]
     */
    fun run(statements: Sequence<Statement>) : Sequence<Statement> {
        // 1 create the normalization state
        val hashToNodes: MutableMap<String, MutableList<BNode>> = mutableMapOf()

        // 2. track all blank nodes in all the quads
        val cached = statements.fold(mutableSetOf<Statement>()) { stmts, s ->
            track(s.subject(), s)
            track(s.`object`(), s)
            track(s.context(), s)

            stmts.add(s)
            stmts
        }

        // 3. contains non-normalized blank nodes
        val nnBlankNodes = blankNodeToQuads.keys.toMutableSet()

        // 4. flag to bail early
        var simple = true
        // 5. issue canonical identifiers for blank nodes
        while (simple) {
            // 5.1
            simple = false
            // 5.2 clear hash to blank nodes map
            hashToNodes.clear()
            // 5.3 for each blank node identifier in non-normalized identifiers:
            // 5.3.1 create a hash, hash, according to the Hash First Degree Quads algorithm.
            // 5.3.2 add hash and identifier to hash to blank nodes map
            nnBlankNodes.forEach { hashToNodes.getOrPut(hashFirstDegree(it)) { mutableListOf() }.add(it) }

            // 5.4 For each hash to identifier list mapping in hash to blank nodes map, lexicographically-sorted by hash:
            hashToNodes.keys
                .asSequence()
                .sorted()
                .map { Pair(it, hashToNodes.getValue(it)) }
                .filter {it.second.size == 1 }
                .forEach {
                    val id = it.second.first()
                    canonicalTracker.track(id)
                    nnBlankNodes.remove(id)
                    hashToNodes.remove(it.first)
                    simple = true
                }
        }

        // 6) For each hash to identifier list mapping in hash to blank nodes map, lexicographically-sorted by hash
        hashToNodes.entries.asSequence()
            .sortedBy { it.key }
            .map { it.value }
            .forEach { nodes ->
                nodes.asSequence()
                    .filterNot { canonicalTracker.contains(it) }
                    .map {
                        val issuer = BNodeTracker("b")
                        issuer.track(it)
                        hashNDegreeQuad(it, issuer)
                    }
                    .sortedBy { it.first }
                    .map { it.second }
                    .forEach { it.tracked().forEach { id -> canonicalTracker.track(id) }}
            }

        // all blank nodes in RDF quads have been assigned canonical identifiers, stored in canonical issuer.
        // each quad is then updated by assigning each of its blank nodes to its new identifier.
        return cached.asSequence().map {
            val context = it.context()
            if (it.subject() is BNode || it.`object`() is BNode || context is BNode) {
                if (includeContext) {
                    Values.statement(renode(it.subject()), it.predicate(), renode(it.`object`()), renode(context))
                } else {
                    Values.statement(renode(it.subject()), it.predicate(), renode(it.`object`()))
                }
            } else {
                it
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Value> renode(value: V) : V {
        return if (value is BNode) canonicalTracker[value] as V else value
    }

    private fun track(value: Value?, statement: Statement) {
        if (value is BNode) {
            blankNodeToQuads.getOrPut(value) { QuadsInfo() }.quads.add(statement)
        }
    }

    private fun doHashToRelated(id: BNode, tracker: BNodeTracker, hashes: MutableMap<String, MutableList<BNode>>,
                                quad: Statement, related: Value?, position: Position) {
        if (related is BNode && id != related) {
            hashes.getOrPut(hashRelatedBlankNode(related, quad, tracker, position)) { mutableListOf() }.add(related)
        }
    }

    private fun createHashToRelated(id: BNode, tracker: BNodeTracker) : Map<String, MutableList<BNode>> {
        val relatedBNodes: MutableMap<String, MutableList<BNode>> = mutableMapOf()

        blankNodeToQuads.getValue(id).quads.forEach {
            doHashToRelated(id, tracker, relatedBNodes, it, it.subject(), Position.Subject)
            doHashToRelated(id, tracker, relatedBNodes, it, it.`object`(), Position.Object)
            doHashToRelated(id, tracker, relatedBNodes, it, it.context(), Position.Context)
        }

        return relatedBNodes
    }

    private fun hashNDegreeQuad(id: BNode, tracker: BNodeTracker) : Pair<String, BNodeTracker> {
        val sha256 = MessageDigest.getInstance("SHA-256")
        var nDegreeTracker = tracker

        createHashToRelated(id, nDegreeTracker).entries.sortedBy { it.key }.forEach { entry ->
            sha256.update(entry.key)

            var chosenPath = ""
            var chosenTracker : BNodeTracker? = null

            for (permutation in Permutations(entry.value)) {
                var trackerCopy = nDegreeTracker.clone()
                var path = ""
                var nextPermutation = false

                val recursionList = mutableListOf<BNode>()
                for (related in permutation) {
                    path += if (canonicalTracker.contains(related)) {
                        canonicalTracker.trackAndGet(related)
                    } else {
                        if (!trackerCopy.contains(related)) {
                            recursionList.add(related)
                        }
                        trackerCopy.trackAndGet(related)
                    }

                    if (chosenPath.isNotEmpty() && path > chosenPath) {
                        nextPermutation = true
                        break
                    }
                }

                if (nextPermutation) {
                    continue
                }

                for (related in recursionList) {
                    val result = hashNDegreeQuad(related, trackerCopy)
                    path += trackerCopy.trackAndGet(related)
                    path += angled(result.first)
                    trackerCopy = result.second

                    if (chosenPath.isNotEmpty() && path > chosenPath) {
                        nextPermutation = true
                        break
                    }
                }

                if (nextPermutation) {
                    continue
                }

                if (chosenPath.isEmpty() || path < chosenPath) {
                    chosenPath = path
                    chosenTracker = trackerCopy
                }
            }

            sha256.update(chosenPath)
            nDegreeTracker = chosenTracker!!
        }

        return Pair(sha256.digest().toHex(), nDegreeTracker)
    }

    private fun hashRelatedBlankNode(related: BNode, quad: Statement, tracker: BNodeTracker, position: Position): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(position.label)

        if (position != Position.Context) {
            sha256.update(angled(quad.predicate().toString()))
        }

        val relatedId = when {
            canonicalTracker.contains(related) -> canonicalTracker.trackAndGet(related)
            tracker.contains(related) -> tracker.trackAndGet(related)
            else -> hashFirstDegree(related)
        }

        return sha256.update(relatedId).digest().toHex()
    }

    private fun hashFirstDegree(id: BNode) : String {
        val info = blankNodeToQuads.getValue(id)
        if (info.hash == null) {
            info.hash = info.quads
                .asSequence()
                .map { aToZ(it, id).writeLine() }
                .sorted()
                .fold(MessageDigest.getInstance("SHA-256")) { sha256, quad ->
                    sha256.update(quad)
                    sha256
                }
                .digest()
                .toHex()
        }

        return info.hash!!
    }

    private fun aToZ(statement: Statement, id: BNode) : Statement {
        return if (statement.subject() is BNode || statement.`object`() is BNode || statement.context() is BNode) {
            val s = aToZ(statement.subject(), id)
            val o = aToZ(statement.`object`(), id)

            val context = statement.context()
            if (includeContext) {
                Values.statement(s, statement.predicate(), o, aToZ(context, id))
            } else {
                Values.statement(s, statement.predicate(), o)
            }
        } else {
            statement
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Value> aToZ(value: V, id: BNode) : V {
        return if (value is BNode) {
            (if (value == id) aNode else zNode) as V
        } else {
            value
        }
    }

    private fun angled(text: String) = "<$text>"
}