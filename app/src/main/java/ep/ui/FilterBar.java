package ep.ui;

import ep.model.CalendarFilter;
import ep.model.Course;
import ep.model.ItemType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Search and filter controls for use case 4: keyword, course, type and a
 * date range, plus a Clear button that brings the whole calendar back.
 */
public class FilterBar extends JPanel {

    private final JTextField keyword = new JTextField(16);
    private CourseCombo courseCombo = new CourseCombo(List.of(), "All courses");
    private final JComboBox<Object> typeCombo = new JComboBox<>(new Object[]{"All types", ItemType.EVENT, ItemType.STUDY_BLOCK});
    private final JTextField from = new JTextField(9);
    private final JTextField to = new JTextField(9);
    private final JButton clear = new JButton("Clear");
    private final JPanel row = new JPanel();
    private Runnable onChange = () -> { };
    private boolean muted;

    /** Creates the bar. */
    public FilterBar() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        keyword.putClientProperty("JTextField.placeholderText", "Search title\u2026");
        keyword.setToolTipText("Search by a word in the title");
        from.setToolTipText("From date (yyyy-MM-dd)");
        to.setToolTipText("To date (yyyy-MM-dd)");
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { fire(); }
            public void removeUpdate(DocumentEvent e) { fire(); }
            public void changedUpdate(DocumentEvent e) { fire(); }
        };
        keyword.getDocument().addDocumentListener(dl);
        from.getDocument().addDocumentListener(dl);
        to.getDocument().addDocumentListener(dl);
        typeCombo.addActionListener(e -> fire());
        clear.addActionListener(e -> clearAll());
        build();
        add(row);
    }

    private void build() {
        row.removeAll();
        row.add(label("Search"));
        row.add(keyword);
        row.add(Box.createHorizontalStrut(8));
        row.add(courseCombo);
        row.add(Box.createHorizontalStrut(8));
        row.add(typeCombo);
        row.add(Box.createHorizontalStrut(8));
        row.add(label("From"));
        row.add(from);
        row.add(label("To"));
        row.add(to);
        row.add(Box.createHorizontalStrut(8));
        row.add(clear);
        for (java.awt.Component c : row.getComponents()) {
            if (c instanceof javax.swing.JComponent) {
                Dimension p = c.getPreferredSize();
                c.setMaximumSize(new Dimension(p.width, 28));
            }
        }
        row.revalidate();
        row.repaint();
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UiTheme.MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        return l;
    }

    private void fire() {
        if (!muted) {
            onChange.run();
        }
    }

    /** @param listener called whenever a filter value changes */
    public void setOnChange(Runnable listener) {
        this.onChange = listener;
    }

    /**
     * Refreshes the course list while keeping the selected course.
     *
     * @param courses current courses
     */
    public void setCourses(List<Course> courses) {
        String selected = courseCombo.getSelectedCourseId();
        muted = true;
        row.remove(courseCombo);
        courseCombo = new CourseCombo(courses, "All courses");
        courseCombo.setSelectedCourseId(selected);
        courseCombo.addActionListener(e -> fire());
        build();
        muted = false;
    }

    /**
     * Selects a course in the filter (used when a course is picked in the
     * sidebar).
     *
     * @param courseId course id or {@code null}
     */
    public void selectCourse(String courseId) {
        muted = true;
        courseCombo.setSelectedCourseId(courseId);
        muted = false;
        fire();
    }

    /** Resets every control and fires one change event. */
    public void clearAll() {
        muted = true;
        keyword.setText("");
        courseCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        from.setText("");
        to.setText("");
        muted = false;
        fire();
    }

    /** @return the current filter built from the controls */
    public CalendarFilter getFilter() {
        Object t = typeCombo.getSelectedItem();
        return new CalendarFilter()
                .keyword(keyword.getText())
                .course(courseCombo.getSelectedCourseId())
                .type(t instanceof ItemType ? (ItemType) t : null)
                .dateRange(parse(from.getText()), parse(to.getText()));
    }

    /** @return the selected course id or {@code null} */
    public String getSelectedCourseId() {
        return courseCombo.getSelectedCourseId();
    }

    private static LocalDate parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
