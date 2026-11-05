package ep.ui;

import ep.model.Event;
import ep.service.CourseManager;
import ep.service.EventManager;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dialog for adding or editing a fixed event: title, date, all-day switch,
 * start and end time, and an optional course link.
 */
public class EventDialog extends FormDialog {

    private final EventManager events;
    private final Event existing;
    private final JTextField titleField = new JTextField(22);
    private final JSpinner dateField;
    private final JCheckBox allDayBox = new JCheckBox("All-day event");
    private final JSpinner startField;
    private final JSpinner endField;
    private final CourseCombo courseCombo;
    private EventManager.EventResult result;

    /**
     * @param owner       parent window
     * @param events      event manager
     * @param courses     course manager (for the course list)
     * @param existing    event to edit, or {@code null} to add
     * @param defaultDate date preselected for a new event
     */
    public EventDialog(Window owner, EventManager events, CourseManager courses, Event existing, LocalDate defaultDate) {
        super(owner, existing == null ? "Add event" : "Edit event",
                existing == null ? "New fixed event" : "Edit fixed event");
        this.events = events;
        this.existing = existing;
        LocalDate date = existing != null ? existing.getDate() : defaultDate;
        LocalTime start = existing != null && !existing.isAllDay() ? existing.getStartTime() : LocalTime.of(10, 0);
        LocalTime end = existing != null && !existing.isAllDay() ? existing.getEndTime() : LocalTime.of(11, 0);
        dateField = Fields.dateSpinner(date);
        startField = Fields.timeSpinner(start);
        endField = Fields.timeSpinner(end);
        courseCombo = new CourseCombo(courses.getCourses());
        if (existing != null) {
            titleField.setText(existing.getTitle());
            allDayBox.setSelected(existing.isAllDay());
            courseCombo.setSelectedCourseId(existing.getCourseId());
        }
        allDayBox.setOpaque(false);
        allDayBox.addActionListener(e -> toggleTimes());
        toggleTimes();

        JPanel form = Fields.form();
        Fields.addRow(form, 0, "Title", titleField);
        Fields.addRow(form, 1, "Date", dateField);
        Fields.addWide(form, 2, allDayBox);
        Fields.addRow(form, 3, "Start", startField);
        Fields.addRow(form, 4, "End", endField);
        Fields.addRow(form, 5, "Course", courseCombo);
        setForm(form);
    }

    private void toggleTimes() {
        boolean timed = !allDayBox.isSelected();
        startField.setEnabled(timed);
        endField.setEnabled(timed);
    }

    @Override
    protected void apply() {
        boolean allDay = allDayBox.isSelected();
        LocalTime s = allDay ? null : Fields.timeOf(startField);
        LocalTime e = allDay ? null : Fields.timeOf(endField);
        if (existing == null) {
            result = events.addEvent(titleField.getText(), Fields.dateOf(dateField), allDay, s, e,
                    courseCombo.getSelectedCourseId());
        } else {
            result = events.editEvent(existing.getId(), titleField.getText(), Fields.dateOf(dateField), allDay, s, e,
                    courseCombo.getSelectedCourseId());
        }
    }

    /** @return the saved event with re-planning info, or {@code null} if cancelled */
    public EventManager.EventResult getResult() {
        return result;
    }
}
