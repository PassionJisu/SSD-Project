package ep.model;

/**
 * Snapshot of an assignment's progress after a change: estimated, completed
 * and remaining hours, plus any shortage left by re-planning the remainder.
 */
public class ProgressReport {

    private final Assignment assignment;
    private final int unscheduledHours;

    /**
     * @param assignment       the assignment
     * @param unscheduledHours hours of the remaining work that did not fit
     */
    public ProgressReport(Assignment assignment, int unscheduledHours) {
        this.assignment = assignment;
        this.unscheduledHours = unscheduledHours;
    }

    /** @return the assignment */
    public Assignment getAssignment() {
        return assignment;
    }

    /** @return estimated total hours */
    public int getEstimatedHours() {
        return assignment.getEstimatedHours();
    }

    /** @return hours recorded as finished */
    public int getCompletedHours() {
        return assignment.getCompletedHours();
    }

    /** @return hours still left */
    public int getRemainingHours() {
        return assignment.getRemainingHours();
    }

    /** @return remaining hours that could not be placed before the deadline */
    public int getUnscheduledHours() {
        return unscheduledHours;
    }

    /** @return {@code true} when the remaining work does not fit */
    public boolean hasShortage() {
        return unscheduledHours > 0;
    }

    /** @return {@code true} when the assignment is finished */
    public boolean isComplete() {
        return assignment.isComplete();
    }

    /** @return completion ratio between 0 and 1 */
    public double getRatio() {
        if (assignment.isComplete()) {
            return 1.0;
        }
        if (assignment.getEstimatedHours() <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) assignment.getCompletedHours() / assignment.getEstimatedHours());
    }
}
