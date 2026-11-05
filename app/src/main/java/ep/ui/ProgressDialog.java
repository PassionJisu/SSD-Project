package ep.ui;

import ep.model.Assignment;
import ep.model.ProgressReport;
import ep.service.ProgressManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import java.awt.Window;

/**
 * Dialog for use case 5. Shows estimated, completed and remaining hours,
 * takes the hours just finished, and offers a "Mark complete" shortcut.
 */
public class ProgressDialog extends FormDialog {

    private final ProgressManager progress;
    private final Assignment assignment;
    private final JSpinner hoursField;
    private ProgressReport result;

    /**
     * @param owner      parent window
     * @param progress   progress manager
     * @param assignment assignment being tracked
     */
    public ProgressDialog(Window owner, ProgressManager progress, Assignment assignment) {
        super(owner, "Track progress", assignment.getTitle());
        this.progress = progress;
        this.assignment = assignment;
        ProgressReport current = progress.getReport(assignment);
        int remaining = current.getRemainingHours();
        hoursField = Fields.intSpinner(Math.min(1, Math.max(0, remaining)), 0, Math.max(1, remaining));
        setOkText("Record hours");

        JPanel form = Fields.form();
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) Math.round(current.getRatio() * 100));
        bar.setStringPainted(true);
        bar.setString(current.getCompletedHours() + " h of " + current.getEstimatedHours() + " h done");
        Fields.addWide(form, 0, bar);
        Fields.addRow(form, 1, "Estimated", new JLabel(current.getEstimatedHours() + " h"));
        Fields.addRow(form, 2, "Completed", new JLabel(current.getCompletedHours() + " h"));
        JLabel left = new JLabel(remaining + " h" + (current.hasShortage()
                ? "  (" + current.getUnscheduledHours() + " h not scheduled)" : ""));
        if (current.hasShortage()) {
            left.setForeground(new java.awt.Color(0xC2410C));
        }
        Fields.addRow(form, 3, "Remaining", left);
        Fields.addRow(form, 4, "Hours just finished", hoursField);

        JButton complete = new JButton("Mark complete");
        complete.addActionListener(e -> {
            result = progress.markComplete(assignment.getId());
            markAcceptedAndClose();
        });
        Fields.addWide(form, 5, complete);
        hoursField.setEnabled(!assignment.isComplete());
        setForm(form);
    }

    private boolean forcedAccept;

    private void markAcceptedAndClose() {
        forcedAccept = true;
        dispose();
    }

    @Override
    public boolean isAccepted() {
        return forcedAccept || super.isAccepted();
    }

    @Override
    public boolean showDialog() {
        setVisible(true);
        return isAccepted();
    }

    @Override
    protected void apply() {
        result = progress.recordProgress(assignment.getId(), Fields.intOf(hoursField));
    }

    /** @return the progress after the change, or {@code null} if cancelled */
    public ProgressReport getResult() {
        return result;
    }
}
