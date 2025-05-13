package com.example.demograph.model

import com.mxgraph.model.mxGeometry
import com.mxgraph.model.mxICell
import com.mxgraph.view.mxGraph

class GraphModel {
    private val graph = mxGraph()
    val parent = graph.defaultParent
    private var nodeCount = 0
    private var edgeCount = 0

    init {
        graph.isCellsSelectable = true
//        graph.isCellsConnectable = false
        graph.isCellsEditable = true
        graph.isHtmlLabels = true
    }

    fun getGraph(): mxGraph = graph

    fun addNode(nodeData: NodeData) {
        graph.model.beginUpdate()
        try {
            nodeCount++
            val label = "<div style='text-align:center'>${nodeData.guideContent}</div>"
            val vertex = graph.insertVertex(
                parent, nodeData.id, label,
                nodeData.xNode, nodeData.yNode, nodeData.width, nodeData.height
            )
            val nodeMap = getNodeMap()
            nodeData.nextStep.forEach { targetId ->
                val target = nodeMap[targetId] as mxICell? ?: return@forEach
                graph.insertEdge(parent, "edge${++edgeCount}", "", vertex, target)
            }
            nodeData.prevStep.forEach { sourceId ->
                val source = nodeMap[sourceId] as mxICell? ?: return@forEach
                graph.insertEdge(parent, "edge${++edgeCount}", "", source, vertex)
            }
        } finally {
            graph.model.endUpdate()
        }
    }

    fun addEdge(source: mxICell, target: mxICell) {
        graph.model.beginUpdate()
        try {
            graph.insertEdge(parent, "edge${++edgeCount}", "", source, target)
        } finally {
            graph.model.endUpdate()
        }
    }

    fun updateNode(nodeData: NodeData) {
        graph.model.beginUpdate()
        try {
            val nodeMap = getNodeMap()
            val cell = nodeMap[nodeData.id] as? mxICell ?: return
            graph.model.setValue(cell, "<div style='text-align:center'>${nodeData.guideContent}</div>")
        } finally {
            graph.model.endUpdate()
        }
    }

    fun deleteSelected() {
        val cells = graph.selectionCells
        if (cells.isNotEmpty()) {
            graph.model.beginUpdate()
            try {
                graph.removeCells(cells)
            } finally {
                graph.model.endUpdate()
            }
        }
    }

    fun getNodeMap(): Map<String, Any> {
        val nodeMap = mutableMapOf<String, Any>()
        val root = graph.model.root as mxICell
        for (i in 0 until graph.model.getChildCount(root)) {
            collectNodes(graph.model.getChildAt(root, i) as mxICell, nodeMap)
        }
        return nodeMap
    }

    fun collectIds(): Set<String> {
        val ids = mutableSetOf<String>()
        val root = graph.model.root as mxICell
        for (i in 0 until graph.model.getChildCount(root)) {
            collectIds(graph.model.getChildAt(root, i) as mxICell, ids)
        }
        return ids
    }

    fun collectNodesAndEdges(
        nodes: MutableList<NodeData>,
        edgeMap: MutableMap<String, MutableList<String>>,
        prevEdgeMap: MutableMap<String, MutableList<String>>
    ) {
        val root = graph.model.root as mxICell
        for (i in 0 until graph.model.getChildCount(root)) {
            collectNodesAndEdges(graph.model.getChildAt(root, i) as mxICell, nodes, edgeMap, prevEdgeMap)
        }
    }

    private fun collectNodes(cell: mxICell, nodeMap: MutableMap<String, Any>) {
        if (graph.model.isVertex(cell) && cell.id != null) {
            nodeMap[cell.id] = cell
        }
        for (i in 0 until graph.model.getChildCount(cell)) {
            collectNodes(graph.model.getChildAt(cell, i) as mxICell, nodeMap)
        }
    }

    private fun collectIds(cell: mxICell, ids: MutableSet<String>) {
        if (graph.model.isVertex(cell) && cell.id != null) {
            ids.add(cell.id)
        }
        for (i in 0 until graph.model.getChildCount(cell)) {
            collectIds(graph.model.getChildAt(cell, i) as mxICell, ids)
        }
    }

    private fun collectNodesAndEdges(
        cell: mxICell,
        nodes: MutableList<NodeData>,
        edgeMap: MutableMap<String, MutableList<String>>,
        prevEdgeMap: MutableMap<String, MutableList<String>>
    ) {
        if (graph.model.isVertex(cell)) {
            val geometry = graph.model.getGeometry(cell) as mxGeometry?
            nodes.add(
                NodeData(
                    id = cell.id ?: throw IllegalStateException("Node missing ID"),
                    guideContent = graph.model.getValue(cell)?.toString() ?: "",
                    xNode = geometry?.x ?: 0.0,
                    yNode = geometry?.y ?: 0.0
                )
            )
        } else if (graph.model.isEdge(cell)) {
            val source = cell.getTerminal(true) as mxICell?
                ?: throw IllegalStateException("Edge missing source")
            val target = cell.getTerminal(false) as mxICell?
                ?: throw IllegalStateException("Edge missing target")
            val sourceId = source.id ?: throw IllegalStateException("Source node missing ID")
            val targetId = target.id ?: throw IllegalStateException("Target node missing ID")
            edgeMap.computeIfAbsent(sourceId) { mutableListOf() }.add(targetId)
            prevEdgeMap.computeIfAbsent(targetId) { mutableListOf() }.add(sourceId)
        }
        for (i in 0 until graph.model.getChildCount(cell)) {
            collectNodesAndEdges(graph.model.getChildAt(cell, i) as mxICell, nodes, edgeMap, prevEdgeMap)
        }
    }

    fun clear() {
        val cells = graph.getChildCells(parent, true, true)
        if (cells.isNotEmpty()) {
            graph.model.beginUpdate()
            try {
                graph.removeCells(cells)
            } finally {
                graph.model.endUpdate()
            }
        }
        nodeCount = 0
        edgeCount = 0
    }

    fun getNodeCount(): Int = nodeCount
    fun getEdgeCount(): Int = edgeCount
    fun setNodeCount(count: Int) {
        nodeCount = count
    }
    fun setEdgeCount(count: Int) {
        edgeCount = count
    }
}