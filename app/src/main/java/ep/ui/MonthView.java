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
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Six-week month grid. Each cell lists the day's all-day events first, then
 * timed events and study blocks in time order. When a cell overflows, a
 * "+N more" line is shown.
 */
public class MonthView extends CalendarView {

    private static final int HEADER_H = 26;
    private static final int CHIP_H = 18;
    private static final int CHIP_GAP = 3;

    /** Creates the month view. */
    public MonthView() {
        setPreferredSize(new Dimension(900, 600));
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
        int cols = 7;
        int rows = 6;
        double cellW = (double) w / cols;
        double cellH = (double) (h - HEADER_H) / rows;

        YearMonth month = YearMonth.from(anchor);
        LocalDate first = CalendarManager.firstGridDay(month);

        // weekday header
        g.setFont(UiTheme.SMALL_BOLD);
        for (int c = 0; c < cols; c++) {
            DayOfWeek dow = DayOfWeek.MONDAY.plus(c);
            String name = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
            g.setColor(dow == DayOfWeek.SUNDAY || dow == DayOfWeek.SATURDAY ? UiTheme.MUTED : UiTheme.TEXT);
            int x = (int) (c * cellW);
            int tx = x + ((int) cellW - g.getFontMetrics().stringWidth(name)) / 2;
            g.drawString(name, tx, 17);
        }
        g.setColor(UiTheme.LINE);
        g.drawLine(0, HEADER_H - 1, w, HEADER_H - 1);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                LocalDate day = first.plusDays(r * 7L + c);
                int x = (int) Math.round(c * cellW);
                int y = HEADER_H + (int) Math.round(r * cellH);
                int cw = (int) Math.round((c + 1) * cellW) - x;
                int ch = HEADER_H + (int) Math.round((r + 1) * cellH) - y;
                paintCell(g, day, month, x, y, cw, ch);
            }
        }
    }

    private void paintCell(Graphics2D g, LocalDate day, YearMonth month, int x, int y, int w, int h) {
        boolean inMonth = YearMonth.from(day).equals(month);
        boolean isToday = day.equals(today);
        g.setColor(isToday ? UiTheme.TODAY_FILL : inMonth ? UiTheme.SURFACE : UiTheme.OUTSIDE_FILL);
        g.fillRect(x, y, w, h);
        g.setColor(UiTheme.LINE);
        g.drawRect(x, y, w, h);
        hits.add(new Hit(new Rectangle(x, y, w, h), null, day));

        // day number
        g.setFont(UiTheme.SMALL_BOLD);
        String num = Integer.toString(day.getDayOfMonth());
        if (isToday) {
            g.setColor(UiTheme.ACCENT);
            g.fillOval(x + 5, y + 4, 20, 20);
            g.setColor(Color.WHITE);
            g.drawString(num, x + 5 + (20 - g.getFontMetrics().stringWidth(num)) / 2, y + 18);
        } else {
            g.setColor(inMonth ? UiTheme.TEXT : UiTheme.MUTED);
            g.drawString(num, x + 9, y + 18);
        }

        List<CalendarItem> list = itemsOn(day);
        int cy = y + 28;
        int available = (y + h - 4 - cy) / (CHIP_H + CHIP_GAP);
        int shown = Math.min(list.size(), available);
        if (shown < list.size() && available > 0) {
            shown = Math.max(0, available - 1);
        }
        for (int i = 0; i < shown; i++) {
            CalendarItem item = list.get(i);
            Color base = UiTheme.colorOf(item, courses);
            String text = item.isAllDay()
                    ? item.getTitle()
                    : item.getStartTime().toString() + " " + item.getTitle();
            int cx = x + 4;
            int cw = w - 8;
            if (item.isAllDay()) {
                // all-day events are drawn edge to edge to stand apart
                cx = x + 1;
                cw = w - 2;
            }
            UiTheme.drawChip(g, cx, cy, cw, CHIP_H, base, item.getType(), text, null);
            hits.add(new Hit(new Rectangle(cx, cy, cw, CHIP_H), item, day));
            cy += CHIP_H + CHIP_GAP;
        }
        if (shown < list.size()) {
            g.setFont(UiTheme.SMALL);
            g.setColor(UiTheme.MUTED);
            g.drawString("+" + (list.size() - shown) + " more", x + 8, cy + 13);
        }
    }
}
