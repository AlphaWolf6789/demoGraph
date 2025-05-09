package com.example.demograph

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.mxgraph.io.mxCodec
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.swing.handler.mxPanningHandler
import com.mxgraph.swing.handler.mxRubberband
import com.mxgraph.util.mxXmlUtils
import com.mxgraph.view.mxGraph
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.*

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
    private var nodeCount = 0 // Biến để tạo vị trí node tăng dần

    init {
        // Cấu hình mxGraph
        graph.isHtmlLabels = true
        graph.isCellsMovable = true // Cho phép di chuyển node
        graph.isCellsSelectable = true // Cho phép chọn node
        graph.isCellsEditable = true // Cho phép chỉnh sửa nhãn
        graph.isAutoSizeCells = true // Tự động điều chỉnh kích thước node
//        graph.isConnectable = true // Cho phép kết nối cạnh

        // Cấu hình graphComponent
        graphComponent.isPanning = true // Bật pan
        graphComponent.isAutoScroll = true // Tự động cuộn khi pan
        graphComponent.isEnabled = true // Bật tất cả tương tác
        graphComponent.isEventsEnabled = true // Bật sự kiện chuột
        graphComponent.isDragEnabled = false // Bật kéo thả
//        graphComponent.minimumZoom = 0.1 // Giới hạn zoom tối thiểu
//        graphComponent.maximumZoom = 5.0 // Giới hạn zoom tối đa

        // Cấu hình panning handler
        val panningHandler = mxPanningHandler(graphComponent)
//        panningHandler.isPanningEnabled = true
//        panningHandler.useLeftButtonForPanning = false // Sử dụng chuột phải để pan

        // Bật rubberband selection
        mxRubberband(graphComponent)

        // Đặt kích thước canvas
        graphComponent.setPreferredSize(java.awt.Dimension(800, 600))

        // Toolbar với các nút
        val toolbar = JToolBar()
        toolbar.add(createButton("Add Node") { addNode() })
        toolbar.add(createButton("Delete Selected") { deleteSelected() })
        toolbar.add(createButton("Save") { saveGraph() })
        toolbar.add(createButton("Load") { loadGraph() })
        toolbar.add(createButton("Zoom In") { graphComponent.zoomIn() })
        toolbar.add(createButton("Zoom Out") { graphComponent.zoomOut() })

        // Thêm toolbar và graphComponent vào panel
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
                }
            }
        })
    }

    private fun addNode() {
        graph.model.beginUpdate()
        try {
            // Tạo vị trí tăng dần để tránh chồng lấn
            val x = 40.0 + (nodeCount % 5) * 100.0
            val y = 40.0 + (nodeCount / 5) * 60.0
            val nodeName = "Node ${++nodeCount}"
            // Tạo nhãn HTML với tên node và parent
            val parentCell = parent // Mặc định là defaultParent
            val parentName = if (parentCell == graph.defaultParent) "No Parent" else (graph.model.getValue(parentCell)?.toString() ?: "No Parent")
            val label = "<div style='text-align:center'>$nodeName<br><i>Parent: $parentName</i></div>"
            graph.insertVertex(parent, null, label, x, y, 100.0, 40.0)
        } finally {
            graph.model.endUpdate()
        }
    }

    private fun deleteSelected() {
        val cells = graph.selectionCells
        if (cells.isNotEmpty()) {
            graph.model.beginUpdate()
            try {
                graph.removeCells(cells)
            } finally {
                graph.model.endUpdate()
            }
        } else {
            JOptionPane.showMessageDialog(this, "No cells selected")
        }
    }

    private fun saveGraph() {
        try {
            val encoder = mxCodec()
            val node = encoder.encode(graph.model)
            val xml = mxXmlUtils.getXml(node)
            val file = File("graph.xml")
            file.writeText(xml)
            JOptionPane.showMessageDialog(this, "Graph saved to ${file.absolutePath}")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Failed to save graph: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
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
                // Reset nodeCount dựa trên số node hiện có
                nodeCount = graph.model.getChildCount(parent)
            } else {
                JOptionPane.showMessageDialog(this, "graph.xml not found")
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Failed to load graph: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}