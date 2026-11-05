package ep.service;

import ep.model.CalendarFilter;
import ep.model.CalendarItem;
import ep.storage.DataStore;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case 4, <em>View, Search and Filter the Calendar</em>. Read-only:
 * it collects fixed events and study blocks for a month or a week and
 * applies a {@link CalendarFilter}. Viewing never changes the saved data.
 */
public class CalendarManager {

    /** Sort order used by the views: date, then all-day first, then start time. */
    public static final Comparator<CalendarItem> ORDER = Comparator
            .comparing(CalendarItem::getDate)
            .thenComparing(item -> !item.isAllDay())
            .thenComparing(item -> item.isAllDay() ? java.time.LocalTime.MIN : item.getStartTime())
            .thenComparing(CalendarItem::getTitle);

    private final DataStore store;

    /** @param store shared in-memory data */
    public CalendarManager(DataStore store) {
        this.store = store;
    }

    /**
     * Applies a filter to every event and study block.
     *
     * @param filter conditions; {@code null} or empty means everything
     * @return matching items in calendar order
     */
    public List<CalendarItem> filter(CalendarFilter filter) {
        return store.getAllItems().stream()
                .filter(item -> filter == null || filter.matches(item))
                .sorted(ORDER)
                .collect(Collectors.toList());
    }

    /**
     * Items of one day that pass the filter.
     *
     * @param day    the day
     * @param filter conditions or {@code null}
     * @return matching items in calendar order
     */
    public List<CalendarItem> itemsOn(LocalDate day, CalendarFilter filter) {
        return filter(filter).stream().filter(i -> i.getDate().equals(day)).collect(Collectors.toList());
    }

    /**
     * Items in an inclusive date range that pass the filter.
     *
     * @param from   first day
     * @param to     last day
     * @param filter conditions or {@code null}
     * @return matching items in calendar order
     */
    public List<CalendarItem> itemsBetween(LocalDate from, LocalDate to, CalendarFilter filter) {
        return filter(filter).stream()
                .filter(i -> !i.getDate().isBefore(from) && !i.getDate().isAfter(to))
                .collect(Collectors.toList());
    }

    /**
     * Items for a month view. The range covers the whole six-week grid that
     * is drawn for the month.
     *
     * @param month  the month
     * @param filter conditions or {@code null}
     * @return matching items in calendar order
     */
    public List<CalendarItem> itemsForMonth(YearMonth month, CalendarFilter filter) {
        LocalDate first = firstGridDay(month);
        return itemsBetween(first, first.plusDays(6 * 7 - 1), filter);
    }

    /**
     * Items for the week (Monday to Sunday) that contains {@code anyDay}.
     *
     * @param anyDay a day in the week
     * @param filter conditions or {@code null}
     * @return matching items in calendar order
     */
    public List<CalendarItem> itemsForWeek(LocalDate anyDay, CalendarFilter filter) {
        LocalDate monday = startOfWeek(anyDay);
        return itemsBetween(monday, monday.plusDays(6), filter);
    }

    /**
     * @param anyDay any date
     * @return the Monday of that date's week
     */
    public static LocalDate startOfWeek(LocalDate anyDay) {
        return anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * @param month a month
     * @return the Monday on or before the first day of the month
     */
    public static LocalDate firstGridDay(YearMonth month) {
        return startOfWeek(month.atDay(1));
    }

    /** @return total number of events and study blocks stored */
    public int countItems() {
        return store.getEvents().size() + store.getBlocks().size();
    }
}
