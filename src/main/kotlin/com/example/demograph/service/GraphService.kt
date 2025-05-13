package com.example.demograph.service

import com.example.demograph.model.GraphModel
import com.example.demograph.model.NodeData
import com.example.demograph.ui.NodeDialog
import com.mxgraph.model.mxICell
import com.mxgraph.swing.mxGraphComponent
import javax.swing.JOptionPane
import javax.swing.JPanel

class GraphService(private val graphModel: GraphModel) {
    fun addNodeWithDialog(parent: JPanel) {
        val existingIds = graphModel.collectIds()
        val nodeData = NodeDialog(parent, existingIds).show()
        if (nodeData != null) {
            graphModel.addNode(nodeData)
        }
    }

    fun deleteNode(nodeId: String) {
        val nodeMap = graphModel.getNodeMap()
        val cell = nodeMap[nodeId] as? mxICell ?: return
        graphModel.getGraph().model.beginUpdate()
        try {
            graphModel.getGraph().removeCells(arrayOf(cell))
            println("deleteNode: Deleted node $nodeId")
        } finally {
            graphModel.getGraph().model.endUpdate()
        }
    }

    fun addEdgeBetweenSelected() {
        val selectedCells = graphModel.getGraph().selectionCells
        if (selectedCells.size == 2 && selectedCells.all { graphModel.getGraph().model.isVertex(it) }) {
            graphModel.addEdge(selectedCells[0] as mxICell, selectedCells[1] as mxICell)
        } else {
            JOptionPane.showMessageDialog(null, "Please select exactly two nodes to add an edge")
        }
    }

    fun deleteSelected() {
        graphModel.deleteSelected()
        if (graphModel.getGraph().selectionCells.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No cells selected")
        }
    }
}