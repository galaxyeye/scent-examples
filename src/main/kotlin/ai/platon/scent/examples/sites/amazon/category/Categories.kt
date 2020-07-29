package ai.platon.scent.examples.sites.amazon.category

import ai.platon.pulsar.dom.nodes.Anchor
import ai.platon.pulsar.dom.nodes.node.ext.*
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeVisitor
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

interface LocalCategoryNodeVisitor {
    fun head(node: LocalCategoryNode, depth: Int)
    fun tail(node: LocalCategoryNode, depth: Int) {}
}

object LocalCategoryNodeTraversor {
    fun traverse(visitor: LocalCategoryNodeVisitor, root: LocalCategoryNode) {
        var node: LocalCategoryNode? = root
        var depth = 0
        while (node != null) {
            visitor.head(node, depth)
            if (node.childNodeSize > 0) {
                node = node.childNode(0)
                depth++
            } else {
                while (node!!.nextSibling() == null && depth > 0) {
                    visitor.tail(node, depth)
                    node = node.parentNode
                    depth--
                }
                visitor.tail(node, depth)
                if (node === root) break
                node = node.nextSibling()
            }
        }
    }
}

class LocalCategoryNode(
        val name: String,
        val href: String,
        val left: Int,
        val rootNode: LocalCategoryNode?
): Comparable<LocalCategoryNode> {

    var depth = 0
    var parentNode: LocalCategoryNode? = null
    val children = mutableListOf<LocalCategoryNode>()
    val childNodeSize get() = children.size
    var selector: String? = null

    fun childNode(i: Int): LocalCategoryNode = children[i]

    fun nextSibling(): LocalCategoryNode? {
        val siblings = parentNode?.children?:return null
        val i = siblings.indexOfFirst { it == this } + 1
        return if (i >= 1 && i < siblings.size) siblings[i] else null
    }

    fun sumBy(accum: (LocalCategoryNode) -> Int): Int {
        var sum = 0
        LocalCategoryNodeTraversor.traverse(object : LocalCategoryNodeVisitor {
            override fun head(node: LocalCategoryNode, depth: Int) {
                sum += accum(node)
            }
        }, this)
        return sum
    }

    fun forEach(action: (LocalCategoryNode) -> Unit) {
        LocalCategoryNodeTraversor.traverse(object : LocalCategoryNodeVisitor {
            override fun head(node: LocalCategoryNode, depth: Int) {
                action(node)
            }
        }, this)
    }

//    override fun hashCode(): Int {
//        return href.hashCode()
//    }

    // lead to a circle
//    override fun equals(other: Any?): Boolean {
//        return other is NavNode && other.text == text && other.href == href
//    }

    override fun compareTo(other: LocalCategoryNode): Int {
        val b = href.compareTo(other.href)
        return if (b == 0) name.compareTo(other.name) else b
    }

    override fun toString(): String {
        return "$name | $href"
    }

    companion object {
        private fun getDepth(categoryNode: LocalCategoryNode): Int {
            var d = 0
            var p = categoryNode.parentNode
            while (p != null) {
                ++d
                p = p.parentNode
            }
            return d
        }
    }
}

/**
 * Implements the conversion by walking the input
 */
class LocalCategoryTreeBuilder(val rootNode: LocalCategoryNode) : NodeVisitor {
    private var parentNode = rootNode
    private var previousNode: LocalCategoryNode? = rootNode

    override fun head(source: Node, d: Int) {
        if (!filter(source)) return

        val anchor = source.ancestors().firstOrNull { it.isAnchor }
        val href = anchor?.attr("abs:href") ?: ""
        val node = LocalCategoryNode(source.cleanText, href, source.left, rootNode)

        previousNode?.also {
            if (node.left > it.left) {
                node.depth = 1 + it.depth
                parentNode = it
            }
        }

        node.parentNode = parentNode
        node.parentNode?.children?.add(node)

        previousNode = node
    }

    override fun tail(source: Node, d: Int) {
        if (!filter(source)) return
    }

    private fun filter(source: Node): Boolean {
        return source is TextNode && source.numChars > 1 && (rootNode.name != source.cleanText)
    }
}

/**
 * Implements the conversion by walking the input
 */
class GlobalCategoryTreeBuilder(val dest: GlobalCategoryNode) : LocalCategoryNodeVisitor {
    var current = dest

    override fun head(node: LocalCategoryNode, depth: Int) {
        val category = GlobalCategoryNode(node.name, node.href, current)
        current.children.add(category)
        current = category
    }

    override fun tail(node: LocalCategoryNode, depth: Int) {
        current = current.parentNode!!
    }
}

data class GlobalCategoryDataNode(
        val id: Int,
        val parentId: Int,
        val depth: Int,
        val numChildren: Int,
        val path: String,
        val url: String
) {
    override fun toString(): String {
        return String.format("%5d | %5d | %3d | %3d | %-80s | %s\n", id, parentId, depth, numChildren, path, url)
    }

    companion object {
        fun parse(s: String): GlobalCategoryDataNode {
            return s.split(" | ").map { it.trim() }.let {
                GlobalCategoryDataNode(it[0].toInt(), it[1].toInt(), it[2].toInt(), it[3].toInt(), it[4], it[5])
            }
        }
    }
}

class GlobalCategoryNode(
        val name: String,
        val url: String,
        val parentNode: GlobalCategoryNode? = null,
        val id: Int = sequencer.incrementAndGet()
): Comparable<GlobalCategoryNode> {
    val subcategoryAnchors = ConcurrentSkipListSet<Anchor>()
    val children = ConcurrentSkipListSet<GlobalCategoryNode>()
    val depth get() = getDepth(this)
    val path get() = getPath(this)
    var localCategoryNodeTree: LocalCategoryNode? = null
    val paginationLinks = ConcurrentSkipListSet<Anchor>()

    fun toDataNode(): GlobalCategoryDataNode {
        return GlobalCategoryDataNode(id, parentNode?.id?:0, depth, children.size, path, url)
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is GlobalCategoryNode && path == other.path
    }

    override fun compareTo(other: GlobalCategoryNode): Int = path.compareTo(other.path)

    override fun toString(): String {
        return String.format("%5d | %5d | %3d | %3d | %-80s | %s\n",
                id, parentNode?.id?:0, depth, children.size, path, url)
    }

    companion object {
        val sequencer = AtomicInteger()

        private fun getDepth(categoryNode: GlobalCategoryNode): Int {
            var d = 0
            var p = categoryNode.parentNode
            while (p != null) {
                ++d
                p = p.parentNode
            }
            return d
        }

        private fun getPath(categoryNode: GlobalCategoryNode): String {
            if (categoryNode.name == "Root") return categoryNode.name

            val sb = StringBuilder(categoryNode.name)
            var p = categoryNode.parentNode
            while (p != null) {
                sb.insert(0, " > ").insert(0, p.name)
                p = p.parentNode
            }
            return sb.toString()
        }
    }
}
