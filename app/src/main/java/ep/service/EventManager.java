package ep.service;

import ep.model.Assignment;
import ep.model.Event;
import ep.model.PlanResult;
import ep.model.StudyBlock;
import ep.model.ValidationException;
import ep.storage.DataStore;
import ep.storage.LocalStorage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Use case 2, <em>Manage Events</em>. Adds, edits and deletes fixed events.
 * A timed event must end after it starts. When a new or moved event lands on
 * a study block, the event is kept and the affected assignment is planned
 * again around it.
 */
public class EventManager extends BaseManager {

    /**
     * Result of adding or editing an event: the event itself plus the
     * re-planning results of any assignments whose blocks had to move.
     */
    public static class EventResult {
        private final Event event;
        private final List<PlanResult> replans;

        EventResult(Event event, List<PlanResult> replans) {
            this.event = event;
            this.replans = replans;
        }

        /** @return the saved event */
        public Event getEvent() {
            return event;
        }

        /** @return re-planning results for displaced assignments (may be empty) */
        public List<PlanResult> getReplans() {
            return replans;
        }
    }

    private final ScheduleManager scheduler;
    private final AssignmentManager assignments;

    /**
     * @param store       shared in-memory data
     * @param storage     persistence
     * @param scheduler   used to find blocks that overlap an event
     * @param assignments used to re-plan displaced assignments
     */
    public EventManager(DataStore store, LocalStorage storage, ScheduleManager scheduler,
                        AssignmentManager assignments) {
        super(store, storage);
        this.scheduler = scheduler;
        this.assignments = assignments;
    }

    /** @return all events */
    public List<Event> getEvents() {
        return store.getEvents();
    }

    /**
     * @param id event id
     * @return the event if present
     */
    public Optional<Event> find(String id) {
        return store.findEvent(id);
    }

    /**
     * Adds a fixed event.
     *
     * @param title    non-blank title
     * @param date     day of the event
     * @param allDay   {@code true} for an all-day event
     * @param start    start time (timed events only)
     * @param end      end time, must be after {@code start} (timed events only)
     * @param courseId linked course or {@code null}
     * @return the saved event and any re-planning that it caused
     * @throws ValidationException when a value is invalid; nothing is saved then
     */
    public EventResult addEvent(String title, LocalDate date, boolean allDay,
                                LocalTime start, LocalTime end, String courseId) {
        validate(title, date, allDay, start, end);
        Event e = new Event(DataStore.newId(), title.trim(), date, allDay, start, end, validCourseId(courseId));
        store.addEvent(e);
        List<PlanResult> replans = clearDisplacedBlocks(e);
        persist();
        return new EventResult(e, replans);
    }

    /**
     * Changes an existing event.
     *
     * @param id       event to change
     * @param title    new title
     * @param date     new day
     * @param allDay   {@code true} for an all-day event
     * @param start    new start time (timed events only)
     * @param end      new end time (timed events only)
     * @param courseId new course or {@code null}
     * @return the saved event and any re-planning that it caused
     * @throws ValidationException when a value is invalid; nothing is saved then
     */
    public EventResult editEvent(String id, String title, LocalDate date, boolean allDay,
                                 LocalTime start, LocalTime end, String courseId) {
        Event e = store.findEvent(id).orElseThrow(() -> new ValidationException("Event not found."));
        validate(title, date, allDay, start, end);
        e.setTitle(title.trim());
        e.setDate(date);
        e.setTime(allDay, start, end);
        e.setCourseId(validCourseId(courseId));
        List<PlanResult> replans = clearDisplacedBlocks(e);
        persist();
        return new EventResult(e, replans);
    }

    /**
     * Deletes an event. Study blocks are not touched; the freed time is used
     * the next time an assignment is planned.
     *
     * @param id event id
     * @return {@code true} if an event was removed
     */
    public boolean deleteEvent(String id) {
        Optional<Event> found = store.findEvent(id);
        if (found.isEmpty()) {
            return false;
        }
        store.removeEvent(found.get());
        persist();
        return true;
    }

    /**
     * Finds study blocks that overlap the event and re-plans their
     * assignments. The fixed event always wins.
     */
    private List<PlanResult> clearDisplacedBlocks(Event e) {
        Set<String> affected = new LinkedHashSet<>();
        for (StudyBlock b : scheduler.findBlocksOverlapping(e)) {
            affected.add(b.getAssignmentId());
        }
        List<PlanResult> replans = new ArrayList<>();
        for (String assignmentId : affected) {
            Optional<Assignment> a = store.findAssignment(assignmentId);
            a.ifPresent(x -> replans.add(assignments.replan(x)));
        }
        return replans;
    }

    private static void validate(String title, LocalDate date, boolean allDay, LocalTime start, LocalTime end) {
        requireText(title, "Title");
        requireNonNull(date, "Date");
        if (!allDay) {
            requireNonNull(start, "Start time");
            requireNonNull(end, "End time");
            if (!end.isAfter(start)) {
                throw new ValidationException("A timed event must end after it starts.");
            }
        }
    }
}
