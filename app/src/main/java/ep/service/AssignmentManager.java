package ep.service;

import ep.model.Assignment;
import ep.model.PlanResult;
import ep.model.StudyBlock;
import ep.model.ValidationException;
import ep.storage.DataStore;
import ep.storage.LocalStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Use case 3, <em>Plan an Assignment from a Deadline</em>. Validates the
 * assignment, asks the {@link ScheduleManager} for study blocks, stores the
 * result and reports any shortage. Editing rebuilds the blocks; deleting
 * removes the assignment together with its blocks.
 */
public class AssignmentManager extends BaseManager {

    private final ScheduleManager scheduler;

    /**
     * @param store     shared in-memory data
     * @param storage   persistence
     * @param scheduler planner used to place study blocks
     */
    public AssignmentManager(DataStore store, LocalStorage storage, ScheduleManager scheduler) {
        super(store, storage);
        this.scheduler = scheduler;
    }

    /** @return all assignments */
    public List<Assignment> getAssignments() {
        return store.getAssignments();
    }

    /**
     * @param id assignment id
     * @return the assignment if present
     */
    public Optional<Assignment> find(String id) {
        return store.findAssignment(id);
    }

    /**
     * @param assignmentId assignment id
     * @return its study blocks in calendar order
     */
    public List<StudyBlock> getBlocks(String assignmentId) {
        return store.getBlocksOf(assignmentId);
    }

    /**
     * Adds an assignment and plans its study blocks.
     *
     * @param title          non-blank title
     * @param dueDate        deadline, today or later
     * @param estimatedHours total hours, at least 1
     * @param blockLength    preferred block length in hours, between 1 and the estimate
     * @param courseId       linked course or {@code null}
     * @return the placed blocks and any shortage
     * @throws ValidationException when a value is invalid; nothing is saved then
     */
    public PlanResult addAssignment(String title, LocalDate dueDate, int estimatedHours,
                                    int blockLength, String courseId) {
        validate(title, dueDate, estimatedHours, blockLength);
        Assignment a = new Assignment(DataStore.newId(), title.trim(), dueDate, estimatedHours,
                blockLength, validCourseId(courseId));
        store.addAssignment(a);
        return replan(a);
    }

    /**
     * Changes an assignment and rebuilds its study blocks for the remaining
     * hours.
     *
     * @param id             assignment to change
     * @param title          new title
     * @param dueDate        new deadline
     * @param estimatedHours new estimate (must not be below completed hours)
     * @param blockLength    new block length
     * @param courseId       new course or {@code null}
     * @return the placed blocks and any shortage
     * @throws ValidationException when a value is invalid; nothing is saved then
     */
    public PlanResult editAssignment(String id, String title, LocalDate dueDate, int estimatedHours,
                                     int blockLength, String courseId) {
        Assignment a = store.findAssignment(id).orElseThrow(() -> new ValidationException("Assignment not found."));
        validate(title, dueDate, estimatedHours, blockLength);
        if (estimatedHours < a.getCompletedHours()) {
            throw new ValidationException("Estimated hours cannot be below the "
                    + a.getCompletedHours() + " h already completed.");
        }
        a.setTitle(title.trim());
        a.setDueDate(dueDate);
        a.setEstimatedHours(estimatedHours);
        a.setBlockLength(blockLength);
        a.setCourseId(validCourseId(courseId));
        if (a.isComplete() && a.getCompletedHours() < estimatedHours) {
            a.setComplete(false);
        }
        return replan(a);
    }

    /**
     * Deletes an assignment and all of its study blocks.
     *
     * @param id assignment id
     * @return {@code true} if something was removed
     */
    public boolean deleteAssignment(String id) {
        Optional<Assignment> found = store.findAssignment(id);
        if (found.isEmpty()) {
            return false;
        }
        store.removeBlocksOf(id);
        store.removeAssignment(found.get());
        persist();
        return true;
    }

    /**
     * Throws away the assignment's current blocks and plans its remaining
     * hours again. Called after add/edit, after progress is recorded, and
     * when a new fixed event lands on one of its blocks.
     *
     * @param a the assignment
     * @return the new blocks and any shortage
     */
    public PlanResult replan(Assignment a) {
        store.removeBlocksOf(a.getId());
        PlanResult result = scheduler.plan(a, a.getRemainingHours());
        for (StudyBlock b : result.getBlocks()) {
            store.addBlock(b);
        }
        persist();
        return result;
    }

    private void validate(String title, LocalDate dueDate, int estimatedHours, int blockLength) {
        requireText(title, "Title");
        requireNonNull(dueDate, "Due date");
        if (dueDate.isBefore(scheduler.today())) {
            throw new ValidationException("Due date must be today or later.");
        }
        if (estimatedHours < 1) {
            throw new ValidationException("Estimated hours must be at least 1.");
        }
        if (blockLength < 1 || blockLength > estimatedHours) {
            throw new ValidationException("Block length must be between 1 and the estimated hours.");
        }
        if (blockLength > 12) {
            throw new ValidationException("Block length cannot exceed 12 hours.");
        }
    }
}
