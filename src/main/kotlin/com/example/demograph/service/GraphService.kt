package com.example.demograph.service

import com.example.demograph.model.GraphModel
import com.example.demograph.model.NodeData
import com.example.demograph.ui.NodeDialog
import com.example.demograph.ui.NodeEditDialog
import com.mxgraph.model.mxICell
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

    fun addNodeFromSource(parent: JPanel, sourceNodeId: String) {
        val existingIds = graphModel.collectIds()
        val sourceNode = graphModel.getNodeMap()[sourceNodeId] as? mxICell ?: return
        
        val sourceGeometry = graphModel.getGraph().model.getGeometry(sourceNode)
        
        val nodeDialog = NodeDialog(parent, existingIds)
        val nodeData = nodeDialog.showWithPrevStep(sourceNodeId, 
            sourceGeometry.x + sourceGeometry.width + 50.0,
            sourceGeometry.y)
        
        if (nodeData != null) {
            graphModel.addNode(nodeData)
        }
    }

    fun editNodeWithDialog(parent: JPanel, nodeId: String) {
        val nodeMap = graphModel.getNodeMap()
        val cell = nodeMap[nodeId] as? mxICell ?: return
        
        val geometry = graphModel.getGraph().model.getGeometry(cell)

        val htmlValue = graphModel.getGraph().model.getValue(cell)?.toString() ?: ""

        val content = extractContentFromHtml(htmlValue)

        val existingIds = graphModel.collectIds()
        val dialog = NodeEditDialog(parent, nodeId, content, existingIds)
        val newContent = dialog.show()
        
        if (newContent != null && newContent.isNotEmpty()) {
            graphModel.getGraph().model.beginUpdate()
            try {
                val updatedNodeData = NodeData(
                    id = nodeId,
                    guideContent = newContent,
                    xNode = geometry.x,
                    yNode = geometry.y,
                    width = geometry.width,
                    height = geometry.height
                )
                graphModel.updateNode(updatedNodeData)
            } finally {
                graphModel.getGraph().model.endUpdate()
            }
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

    private fun extractContentFromHtml(htmlString: String): String {
        val brIndex = htmlString.indexOf("<br/>")
        if (brIndex != -1 && brIndex + 5 < htmlString.length) {
            val content = htmlString.substring(brIndex + 5)
            return content.replace("</div>", "").trim()
        }

        return htmlString.replace(Regex("<[^>]*>"), "").trim()
    }
}

