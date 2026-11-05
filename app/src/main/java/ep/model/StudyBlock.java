package ep.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * One block of study time generated for an {@link Assignment}. Blocks are
 * created and removed only by the planner; the student does not edit them
 * directly.
 */
public class StudyBlock implements CalendarItem {

    private final String id;
    private final String assignmentId;
    private String title;
    private String courseId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    /**
     * Creates a study block.
     *
     * @param id           unique identifier
     * @param assignmentId id of the owning assignment
     * @param title        title copied from the assignment for display
     * @param courseId     course copied from the assignment or {@code null}
     * @param date         day of the block
     * @param startTime    start time
     * @param endTime      end time (exclusive)
     */
    public StudyBlock(String id, String assignmentId, String title, String courseId,
                      LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.id = Objects.requireNonNull(id);
        this.assignmentId = Objects.requireNonNull(assignmentId);
        this.title = title;
        this.courseId = courseId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public String getId() {
        return id;
    }

    /** @return id of the assignment this block belongs to */
    public String getAssignmentId() {
        return assignmentId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    /** @param title new display title (kept in sync with the assignment) */
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public LocalTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalTime getEndTime() {
        return endTime;
    }

    @Override
    public boolean isAllDay() {
        return false;
    }

    @Override
    public String getCourseId() {
        return courseId;
    }

    /** @param courseId course id or {@code null} */
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    @Override
    public ItemType getType() {
        return ItemType.STUDY_BLOCK;
    }

    /** @return length of the block in whole hours (rounded down) */
    public int getHours() {
        return getDurationMinutes() / 60;
    }

    @Override
    public String toString() {
        return "Study: " + title + " (" + date + " " + startTime + "-" + endTime + ")";
    }
}
