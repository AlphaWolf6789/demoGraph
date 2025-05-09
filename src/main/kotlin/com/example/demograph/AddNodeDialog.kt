package com.example.demograph

import com.mxgraph.model.mxCell
import com.mxgraph.swing.mxGraphComponent
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.border.EmptyBorder

/**
 * A utility class for handling the add node dialog
 */
class AddNodeDialog {
    companion object {
        /**
         * Shows a dialog to add a new node in the graph
         *
         * @param parent The parent component (GraphComponent) that contains the add button
         * @param sourceCell The cell representing the add button that triggered this dialog
         * @param createNodeCallback The callback function to create a node from name and content text
         */
        fun showAddNodeDialog(parent: mxGraphComponent, sourceCell: mxCell, createNodeCallback: (mxCell, String, String) -> Unit) {
            val dialog = JDialog()
            dialog.title = "Add Node"
            dialog.isModal = true
            dialog.background = Color(30, 32, 35)
            dialog.isUndecorated = true
            
            // Set dialog size
            dialog.preferredSize = Dimension(400, 250)
            
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.background = Color(30, 32, 35)
            panel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
            
            // Create title label
            val titleLabel = JLabel("Add Node")
            titleLabel.foreground = Color.WHITE
            titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
            titleLabel.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            panel.add(titleLabel)
            
            // Add spacing
            panel.add(javax.swing.Box.createVerticalStrut(15))
            
            // Create node name field
            val nodeNameField = PlaceholderTextField("Node Name", 20)
            nodeNameField.background = Color(50, 52, 55)
            nodeNameField.foreground = Color.WHITE
            nodeNameField.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 72, 75), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
            nodeNameField.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            nodeNameField.maximumSize = Dimension(360, 40)
            panel.add(nodeNameField)
            
            // Add spacing
            panel.add(javax.swing.Box.createVerticalStrut(10))
            
            // Create content text field
            val contentTextField = PlaceholderTextField("Node Content", 20)
            contentTextField.background = Color(50, 52, 55)
            contentTextField.foreground = Color.WHITE
            contentTextField.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 72, 75), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
            contentTextField.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            contentTextField.maximumSize = Dimension(360, 40)
            panel.add(contentTextField)
            
            // Add spacing
            panel.add(javax.swing.Box.createVerticalStrut(20))
            
            // Create button panel
            val buttonPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.RIGHT))
            buttonPanel.background = Color(30, 32, 35)
            
            // Action to be executed when adding a new node
            val addNodeAction = {
                val nodeName = nodeNameField.getActualText()
                val contentText = contentTextField.getActualText()
                
                if (nodeName.isNotEmpty()) {
                    createNodeCallback(sourceCell, nodeName, contentText)
                    dialog.dispose()
                }
            }
            
            // Create buttons
            val addButton = createRoundedButton("Add", Color(99, 102, 241))
            addButton.addActionListener { addNodeAction() }
            
            val cancelButton = createRoundedButton("Cancel", Color(60, 62, 65))
            cancelButton.addActionListener { dialog.dispose() }
            
            // Add key binding for Enter key
            val inputMap = nodeNameField.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
            inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "enter")
            nodeNameField.actionMap.put("enter", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent) {
                    addNodeAction()
                }
            })
            
            // Add key binding for Enter key on content field too
            val contentInputMap = contentTextField.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
            contentInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "enter")
            contentTextField.actionMap.put("enter", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent) {
                    addNodeAction()
                }
            })
            
            buttonPanel.add(addButton)
            buttonPanel.add(cancelButton)
            buttonPanel.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            
            panel.add(buttonPanel)
            
            // Add dialog border
            dialog.rootPane.border = BorderFactory.createLineBorder(Color(60, 62, 65), 1)
            
            dialog.contentPane = panel
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.isVisible = true
        }
        
        /**
         * Helper method to create rounded buttons
         */
        private fun createRoundedButton(text: String, bgColor: Color): JButton {
            val button = JButton(text)
            button.background = bgColor
            button.foreground = Color.WHITE
            button.isFocusPainted = false
            button.isContentAreaFilled = true
            button.isBorderPainted = false
            button.border = BorderFactory.createEmptyBorder(8, 20, 8, 20)
            button.font = button.font.deriveFont(Font.PLAIN, 12f)
            
            // Add rounded corners effect
            button.putClientProperty("JButton.buttonType", "roundRect")
            
            return button
        }
    }
} 