package com.burp.unireq.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * SwingUtils - Utility functions for Swing GUI components
 * 
 * This class provides common utility functions for working with Swing
 * components in the UniReq extension. It includes methods for creating
 * consistent UI elements, handling events, and managing layouts.
 * 
 * @author Harshit Shah
 */
public class SwingUtils {
    
    // Standard colors for the extension
    public static final Color SUCCESS_COLOR = new Color(46, 125, 50);
    public static final Color WARNING_COLOR = new Color(255, 152, 0);
    public static final Color ERROR_COLOR = new Color(211, 47, 47);
    public static final Color INFO_COLOR = new Color(25, 118, 210);
    
    // Standard fonts
    public static final Font MONOSPACE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Font BOLD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    
    /**
     * Creates a standardized button with consistent styling.
     * 
     * @param text The button text
     * @param tooltip The tooltip text
     * @param listener The action listener
     * @return A configured JButton
     */
    public static JButton createButton(String text, String tooltip, ActionListener listener) {
        JButton button = new JButton(text);
        if (tooltip != null && !tooltip.isEmpty()) {
            button.setToolTipText(tooltip);
        }
        if (listener != null) {
            button.addActionListener(listener);
        }
        button.setFocusPainted(false);
        return button;
    }
    
    /**
     * Creates a standardized button with an icon.
     * 
     * @param text The button text
     * @param tooltip The tooltip text
     * @param listener The action listener
     * @param iconPath Path to the icon resource (can be null)
     * @return A configured JButton
     */
    public static JButton createButton(String text, String tooltip, ActionListener listener, String iconPath) {
        JButton button = createButton(text, tooltip, listener);
        
        if (iconPath != null) {
            try {
                // Try to load icon from resources
                java.net.URL iconUrl = SwingUtils.class.getResource(iconPath);
                if (iconUrl != null) {
                    ImageIcon icon = new ImageIcon(iconUrl);
                    button.setIcon(icon);
                }
            } catch (Exception e) {
                // Ignore icon loading errors
            }
        }
        
        return button;
    }
    
    /**
     * Creates a labeled text field with consistent styling.
     * 
     * @param labelText The label text
     * @param fieldText The initial field text
     * @param columns The number of columns for the text field
     * @return A JPanel containing the label and text field
     */
    public static JPanel createLabeledTextField(String labelText, String fieldText, int columns) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel label = new JLabel(labelText);
        JTextField field = new JTextField(fieldText, columns);
        
        panel.add(label);
        panel.add(field);
        
        return panel;
    }
    
    /**
     * Creates a combo box with consistent styling.
     * 
     * @param items The items for the combo box
     * @param selectedItem The initially selected item
     * @return A configured JComboBox
     */
    public static JComboBox<String> createComboBox(String[] items, String selectedItem) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        if (selectedItem != null) {
            comboBox.setSelectedItem(selectedItem);
        }
        return comboBox;
    }
    
    /**
     * Creates a checkbox with consistent styling.
     * 
     * @param text The checkbox text
     * @param selected The initial selection state
     * @param tooltip The tooltip text
     * @return A configured JCheckBox
     */
    public static JCheckBox createCheckBox(String text, boolean selected, String tooltip) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        if (tooltip != null && !tooltip.isEmpty()) {
            checkBox.setToolTipText(tooltip);
        }
        return checkBox;
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
     * Creates a titled border panel.
     * 
     * @param title The border title
     * @param content The content component
     * @return A JPanel with titled border
     */
    public static JPanel createTitledPanel(String title, Component content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Creates a horizontal separator.
     * 
     * @return A JSeparator configured for horizontal layout
     */
    public static JSeparator createHorizontalSeparator() {
        return new JSeparator(SwingConstants.HORIZONTAL);
    }
    
    /**
     * Creates a vertical separator.
     * 
     * @return A JSeparator configured for vertical layout
     */
    public static JSeparator createVerticalSeparator() {
        return new JSeparator(SwingConstants.VERTICAL);
    }
    
    /**
     * Shows a confirmation dialog with standard styling.
     * 
     * @param parent The parent component
     * @param message The confirmation message
     * @param title The dialog title
     * @return true if user confirms, false otherwise
     */
    public static boolean showConfirmDialog(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(
            parent,
            message,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }
    
    /**
     * Shows an error dialog with standard styling.
     * 
     * @param parent The parent component
     * @param message The error message
     * @param title The dialog title
     */
    public static void showErrorDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            title,
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    /**
     * Shows an information dialog with standard styling.
     * 
     * @param parent The parent component
     * @param message The information message
     * @param title The dialog title
     */
    public static void showInfoDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            title,
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Centers a window on the screen.
     * 
     * @param window The window to center
     */
    public static void centerWindow(Window window) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = window.getSize();
        
        int x = (screenSize.width - windowSize.width) / 2;
        int y = (screenSize.height - windowSize.height) / 2;
        
        window.setLocation(x, y);
    }
    
    /**
     * Sets the look and feel to system default if possible.
     */
    public static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignore - will use default look and feel
        }
    }
    
    /**
     * Creates a progress bar with indeterminate state.
     * 
     * @param text The progress text
     * @return A configured JProgressBar
     */
    public static JProgressBar createProgressBar(String text) {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString(text);
        progressBar.setStringPainted(true);
        return progressBar;
    }
    
    /**
     * Creates a text area with scroll pane.
     * 
     * @param text The initial text
     * @param rows The number of rows
     * @param columns The number of columns
     * @param editable Whether the text area is editable
     * @return A JScrollPane containing the text area
     */
    public static JScrollPane createScrollableTextArea(String text, int rows, int columns, boolean editable) {
        JTextArea textArea = new JTextArea(text, rows, columns);
        textArea.setEditable(editable);
        textArea.setFont(MONOSPACE_FONT);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        return scrollPane;
    }
    
    /**
     * Enumeration for status label types.
     */
    public enum StatusType {
        SUCCESS, WARNING, ERROR, INFO, DEFAULT
    }
    
    /**
     * Creates a simple action listener that runs a Runnable.
     * 
     * @param action The action to run
     * @return An ActionListener
     */
    public static ActionListener createActionListener(Runnable action) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (action != null) {
                    action.run();
                }
            }
        };
    }
    
    /**
     * Safely runs a task on the EDT (Event Dispatch Thread).
     * 
     * @param task The task to run
     */
    public static void runOnEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
    
    /**
     * Safely runs a task on the EDT and waits for completion.
     * 
     * @param task The task to run
     */
    public static void runOnEDTAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (Exception e) {
                // Handle the exception appropriately
                e.printStackTrace();
            }
        }
    }
} 