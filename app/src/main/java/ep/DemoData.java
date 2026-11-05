package ep;

import ep.api.EnoughProcrastination;
import ep.model.Course;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Fills an empty data folder with a realistic sample semester so the beta
 * can be demonstrated (and screenshotted) without typing everything in.
 */
public final class DemoData {

    private DemoData() {
    }

    /**
     * Seeds courses, weekly lectures, an all-day event, three assignments
     * and some recorded progress, all relative to today.
     *
     * @param app application core backed by an empty store
     */
    public static void seed(EnoughProcrastination app) {
        if (!app.courses().getCourses().isEmpty()) {
            return;
        }
        LocalDate today = app.scheduler().today();
        Course se = app.courses().addCourse("Software Engineering", "#4F6BED");
        Course algo = app.courses().addCourse("Algorithms", "#7C5CE6");
        Course db = app.courses().addCourse("Database", "#2BA47A");
        Course personal = app.courses().addCourse("Personal", "#E8903A");

        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        for (int w = 0; w < 5; w++) {
            LocalDate m = monday.plusWeeks(w);
            app.events().addEvent("Software Engineering lecture", m, false,
                    LocalTime.of(9, 0), LocalTime.of(10, 30), se.getId());
            app.events().addEvent("Algorithms lecture", m.plusDays(1), false,
                    LocalTime.of(13, 0), LocalTime.of(14, 30), algo.getId());
            app.events().addEvent("Team meeting", m.plusDays(2), false,
                    LocalTime.of(15, 0), LocalTime.of(16, 0), se.getId());
            app.events().addEvent("Database lecture", m.plusDays(3), false,
                    LocalTime.of(10, 0), LocalTime.of(11, 30), db.getId());
            app.events().addEvent("Gym", m.plusDays(4), false,
                    LocalTime.of(18, 0), LocalTime.of(19, 30), personal.getId());
        }
        app.events().addEvent("Career fair", today.plusDays(8), true, null, null, null);
        app.events().addEvent("Advisor meeting", today.plusDays(2), false,
                LocalTime.of(14, 0), LocalTime.of(15, 0), personal.getId());

        app.assignments().addAssignment("Requirements report", today.plusDays(6), 6, 2, se.getId());
        app.assignments().addAssignment("Problem set 4", today.plusDays(4), 5, 1, algo.getId());
        app.assignments().addAssignment("ER modeling exercise", today.plusDays(12), 8, 2, db.getId());

        app.assignments().getAssignments().stream()
                .filter(a -> a.getTitle().equals("Requirements report"))
                .findFirst()
                .ifPresent(a -> app.progress().recordProgress(a.getId(), 2));
    }
}
