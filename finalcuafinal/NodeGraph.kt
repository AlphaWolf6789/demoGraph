package com.example.finalcuafinal

import com.mxgraph.model.mxCell
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxConstants
import com.mxgraph.util.mxEvent
import com.mxgraph.util.mxEventObject
import com.mxgraph.util.mxEventSource.mxIEventListener
import com.mxgraph.view.mxGraph
import com.mxgraph.view.mxStylesheet
import com.mxgraph.canvas.mxGraphics2DCanvas
import com.mxgraph.model.mxGeometry
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Point
import java.awt.Rectangle
import java.util.HashMap
import javax.swing.JPanel
import javax.swing.BorderFactory
import javax.swing.JPopupMenu
import javax.swing.JMenuItem
import javax.swing.border.EmptyBorder
import javax.swing.UIManager
import javax.swing.border.LineBorder
import javax.swing.JOptionPane

class NodeGraph : JPanel() {
    private val graph = CustomMxGraph()
    private val graphComponent: mxGraphComponent
    private val parent = graph.defaultParent
    private var currentHoverCell: Any? = null
    private var hoveredEdge: mxCell? = null
    private var dotCells = mutableListOf<mxCell>()
    private var connectionDots = mutableListOf<mxCell>()
    private var isUpdatingLayout = false // Flag to prevent layout update recursion
    
    // Layout constants
    companion object {
        const val SHAPE_NODEWITH_DOT = "nodeWithDot"
        const val SHAPE_STEP_NODE = "stepNodeShape"
        
        // Layout positions
        const val MAIN_AREA_Y = 100.0 // Y position for main workflow
        const val ORPHAN_AREA_Y = 400.0 // Y position for orphaned nodes
        const val HORIZONTAL_SPACING = 70.0 // Space between node and add button
        const val VERTICAL_SPACING = 40.0 // Vertical spacing between nodes in the same column
        const val NODE_WIDTH = 180.0
        const val NODE_HEIGHT = 90.0
    }
    
    init {
        layout = null
        background = Color(30, 30, 30) // Dark background like in the example
        border = EmptyBorder(0, 0, 0, 0)
        
        // Đăng ký custom shape
        registerCustomShapes()
        
        // Configure graph appearance
        configureGraphStyles()
        
        // Register custom renderers and styles
        CustomGraphRenderer.register(graph)
        
        // Configure add button style
        CircularAddButton.configureStyle(graph)
        
        // Create the graph component
        graphComponent = mxGraphComponent(graph)
        graphComponent.setConnectable(true)
        graphComponent.setAntiAlias(true)
        graphComponent.setTextAntiAlias(true)
        graphComponent.background = Color(30, 30, 30)
        
        // Configure graph component display
        graphComponent.isGridVisible = true
        graphComponent.gridColor = Color(45, 45, 45) // Subtle grid
        
        // Disable selection highlighting
        graphComponent.isFocusable = false
        graph.isCellsSelectable = true
        
        // Make the graph component resizable with parent container
        graphComponent.preferredSize = Dimension(800, 600)
        
        // Remove graph component border
        graphComponent.border = null
        graph.setCellsMovable(false)
        
        // Allow dragging connections from any part of a node
        configureConnectionOptions()
        
        // Set graph component to fill the entire panel
        graphComponent.setBounds(0, 0, width, height)
        
        // Add component to panel
        add(graphComponent)
        
        // Setup hover handling
        setupGraphListeners()
        
        // Add resize listener to adjust graph component size when panel is resized
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                graphComponent.setBounds(0, 0, width, height)
                graphComponent.repaint()
            }
        })
        
        // Setup graph model listener for edge changes
        setupModelChangeListeners()
    }
    
    // Add model listener to detect edge changes
    private fun setupModelChangeListeners() {
        graph.model.addListener(mxEvent.CHANGE, mxIEventListener { sender, evt ->
            // Prevent recursive updates
            if (!isUpdatingLayout) {
                // Use a small delay to batch multiple changes
                javax.swing.SwingUtilities.invokeLater {
                    updateLayout()
                }
            }
        })
    }

    private fun isRootWorkflowNode(node: mxCell): Boolean {
        val geom = graph.getCellGeometry(node)
        // SỬA lại y = 100.0 thay vì 150.0
        return geom.x == 50.0 && geom.y == 400.0
    }
    
    // Update layout of all nodes based on their connections
    private fun updateLayout() {
        // Prevent recursive updates
        if (isUpdatingLayout) return
        isUpdatingLayout = true
        
        try {
            // Get all nodes except add buttons, dots, etc.
            val allNodes = graph.getChildVertices(parent).filterIsInstance<mxCell>().filter { 
                !isAddButton(it) && !dotCells.contains(it) && !connectionDots.contains(it)
            }
            
            graph.model.beginUpdate()
            try {
                // Identify TRUE orphan nodes: no incoming edges AND no outgoing edges to real nodes
                val orphanNodes = allNodes.filter { node ->
                    val incoming = graph.getIncomingEdges(node)?.filter { edge ->
                        val src = graph.getModel().getTerminal(edge, true)
                        val tgt = graph.getModel().getTerminal(edge, false)
                        tgt != null && !isAddButton(src)
                    } ?: emptyList()
                    // Đừng coi node đầu tiên là orphan nếu nó là gốc workflow
                    incoming.isEmpty() && !isRootWorkflowNode(node)
                }.toMutableList()
                
                // Find root nodes (no incoming edges but with real outgoing edges)
                val rootNodes = allNodes.filter { node ->
                    // Has no incoming edges
                    (graph.getIncomingEdges(node)?.isEmpty() ?: true) &&
                    // Has at least one outgoing edge to a non-add-button node
                    (graph.getOutgoingEdges(node)?.any { edge ->
                        val tgt = graph.getModel().getTerminal(edge, false) as? mxCell
                        tgt != null && !isAddButton(tgt)
                    } ?: false)
                }
                
                // Remove root nodes from orphan nodes list
                orphanNodes.removeAll(rootNodes)
                
                // Organize orphan nodes in the orphan area
                organizeOrphanNodes(orphanNodes)
                
                // Process each root node and its descendants
                rootNodes.forEach { rootNode ->
                    val geom = graph.getCellGeometry(rootNode)
                    organizeNodeHierarchy(rootNode, geom.y, geom.x)
                }
                
                // Update add button positions for all nodes
                updateAddButtonPositions()
                
                // Update connection dots
                updateConnectionDots()
                
            } finally {
                graph.model.endUpdate()
            }
        } finally {
            isUpdatingLayout = false
            graphComponent.repaint()
        }
    }
    
    // Place orphan nodes in the orphaned area
    private fun organizeOrphanNodes(orphanNodes: List<mxCell>) {
        if (orphanNodes.isEmpty()) return
    
        // Lấy Y lớn nhất của các node workflow để orphan bắt đầu từ dưới cùng (hoặc dùng ORPHAN_AREA_Y nếu muốn cố định)
        val allNodes = graph.getChildVertices(parent).filterIsInstance<mxCell>()
            .filter { !isAddButton(it) && !dotCells.contains(it) && !connectionDots.contains(it) && !orphanNodes.contains(it) }
        val maxY = allNodes.map { (graph.getCellGeometry(it)?.y ?: 0.0) + (graph.getCellGeometry(it)?.height ?: 0.0) }
            .maxOrNull() ?: ORPHAN_AREA_Y
    
        val orphanY = maxY + 100.0 // hoặc ORPHAN_AREA_Y nếu muốn cố định
        var currentX = 50.0 // orphan đầu tiên luôn ở X = 50
    
        orphanNodes.forEach { node ->
            val geometry = graph.getCellGeometry(node).clone() as mxGeometry
            geometry.x = currentX
            geometry.y = orphanY
            graph.model.setGeometry(node, geometry)
            updateAddButtonForNode(node)
            currentX += NODE_WIDTH + HORIZONTAL_SPACING + 30.0 // Tăng X cho node tiếp theo
        }
    }
    
    
    // Organize a node hierarchy (node and all its descendants)
    private fun organizeNodeHierarchy(node: mxCell, yPosition: Double, xPosition: Double): Double {
        // Set this node's position
        val nodeGeometry = graph.getCellGeometry(node).clone() as mxGeometry
        nodeGeometry.x = xPosition
        nodeGeometry.y = yPosition
        graph.model.setGeometry(node, nodeGeometry)
        
        // Get all children except add buttons
        val outgoingEdges = graph.getOutgoingEdges(node)
        val childNodes = outgoingEdges?.mapNotNull { edge ->
            val target = graph.getModel().getTerminal(edge, false) as? mxCell
            if (target != null && !isAddButton(target)) target else null
        } ?: emptyList()
        
        // If no children, return the next Y position
        if (childNodes.isEmpty()) {
            return yPosition + NODE_HEIGHT + VERTICAL_SPACING
        }
        
        val childX = xPosition + NODE_WIDTH + HORIZONTAL_SPACING + 30.0
        val baseY = yPosition
        val offset = NODE_HEIGHT + VERTICAL_SPACING
        
        // Process children in a zigzag pattern
        childNodes.forEachIndexed { idx, childNode ->
            val step = (idx + 1) / 2
            val direction = if (idx % 2 == 0) 1 else -1
            val childY = baseY + direction * step * offset
            organizeNodeHierarchy(childNode, childY, childX)
        }
        
        // Calculate max space needed vertically, accounting for nodes above and below parent
        val maxNodeCount = childNodes.size / 2 + 1
        return yPosition + NODE_HEIGHT + VERTICAL_SPACING * maxNodeCount
    }
    
    // Update positions of all add buttons
    private fun updateAddButtonPositions() {
        val allNodes = graph.getChildVertices(parent).filterIsInstance<mxCell>()
        
        allNodes.forEach { node ->
            if (!isAddButton(node) && !dotCells.contains(node) && !connectionDots.contains(node)) {
                updateAddButtonForNode(node)
            }
        }
    }
    
    // Update position of an add button associated with a node
    private fun updateAddButtonForNode(node: mxCell) {
        val outgoingEdges = graph.getOutgoingEdges(node)
        outgoingEdges?.forEach { edge ->
            val target = graph.getModel().getTerminal(edge, false) as? mxCell
            if (target != null && isAddButton(target)) {
                // Update add button position
                val nodeGeometry = graph.getCellGeometry(node)
                val buttonGeometry = graph.getCellGeometry(target).clone() as mxGeometry
                buttonGeometry.x = nodeGeometry.x + nodeGeometry.width + HORIZONTAL_SPACING
                
                // Center the add button vertically with the node
                buttonGeometry.y = nodeGeometry.y + (nodeGeometry.height/2) - (buttonGeometry.height/2)
                
                graph.model.setGeometry(target, buttonGeometry)
            }
        }
    }
    
    private fun setupGraphListeners() {
        // Hover handler for nodes and edges
        graphComponent.graphControl.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val cell = graphComponent.getCellAt(e.x, e.y)
                
                // Handle edge highlighting
                if (cell != null && graph.model.isEdge(cell)) {
                    // Only update if it's a different edge
                    if (hoveredEdge != cell) {
                        resetHighlightedEdge()
                        val edgeCell = cell as mxCell
                        hoveredEdge = edgeCell
                        if (canDeleteEdge(edgeCell)) {
                            highlightEdge(edgeCell)
                        }
                    }
                } else if (hoveredEdge != null) {
                    // Mouse moved away from any edge
                    resetHighlightedEdge()
                }
                
                // Node hover handling with improved consistency
                handleNodeHover(cell, e)
            }
        })
        
        // Click handler for the graph
        graphComponent.graphControl.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val cell = graphComponent.getCellAt(e.x, e.y)
                
                // Handle right-click for context menu
                if (e.isPopupTrigger || e.button == MouseEvent.BUTTON3) {
                    handleRightClick(e, cell)
                    return
                }
                
                if (cell != null) {
                    // Check if the clicked cell is an add button
                    if (isAddButton(cell)) {
                        showAddStepDialog(cell as mxCell)
                        return
                    }
                    
                    // Check if the clicked cell is an edit dot
                    if (graph.getModel().getStyle(cell)?.contains("editDot") == true || 
                        graph.getModel().getStyle(cell)?.contains("editGreenDot") == true) {
                        // Find the parent node of this dot
                        val parentCell = findParentNodeForDot(cell)
                        if (parentCell != null) {
                            showEditDialog(parentCell)
                        }
                    }
                    // Check if the clicked cell is a delete dot
                    else if (graph.getModel().getStyle(cell)?.contains("deleteDot") == true || 
                             graph.getModel().getStyle(cell)?.contains("deleteGreenDot") == true) {
                        // Find the parent node of this dot
                        val parentCell = findParentNodeForDot(cell)
                        if (parentCell != null) {
                            // Future implementation for delete functionality
                            println("Delete functionality not yet implemented for node: ${graph.getModel().getValue(parentCell)}")
                        }
                    }
                }
            }
            
            override fun mouseReleased(e: MouseEvent) {
                // Check for popup trigger on release as well (for cross-platform support)
                if (e.isPopupTrigger || e.button == MouseEvent.BUTTON3) {
                    val cell = graphComponent.getCellAt(e.x, e.y)
                    handleRightClick(e, cell)
                }
            }
            
            override fun mouseClicked(e: MouseEvent) {
                // Handle double clicks
                if (e.clickCount == 2) {
                    val cell = graphComponent.getCellAt(e.x, e.y)
                    // If it's a node (not a dot, edge, or add button)
                    if (cell != null && graph.model.isVertex(cell) && 
                        !dotCells.contains(cell) && !isAddButton(cell)) {
                        showEditDialog(cell as mxCell)
                    }
                }
            }
            
            override fun mouseExited(e: MouseEvent) {
                // Clear dots when mouse exits the component
                clearDragDots()
                currentHoverCell = null
            }
        })
    }
    
    // Handle right-click context menu
    private fun handleRightClick(e: MouseEvent, cell: Any?) {
        if (cell != null && graph.model.isEdge(cell)) {
            val edge = cell as mxCell
            // Check if the edge can be deleted before highlighting
            if (canDeleteEdge(edge)) {
                // Only highlight the edge if it can be deleted
                highlightEdgeSpecial(edge)
                showEdgeContextMenu(e.point, edge)
            } else {
                // Just show the menu without highlighting if not deletable
                showEdgeContextMenu(e.point, edge)
            }
        }
    }
    
    // Determine if an edge can be deleted (target node has no real children)
    private fun canDeleteEdge(edge: mxCell): Boolean {
        // Get the target cell using the model instead of directly accessing edge.target
        val targetNode = graph.getModel().getTerminal(edge, false) // false means target (destination)
        if (targetNode != null) {
            // Don't allow deleting edges that connect to an add button
            if (targetNode is mxCell && isAddButton(targetNode)) {
                return false
            }
            
            val outgoingEdges = graph.getOutgoingEdges(targetNode)
            
            // Check if target has any outgoing edges that don't lead to add buttons
            return !(outgoingEdges?.any { outEdge -> 
                val targetCell = graph.getModel().getTerminal(outEdge, false) as? mxCell
                targetCell != null && !isAddButton(targetCell)
            } ?: false)
        }
        return false
    }
    
    // Show context menu for edges
    private fun showEdgeContextMenu(point: Point, edge: mxCell) {
        val popupMenu = JPopupMenu()
        popupMenu.border = LineBorder(Color(99, 102, 241), 1) // Purple border matching graph theme
        popupMenu.background = Color(45, 45, 45) // Dark background
        
        // Only add delete option if the edge can be deleted
        if (canDeleteEdge(edge)) {
            // Create delete menu item
            val deleteItem = JMenuItem("Delete Connection")
            deleteItem.foreground = Color.WHITE
            deleteItem.background = Color(45, 45, 45)
            deleteItem.addActionListener {
                // Delete immediately without confirmation
                deleteEdge(edge)
            }
            
            // Style the menu item when hovered
            deleteItem.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    deleteItem.background = Color(60, 60, 60)
                }
                
                override fun mouseExited(e: MouseEvent) {
                    deleteItem.background = Color(45, 45, 45)
                }
            })
            
            popupMenu.add(deleteItem)
        }
        
        // If menu has items, show it
        if (popupMenu.componentCount > 0) {
            // Make sure we reset the highlighting when popup closes
            popupMenu.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent) {}
                
                override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent) {
                    resetHighlightedEdge()
                }
                
                override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent) {
                    resetHighlightedEdge()
                }
            })
            
            popupMenu.show(graphComponent.graphControl, point.x, point.y)
        } else {
            // No menu items to show, just reset highlighting
            resetHighlightedEdge()
        }
    }
    
    // Special highlighting effect for right-clicked edges
    private fun highlightEdgeSpecial(edge: mxCell) {
        hoveredEdge = edge
        graph.model.beginUpdate()
        try {
            // Apply a special highlighted style directly to the edge
            val highlightStyle = "strokeColor=#8b5cf6;strokeWidth=4;dashed=1;dashPattern=8 4;" +
                                "endArrow=classic;startArrow=none;edgeStyle=orthogonalEdgeStyle;" +
                                "verticalAlign=middle;exitX=1.0;exitY=0.5;entryX=0.0;entryY=0.5"
            
            graph.model.setStyle(edge, highlightStyle)
        } finally {
            graph.model.endUpdate()
        }
        graphComponent.refresh()
    }
    
    // Delete an edge from the graph
    private fun deleteEdge(edge: mxCell) {
        // Store the source and target before removing the edge
        val sourceNode = graph.getModel().getTerminal(edge, true) as? mxCell
        val targetNode = graph.getModel().getTerminal(edge, false) as? mxCell
        
        graph.model.beginUpdate()
        try {
            // Remove the edge
            graph.removeCells(arrayOf(edge))
            
            // If the target was an add button with no other connections, remove it
            if (targetNode != null && isAddButton(targetNode)) {
                val incomingEdges = graph.getIncomingEdges(targetNode)
                if (incomingEdges == null || incomingEdges.isEmpty()) {
                    graph.removeCells(arrayOf(targetNode))
                }
            }
            
            // Determine if target node has become an orphan
            if (targetNode != null && !isAddButton(targetNode)) {
                val incomingEdges = graph.getIncomingEdges(targetNode)
                if (incomingEdges == null || incomingEdges.isEmpty()) {
                    // Target node has no more incoming edges - check if it's a true orphan
                    val hasRealOutgoingEdges = graph.getOutgoingEdges(targetNode)?.any { outEdge ->
                        val outTarget = graph.getModel().getTerminal(outEdge, false) as? mxCell
                        outTarget != null && !isAddButton(outTarget)
                    } ?: false
                    
                    if (!hasRealOutgoingEdges) {
                        // This is a true orphan - move it to the orphan area immediately
                        val geometry = graph.getCellGeometry(targetNode).clone() as mxGeometry
                        geometry.y = geometry.y + 200
                        graph.model.setGeometry(targetNode, geometry)
                    }
                }
            }
            
            // Update the layout after deleting the edge
            
            updateLayout()
    
        } finally {
            graph.model.endUpdate()
        }
    }

 
    private fun createDragDots(cell: mxCell) {
        graph.model.beginUpdate()
        try {
            val cellGeometry = graph.getCellGeometry(cell)
            val x = cellGeometry.x
            val y = cellGeometry.y + cellGeometry.height + 10 // Position dots below the node
            
            // Determine if this is a step node (with green border) or query node (with purple border)
            val isStepNode = graph.getCellStyle(cell).getOrDefault(mxConstants.STYLE_STROKECOLOR, "") == "#4ade80"
            
            // Create edit and copy dots
            createEditDot(cell, x, y, isStepNode)
            createCopyDot(cell, x, y, isStepNode)
        } finally {
            graph.model.endUpdate()
        }
    }
    
    // Create edit dot with a pen icon
    private fun createEditDot(cell: mxCell, x: Double, y: Double, isStepNode: Boolean) {
        val editDotStyle = if (isStepNode) "editGreenDot" else "editDot"
        val iconColor = if (isStepNode) "#4ade80" else "#6366F1"
        
        // Create HTML content with enhanced styling for the edit icon
        val editIconHtml = """
            <div style="display:flex;justify-content:center;align-items:center;width:100%;height:100%;padding-bottom:10px;">
                <span style="font-size:12px;font-weight:bold;color:${iconColor};">✎</span>
            </div>
        """.trimIndent()
        
        // Apply HTML style directly in the style string
        val editDot = graph.insertVertex(
            parent, null, editIconHtml,
            x + 5, y, 20.0, 20.0, editDotStyle + ";html=1"
        )

        dotCells.add(editDot as mxCell)
    }
    
    // Create copy dot with a copy icon
    private fun createCopyDot(cell: mxCell, x: Double, y: Double, isStepNode: Boolean) {
        val iconColor = if (isStepNode) "#4ade80" else "#6366F1"
        
        // Create HTML content with enhanced styling for the copy icon
        val copyIconHtml = """
            <div style="display:flex;justify-content:center;align-items:center;width:100%;height:100%;padding-bottom:10px;">
                <span style="font-size:10px;font-weight:bold;color:${iconColor};">⧉</span>
            </div>
        """.trimIndent()
        
        // Create copy dot (document/copy icon)
        val deleteDotStyle = if (isStepNode) "deleteGreenDot" else "deleteDot"
        val deleteDot = graph.insertVertex(
            parent, null, copyIconHtml,
            x + 35, y, 20.0, 20.0, deleteDotStyle + ";html=1"
        )

        dotCells.add(deleteDot as mxCell)
    }

    private fun showEditDialog(cell: mxCell) {
        NodeEditDialog.showEditDialog(this, cell, graph)
    }
    
    private fun registerCustomShapes() {
        // Đăng ký shape tùy chỉnh vào canvas
        mxGraphics2DCanvas.putShape(SHAPE_NODEWITH_DOT, NodeWithDotShape())
        mxGraphics2DCanvas.putShape(SHAPE_STEP_NODE, StepNodeShape())
    }
    
    private fun configureGraphStyles() {
        val stylesheet = graph.stylesheet
        
        // Configure different style types
        configureNodeStyles(stylesheet)
        configureStepNodeStyles(stylesheet)
        configureStandardEdgeStyle(stylesheet)
        configureDotStyles(stylesheet)
        configureActionDotStyles(stylesheet)
    }
    
    // Configure base node styles
    private fun configureNodeStyles(stylesheet: mxStylesheet) {
        val nodeStyle = stylesheet.getDefaultVertexStyle()
        nodeStyle[mxConstants.STYLE_FILLCOLOR] = "#2D2D2D" // Dark fill
        nodeStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple border
        nodeStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        nodeStyle[mxConstants.STYLE_FONTCOLOR] = "#FFFFFF"
        nodeStyle[mxConstants.STYLE_FONTFAMILY] = "Arial"
        nodeStyle[mxConstants.STYLE_FONTSIZE] = "12"
        nodeStyle[mxConstants.STYLE_SHADOW] = "0"
        nodeStyle[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_LEFT
        nodeStyle[mxConstants.STYLE_SHAPE] = SHAPE_NODEWITH_DOT // Use custom shape
    }
    
    // Configure step node styles
    private fun configureStepNodeStyles(stylesheet: mxStylesheet) {
        val stepNodeStyle = HashMap<String, Any>()
        stepNodeStyle[mxConstants.STYLE_SHAPE] = SHAPE_STEP_NODE
        stepNodeStyle[mxConstants.STYLE_ROUNDED] = "1" // Rounded corners
        stepNodeStyle[mxConstants.STYLE_FILLCOLOR] = "#2D2D2D" // Dark fill
        stepNodeStyle[mxConstants.STYLE_STROKECOLOR] = "#4ade80" // Green border
        stepNodeStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        stepNodeStyle[mxConstants.STYLE_FONTCOLOR] = "#FFFFFF"
        stepNodeStyle[mxConstants.STYLE_FONTFAMILY] = "Arial"
        stepNodeStyle[mxConstants.STYLE_FONTSIZE] = "12"
        stepNodeStyle[mxConstants.STYLE_SHADOW] = "0"
        stepNodeStyle[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_LEFT
        stylesheet.putCellStyle("stepNode", stepNodeStyle)
    }
    
    // Configure dot styles (active/inactive for both purple and green)
    private fun configureDotStyles(stylesheet: mxStylesheet) {
        // Inactive dot style (gray with purple border)
        val inactiveDotStyle = HashMap<String, Any>()
        inactiveDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        inactiveDotStyle[mxConstants.STYLE_FILLCOLOR] = "none"
        inactiveDotStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple
        inactiveDotStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        stylesheet.putCellStyle("inactiveDot", inactiveDotStyle)
        
        // Active dot style (purple filled)
        val activeDotStyle = HashMap<String, Any>()
        activeDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        activeDotStyle[mxConstants.STYLE_FILLCOLOR] = "#6366F1" // Purple
        activeDotStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1"
        activeDotStyle[mxConstants.STYLE_STROKEWIDTH] = "0"
        stylesheet.putCellStyle("activeDot", activeDotStyle)
        
        // Inactive dot style (gray with green border)
        val inactiveGreenDotStyle = HashMap<String, Any>()
        inactiveGreenDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        inactiveGreenDotStyle[mxConstants.STYLE_FILLCOLOR] = "none"
        inactiveGreenDotStyle[mxConstants.STYLE_STROKECOLOR] = "#4ade80" // Green
        inactiveGreenDotStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        stylesheet.putCellStyle("inactiveGreenDot", inactiveGreenDotStyle)
        
        // Active dot style (green filled)
        val activeGreenDotStyle = HashMap<String, Any>()
        activeGreenDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        activeGreenDotStyle[mxConstants.STYLE_FILLCOLOR] = "#4ade80" // Green
        activeGreenDotStyle[mxConstants.STYLE_STROKECOLOR] = "#4ade80"
        activeGreenDotStyle[mxConstants.STYLE_STROKEWIDTH] = "0"
        stylesheet.putCellStyle("activeGreenDot", activeGreenDotStyle)

        val connectionDotStyle = HashMap<String, Any>()
        connectionDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        connectionDotStyle[mxConstants.STYLE_FILLCOLOR] = "#6366F1"
        connectionDotStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1"
        connectionDotStyle[mxConstants.STYLE_STROKEWIDTH] = "1"
        stylesheet.putCellStyle("connectionDot", connectionDotStyle)
    }
    
    // Configure action dot styles (edit/delete for both purple and green)
    private fun configureActionDotStyles(stylesheet: mxStylesheet) {
        // Common properties for action dots
        val commonProps = HashMap<String, Any>()
        commonProps[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        commonProps[mxConstants.STYLE_FILLCOLOR] = "#2D2D2D" // Same as background
        commonProps[mxConstants.STYLE_STROKEWIDTH] = 2
        commonProps[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_MIDDLE
        commonProps[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_CENTER
        
        // Create styles with different colors
        createActionDotStyle(stylesheet, "editDot", "#6366F1", commonProps) // Purple
        createActionDotStyle(stylesheet, "editGreenDot", "#4ade80", commonProps) // Green
        createActionDotStyle(stylesheet, "deleteDot", "#6366F1", commonProps) // Purple
        createActionDotStyle(stylesheet, "deleteGreenDot", "#4ade80", commonProps) // Green
    }
    
    // Helper method to create action dot styles with specific colors
    private fun createActionDotStyle(stylesheet: mxStylesheet, styleName: String, color: String, baseProps: HashMap<String, Any>) {
        val style = HashMap<String, Any>(baseProps) // Clone the base properties
        style[mxConstants.STYLE_STROKECOLOR] = color
        style[mxConstants.STYLE_FONTCOLOR] = color
        stylesheet.putCellStyle(styleName, style)
    }
    
    // Define a consistent edge style for all connections
    private fun configureStandardEdgeStyle(stylesheet: mxStylesheet) {
        val edgeStyle = stylesheet.getDefaultEdgeStyle()
        edgeStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple edge
        edgeStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        edgeStyle[mxConstants.STYLE_ENDARROW] = mxConstants.ARROW_CLASSIC
        edgeStyle[mxConstants.STYLE_STARTARROW] = mxConstants.NONE
        edgeStyle[mxConstants.STYLE_EDGE] = mxConstants.EDGESTYLE_ORTHOGONAL
        edgeStyle[mxConstants.STYLE_DASHED] = "1" // Dashed line like in image
        edgeStyle[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_MIDDLE // Center edge vertically
        edgeStyle[mxConstants.STYLE_EXIT_X] = "1.0" // Exit from right side of source
        edgeStyle[mxConstants.STYLE_EXIT_Y] = "0.5" // Exit from middle of right side
        edgeStyle[mxConstants.STYLE_ENTRY_X] = "0.0" // Enter from left side of target
        edgeStyle[mxConstants.STYLE_ENTRY_Y] = "0.5" // Enter from middle of left side
        
        // Define the dash pattern more precisely
        edgeStyle[mxConstants.STYLE_DASH_PATTERN] = "8 4" // 8px dash, 4px space
        
        // Create a specific style for edges connecting nodes
        val standardEdgeStyle = HashMap<String, Any>()
        
        // Safe copying of values from edgeStyle to standardEdgeStyle
        edgeStyle.forEach { (key, value) ->
            if (value != null) {
                standardEdgeStyle[key] = value
            }
        }
        
        stylesheet.putCellStyle("defaultEdge", standardEdgeStyle)
        
        // Create a highlighted edge style for hover effect
        val highlightedEdgeStyle = HashMap<String, Any>(standardEdgeStyle)
        highlightedEdgeStyle[mxConstants.STYLE_STROKECOLOR] = "#a5b4fc" // Lighter purple for highlighting
        highlightedEdgeStyle[mxConstants.STYLE_STROKEWIDTH] = "3" // Thicker line when highlighted
        stylesheet.putCellStyle("highlightedEdge", highlightedEdgeStyle)
    }
    
    fun createNewProjectNode() {
        graph.model.beginUpdate()
        try {
            // Create the initial query node
            val queryNode = createInitialQueryNode()
            
            // Create the add button and connect it to the query node
            createAddButtonForNode(queryNode)
        } finally {
            graph.model.endUpdate()
        }
    }
    
    // Create the initial query node for a new project
    private fun createInitialQueryNode(): Any {
        val initialQueryText = "How do I use Audio Eraser?"
        return graph.insertVertex(
            parent, null, NodeEditDialog.createQueryNodeContent(initialQueryText),
            50.0, 400.0, 180.0, 90.0, "defaultVertex"
        )
    }
    
    // Create an add button connected to the specified node
    private fun createAddButtonForNode(sourceNode: Any) {
        // Get the geometry of the source node
        val nodeGeometry = graph.getCellGeometry(sourceNode)
        
        // Calculate vertical center position for the add button
        val nodeCenterY = nodeGeometry.y + (nodeGeometry.height / 2.0) - 15.0 // nodeY + (nodeHeight/2) - (buttonHeight/2)
        
        // Add button node with + symbol - align with sourceNode vertically centered
        val addButtonNode = graph.insertVertex(
            parent, null, "+", 
            nodeGeometry.x + nodeGeometry.width + 70.0, nodeCenterY, 30.0, 30.0, "addButton"
        )
        
        // Connect them with a dashed edge
        graph.insertEdge(
            parent, null, "", 
            sourceNode, addButtonNode, "defaultEdge"
        )
    }

    // Helper method to check if a cell is the add button
    private fun isAddButton(cell: Any): Boolean {
        val style = graph.getCellStyle(cell)
        return style.getOrDefault(mxConstants.STYLE_SHAPE, "") == "addButton" || 
               (cell is mxCell && cell.value == "+")
    }
    
    // Helper method to identify the user query node
    private fun isUserQueryNode(cell: mxCell): Boolean {
        // Simple implementation - can be enhanced based on cell properties
        // Here we assume the user query node contains "User Query" text
        val value = cell.value?.toString() ?: ""
        return value.contains("User Query")
    }
    
    // Show dialog to add a new step
    private fun showAddStepDialog(sourceCell: mxCell) {
        AddStepDialog.showAddStepDialog(this, sourceCell) { cell, stepName, guideText ->
            createStepNode(cell, stepName, guideText)
        }
    }
    
    // Create a new step node connected to the source cell
    private fun createStepNode(sourceCell: mxCell, stepName: String, guideText: String) {
        graph.model.beginUpdate()
        try {
            // Find the parent node that the + button is connected to
            val parentNode = findParentNodeForAddButton(sourceCell)
            
            // Get the current position of the + button
            val buttonGeometry = graph.getCellGeometry(sourceCell)
            
            // Create the step node
            val stepNode = createStepNodeAtPosition(
                buttonGeometry.x,
                buttonGeometry.y - 45.0 + (buttonGeometry.height / 2),
                stepName,
                guideText
            ) as mxCell
            
            // Connection dot will be added in updateLayout()
            
            // Connect the parent node to the step node
            if (parentNode != null && parentNode != sourceCell) {
                connectNodeToStepNode(parentNode, stepNode, sourceCell)
            }
            
            // Create a new add button connected to the step node
            val newAddButton = createAddButtonAfterNode(stepNode, buttonGeometry)
            
            // Remove the old + button
            graph.removeCells(arrayOf(sourceCell))
            
            // Update the layout
            updateLayout()
            
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
            if (edgeCell.source != addButton) {
                return edgeCell.source as mxCell
            }
        }
        
        return null
    }
    
    // Create a step node at the specified position
    private fun createStepNodeAtPosition(x: Double, y: Double, stepName: String, guideText: String): Any {
        // Create step node content
        val content = NodeEditDialog.createStepNodeContent(stepName, guideText)
        
        // Determine appropriate node size
        val nodeWidth = 180.0
        val nodeHeight = 90.0
        
        // Create the step node
        return graph.insertVertex(
            parent, null, content, 
            x, y, nodeWidth, nodeHeight, "stepNode"
        )
    }
    
    // Connect a parent node to a step node, removing any existing connection to an add button
    private fun connectNodeToStepNode(parentNode: mxCell, stepNode: Any, oldAddButton: mxCell) {
        // Remove existing edges from parent to the old + button
        val edges = graph.getEdges(oldAddButton)
        for (edge in edges) {
            val edgeCell = edge as mxCell
            if (edgeCell.source == parentNode && edgeCell.target == oldAddButton) {
                graph.removeCells(arrayOf(edgeCell))
                break
            }
        }
        
        // Add new edge from parent to step node
        graph.insertEdge(
            parent, null, "", 
            parentNode, stepNode, "defaultEdge"
        )
    }
    
    // Create a new add button after the specified node
    private fun createAddButtonAfterNode(node: Any, referenceGeometry: com.mxgraph.model.mxGeometry): Any {
        val nodeGeometry = graph.getCellGeometry(node)
        
        // Calculate position for the new + button
        val spacing = 70.0 // Space between nodes
        val newButtonX = nodeGeometry.x + nodeGeometry.width + spacing
        val newButtonY = referenceGeometry.y // Keep the same Y position as reference
        
        // Create the new add button
        val newAddButton = graph.insertVertex(
            parent, null, "+", 
            newButtonX, newButtonY, 30.0, 30.0, "addButton"
        )
        
        // Connect step node to the new + button
        graph.insertEdge(
            parent, null, "", 
            node, newAddButton, "defaultEdge"
        )
        
        return newAddButton
    }
    
    // Helper method to find the parent node for a dot
    private fun findParentNodeForDot(dotCell: Any): mxCell? {
        // Get all vertices in the graph
        val vertices = graph.getChildVertices(graph.defaultParent)
        
        // Find the node that owns this dot (closest node above the dot)
        return vertices.filterIsInstance<mxCell>()
            .filter { it != dotCell && !dotCells.contains(it) }
            .minByOrNull { 
                val dotGeom = graph.getCellGeometry(dotCell)
                val cellGeom = graph.getCellGeometry(it)
                val dx = dotGeom.x - cellGeom.x
                val dy = dotGeom.y - (cellGeom.y + cellGeom.height)
                if (dx >= 0 && dy >= 0 && dx < cellGeom.width + 50) dy else Double.MAX_VALUE
            }
    }
    
    // Helper method to clear drag dots
    private fun clearDragDots() {
        if (dotCells.isNotEmpty()) {
            graph.removeCells(dotCells.toTypedArray())
            dotCells.clear()
        }
    }

    // Highlight an edge when hovered
    private fun highlightEdge(edge: mxCell?) {
        if (edge != null) {
            graph.model.beginUpdate()
            try {
                graph.model.setStyle(edge, "highlightedEdge")
            } finally {
                graph.model.endUpdate()
            }
            graphComponent.refresh()
        }
    }

    // Reset the highlighted edge back to normal
    private fun resetHighlightedEdge() {
        val edge = hoveredEdge
        if (edge != null) {
            graph.model.beginUpdate()
            try {
                graph.model.setStyle(edge, "defaultEdge")
            } finally {
                graph.model.endUpdate()
            }
            graphComponent.refresh()
            hoveredEdge = null
        }
    }

    // Delete the confirmation dialog method since it's no longer needed
    private fun confirmAndDeleteEdge(edge: mxCell) {
        // Delete edge immediately
        deleteEdge(edge)
    }

    // Configure options for creating connections
    private fun configureConnectionOptions() {
        // Set basic connection properties
        graphComponent.getConnectionHandler().setEnabled(true)
        
        // Configure the connection points
        graph.setAllowDanglingEdges(false)
        graph.setPortsEnabled(true)
        
        // Set cell connection style
        val edgeStyle = graph.stylesheet.getDefaultEdgeStyle()
        edgeStyle[mxConstants.STYLE_EDGE] = mxConstants.EDGESTYLE_ORTHOGONAL
        
        // Custom edge preview color and width
        graphComponent.getConnectionHandler().getMarker().setValidColor(Color(99, 102, 241)) // Purple
        
        // Add mouse behavior to initiate connections
        graphComponent.getGraphControl().addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    val cell = graphComponent.getCellAt(e.x, e.y)
                    if (cell != null && graph.model.isVertex(cell) && !isAddButton(cell as mxCell)) {
                        val state = graph.getView().getState(cell)
                        if (state != null) {
                            // Manually start connection from this point
                            graphComponent.getConnectionHandler().start(e, state)
                        }
                    }
                }
            }
        })
        
        // Add connection handler to update layout after connection is made
        graphComponent.connectionHandler.addListener(mxEvent.CONNECT, mxIEventListener { sender, evt ->
            // Update the layout when a connection is made
            updateLayout()
        })
        
        // Allow clicking anywhere on a cell to start a connection
        graph.setPortsEnabled(true)
        graph.setAllowDanglingEdges(false)
    }

    // Separate method to handle node hovering to improve clarity and consistency
    private fun handleNodeHover(cell: Any?, e: MouseEvent) {
        // If hovering over a dot, keep the dots visible
        if (cell != null && (dotCells.contains(cell) || connectionDots.contains(cell))) {
            return
        }
        
        // Check if the cell is a node we should react to
        val isHoveringOverNode = cell != null && 
                              graph.model.isVertex(cell) && 
                              !isAddButton(cell as mxCell) &&
                              !dotCells.contains(cell) &&
                              !connectionDots.contains(cell)
        
        // Determine if we need to clear existing dots
        var shouldClearDots = false
        
        // If moving to a different cell or empty space
        if (cell != currentHoverCell) {
            // If we were hovering over a node before, check if we're still in the dot area
            if (currentHoverCell != null) {
                // Safely capture the current hover cell in a local variable
                val hoverCell = currentHoverCell
                
                // If the current position is not in a dot area, clear the dots
                val inDotArea = if (hoverCell != null) isInDotArea(hoverCell, e.point) else false
                if (!inDotArea && !dotCells.contains(cell) && !connectionDots.contains(cell)) {
                    shouldClearDots = true
                }
            } else {
                // We weren't hovering over anything before, so just clear
                shouldClearDots = true
            }
        }
        
        // Clear dots if needed
        if (shouldClearDots) {
            clearDragDots()
            currentHoverCell = null
        }
        
        // Create new dots if hovering over a node
        if (isHoveringOverNode && cell != currentHoverCell) {
            currentHoverCell = cell
            createDragDots(cell as mxCell)
        }
    }

    // Check if a point is in the dot area below a node
    private fun isInDotArea(node: Any, point: Point): Boolean {
        val cellGeometry = graph.getCellGeometry(node)
        if (cellGeometry != null) {
            // Get the transform from the graph view
            val transform = graph.view.getState(node)?.absoluteOffset
            val scale = graph.view.scale
            
            // Calculate the dot area in screen coordinates
            val x = (cellGeometry.x * scale).toInt()
            val y = ((cellGeometry.y + cellGeometry.height) * scale).toInt()
            val width = (cellGeometry.width * scale).toInt()
            val height = (40 * scale).toInt()  // Height of dot area
            
            // If we have a transform, apply it
            val dotArea = if (transform != null) {
                Rectangle(
                    x + transform.x.toInt(),
                    y + transform.y.toInt(),
                    width,
                    height
                )
            } else {
                Rectangle(x, y, width, height)
            }
            
            return dotArea.contains(point)
        }
        return false
    }

    // Update positions of all connection dots
    private fun updateConnectionDots() {
        // Remove existing connection dots
        if (connectionDots.isNotEmpty()) {
            graph.removeCells(connectionDots.toTypedArray())
            connectionDots.clear()
        }
        
        // Create new connection dots for all nodes
        val allNodes = graph.getChildVertices(parent).filterIsInstance<mxCell>().filter { 
            !isAddButton(it) && !dotCells.contains(it) && !connectionDots.contains(it)
        }
        
        allNodes.forEach { node ->
            createConnectionDot(node)
        }
    }

    // Create a connection dot for a node
    private fun createConnectionDot(node: mxCell) {
        val geom = graph.getCellGeometry(node)
        val dotX = geom.x + geom.width - 10
        val dotY = geom.y + geom.height / 2 - 10
        val dot = graph.insertVertex(parent, null, "", dotX, dotY, 20.0, 20.0, "connectionDot") as mxCell
        connectionDots.add(dot)
    }

    inner class CustomMxGraph : mxGraph() {
        init {
            setAllowDanglingEdges(false)
            setCellsEditable(false)
            setCellsResizable(false)
            setAllowLoops(false)
            setCellsBendable(true)
            setConnectableEdges(false)
            setDisconnectOnMove(false)
            setHtmlLabels(true) // Enable HTML labels for rich formatting
            
            // Disable selection borders and highlighting
            setCellsSelectable(true)
            setCellsDisconnectable(true)
            setDropEnabled(true)
            setSplitEnabled(true)
            setCellsBendable(true)

        }
        

    }
} 