package ep.model;

/**
 * Thrown by the managers when user input breaks one of the rules in the
 * requirements (blank or duplicate course name, event that ends before it
 * starts, progress larger than the estimate, and so on). The GUI shows the
 * message and keeps the saved data unchanged.
 */
public class ValidationException extends RuntimeException {

    /** @param message text to show the student */
    public ValidationException(String message) {
        super(message);
    }
}
