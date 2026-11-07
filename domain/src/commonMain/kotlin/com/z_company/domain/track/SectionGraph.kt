package com.z_company.domain.track

import kotlin.math.abs

/*
 * Граф оцифрованных участков советника (GET /v1/advisor/graph): узлы — стыки и развилки,
 * рёбра — отрезки оси участка-владельца (sectionId + диапазон км). По цепочке рёбер
 * собирается «виртуальный участок» (VirtualSection) как обычный SectionProfile.
 * Чистая логика KMP; совпадает с services/advisor_graph.py на сервере.
 */

/** Узел графа: стык, развилка или тупик. [name] — станция стыка, если известна. */
data class GraphNode(val id: String, val lat: Double, val lon: Double, val name: String)

/** Ребро графа: отрезок оси участка [sectionId] между узлами, км — по его оси ([fromKm] < [toKm]). */
data class GraphEdge(
    val id: String,
    val sectionId: String,
    val fromKm: Double,
    val toKm: Double,
    val fromNode: String,
    val toNode: String,
    val lengthKm: Double,
)

/** Проезд ребра по ходу поезда: реальные км [startKm] → [endKm] (убывание — в обратную сторону оси). */
data class EdgeTraversal(val edge: GraphEdge, val startKm: Double, val endKm: Double) {
    val reverse: Boolean get() = endKm < startKm
    val lengthKm: Double get() = abs(endKm - startKm)
}

/** Точка графа: узел или место на ребре. */
sealed interface GraphPoint {
    data class AtNode(val nodeId: String) : GraphPoint
    data class OnEdge(val edgeId: String, val km: Double) : GraphPoint
}

class SectionGraph(val version: String, val nodes: List<GraphNode>, val edges: List<GraphEdge>) {
    private val nodeById: Map<String, GraphNode> = nodes.associateBy { it.id }
    private val edgeById: Map<String, GraphEdge> = edges.associateBy { it.id }

    fun node(id: String): GraphNode? = nodeById[id]
    fun edge(id: String): GraphEdge? = edgeById[id]
    fun edgesAt(nodeId: String): List<GraphEdge> = edges.filter { it.fromNode == nodeId || it.toNode == nodeId }

    private fun otherEnd(e: GraphEdge, nodeId: String): String = if (e.fromNode == nodeId) e.toNode else e.fromNode
    private fun kmAt(e: GraphEdge, nodeId: String): Double = if (e.fromNode == nodeId) e.fromKm else e.toKm

    /**
     * Кратчайший по км путь между точками графа (через узлы; точки на одном ребре — напрямую).
     * null — пути нет (граф не связен в этой части).
     */
    fun path(from: GraphPoint, to: GraphPoint): List<EdgeTraversal>? {
        if (from is GraphPoint.OnEdge && to is GraphPoint.OnEdge && from.edgeId == to.edgeId) {
            val e = edge(from.edgeId) ?: return null
            return listOf(EdgeTraversal(e, from.km, to.km))
        }
        // Дейкстра по узлам; точки на рёбрах подключаются к их узлам «хвостами» (startTails/endTail).
        val dist = HashMap<String, Double>()
        val cameFrom = HashMap<String, Pair<String, EdgeTraversal>>() // узел -> (предыдущий узел, ребро между ними)
        val startTails = HashMap<String, EdgeTraversal>() // узел -> хвост от точки from до него
        fun relax(u: String, v: String, t: EdgeTraversal) {
            if ((dist[u] ?: Double.MAX_VALUE) + t.lengthKm < (dist[v] ?: Double.MAX_VALUE)) {
                dist[v] = (dist[u] ?: Double.MAX_VALUE) + t.lengthKm
                cameFrom[v] = u to t
            }
        }
        when (from) {
            is GraphPoint.AtNode -> dist[from.nodeId] = 0.0
            is GraphPoint.OnEdge -> {
                val e = edge(from.edgeId) ?: return null
                for (nodeId in listOf(e.fromNode, e.toNode)) {
                    dist[nodeId] = abs(from.km - kmAt(e, nodeId))
                    startTails[nodeId] = EdgeTraversal(e, from.km, kmAt(e, nodeId))
                }
            }
        }
        val settled = HashSet<String>()
        while (true) {
            val u = dist.entries.filter { it.key !in settled }.minByOrNull { it.value }?.key ?: break
            if (!settled.add(u)) break
            for (e in edgesAt(u)) {
                val v = otherEnd(e, u)
                if (v !in settled) relax(u, v, EdgeTraversal(e, kmAt(e, u), kmAt(e, v)))
            }
        }
        var endNode: String? = null
        var endTail: EdgeTraversal? = null
        when (to) {
            is GraphPoint.AtNode -> endNode = to.nodeId.takeIf { dist.containsKey(it) }
            is GraphPoint.OnEdge -> {
                val e = edge(to.edgeId) ?: return null
                val a = dist[e.fromNode]?.plus(abs(to.km - e.fromKm))
                val b = dist[e.toNode]?.plus(abs(to.km - e.toKm))
                if (a == null && b == null) return null
                if ((a ?: Double.MAX_VALUE) <= (b ?: Double.MAX_VALUE)) {
                    endNode = e.fromNode
                    endTail = EdgeTraversal(e, kmAt(e, e.fromNode), to.km)
                } else {
                    endNode = e.toNode
                    endTail = EdgeTraversal(e, kmAt(e, e.toNode), to.km)
                }
            }
        }
        val target = endNode ?: return null
        if (!dist.containsKey(target)) return null
        val middle = ArrayList<EdgeTraversal>()
        val guard = HashSet<String>()
        var cur = target
        while (guard.add(cur)) {
            val (prevNode, t) = cameFrom[cur] ?: break
            middle += t
            cur = prevNode
        }
        middle.reverse()
        val chain = ArrayList<EdgeTraversal>()
        startTails[cur]?.let { chain += it }
        chain += middle
        endTail?.let { chain += it }
        return chain.takeIf { it.isNotEmpty() }
    }
}

/** Подбор точек графа по названиям станций плеча и поиск пути между ними. */
object GraphLeg {
    /**
     * Точки графа, где проходит станция [name]: узлы с этим именем и места на рёбрах,
     * чей владелец держит станцию в своём диапазоне км. Пусто — станции в графе нет.
     */
    fun pointsForStation(graph: SectionGraph, sectionsById: Map<String, SectionProfile>, name: String): List<GraphPoint> {
        val out = ArrayList<GraphPoint>()
        for (n in graph.nodes) {
            if (n.name.isNotEmpty() && SectionPicker.sameStation(n.name, name)) out += GraphPoint.AtNode(n.id)
        }
        for (e in graph.edges) {
            val owner = sectionsById[e.sectionId] ?: continue
            val st = owner.stations.firstOrNull { SectionPicker.sameStation(it.name, name) } ?: continue
            if (st.km >= e.fromKm - 0.05 && st.km <= e.toKm + 0.05) {
                out += GraphPoint.OnEdge(e.id, st.km.coerceIn(e.fromKm, e.toKm))
            }
        }
        return out
    }

    /** Кратчайший по км путь между станциями плеча через граф; null — нет такой пары точек или пути. */
    fun pathForStops(
        graph: SectionGraph,
        sectionsById: Map<String, SectionProfile>,
        fromName: String,
        toName: String,
    ): List<EdgeTraversal>? {
        val froms = pointsForStation(graph, sectionsById, fromName)
        val tos = pointsForStation(graph, sectionsById, toName)
        var best: List<EdgeTraversal>? = null
        var bestKm = Double.MAX_VALUE
        for (f in froms) for (t in tos) {
            val p = graph.path(f, t) ?: continue
            val len = p.sumOf { it.lengthKm }
            if (len < bestKm && len > 0) {
                bestKm = len
                best = p
            }
        }
        return best
    }
}

/** «Маршрут по факту» без плеча: следование по выбранному по GPS участку до ближайшей развилки пути. */
object FactRoute {
    /** Узел развилки, если к нему ведут больше двух рёбер (продолжаются оцифрованные участки). */
    const val JUNCTION_DEGREE = 3

    /** Развилки примыкают к оси не дальше этого (м): узел стыка может быть чуть в стороне от оси. */
    const val JUNCTION_SNAP_M = 60.0

    /**
     * Км оси ближайшей развилки впереди по ходу (null - впереди до конца участка развилок нет).
     * [posKm] - положение поезда на оси участка; [reversed] - поезд идёт от конца оси к началу.
     */
    fun nextJunctionKm(graph: SectionGraph, section: SectionProfile, posKm: Double, reversed: Boolean): Double? =
        nextJunction(graph, section, posKm, reversed)?.km

    /** Ближайшая развилка впереди: км оси, имя узла (пустое - узел без имени станции) и его id. */
    data class Ahead(val km: Double, val name: String, val nodeId: String)

    /** Ближайшая развилка впереди по ходу (см. [nextJunctionKm]); null - развилок впереди нет. */
    fun nextJunction(graph: SectionGraph, section: SectionProfile, posKm: Double, reversed: Boolean): Ahead? {
        val degree = HashMap<String, Int>()
        for (e in graph.edges) {
            degree[e.fromNode] = (degree[e.fromNode] ?: 0) + 1
            degree[e.toNode] = (degree[e.toNode] ?: 0) + 1
        }
        val locator = section.axis.locator
        var best: Ahead? = null
        for (n in graph.nodes) {
            if ((degree[n.id] ?: 0) < JUNCTION_DEGREE) continue
            val hit = locator.project(n.lat, n.lon) ?: continue
            if (hit.offsetM > JUNCTION_SNAP_M) continue
            val km = hit.kmM / 1000.0
            if (!reversed && km <= posKm + 0.05) continue
            if (reversed && km >= posKm - 0.05) continue
            if (best == null || (!reversed && km < best.km) || (reversed && km > best.km)) best = Ahead(km, n.name, n.id)
        }
        return best
    }

    /**
     * Диапазон оси для показа «по факту»: от поезда ([posKm], с запасом [BEHIND_KM] позади) до ближайшей
     * развилки впереди по ходу. null - развилки впереди нет или положение неизвестно - участок целиком.
     */
    fun axisRange(graph: SectionGraph, section: SectionProfile, posKm: Double, reversed: Boolean): ClosedFloatingPointRange<Double>? {
        val junction = nextJunctionKm(graph, section, posKm, reversed) ?: return null
        return axisRangeAt(section, posKm, reversed, junction)
    }

    /** Диапазон от поезда до известной развилки [junctionKm] (без скана узлов графа). */
    fun axisRangeAt(section: SectionProfile, posKm: Double, reversed: Boolean, junctionKm: Double): ClosedFloatingPointRange<Double>? {
        val lengthKm = section.axis.locator.lengthKm
        val behind = (posKm - BEHIND_KM).coerceAtLeast(0.0)
        val ahead = posKm + BEHIND_KM
        val lo = minOf(behind, junctionKm).coerceAtLeast(0.0)
        val hi = maxOf(ahead, junctionKm).coerceAtMost(lengthKm)
        return if (hi - lo < MIN_RANGE_KM) null else lo..hi
    }

    /** Сколько км профиля оставлять позади поезда (чтобы окно показывало и то, что уже проехали). */
    const val BEHIND_KM = 2.0
    const val MIN_RANGE_KM = 0.5
}

/** Ветка за развилкой: ребро графа от узла развилки и подпись для чипа («→ Колпино · 12 км»). */
data class Branch(
    val edgeId: String,
    val edge: GraphEdge,
    /** Имя для чипа: ближайшая по ветке станция (или узел в конце ветки). */
    val title: String,
    /** Расстояние до [title] вдоль ветки, км. */
    val distanceKm: Double,
    /** Узел в конце ветки (для автоопределения направления после прохода развилки). */
    val farNodeId: String,
)

object Branches {
    /**
     * Ветки за ближайшей развилкой впереди: рёбра узла развилки, кроме ребра, по которому поезд
     * подъехал (по нему «назад»). Подпись - ближайшая станция владельца в пределах ребра, иначе
     * имя узла в конце; расстояние - до этой станции, иначе до конца ребра.
     */
    fun ahead(
        graph: SectionGraph,
        section: SectionProfile,
        posKm: Double,
        reversed: Boolean,
        sectionsById: Map<String, SectionProfile> = emptyMap(),
    ): List<Branch> {
        val junction = FactRoute.nextJunction(graph, section, posKm, reversed) ?: return emptyList()
        val node = graph.nodes.firstOrNull { it.id == junction.nodeId } ?: return emptyList()
        val locator = section.axis.locator
        fun kmOnSection(lat: Double, lon: Double): Double? {
            val hit = locator.project(lat, lon) ?: return null
            return if (hit.offsetM <= FactRoute.JUNCTION_SNAP_M) hit.kmM / 1000.0 else null
        }
        fun behind(km: Double): Boolean = if (!reversed) km < posKm - 0.05 else km > posKm + 0.05
        val atNode = graph.edges.filter { it.fromNode == node.id || it.toNode == node.id }
        // Входящее ребро - то, чей дальний узел лежит на этой оси ПОЗАДИ поезда (по нему приехали).
        val incoming = atNode.filter { e ->
            val far = if (e.fromNode == node.id) e.toNode else e.fromNode
            val farNode = graph.nodes.firstOrNull { it.id == far }
            farNode?.let { kmOnSection(it.lat, it.lon)?.let(::behind) } == true
        }.map { it.id }.toSet()
        return atNode.filter { it.id !in incoming }.map { e ->
            val farId = if (e.fromNode == node.id) e.toNode else e.fromNode
            val farNode = graph.nodes.firstOrNull { it.id == farId }
            val owner = sectionsById[e.sectionId] ?: section.takeIf { it.id == e.sectionId }
            // Первая станция владельца на ребре в сторону от развилки; имя узла - запасной вариант.
            val stations = (owner?.stations ?: emptyList())
                .filter { it.km >= minOf(e.fromKm, e.toKm) - 0.05 && it.km <= maxOf(e.fromKm, e.toKm) + 0.05 }
                .sortedBy { abs(it.km - junction.km) }
            val title = stations.firstOrNull()?.name ?: farNode?.name ?: "Ветка"
            val dist = stations.firstOrNull()?.let { abs(it.km - junction.km) } ?: e.lengthKm
            Branch(e.id, e, title, dist, farId)
        }.sortedBy { it.distanceKm }
    }
}
