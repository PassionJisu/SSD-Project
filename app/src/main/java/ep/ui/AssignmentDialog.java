package ep.ui;

import ep.model.Assignment;
import ep.model.PlanResult;
import ep.service.AssignmentManager;
import ep.service.CourseManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.Window;
import java.time.LocalDate;

/**
 * Dialog for adding or editing an assignment: title, due date, estimated
 * hours, preferred block length and an optional course. Saving plans (or
 * re-plans) the study blocks.
 */
public class AssignmentDialog extends FormDialog {

    private final AssignmentManager assignments;
    private final Assignment existing;
    private final JTextField titleField = new JTextField(22);
    private final JSpinner dueField;
    private final JSpinner hoursField;
    private final JSpinner blockField;
    private final CourseCombo courseCombo;
    private PlanResult result;

    /**
     * @param owner       parent window
     * @param assignments assignment manager
     * @param courses     course manager (for the course list)
     * @param existing    assignment to edit, or {@code null} to add
     * @param today       today's date (default due date is a week later)
     */
    public AssignmentDialog(Window owner, AssignmentManager assignments, CourseManager courses,
                            Assignment existing, LocalDate today) {
        super(owner, existing == null ? "Add assignment" : "Edit assignment",
                existing == null ? "Plan a new assignment" : "Edit assignment");
        this.assignments = assignments;
        this.existing = existing;
        dueField = Fields.dateSpinner(existing != null ? existing.getDueDate() : today.plusWeeks(1));
        hoursField = Fields.intSpinner(existing != null ? existing.getEstimatedHours() : 6, 1, 500);
        blockField = Fields.intSpinner(existing != null ? existing.getBlockLength() : 2, 1, 12);
        courseCombo = new CourseCombo(courses.getCourses());
        if (existing != null) {
            titleField.setText(existing.getTitle());
            courseCombo.setSelectedCourseId(existing.getCourseId());
        }
        setOkText(existing == null ? "Plan blocks" : "Save & re-plan");

        JPanel form = Fields.form();
        Fields.addRow(form, 0, "Title", titleField);
        Fields.addRow(form, 1, "Due date", dueField);
        Fields.addRow(form, 2, "Estimated hours", hoursField);
        Fields.addRow(form, 3, "Block length (h)", blockField);
        Fields.addRow(form, 4, "Course", courseCombo);
        JLabel hint = new JLabel("<html><span style='color:#7A8294'>Blocks are placed backward from the day "
                + "before the due date,<br>skipping fixed events and other study blocks.</span></html>");
        hint.setFont(UiTheme.SMALL);
        Fields.addWide(form, 5, hint);
        setForm(form);
    }

    @Override
    protected void apply() {
        if (existing == null) {
            result = assignments.addAssignment(titleField.getText(), Fields.dateOf(dueField),
                    Fields.intOf(hoursField), Fields.intOf(blockField), courseCombo.getSelectedCourseId());
        } else {
            result = assignments.editAssignment(existing.getId(), titleField.getText(), Fields.dateOf(dueField),
                    Fields.intOf(hoursField), Fields.intOf(blockField), courseCombo.getSelectedCourseId());
        }
    }

    /** @return the planning result, or {@code null} if cancelled */
    public PlanResult getResult() {
        return result;
    }
}
