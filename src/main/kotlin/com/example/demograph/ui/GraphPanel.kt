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
import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
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
    
    // Rectangle để theo dõi vùng an toàn (node + buttons)
    private var safeZone: Rectangle? = null

    init {
        // Cấu hình graphComponent
        graphComponent.isAutoScroll = true
        graphComponent.isEnabled = true
        graphComponent.zoomFactor = 1.2
        graphComponent.isDragEnabled = false // Cho phép kéo node mặc định
        graphComponent.isConnectable = true
        graphComponent.connectionHandler.isEnabled = true
        graphComponent.preferredSize = java.awt.Dimension(800, 600)
        graphModel.getGraph().isCellsSelectable = true
        graphModel.getGraph().isCellsEditable = true
        
        // Thêm mxRubberband để cho phép chọn nhiều node
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
        
        // Thêm event listener để xử lý khi cell thay đổi - để debug
        graphModel.getGraph().addListener(mxEvent.CELLS_MOVED, mxIEventListener { sender, evt ->
            println("Cells moved event: ${evt.properties}")
        })

        // Thêm mouse listener để xử lý hover
        graphComponent.graphControl.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                handleMouseMovement(e)
            }
        })
        
        // Xử lý khi chuột rời khỏi vùng đồ thị hoặc khi click
        graphComponent.graphControl.addMouseListener(object : MouseAdapter() {
            override fun mouseExited(e: MouseEvent) {
                // Không ẩn panel ngay lập tức khi thoát khỏi graphControl
                // vì có thể đang chuyển đến panel buttons
            }
            
            override fun mousePressed(e: MouseEvent) {
                // Giữ lại panel nếu click vào nút, nếu không thì ẩn
                if (!isInSafeZone(e.point)) {
                    hideHoverPanel()
                    currentHoveredCell = null
                    safeZone = null
                }
            }
            
            // Thêm để catch sự kiện mouseEntered nếu cần
            override fun mouseEntered(e: MouseEvent) {
                // Không làm gì đặc biệt khi chuột vào vùng graphControl
            }
        })

        CustomGraphRenderer.applyStyles(graphModel.getGraph())

        // Toolbar
        val toolbar = JToolBar()
        toolbar.add(createButton("Add Node") { 
            graphService.addNodeWithDialog(this)
        })
        toolbar.add(createButton("Add Edge") {
            graphService.addEdgeBetweenSelected()
        })
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
    
    private fun isInSafeZone(point: Point): Boolean {
        return safeZone?.contains(point) ?: false
    }
    
    private fun handleMouseMovement(e: MouseEvent) {
        // Lấy cell tại vị trí chuột
        val cell = graphComponent.getCellAt(e.x, e.y) as? mxICell
        
        // Kiểm tra xem chuột có nằm trong vùng an toàn không
        if (isInSafeZone(e.point)) {
            // Chuột đang ở trong vùng an toàn, không làm gì
            return
        }
        
        if (cell != null && graphModel.getGraph().model.isVertex(cell)) {
            if (cell != currentHoveredCell) {
                hideHoverPanel() // Đảm bảo panel cũ bị ẩn trước khi hiển thị panel mới
                currentHoveredCell = cell
                showHoverPanel(cell)
                println("Mouse over cell ID: ${cell.id}")
            }
        } else if (cell == null) {
            // Nếu chuột không ở trên cell và không ở trong vùng an toàn, ẩn panel
            hideHoverPanel()
            currentHoveredCell = null
            safeZone = null
        }
    }

    private fun showHoverPanel(cell: mxICell) {
        // Ẩn panel hiện tại nếu có
        hideHoverPanel()
        
        // Kiểm tra ID của cell
        val cellId = cell.id ?: return
        
        println("Creating hover panel for cell ID: $cellId")
        
        // Tạo panel mới
        hoverPanel = JPanel()
        hoverPanel?.layout = BoxLayout(hoverPanel, BoxLayout.X_AXIS)
        hoverPanel?.background = Color(240, 240, 240, 220) // Increased opacity
        hoverPanel?.border = BorderFactory.createLineBorder(Color.GRAY)
        
        // Tạo các nút với kích thước lớn hơn để dễ click
        
        // Nút Add - Thêm node mới với prevStep là node hiện tại
        val addButton = JButton("Add")
        addButton.addActionListener {
            graphService.addNodeFromSource(this@GraphPanel, cellId)
            hideHoverPanel()
            safeZone = null
        }
        
        // Nút Edit - Chỉnh sửa nội dung node
        val editButton = JButton("Edit")
        editButton.addActionListener {
            graphService.editNodeWithDialog(this@GraphPanel, cellId)
            hideHoverPanel()
            safeZone = null
        }
        
        // Nút Delete - Xóa node
        val deleteButton = JButton("Delete")
        deleteButton.addActionListener {
            graphService.deleteNode(cellId)
            hideHoverPanel()
            safeZone = null
        }
        
        // Thêm các nút vào panel
        hoverPanel?.add(addButton)
        hoverPanel?.add(Box.createHorizontalStrut(5)) // Tạo khoảng cách giữa các nút
        hoverPanel?.add(editButton)
        hoverPanel?.add(Box.createHorizontalStrut(5)) // Tạo khoảng cách giữa các nút
        hoverPanel?.add(deleteButton)

        // Định vị panel bên dưới node
        val state = graphComponent.getGraph().view.getState(cell)
        if (state != null) {
            // Vị trí panel bên dưới node
            val screenX = state.x
            val screenY = state.y + state.height + 5 // +5 pixel spacing

            // Tạo panel với kích thước vừa đủ
            val buttonPanelWidth = 260 // Đã sửa từ 170 thành 270
            val buttonPanelHeight = 30
            
            hoverPanel?.setBounds(
                screenX.toInt(),
                screenY.toInt(),
                buttonPanelWidth, 
                buttonPanelHeight
            )
            
            // Tạo vùng an toàn bao gồm cả node và panel nút
            safeZone = Rectangle(
                state.x.toInt() - 10, // Mở rộng ra hai bên 10px
                state.y.toInt() - 10,  // Mở rộng lên trên 10px
                (state.width + 20).toInt(), // +20 để bù cho việc mở rộng 10px mỗi bên
                (state.height + buttonPanelHeight + 25).toInt() // +25 bao gồm khoảng cách 5px và mở rộng thêm 10px dưới button panel
            )
            
            graphComponent.add(hoverPanel, 0) // Thêm vào vị trí z-index cao nhất
            hoverPanel?.isVisible = true
            graphComponent.revalidate()
            graphComponent.repaint()
            println("showHoverPanel: Panel displayed at ($screenX, $screenY)")
            println("safeZone: $safeZone")
        } else {
            println("showHoverPanel: Could not get state for cell $cellId")
        }
    }

    private fun hideHoverPanel() {
        hoverPanel?.let {
            graphComponent.remove(it)
            graphComponent.revalidate()
            graphComponent.repaint()
            println("hideHoverPanel: Panel hidden")
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