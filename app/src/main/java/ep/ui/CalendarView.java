package ep.ui;

import ep.model.CalendarItem;
import ep.model.Course;

import javax.swing.JPanel;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class of the month and week views. Subclasses paint the calendar and
 * register the on-screen rectangle of every item and day so that clicks can
 * be mapped back to the model.
 */
public abstract class CalendarView extends JPanel {

    /** Painted item with its screen rectangle. */
    protected static class Hit {
        final Rectangle rect;
        final CalendarItem item;
        final LocalDate day;

        Hit(Rectangle rect, CalendarItem item, LocalDate day) {
            this.rect = rect;
            this.item = item;
            this.day = day;
        }
    }

    /** Items currently shown. */
    protected List<CalendarItem> items = new ArrayList<>();
    /** Day in focus (determines month / week). */
    protected LocalDate anchor = LocalDate.now();
    /** Today's date (for highlighting). */
    protected LocalDate today = LocalDate.now();
    /** Course lookup used for colors. */
    protected Function<String, Optional<Course>> courses = id -> Optional.empty();
    /** Rectangles painted in the last paint call. */
    protected final List<Hit> hits = new ArrayList<>();

    private Consumer<CalendarItem> onItem = item -> { };
    private Consumer<LocalDate> onDay = day -> { };

    /** Creates the view and wires mouse handling. */
    protected CalendarView() {
        setBackground(UiTheme.SURFACE);
        setOpaque(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Hit h : hits) {
                    if (h.item != null && h.rect.contains(e.getPoint())) {
                        onItem.accept(h.item);
                        return;
                    }
                }
                if (e.getClickCount() == 2) {
                    for (Hit h : hits) {
                        if (h.item == null && h.rect.contains(e.getPoint())) {
                            onDay.accept(h.day);
                            return;
                        }
                    }
                }
            }
        });
    }

    /**
     * Replaces the data shown by the view.
     *
     * @param anchor  day in focus
     * @param today   today's date
     * @param items   items to draw
     * @param courses course lookup for colors
     */
    public void setData(LocalDate anchor, LocalDate today, List<CalendarItem> items,
                        Function<String, Optional<Course>> courses) {
        this.anchor = anchor;
        this.today = today;
        this.items = new ArrayList<>(items);
        this.courses = courses;
        repaint();
    }

    /** @param listener called when an item chip is clicked */
    public void setOnItemClicked(Consumer<CalendarItem> listener) {
        this.onItem = listener;
    }

    /** @param listener called when an empty day area is double-clicked */
    public void setOnDayDoubleClicked(Consumer<LocalDate> listener) {
        this.onDay = listener;
    }

    /** @return the day in focus */
    public LocalDate getAnchor() {
        return anchor;
    }

    /**
     * @param day a date
     * @return items of that day in display order
     */
    protected List<CalendarItem> itemsOn(LocalDate day) {
        List<CalendarItem> out = new ArrayList<>();
        for (CalendarItem i : items) {
            if (i.getDate().equals(day)) {
                out.add(i);
            }
        }
        return out;
    }
}
