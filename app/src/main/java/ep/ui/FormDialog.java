package ep.ui;

import ep.model.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Modal dialog skeleton with a heading, a form area and OK / Cancel buttons.
 * Subclasses build the form and implement {@link #apply()}; a
 * {@link ValidationException} thrown there is shown to the student and the
 * dialog stays open, so invalid input never reaches the saved data.
 */
public abstract class FormDialog extends JDialog {

    private boolean accepted;
    private final JPanel body = new JPanel(new BorderLayout());
    private final JButton okButton = new JButton("Save");

    /**
     * @param owner   parent window
     * @param title   window title
     * @param heading text shown at the top of the dialog
     */
    protected FormDialog(Window owner, String title, String heading) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        root.setBackground(UiTheme.SURFACE);
        JLabel head = new JLabel(heading);
        head.setFont(UiTheme.TITLE.deriveFont(16f));
        head.setForeground(UiTheme.TEXT);
        root.add(head, BorderLayout.NORTH);
        body.setOpaque(false);
        root.add(body, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        okButton.addActionListener(e -> onOk());
        buttons.add(cancel);
        buttons.add(okButton);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(okButton);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * @param form the form component built by the subclass
     */
    protected void setForm(JComponent form) {
        body.removeAll();
        body.add(form, BorderLayout.CENTER);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    /** @param text label of the OK button */
    protected void setOkText(String text) {
        okButton.setText(text);
    }

    /**
     * Performs the change. Throw {@link ValidationException} to reject the
     * input and keep the dialog open.
     */
    protected abstract void apply();

    private void onOk() {
        try {
            apply();
            accepted = true;
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Please check the input",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** @return {@code true} if the student pressed OK and the change was applied */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Shows the dialog and waits.
     *
     * @return {@code true} if the change was applied
     */
    public boolean showDialog() {
        setVisible(true);
        return accepted;
    }
}
