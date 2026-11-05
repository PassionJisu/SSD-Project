package ep.ui;

import ep.model.Course;
import ep.service.CourseManager;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * Dialog for adding or editing a course: a name and a color chosen from a
 * palette (or any color through the system color chooser).
 */
public class CourseDialog extends FormDialog {

    private final CourseManager courses;
    private final Course existing;
    private final JTextField nameField = new JTextField(22);
    private String color;
    private Course result;

    /**
     * @param owner    parent window
     * @param courses  course manager
     * @param existing course to edit, or {@code null} to add
     */
    public CourseDialog(Window owner, CourseManager courses, Course existing) {
        super(owner, existing == null ? "Add course" : "Edit course",
                existing == null ? "New course" : "Edit course");
        this.courses = courses;
        this.existing = existing;
        this.color = existing == null ? UiTheme.PALETTE[courses.getCourses().size() % UiTheme.PALETTE.length]
                : existing.getColor();
        if (existing != null) {
            nameField.setText(existing.getName());
        }

        JPanel form = Fields.form();
        Fields.addRow(form, 0, "Name", nameField);
        Fields.addRow(form, 1, "Color", paletteRow());
        setForm(form);
    }

    private JPanel paletteRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (String hex : UiTheme.PALETTE) {
            JToggleButton b = swatch(UiTheme.parse(hex));
            b.setSelected(hex.equalsIgnoreCase(color));
            b.addActionListener(e -> color = hex);
            group.add(b);
            row.add(b);
        }
        JToggleButton custom = new JToggleButton("\u2026");
        custom.setPreferredSize(new Dimension(26, 26));
        custom.setMargin(new java.awt.Insets(0, 0, 0, 0));
        custom.setToolTipText("Pick any color");
        custom.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Course color", UiTheme.parse(color));
            if (c != null) {
                color = UiTheme.toHex(c);
                custom.setBackground(c);
            }
        });
        group.add(custom);
        row.add(custom);
        return row;
    }

    private static JToggleButton swatch(Color c) {
        JToggleButton b = new JToggleButton();
        b.setPreferredSize(new Dimension(26, 26));
        b.setBackground(c);
        b.setOpaque(true);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createLineBorder(UiTheme.shade(c, 0.25), 1));
        b.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent comp) {
                JToggleButton t = (JToggleButton) comp;
                g.setColor(c);
                g.fillRoundRect(3, 3, comp.getWidth() - 6, comp.getHeight() - 6, 8, 8);
                if (t.isSelected()) {
                    g.setColor(UiTheme.TEXT);
                    ((java.awt.Graphics2D) g).setStroke(new java.awt.BasicStroke(2f));
                    g.drawRoundRect(1, 1, comp.getWidth() - 3, comp.getHeight() - 3, 10, 10);
                }
            }
        });
        return b;
    }

    @Override
    protected void apply() {
        if (existing == null) {
            result = courses.addCourse(nameField.getText(), color);
        } else {
            result = courses.editCourse(existing.getId(), nameField.getText(), color);
        }
    }

    /** @return the saved course, or {@code null} if cancelled */
    public Course getResult() {
        return result;
    }
}
