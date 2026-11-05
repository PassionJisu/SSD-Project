package ep.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An assignment with a deadline. The student gives an estimate of the hours
 * needed and a preferred block length, and the planner turns that into
 * {@link StudyBlock}s before the due date. Progress (completed hours and the
 * complete flag) is stored on the assignment as well.
 */
public class Assignment {

    private final String id;
    private String title;
    private LocalDate dueDate;
    private int estimatedHours;
    private int blockLength;
    private String courseId;
    private int completedHours;
    private boolean complete;

    /**
     * Creates an assignment with no progress recorded.
     *
     * @param id             unique identifier
     * @param title          non-blank title
     * @param dueDate        deadline (blocks are placed on days before it)
     * @param estimatedHours total hours the student expects to need
     * @param blockLength    preferred length of one study block, in hours
     * @param courseId       linked course id or {@code null}
     */
    public Assignment(String id, String title, LocalDate dueDate, int estimatedHours,
                      int blockLength, String courseId) {
        this.id = Objects.requireNonNull(id);
        this.title = title;
        this.dueDate = dueDate;
        this.estimatedHours = estimatedHours;
        this.blockLength = blockLength;
        this.courseId = courseId;
    }

    /** @return unique identifier */
    public String getId() {
        return id;
    }

    /** @return title */
    public String getTitle() {
        return title;
    }

    /** @param title new title */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return the deadline */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /** @param dueDate new deadline */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /** @return estimated total hours */
    public int getEstimatedHours() {
        return estimatedHours;
    }

    /** @param estimatedHours new estimate in hours */
    public void setEstimatedHours(int estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    /** @return preferred study block length in hours */
    public int getBlockLength() {
        return blockLength;
    }

    /** @param blockLength preferred block length in hours */
    public void setBlockLength(int blockLength) {
        this.blockLength = blockLength;
    }

    /** @return linked course id or {@code null} */
    public String getCourseId() {
        return courseId;
    }

    /** @param courseId linked course id or {@code null} */
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    /** @return hours the student has recorded as finished */
    public int getCompletedHours() {
        return completedHours;
    }

    /** @param completedHours hours recorded as finished */
    public void setCompletedHours(int completedHours) {
        this.completedHours = completedHours;
    }

    /** @return {@code true} once the student marked the assignment complete */
    public boolean isComplete() {
        return complete;
    }

    /** @param complete the complete flag */
    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    /** @return hours still to be studied; zero when complete */
    public int getRemainingHours() {
        if (complete) {
            return 0;
        }
        return Math.max(0, estimatedHours - completedHours);
    }

    @Override
    public String toString() {
        return title;
    }
}
