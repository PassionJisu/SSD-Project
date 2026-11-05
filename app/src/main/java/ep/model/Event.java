package ep.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * A fixed commitment such as a lecture or an appointment. An event is busy
 * time: the planner never places a {@link StudyBlock} over it, and the
 * program never moves or shortens it.
 */
public class Event implements CalendarItem {

    private final String id;
    private String title;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean allDay;
    private String courseId;

    /**
     * Creates an event.
     *
     * @param id        unique identifier
     * @param title     non-blank title
     * @param date      the day of the event
     * @param allDay    {@code true} for an all-day event
     * @param startTime start time, ignored when {@code allDay}
     * @param endTime   end time, ignored when {@code allDay}
     * @param courseId  linked course id or {@code null}
     */
    public Event(String id, String title, LocalDate date, boolean allDay,
                 LocalTime startTime, LocalTime endTime, String courseId) {
        this.id = Objects.requireNonNull(id);
        this.title = title;
        this.date = date;
        this.allDay = allDay;
        this.startTime = allDay ? null : startTime;
        this.endTime = allDay ? null : endTime;
        this.courseId = courseId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    /** @param title new title */
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    /** @param date new day */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public LocalTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Changes the time of the event.
     *
     * @param allDay    {@code true} for an all-day event
     * @param startTime start time, ignored when {@code allDay}
     * @param endTime   end time, ignored when {@code allDay}
     */
    public void setTime(boolean allDay, LocalTime startTime, LocalTime endTime) {
        this.allDay = allDay;
        this.startTime = allDay ? null : startTime;
        this.endTime = allDay ? null : endTime;
    }

    @Override
    public boolean isAllDay() {
        return allDay;
    }

    @Override
    public String getCourseId() {
        return courseId;
    }

    /** @param courseId linked course id or {@code null} */
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    @Override
    public ItemType getType() {
        return ItemType.EVENT;
    }

    @Override
    public String toString() {
        return title + " (" + date + (allDay ? ", all day)" : " " + startTime + "-" + endTime + ")");
    }
}
