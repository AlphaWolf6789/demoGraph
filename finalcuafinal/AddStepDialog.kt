package com.example.finalcuafinal

import com.mxgraph.model.mxCell
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
 * A utility class for handling the add step dialog
 */
class AddStepDialog {
    companion object {
        /**
         * Shows a dialog to add a new step in the flow
         *
         * @param parent The parent component (NodeGraph) that contains the add button
         * @param sourceCell The cell representing the add button that triggered this dialog
         * @param createStepNodeCallback The callback function to create a step node from name and guide text
         */
        fun showAddStepDialog(parent: JPanel, sourceCell: mxCell, createStepNodeCallback: (mxCell, String, String) -> Unit) {
            val dialog = JDialog()
            dialog.title = "Add Step"
            dialog.isModal = true
            dialog.background = Color(30, 32, 35)
            dialog.setUndecorated(true)
            
            // Set dialog size
            dialog.preferredSize = Dimension(400, 250)
            
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.background = Color(30, 32, 35)
            panel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
            
            // Create title label
            val titleLabel = JLabel("Add Step")
            titleLabel.foreground = Color.WHITE
            titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
            titleLabel.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            panel.add(titleLabel)
            
            // Add spacing
            panel.add(javax.swing.Box.createVerticalStrut(15))
            
            // Create step name field
            val stepNameField = PlaceholderTextField("Step Name", 20)
            stepNameField.background = Color(50, 52, 55)
            stepNameField.foreground = Color.WHITE
            stepNameField.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 72, 75), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
            stepNameField.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            stepNameField.maximumSize = Dimension(360, 40)
            panel.add(stepNameField)
            
            // Add spacing
            panel.add(javax.swing.Box.createVerticalStrut(10))
            
            // Create guide text field
            val guideTextField = PlaceholderTextField("Tap on the video you want to edit", 20)
            guideTextField.background = Color(50, 52, 55)
            guideTextField.foreground = Color.WHITE
            guideTextField.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 72, 75), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
            guideTextField.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            guideTextField.maximumSize = Dimension(360, 40)
            panel.add(guideTextField)
            
            // Add spacing
            panel.add(javax.swing.Box.createVerticalStrut(20))
            
            // Create button panel
            val buttonPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.RIGHT))
            buttonPanel.background = Color(30, 32, 35)
            
            // Action to be executed when adding a new step
            val addStepAction = {
                val stepName = stepNameField.getActualText()
                val guideText = guideTextField.getActualText()
                
                if (stepName.isNotEmpty()) {
                    createStepNodeCallback(sourceCell, stepName, guideText)
                    dialog.dispose()
                }
            }
            
            // Create buttons
            val addButton = createRoundedButton("Add", Color(99, 102, 241))
            addButton.addActionListener { addStepAction() }
            
            val cancelButton = createRoundedButton("Cancel", Color(60, 62, 65))
            cancelButton.addActionListener { dialog.dispose() }
            
            // Add key binding for Enter key
            val inputMap = stepNameField.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
            inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "enter")
            stepNameField.actionMap.put("enter", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent) {
                    addStepAction()
                }
            })
            
            // Add key binding for Enter key on guide field too
            val guideInputMap = guideTextField.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
            guideInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "enter")
            guideTextField.actionMap.put("enter", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent) {
                    addStepAction()
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