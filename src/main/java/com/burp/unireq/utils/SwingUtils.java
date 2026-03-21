package com.burp.unireq.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * SwingUtils - Utility functions for Swing GUI components
 *
 * @author Harshit Shah
 */
public class SwingUtils {

    // Standard colors for the extension
    public static final Color SUCCESS_COLOR = new Color(46, 125, 50);
    public static final Color WARNING_COLOR = new Color(255, 152, 0);
    public static final Color ERROR_COLOR = new Color(211, 47, 47);
    public static final Color INFO_COLOR = new Color(25, 118, 210);

    // Modern UI constants
    public static final int BORDER_RADIUS = 8;
    public static final Color BORDER_COLOR = new Color(200, 200, 200);
    public static final Color HOVER_COLOR = new Color(245, 245, 245);

    /**
     * Creates a rounded border for modern UI components.
     *
     * @param radius The border radius in pixels
     * @param color  The border color
     * @return A rounded border
     */
    public static javax.swing.border.Border createRoundedBorder(int radius, Color color) {
        return new javax.swing.border.AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
                g2.dispose();
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(4, 8, 4, 8);
            }
        };
    }

    /**
     * Creates a modern flat button with rounded corners and hover effects.
     *
     * @param text     The button text
     * @param tooltip  The tooltip text
     * @param listener The action listener
     * @return A modern styled JButton
     */
    public static JButton createModernButton(String text, String tooltip, ActionListener listener) {
        JButton button = new JButton(text);
        if (tooltip != null && !tooltip.isEmpty()) {
            button.setToolTipText(tooltip);
        }
        if (listener != null) {
            button.addActionListener(listener);
        }

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        button.setBorder(createRoundedBorder(BORDER_RADIUS, BORDER_COLOR));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(HOVER_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });

        return button;
    }

    /**
     * Creates a modern text field with rounded borders.
     *
     * @param placeholder The placeholder text
     * @param columns     The number of columns
     * @return A modern styled JTextField
     */
    public static JTextField createModernTextField(String placeholder, int columns) {
        JTextField field = new JTextField(columns);
        field.setBorder(createRoundedBorder(BORDER_RADIUS, BORDER_COLOR));
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        if (placeholder != null && !placeholder.isEmpty()) {
            field.setToolTipText(placeholder);
        }

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(createRoundedBorder(BORDER_RADIUS, INFO_COLOR));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(createRoundedBorder(BORDER_RADIUS, BORDER_COLOR));
            }
        });

        return field;
    }

    /**
     * Creates a modern combo box with rounded borders.
     *
     * @param items        The items for the combo box
     * @param selectedItem The initially selected item
     * @return A modern styled JComboBox
     */
    public static JComboBox<String> createModernComboBox(String[] items, String selectedItem) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        if (selectedItem != null) {
            comboBox.setSelectedItem(selectedItem);
        }

        comboBox.setBorder(createRoundedBorder(BORDER_RADIUS, BORDER_COLOR));
        comboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        comboBox.setBackground(Color.WHITE);

        return comboBox;
    }

    /**
     * Creates a status label with color coding.
     *
     * @param text The status text
     * @param type The status type (success, warning, error, info)
     * @return A configured JLabel
     */
    public static JLabel createStatusLabel(String text, StatusType type) {
        JLabel label = new JLabel(text);

        switch (type) {
            case SUCCESS:
                label.setForeground(SUCCESS_COLOR);
                break;
            case WARNING:
                label.setForeground(WARNING_COLOR);
                break;
            case ERROR:
                label.setForeground(ERROR_COLOR);
                break;
            case INFO:
                label.setForeground(INFO_COLOR);
                break;
            default:
                break;
        }

        return label;
    }

    /**
     * Enumeration for status label types.
     */
    public enum StatusType {
        SUCCESS, WARNING, ERROR, INFO
    }
}
