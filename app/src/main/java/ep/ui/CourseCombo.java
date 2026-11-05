package ep.ui;

import ep.model.Course;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

/**
 * Drop-down of courses with a leading "(no course)" entry and a color dot
 * in front of every name.
 */
public class CourseCombo extends JComboBox<Object> {

    private static final String NONE = "(no course)";

    /**
     * @param courses   courses to offer
     * @param noneLabel label of the empty entry (e.g. "All courses")
     */
    public CourseCombo(List<Course> courses, String noneLabel) {
        addItem(noneLabel == null ? NONE : noneLabel);
        for (Course c : courses) {
            addItem(c);
        }
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Course) {
                    Course c = (Course) value;
                    setText(c.getName());
                    setIcon(new DotIcon(UiTheme.parse(c.getColor())));
                } else {
                    setIcon(new DotIcon(null));
                }
                return this;
            }
        });
    }

    /** @param courses courses to offer */
    public CourseCombo(List<Course> courses) {
        this(courses, null);
    }

    /** @return the selected course id, or {@code null} for the empty entry */
    public String getSelectedCourseId() {
        Object v = getSelectedItem();
        return v instanceof Course ? ((Course) v).getId() : null;
    }

    /** @param courseId id to select; {@code null} selects the empty entry */
    public void setSelectedCourseId(String courseId) {
        if (courseId == null) {
            setSelectedIndex(0);
            return;
        }
        for (int i = 1; i < getItemCount(); i++) {
            Object v = getItemAt(i);
            if (v instanceof Course && ((Course) v).getId().equals(courseId)) {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(0);
    }

    /** Small round color icon. */
    public static class DotIcon implements javax.swing.Icon {
        private final Color color;

        /** @param color dot color, or {@code null} for an outline only */
        public DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            if (color == null) {
                g2.setColor(UiTheme.LINE);
                g2.drawOval(x + 1, y + 1, 9, 9);
            } else {
                g2.setColor(color);
                g2.fillOval(x + 1, y + 1, 10, 10);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }
}
