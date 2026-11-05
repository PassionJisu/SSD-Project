package ep.storage;

import ep.model.Assignment;
import ep.model.Course;
import ep.model.Event;
import ep.model.StudyBlock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads the {@link DataStore} as plain text files in one folder.
 * There is no server and no database: each kind of data gets its own file
 * ({@code courses.txt}, {@code events.txt}, {@code assignments.txt},
 * {@code blocks.txt}) with one record per line and {@code |} between fields.
 */
public class LocalStorage {

    private static final String COURSES = "courses.txt";
    private static final String EVENTS = "events.txt";
    private static final String ASSIGNMENTS = "assignments.txt";
    private static final String BLOCKS = "blocks.txt";

    private final Path directory;

    /** @param directory folder that holds the data files (created on save) */
    public LocalStorage(Path directory) {
        this.directory = directory;
    }

    /** @return the folder that holds the data files */
    public Path getDirectory() {
        return directory;
    }

    /**
     * Reads every data file into the given store. Missing files are treated
     * as empty, so a first start works without any setup.
     *
     * @param store store to fill (it is cleared first)
     */
    public void load(DataStore store) {
        store.clear();
        for (String[] f : readRecords(COURSES)) {
            store.addCourse(new Course(f[0], f[1], f[2]));
        }
        for (String[] f : readRecords(EVENTS)) {
            boolean allDay = Boolean.parseBoolean(f[5]);
            store.addEvent(new Event(f[0], f[1], LocalDate.parse(f[2]), allDay,
                    allDay ? null : LocalTime.parse(f[3]),
                    allDay ? null : LocalTime.parse(f[4]),
                    emptyToNull(f[6])));
        }
        for (String[] f : readRecords(ASSIGNMENTS)) {
            Assignment a = new Assignment(f[0], f[1], LocalDate.parse(f[2]),
                    Integer.parseInt(f[3]), Integer.parseInt(f[4]), emptyToNull(f[5]));
            a.setCompletedHours(Integer.parseInt(f[6]));
            a.setComplete(Boolean.parseBoolean(f[7]));
            store.addAssignment(a);
        }
        for (String[] f : readRecords(BLOCKS)) {
            store.addBlock(new StudyBlock(f[0], f[1], f[2], emptyToNull(f[3]),
                    LocalDate.parse(f[4]), LocalTime.parse(f[5]), LocalTime.parse(f[6])));
        }
    }

    /**
     * Writes the whole store to disk, replacing the previous files.
     *
     * @param store data to persist
     */
    public void save(DataStore store) {
        try {
            Files.createDirectories(directory);
            List<String> lines = new ArrayList<>();
            for (Course c : store.getCourses()) {
                lines.add(join(c.getId(), c.getName(), c.getColor()));
            }
            write(COURSES, lines);

            lines.clear();
            for (Event e : store.getEvents()) {
                lines.add(join(e.getId(), e.getTitle(), e.getDate().toString(),
                        e.isAllDay() ? "" : e.getStartTime().toString(),
                        e.isAllDay() ? "" : e.getEndTime().toString(),
                        Boolean.toString(e.isAllDay()), nullToEmpty(e.getCourseId())));
            }
            write(EVENTS, lines);

            lines.clear();
            for (Assignment a : store.getAssignments()) {
                lines.add(join(a.getId(), a.getTitle(), a.getDueDate().toString(),
                        Integer.toString(a.getEstimatedHours()), Integer.toString(a.getBlockLength()),
                        nullToEmpty(a.getCourseId()), Integer.toString(a.getCompletedHours()),
                        Boolean.toString(a.isComplete())));
            }
            write(ASSIGNMENTS, lines);

            lines.clear();
            for (StudyBlock b : store.getBlocks()) {
                lines.add(join(b.getId(), b.getAssignmentId(), b.getTitle(), nullToEmpty(b.getCourseId()),
                        b.getDate().toString(), b.getStartTime().toString(), b.getEndTime().toString()));
            }
            write(BLOCKS, lines);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not save data to " + directory, ex);
        }
    }

    // ---- file helpers ------------------------------------------------

    private List<String[]> readRecords(String file) {
        Path path = directory.resolve(file);
        List<String[]> records = new ArrayList<>();
        if (!Files.exists(path)) {
            return records;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    records.add(split(line));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read " + path, ex);
        }
        return records;
    }

    private void write(String file, List<String> lines) throws IOException {
        Files.write(directory.resolve(file), lines, StandardCharsets.UTF_8);
    }

    // ---- record format -----------------------------------------------

    /**
     * Joins fields with {@code |}, escaping {@code \}, {@code |} and line
     * breaks so that any title round-trips safely.
     */
    static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            String f = fields[i] == null ? "" : fields[i];
            for (char ch : f.toCharArray()) {
                switch (ch) {
                    case '\\': sb.append("\\\\"); break;
                    case '|': sb.append("\\p"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': break;
                    default: sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    /** Inverse of {@link #join(String...)}. */
    static String[] split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 1 < line.length()) {
                char n = line.charAt(++i);
                switch (n) {
                    case 'p': cur.append('|'); break;
                    case 'n': cur.append('\n'); break;
                    default: cur.append(n);
                }
            } else if (ch == '|') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
