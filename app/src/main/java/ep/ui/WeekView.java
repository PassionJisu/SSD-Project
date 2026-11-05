package ep.ui;

import ep.model.CalendarItem;
import ep.service.CalendarManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Seven-day week grid with an hour axis. All-day events sit in a separate
 * band under the day header; timed events and study blocks are placed by
 * their start and end times.
 */
public class WeekView extends CalendarView {

    private static final int TIME_COL = 52;
    private static final int HEADER_H = 44;
    private static final int ALLDAY_ROW_H = 24;
    private static final int FIRST_HOUR = 7;
    private static final int LAST_HOUR = 23;
    private static final int MIN_HOUR_H = 44;

    /** Creates the week view. */
    public WeekView() {
        setPreferredSize(new Dimension(900, HEADER_H + ALLDAY_ROW_H * 2 + (LAST_HOUR - FIRST_HOUR) * MIN_HOUR_H));
    }

    @Override
    public Dimension getPreferredSize() {
        int allDayRows = Math.max(1, maxAllDayRows());
        return new Dimension(900, HEADER_H + ALLDAY_ROW_H * allDayRows + 8 + (LAST_HOUR - FIRST_HOUR) * MIN_HOUR_H);
    }

    private int maxAllDayRows() {
        LocalDate monday = CalendarManager.startOfWeek(anchor);
        int max = 0;
        for (int i = 0; i < 7; i++) {
            int n = 0;
            for (CalendarItem it : itemsOn(monday.plusDays(i))) {
                if (it.isAllDay()) {
                    n++;
                }
            }
            max = Math.max(max, n);
        }
        return max;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hits.clear();

        int w = getWidth();
        int h = getHeight();
        LocalDate monday = CalendarManager.startOfWeek(anchor);
        int allDayRows = Math.max(1, maxAllDayRows());
        int bandH = ALLDAY_ROW_H * allDayRows + 8;
        int gridTop = HEADER_H + bandH;
        double colW = (double) (w - TIME_COL) / 7;
        double hourH = (double) (h - gridTop) / (LAST_HOUR - FIRST_HOUR);

        // header
        for (int c = 0; c < 7; c++) {
            LocalDate day = monday.plusDays(c);
            int x = TIME_COL + (int) Math.round(c * colW);
            int cw = TIME_COL + (int) Math.round((c + 1) * colW) - x;
            boolean isToday = day.equals(today);
            if (isToday) {
                g.setColor(UiTheme.TODAY_FILL);
                g.fillRect(x, 0, cw, h);
            }
            DayOfWeek dow = day.getDayOfWeek();
            String name = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
            g.setFont(UiTheme.SMALL);
            g.setColor(UiTheme.MUTED);
            g.drawString(name, x + (cw - g.getFontMetrics().stringWidth(name)) / 2, 16);
            g.setFont(UiTheme.TITLE.deriveFont(16f));
            String num = Integer.toString(day.getDayOfMonth());
            int nw = g.getFontMetrics().stringWidth(num);
            if (isToday) {
                g.setColor(UiTheme.ACCENT);
                g.fillOval(x + cw / 2 - 13, 19, 26, 26);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(UiTheme.TEXT);
            }
            g.drawString(num, x + (cw - nw) / 2, 38);
        }
        g.setColor(UiTheme.LINE);
        g.drawLine(0, HEADER_H, w, HEADER_H);

        // all-day band
        g.setFont(UiTheme.SMALL);
        g.setColor(UiTheme.MUTED);
        g.drawString("all-day", 6, HEADER_H + 16);
        for (int c = 0; c < 7; c++) {
            LocalDate day = monday.plusDays(c);
            int x = TIME_COL + (int) Math.round(c * colW);
            int cw = TIME_COL + (int) Math.round((c + 1) * colW) - x;
            int row = 0;
            for (CalendarItem it : itemsOn(day)) {
                if (!it.isAllDay()) {
                    continue;
                }
                int y = HEADER_H + 4 + row * ALLDAY_ROW_H;
                Color base = UiTheme.colorOf(it, courses);
                UiTheme.drawChip(g, x + 2, y, cw - 4, ALLDAY_ROW_H - 4, base, it.getType(), it.getTitle(), null);
                hits.add(new Hit(new Rectangle(x + 2, y, cw - 4, ALLDAY_ROW_H - 4), it, day));
                row++;
            }
        }
        g.setColor(UiTheme.LINE);
        g.drawLine(0, gridTop, w, gridTop);

        // hour grid
        for (int hr = FIRST_HOUR; hr <= LAST_HOUR; hr++) {
            int y = gridTop + (int) Math.round((hr - FIRST_HOUR) * hourH);
            g.setColor(UiTheme.LINE);
            g.drawLine(TIME_COL, y, w, y);
            g.setFont(UiTheme.SMALL);
            g.setColor(UiTheme.MUTED);
            String label = String.format("%02d:00", hr);
            g.drawString(label, TIME_COL - 6 - g.getFontMetrics().stringWidth(label), y + 4);
        }
        for (int c = 0; c <= 7; c++) {
            int x = TIME_COL + (int) Math.round(c * colW);
            g.setColor(UiTheme.LINE);
            g.drawLine(x, HEADER_H, x, h);
        }

        // timed items, with simple side-by-side layout for overlaps
        for (int c = 0; c < 7; c++) {
            LocalDate day = monday.plusDays(c);
            int x = TIME_COL + (int) Math.round(c * colW);
            int cw = TIME_COL + (int) Math.round((c + 1) * colW) - x;
            hits.add(new Hit(new Rectangle(x, gridTop, cw, h - gridTop), null, day));
            List<CalendarItem> timed = new ArrayList<>();
            for (CalendarItem it : itemsOn(day)) {
                if (!it.isAllDay()) {
                    timed.add(it);
                }
            }
            for (int i = 0; i < timed.size(); i++) {
                CalendarItem it = timed.get(i);
                int lane = 0;
                int lanes = 1;
                for (int j = 0; j < timed.size(); j++) {
                    if (j != i && timed.get(j).overlaps(it)) {
                        lanes = 2;
                        if (j < i) {
                            lane = 1;
                        }
                    }
                }
                int y1 = gridTop + (int) Math.round(minutesFromFirst(it.getStartTime()) / 60.0 * hourH);
                int y2 = gridTop + (int) Math.round(minutesFromFirst(it.getEndTime()) / 60.0 * hourH);
                int laneW = (cw - 6) / lanes;
                int ix = x + 3 + lane * laneW;
                int iy = y1 + 1;
                int ih = Math.max(16, y2 - y1 - 2);
                Color base = UiTheme.colorOf(it, courses);
                String sub = it.getStartTime() + " \u2013 " + it.getEndTime()
                        + (it.getType() == ep.model.ItemType.STUDY_BLOCK ? " \u00b7 study" : "");
                UiTheme.drawChip(g, ix, iy, laneW - 2, ih, base, it.getType(), it.getTitle(), sub);
                hits.add(new Hit(new Rectangle(ix, iy, laneW - 2, ih), it, day));
            }
        }
    }

    private static int minutesFromFirst(LocalTime t) {
        int m = t.getHour() * 60 + t.getMinute() - FIRST_HOUR * 60;
        return Math.max(0, Math.min((LAST_HOUR - FIRST_HOUR) * 60, m));
    }
}
