package com.example.demograph

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.mxgraph.io.mxCodec
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.swing.handler.mxRubberband
import com.mxgraph.util.mxXmlUtils
import com.mxgraph.model.mxICell
import com.mxgraph.model.mxGeometry
import com.mxgraph.view.mxGraph
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.*

data class NodeData(
    val id: String,
    val label: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val nextStep: List<String> = emptyList(),
    val prevStep: List<String> = emptyList()
)

data class GraphData(
    val nodes: List<NodeData>
)

class GraphToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val graphPanel = GraphPanel()
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(graphPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class GraphPanel : JPanel(BorderLayout()) {
    private val graph = mxGraph()
    private val graphComponent = mxGraphComponent(graph)
    private val parent = graph.defaultParent
    private var nodeCount = 0
    private var edgeCount = 0
    private val mapper = jacksonObjectMapper()
    private var currentZoom = 1.0

    init {
        // Cấu hình mxGraph
        graph.setCellsMovable(true)
        graph.setCellsSelectable(true)
        graph.setCellsEditable(true)
        graph.setAutoSizeCells(true)
//        graph.setCellsConnectable(true)
        graph.setHtmlLabels(true)

        // Cấu hình graphComponent
        graphComponent.isPanning = true
        graphComponent.isAutoScroll = true
        graphComponent.isEnabled = true
        graphComponent.zoomFactor = 1.2
        graphComponent.isDragEnabled = false

        // Bật rubberband selection
        mxRubberband(graphComponent)

        // Đặt kích thước canvas
        graphComponent.preferredSize = java.awt.Dimension(800, 600)

        // Toolbar với các nút
        val toolbar = JToolBar()
        toolbar.add(createButton("Add Node") { addNode() })
        toolbar.add(createButton("Add Edge") { addEdge() })
        toolbar.add(createButton("Delete Selected") { deleteSelected() })
        toolbar.add(createButton("Save XML") { saveGraphXml() })
        toolbar.add(createButton("Load XML") { loadGraphXml() })
        toolbar.add(createButton("Save JSON") { saveGraphJson() })
        toolbar.add(createButton("Load JSON") { loadGraphJson() })
        toolbar.add(createButton("Zoom In") { zoomIn() })
        toolbar.add(createButton("Zoom Out") { zoomOut() })

        // Thêm toolbar và graphComponent
        add(toolbar, BorderLayout.NORTH)
        add(graphComponent, BorderLayout.CENTER)
    }

    private fun createButton(text: String, action: () -> Unit): JButton {
        return JButton(object : AbstractAction(text) {
            override fun actionPerformed(e: ActionEvent?) {
                try {
                    action()
                } catch (ex: Exception) {
                    JOptionPane.showMessageDialog(this@GraphPanel, "Error: ${ex.message}", "Error", JOptionPane.ERROR_MESSAGE)
                    println("Button action error: ${ex.message}")
                }
            }
        })
    }

    private fun zoomIn() {
        if (currentZoom < 5.0) {
            graphComponent.zoomIn()
            currentZoom *= graphComponent.zoomFactor
            println("Zoom in: $currentZoom")
        }
    }

    private fun zoomOut() {
        if (currentZoom > 0.1) {
            graphComponent.zoomOut()
            currentZoom /= graphComponent.zoomFactor
            println("Zoom out: $currentZoom")
        }
    }

    private fun showNodeDialog(): NodeData? {
        val owner = SwingUtilities.getWindowAncestor(this)
        val dialog = JDialog(owner, "Add Node", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.layout = BorderLayout()
        dialog.setSize(400, 350)
        dialog.setLocationRelativeTo(this)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // Lấy danh sách ID node hiện có
        val existingIds = mutableSetOf<String>()
        val root = graph.model.root as mxICell
        for (i in 0 until graph.model.getChildCount(root)) {
            val cell = graph.model.getChildAt(root, i) as mxICell
            collectIds(cell, existingIds)
        }

        // Các trường nhập
        val idField = JTextField("node${nodeCount + 1}", 20)
        val labelField = JTextField("Node ${nodeCount + 1}", 20)
        val nextStepField = JTextField("", 20)
        val prevStepField = JTextField("", 20)

        panel.add(JLabel("Node ID:"))
        panel.add(idField)
        panel.add(JLabel("Label:"))
        panel.add(labelField)
        panel.add(JLabel("Next Step (comma-separated IDs, e.g., node1,node2; optional, leave empty for none):"))
        panel.add(nextStepField)
        panel.add(JLabel("Prev Step (comma-separated IDs, e.g., node1,node2; optional, leave empty for none):"))
        panel.add(prevStepField)
        panel.add(JLabel("Existing IDs (${existingIds.size}): ${existingIds.joinToString(", ")}"))

        val buttonPanel = JPanel()
        val okButton = JButton("OK")
        val cancelButton = JButton("Cancel")

        var result: NodeData? = null
        okButton.addActionListener {
            val id = idField.text.trim()
            val label = labelField.text.trim()
            val nextStep = nextStepField.text.trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val prevStep = prevStepField.text.trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // Log debug
            println("Dialog input - ID: $id, Label: $label, NextStep: $nextStep, PrevStep: $prevStep, Existing IDs: $existingIds")

            // Kiểm tra đầu vào
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "ID cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                println("Error: ID is empty")
                return@addActionListener
            }
            if (existingIds.contains(id)) {
                JOptionPane.showMessageDialog(dialog, "ID '$id' already exists", "Error", JOptionPane.ERROR_MESSAGE)
                println("Error: ID '$id' already exists")
                return@addActionListener
            }
            if (label.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Label cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                println("Error: Label is empty")
                return@addActionListener
            }

            // Kiểm tra nextStep và prevStep chỉ khi không rỗng và có node khác
            if (existingIds.isNotEmpty()) {
                val invalidNext = nextStep.filter { it !in existingIds }
                val invalidPrev = prevStep.filter { it !in existingIds }
                if (invalidNext.isNotEmpty() || invalidPrev.isNotEmpty()) {
                    val message = buildString {
                        if (invalidNext.isNotEmpty()) append("Invalid nextStep IDs: ${invalidNext.joinToString(", ")}\n")
                        if (invalidPrev.isNotEmpty()) append("Invalid prevStep IDs: ${invalidPrev.joinToString(", ")}")
                    }
                    JOptionPane.showMessageDialog(dialog, message, "Warning", JOptionPane.WARNING_MESSAGE)
                    println("Warning: $message")
                }
            }

            result = NodeData(
                id = id,
                label = label,
                x = 40.0 + (nodeCount % 5) * 100.0,
                y = 40.0 + (nodeCount / 5) * 60.0,
                width = 100.0,
                height = 40.0,
                nextStep = nextStep.filter { existingIds.isEmpty() || it in existingIds },
                prevStep = prevStep.filter { existingIds.isEmpty() || it in existingIds }
            )
            println("NodeData created: $result")
            dialog.dispose()
        }
        cancelButton.addActionListener {
            println("Dialog cancelled")
            dialog.dispose()
        }

        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)
        dialog.add(panel, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)
        dialog.isVisible = true

        return result
    }

    private fun collectIds(cell: mxICell, ids: MutableSet<String>) {
        if (graph.model.isVertex(cell) && cell.id != null) {
            ids.add(cell.id)
        }
        for (i in 0 until graph.model.getChildCount(cell)) {
            collectIds(graph.model.getChildAt(cell, i) as mxICell, ids)
        }
    }

    private fun getNodeMap(): Map<String, Any> {
        val nodeMap = mutableMapOf<String, Any>()
        val root = graph.model.root as mxICell
        for (i in 0 until graph.model.getChildCount(root)) {
            val cell = graph.model.getChildAt(root, i) as mxICell
            collectNodesForMap(cell, nodeMap)
        }
        return nodeMap
    }

    private fun collectNodesForMap(cell: mxICell, nodeMap: MutableMap<String, Any>) {
        if (graph.model.isVertex(cell) && cell.id != null) {
            nodeMap[cell.id] = cell
        }
        for (i in 0 until graph.model.getChildCount(cell)) {
            collectNodesForMap(graph.model.getChildAt(cell, i) as mxICell, nodeMap)
        }
    }

    private fun addNode() {
        val nodeData = showNodeDialog()
        if (nodeData == null) {
            println("addNode: NodeData is null, dialog likely cancelled")
            return
        }
        println("addNode: Creating node - ID: ${nodeData.id}, Label: ${nodeData.label}, NextStep: ${nodeData.nextStep}, PrevStep: ${nodeData.prevStep}")
        graph.model.beginUpdate()
        try {
            nodeCount++
            val label = "<div style='text-align:center'>${nodeData.label}</div>"
            val vertex = graph.insertVertex(
                parent,
                nodeData.id,
                label,
                nodeData.x,
                nodeData.y,
                nodeData.width,
                nodeData.height
            )
            println("addNode: Vertex created with ID: ${nodeData.id}")

            // Tạo cạnh cho nextStep
            val nodeMap = getNodeMap()
            for (targetId in nodeData.nextStep) {
                val target = nodeMap[targetId] as mxICell? ?: continue
                val edgeId = "edge${++edgeCount}"
                graph.insertEdge(parent, edgeId, "", vertex, target)
                println("addNode: Edge created from ${nodeData.id} to $targetId")
            }

            // Tạo cạnh cho prevStep
            for (sourceId in nodeData.prevStep) {
                val source = nodeMap[sourceId] as mxICell? ?: continue
                val edgeId = "edge${++edgeCount}"
                graph.insertEdge(parent, edgeId, "", source, vertex)
                println("addNode: Edge created from $sourceId to ${nodeData.id}")
            }
        } finally {
            graph.model.endUpdate()
        }
    }

    private fun addEdge() {
        val selectedCells = graph.selectionCells
        if (selectedCells.size == 2 && graph.model.isVertex(selectedCells[0] as mxICell) && graph.model.isVertex(selectedCells[1] as mxICell)) {
            graph.model.beginUpdate()
            try {
                val source = selectedCells[0]
                val target = selectedCells[1]
                val id = "edge${++edgeCount}"
                graph.insertEdge(parent, id, "", source, target)
                println("addEdge: Edge created from ${source} to ${target}")
            } finally {
                graph.model.endUpdate()
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select exactly two nodes to add an edge")
            println("addEdge: Invalid selection for edge creation")
        }
    }

    private fun deleteSelected() {
        val cells = graph.selectionCells
        if (cells.isNotEmpty()) {
            graph.model.beginUpdate()
            try {
                graph.removeCells(cells)
                println("deleteSelected: Removed ${cells.size} cells")
            } finally {
                graph.model.endUpdate()
            }
        } else {
            JOptionPane.showMessageDialog(this, "No cells selected")
            println("deleteSelected: No cells selected")
        }
    }

    private fun saveGraphXml() {
        try {
            val encoder = mxCodec()
            val node = encoder.encode(graph.model)
            val xml = mxXmlUtils.getXml(node)
            val file = File("graph.xml")
            file.writeText(xml)
            JOptionPane.showMessageDialog(this, "XML saved to ${file.absolutePath}")
            println("saveGraphXml: XML saved to ${file.absolutePath}")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Failed to save XML: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("saveGraphXml: Error - ${e.message}")
        }
    }

    private fun loadGraphXml() {
        try {
            val file = File("graph.xml")
            if (file.exists()) {
                val xml = file.readText()
                val document = mxXmlUtils.parseXml(xml)
                val decoder = mxCodec(document)
                decoder.decode(document.documentElement, graph.model)
                graph.refresh()
                nodeCount = graph.model.getChildCount(parent as mxICell)
                edgeCount = graph.model.getChildCount(parent as mxICell) - nodeCount
                println("loadGraphXml: XML loaded, nodeCount: $nodeCount, edgeCount: $edgeCount")
            } else {
                JOptionPane.showMessageDialog(this, "graph.xml not found")
                println("loadGraphXml: graph.xml not found")
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Failed to load XML: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("loadGraphXml: Error - ${e.message}")
        }
    }

    private fun saveGraphJson() {
        try {
            val nodes = mutableListOf<NodeData>()
            val edgeMap = mutableMapOf<String, MutableList<String>>() // Lưu nextStep
            val prevEdgeMap = mutableMapOf<String, MutableList<String>>() // Lưu prevStep
            val root = graph.model.root as mxICell
            for (i in 0 until graph.model.getChildCount(root)) {
                val cell = graph.model.getChildAt(root, i) as mxICell
                collectNodesAndEdges(cell, nodes, edgeMap, prevEdgeMap)
            }
            // Tạo danh sách NodeData với nextStep và prevStep
            val finalNodes = nodes.map { node ->
                node.copy(
                    nextStep = edgeMap[node.id] ?: emptyList(),
                    prevStep = prevEdgeMap[node.id] ?: emptyList()
                )
            }
            val graphData = GraphData(finalNodes)
            val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(graphData)
            val file = File("graph.json")
            file.writeText(json)
            JOptionPane.showMessageDialog(this, "JSON saved to ${file.absolutePath}")
            println("saveGraphJson: JSON saved to ${file.absolutePath}")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Failed to save JSON: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("saveGraphJson: Error - ${e.message}")
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
                    label = graph.model.getValue(cell)?.toString() ?: "",
                    x = geometry?.x ?: 0.0,
                    y = geometry?.y ?: 0.0,
                    width = geometry?.width ?: 0.0,
                    height = geometry?.height ?: 0.0
                )
            )
            println("collectNodesAndEdges: Added node ${cell.id}")
        } else if (graph.model.isEdge(cell)) {
            val source = cell.getTerminal(true) as mxICell?
                ?: throw IllegalStateException("Edge missing source for cell ${cell.id}")
            val target = cell.getTerminal(false) as mxICell?
                ?: throw IllegalStateException("Edge missing target for cell ${cell.id}")
            val sourceId = source.id ?: throw IllegalStateException("Source node missing ID")
            val targetId = target.id ?: throw IllegalStateException("Target node missing ID")
            edgeMap.computeIfAbsent(sourceId) { mutableListOf() }.add(targetId)
            prevEdgeMap.computeIfAbsent(targetId) { mutableListOf() }.add(sourceId)
            println("collectNodesAndEdges: Added edge from $sourceId to $targetId")
        }
        for (i in 0 until graph.model.getChildCount(cell)) {
            collectNodesAndEdges(graph.model.getChildAt(cell, i) as mxICell, nodes, edgeMap, prevEdgeMap)
        }
    }

    private fun loadGraphJson() {
        try {
            val file = File("graph.json")
            if (file.exists()) {
                val json = file.readText()
                val graphData: GraphData = mapper.readValue(json)
                graph.model.beginUpdate()
                try {
                    // Xóa tất cả cell trong defaultParent
                    val cells = graph.getChildCells(graph.defaultParent, true, true)
                    if (cells.isNotEmpty()) {
                        graph.removeCells(cells)
                        println("loadGraphJson: Cleared ${cells.size} existing cells")
                    }
                    val nodeMap = mutableMapOf<String, Any>()
                    nodeMap["defaultParent"] = graph.defaultParent

                    // Thêm tất cả node
                    for (nodeData in graphData.nodes) {
                        val cell = graph.insertVertex(
                            parent,
                            nodeData.id,
                            nodeData.label,
                            nodeData.x,
                            nodeData.y,
                            nodeData.width,
                            nodeData.height
                        )
                        nodeMap[nodeData.id] = cell
                        println("loadGraphJson: Added node ${nodeData.id}")
                    }

                    // Thêm tất cả cạnh từ nextStep
                    for (nodeData in graphData.nodes) {
                        val source = nodeMap[nodeData.id] ?: continue
                        for (targetId in nodeData.nextStep) {
                            val target = nodeMap[targetId] ?: continue
                            val id = "edge${++edgeCount}"
                            graph.insertEdge(parent, id, "", source, target)
                            println("loadGraphJson: Added edge from ${nodeData.id} to $targetId")
                        }
                    }
                } finally {
                    graph.model.endUpdate()
                }
                graph.refresh()
                nodeCount = graphData.nodes.size
                edgeCount = graphData.nodes.sumOf { it.nextStep.size }
                println("loadGraphJson: Loaded ${nodeCount} nodes, ${edgeCount} edges")
            } else {
                JOptionPane.showMessageDialog(this, "graph.xml not found")
                println("loadGraphJson: graph.json not found")
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Failed to load JSON: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("loadGraphJson: Error - ${e.message}")
        }
    }
}