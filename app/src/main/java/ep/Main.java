package ep;

import ep.api.EnoughProcrastination;
import ep.ui.AssignmentDialog;
import ep.ui.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Program entry point.
 *
 * <pre>
 * java -jar enough-procrastination.jar [--data DIR] [--demo] [--screenshot DIR]
 * </pre>
 * <ul>
 *   <li>{@code --data DIR}: folder for the local data files
 *       (default {@code ~/.enough-procrastination}).</li>
 *   <li>{@code --demo}: start with sample data in a temporary folder (unless
 *       {@code --data} is given) so the beta can be tried at once.</li>
 *   <li>{@code --screenshot DIR}: write PNG screenshots of the month view,
 *       the week view and the assignment dialog into DIR, then exit.</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    /**
     * @param args command line options, see class comment
     * @throws Exception if the window cannot be created
     */
    public static void main(String[] args) throws Exception {
        Path data = null;
        boolean demo = false;
        Path shots = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--data": data = Paths.get(args[++i]); break;
                case "--demo": demo = true; break;
                case "--screenshot": shots = Paths.get(args[++i]); break;
                default: System.err.println("Unknown option: " + args[i]);
            }
        }
        if (data == null) {
            data = demo ? Files.createTempDirectory("ep-demo")
                    : Paths.get(System.getProperty("user.home"), ".enough-procrastination");
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // default look and feel is fine
        }
        UIManager.put("Button.font", ep.ui.UiTheme.BASE);
        UIManager.put("Label.font", ep.ui.UiTheme.BASE);
        UIManager.put("TextField.font", ep.ui.UiTheme.BASE);
        UIManager.put("ComboBox.font", ep.ui.UiTheme.BASE);
        UIManager.put("List.font", ep.ui.UiTheme.BASE);
        UIManager.put("Spinner.font", ep.ui.UiTheme.BASE);
        UIManager.put("ToggleButton.font", ep.ui.UiTheme.BASE);
        UIManager.put("CheckBox.font", ep.ui.UiTheme.BASE);
        UIManager.put("OptionPane.messageFont", ep.ui.UiTheme.BASE);
        UIManager.put("OptionPane.buttonFont", ep.ui.UiTheme.BASE);

        EnoughProcrastination app = new EnoughProcrastination(data);
        if (demo) {
            DemoData.seed(app);
        }
        final Path shotDir = shots;
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(app);
            frame.setVisible(true);
            if (shotDir != null) {
                new Thread(() -> captureAndExit(frame, app, shotDir)).start();
            }
        });
    }

    private static void captureAndExit(MainFrame frame, EnoughProcrastination app, Path dir) {
        try {
            Files.createDirectories(dir);
            Thread.sleep(1500);
            save(frame, null, dir.resolve("beta-month-view.png"));
            SwingUtilities.invokeAndWait(() -> frame.setWeekMode(true));
            Thread.sleep(800);
            save(frame, null, dir.resolve("beta-week-view.png"));
            SwingUtilities.invokeAndWait(() -> frame.setWeekMode(false));
            Thread.sleep(400);
            final AssignmentDialog[] holder = new AssignmentDialog[1];
            SwingUtilities.invokeAndWait(() -> {
                AssignmentDialog d = new AssignmentDialog(frame, app.assignments(), app.courses(), null,
                        app.scheduler().today());
                d.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
                d.setVisible(true);
                holder[0] = d;
            });
            Thread.sleep(800);
            save(frame, holder[0], dir.resolve("beta-assignment-dialog.png"));
            SwingUtilities.invokeAndWait(() -> holder[0].dispose());
            final ep.ui.ProgressDialog[] progress = new ep.ui.ProgressDialog[1];
            SwingUtilities.invokeAndWait(() -> {
                app.assignments().getAssignments().stream().filter(a -> !a.isComplete()).findFirst().ifPresent(a -> {
                    ep.ui.ProgressDialog d = new ep.ui.ProgressDialog(frame, app.progress(), a);
                    d.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
                    d.setVisible(true);
                    progress[0] = d;
                });
            });
            Thread.sleep(800);
            if (progress[0] != null) {
                save(frame, progress[0], dir.resolve("beta-progress-dialog.png"));
                SwingUtilities.invokeAndWait(() -> progress[0].dispose());
            }
            System.out.println("Screenshots written to " + dir.toAbsolutePath());
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Renders the window (and any dialog that is currently open over it) into
     * a PNG without touching the screen, so nothing but the application
     * itself ends up in the image.
     */
    private static void save(MainFrame frame, Window dialog, Path file) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                int w = frame.getWidth();
                int h = frame.getHeight();
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(ep.ui.UiTheme.BACKGROUND);
                g.fillRect(0, 0, w, h);
                frame.printAll(g);
                if (dialog != null) {
                    int dx = dialog.getX() - frame.getX();
                    int dy = dialog.getY() - frame.getY();
                    g.setColor(new Color(0, 0, 0, 70));
                    g.fillRect(0, 0, w, h);
                    g.setColor(new Color(0, 0, 0, 40));
                    g.fillRoundRect(dx + 4, dy + 6, dialog.getWidth(), dialog.getHeight(), 12, 12);
                    Graphics2D dg = (Graphics2D) g.create(dx, dy, dialog.getWidth(), dialog.getHeight());
                    dialog.printAll(dg);
                    dg.dispose();
                }
                g.dispose();
                ImageIO.write(img, "png", file.toFile());
            } catch (IOException ex) {
                throw new java.io.UncheckedIOException(ex);
            }
        });
    }
}
