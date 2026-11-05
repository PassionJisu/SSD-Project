package ep.storage;

import ep.model.Assignment;
import ep.model.CalendarItem;
import ep.model.Course;
import ep.model.Event;
import ep.model.StudyBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * In-memory collections of all program data. The managers read and change
 * this object; {@link LocalStorage} loads it from and writes it to disk.
 */
public class DataStore {

    private final List<Course> courses = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<Assignment> assignments = new ArrayList<>();
    private final List<StudyBlock> blocks = new ArrayList<>();

    /** @return a fresh unique id */
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    // ---- courses -----------------------------------------------------

    /** @return read-only list of courses */
    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    /** @param course course to add */
    public void addCourse(Course course) {
        courses.add(course);
    }

    /** @param course course to remove */
    public void removeCourse(Course course) {
        courses.remove(course);
    }

    /**
     * @param id course id
     * @return the course if present
     */
    public Optional<Course> findCourse(String id) {
        return courses.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    // ---- events ------------------------------------------------------

    /** @return read-only list of events */
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** @param event event to add */
    public void addEvent(Event event) {
        events.add(event);
    }

    /** @param event event to remove */
    public void removeEvent(Event event) {
        events.remove(event);
    }

    /**
     * @param id event id
     * @return the event if present
     */
    public Optional<Event> findEvent(String id) {
        return events.stream().filter(e -> e.getId().equals(id)).findFirst();
    }

    // ---- assignments -------------------------------------------------

    /** @return read-only list of assignments */
    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    /** @param assignment assignment to add */
    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
    }

    /** @param assignment assignment to remove */
    public void removeAssignment(Assignment assignment) {
        assignments.remove(assignment);
    }

    /**
     * @param id assignment id
     * @return the assignment if present
     */
    public Optional<Assignment> findAssignment(String id) {
        return assignments.stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    // ---- study blocks ------------------------------------------------

    /** @return read-only list of study blocks */
    public List<StudyBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    /** @param block block to add */
    public void addBlock(StudyBlock block) {
        blocks.add(block);
    }

    /** @param block block to remove */
    public void removeBlock(StudyBlock block) {
        blocks.remove(block);
    }

    /**
     * @param assignmentId owning assignment
     * @return blocks of that assignment, in calendar order
     */
    public List<StudyBlock> getBlocksOf(String assignmentId) {
        return blocks.stream()
                .filter(b -> b.getAssignmentId().equals(assignmentId))
                .sorted((a, b) -> a.getDate().equals(b.getDate())
                        ? a.getStartTime().compareTo(b.getStartTime())
                        : a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /** @param assignmentId owning assignment whose blocks are removed */
    public void removeBlocksOf(String assignmentId) {
        blocks.removeIf(b -> b.getAssignmentId().equals(assignmentId));
    }

    // ---- combined view -----------------------------------------------

    /** @return every event and study block as one list */
    public List<CalendarItem> getAllItems() {
        List<CalendarItem> all = new ArrayList<>(events);
        all.addAll(blocks);
        return all;
    }

    /** Removes everything. Used when loading from disk. */
    public void clear() {
        courses.clear();
        events.clear();
        assignments.clear();
        blocks.clear();
    }
}
