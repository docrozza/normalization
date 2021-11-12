package io.docrozza.normalization.canonize

import com.stardog.stark.BNode
import java.util.NoSuchElementException

class Permutations(items: List<BNode>) : Iterator<List<BNode>> {

    private var done = false

    private val list = items.sortedBy { it.id() }.toMutableList()

    private val left = mutableMapOf<BNode, Boolean>().apply {
        list.forEach { this[it] = true }
    }

    override fun hasNext() = !done

    override fun next() : List<BNode> {
        if (done) {
            throw NoSuchElementException("All permutations already completed")
        }

        // copy current permutation
        val rval = list.toMutableList()

        // get the largest mobile element k (mobile: element is greater than the one it is looking at)
        var k : BNode? = null
        var pos = 0
        list.forEachIndexed { i, element ->
            val loopK = k
            val isLeft = left.getValue(element)
            if ((loopK == null || checkElement(element, loopK)) &&
                ((isLeft && i > 0 && checkElement(element, list[i - 1])) ||
                (!isLeft && i < (list.size - 1) && checkElement(element, list[i + 1])))) {
                k = element
                pos = i
            }
        }

        if (k == null) {
            // no more permutations
            this.done = true
        } else {
            // swap k and the element it is looking at
            val foundK = k!!
            val swap = if (left.getValue(foundK)) pos - 1 else pos + 1
            list[pos] = list[swap]
            list[swap] = foundK

            // reverse the direction of all elements larger than k
            list.forEach { element ->
                if (checkElement(element, foundK)) {
                    left[element] = !left.getValue(element)
                }
            }
        }

        return rval
    }

    private fun checkElement(element: BNode, kItem: BNode) = element.id() > kItem.id()
}