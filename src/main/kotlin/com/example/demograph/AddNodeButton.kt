package com.example.demograph

import com.mxgraph.model.mxCell
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxEvent
import com.mxgraph.util.mxEventObject
import com.mxgraph.model.mxGeometry
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.JOptionPane

// Actual implementation of the AddNodeButton class declared in GraphManager
class AddNodeButton(
    private val graphComponent: mxGraphComponent,
    private val graphManager: GraphManager
) : JPanel() {
    
    private val buttonSize = 30
    private val plusIcon = createPlusIcon()
    private var mouseX = 0
    private var mouseY = 0
    
    init {
        isOpaque = false
        layout = null
        
        // Set size and position (will be updated on graph movement)
        setBounds(0, 0, graphComponent.width, graphComponent.height)
        
        // Add mouse listener for the "+" button
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                // Find clicked add button cells
                val clickedCell = findClickedAddButton(e.point)
                if (clickedCell != null) {
                    showAddNodeDialog(clickedCell)
                }
            }
        })
        
        // Add the overlay panel to the graph component
        graphComponent.add(this)
        
        // Listen for graph changes to update button positions
        graphComponent.graph.addListener(mxEvent.CELLS_ADDED) { _: Any, evt: mxEventObject ->
            repaint()
        }
        
        // Listen for mouse movements to track cursor position
        graphComponent.graphControl.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                mouseX = e.x
                mouseY = e.y
                repaint()
            }
        })
    }
    
    private fun createPlusIcon(): Image {
        // Create plus icon - this is a simple implementation
        // A better approach would be to load an actual icon resource
        val img = BufferedImage(buttonSize, buttonSize, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        
        // Draw purple circle - matching the image reference
        g.setColor(Color(99, 102, 241)) // #6366F1 purple
        g.fillOval(0, 0, buttonSize, buttonSize)
        
        // Draw plus sign
        g.setColor(Color.WHITE)
        val margin = buttonSize / 3
        g.drawLine(margin, buttonSize / 2, buttonSize - margin, buttonSize / 2)
        g.drawLine(buttonSize / 2, margin, buttonSize / 2, buttonSize - margin)
        
        // Use anti-aliasing for smoother lines
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        g.dispose()
        return img
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        // Get all cells in the graph
        val cells = graphComponent.graph.getChildVertices(graphComponent.graph.defaultParent)
        
        // Draw plus buttons beside cells (similar to the reference image)
        if (cells.isNotEmpty()) {
            for (cell in cells) {
                val cellGeometry = graphComponent.graph.getCellGeometry(cell)
                if (cellGeometry != null) {
                    // Calculate button position (right side of cell with dashed line)
                    val point = getPointForCell(cell, cellGeometry)
                    val x = point.x.toInt() + 40 // Offset to the right for better spacing
                    val y = point.y.toInt()
                    
                    // Draw dashed line connecting node to plus button (as in the reference image)
                    drawDashedLine(g2d, point.x.toInt(), point.y.toInt(), x, y)
                    
                    // Draw plus button
                    drawPlusButton(g2d, x, y)
                }
            }
        } else {
            // If no cells exist, draw a default button in the center
            val x = graphComponent.width / 2
            val y = graphComponent.height / 2
            drawPlusButton(g2d, x, y)
        }
    }
    
    private fun drawDashedLine(g: Graphics2D, x1: Int, y1: Int, x2: Int, y2: Int) {
        // Save the original stroke
        val originalStroke = g.stroke
        
        // Create a dashed stroke pattern
        val dashed = BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(5f), 0f)
        g.stroke = dashed
        g.color = Color(128, 80, 255) // Match the purple color
        
        // Draw the dashed line
        g.drawLine(x1, y1, x2, y2)
        
        // Restore the original stroke
        g.stroke = originalStroke
    }
    
    // Custom method to get point for cell since getPointForAnchor is not available
    private fun getPointForCell(cell: Any, geometry: mxGeometry): Point {
        val state = graphComponent.graph.getView().getState(cell)
        return if (state != null) {
            val x = state.x + geometry.width
            val y = state.y + geometry.height / 2
            Point(x.toInt(), y.toInt())
        } else {
            // Fallback if state is not available
            Point((geometry.x + geometry.width).toInt(), (geometry.y + geometry.height / 2).toInt())
        }
    }
    
    private fun drawPlusButton(g: Graphics2D, x: Int, y: Int) {
        g.drawImage(plusIcon, x - buttonSize / 2, y - buttonSize / 2, null)
    }
    
    private fun findClickedAddButton(point: Point): mxCell? {
        val cells = graphComponent.graph.getChildVertices(graphComponent.graph.defaultParent).filterIsInstance<mxCell>()
        
        // Filter to only get add button cells
        val addButtons = cells.filter { cell -> 
            graphManager.isAddButton(cell)
        }
        
        // Check if the click is within any add button area
        for (button in addButtons) {
            val state = graphComponent.graph.getView().getState(button)
            if (state != null) {
                val cellBounds = state.rectangle
                if (cellBounds.contains(point)) {
                    return button
                }
            }
        }
        
        return null
    }
    
    private fun showAddNodeDialog(addButtonCell: mxCell) {
        // Find the source cell (node that has a connection to this add button)
        val sourceCell = getSourceNodeForAddButton(addButtonCell)
        
        if (sourceCell != null) {
            // Show dialog to get input
            val result = JOptionPane.showInputDialog(graphComponent, "Enter step name:", "Add New Step", JOptionPane.PLAIN_MESSAGE)
            
            if (result != null && result.isNotEmpty()) {
                // Create new node
                createNewNodeFromAddButton(addButtonCell, result)
            }
        }
    }
    
    /**
     * Find the node that is connected to the add button
     */
    private fun getSourceNodeForAddButton(addButtonCell: mxCell): mxCell? {
        val edges = graphComponent.graph.getEdges(addButtonCell)
        for (edge in edges) {
            val source = graphComponent.graph.getModel().getTerminal(edge, true) as? mxCell
            if (source != null && !graphManager.isAddButton(source)) {
                return source
            }
        }
        return null
    }
    
    /**
     * Create a new node from an add button click
     */
    private fun createNewNodeFromAddButton(addButtonCell: mxCell, nodeName: String) {
        val graph = graphComponent.graph
        graph.model.beginUpdate()
        try {
            // Get add button position and geometry
            val buttonGeo = graph.getCellGeometry(addButtonCell)
            
            // Calculate position for the new node
            val x = buttonGeo.x + 60
            val y = buttonGeo.y - 30
            
            // Get the source node that this add button is connected to
            val sourceCell = getSourceNodeForAddButton(addButtonCell)
            
            if (sourceCell != null) {
                // Create new node and connect to source
                val nodeId = sourceCell.id + "_child"
                graphManager.addNode(x, y, sourceCell.id)
            }
        } finally {
            graph.model.endUpdate()
        }
    }
} 