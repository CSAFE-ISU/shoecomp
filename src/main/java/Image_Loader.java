import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Image_Loader implements PlugIn {
    boolean markup_begin;
    private final Image_LoaderGUI gui;
    private JFileChooser chooser;
    private boolean img_valid;
    private boolean markup_valid;

    public Image_Loader() {
        this.gui = new Image_LoaderGUI();
        this.chooser = new JFileChooser();
        this.loadReactions();

        markup_begin = false;
    }

    public static void callFromMacro() {
        Image_Loader x = new Image_Loader();
        x.run("");
    }

    private void loadReactions() {
        Preferences prefs = Preferences.userNodeForPackage(Image_Loader.class);
        gui.getMarkupLoadButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String prev = prefs.get("PreviousJSONLoad", System.getProperty("user.home"));
                chooser = new JFileChooser(prev);
                String validPath = checkFileLoad("json", "txt");
                if (validPath == null || validPath.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Invalid File!");
                    markup_valid = false;
                } else {
                    gui.getMarkupPath().setText(validPath);
                    markup_valid = true;
                    String selected = new File(validPath).getParent();
                    prefs.put("PreviousJSONLoad", selected);
                }
            }
        });

        gui.getImgLoadButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String prev = prefs.get("PreviousImageLoad", System.getProperty("user.home"));
                chooser = new JFileChooser(prev);
                String validPath = checkFileLoad("tiff", "jpg", "png", "tif", "jpeg");
                if (validPath == null || validPath.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Invalid File!");
                    img_valid = false;
                } else {
                    gui.getImgPath().setText(validPath);
                    img_valid = true;
                    String selected = new File(validPath).getParent();
                    prefs.put("PreviousImageLoad", selected);
                }
            }
        });

    }

    String checkFileLoad(String... fileTypes) {
        chooser.setFileFilter(new FileNameExtensionFilter("Load Image Data", fileTypes));
        int returnValue = chooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            return file.getAbsolutePath();
        }
        return "";
    }

    public void run(String arg) {
        int p = JOptionPane.showConfirmDialog(null, gui.getPanel(),
                "Load Image and Markup", JOptionPane.OK_CANCEL_OPTION);
        if (p == JOptionPane.CANCEL_OPTION || p == JOptionPane.CLOSED_OPTION) return;
        if (!img_valid) return;

        ImagePlus tmp = IJ.openImage(gui.getImgPath().getText());
        ImageProcessor raw = tmp.getProcessor();

        ImageStack overlay = new ImageStack();
        ImageProcessor marked = raw.duplicate();
        ImageProcessor mask = raw.createProcessor(raw.getWidth(), raw.getHeight());

        overlay.addSlice(marked);
        overlay.addSlice(mask);
        overlay.addSlice(raw);
        gui.setImg(new ImagePlus(tmp.getShortTitle(), overlay));
        gui.getImg().show();

        boolean mark_bounds;
        PolygonRoi pol = null;
        PointRoi pts;

        if (markup_valid) {
            try {
                MarkupData m = MarkupData.fromFile(gui.getMarkupPath().getText());
                if (m.nbounds != 0) {
                    pol = m.getBoundsAsRoi();
                    mark_bounds = true;
                } else {
                    this.showBoundsHelper(gui.getImg());
                    mark_bounds = false;
                }
                if (m.npoints != 0) {
                    pts = m.getPointsAsRoi();
                } else {
                    pts = new PointRoi();
                }
            } catch (Exception e) {
                // might need to show an error here
                System.out.println("unable to read JSON!");
                e.printStackTrace();
                gui.getImg().close();
                return;
            }
        } else {
            // if we didn't load a JSON,
            // we still need to mark bounds
            mark_bounds = false;
            pts = new PointRoi();
        }

        if (!mark_bounds) {
            pol = this.showBoundsHelper(gui.getImg());
        }
        if (pol == null || pts == null) {
            return;
        }

        marked.setColor(Color.BLACK);
        marked.fillOutside(pol);

        mask.setColor(Color.WHITE);
        mask.fill(pol);

        tmp.close();
        gui.getImg().updateAndDraw();

        pts.setSize(3);
        pts.setFillColor(Color.RED);
        pts.setStrokeColor(Color.RED);
        gui.getImg().setProperty("points", pts);
        gui.getImg().setProperty("bounds", pol);
        gui.getImg().setProperty("name", gui.getImg().getShortTitle());
        gui.getImg().setTitle(gui.getImg().getProperty("name") + ": " +  pts.size() + " Points Marked");
        /* TODO: make behavior consistent
            when marking anew, the PointRoi does not show on all layers  */
        ij.IJ.setTool("Multi-Point");
        gui.getImg().setRoi(pts);
        ImageCanvas canv = gui.getImg().getWindow().getCanvas();
        canv.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (gui.getImg().getRoi() == null) {
                    reconfigurePointRoi();
                }
                int numPoints = (gui.getImg().getRoi() != null) ? gui.getImg().getRoi().getContainedPoints().length : 0;
                gui.getImg().setTitle(gui.getImg().getProperty("name") + ": " +  numPoints + " Points Marked");
            }
        });
    }

    void reconfigurePointRoi() {
        PointRoi pts = new PointRoi();
        pts.setSize(3);
        pts.setFillColor(Color.RED);
        pts.setStrokeColor(Color.RED);
        gui.getImg().setProperty("points", pts);
        ij.IJ.setTool("Multi-Point");
        gui.getImg().setRoi(pts);

    }

    PolygonRoi showBoundsHelper(ImagePlus t) {
        final Object obj = new Object();
        ij.IJ.setTool(Toolbar.POLYGON);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                JDialog d = new JDialog();
                JPanel subpanel = new JPanel(new GridLayout(1, 1));
                JTextArea b = new JTextArea();
                b.setText("Close this window after marking\nthe boundary of the shoeprint");
                b.setFont(b.getFont().deriveFont(28f));
                b.setEditable(false);
                subpanel.add(b);
                d.setContentPane(subpanel);
                d.setTitle("Mark boundary of shoeprint");
                d.pack();
                d.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        synchronized (obj) { // can lock when worker thread releases with wait
                            obj.notify(); // signals wait
                        }
                        super.windowClosed(e);
                    }
                });
                d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                d.setVisible(true);
            }
        };
        try {
            synchronized (obj) {
                SwingUtilities.invokeLater(r);
                obj.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        PolygonRoi pol = (PolygonRoi) t.getRoi();
        return pol;
    }
}
