package ep.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Small factory for the input widgets used by the dialogs: date and time
 * spinners, number spinners and a simple two-column form layout.
 */
public final class Fields {

    private Fields() {
    }

    /**
     * @param initial starting value
     * @return a spinner that edits a date as {@code yyyy-MM-dd}
     */
    public static JSpinner dateSpinner(LocalDate initial) {
        JSpinner s = new JSpinner(new SpinnerDateModel(toDate(initial), null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        return s;
    }

    /**
     * @param initial starting value
     * @return a spinner that edits a time as {@code HH:mm}
     */
    public static JSpinner timeSpinner(LocalTime initial) {
        JSpinner s = new JSpinner(new SpinnerDateModel(toDate(LocalDate.of(2000, 1, 1), initial), null, null,
                Calendar.MINUTE));
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm"));
        return s;
    }

    /**
     * @param value starting value
     * @param min   minimum
     * @param max   maximum
     * @return an integer spinner
     */
    public static JSpinner intSpinner(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    /**
     * @param s a spinner made by {@link #dateSpinner(LocalDate)}
     * @return the selected date
     */
    public static LocalDate dateOf(JSpinner s) {
        Date d = (Date) s.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * @param s a spinner made by {@link #timeSpinner(LocalTime)}
     * @return the selected time (seconds dropped)
     */
    public static LocalTime timeOf(JSpinner s) {
        Date d = (Date) s.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }

    /**
     * @param s an integer spinner
     * @return its value
     */
    public static int intOf(JSpinner s) {
        return ((Number) s.getValue()).intValue();
    }

    /** @return a panel with a {@link GridBagLayout} ready for {@link #addRow} */
    public static JPanel form() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    /**
     * Adds a label/field pair to a form.
     *
     * @param form  panel from {@link #form()}
     * @param row   zero-based row
     * @param label text of the label
     * @param field the input component
     */
    public static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(5, 6, 5, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JLabel l = new JLabel(label);
        l.setForeground(UiTheme.MUTED);
        form.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    /**
     * Adds a component that spans both columns.
     *
     * @param form panel from {@link #form()}
     * @param row  zero-based row
     * @param comp component to add
     */
    public static void addWide(JPanel form, int row, JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(comp, c);
    }

    private static Date toDate(LocalDate d) {
        return toDate(d, LocalTime.NOON);
    }

    private static Date toDate(LocalDate d, LocalTime t) {
        return Date.from(d.atTime(t).atZone(ZoneId.systemDefault()).toInstant());
    }
}
