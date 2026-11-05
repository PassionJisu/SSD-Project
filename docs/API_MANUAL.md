# Enough Procrastination — API Manual (beta, CP3)

Jisu Han (233950) · Dept. Software Engineering, Chonnam National University

The full, browsable reference is the Javadoc in [`docs/api/index.html`](api/index.html)
(regenerate with `app\build.ps1 -Docs`). This page summarizes the public API and maps it to the
*Function Specification* (section VIII) of the design document.

Screenshots of the generated manual: `docs/screenshots/api-manual-*.png`.

---

## 1. Entry point: `ep.api.EnoughProcrastination`

```java
EnoughProcrastination app = new EnoughProcrastination(Paths.get(System.getProperty("user.home"), ".enough-procrastination"));
```

Creating the facade loads the saved files (or starts empty). It owns one `DataStore`, one
`LocalStorage` and the five managers. Everything the GUI does goes through this object.

| Function in the design document | Implementation | Returns |
|---|---|---|
| `boolean manageCourse(String actionType, String name, String color)` | `EnoughProcrastination.manageCourse` → `CourseManager` | `true` if saved |
| `boolean manageEvent(String actionType, String title, String date, String startTime, String endTime, boolean isAllDay)` | `EnoughProcrastination.manageEvent` → `EventManager` | `true` if saved |
| `list planAssignment(String title, String dueDate, int totalHours, int blockLength)` | `EnoughProcrastination.planAssignment` → `AssignmentManager` + `ScheduleManager` | `List<StudyBlock>` (empty if rejected) |
| `list filterCalendar(String keyword, String courseName, String dateRange)` | `EnoughProcrastination.filterCalendar` → `CalendarManager` | `List<CalendarItem>` |
| `int trackProgress(object assignment, int completedHours, boolean isComplete)` | `EnoughProcrastination.trackProgress` → `ProgressManager` | remaining hours, `-1` if rejected |

After any call, `getLastMessage()` holds the error text, the shortage warning or a short summary,
and `hasWarning()` is `true` when some hours could not be scheduled.

### Conventions

* Dates: `yyyy-MM-dd`. Times: `HH:mm`. Date range: `from..to`, `from..`, `..to` or a single date.
* `actionType` is one of `"add"`, `"edit"`, `"delete"` (case-insensitive).
* Invalid input never changes the saved data; the managers throw `ValidationException`, which the
  facade turns into `false` / empty list / `-1` plus a message.

### Examples

```java
app.manageCourse("add", "Software Engineering", "#4F6BED");          // true
app.manageCourse("add", "software engineering", "#000000");          // false, "A course named ... already exists."
app.manageEvent("add", "SE lecture", "2026-11-09", "09:00", "10:30", false);
app.manageEvent("add", "Career fair", "2026-11-13", null, null, true); // all-day event blocks the whole day

List<StudyBlock> blocks = app.planAssignment("Requirements report", "2026-11-12", 6, 2);
if (app.hasWarning()) System.out.println(app.getLastMessage());     // "... 2 h could not be scheduled before 2026-11-12."

List<CalendarItem> hits = app.filterCalendar("report", "", "2026-11-02..2026-11-08");
int left = app.trackProgress(assignment, 2, false);                  // e.g. 4
app.trackProgress(assignment, 0, true);                              // mark complete -> 0
```

---

## 2. Typed managers (used by the GUI)

| Use case | Manager | Key methods |
|---|---|---|
| UC1 Manage Courses | `CourseManager` | `addCourse(name, color)`, `editCourse(id, name, color)`, `deleteCourse(id)`, `findByName(name)`, `getCourses()` |
| UC2 Manage Events | `EventManager` | `addEvent(title, date, allDay, start, end, courseId)`, `editEvent(...)`, `deleteEvent(id)`; both add/edit return `EventResult` with any re-planned assignments |
| UC3 Plan Assignment | `AssignmentManager` | `addAssignment(title, dueDate, estimatedHours, blockLength, courseId)`, `editAssignment(...)`, `deleteAssignment(id)`, `replan(assignment)` → `PlanResult` |
| (planner) | `ScheduleManager` | `plan(assignment, hours)`, `findLatestFreeStart(day, hours, pending)`, `isFree(day, start, end, pending)`, `findBlocksOverlapping(event)` |
| UC4 View / Search / Filter | `CalendarManager` | `filter(CalendarFilter)`, `itemsForMonth(month, filter)`, `itemsForWeek(day, filter)`, `itemsBetween(from, to, filter)` |
| UC5 Track Progress | `ProgressManager` | `getReport(assignment)`, `recordProgress(id, hours)`, `markComplete(id)`, `reopen(id, completedHours)` |

### Rules enforced by the managers

* **Course**: name not blank, unique (case-insensitive). Delete only clears `courseId` on events,
  assignments and study blocks.
* **Event**: title not blank; a timed event must end after it starts. Adding/editing an event that
  overlaps a study block keeps the event and re-plans that assignment (`EventResult.getReplans()`).
* **Assignment**: title not blank; due date today or later; estimated hours ≥ 1;
  1 ≤ block length ≤ estimated hours (and ≤ 12). Edit re-plans the remaining hours; delete removes the
  blocks too.
* **Progress**: recorded hours > 0 and completed total ≤ estimate. Remaining hours are re-planned so the
  calendar shows only unfinished blocks; reaching the estimate (or *Mark complete*) removes all blocks.

### Placement rules of `ScheduleManager`

1. Candidate days run from the day **before** the due date back to today (never in the past).
2. Within a day, slots between **09:00 and 22:00** are tried from the latest to the earliest in
   30-minute steps.
3. A slot is free when it touches no fixed event and no study block. An all-day event blocks its day.
4. First pass: at most one block per day, latest day first. Further passes add more blocks per day while
   hours remain and slots exist.
5. Anything still left is reported as `PlanResult.getUnscheduledHours()` (shortage warning).

---

## 3. Model

| Class | Purpose |
|---|---|
| `Course` | id, name, color (`#RRGGBB`) |
| `Event` (implements `CalendarItem`) | fixed commitment: title, date, all-day flag, start/end, optional course |
| `Assignment` | title, due date, estimated hours, block length, optional course, completed hours, complete flag; `getRemainingHours()` |
| `StudyBlock` (implements `CalendarItem`) | one generated block: assignment id, date, start/end |
| `CalendarItem` | common view of events and blocks; `overlaps(...)` |
| `CalendarFilter` | keyword / course / type / date range; `matches(item)` |
| `PlanResult` | blocks placed + unscheduled hours; `describe()` |
| `ProgressReport` | estimated / completed / remaining + shortage |
| `ValidationException` | thrown for every rule violation |

---

## 4. Storage

`LocalStorage` writes four plain-text files into the data folder (default
`~/.enough-procrastination`, override with `--data DIR`):

| File | Fields (separated by `|`) |
|---|---|
| `courses.txt` | id, name, color |
| `events.txt` | id, title, date, start, end, allDay, courseId |
| `assignments.txt` | id, title, dueDate, estimatedHours, blockLength, courseId, completedHours, complete |
| `blocks.txt` | id, assignmentId, title, courseId, date, start, end |

`\`, `|` and line breaks inside a field are escaped, so any title round-trips. Files are rewritten
after every successful change and read once at start-up.

---

## 5. Running

```powershell
cd app
.\build.ps1          # compile + jar
.\build.ps1 -Demo    # start with sample data
.\build.ps1 -Run     # start with your own data
.\build.ps1 -Docs    # regenerate docs/api (Javadoc)
java -jar out\enough-procrastination.jar [--data DIR] [--demo] [--screenshot DIR]
```
