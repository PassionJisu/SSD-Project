package ep.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Anything that can be drawn on the calendar: a fixed {@link Event} or a
 * generated {@link StudyBlock}. The calendar views and the filter only work
 * with this interface, so both kinds of entries are handled the same way.
 */
public interface CalendarItem {

    /** @return unique identifier */
    String getId();

    /** @return text shown on the calendar */
    String getTitle();

    /** @return the day this item belongs to */
    LocalDate getDate();

    /** @return start time, or {@code null} for an all-day item */
    LocalTime getStartTime();

    /** @return end time (exclusive), or {@code null} for an all-day item */
    LocalTime getEndTime();

    /** @return {@code true} if the item has no start/end time */
    boolean isAllDay();

    /** @return id of the linked course, or {@code null} if unlinked */
    String getCourseId();

    /** @return whether this is a fixed event or a study block */
    ItemType getType();

    /**
     * Tells whether this item occupies any part of the given time range.
     * An all-day item occupies the whole day.
     *
     * @param date  day to test
     * @param start start of the range (inclusive)
     * @param end   end of the range (exclusive)
     * @return {@code true} if the ranges overlap
     */
    default boolean overlaps(LocalDate date, LocalTime start, LocalTime end) {
        if (!getDate().equals(date)) {
            return false;
        }
        if (isAllDay()) {
            return true;
        }
        return getStartTime().isBefore(end) && start.isBefore(getEndTime());
    }

    /**
     * Tells whether this item occupies any part of another item.
     *
     * @param other the other item
     * @return {@code true} if the two items share time on the same day
     */
    default boolean overlaps(CalendarItem other) {
        if (!getDate().equals(other.getDate())) {
            return false;
        }
        if (isAllDay() || other.isAllDay()) {
            return true;
        }
        return overlaps(other.getDate(), other.getStartTime(), other.getEndTime());
    }

    /** @return length of the item in minutes (24h for an all-day item) */
    default int getDurationMinutes() {
        if (isAllDay()) {
            return 24 * 60;
        }
        return getEndTime().toSecondOfDay() / 60 - getStartTime().toSecondOfDay() / 60;
    }
}
