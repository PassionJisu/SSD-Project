package ep.service;

import ep.model.Assignment;
import ep.model.Course;
import ep.model.Event;
import ep.model.StudyBlock;
import ep.model.ValidationException;
import ep.storage.DataStore;
import ep.storage.LocalStorage;

import java.util.List;
import java.util.Optional;

/**
 * Use case 1, <em>Manage Courses</em>. Adds, edits and deletes courses and
 * enforces the two rules: a name cannot be empty and cannot be repeated, and
 * deleting a course only clears the link on events and assignments.
 */
public class CourseManager extends BaseManager {

    /** Default color offered to a new course. */
    public static final String DEFAULT_COLOR = "#4F6BED";

    /**
     * @param store   shared in-memory data
     * @param storage persistence
     */
    public CourseManager(DataStore store, LocalStorage storage) {
        super(store, storage);
    }

    /** @return all courses in insertion order */
    public List<Course> getCourses() {
        return store.getCourses();
    }

    /**
     * @param id course id
     * @return the course if it exists
     */
    public Optional<Course> find(String id) {
        return store.findCourse(id);
    }

    /**
     * @param name course name (case-insensitive)
     * @return the course with that name if it exists
     */
    public Optional<Course> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String n = name.trim();
        return store.getCourses().stream().filter(c -> c.getName().equalsIgnoreCase(n)).findFirst();
    }

    /**
     * Adds a course.
     *
     * @param name  non-blank, unique name
     * @param color color in {@code #RRGGBB} form; {@link #DEFAULT_COLOR} when blank
     * @return the saved course
     * @throws ValidationException if the name is blank or already used
     */
    public Course addCourse(String name, String color) {
        String n = requireText(name, "Course name");
        checkUnique(n, null);
        Course c = new Course(DataStore.newId(), n, normalizeColor(color));
        store.addCourse(c);
        persist();
        return c;
    }

    /**
     * Changes the name and/or color of a course. Linked events and
     * assignments stay linked.
     *
     * @param id    course to change
     * @param name  new non-blank, unique name
     * @param color new color
     * @return the updated course
     * @throws ValidationException if the course is unknown or the name is invalid
     */
    public Course editCourse(String id, String name, String color) {
        Course c = store.findCourse(id).orElseThrow(() -> new ValidationException("Course not found."));
        String n = requireText(name, "Course name");
        checkUnique(n, id);
        c.setName(n);
        c.setColor(normalizeColor(color));
        persist();
        return c;
    }

    /**
     * Deletes a course. Events, assignments and study blocks that pointed to
     * it are kept and simply become unlinked.
     *
     * @param id course to delete
     * @return {@code true} if a course was removed
     */
    public boolean deleteCourse(String id) {
        Optional<Course> found = store.findCourse(id);
        if (found.isEmpty()) {
            return false;
        }
        for (Event e : store.getEvents()) {
            if (id.equals(e.getCourseId())) {
                e.setCourseId(null);
            }
        }
        for (Assignment a : store.getAssignments()) {
            if (id.equals(a.getCourseId())) {
                a.setCourseId(null);
            }
        }
        for (StudyBlock b : store.getBlocks()) {
            if (id.equals(b.getCourseId())) {
                b.setCourseId(null);
            }
        }
        store.removeCourse(found.get());
        persist();
        return true;
    }

    private void checkUnique(String name, String exceptId) {
        for (Course c : store.getCourses()) {
            if (c.getName().equalsIgnoreCase(name) && !c.getId().equals(exceptId)) {
                throw new ValidationException("A course named \"" + name + "\" already exists.");
            }
        }
    }

    private static String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return DEFAULT_COLOR;
        }
        String c = color.trim();
        if (!c.startsWith("#")) {
            c = "#" + c;
        }
        if (!c.matches("#[0-9A-Fa-f]{6}")) {
            throw new ValidationException("Color must look like #RRGGBB.");
        }
        return c.toUpperCase();
    }
}
