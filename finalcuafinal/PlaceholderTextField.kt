package com.example.finalcuafinal

import java.awt.Color
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextField

/**
 * A custom JTextField that displays placeholder text when empty
 */
class PlaceholderTextField(
    private val placeholder: String,
    columns: Int = 20
) : JTextField(columns) {

    private var showingPlaceholder = true

    init {
        text = placeholder
        foreground = Color(150, 150, 150) // Gray placeholder text

        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (showingPlaceholder) {
                    text = ""
                    foreground = Color.WHITE
                    showingPlaceholder = false
                }
            }

            override fun focusLost(e: FocusEvent) {
                if (text.isEmpty()) {
                    text = placeholder
                    foreground = Color(150, 150, 150)
                    showingPlaceholder = true
                }
            }
        })
    }

    /**
     * Returns the actual text (empty string if showing placeholder)
     */
    fun getActualText(): String {
        return if (showingPlaceholder) "" else text
    }
}