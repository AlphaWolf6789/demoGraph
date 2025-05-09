package com.example.demograph

import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import com.mxgraph.util.mxEvent
import com.mxgraph.util.mxEventObject
import com.mxgraph.util.mxEventSource.mxIEventListener
import com.mxgraph.util.mxPoint
import com.mxgraph.view.mxGraph
import java.awt.Color
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.HashMap

/**
 * A class to handle the creation and management of connection dots on nodes
 */
class ConnectionDotManager(
    private val graph: mxGraph,
    private val graphComponent: mxGraphComponent
) {
    private val connectionDots = mutableListOf<mxCell>()
    private var activeConnectionDot: mxCell? = null
    private var temporaryPreviewEdge: mxCell? = null
    
    init {
        // Configure connection dots styles
        configureConnectionDotStyles()
        
        // Set up event handlers
        //setupMouseHandlers()
        
        // Configure connection options
        configureConnectionOptions()
    }
    
    /**
     * Initialize styles for connection dots
     */
    private fun configureConnectionDotStyles() {
        val stylesheet = graph.stylesheet
        
        // Purple connection dot style (for purple-bordered nodes)
        val purpleConnectionDotStyle = HashMap<String, Any>()
        purpleConnectionDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        purpleConnectionDotStyle[mxConstants.STYLE_FILLCOLOR] = "#6366F1" // Purple
        purpleConnectionDotStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple
        purpleConnectionDotStyle[mxConstants.STYLE_STROKEWIDTH] = "1"
        purpleConnectionDotStyle[mxConstants.STYLE_MOVABLE] = "0"
purpleConnectionDotStyle[mxConstants.STYLE_RESIZABLE] = "0"
purpleConnectionDotStyle[mxConstants.STYLE_EDITABLE] = "0"
purpleConnectionDotStyle[mxConstants.STYLE_FOLDABLE] = "0"
purpleConnectionDotStyle[mxConstants.STYLE_BENDABLE] = "0"
        stylesheet.putCellStyle("purpleConnectionDot", purpleConnectionDotStyle)
        
        // Green connection dot style (for green-bordered nodes)
        val greenConnectionDotStyle = HashMap<String, Any>()
        greenConnectionDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        greenConnectionDotStyle[mxConstants.STYLE_FILLCOLOR] = "#4ade80" // Green
        greenConnectionDotStyle[mxConstants.STYLE_STROKECOLOR] = "#4ade80" // Green
        greenConnectionDotStyle[mxConstants.STYLE_STROKEWIDTH] = "1"
        stylesheet.putCellStyle("greenConnectionDot", greenConnectionDotStyle)
        
        // Longer edge style
        val edgeStyle = HashMap<String, Any>()
        edgeStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple edge
        edgeStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        edgeStyle[mxConstants.STYLE_ENDARROW] = mxConstants.ARROW_CLASSIC
        edgeStyle[mxConstants.STYLE_STARTARROW] = mxConstants.NONE
        edgeStyle[mxConstants.STYLE_EDGE] = mxConstants.EDGESTYLE_ORTHOGONAL
        edgeStyle[mxConstants.STYLE_DASHED] = "1" // Dashed line
        edgeStyle[mxConstants.STYLE_DASH_PATTERN] = "8 4" // 8px dash, 4px space
        
        // Improve orthogonal routing to make edges longer
        // We'll use other properties instead since STYLE_ORTHOGONAL_LOOP doesn't exist
        edgeStyle[mxConstants.STYLE_ROUTING_CENTER_X] = 1.0
        edgeStyle[mxConstants.STYLE_ROUNDED] = true
        edgeStyle[mxConstants.STYLE_ARCSIZE] = 20.0 // Corner radius for rounded edges
        
        stylesheet.putCellStyle("connectionEdge", edgeStyle)
        val portStyle = HashMap<String, Any>()
portStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
portStyle[mxConstants.STYLE_FILLCOLOR] = "#6366F1"
portStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1"
portStyle[mxConstants.STYLE_STROKEWIDTH] = "1"
portStyle[mxConstants.STYLE_MOVABLE] = "0"
portStyle[mxConstants.STYLE_RESIZABLE] = "0"
portStyle[mxConstants.STYLE_EDITABLE] = "0"
stylesheet.putCellStyle("portStyle", portStyle)
        
        // Green edge style (for connections from green nodes)
        val greenEdgeStyle = HashMap<String, Any>(edgeStyle)
        greenEdgeStyle[mxConstants.STYLE_STROKECOLOR] = "#4ade80" // Green edge
        stylesheet.putCellStyle("greenConnectionEdge", greenEdgeStyle)
    }
    
    /**
     * Set up mouse handlers for connection dot interaction
     */
    private fun setupMouseHandlers() {
        // Mouse handler for connection dots
        graphComponent.graphControl.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val cell = graphComponent.getCellAt(e.x, e.y)
                
                // Check if clicked on a connection dot
            
            }
            
            override fun mouseReleased(e: MouseEvent) {
                if (activeConnectionDot != null) {
                    // Check if released on a valid target
                    val targetCell = graphComponent.getCellAt(e.x, e.y)
                    
                    if (targetCell != null && isValidConnectionTarget(targetCell)) {
                        // Create actual connection
                        createConnection(activeConnectionDot!!, targetCell as mxCell)
                    }
                    
                    // Clean up temporary preview
                    activeConnectionDot = null
                }
            }
        })
        
        // Mouse motion handler for updating connection preview
      
    }
    
    /**
     * Configure connection options for the graph
     */
    private fun configureConnectionOptions() {
        // Set basic connection properties
        graphComponent.connectionHandler.isEnabled = true
        
        // Configure the connection points
        graph.isAllowDanglingEdges = false
        graph.isPortsEnabled = true
        
        // // Add connection handler to update after connection is made
        // graphComponent.connectionHandler.addListener(mxEvent.CONNECT, object : mxIEventListener {
        //     override fun invoke(sender: Any, evt: mxEventObject) {
        //         // Update the connection dots when a connection is made
        //         updateConnectionDots()
        //     }
        // })
    }
    
    /**
     * Update the positions of all connection dots
     */
    fun updateConnectionDots() {
        // Remove existing connection dots
        if (connectionDots.isNotEmpty()) {
            graph.removeCells(connectionDots.toTypedArray())
            connectionDots.clear()
        }
        
        // Create new connection dots for all nodes
        val allNodes = graph.getChildVertices(graph.defaultParent).filterIsInstance<mxCell>().filter { 
            !isAddButton(it) && !connectionDots.contains(it)
        }
        
        allNodes.forEach { node ->
            createConnectionDot(node)
        }
    }
    
    /**
     * Create a connection dot for a node
     */
    fun createConnectionDot(node: mxCell) {
        // Chỉ tạo port là child vertex của node, không tạo dot ngoài node
        val portGeometry = mxGeometry(1.0, 0.5, 16.0, 16.0)
        portGeometry.setRelative(true)
        portGeometry.offset = mxPoint(-8.0, -8.0)
        val port = mxCell(null, portGeometry, "portStyle")
        port.setVertex(true)
        port.setConnectable(true)
        node.insert(port)
        connectionDots.add(port)
    }
    
    /**
     * Start drawing a connection preview from a connection dot
     */
  
    
    /**
     * Update the connection preview as mouse moves
     */
 
    
    /**
     * Clean up temporary connection preview
     */

    
    /**
     * Create an actual connection between nodes
     */
    private fun createConnection(sourceDot: mxCell, targetCell: mxCell) {
        val sourceNode = sourceDot.parent as? mxCell
        if (sourceNode != null) {
            graph.model.beginUpdate()
            try {
                val isGreenNode = sourceNode.style?.contains("greenNode") == true
                val edgeStyle = if (isGreenNode) "greenConnectionEdge" else "connectionEdge"
                graph.insertEdge(
                    graph.defaultParent, null, "",
                    sourceNode, targetCell, edgeStyle
                )
            } finally {
                graph.model.endUpdate()
            }
        }
    }
    
    /**
     * Check if a cell can be a valid connection target
     */
    private fun isValidConnectionTarget(cell: Any): Boolean {
        // Chỉ cho phép target là node chính, không phải port, không phải dot chức năng
        if (!graph.model.isVertex(cell)) return false
        if (connectionDots.contains(cell)) return false
        // Có thể thêm: !dotManager.isEditDot(cell as mxCell) && !dotManager.isCopyDot(cell as mxCell)
        val sourceNode = (activeConnectionDot?.parent as? mxCell)
        return cell != sourceNode
    }
    /**
     * Find the parent node for a connection dot
     */
    private fun findParentNodeForDot(dotCell: mxCell): mxCell? {
        // Get all vertices in the graph
        val vertices = graph.getChildVertices(graph.defaultParent)
        
        // Find the node that owns this dot (closest to the dot)
        return vertices.filterIsInstance<mxCell>()
            .filter { it != dotCell && !connectionDots.contains(it) }
            .minByOrNull { 
                val dotGeom = graph.getCellGeometry(dotCell)
                val cellGeom = graph.getCellGeometry(it)
                
                // Calculate distance between dot and potential parent cell
                val dx = dotGeom.x - (cellGeom.x + cellGeom.width - 10)
                val dy = dotGeom.y - (cellGeom.y + cellGeom.height/2 - 10)
                
                // Return Euclidean distance squared (cheaper than actual distance)
                dx * dx + dy * dy
            }
    }
    
    /**
     * Check if a cell is the add button
     */
    private fun isAddButton(cell: mxCell): Boolean {
        val style = graph.getCellStyle(cell)
        return style.getOrDefault(mxConstants.STYLE_SHAPE, "") == "addButton" || 
               (cell.value == "+" && cell.style?.contains("addButton") == true)
    }
} 