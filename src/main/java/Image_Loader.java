import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

class ModPanel extends Panel {

  public GridBagLayout lyt;
  public GridBagConstraints gbc;

  public Label marked;
  public Button flipHButton;
  public Button flipVButton;
  public Button clearPoints;
  public Button markPolygon;


  public ModPanel() {
    super();
    this.lyt = new GridBagLayout();
    this.gbc = new GridBagConstraints();
    this.setLayout(this.lyt);
    clearPoints = new Button("Clear Points");
    marked = new Label("0 Points Marked");
    flipHButton = new Button("Flip Horizontal");
    flipVButton = new Button("Flip Vertical");
    markPolygon = new Button("Click after marking shoeprint boundary");
    this.setUI1();
  }

  public void setUI1() {
    gbc.fill = GridBagConstraints.BOTH;
    markPolygon.setFont(new Font("anno", Font.BOLD, 18));
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    gbc.gridheight = 2;
    gbc.ipady = 20;
    lyt.setConstraints(markPolygon, gbc);
    this.add(markPolygon);

    flipVButton.setEnabled(false);
    flipHButton.setEnabled(false);
    clearPoints.setEnabled(false);
    markPolygon.setEnabled(true);
  }

  public void setUI2() {
    this.remove(markPolygon);
    markPolygon.setVisible(false);
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.ipadx = 2;
    gbc.ipady = 2;

    Font fnt = new Font("loader", Font.PLAIN, 18);
    marked.setFont(fnt);
    gbc.gridx = 0;
    gbc.gridy = 0;
    lyt.setConstraints(marked, gbc);
    this.add(marked);

    clearPoints.setFont(fnt);
    gbc.gridx = 1;
    gbc.gridy = 0;
    lyt.setConstraints(clearPoints, gbc);
    this.add(clearPoints);

    flipHButton.setFont(fnt);
    gbc.gridx = 0;
    gbc.gridy = 1;
    lyt.setConstraints(flipHButton, gbc);
    this.add(flipHButton);

    flipVButton.setFont(fnt);
    gbc.gridx = 1;
    gbc.gridy = 1;
    lyt.setConstraints(flipVButton, gbc);
    this.add(flipVButton);

    flipVButton.setEnabled(true);
    flipHButton.setEnabled(true);
    clearPoints.setEnabled(true);
  }
}

public class Image_Loader implements PlugIn {
  private final Image_LoaderGUI gui;
  boolean markup_begin;
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
    gui.getMarkupLoadButton()
        .addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                String prev = prefs.get("PreviousJSONLoad", System.getProperty("user.home"));
                chooser = new JFileChooser(prev);
                String validPath = checkFileLoad("json", "txt");
                if (validPath == null || validPath.isEmpty()) {
                  markup_valid = false;
                } else {
                  gui.getMarkupPath().setText(validPath);
                  markup_valid = true;
                  String selected = new File(validPath).getParent();
                  prefs.put("PreviousJSONLoad", selected);
                }
              }
            });

    gui.getImgLoadButton()
        .addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                String prev = prefs.get("PreviousImageLoad", System.getProperty("user.home"));
                chooser = new JFileChooser(prev);
                String validPath = checkFileLoad("tiff", "jpg", "png", "tif", "jpeg");
                if (validPath == null || validPath.isEmpty()) {
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
    int p =
        JOptionPane.showConfirmDialog(
            null, gui.getPanel(), "Load Image and Markup", JOptionPane.OK_CANCEL_OPTION);
    if (p == JOptionPane.CANCEL_OPTION) return;
    if (!img_valid) {
      JOptionPane.showMessageDialog(null, "Unable to load image file!");
      return;
    }

    ImagePlus tmp = IJ.openImage(gui.getImgPath().getText());
    ImageProcessor raw = tmp.getProcessor();

    ImageStack overlay = new ImageStack();
    ImageProcessor marked = raw.convertToColorProcessor();
    ImageProcessor mask = raw.createProcessor(raw.getWidth(), raw.getHeight());

    overlay.addSlice(marked);
    overlay.addSlice(mask);
    overlay.addSlice(raw);
    gui.setImg(new ImagePlus(tmp.getShortTitle(), marked));

    ImagePlus img = gui.getImg();
    img.show();
    img.setProperty("stack", overlay);
    ImageWindow win = img.getWindow();
    ImageCanvas canv = win.getCanvas();

    boolean mark_bounds;
    PolygonRoi pol = null;
    PointRoi pts, old_pts = null;

    ModPanel modpanel = new ModPanel();
    win.add(modpanel, 1);
    canv.fitToWindow();
    win.pack();


    if (markup_valid) {
      try {
        MarkupData m = MarkupData.fromFile(gui.getMarkupPath().getText());
        if (m.nbounds != 0) {
          pol = m.getBoundsAsRoi();
          mark_bounds = true;
        } else {
          pol = this.showBoundsHelper(gui.getImg(), modpanel);
          mark_bounds = true;
        }
        if (m.npoints != 0) {
          old_pts = m.getPointsAsRoi();
          old_pts.promptBeforeDeleting(false);
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
    }

    if (!mark_bounds) {
      pol = this.showBoundsHelper(gui.getImg(), modpanel);
    }
    if (pol == null) {
      tmp.close();
      img.close();
      return;
    }

    pts = new PointRoi();
    pts.setSize(3);
    pts.setPointType(PointRoi.DOT);
    pts.setFillColor(new Color(0xf8, 0x5d, 0x19, 0xff));
    pts.setStrokeColor(new Color(0xf8, 0x5d, 0x19, 0xff));
    pts.setImage(img);
    img.setRoi(pts);
    pts.promptBeforeDeleting(false);

    if (old_pts != null) {
      for (Point z : old_pts.getContainedPoints()) {
        pts.addUserPoint(gui.getImg(), z.x, z.y);
      }
    }

    ImageProcessor sg = new ByteProcessor(marked.getWidth(), marked.getHeight());
    sg.setColor(Color.WHITE);
    sg.fill(pol);
    sg.setColorModel(CustomColorModelFactory.getModel(0x90, 0x90, 0x90, 0xD0, 1));
    marked.drawOverlay(new Overlay(new ImageRoi(0,0,sg)));

    mask.setColor(Color.WHITE);
    mask.fill(pol);

    tmp.close();
    img.updateAndDraw();
    img.setProperty("points", pts);
    img.setProperty("bounds", pol);
    img.setProperty("name", gui.getImg().getShortTitle());
    /* TODO: make behavior consistent
    when marking anew, the PointRoi does not show on all layers  */
    ij.IJ.setTool("Multi-Point");
    img.setRoi(pts);

    modpanel.marked.setText(pts.getContainedPoints().length + " Points Marked");
    setPanelReactions(modpanel);
    modpanel.validate();
    img.repaintWindow();

    canv.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            // super.mousePressed(e);
            if (gui.getImg().getRoi() == null) {
              reconfigurePointRoi();
            }
            int numPoints =
                (gui.getImg().getRoi() != null)
                    ? gui.getImg().getRoi().getContainedPoints().length
                    : 0;
            modpanel.marked.setText(numPoints + " Points Marked");
            // img.repaintWindow();
          }
        });
  }

  private void setPanelReactions(ModPanel p) {
    p.setUI2();
    p.flipHButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            ImagePlus ipz = gui.getImg();
            if (ipz == null) return;
            if (ipz.getProperty("stack") != null) {
              ImageStack stk = (ImageStack) ipz.getProperty("stack");
              int N = stk.getSize();
              for (int i = 1; i <= N; ++i) {
                stk.getProcessor(i).flipHorizontal();
              }
            } else {
              ipz.getProcessor().flipHorizontal();
            }
            ipz.repaintWindow();
          }
        });

    p.flipVButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            ImagePlus ipz = gui.getImg();
            if (ipz == null) return;
            if (ipz.getProperty("stack") != null) {
              ImageStack stk = (ImageStack) ipz.getProperty("stack");
              int N = stk.getSize();
              for (int i = 1; i <= N; ++i) {
                stk.getProcessor(i).flipVertical();
              }
            } else {
              ipz.getProcessor().flipVertical();
            }
            ipz.repaintWindow();
          }
        });

    p.clearPoints.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            reconfigurePointRoi();
            p.marked.setText(0 + " Points Marked");
          }
        });
  }

  void reconfigurePointRoi() {
    PointRoi pts = new PointRoi();
    pts.setSize(3);
    pts.setPointType(PointRoi.DOT);
    pts.setFillColor(new Color(0xf8, 0x5d, 0x19, 0xff));
    pts.setStrokeColor(new Color(0xf8, 0x5d, 0x19, 0xff));
    pts.promptBeforeDeleting(false);
    gui.getImg().setProperty("points", pts);
    ij.IJ.setTool("Multi-Point");
    gui.getImg().setRoi(pts, true);
  }

  PolygonRoi showBoundsHelper(ImagePlus imp, ModPanel modpanel) {
    ImageWindow win = imp.getWindow();
    final Object obj = new Object();
    ij.IJ.setTool(Toolbar.POLYGON);


    Runnable r =
        new Runnable() {
          @Override
          public void run() {
            modpanel.markPolygon.addActionListener(
                new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent actionEvent) {
                    synchronized (obj) {
                      obj.notify();
                    }
                  }
                });
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

    PolygonRoi pol = (PolygonRoi) imp.getRoi();
    return pol;
  }
}
