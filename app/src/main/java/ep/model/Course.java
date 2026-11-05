package ep.model;

import java.util.Objects;

/**
 * A course is a name plus a color. It is used only to group related
 * {@link Event}s and {@link Assignment}s on the calendar.
 */
public class Course {

    private final String id;
    private String name;
    private String color;

    /**
     * Creates a course.
     *
     * @param id    unique identifier
     * @param name  non-blank, unique course name
     * @param color color in {@code #RRGGBB} form
     */
    public Course(String id, String name, String color) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.color = color;
    }

    /** @return the unique identifier of this course */
    public String getId() {
        return id;
    }

    /** @return the display name */
    public String getName() {
        return name;
    }

    /** @param name new display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the color in {@code #RRGGBB} form */
    public String getColor() {
        return color;
    }

    /** @param color new color in {@code #RRGGBB} form */
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Course && ((Course) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
