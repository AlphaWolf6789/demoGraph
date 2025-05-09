package com.example.demograph

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.border.EmptyBorder

class NodeAddDialog(
    private val parent: JComponent,
    private val graphManager: GraphManager,
    private val x: Double,
    private val y: Double
) : JDialog() {
    private val stepIdField = JTextField(20)
    private val screenIdField = JTextField(20)
    private val guideContentArea = JTextArea(5, 20)
    private val previousStepComboBox = JComboBox<String>()
    private val nextStepComboBox = JComboBox<String>()
    
    init {
        title = "Add Step : Step details"
        
        // Set modal to true to block interaction with the parent window
        isModal = true
        
        // Create and set up the components
        setupComponents()
        
        // Add components to the dialog
        createLayout()
        
        // Set dialog properties
        setSize(600, 400)
        setLocationRelativeTo(parent)
    }
    
    private fun setupComponents() {
        // Set up the text areas
        guideContentArea.lineWrap = true
        guideContentArea.wrapStyleWord = true
        
        // Populate the node lists for previous and next steps
        populateNodeLists()
        
        // Add hint texts
        stepIdField.text = "e.g.Onboarding"
        screenIdField.text = "android.app.home.UISelect.galleryselect"
        guideContentArea.text = "Step description to be shown to the user"
    }
    
    private fun populateNodeLists() {
        // Get all node IDs
        val nodeIds = graphManager.getAllNodeIds()
        
        // Add empty selection first
        previousStepComboBox.addItem("Select Previous Step")
        nextStepComboBox.addItem("Select Next Step (Optional)")
        
        // Add all node IDs
        for (id in nodeIds) {
            previousStepComboBox.addItem(id)
            nextStepComboBox.addItem(id)
        }
    }
    
    private fun createLayout() {
        val panel = JPanel(GridBagLayout())
        panel.border = EmptyBorder(10, 10, 10, 10)
        
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
            anchor = GridBagConstraints.WEST
        }
        
        // Step ID
        constraints.gridx = 0
        constraints.gridy = 0
        panel.add(JLabel("Step ID"), constraints)
        
        constraints.gridx = 1
        constraints.gridy = 0
        constraints.weightx = 1.0
        panel.add(stepIdField, constraints)
        
        // Screen ID
        constraints.gridx = 0
        constraints.gridy = 1
        constraints.weightx = 0.0
        panel.add(JLabel("Screen ID"), constraints)
        
        constraints.gridx = 1
        constraints.gridy = 1
        constraints.weightx = 1.0
        panel.add(screenIdField, constraints)
        
        // Guide Content
        constraints.gridx = 0
        constraints.gridy = 2
        constraints.weightx = 0.0
        panel.add(JLabel("Guide Content"), constraints)
        
        constraints.gridx = 1
        constraints.gridy = 2
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.BOTH
        constraints.weighty = 1.0
        panel.add(JScrollPane(guideContentArea), constraints)
        
        // Previous Step
        constraints.gridx = 0
        constraints.gridy = 3
        constraints.weightx = 0.0
        constraints.weighty = 0.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        panel.add(JLabel("Previous Step"), constraints)
        
        constraints.gridx = 1
        constraints.gridy = 3
        constraints.weightx = 1.0
        panel.add(previousStepComboBox, constraints)
        
        // Next Step
        constraints.gridx = 0
        constraints.gridy = 4
        constraints.weightx = 0.0
        panel.add(JLabel("Next Step"), constraints)
        
        constraints.gridx = 1
        constraints.gridy = 4
        constraints.weightx = 1.0
        panel.add(nextStepComboBox, constraints)
        
        // Buttons panel
        val buttonsPanel = JPanel()
        buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.X_AXIS)
        buttonsPanel.add(Box.createHorizontalGlue())
        
        val backButton = JButton("Back")
        backButton.addActionListener { dispose() }
        buttonsPanel.add(backButton)
        
        buttonsPanel.add(Box.createRigidArea(Dimension(10, 0)))
        
        val nextButton = JButton("Next")
        nextButton.addActionListener { addNodeAndClose() }
        buttonsPanel.add(nextButton)
        
        // Add main panel and buttons to dialog
        contentPane.add(panel, BorderLayout.CENTER)
        contentPane.add(buttonsPanel, BorderLayout.SOUTH)
    }
    
    private fun addNodeAndClose() {
        // Get values from form
        val stepId = stepIdField.text.takeIf { it != "e.g.Onboarding" }
        val previousStepId = previousStepComboBox.selectedItem?.toString()
            .takeIf { it != "Select Previous Step" }
        val nextStepId = nextStepComboBox.selectedItem?.toString()
            .takeIf { it != "Select Next Step (Optional)" }
        
        // Validate that Previous Step is selected
        if (previousStepId == null) {
            JOptionPane.showMessageDialog(this, 
                "Previous Step is required to link nodes in the graph.",
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE)
            return
        }
        
        // Add the node with the collected data
        graphManager.addNode(x, y, previousStepId, nextStepId)
        
        // Close the dialog
        dispose()
    }
} 