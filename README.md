# Enough Procrastination

Personal schedule management program for one university student — fixed events on a calendar plus
assignment deadlines that are turned into study blocks placed **backward from the due date**.
Offline Java Swing desktop application; no server, no account, no internet.

Software System Design project · Jisu Han (233950), Dept. Software Engineering, Chonnam National University.

## Check point 3 deliverables

| Item | Where |
|---|---|
| Class diagram | `docs/diagrams/class-diagram-*.png`, explanation in [`docs/CLASS_DIAGRAM.md`](docs/CLASS_DIAGRAM.md), sources `docs/class-diagram.puml` / `docs/class-diagram.html` |
| API manual | Javadoc in `docs/api/index.html`, summary in [`docs/API_MANUAL.md`](docs/API_MANUAL.md), screenshots `docs/screenshots/api-manual-*.png` |
| Beta release screenshots | `docs/screenshots/beta-*.png` |
| Beta source code | `app/src/main/java/ep/**` (this repository) |

## Repository layout

```
app/                     Java 17 Swing beta
  src/main/java/ep/
    Main.java            entry point (--data DIR, --demo, --screenshot DIR)
    DemoData.java        sample semester for demos
    api/                 EnoughProcrastination facade (Function Specification VIII)
    service/             CourseManager, EventManager, AssignmentManager, ScheduleManager,
                         CalendarManager, ProgressManager (one per use case)
    storage/             DataStore (in memory) + LocalStorage (text files)
    model/               Course, Event, Assignment, StudyBlock, CalendarItem, ...
    ui/                  MainFrame, MonthView, WeekView, dialogs and panels
  build.ps1 / build.sh   plain-JDK build (no Maven/Gradle required)
docs/                    diagrams, API manual, screenshots
src/, index.html, ...    React/Vite UI prototype from the earlier design check point
```

## Build and run (JDK 17+)

```powershell
cd app
.\build.ps1          # compile + jar  -> app\out\enough-procrastination.jar
.\build.ps1 -Demo    # start with sample data (temporary folder)
.\build.ps1 -Run     # start with your own data (~/.enough-procrastination)
.\build.ps1 -Docs    # regenerate the Javadoc API manual into docs\api
```

On macOS/Linux use `./build.sh [run|demo|docs]`.

## Features in the beta

* **Courses** — add / edit / delete with a color; names must be unique and non-blank; deleting only
  unlinks events and assignments.
* **Fixed events** — timed or all-day, optional course; a timed event must end after it starts; an
  event that lands on a study block wins and the assignment is re-planned.
* **Assignments** — title, due date, estimated hours, preferred block length, optional course. Blocks
  are placed backward from the day before the deadline into free 09:00–22:00 slots, skipping events
  and other blocks; a shortage warning reports hours that did not fit.
* **Calendar** — month and week views; fixed events solid, study blocks dashed; all-day events in a
  separate row; search by keyword, filter by course, type and date range; clear to show everything.
* **Progress** — record completed hours (> 0, ≤ estimate), see remaining hours, keep only unfinished
  blocks on the calendar, or mark complete to remove them all.
* **Local files** — `courses.txt`, `events.txt`, `assignments.txt`, `blocks.txt` are rewritten after
  every change and loaded at start.
