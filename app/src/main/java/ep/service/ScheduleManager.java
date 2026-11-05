package ep.service;

import ep.model.Assignment;
import ep.model.CalendarItem;
import ep.model.Event;
import ep.model.PlanResult;
import ep.model.StudyBlock;
import ep.storage.DataStore;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The planner. Given an assignment and a number of hours it cuts the hours
 * into blocks of the preferred length and searches <em>backward</em> from the
 * deadline for free slots.
 *
 * <p>Placement rules (plain enough to be checked on the calendar):</p>
 * <ul>
 *   <li>Blocks go on days strictly before the due date and never in the past.</li>
 *   <li>Within a day, candidate slots lie between {@link #DAY_START} and
 *       {@link #DAY_END} and are tried from the latest to the earliest in
 *       {@link #STEP_MINUTES} steps.</li>
 *   <li>A slot is free when it touches no fixed event and no other study
 *       block. An all-day event blocks its whole day.</li>
 *   <li>The planner first places at most one block per day (starting at the day
 *       before the deadline), then makes further passes to add a second, third,
 *       ... block per day while hours remain and a slot can still be found.</li>
 *   <li>If hours remain after that, they are reported as unscheduled.</li>
 * </ul>
 */
public class ScheduleManager {

    /** Earliest time of day a study block may start. */
    public static final LocalTime DAY_START = LocalTime.of(9, 0);
    /** Latest time of day a study block may end. */
    public static final LocalTime DAY_END = LocalTime.of(22, 0);
    /** Granularity of the backward search within a day. */
    public static final int STEP_MINUTES = 30;

    private final DataStore store;
    private final Clock clock;

    /**
     * @param store shared in-memory data (used to find busy time)
     * @param clock source of "now"; blocks are never placed in the past
     */
    public ScheduleManager(DataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /** @return today's date according to the planner clock */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Plans {@code hours} of work for an assignment. Existing blocks of the
     * same assignment are treated as busy time, so callers normally remove
     * them first (see {@link AssignmentManager#replan(Assignment)}). The
     * returned blocks are <em>not</em> added to the store.
     *
     * @param assignment the assignment (gives deadline, block length, title)
     * @param hours      number of hours to place
     * @return placed blocks and the number of hours that did not fit
     */
    public PlanResult plan(Assignment assignment, int hours) {
        List<StudyBlock> placed = new ArrayList<>();
        int remaining = Math.max(0, hours);
        int blockLength = Math.max(1, assignment.getBlockLength());

        List<LocalDate> days = candidateDays(assignment.getDueDate());
        boolean progress = true;
        while (remaining > 0 && progress) {
            progress = false;
            for (LocalDate day : days) {
                if (remaining <= 0) {
                    break;
                }
                int len = Math.min(blockLength, remaining);
                LocalTime start = findLatestFreeStart(day, len, placed);
                if (start != null) {
                    placed.add(new StudyBlock(DataStore.newId(), assignment.getId(), assignment.getTitle(),
                            assignment.getCourseId(), day, start, start.plusHours(len)));
                    remaining -= len;
                    progress = true;
                }
            }
        }
        return new PlanResult(assignment, placed, remaining);
    }

    /**
     * Days on which a block may be placed, latest first: from the day before
     * the deadline back to today.
     */
    private List<LocalDate> candidateDays(LocalDate dueDate) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate today = today();
        for (LocalDate d = dueDate.minusDays(1); !d.isBefore(today); d = d.minusDays(1)) {
            days.add(d);
        }
        return days;
    }

    /**
     * Finds the latest start time on {@code day} for a block of
     * {@code hours} that is completely free.
     *
     * @param day     the day to search
     * @param hours   block length in hours
     * @param pending blocks already chosen in this planning run
     * @return the start time or {@code null} if nothing fits
     */
    public LocalTime findLatestFreeStart(LocalDate day, int hours, List<StudyBlock> pending) {
        LocalTime earliest = DAY_START;
        if (day.equals(today())) {
            LocalTime now = LocalTime.now(clock);
            int minute = ((now.getMinute() + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES;
            LocalTime rounded = minute >= 60
                    ? now.withMinute(0).withSecond(0).withNano(0).plusHours(1)
                    : now.withMinute(minute).withSecond(0).withNano(0);
            if (rounded.isAfter(earliest)) {
                earliest = rounded;
            }
        }
        LocalTime start = DAY_END.minusHours(hours);
        while (!start.isBefore(earliest)) {
            if (isFree(day, start, start.plusHours(hours), pending)) {
                return start;
            }
            if (start.equals(LocalTime.MIDNIGHT)) {
                break;
            }
            start = start.minusMinutes(STEP_MINUTES);
        }
        return null;
    }

    /**
     * Tells whether the time range is free of fixed events and study blocks.
     *
     * @param day     the day
     * @param start   start of the range
     * @param end     end of the range (exclusive)
     * @param pending extra items that count as busy (may be {@code null})
     * @return {@code true} if nothing occupies the range
     */
    public boolean isFree(LocalDate day, LocalTime start, LocalTime end, List<? extends CalendarItem> pending) {
        for (CalendarItem item : store.getAllItems()) {
            if (item.overlaps(day, start, end)) {
                return false;
            }
        }
        if (pending != null) {
            for (CalendarItem item : pending) {
                if (item.overlaps(day, start, end)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Study blocks that share time with the given event. Used when a fixed
     * event is added or moved: the event is kept and these blocks are
     * cleared so the work can be planned again.
     *
     * @param event a fixed event
     * @return overlapping blocks (possibly empty)
     */
    public List<StudyBlock> findBlocksOverlapping(Event event) {
        List<StudyBlock> hits = new ArrayList<>();
        for (StudyBlock b : store.getBlocks()) {
            if (b.overlaps(event)) {
                hits.add(b);
            }
        }
        return hits;
    }

    /** @return the current date-time of the planner clock */
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
