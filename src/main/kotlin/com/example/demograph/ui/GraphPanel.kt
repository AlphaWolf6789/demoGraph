package com.example.demograph.ui

import com.example.demograph.model.GraphModel
import com.example.demograph.service.GraphService
import com.example.demograph.service.StorageService
import com.example.demograph.service.ZoomService
import com.mxgraph.model.mxICell
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.swing.handler.mxRubberband
import com.mxgraph.util.mxEvent
import com.mxgraph.util.mxEventObject
import com.mxgraph.util.mxEventSource.mxIEventListener
import com.mxgraph.util.mxPoint
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class GraphPanel : JPanel(BorderLayout()) {
    private val graphModel = GraphModel()
    private val graphService = GraphService(graphModel)
    private val storageService = StorageService(graphModel)
    private val zoomService = ZoomService()
    private val graphComponent = mxGraphComponent(graphModel.getGraph())
    private var hoverPanel: JPanel? = null
    private var currentHoveredCell: mxICell? = null

    init {
        // Cấu hình graphComponent
        graphComponent.isPanning = true
        graphComponent.isAutoScroll = true
        graphComponent.isEnabled = true
        graphComponent.zoomFactor = 1.2
        graphComponent.isDragEnabled = false
        graphComponent.isConnectable = true
        graphComponent.connectionHandler.isEnabled = true
        graphComponent.preferredSize = java.awt.Dimension(800, 600)
        mxRubberband(graphComponent)

        graphComponent.connectionHandler.addListener(mxEvent.CONNECT, mxIEventListener { sender, evt ->
            val edge = evt.getProperty("cell") as? mxICell
            val target = edge?.getTerminal(false) as? mxICell
            if (edge != null && (target == null || !graphModel.getGraph().model.isVertex(target))) {
                graphModel.getGraph().model.beginUpdate()
                try {
                    graphModel.getGraph().removeCells(arrayOf(edge))
                    JOptionPane.showMessageDialog(
                        this@GraphPanel,
                        "Target must be a valid node",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                    println("mxConnectionHandler: Invalid target, edge removed")
                } finally {
                    graphModel.getGraph().model.endUpdate()
                }
            } else if (edge != null) {
                println("mxConnectionHandler: Edge created successfully")
            }
        })

        // Thêm mouse listener để xử lý hover
        graphComponent.graphControl.addMouseListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val cell = graphComponent.getCellAt(e.x, e.y) as? mxICell
                if (cell != null && graphModel.getGraph().model.isVertex(cell) && cell != currentHoveredCell) {
                    showHoverPanel(cell, e.point)
                    currentHoveredCell = cell
                } else if (cell == null || !graphModel.getGraph().model.isVertex(cell)) {
                    hideHoverPanel()
                    currentHoveredCell = null
                }
            }
        })

        CustomGraphRenderer.applyStyles(graphModel.getGraph())

        // Toolbar
        val toolbar = JToolBar()
        toolbar.add(createButton("Add Node") { graphService.addNodeWithDialog(this) })
        toolbar.add(createButton("Add Edge") { graphService.addEdgeBetweenSelected() })
        toolbar.add(createButton("Delete Selected") { graphService.deleteSelected() })
        toolbar.add(createButton("Save XML") { storageService.saveGraphXml() })
        toolbar.add(createButton("Load XML") { storageService.loadGraphXml() })
        toolbar.add(createButton("Save JSON") { storageService.saveGraphJson() })
        toolbar.add(createButton("Load JSON") { storageService.loadGraphJson() })
        toolbar.add(createButton("Zoom In") { zoomService.zoomIn(graphComponent) })
        toolbar.add(createButton("Zoom Out") { zoomService.zoomOut(graphComponent) })

        add(toolbar, BorderLayout.NORTH)
        add(graphComponent, BorderLayout.CENTER)
    }

    private fun showHoverPanel(cell: mxICell, mousePoint: Point) {
        // Ẩn panel hiện tại nếu có
        hideHoverPanel()

        // Tạo panel mới
        hoverPanel = JPanel()
        hoverPanel?.layout = BoxLayout(hoverPanel, BoxLayout.X_AXIS)
        hoverPanel?.border = BorderFactory.createEtchedBorder()

        // Nút Edit
        val editButton = JButton("Edit")
        editButton.addActionListener {
//            cell.id?.let { graphService.editNodeWithDialog(this@GraphPanel, graphComponent, it) }
            hideHoverPanel()
        }

        // Nút Delete
        val deleteButton = JButton("Delete")
        deleteButton.addActionListener {
            cell.id?.let { graphService.deleteNode(it) }
            hideHoverPanel()
        }

        // Nút Add Node
        val addButton = JButton("Add Node")
        addButton.addActionListener {
            cell.id?.let { graphService.addNodeWithDialog(this@GraphPanel) }
            hideHoverPanel()
        }

        hoverPanel?.add(editButton)
        hoverPanel?.add(deleteButton)
        hoverPanel?.add(addButton)

        // Định vị panel
        val geometry = graphModel.getGraph().model.getGeometry(cell)
        if (geometry != null) {
            // Chuyển đổi tọa độ đồ thị sang tọa độ màn hình
            val view = graphComponent.graph.view
            val scale = view.scale
            val translate = view.translate
            val screenX = (geometry.x * scale) + translate.x
            val screenY = (geometry.y * scale) + translate.y

            hoverPanel?.setBounds(
                screenX.toInt(),
                screenY.toInt() - 30, // Hiển thị phía trên node
                200, 30
            )
            graphComponent.add(hoverPanel)
            graphComponent.revalidate()
            graphComponent.repaint()
            println("showHoverPanel: Panel displayed at ($screenX, ${screenY - 30}), scale=$scale, translate=($translate.x, $translate.y)")
        }
    }

    private fun hideHoverPanel() {
        hoverPanel?.let {
            graphComponent.remove(it)
            graphComponent.revalidate()
            graphComponent.repaint()
        }
        hoverPanel = null
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
}