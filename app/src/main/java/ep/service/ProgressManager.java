package ep.service;

import ep.model.Assignment;
import ep.model.PlanResult;
import ep.model.ProgressReport;
import ep.model.ValidationException;
import ep.storage.DataStore;
import ep.storage.LocalStorage;

/**
 * Use case 5, <em>Track Assignment Progress</em>. Records completed hours,
 * keeps the remaining hours and the study blocks on the calendar in
 * agreement, and lets the student mark an assignment complete.
 */
public class ProgressManager extends BaseManager {

    private final AssignmentManager assignments;

    /**
     * @param store       shared in-memory data
     * @param storage     persistence
     * @param assignments used to rebuild blocks for the remaining hours
     */
    public ProgressManager(DataStore store, LocalStorage storage, AssignmentManager assignments) {
        super(store, storage);
        this.assignments = assignments;
    }

    /**
     * @param assignment an assignment
     * @return its current progress (no change is made)
     */
    public ProgressReport getReport(Assignment assignment) {
        int placed = store.getBlocksOf(assignment.getId()).stream().mapToInt(b -> b.getHours()).sum();
        return new ProgressReport(assignment, Math.max(0, assignment.getRemainingHours() - placed));
    }

    /**
     * Records time the student just finished. The completed total goes up,
     * the finished blocks disappear and only the unfinished hours stay on the
     * calendar. When the total reaches the estimate the assignment becomes
     * complete.
     *
     * @param assignmentId   assignment to update
     * @param completedHours hours just finished; must be positive and must not
     *                       push the total past the estimate
     * @return the new progress and any shortage of the remaining hours
     * @throws ValidationException when the hours are invalid; progress is unchanged then
     */
    public ProgressReport recordProgress(String assignmentId, int completedHours) {
        Assignment a = store.findAssignment(assignmentId)
                .orElseThrow(() -> new ValidationException("Assignment not found."));
        if (a.isComplete()) {
            throw new ValidationException("This assignment is already complete.");
        }
        if (completedHours <= 0) {
            throw new ValidationException("Completed time must be greater than zero.");
        }
        int total = a.getCompletedHours() + completedHours;
        if (total > a.getEstimatedHours()) {
            throw new ValidationException("Completed time cannot exceed the estimate of "
                    + a.getEstimatedHours() + " h (" + a.getCompletedHours() + " h already recorded).");
        }
        a.setCompletedHours(total);
        if (total == a.getEstimatedHours()) {
            a.setComplete(true);
            store.removeBlocksOf(a.getId());
            persist();
            return new ProgressReport(a, 0);
        }
        PlanResult plan = assignments.replan(a);
        return new ProgressReport(a, plan.getUnscheduledHours());
    }

    /**
     * Marks an assignment complete. Remaining hours become zero and its study
     * blocks are removed from the calendar.
     *
     * @param assignmentId assignment to finish
     * @return the final progress
     * @throws ValidationException when the assignment is unknown
     */
    public ProgressReport markComplete(String assignmentId) {
        Assignment a = store.findAssignment(assignmentId)
                .orElseThrow(() -> new ValidationException("Assignment not found."));
        a.setComplete(true);
        a.setCompletedHours(a.getEstimatedHours());
        store.removeBlocksOf(a.getId());
        persist();
        return new ProgressReport(a, 0);
    }

    /**
     * Reopens a completed assignment so that the remaining hours can be
     * planned again.
     *
     * @param assignmentId   assignment to reopen
     * @param completedHours hours to keep as completed (0 .. estimate - 1)
     * @return the new progress and any shortage
     * @throws ValidationException when the value is out of range
     */
    public ProgressReport reopen(String assignmentId, int completedHours) {
        Assignment a = store.findAssignment(assignmentId)
                .orElseThrow(() -> new ValidationException("Assignment not found."));
        if (completedHours < 0 || completedHours >= a.getEstimatedHours()) {
            throw new ValidationException("Completed hours must be between 0 and " + (a.getEstimatedHours() - 1) + ".");
        }
        a.setComplete(false);
        a.setCompletedHours(completedHours);
        PlanResult plan = assignments.replan(a);
        return new ProgressReport(a, plan.getUnscheduledHours());
    }
}
