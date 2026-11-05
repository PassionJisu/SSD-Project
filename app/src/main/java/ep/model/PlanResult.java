package ep.model;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of one planning run for an {@link Assignment}: the study blocks that
 * were placed and the number of hours that did not fit before the deadline.
 */
public class PlanResult {

    private final Assignment assignment;
    private final List<StudyBlock> blocks;
    private final int unscheduledHours;

    /**
     * @param assignment       the assignment that was planned
     * @param blocks           blocks that were placed in free time
     * @param unscheduledHours hours that could not be placed
     */
    public PlanResult(Assignment assignment, List<StudyBlock> blocks, int unscheduledHours) {
        this.assignment = assignment;
        this.blocks = Collections.unmodifiableList(blocks);
        this.unscheduledHours = unscheduledHours;
    }

    /** @return the planned assignment */
    public Assignment getAssignment() {
        return assignment;
    }

    /** @return the blocks placed by this run */
    public List<StudyBlock> getBlocks() {
        return blocks;
    }

    /** @return hours that could not be placed before the deadline */
    public int getUnscheduledHours() {
        return unscheduledHours;
    }

    /** @return {@code true} when some hours could not be placed */
    public boolean hasShortage() {
        return unscheduledHours > 0;
    }

    /** @return total hours actually placed */
    public int getScheduledHours() {
        return blocks.stream().mapToInt(StudyBlock::getHours).sum();
    }

    /** @return a one-line message for the status bar or a warning dialog */
    public String describe() {
        if (hasShortage()) {
            return "\"" + assignment.getTitle() + "\": " + getScheduledHours()
                    + " h placed, " + unscheduledHours + " h could not be scheduled before "
                    + assignment.getDueDate() + ".";
        }
        return "\"" + assignment.getTitle() + "\": " + blocks.size() + " study block(s), "
                + getScheduledHours() + " h placed before " + assignment.getDueDate() + ".";
    }
}
