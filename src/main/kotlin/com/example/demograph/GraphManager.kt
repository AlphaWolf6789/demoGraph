package com.example.demograph

import com.mxgraph.io.mxCodec
import com.mxgraph.model.mxCell
import com.mxgraph.shape.mxIShape
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.swing.handler.mxPanningHandler
import com.mxgraph.swing.handler.mxRubberband
import com.mxgraph.util.mxConstants
import com.mxgraph.util.mxXmlUtils
import com.mxgraph.view.mxGraph
import com.mxgraph.view.mxStylesheet
import com.mxgraph.canvas.mxGraphics2DCanvas
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import javax.swing.*

/**
 * Main class for managing the graph component and its interactions
 */
class GraphManager {
    private val graph = mxGraph()
    val graphComponent = mxGraphComponent(graph)
    val parent = graph.defaultParent
    private var nodeCount = 0
    private lateinit var addNodeButton: AddNodeButton
    
    // Helper classes
    private val nodeFactory: NodeFactory
    private val dotManager: DotManager
    
    // Constants for node dimensions
    companion object {
        const val NODE_WIDTH = 200.0
        const val NODE_HEIGHT = 100.0
    }
    
    init {
        // Initialize helper classes
        nodeFactory = NodeFactory(graph)
        dotManager = DotManager(graph)
        
        // Register custom shapes first
        registerCustomShapes()
        
        // Configure mxGraph
        configureGraph()
        
        // Configure graphComponent
        configureGraphComponent()
        
        // Set up panning and selection
        setupInteractions()
        
        // Add resize listener to adjust overlay components
        graphComponent.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                adjustOverlayComponentSizes()
            }
        })
        
        // Create initial root node
        createRootNode()
    }
    
    private fun registerCustomShapes() {
        // Register node with dot shape
        val nodeWithDotShapeClass = NodeWithDotShape::class.java
        mxGraphics2DCanvas.putShape(CustomGraphRenderer.SHAPE_NODEWITH_DOT, nodeWithDotShapeClass.newInstance() as mxIShape)
        
        // Apply custom styling
        CustomGraphRenderer.register(graph)
        
        // Configure add button style
        CircularAddButton.configureStyle(graph)
    }
    
    private fun configureGraph() {
        graph.isHtmlLabels = true
        graph.isCellsMovable = true
        graph.isCellsSelectable = true
        graph.isCellsEditable = true
        graph.isAutoSizeCells = true

        // Configure global stylesheet
        val stylesheet = graph.stylesheet
        
        // Edge style for dashed lines connecting to add buttons
        val dashedEdgeStyle = HashMap<String, Any>()
        dashedEdgeStyle[mxConstants.STYLE_DASHED] = true
        dashedEdgeStyle[mxConstants.STYLE_DASH_PATTERN] = "3 3"
        dashedEdgeStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple
        dashedEdgeStyle[mxConstants.STYLE_STROKEWIDTH] = "1"
        stylesheet.putCellStyle("dashedEdge", dashedEdgeStyle)
        
        // Edge style for connections between nodes
        val nodeConnectionStyle = HashMap<String, Any>()
        nodeConnectionStyle[mxConstants.STYLE_STROKECOLOR] = "#4ade80" // Green
        nodeConnectionStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        nodeConnectionStyle[mxConstants.STYLE_ENDARROW] = mxConstants.ARROW_CLASSIC
        nodeConnectionStyle[mxConstants.STYLE_EDGE] = mxConstants.EDGESTYLE_ORTHOGONAL
        stylesheet.putCellStyle("nodeConnection", nodeConnectionStyle)
        
        // Style for nodes with green border (step nodes)
        val greenNodeStyle = HashMap<String, Any>()
        greenNodeStyle[mxConstants.STYLE_FILLCOLOR] = "#1e1e1e" // Dark background
        greenNodeStyle[mxConstants.STYLE_STROKECOLOR] = "#4ade80" // Green border
        greenNodeStyle[mxConstants.STYLE_STROKEWIDTH] = 2.0
        greenNodeStyle[mxConstants.STYLE_ROUNDED] = true
        greenNodeStyle[mxConstants.STYLE_ARCSIZE] = 6.0 // Rounded corner amount
        greenNodeStyle[mxConstants.STYLE_SHADOW] = false
        stylesheet.putCellStyle("greenNode", greenNodeStyle)
    }
    
    private fun configureGraphComponent() {
        // Set dark background color
        graphComponent.background = Color(30, 30, 30) // Dark background
        
        // Configure graphComponent
        graphComponent.isPanning = true
        graphComponent.isAutoScroll = true
        graphComponent.isEnabled = true
        graphComponent.isEventsEnabled = true
        graphComponent.isDragEnabled = false
        
        // Set component size
        graphComponent.preferredSize = Dimension(800, 600)
        
        // Set grid properties
        graphComponent.isGridVisible = true
        graphComponent.gridColor = Color(45, 45, 45) // Subtle grid
        
        // Remove the focus border
        graphComponent.isFocusable = false
    }
    
    private fun setupInteractions() {
        // Set up panning handler
        val panningHandler = mxPanningHandler(graphComponent)
        panningHandler.isEnabled = true
        
        // Enable rubber band selection
        mxRubberband(graphComponent)
        
        // Add the "+" button overlay
        addNodeButton = AddNodeButton(graphComponent, this)
        
        // Setup hover and click handlers
        setupGraphListeners()
    }
    
    // Setup mouse listeners for hover and click events
    private fun setupGraphListeners() {
        // Add mouse motion listener for hover detection
        graphComponent.graphControl.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
            override fun mouseMoved(e: java.awt.event.MouseEvent) {
                val cell = graphComponent.getCellAt(e.x, e.y)
                dotManager.handleNodeHover(cell, e.point)
            }
        })
        
        // Add mouse listener for click detection
        graphComponent.graphControl.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                val cell = graphComponent.getCellAt(e.x, e.y)
                
                if (cell != null) {
                    // Check if the clicked cell is an add button
                    if (isAddButton(cell as mxCell)) {
                        // Show the add node dialog when add button is clicked
                        showAddNodeDialog(cell)
                        return
                    }
                    
                    // Check if the clicked cell is an edit dot
                    if (dotManager.isEditDot(cell as mxCell)) {
                        // Find the parent node of this dot
                        val parentCell = dotManager.findParentNodeForDot(cell)
                        if (parentCell != null) {
                            NodeEditDialog.showEditDialog(graphComponent, parentCell, graph)
                        }
                    }
                    // Check if clicked cell is a copy dot
                    else if (dotManager.isCopyDot(cell as mxCell)) {
                        // Find the parent node of this dot
                        val parentCell = dotManager.findParentNodeForDot(cell)
                        if (parentCell != null) {
                            nodeFactory.copyNode(parentCell)
                            nodeCount++
                        }
                    }
                }
            }
            
            override fun mouseExited(e: java.awt.event.MouseEvent) {
                // Clear dots when mouse exits the component
                dotManager.clearDragDots()
                dotManager.setCurrentHoverCell(null)
            }
            
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                // Handle double clicks
                if (e.clickCount == 2) {
                    val cell = graphComponent.getCellAt(e.x, e.y)
                    // If it's a node (not a dot, edge, or add button)
                    if (cell != null && graph.model.isVertex(cell) && 
                        !dotManager.getDotCells().contains(cell) && !isAddButton(cell as mxCell)) {
                        NodeEditDialog.showEditDialog(graphComponent, cell as mxCell, graph)
                    }
                }
            }
        })
    }
    
    // Show the add node dialog
    private fun showAddNodeDialog(addButtonCell: mxCell) {
        AddNodeDialog.showAddNodeDialog(graphComponent, addButtonCell) { sourceCell, nodeName, contentText ->
            createNewNode(sourceCell, nodeName, contentText)
        }
    }
    
    // Create a new node from the add dialog
    private fun createNewNode(sourceCell: mxCell, nodeName: String, contentText: String) {
        graph.model.beginUpdate()
        try {
            // Find the parent node that the + button is connected to
            val parentNode = findParentNodeForAddButton(sourceCell)
            
            // Get the current position of the + button
            val buttonGeometry = graph.getCellGeometry(sourceCell)
            
            // Position for the new node - align with parent node's Y position
            val parentGeometry = parentNode?.let { graph.getCellGeometry(it) }
            val x = buttonGeometry.x
            val y = parentGeometry?.y ?: buttonGeometry.y // Keep same Y as parent node
            
            // Create the node content
            val content = if (contentText.isEmpty()) {
                NodeEditDialog.createNodeContentFromText(nodeName)
            } else {
                NodeEditDialog.createStepNodeContent(nodeName, contentText)
            }
            
            // Create the node with green style
            val newNode = graph.insertVertex(
                parent,
                null,
                content,
                x,
                y,
                NODE_WIDTH,
                NODE_HEIGHT,
                "greenNode"
            ) as mxCell
            
            // Connect the parent node to the new node
            if (parentNode != null && parentNode != sourceCell) {
                // Remove existing edge from parent to add button
                val edges = graph.getEdges(sourceCell)
                for (edge in edges) {
                    val edgeCell = edge as mxCell
                    if (edgeCell.source == parentNode && edgeCell.target == sourceCell) {
                        graph.removeCells(arrayOf(edgeCell))
                        break
                    }
                }
                
                // Add new edge from parent to new node
                graph.insertEdge(parent, null, "", parentNode, newNode, "nodeConnection")
            }
            
            // Create new add button connected to the new node
            nodeFactory.addAddButtonToNode(newNode)
            
            // Remove the old + button
            graph.removeCells(arrayOf(sourceCell))
            
            // Increment node count
            nodeCount++
            
        } finally {
            graph.model.endUpdate()
        }
    }
    
    // Find the parent node that an add button is connected to
    private fun findParentNodeForAddButton(addButton: mxCell): mxCell? {
        val edges = graph.getEdges(addButton)
        
        // Find the incoming edge to the add button
        for (edge in edges) {
            val edgeCell = edge as mxCell
            if (edgeCell.target == addButton) {
                return edgeCell.source as mxCell
            }
        }
        
        return null
    }
    
    private fun adjustOverlayComponentSizes() {
        // Resize any overlay components when the graph component is resized
        addNodeButton.setBounds(0, 0, graphComponent.width, graphComponent.height)
    }
    
    private fun createRootNode() {
        graph.model.beginUpdate()
        try {
            // Create root node using the factory
            val vertex = nodeFactory.createRootNode(parent)
            
            // Add circular add button to the right of the root node
            nodeFactory.addAddButtonToNode(vertex)
            
            // Increment node count
            nodeCount++
        } finally {
            graph.model.endUpdate()
        }
    }
    
    fun createToolbar(): JToolBar {
        val toolbar = JToolBar()
        toolbar.add(createButton("Delete Selected") { deleteSelected() })
        toolbar.add(createButton("Save") { saveGraph() })
        toolbar.add(createButton("Load") { loadGraph() })
        toolbar.add(createButton("Zoom In") { graphComponent.zoomIn() })
        toolbar.add(createButton("Zoom Out") { graphComponent.zoomOut() })
        
        // Note: Add Node button is removed as requested
        
        return toolbar
    }
    
    private fun createButton(text: String, action: () -> Unit): JButton {
        return JButton(object : AbstractAction(text) {
            override fun actionPerformed(e: ActionEvent?) {
                try {
                    action()
                } catch (ex: Exception) {
                    JOptionPane.showMessageDialog(null, "Error: ${ex.message}", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        })
    }
    
    fun addNode(x: Double, y: Double, previousStepId: String?, nextStepId: String? = null) {
        val node = nodeFactory.addNode(x, y, previousStepId, nextStepId, parent)
        nodeFactory.addAddButtonToNode(node)
        nodeCount++
    }
    
    private fun deleteSelected() {
        nodeFactory.deleteSelected(graph.selectionCells)
    }
    
    private fun saveGraph() {
        try {
            val encoder = mxCodec()
            val node = encoder.encode(graph.model)
            val xml = mxXmlUtils.getXml(node)
            val file = File("graph.xml")
            file.writeText(xml)
            JOptionPane.showMessageDialog(null, "Graph saved to ${file.absolutePath}")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to save graph: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
    
    private fun loadGraph() {
        try {
            val file = File("graph.xml")
            if (file.exists()) {
                val xml = file.readText()
                val document = mxXmlUtils.parseXml(xml)
                val decoder = mxCodec(document)
                decoder.decode(document.documentElement, graph.model)
                graph.refresh()
                
                // Reset nodeCount based on the number of existing nodes
                nodeCount = graph.model.getChildCount(parent)
            } else {
                JOptionPane.showMessageDialog(null, "graph.xml not found")
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to load graph: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
    
    // Get all node IDs for dropdown lists
    fun getAllNodeIds(): List<String> {
        val result = mutableListOf<String>()
        val cells = graph.getChildCells(parent)
        
        for (cell in cells) {
            val cellValue = graph.model.getValue(cell)
            if (cellValue is NodeData) {
                result.add(cellValue.id)
            }
        }
        
        return result
    }
    
    // Helper method to check if a cell is the add button
    fun isAddButton(cell: mxCell): Boolean {
        val style = graph.getCellStyle(cell)
        return style.getOrDefault(mxConstants.STYLE_SHAPE, "") == "addButton" || 
               (cell.value == "+" && cell.style?.contains("addButton") == true)
    }
} 