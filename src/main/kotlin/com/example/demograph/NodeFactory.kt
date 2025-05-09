package com.example.demograph

import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import com.mxgraph.view.mxGraph
import javax.swing.JOptionPane

/**
 * A factory class for creating and manipulating nodes in the graph
 */
class NodeFactory(private val graph: mxGraph) {
    
    // Reference to GraphManager for updates
    private var graphManager: GraphManager? = null
    
    // Set GraphManager reference
    fun setGraphManager(manager: GraphManager) {
        graphManager = manager
    }
    
    /**
     * Creates a root node with the specified query text
     */
    fun createRootNode(parent: Any): Any {
        graph.model.beginUpdate()
        try {
            // Create HTML content for the query node
            val queryText = "How do I use Audio Eraser?"
            val content = NodeEditDialog.createQueryNodeContent(queryText)
            
            // Position in the center-left of the screen
            val x = 100.0
            val y = 100.0
            
            // Create the vertex (node) with consistent width
            return graph.insertVertex(
                parent, 
                "root", 
                content, 
                x, 
                y, 
                GraphManager.NODE_WIDTH, 
                GraphManager.NODE_HEIGHT
            )
        } finally {
            graph.model.endUpdate()
        }
    }
    
    /**
     * Adds a circular add button to a node, connected with a dashed line
     */
    fun addAddButtonToNode(node: Any): Any {
        graph.model.beginUpdate()
        try {
            // Get node geometry
            val nodeGeometry = graph.getCellGeometry(node)
            
            // Calculate position for the add button (to the right of the node)
            val x = nodeGeometry.x + nodeGeometry.width + 40
            val y = nodeGeometry.y + nodeGeometry.height / 2 - 15 // Center vertically
            
            // Create the add button
            val addButtonValue = "+"
            val addButton = graph.insertVertex(graph.defaultParent, null, addButtonValue, x, y, 30.0, 30.0, "addButton")
            
            // Create edge from node to add button (dashed line)
            graph.insertEdge(graph.defaultParent, null, "", node, addButton, "dashedEdge")
            
            return addButton
        } finally {
            graph.model.endUpdate()
        }
    }
    
    /**
     * Creates a new node with specified properties
     */
    fun addNode(x: Double, y: Double, previousStepId: String?, nextStepId: String? = null, parent: Any): Any {
        graph.model.beginUpdate()
        try {
            val nodeName = "Node ${System.currentTimeMillis()}"
            val label = NodeEditDialog.createNodeContentFromText(nodeName)
            
            // Store node data as object instead of just label
            val nodeData = NodeData(System.currentTimeMillis().toString(), nodeName, previousStepId, nextStepId)
            val vertex = graph.insertVertex(
                parent, 
                null, 
                label, 
                x, 
                y, 
                GraphManager.NODE_WIDTH, 
                GraphManager.NODE_HEIGHT,
                "greenNode" // New nodes use green style
            )
            
            // Connect to previous and next steps if provided
            if (previousStepId != null) {
                connectToPreviousStep(vertex, previousStepId)
            }
            
            if (nextStepId != null) {
                connectToNextStep(vertex, nextStepId)
            }
            
            // Update connection dots after node creation
            graphManager?.updateAfterNodeChange()
            
            return vertex
        } finally {
            graph.model.endUpdate()
        }
    }
    
    /**
     * Copy (duplicate) a node
     */
    fun copyNode(sourceCell: mxCell): Any {
        graph.model.beginUpdate()
        try {
            // Get the source cell's properties
            val sourceGeometry = graph.getCellGeometry(sourceCell)
            val sourceContent = graph.getModel().getValue(sourceCell).toString()
            
            // Position for the duplicate node (offset slightly to be visible)
            val newX = sourceGeometry.x + 20.0
            val newY = sourceGeometry.y + 80.0
            
            // Create a duplicate node
            val duplicateNode = graph.insertVertex(
                graph.defaultParent, 
                null,
                sourceContent,
                newX,
                newY,
                GraphManager.NODE_WIDTH,
                GraphManager.NODE_HEIGHT,
                sourceCell.style
            )
            
            // Add circular add button to the new node
            addAddButtonToNode(duplicateNode)
            
            // Update connection dots after node creation
            graphManager?.updateAfterNodeChange()
            
            // Show a brief message that the node was copied
            JOptionPane.showMessageDialog(
                null,
                "Node duplicated successfully",
                "Copy Complete",
                JOptionPane.INFORMATION_MESSAGE
            )
            
            return duplicateNode
        } finally {
            graph.model.endUpdate()
        }
    }
    
    private fun connectToPreviousStep(vertex: Any, previousStepId: String) {
        // Find the previous step cell
        val cells = graph.getChildCells(graph.defaultParent)
        for (cell in cells) {
            val cellValue = graph.model.getValue(cell)
            if (cellValue is NodeData && cellValue.id == previousStepId) {
                // Create edge from previous to current
                graph.insertEdge(graph.defaultParent, null, "", cell, vertex)
                break
            }
        }
    }
    
    private fun connectToNextStep(vertex: Any, nextStepId: String) {
        // Find the next step cell
        val cells = graph.getChildCells(graph.defaultParent)
        for (cell in cells) {
            val cellValue = graph.model.getValue(cell)
            if (cellValue is NodeData && cellValue.id == nextStepId) {
                // Create edge from current to next
                graph.insertEdge(graph.defaultParent, null, "", vertex, cell)
                break
            }
        }
    }
    
    fun deleteSelected(cells: Array<Any>) {
        if (cells.isNotEmpty()) {
            graph.model.beginUpdate()
            try {
                graph.removeCells(cells)
                
                // Update connection dots after node deletion
                graphManager?.updateAfterNodeChange()
            } finally {
                graph.model.endUpdate()
            }
        } else {
            JOptionPane.showMessageDialog(null, "No cells selected")
        }
    }
} 