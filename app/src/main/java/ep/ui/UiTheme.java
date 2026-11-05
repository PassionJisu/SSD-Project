package ep.ui;

import ep.model.CalendarItem;
import ep.model.Course;
import ep.model.ItemType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Optional;
import java.util.function.Function;

/**
 * Colors, fonts and small drawing helpers shared by the calendar views.
 * Fixed events are drawn as solid chips in the course color; study blocks
 * use a pale tint of the same color with a dashed border so the two kinds
 * are always distinguishable.
 */
public final class UiTheme {

    /** Window background. */
    public static final Color BACKGROUND = new Color(0xF6F7FB);
    /** Card / panel background. */
    public static final Color SURFACE = Color.WHITE;
    /** Thin grid and border lines. */
    public static final Color LINE = new Color(0xE3E6EF);
    /** Primary text. */
    public static final Color TEXT = new Color(0x1F2430);
    /** Secondary text. */
    public static final Color MUTED = new Color(0x7A8294);
    /** Accent used for buttons and today's marker. */
    public static final Color ACCENT = new Color(0x4F6BED);
    /** Light accent fill for today's cell. */
    public static final Color TODAY_FILL = new Color(0xEEF1FF);
    /** Fill for days outside the shown month. */
    public static final Color OUTSIDE_FILL = new Color(0xFAFAFC);
    /** Color used for items without a course. */
    public static final Color UNLINKED = new Color(0x8A94A6);

    /** Base font. */
    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 13);
    /** Bold base font. */
    public static final Font BOLD = BASE.deriveFont(Font.BOLD);
    /** Small font for chips. */
    public static final Font SMALL = BASE.deriveFont(11.5f);
    /** Small bold font for chips. */
    public static final Font SMALL_BOLD = SMALL.deriveFont(Font.BOLD);
    /** Heading font. */
    public static final Font TITLE = BASE.deriveFont(Font.BOLD, 18f);

    /** Dashed stroke used for study-block borders. */
    public static final Stroke DASHED = new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
            10f, new float[]{4f, 3f}, 0f);

    /** Palette offered when creating a course. */
    public static final String[] PALETTE = {
            "#4F6BED", "#7C5CE6", "#2BA47A", "#E8903A", "#E05270", "#1FA4C9", "#C9A227", "#6B7280"
    };

    private UiTheme() {
    }

    /**
     * @param hex color text such as {@code #4F6BED}
     * @return the color, or {@link #UNLINKED} when the text is invalid
     */
    public static Color parse(String hex) {
        try {
            return Color.decode(hex);
        } catch (RuntimeException ex) {
            return UNLINKED;
        }
    }

    /**
     * @param c a color
     * @return the same color in {@code #RRGGBB} form
     */
    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Mixes a color with white.
     *
     * @param c     base color
     * @param ratio 0 keeps the color, 1 gives white
     * @return the tinted color
     */
    public static Color tint(Color c, double ratio) {
        int r = (int) Math.round(c.getRed() + (255 - c.getRed()) * ratio);
        int g = (int) Math.round(c.getGreen() + (255 - c.getGreen()) * ratio);
        int b = (int) Math.round(c.getBlue() + (255 - c.getBlue()) * ratio);
        return new Color(r, g, b);
    }

    /**
     * Mixes a color with black.
     *
     * @param c     base color
     * @param ratio 0 keeps the color, 1 gives black
     * @return the darker color
     */
    public static Color shade(Color c, double ratio) {
        int r = (int) Math.round(c.getRed() * (1 - ratio));
        int g = (int) Math.round(c.getGreen() * (1 - ratio));
        int b = (int) Math.round(c.getBlue() * (1 - ratio));
        return new Color(r, g, b);
    }

    /**
     * Resolves the base color of a calendar item from its course.
     *
     * @param item         the item
     * @param courseLookup function from course id to course
     * @return the course color or {@link #UNLINKED}
     */
    public static Color colorOf(CalendarItem item, Function<String, Optional<Course>> courseLookup) {
        if (item.getCourseId() == null) {
            return UNLINKED;
        }
        return courseLookup.apply(item.getCourseId()).map(c -> parse(c.getColor())).orElse(UNLINKED);
    }

    /**
     * Draws one calendar chip (rounded rectangle plus text). Fixed events are
     * solid; study blocks are pale with a dashed outline.
     *
     * @param g      graphics
     * @param x      left
     * @param y      top
     * @param w      width
     * @param h      height
     * @param base   course color
     * @param type   item type
     * @param text   first line of text
     * @param sub    optional second line (may be {@code null})
     */
    public static void drawChip(Graphics2D g, int x, int y, int w, int h, Color base, ItemType type,
                                String text, String sub) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int arc = 8;
        if (type == ItemType.EVENT) {
            g.setColor(base);
            g.fillRoundRect(x, y, w, h, arc, arc);
            g.setColor(Color.WHITE);
        } else {
            g.setColor(tint(base, 0.82));
            g.fillRoundRect(x, y, w, h, arc, arc);
            Stroke old = g.getStroke();
            g.setStroke(DASHED);
            g.setColor(shade(base, 0.1));
            g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            g.setStroke(old);
            g.setColor(shade(base, 0.45));
        }
        java.awt.Shape clip = g.getClip();
        g.clipRect(x + 2, y, w - 4, h);
        g.setFont(SMALL_BOLD);
        int lineH = g.getFontMetrics().getHeight();
        int ty = y + g.getFontMetrics().getAscent() + 3;
        g.drawString(clipText(g, text, w - 10), x + 6, ty);
        if (sub != null && h >= lineH * 2 + 4) {
            g.setFont(SMALL);
            g.drawString(clipText(g, sub, w - 10), x + 6, ty + lineH);
        }
        g.setClip(clip);
    }

    /**
     * Shortens text with an ellipsis so that it fits in the given width.
     *
     * @param g     graphics (its current font is used)
     * @param text  the text
     * @param width available width in pixels
     * @return the text, possibly shortened
     */
    public static String clipText(Graphics2D g, String text, int width) {
        if (text == null) {
            return "";
        }
        java.awt.FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String ell = "\u2026";
        int ellW = fm.stringWidth(ell);
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (fm.stringWidth(sb.toString() + ch) + ellW > width) {
                break;
            }
            sb.append(ch);
        }
        return sb.append(ell).toString();
    }
}
