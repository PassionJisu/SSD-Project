package ep.api;

import ep.model.Assignment;
import ep.model.CalendarFilter;
import ep.model.CalendarItem;
import ep.model.Course;
import ep.model.Event;
import ep.model.ItemType;
import ep.model.PlanResult;
import ep.model.ProgressReport;
import ep.model.StudyBlock;
import ep.model.ValidationException;
import ep.service.AssignmentManager;
import ep.service.CalendarManager;
import ep.service.CourseManager;
import ep.service.EventManager;
import ep.service.ProgressManager;
import ep.service.ScheduleManager;
import ep.storage.DataStore;
import ep.storage.LocalStorage;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Entry point of the application core and the public API of
 * <strong>Enough Procrastination</strong>.
 *
 * <p>This facade wires the data store, local storage and the five managers
 * together, loads the saved files once, and exposes the five functions from
 * the <em>Function Specification</em> of the design document with their
 * original string-based signatures:</p>
 *
 * <ol>
 *   <li>{@link #manageCourse(String, String, String)}</li>
 *   <li>{@link #manageEvent(String, String, String, String, String, boolean)}</li>
 *   <li>{@link #planAssignment(String, String, int, int)}</li>
 *   <li>{@link #filterCalendar(String, String, String)}</li>
 *   <li>{@link #trackProgress(Assignment, int, boolean)}</li>
 * </ol>
 *
 * <p>The GUI uses the typed managers directly (see {@link #courses()},
 * {@link #events()}, {@link #assignments()}, {@link #calendar()},
 * {@link #progress()}); the string-based functions are thin wrappers over
 * them. Whenever a function returns {@code false}, an empty list or
 * {@code -1}, the reason can be read with {@link #getLastMessage()}.</p>
 *
 * <p>Dates use ISO form {@code yyyy-MM-dd}, times use {@code HH:mm}, and a
 * date range is written {@code yyyy-MM-dd..yyyy-MM-dd} (either side may be
 * left out).</p>
 */
public class EnoughProcrastination {

    private final DataStore store;
    private final LocalStorage storage;
    private final ScheduleManager scheduleManager;
    private final CourseManager courseManager;
    private final AssignmentManager assignmentManager;
    private final EventManager eventManager;
    private final CalendarManager calendarManager;
    private final ProgressManager progressManager;

    private String lastMessage = "";

    /**
     * Opens (or creates) the data folder and loads any saved files.
     *
     * @param dataDirectory folder holding the local data files
     */
    public EnoughProcrastination(Path dataDirectory) {
        this(dataDirectory, Clock.systemDefaultZone());
    }

    /**
     * Opens (or creates) the data folder with an explicit clock. Mainly
     * useful for tests and demos that need a fixed "today".
     *
     * @param dataDirectory folder holding the local data files
     * @param clock         source of the current date and time
     */
    public EnoughProcrastination(Path dataDirectory, Clock clock) {
        this.store = new DataStore();
        this.storage = new LocalStorage(dataDirectory);
        this.storage.load(store);
        this.scheduleManager = new ScheduleManager(store, clock);
        this.courseManager = new CourseManager(store, storage);
        this.assignmentManager = new AssignmentManager(store, storage, scheduleManager);
        this.eventManager = new EventManager(store, storage, scheduleManager, assignmentManager);
        this.calendarManager = new CalendarManager(store);
        this.progressManager = new ProgressManager(store, storage, assignmentManager);
    }

    // ---- function specification ---------------------------------------

    /**
     * Adds, edits or deletes a course.
     *
     * <ul>
     *   <li>{@code "add"}: creates a course with {@code name} and {@code color}.</li>
     *   <li>{@code "edit"}: finds the course called {@code name} and gives it
     *       the new {@code color}. (Renaming is done with
     *       {@link CourseManager#editCourse(String, String, String)}.)</li>
     *   <li>{@code "delete"}: removes the course called {@code name}; linked
     *       events and assignments stay and become unlinked.</li>
     * </ul>
     *
     * @param actionType {@code "add"}, {@code "edit"} or {@code "delete"}
     * @param name       course name; must be non-blank and unique when adding
     * @param color      color in {@code #RRGGBB} form (may be blank for the default)
     * @return {@code true} if the change was saved
     */
    public boolean manageCourse(String actionType, String name, String color) {
        try {
            switch (action(actionType)) {
                case "add":
                    courseManager.addCourse(name, color);
                    return ok("Course added.");
                case "edit": {
                    Course c = requireCourse(name);
                    courseManager.editCourse(c.getId(), c.getName(), color);
                    return ok("Course updated.");
                }
                case "delete": {
                    Course c = requireCourse(name);
                    courseManager.deleteCourse(c.getId());
                    return ok("Course deleted.");
                }
                default:
                    return fail("Unknown action: " + actionType);
            }
        } catch (ValidationException ex) {
            return fail(ex.getMessage());
        }
    }

    /**
     * Adds, edits or deletes a fixed event. For {@code "edit"} and
     * {@code "delete"} the event is identified by {@code title} and
     * {@code date}; an edit replaces its time with the given values.
     *
     * @param actionType {@code "add"}, {@code "edit"} or {@code "delete"}
     * @param title      non-blank title
     * @param date       day in {@code yyyy-MM-dd} form
     * @param startTime  start in {@code HH:mm} form (ignored when all-day)
     * @param endTime    end in {@code HH:mm} form, after the start (ignored when all-day)
     * @param isAllDay   {@code true} for an all-day event
     * @return {@code true} if the change was saved
     */
    public boolean manageEvent(String actionType, String title, String date,
                               String startTime, String endTime, boolean isAllDay) {
        try {
            LocalDate d = parseDate(date);
            LocalTime s = isAllDay ? null : parseTime(startTime, "Start time");
            LocalTime e = isAllDay ? null : parseTime(endTime, "End time");
            switch (action(actionType)) {
                case "add":
                    return describe(eventManager.addEvent(title, d, isAllDay, s, e, null));
                case "edit": {
                    Event ev = requireEvent(title, d);
                    return describe(eventManager.editEvent(ev.getId(), title, d, isAllDay, s, e, ev.getCourseId()));
                }
                case "delete": {
                    Event ev = requireEvent(title, d);
                    eventManager.deleteEvent(ev.getId());
                    return ok("Event deleted.");
                }
                default:
                    return fail("Unknown action: " + actionType);
            }
        } catch (ValidationException ex) {
            return fail(ex.getMessage());
        }
    }

    /**
     * Creates an assignment and plans its study blocks backward from the
     * deadline. If the free time is short, only the blocks that fit are
     * placed and {@link #getLastMessage()} carries the shortage warning
     * ({@link #hasWarning()} becomes {@code true}).
     *
     * @param title       non-blank title
     * @param dueDate     deadline in {@code yyyy-MM-dd} form, today or later
     * @param totalHours  estimated hours, at least 1
     * @param blockLength preferred block length in hours
     * @return the scheduled blocks, or an empty list if the input was invalid
     */
    public List<StudyBlock> planAssignment(String title, String dueDate, int totalHours, int blockLength) {
        try {
            PlanResult result = assignmentManager.addAssignment(title, parseDate(dueDate), totalHours, blockLength, null);
            lastMessage = result.describe();
            warning = result.hasShortage();
            return result.getBlocks();
        } catch (ValidationException ex) {
            fail(ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches all saved events and study blocks.
     *
     * @param keyword    word that must appear in the title, or blank
     * @param courseName name of the course to filter by, or blank
     * @param dateRange  {@code yyyy-MM-dd}, {@code from..to}, {@code from..} or
     *                   {@code ..to}; blank for any date
     * @return the matching items in calendar order (empty when nothing matches)
     */
    public List<CalendarItem> filterCalendar(String keyword, String courseName, String dateRange) {
        try {
            CalendarFilter f = new CalendarFilter().keyword(keyword);
            if (courseName != null && !courseName.isBlank()) {
                f.course(requireCourse(courseName).getId());
            }
            LocalDate[] range = parseRange(dateRange);
            f.dateRange(range[0], range[1]);
            List<CalendarItem> items = calendarManager.filter(f);
            lastMessage = items.isEmpty() ? "Nothing was found." : items.size() + " item(s) found.";
            warning = false;
            return items;
        } catch (ValidationException ex) {
            fail(ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Records finished hours or marks an assignment complete.
     *
     * @param assignment     the assignment to update
     * @param completedHours hours just finished (ignored when {@code isComplete})
     * @param isComplete     {@code true} to mark the assignment complete
     * @return the hours still left, or {@code -1} if the input was rejected
     */
    public int trackProgress(Assignment assignment, int completedHours, boolean isComplete) {
        try {
            if (assignment == null) {
                return failInt("Assignment is required.");
            }
            ProgressReport r = isComplete
                    ? progressManager.markComplete(assignment.getId())
                    : progressManager.recordProgress(assignment.getId(), completedHours);
            warning = r.hasShortage();
            lastMessage = r.isComplete()
                    ? "\"" + assignment.getTitle() + "\" is complete."
                    : r.getRemainingHours() + " h remaining"
                      + (r.hasShortage() ? "; " + r.getUnscheduledHours() + " h could not be scheduled." : ".");
            return r.getRemainingHours();
        } catch (ValidationException ex) {
            return failInt(ex.getMessage());
        }
    }

    // ---- typed access for the GUI ------------------------------------

    /** @return course operations (use case 1) */
    public CourseManager courses() {
        return courseManager;
    }

    /** @return event operations (use case 2) */
    public EventManager events() {
        return eventManager;
    }

    /** @return assignment and planning operations (use case 3) */
    public AssignmentManager assignments() {
        return assignmentManager;
    }

    /** @return calendar queries and filtering (use case 4) */
    public CalendarManager calendar() {
        return calendarManager;
    }

    /** @return progress operations (use case 5) */
    public ProgressManager progress() {
        return progressManager;
    }

    /** @return the planner */
    public ScheduleManager scheduler() {
        return scheduleManager;
    }

    /** @return the raw in-memory data */
    public DataStore store() {
        return store;
    }

    /** @return where data files are kept */
    public Path getDataDirectory() {
        return storage.getDirectory();
    }

    /** @return message from the last function call (error, warning or summary) */
    public String getLastMessage() {
        return lastMessage;
    }

    private boolean warning;

    /** @return {@code true} if the last call produced a shortage warning */
    public boolean hasWarning() {
        return warning;
    }

    /**
     * Builds a {@link CalendarFilter} from GUI inputs.
     *
     * @param keyword  text or blank
     * @param courseId course id or {@code null}
     * @param type     item type or {@code null}
     * @param from     first day or {@code null}
     * @param to       last day or {@code null}
     * @return the filter
     */
    public static CalendarFilter filterOf(String keyword, String courseId, ItemType type, LocalDate from, LocalDate to) {
        return new CalendarFilter().keyword(keyword).course(courseId).type(type).dateRange(from, to);
    }

    // ---- helpers -------------------------------------------------------

    private boolean describe(EventManager.EventResult r) {
        List<String> parts = new ArrayList<>();
        parts.add("Event saved.");
        warning = false;
        for (PlanResult p : r.getReplans()) {
            parts.add("Re-planned " + p.describe());
            warning |= p.hasShortage();
        }
        lastMessage = String.join(" ", parts);
        return true;
    }

    private boolean ok(String message) {
        lastMessage = message;
        warning = false;
        return true;
    }

    private boolean fail(String message) {
        lastMessage = message;
        warning = false;
        return false;
    }

    private int failInt(String message) {
        fail(message);
        return -1;
    }

    private static String action(String actionType) {
        return actionType == null ? "" : actionType.trim().toLowerCase();
    }

    private Course requireCourse(String name) {
        Optional<Course> c = courseManager.findByName(name);
        return c.orElseThrow(() -> new ValidationException("No course named \"" + name + "\"."));
    }

    private Event requireEvent(String title, LocalDate date) {
        String t = title == null ? "" : title.trim();
        return store.getEvents().stream()
                .filter(e -> e.getTitle().equalsIgnoreCase(t) && e.getDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new ValidationException("No event \"" + title + "\" on " + date + "."));
    }

    /**
     * @param text ISO date text
     * @return the date
     * @throws ValidationException when the text is not a date
     */
    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Date is required (yyyy-MM-dd).");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Date must look like yyyy-MM-dd: " + text);
        }
    }

    /**
     * @param text {@code HH:mm} text
     * @param what name for the error message
     * @return the time
     * @throws ValidationException when the text is not a time
     */
    public static LocalTime parseTime(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(what + " is required (HH:mm).");
        }
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException(what + " must look like HH:mm: " + text);
        }
    }

    private static LocalDate[] parseRange(String range) {
        if (range == null || range.isBlank()) {
            return new LocalDate[]{null, null};
        }
        String r = range.trim();
        int sep = r.indexOf("..");
        if (sep < 0) {
            LocalDate d = parseDate(r);
            return new LocalDate[]{d, d};
        }
        String a = r.substring(0, sep).trim();
        String b = r.substring(sep + 2).trim();
        return new LocalDate[]{a.isEmpty() ? null : parseDate(a), b.isEmpty() ? null : parseDate(b)};
    }
}
