package ep.ui;

import ep.model.Course;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sidebar section for use case 1: the course list with Add / Edit / Delete
 * buttons. Selecting a course also acts as a calendar filter.
 */
public class CoursePanel extends JPanel {

    private final DefaultListModel<Course> model = new DefaultListModel<>();
    private final JList<Course> list = new JList<>(model);
    private final JButton edit = new JButton("Edit");
    private final JButton delete = new JButton("Delete");

    /**
     * @param onAdd      called when Add is pressed
     * @param onEdit     called with the selected course when Edit is pressed
     * @param onDelete   called with the selected course when Delete is pressed
     * @param onSelected called with the selected course (or {@code null}) when the selection changes
     */
    public CoursePanel(Runnable onAdd, Consumer<Course> onEdit, Consumer<Course> onDelete,
                       Consumer<Course> onSelected) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        JLabel title = new JLabel("Courses");
        title.setFont(UiTheme.BOLD);
        title.setForeground(UiTheme.TEXT);

        JButton add = new JButton("+ Add");
        add.addActionListener(e -> onAdd.run());
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(add, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                Course c = (Course) value;
                setText(c.getName());
                setIcon(new CourseCombo.DotIcon(UiTheme.parse(c.getColor())));
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                return this;
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean has = list.getSelectedValue() != null;
                edit.setEnabled(has);
                delete.setEnabled(has);
                onSelected.accept(list.getSelectedValue());
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.LINE));
        scroll.setPreferredSize(new Dimension(230, 150));
        add(scroll, BorderLayout.CENTER);

        edit.addActionListener(e -> onEdit.accept(list.getSelectedValue()));
        delete.addActionListener(e -> onDelete.accept(list.getSelectedValue()));
        edit.setEnabled(false);
        delete.setEnabled(false);
        JButton clear = new JButton("Show all");
        clear.setToolTipText("Clear the course selection");
        clear.addActionListener(e -> list.clearSelection());
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(edit);
        buttons.add(Box.createHorizontalStrut(4));
        buttons.add(delete);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(clear);
        add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Refreshes the list, keeping the selection if the course still exists.
     *
     * @param courses current courses
     */
    public void setCourses(List<Course> courses) {
        Course selected = list.getSelectedValue();
        model.clear();
        for (Course c : courses) {
            model.addElement(c);
        }
        if (selected != null) {
            for (int i = 0; i < model.size(); i++) {
                if (model.get(i).getId().equals(selected.getId())) {
                    list.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    /** @return the selected course or {@code null} */
    public Course getSelected() {
        return list.getSelectedValue();
    }

    /** Clears the selection. */
    public void clearSelection() {
        list.clearSelection();
    }
}
