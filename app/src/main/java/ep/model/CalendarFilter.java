package ep.model;

import java.time.LocalDate;

/**
 * Search and filter conditions for the calendar. Every field is optional; an
 * empty filter matches everything.
 */
public class CalendarFilter {

    private String keyword;
    private String courseId;
    private ItemType type;
    private LocalDate from;
    private LocalDate to;

    /** Creates an empty filter that matches every item. */
    public CalendarFilter() {
    }

    /** @return keyword to look for in titles, or {@code null} */
    public String getKeyword() {
        return keyword;
    }

    /**
     * @param keyword case-insensitive word that must appear in the title
     * @return this filter
     */
    public CalendarFilter keyword(String keyword) {
        this.keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return this;
    }

    /** @return course id to match, or {@code null} */
    public String getCourseId() {
        return courseId;
    }

    /**
     * @param courseId only items linked to this course
     * @return this filter
     */
    public CalendarFilter course(String courseId) {
        this.courseId = courseId;
        return this;
    }

    /** @return item type to match, or {@code null} for both */
    public ItemType getType() {
        return type;
    }

    /**
     * @param type only fixed events or only study blocks
     * @return this filter
     */
    public CalendarFilter type(ItemType type) {
        this.type = type;
        return this;
    }

    /** @return first day to include, or {@code null} */
    public LocalDate getFrom() {
        return from;
    }

    /** @return last day to include, or {@code null} */
    public LocalDate getTo() {
        return to;
    }

    /**
     * @param from first day (inclusive) or {@code null}
     * @param to   last day (inclusive) or {@code null}
     * @return this filter
     */
    public CalendarFilter dateRange(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
        return this;
    }

    /** @return {@code true} if no condition is set */
    public boolean isEmpty() {
        return keyword == null && courseId == null && type == null && from == null && to == null;
    }

    /**
     * Tests one item against every condition.
     *
     * @param item the calendar item
     * @return {@code true} if the item passes all conditions
     */
    public boolean matches(CalendarItem item) {
        if (keyword != null && !item.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
            return false;
        }
        if (courseId != null && !courseId.equals(item.getCourseId())) {
            return false;
        }
        if (type != null && item.getType() != type) {
            return false;
        }
        if (from != null && item.getDate().isBefore(from)) {
            return false;
        }
        if (to != null && item.getDate().isAfter(to)) {
            return false;
        }
        return true;
    }
}
