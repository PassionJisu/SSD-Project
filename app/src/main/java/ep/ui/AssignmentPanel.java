package ep.ui;

import ep.model.Assignment;
import ep.model.Course;
import ep.model.ProgressReport;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sidebar section for use cases 3 and 5: the assignment list with a progress
 * bar per row and buttons to add, edit, delete, record progress and mark
 * complete.
 */
public class AssignmentPanel extends JPanel {

    private final DefaultListModel<Assignment> model = new DefaultListModel<>();
    private final JList<Assignment> list = new JList<>(model);
    private final JButton edit = new JButton("Edit");
    private final JButton delete = new JButton("Delete");
    private final JButton progress = new JButton("Progress\u2026");
    private final JButton complete = new JButton("Complete");
    private Function<Assignment, ProgressReport> reports = a -> new ProgressReport(a, 0);
    private Function<String, Optional<Course>> courses = id -> Optional.empty();
    private LocalDate today = LocalDate.now();

    /**
     * @param onAdd      called when Add is pressed
     * @param onEdit     called with the selected assignment when Edit is pressed
     * @param onDelete   called with the selected assignment when Delete is pressed
     * @param onProgress called with the selected assignment when Progress is pressed
     * @param onComplete called with the selected assignment when Complete is pressed
     */
    public AssignmentPanel(Runnable onAdd, Consumer<Assignment> onEdit, Consumer<Assignment> onDelete,
                           Consumer<Assignment> onProgress, Consumer<Assignment> onComplete) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        JLabel title = new JLabel("Assignments");
        title.setFont(UiTheme.BOLD);
        title.setForeground(UiTheme.TEXT);
        JButton add = new JButton("+ Plan");
        add.addActionListener(e -> onAdd.run());
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(add, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new Renderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Assignment a = list.getSelectedValue();
                boolean has = a != null;
                edit.setEnabled(has);
                delete.setEnabled(has);
                progress.setEnabled(has && !a.isComplete());
                complete.setEnabled(has && !a.isComplete());
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    onProgress.accept(list.getSelectedValue());
                }
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.LINE));
        scroll.setPreferredSize(new Dimension(230, 240));
        add(scroll, BorderLayout.CENTER);

        edit.addActionListener(e -> onEdit.accept(list.getSelectedValue()));
        delete.addActionListener(e -> onDelete.accept(list.getSelectedValue()));
        progress.addActionListener(e -> onProgress.accept(list.getSelectedValue()));
        complete.addActionListener(e -> onComplete.accept(list.getSelectedValue()));
        for (JButton b : new JButton[]{edit, delete, progress, complete}) {
            b.setEnabled(false);
        }
        JPanel row1 = new JPanel();
        row1.setOpaque(false);
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.add(progress);
        row1.add(Box.createHorizontalStrut(4));
        row1.add(complete);
        row1.add(Box.createHorizontalGlue());
        row1.add(edit);
        row1.add(Box.createHorizontalStrut(4));
        row1.add(delete);
        add(row1, BorderLayout.SOUTH);
    }

    /**
     * Refreshes the list.
     *
     * @param assignments current assignments
     * @param reports     function giving the progress of an assignment
     * @param courses     course lookup for colors
     * @param today       today's date (for the "days left" text)
     */
    public void setAssignments(List<Assignment> assignments, Function<Assignment, ProgressReport> reports,
                               Function<String, Optional<Course>> courses, LocalDate today) {
        this.reports = reports;
        this.courses = courses;
        this.today = today;
        Assignment selected = list.getSelectedValue();
        model.clear();
        for (Assignment a : assignments) {
            model.addElement(a);
        }
        if (selected != null) {
            for (int i = 0; i < model.size(); i++) {
                if (model.get(i).getId().equals(selected.getId())) {
                    list.setSelectedIndex(i);
                    break;
                }
            }
        }
        list.repaint();
    }

    /** @return the selected assignment or {@code null} */
    public Assignment getSelected() {
        return list.getSelectedValue();
    }

    /** Custom row: title, course + due, and a thin progress bar. */
    private class Renderer extends JPanel implements ListCellRenderer<Assignment> {
        private Assignment value;
        private boolean selected;

        Renderer() {
            setPreferredSize(new Dimension(220, 54));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Assignment> l, Assignment v, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            value = v;
            selected = isSelected;
            setBackground(isSelected ? UiTheme.TODAY_FILL : UiTheme.SURFACE);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (value == null) {
                return;
            }
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ProgressReport r = reports.apply(value);
            Color base = value.getCourseId() == null ? UiTheme.UNLINKED
                    : courses.apply(value.getCourseId()).map(c -> UiTheme.parse(c.getColor())).orElse(UiTheme.UNLINKED);
            int w = getWidth();
            g.setColor(base);
            g.fillRoundRect(8, 10, 5, getHeight() - 20, 4, 4);

            g.setFont(UiTheme.BOLD);
            g.setColor(value.isComplete() ? UiTheme.MUTED : UiTheme.TEXT);
            String t = value.isComplete() ? "\u2713 " + value.getTitle() : value.getTitle();
            g.drawString(UiTheme.clipText(g, t, w - 80), 20, 20);

            g.setFont(UiTheme.SMALL);
            g.setColor(UiTheme.MUTED);
            long days = ChronoUnit.DAYS.between(today, value.getDueDate());
            String due = value.isComplete() ? "complete"
                    : days < 0 ? "overdue" : days == 0 ? "due today" : "due in " + days + " d";
            String course = value.getCourseId() == null ? "" :
                    courses.apply(value.getCourseId()).map(Course::getName).orElse("") + " \u00b7 ";
            g.drawString(UiTheme.clipText(g, course + value.getDueDate() + " \u00b7 " + due, w - 80), 20, 36);

            String pct = r.getCompletedHours() + "/" + r.getEstimatedHours() + " h";
            g.setFont(UiTheme.SMALL_BOLD);
            g.setColor(r.hasShortage() ? new Color(0xC2410C) : UiTheme.TEXT);
            g.drawString(pct, w - 10 - g.getFontMetrics().stringWidth(pct), 20);

            int bx = 20;
            int bw = w - 30;
            g.setColor(UiTheme.LINE);
            g.fillRoundRect(bx, 43, bw, 5, 5, 5);
            g.setColor(value.isComplete() ? new Color(0x2BA47A) : base);
            g.fillRoundRect(bx, 43, (int) Math.round(bw * r.getRatio()), 5, 5, 5);
            if (!selected) {
                g.setColor(UiTheme.LINE);
                g.drawLine(0, getHeight() - 1, w, getHeight() - 1);
            }
        }
    }
}
