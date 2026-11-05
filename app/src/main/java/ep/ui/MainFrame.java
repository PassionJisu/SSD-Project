package ep.ui;

import ep.api.EnoughProcrastination;
import ep.model.Assignment;
import ep.model.CalendarFilter;
import ep.model.CalendarItem;
import ep.model.Course;
import ep.model.Event;
import ep.model.PlanResult;
import ep.model.ProgressReport;
import ep.model.StudyBlock;
import ep.model.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * The application window. It owns the five managers through the
 * {@link EnoughProcrastination} facade and connects them to the sidebar
 * (courses, assignments), the toolbar (navigation, view switch, filter) and
 * the month / week calendar views.
 */
public class MainFrame extends JFrame {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final EnoughProcrastination app;
    private final MonthView monthView = new MonthView();
    private final WeekView weekView = new WeekView();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel periodLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JLabel emptyBanner = new JLabel("Nothing was found for the current search / filter.");
    private final JToggleButton monthButton = new JToggleButton("Month");
    private final JToggleButton weekButton = new JToggleButton("Week");
    private final FilterBar filterBar = new FilterBar();
    private final CoursePanel coursePanel;
    private final AssignmentPanel assignmentPanel;
    private LocalDate anchor;
    private boolean weekMode;

    /** @param app the application core */
    public MainFrame(EnoughProcrastination app) {
        super("Enough Procrastination \u2014 personal schedule planner");
        this.app = app;
        this.anchor = app.scheduler().today();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        setLayout(new BorderLayout());

        coursePanel = new CoursePanel(this::addCourse, this::editCourse, this::deleteCourse,
                c -> filterBar.selectCourse(c == null ? null : c.getId()));
        assignmentPanel = new AssignmentPanel(this::addAssignment, this::editAssignment, this::deleteAssignment,
                this::trackProgress, this::markComplete);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        monthView.setOnItemClicked(this::openItem);
        weekView.setOnItemClicked(this::openItem);
        monthView.setOnDayDoubleClicked(this::addEventOn);
        weekView.setOnDayDoubleClicked(this::addEventOn);
        filterBar.setOnChange(this::refresh);

        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        refresh();
        setStatus("Loaded " + app.courses().getCourses().size() + " course(s), "
                + app.events().getEvents().size() + " event(s), "
                + app.assignments().getAssignments().size() + " assignment(s) from "
                + app.getDataDirectory());
    }

    // ---- layout -------------------------------------------------------

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UiTheme.SURFACE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.LINE));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(10, 16, 6, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        JLabel brand = new JLabel("Enough Procrastination");
        brand.setFont(UiTheme.TITLE);
        brand.setForeground(UiTheme.ACCENT);
        left.add(brand);
        left.add(Box.createHorizontalStrut(18));
        JButton prev = new JButton("\u2039");
        JButton next = new JButton("\u203a");
        JButton todayBtn = new JButton("Today");
        prev.addActionListener(e -> move(-1));
        next.addActionListener(e -> move(1));
        todayBtn.addActionListener(e -> {
            anchor = app.scheduler().today();
            refresh();
        });
        left.add(prev);
        left.add(todayBtn);
        left.add(next);
        periodLabel.setFont(UiTheme.BOLD.deriveFont(15f));
        periodLabel.setForeground(UiTheme.TEXT);
        periodLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        left.add(periodLabel);
        top.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        ButtonGroup views = new ButtonGroup();
        views.add(monthButton);
        views.add(weekButton);
        monthButton.setSelected(true);
        monthButton.addActionListener(e -> setWeekMode(false));
        weekButton.addActionListener(e -> setWeekMode(true));
        right.add(monthButton);
        right.add(weekButton);
        right.add(Box.createHorizontalStrut(12));
        JButton addEvent = new JButton("+ Event");
        addEvent.addActionListener(e -> addEventOn(anchor));
        JButton addAssignment = new JButton("+ Assignment");
        addAssignment.addActionListener(e -> addAssignment());
        right.add(addEvent);
        right.add(addAssignment);
        top.add(right, BorderLayout.EAST);
        header.add(top);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filterRow.setOpaque(false);
        filterRow.setBorder(BorderFactory.createEmptyBorder(2, 16, 10, 16));
        filterRow.add(filterBar);
        header.add(filterRow);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(UiTheme.BACKGROUND);
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));
        side.setPreferredSize(new Dimension(290, 600));
        side.add(card(coursePanel));
        side.add(Box.createVerticalStrut(10));
        side.add(card(assignmentPanel));
        side.add(Box.createVerticalStrut(10));
        side.add(legend());
        return side;
    }

    private static JPanel card(JPanel content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UiTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.LINE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.add(content, BorderLayout.CENTER);
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    private JPanel legend() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g0) {
                super.paintComponent(g0);
                java.awt.Graphics2D g = (java.awt.Graphics2D) g0;
                UiTheme.drawChip(g, 10, 8, 118, 20, UiTheme.ACCENT, ep.model.ItemType.EVENT, "Fixed event", null);
                UiTheme.drawChip(g, 136, 8, 118, 20, UiTheme.ACCENT, ep.model.ItemType.STUDY_BLOCK, "Study block", null);
                g.setFont(UiTheme.SMALL);
                g.setColor(UiTheme.MUTED);
                g.drawString("Click an item to edit it. Double-click a day to add an event.", 10, 46);
            }
        };
        p.setPreferredSize(new Dimension(260, 56));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 12));
        emptyBanner.setOpaque(true);
        emptyBanner.setBackground(new Color(0xFFF4E5));
        emptyBanner.setForeground(new Color(0x9A3412));
        emptyBanner.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        emptyBanner.setVisible(false);
        center.add(emptyBanner, BorderLayout.NORTH);
        JScrollPane weekScroll = new JScrollPane(weekView);
        weekScroll.setBorder(null);
        weekScroll.getVerticalScrollBar().setUnitIncrement(16);
        cardPanel.add(monthView, "month");
        cardPanel.add(weekScroll, "week");
        cardPanel.setBorder(BorderFactory.createLineBorder(UiTheme.LINE));
        center.add(cardPanel, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        statusLabel.setFont(UiTheme.SMALL);
        statusLabel.setForeground(UiTheme.MUTED);
        bar.add(statusLabel, BorderLayout.WEST);
        JLabel saved = new JLabel("Saved locally \u00b7 " + app.getDataDirectory());
        saved.setFont(UiTheme.SMALL);
        saved.setForeground(UiTheme.MUTED);
        bar.add(saved, BorderLayout.EAST);
        return bar;
    }

    // ---- navigation ---------------------------------------------------

    private void move(int delta) {
        anchor = weekMode ? anchor.plusWeeks(delta) : anchor.plusMonths(delta);
        refresh();
    }

    /**
     * Switches between the month and the week view.
     *
     * @param week {@code true} for the week view
     */
    public void setWeekMode(boolean week) {
        weekMode = week;
        monthButton.setSelected(!week);
        weekButton.setSelected(week);
        cards.show(cardPanel, week ? "week" : "month");
        refresh();
    }

    /** @return {@code true} if the week view is shown */
    public boolean isWeekMode() {
        return weekMode;
    }

    /** Reloads every panel from the managers. */
    public void refresh() {
        LocalDate today = app.scheduler().today();
        CalendarFilter filter = filterBar.getFilter();
        Function<String, Optional<Course>> lookup = id -> app.courses().find(id);
        List<CalendarItem> items = weekMode
                ? app.calendar().itemsForWeek(anchor, filter)
                : app.calendar().itemsForMonth(YearMonth.from(anchor), filter);
        monthView.setData(anchor, today, items, lookup);
        weekView.setData(anchor, today, items, lookup);
        weekView.revalidate();
        if (weekMode) {
            LocalDate monday = ep.service.CalendarManager.startOfWeek(anchor);
            periodLabel.setText(monday.format(DAY_FMT) + " \u2013 " + monday.plusDays(6).format(DAY_FMT)
                    + ", " + monday.getYear());
        } else {
            periodLabel.setText(YearMonth.from(anchor).format(MONTH_FMT));
        }
        emptyBanner.setVisible(!filter.isEmpty() && items.isEmpty());
        coursePanel.setCourses(app.courses().getCourses());
        filterBar.setCourses(app.courses().getCourses());
        assignmentPanel.setAssignments(app.assignments().getAssignments(), a -> app.progress().getReport(a),
                lookup, today);
        filterBar.revalidate();
        repaint();
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    // ---- course actions (use case 1) ---------------------------------

    private void addCourse() {
        CourseDialog d = new CourseDialog(this, app.courses(), null);
        if (d.showDialog()) {
            refresh();
            setStatus("Course \"" + d.getResult().getName() + "\" added.");
        }
    }

    private void editCourse(Course c) {
        if (c == null) {
            return;
        }
        CourseDialog d = new CourseDialog(this, app.courses(), c);
        if (d.showDialog()) {
            refresh();
            setStatus("Course \"" + d.getResult().getName() + "\" updated.");
        }
    }

    private void deleteCourse(Course c) {
        if (c == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete course \"" + c.getName() + "\"?\nEvents and assignments stay; they only lose the link.",
                "Delete course", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            app.courses().deleteCourse(c.getId());
            coursePanel.clearSelection();
            refresh();
            setStatus("Course \"" + c.getName() + "\" deleted.");
        }
    }

    // ---- event actions (use case 2) ----------------------------------

    private void addEventOn(LocalDate day) {
        EventDialog d = new EventDialog(this, app.events(), app.courses(), null, day);
        if (d.showDialog()) {
            afterEvent(d.getResult(), "added");
        }
    }

    private void editEvent(Event e) {
        EventDialog d = new EventDialog(this, app.events(), app.courses(), e, e.getDate());
        if (d.showDialog()) {
            afterEvent(d.getResult(), "updated");
        }
    }

    private void afterEvent(ep.service.EventManager.EventResult r, String verb) {
        anchor = r.getEvent().getDate();
        refresh();
        StringBuilder sb = new StringBuilder("Event \"" + r.getEvent().getTitle() + "\" " + verb + ".");
        boolean shortage = false;
        for (PlanResult p : r.getReplans()) {
            sb.append(" Re-planned ").append(p.describe());
            shortage |= p.hasShortage();
        }
        setStatus(sb.toString());
        if (shortage) {
            warn(sb.toString());
        }
    }

    private void openItem(CalendarItem item) {
        if (item instanceof Event) {
            Event e = (Event) item;
            Object[] options = {"Edit", "Delete", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this, e.toString(), "Fixed event",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                editEvent(e);
            } else if (choice == 1) {
                app.events().deleteEvent(e.getId());
                refresh();
                setStatus("Event \"" + e.getTitle() + "\" deleted.");
            }
        } else if (item instanceof StudyBlock) {
            StudyBlock b = (StudyBlock) item;
            app.assignments().find(b.getAssignmentId()).ifPresent(this::trackProgress);
        }
    }

    // ---- assignment actions (use case 3) -----------------------------

    private void addAssignment() {
        AssignmentDialog d = new AssignmentDialog(this, app.assignments(), app.courses(), null, app.scheduler().today());
        if (d.showDialog()) {
            afterPlan(d.getResult());
        }
    }

    private void editAssignment(Assignment a) {
        if (a == null) {
            return;
        }
        AssignmentDialog d = new AssignmentDialog(this, app.assignments(), app.courses(), a, app.scheduler().today());
        if (d.showDialog()) {
            afterPlan(d.getResult());
        }
    }

    private void deleteAssignment(Assignment a) {
        if (a == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete assignment \"" + a.getTitle() + "\" and all of its study blocks?",
                "Delete assignment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            app.assignments().deleteAssignment(a.getId());
            refresh();
            setStatus("Assignment \"" + a.getTitle() + "\" deleted.");
        }
    }

    private void afterPlan(PlanResult r) {
        if (!r.getBlocks().isEmpty()) {
            anchor = r.getBlocks().get(0).getDate();
        }
        refresh();
        setStatus(r.describe());
        if (r.hasShortage()) {
            warn("Not enough free time before the deadline.\n" + r.describe()
                    + "\nAdd free time (move or delete fixed events), extend the due date, or lower the estimate.");
        }
    }

    // ---- progress actions (use case 5) -------------------------------

    private void trackProgress(Assignment a) {
        if (a == null) {
            return;
        }
        ProgressDialog d = new ProgressDialog(this, app.progress(), a);
        if (d.showDialog()) {
            afterProgress(d.getResult());
        }
    }

    private void markComplete(Assignment a) {
        if (a == null) {
            return;
        }
        try {
            afterProgress(app.progress().markComplete(a.getId()));
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        }
    }

    private void afterProgress(ProgressReport r) {
        refresh();
        if (r.isComplete()) {
            setStatus("\"" + r.getAssignment().getTitle() + "\" marked complete; its study blocks were removed.");
        } else {
            setStatus("\"" + r.getAssignment().getTitle() + "\": " + r.getCompletedHours() + " h done, "
                    + r.getRemainingHours() + " h remaining"
                    + (r.hasShortage() ? " (" + r.getUnscheduledHours() + " h could not be scheduled)" : "") + ".");
            if (r.hasShortage()) {
                warn("Progress saved, but " + r.getUnscheduledHours()
                        + " h of the remaining work do not fit before the deadline.");
            }
        }
    }

    private void warn(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "Shortage warning",
                JOptionPane.WARNING_MESSAGE));
    }

    /** @return the day currently in focus */
    public LocalDate getAnchor() {
        return anchor;
    }

    /** @param day day to focus */
    public void setAnchor(LocalDate day) {
        this.anchor = day;
        refresh();
    }
}
