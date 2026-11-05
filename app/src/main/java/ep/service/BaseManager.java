package ep.service;

import ep.model.ValidationException;
import ep.storage.DataStore;
import ep.storage.LocalStorage;

/**
 * Shared plumbing for the managers: access to the in-memory
 * {@link DataStore}, saving through {@link LocalStorage}, and a few input
 * checks that every manager needs.
 */
public abstract class BaseManager {

    /** In-memory data shared by all managers. */
    protected final DataStore store;
    /** Where changes are written after every successful operation. */
    protected final LocalStorage storage;

    /**
     * @param store   shared in-memory data
     * @param storage persistence used after each change
     */
    protected BaseManager(DataStore store, LocalStorage storage) {
        this.store = store;
        this.storage = storage;
    }

    /** Writes the current store to disk. */
    protected void persist() {
        storage.save(store);
    }

    /**
     * @param value text to check
     * @param what  name used in the error message
     * @return the trimmed text
     * @throws ValidationException when the text is {@code null} or blank
     */
    protected static String requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(what + " cannot be empty.");
        }
        return value.trim();
    }

    /**
     * @param value object to check
     * @param what  name used in the error message
     * @throws ValidationException when the object is {@code null}
     */
    protected static void requireNonNull(Object value, String what) {
        if (value == null) {
            throw new ValidationException(what + " is required.");
        }
    }

    /**
     * @param courseId course id to check, may be {@code null}
     * @return the same id, or {@code null} if the course does not exist
     */
    protected String validCourseId(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return null;
        }
        return store.findCourse(courseId).map(c -> c.getId()).orElse(null);
    }
}
