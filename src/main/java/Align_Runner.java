import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.*;
import ij.io.FileInfo;
import ij.io.TiffEncoder;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.github.ahgamut.clqmtch.Graph;
import io.github.ahgamut.clqmtch.HeuristicSearch;
import io.github.ahgamut.clqmtch.StackDFS;
import java.awt.*;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import mpicbg.ij.TransformMapping;
import mpicbg.models.*;
import org.json.JSONArray;
import org.json.JSONObject;

enum StatusProgress {
  CANCELED(0) {},
  STARTING(1) {
    @Override
    public String report() {
      return "Starting...";
    }
  },
  RESCALING(5) {
    @Override
    public String report() {
      return "Checking scales...";
    }
  },
  ALIGNING(10) {
    @Override
    public String report() {
      return "Aligning...";
    }
  },
  SIMILARITY(25) {
    @Override
    public String report() {
      return "Calculating similarity scores...";
    }
  },
  OVERLAY(50) {
    @Override
    public String report() {
      return "Creating Overlay...";
    }
  },
  ZIPSELECT(75) {
    @Override
    public String report() {
      return "Save alignment?";
    }
  },
  SAVING(95) {
    @Override
    public String report() {
      return "Saving ZIP File";
    }
  },
  COMPLETED(100) {
    @Override
    public String report() {
      return "Complete";
    }
  };

  private final int progressValue;

  StatusProgress(int progressValue) {
    this.progressValue = progressValue;
  }

  public int getProgressValue() {
    return this.progressValue;
  }

  public String report() {
    return "Canceled.";
  }
}

class ThresholdPanel extends Panel {
  JSlider slider;
  JLabel label;
  ImagePlus imp;
  ImageProcessor k;
  ImageProcessor ovr;
  ImageProcessor backup;
  GridBagLayout layout;
  GridBagConstraints gbc;
  int clq_size;
  JLabel clq;

  ThresholdPanel(int clq_size, ImagePlus imp, ImageProcessor k, ImageProcessor ovr) {
    super();
    this.layout = new GridBagLayout();
    this.gbc = new GridBagConstraints();
    this.setLayout(layout);
    this.imp = imp;
    this.k = k;
    this.clq_size = clq_size;
    this.ovr = ovr;
    this.backup = ovr.duplicate();
    ovr.snapshot();
    setUI();
    setReactions();
  }

  void setUI() {
    this.slider = new JSlider(JSlider.HORIZONTAL, 1, 255, 110);
    this.slider.setToolTipText("set transparency of overlay");
    this.label = new JLabel("Transparency");
    this.clq = new JLabel(clq_size + " Points Aligned");
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridwidth = 3;
    layout.setConstraints(clq, gbc);
    this.add(clq);
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    layout.setConstraints(label, gbc);
    this.add(label);
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    layout.setConstraints(slider, gbc);
    this.add(slider);
  }

  void doOverlay(int b) {
    ImageProcessor k_pres = k.duplicate();
    k_pres.setColorModel(CustomColorModelFactory.getModelThreshed(b));
    ImageRoi roi = new ImageRoi(0, 0, k_pres);
    ovr.reset();
    // ovr.snapshot();
    ovr.drawRoi(roi);
    imp.repaintWindow();
  }

  void setReactions() {
    this.slider.addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent changeEvent) {
            if (slider.getValueIsAdjusting()) {
              int b = slider.getValue();
              doOverlay(b);
            }
          }
        });
  }
}

public class Align_Runner implements PlugIn {
  ImagePlus q_img;
  Point[] q_pts;
  PolygonRoi q_bounds;

  ImagePlus k_img;
  Point[] k_pts;
  PolygonRoi k_bounds;
  double delta;
  double epsilon;
  double min_ratio;
  double max_ratio;
  int lower_bound;
  int upper_bound;
  boolean show_score;
  String score_name;

  /* these are post-processing */
  AdjMat amat;
  ArrayList<Integer> clq;
  ArrayList<Integer> altclq;
  AlignImagePairFromPoints<SimilarityModel2D> aip;
  ImagePlus rimg;
  ImagePlus histPlot;
  double score;

  public Align_Runner(
      ImagePlus q_img,
      ImagePlus k_img,
      double delta,
      double epsilon,
      double min_ratio,
      double max_ratio,
      int lower_bound,
      boolean show_score,
      String score_name) {
    this.q_img = q_img;
    this.q_pts = ((PointRoi) q_img.getProperty("points")).getContainedPoints();
    this.q_bounds = (PolygonRoi) q_img.getProperty("bounds");
    this.k_img = k_img;
    this.k_pts = ((PointRoi) k_img.getProperty("points")).getContainedPoints();
    this.k_bounds = (PolygonRoi) k_img.getProperty("bounds");
    this.delta = delta;
    this.epsilon = epsilon;
    this.min_ratio = min_ratio;
    this.max_ratio = max_ratio;
    this.lower_bound = lower_bound;
    this.upper_bound = Math.min(q_pts.length, k_pts.length);
    this.show_score = show_score;
    this.score_name = score_name;
    /* */
    amat = null;
    clq = new ArrayList<>();
    aip = new AlignImagePairFromPoints<>(SimilarityModel2D::new);
    rimg = null;
    histPlot = null;
    score = 0.0;
  }

  public static void callFromMacro() {
    Align_RunnerGUI gui = new Align_RunnerGUI();
    gui.loadReactions();

    int p =
        JOptionPane.showConfirmDialog(
            null, gui.getPanel(), "Align Images with Markup", JOptionPane.OK_CANCEL_OPTION);
    if (!gui.isUiLoaded() || p == JOptionPane.CANCEL_OPTION) return;

    ImagePlus q_img = gui.getQImg();
    ImagePlus k_img = gui.getKImg();
    double delta = gui.getDelta();
    double epsilon = gui.getEpsilon();
    double min_ratio = gui.getMinRatio();
    double max_ratio = gui.getMaxRatio();
    int lower_bound = gui.getLowerBound();
    boolean show_score = gui.getShowScore();
    String score_name = gui.getScoreName();

    Align_Runner x =
        new Align_Runner(
            q_img,
            k_img,
            delta,
            epsilon,
            min_ratio,
            max_ratio,
            lower_bound,
            show_score,
            score_name);
    x.run("");
  }

  public void run(String arg) {
    AlignProgression prog = new AlignProgression(this);
    q_img.lock();
    k_img.lock();
    prog.run();
    q_img.unlock();
    k_img.unlock();
  }

  void make_adjmat() {
    Mapper3 x = new Mapper3();
    System.out.println("creating the graph");
    this.amat =
        x.construct_graph(
            q_pts, q_pts.length, k_pts, k_pts.length, delta, epsilon, min_ratio, max_ratio);
  }

  int get_heuristic_lb(AdjMat a, int l) {
    ArrayList<Integer> s1 = a.get_pruned_indices(l);
    if (s1.isEmpty()) {
      return 0;
    }
    AdjMat submat = a.get_submat(s1);
    Graph g = new Graph();
    g.load_matrix(submat.matsize, submat.mat);
    HeuristicSearch h = new HeuristicSearch();
    h.process_graph(g);
    ArrayList<Integer> clq = g.get_max_clique();
    return clq.size();
  }

  ArrayList<Integer> find_clique(AdjMat a, int l) {
    ArrayList<Integer> res = new ArrayList<>();
    int lb = get_heuristic_lb(a, l);
    // System.out.printf("heuristic gave: %d, lb is: %d\n", lb, lower_bound);
    lb = Math.max(lb, l);
    if (lb >= upper_bound) lb = upper_bound - 1;

    StackDFS s = new StackDFS();
    ArrayList<Integer> s2 = amat.get_pruned_indices(lb);
    if (s2.isEmpty()) return res;

    AdjMat s2m = a.get_submat(s2);
    Graph subg = new Graph();
    subg.load_matrix(s2m.matsize, s2m.mat);
    // System.out.printf("%d, %d, %s\n", l, amat.matsize, subg.toString());
    s.process_graph(subg, lb, upper_bound); /* warning is glitch */

    ArrayList<Integer> subclq = subg.get_max_clique();
    for (int x : subclq) {
      res.add(s2.get(x));
    }
    return res;
  }

  ArrayList<Integer> clique_breaker(AdjMat a) {
    ArrayList<Integer> res = new ArrayList<>();
    for (int l = upper_bound; l >= lower_bound; l--) {
      res = find_clique(a, l);
      if (!res.isEmpty()) {
        break;
      }
    }
    return res;
  }

  void clique_search() {
    System.out.println("max clique");
    this.clq = clique_breaker(this.amat);
    /* clq = new ArrayList<>(clq.stream().limit(5).collect(Collectors.toList())); */
    AdjMat clean = this.amat.get_wipemat(this.clq);
    this.altclq = clique_breaker(clean);
    System.out.println(this.clq);
    System.out.println(this.altclq);
  }

  void transformImages() throws NotEnoughDataPointsException, IllDefinedDataPointsException {
    aip.load(q_pts, k_pts, clq);
    System.out.println("fitting tform...");
    aip.estimate();
    System.out.println("should be calcing scores");
  }

  void calculateScore() {
    Point[] qp1 = q_pts;
    ArrayList<Integer> qc_ind = aip.getCorrQ_ind();
    /* TODO: score is currently set to clique_fraction always,
    have a generalized approach for different scores */
    score = (1.0 * qc_ind.size()) / qp1.length;
  }

  void viewScoreWithHistogram() throws FileNotFoundException {
    ScoreViewer s = ScoreViewer.fromJSONInFolder(score_name);
    this.histPlot = s.showScoreWithReference(score);
  }

  public void createOverlay() {
    ij.ImageStack q_stack = (ImageStack) q_img.getProperty("stack");
    Point[] qp1 = q_pts;
    ArrayList<Integer> qc_ind = aip.getCorrQ_ind();
    Stroke qs = new BasicStroke(18F);
    Color qcol = new Color(0xf8, 0x5d, 0x19, 0xff);

    ij.ImageStack k_stack = (ImageStack) k_img.getProperty("stack");
    Point[] kp1 = aip.getMappedK_ptsAsRoi().getContainedPoints();
    PolygonRoi mapped_k_bounds = aip.mapPolygonRoi(k_bounds, false);
    ArrayList<Integer> kc_ind = aip.getCorrK_ind();
    Stroke ks = new BasicStroke(18F);
    Color kcol = new Color(0x13, 0x83, 0xbd, 0xff);

    ImageProcessor q0 = q_stack.getProcessor(1).duplicate();
    q0.setColor(Color.BLACK);
    q0.fillOutside(q_bounds);

    ImageProcessor k0 = k_stack.getProcessor(1).duplicate();
    k0.setColor(Color.BLACK);
    k0.fillOutside(k_bounds);

    ImageProcessor q1 = q0.duplicate();
    ImageProcessor q2 = q_stack.getProcessor(2).duplicate();

    /* transform images via fit */
    ImageProcessor k1 = k0.createProcessor(q1.getWidth(), q1.getHeight());
    aip.mapImage(k0, false, k1);

    ImageProcessor k10 = k1.convertToByteProcessor().duplicate();
    ShapeRoi common_bounds = new ShapeRoi(mapped_k_bounds);
    common_bounds = common_bounds.and(new ShapeRoi(q_bounds));
    k10.setColor(Color.WHITE);
    k10.fillOutside(common_bounds);

    /* render necessary things on overlay */
    BufferedImage bi;
    Graphics2D g;
    Overlay pts_overlay = new Overlay();
    ImageProcessor tmp = q1.createProcessor(q1.getWidth(), q1.getHeight());
    bi = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_INT_ARGB);
    g = (Graphics2D) bi.getGraphics();
    g.setPaint(new Color(0, 0, 0, 0));
    g.fillRect(0, 0, q1.getWidth(), q1.getHeight());
    g.setBackground(new Color(0, 0, 0, 0));
    g.setColor(new Color(0, 0, 0, 0));
    burnPoints(g, qp1, qc_ind, qs, qcol);
    burnPoints(g, kp1, kc_ind, ks, kcol);
    pts_overlay.add(new ImageRoi(0, 0, bi));

    q1.drawOverlay(pts_overlay);
    q2.drawOverlay(pts_overlay);
    k1.drawOverlay(pts_overlay);

    ImageProcessor ovr = q1.duplicate();
    ovr.drawOverlay(pts_overlay);

    ij.ImageStack res = new ImageStack();
    res.addSlice(ovr);
    res.addSlice(q1);
    res.addSlice(k1);
    res.addSlice(q2);
    this.rimg = new ImagePlus("Overlay", res);
    this.rimg.show();
    ThresholdPanel tpanel = new ThresholdPanel(this.clq.size(), this.rimg, k10, ovr);
    this.rimg.getWindow().add(tpanel, 1);
    this.rimg.getCanvas().fitToWindow();
    this.rimg.getWindow().pack();
    tpanel.doOverlay(110);
  }

  BufferedImage getWritableImage(ImageProcessor imp) {
    BufferedImage bi =
        new BufferedImage(imp.getWidth(), imp.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = (Graphics2D) bi.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.drawImage(imp.createImage(), 0, 0, null);
    return bi;
  }

  ImagePlus rasterize(BufferedImage bi) {
    return new ImagePlus("", new ColorProcessor(bi));
  }

  boolean saveOverlay(String targ_zip, JLabel currentWork, String imgtype) {
    ij.ImageStack q_stack = (ImageStack) q_img.getProperty("stack");
    Point[] qp0 = q_pts;
    ArrayList<Integer> qc_ind = aip.getCorrQ_ind();
    Stroke qs = new BasicStroke(18F);
    Color qcol = new Color(0xf8, 0x5d, 0x19, 0xff);

    ij.ImageStack k_stack = (ImageStack) k_img.getProperty("stack");
    Point[] kp0 = k_pts;
    Point[] kp1 = aip.getMappedK_ptsAsRoi().getContainedPoints();
    ArrayList<Integer> kc_ind = aip.getCorrK_ind();
    Stroke ks = new BasicStroke(18F);
    Color kcol = new Color(0x13, 0x83, 0xbd, 0xff);

    System.out.println("Saving to " + targ_zip);
    DataOutputStream out;
    FileInfo info;
    ImagePlus img;
    TiffEncoder te;

    BufferedImage bi;
    Graphics2D g;

    ImageProcessor temp;
    MarkupData m;
    ImageStack res_stack = new ImageStack();

    /* add Q image */
    bi = getWritableImage(q_stack.getProcessor(3));
    res_stack.addSlice("Q_image", rasterize(bi).getProcessor());

    /* add Q mask */
    bi = getWritableImage(q_stack.getProcessor(2));
    res_stack.addSlice("Q_mask", rasterize(bi).getProcessor());

    /* add Q annotations */
    bi =
        getWritableImage(
            q_stack.getProcessor(2).createProcessor(q_stack.getWidth(), q_stack.getHeight()));
    g = (Graphics2D) bi.getGraphics();
    burnPoints(g, qp0, qc_ind, qs, qcol);
    res_stack.addSlice("Q_points", rasterize(bi).getProcessor());

    /* add K image */
    bi = getWritableImage(k_stack.getProcessor(3));
    res_stack.addSlice("K_image", rasterize(bi).getProcessor());

    /* add K mask */
    bi = getWritableImage(k_stack.getProcessor(2));
    res_stack.addSlice("K_mask", rasterize(bi).getProcessor());

    /* add K annotations */
    bi =
        getWritableImage(
            k_stack.getProcessor(2).createProcessor(k_stack.getWidth(), k_stack.getHeight()));
    g = (Graphics2D) bi.getGraphics();
    burnPoints(g, kp0, kc_ind, ks, kcol);
    res_stack.addSlice("K_points", rasterize(bi).getProcessor());

    /* add transformed K image */
    temp = k_stack.getProcessor(3).createProcessor(q_stack.getWidth(), q_stack.getHeight());
    aip.mapImage(k_stack.getProcessor(3), false, temp);
    bi = getWritableImage(temp);
    res_stack.addSlice("K_image_MAPPED", rasterize(bi).getProcessor());

    /* add transformed K mask */
    temp = k_stack.getProcessor(2).createProcessor(q_stack.getWidth(), q_stack.getHeight());
    aip.mapImage(k_stack.getProcessor(2), false, temp);
    bi = getWritableImage(temp);
    res_stack.addSlice("K_mask_MAPPED", rasterize(bi).getProcessor());

    /* add transformed K annotations */
    temp = k_stack.getProcessor(2).createProcessor(q_stack.getWidth(), q_stack.getHeight());
    bi = getWritableImage(temp);
    g = (Graphics2D) bi.getGraphics();
    burnPoints(g, kp1, kc_ind, ks, kcol);
    res_stack.addSlice("K_points_MAPPED", rasterize(bi).getProcessor());

    try {
      ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(targ_zip)));
      out = new DataOutputStream(new BufferedOutputStream(zos, 4096));
      int n = res_stack.size();
      for (int j = 1; j <= n; ++j) {
        String tmp = res_stack.getSliceLabel(j);
        currentWork.setText("Saving in ZIP: " + tmp);
        img = new ImagePlus("", res_stack.getProcessor(j));
        info = img.getFileInfo();
        if (imgtype.equals("png")) {
          zos.putNextEntry(new ZipEntry(tmp + ".png"));
          ImageIO.write(img.getBufferedImage(), "png", out);
        } else {
          zos.putNextEntry(new ZipEntry(tmp + ".tiff"));
          te = new TiffEncoder(info);
          te.write(out);
        }
        System.out.println(res_stack.getSliceLabel(j));
      }

      m =
          MarkupData.fromROIPair(
              (PolygonRoi) q_img.getProperty("bounds"), (PointRoi) q_img.getProperty("points"));
      zos.putNextEntry(new ZipEntry("Q_markup.json"));
      zos.write(m.toJSON().toString().getBytes(StandardCharsets.UTF_8));

      m =
          MarkupData.fromROIPair(
              (PolygonRoi) k_img.getProperty("bounds"), (PointRoi) k_img.getProperty("points"));
      zos.putNextEntry(new ZipEntry("K_markup.json"));
      zos.write(m.toJSON().toString().getBytes(StandardCharsets.UTF_8));

      JSONObject res = new JSONObject();
      res.put("alignment", aip.toJSON());
      if (show_score) {
        JSONObject sc = new JSONObject();
        sc.put("name", score_name);
        sc.put("value", score);
        res.put("score", sc);
      }
      zos.putNextEntry(new ZipEntry("align_and_score.json"));
      zos.write(res.toString().getBytes(StandardCharsets.UTF_8));

      out.close();
      return true;
    } catch (Exception e) {
      System.out.println("failed: " + e.getMessage() + " " + e);
      e.printStackTrace();
      return false;
    }
  }

  void burnPoints(Graphics2D g, Point[] pts, ArrayList<Integer> clq_ind, Stroke s, Color c) {
    g.setStroke(s);
    g.setPaint(c);
    for (int j = 0; j < pts.length; ++j) {
      if (clq_ind.contains(j)) {
        g.drawRect(pts[j].x, pts[j].y, 53, 53);
      } else {
        g.drawOval(pts[j].x, pts[j].y, 75, 75);
      }
    }
  }
}

class ProgressInterruptListener implements ActionListener {
  Thread t;
  StatusProgress s;

  ProgressInterruptListener(Thread t, StatusProgress s) {
    super();
    this.t = t;
    this.s = s;
  }

  @Override
  public void actionPerformed(ActionEvent actionEvent) {
    s = StatusProgress.CANCELED;
    t.interrupt();
  }
}

class AlignProgression {
  String targ_zip;
  StatusProgress status;
  int prevstat;
  JFrame frame;
  JPanel subpanel;

  GridBagLayout layout;
  GridBagConstraints gbc;
  JProgressBar bar;
  JLabel currentWork;
  JButton saveOK;
  JButton cancelRun;

  JButton nextMax;

  ButtonGroup imgtype;
  JRadioButton asPNG;
  JRadioButton asTIFF;

  Align_Runner x;

  AlignProgression(Align_Runner runner) {
    this.x = runner;
    targ_zip = "";
    status = StatusProgress.STARTING;
    loadUI();
    loadReactions();
  }

  void loadUI() {
    frame = new JFrame();
    frame.setTitle("processing....");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setSize(320, 240);

    layout = new GridBagLayout();
    subpanel = new JPanel(layout);
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;

    bar = new JProgressBar();
    currentWork = new JLabel();
    saveOK = new JButton("Save Info");
    cancelRun = new JButton("Cancel");
    nextMax = new JButton("Next Alignment");
    imgtype = new ButtonGroup();
    asPNG = new JRadioButton("PNG");
    asTIFF = new JRadioButton("TIFF");
    imgtype.add(asPNG);
    imgtype.add(asTIFF);
    asTIFF.setSelected(true);
    asPNG.setSelected(false);

    gbc.gridx = 0;
    gbc.ipady = 40;
    gbc.gridy = 0;
    gbc.gridwidth = 4;
    layout.setConstraints(currentWork, gbc);
    subpanel.add(currentWork, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.ipady = 20;
    gbc.gridwidth = 4;
    layout.setConstraints(bar, gbc);
    subpanel.add(bar, gbc);

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.ipady = 30;
    gbc.gridwidth = 2;
    layout.setConstraints(saveOK, gbc);
    subpanel.add(saveOK, gbc);

    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.ipady = 30;
    gbc.gridwidth = 2;
    layout.setConstraints(cancelRun, gbc);
    subpanel.add(cancelRun, gbc);

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.ipady = 0;
    gbc.gridwidth = 1;
    layout.setConstraints(asPNG, gbc);
    subpanel.add(asPNG, gbc);
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.ipady = 0;
    gbc.gridwidth = 1;
    layout.setConstraints(asTIFF, gbc);
    subpanel.add(asTIFF, gbc);
    gbc.gridx = 2;
    gbc.gridy = 3;
    gbc.ipady = 0;
    gbc.gridwidth = 2;
    layout.setConstraints(nextMax, gbc);
    subpanel.add(nextMax, gbc);

    frame.setContentPane(subpanel);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }

  void loadReactions() {
    nextMax.setEnabled(false);
    saveOK.setEnabled(false);
    asTIFF.setEnabled(false);
    asPNG.setEnabled(false);
    saveOK.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("*.zip", "zip"));
            chooser.setDialogTitle("Save Info into a ZIP File");
            int returnValue = chooser.showSaveDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
              stepSetZipTarget(chooser);
            }
          }
        });
    nextMax.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            tryNextAlignment();
          }
        });
    cancelRun.setEnabled(true);
  }

  public boolean stillRunning() {
    return status != StatusProgress.COMPLETED && status != StatusProgress.CANCELED;
  }

  void setStatus(StatusProgress s) {
    if (stillRunning()) status = s;
  }

  public boolean atSameStep() {
    return prevstat == status.getProgressValue();
  }

  public void run() {
    Thread work =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  while (stillRunning()) {
                    doWork(); /* TODO: how to interrupt while doing work? */
                    if (Thread.currentThread().isInterrupted())
                      throw new InterruptedException("user cancellation");
                  }
                  stepFinishWork();
                } catch (Exception e) {
                  if (e instanceof InterruptedException) {
                    System.out.println("canceled: " + e);
                  } else {
                    int res =
                        JOptionPane.showConfirmDialog(
                            null,
                            "Unable to align images. Perhaps you can increase the allowable distortion, decrease the lower bound, or mark more points.\nPress OK to view debug log for more details.",
                            "Unable to Align!",
                            JOptionPane.OK_CANCEL_OPTION);
                    if (res == JOptionPane.OK_OPTION) {
                      e.printStackTrace();
                    }
                  }
                  stepFailCancel();
                } finally {
                  frame.setVisible(false);
                }
              }
            });
    cancelRun.addActionListener(new ProgressInterruptListener(work, status));
    frame.setVisible(true);
    work.start();
    try {
      work.join();
    } catch (InterruptedException e) {
      System.out.println("possible disposal" + e);
    } finally {
      if (x.rimg != null) {
        x.rimg.close();
        x.rimg = null;
      }
      if (x.histPlot != null) {
        x.histPlot.close();
        x.histPlot = null;
      }
    }
  }

  public void changeUI() {
    prevstat = status.getProgressValue();
    if (stillRunning()) {
      bar.setValue(status.getProgressValue());
      currentWork.setText(status.report());
    } else {
      frame.setVisible(false);
    }
  }

  public void doWork()
      throws IOException, NotEnoughDataPointsException, IllDefinedDataPointsException {
    switch (status) {
      case STARTING:
        setStatus(StatusProgress.RESCALING);
        break;
      case RESCALING:
        x.make_adjmat();
        setStatus(StatusProgress.ALIGNING);
        break;
      case ALIGNING:
        x.clique_search();
        setStatus(StatusProgress.SIMILARITY);
        break;
      case SIMILARITY:
        x.transformImages();
        x.calculateScore();
        setStatus(StatusProgress.OVERLAY);
        break;
      case OVERLAY:
        x.createOverlay();
        x.viewScoreWithHistogram();
        if (x.show_score) {
          x.histPlot.show();
        }
        saveOK.setEnabled(true);
        cancelRun.setEnabled(true);
        asTIFF.setEnabled(true);
        asPNG.setEnabled(true);
        nextMax.setEnabled(!x.altclq.isEmpty());
        setStatus(StatusProgress.ZIPSELECT);
        cancelRun.setText("Don't Save");
        break;
      case SAVING:
        saveOK.setEnabled(false);
        cancelRun.setEnabled(false);
        asTIFF.setEnabled(false);
        asPNG.setEnabled(false);
        frame.setTitle("Saving...");
        currentWork.setText("Saving...");
        String s = asPNG.isSelected() ? "png" : "tiff";
        if (!x.saveOverlay(targ_zip, currentWork, s)) {
          throw new IOException("unable to save zip");
        }
        setStatus(StatusProgress.COMPLETED);
        break;
    }
    if (!atSameStep()) changeUI();
  }

  void tryNextAlignment() {
    nextMax.setEnabled(false);
    saveOK.setEnabled(false);
    asTIFF.setEnabled(false);
    asPNG.setEnabled(false);
    cancelRun.setText("Cancel");
    x.amat = x.amat.get_wipemat(x.clq);
    status = StatusProgress.ALIGNING;
    if (x.rimg != null) {
      x.rimg.close();
      x.rimg = null;
    }
    if (x.histPlot != null) {
      x.histPlot.close();
      x.histPlot = null;
    }
    changeUI();
  }

  void stepSetZipTarget(JFileChooser chooser) {
    File file = chooser.getSelectedFile();
    targ_zip = file.getAbsolutePath();
    if (!targ_zip.isEmpty()) {
      if (!targ_zip.endsWith(".zip")) {
        targ_zip = targ_zip + ".zip";
      }
      status = StatusProgress.SAVING;
      changeUI();
    }
  }

  void stepFailCancel() {
    status = StatusProgress.CANCELED;
    changeUI();
  }

  void stepFinishWork() {
    setStatus(StatusProgress.COMPLETED);
    System.out.println("finished work thread");
  }
}

class AlignImagePairFromPoints<T extends mpicbg.models.AbstractModel<T>> {
  private final T tform_q2k;
  private final T tform_k2q;
  ArrayList<Integer> qc_ind;
  ArrayList<Integer> kc_ind;
  private double[][] q_pts;
  private double[][] qc;
  private double[][] k_pts;
  private double[][] kc;
  private int[] clique;

  public AlignImagePairFromPoints(Supplier<? extends T> ctor) {
    tform_q2k = Objects.requireNonNull(ctor).get();
    tform_k2q = Objects.requireNonNull(ctor).get();
  }

  public void load(Point[] q_pt0, Point[] k_pt0, ArrayList<Integer> c) {
    q_pts = new double[q_pt0.length][2];
    k_pts = new double[k_pt0.length][2];
    qc = new double[c.size()][2];
    kc = new double[c.size()][2];
    clique = new int[c.size()];
    qc_ind = new ArrayList<>();
    kc_ind = new ArrayList<>();

    int j, qlen, klen;
    qlen = q_pt0.length;
    klen = k_pt0.length;
    for (j = 0; j < qlen; ++j) {
      q_pts[j][0] = q_pt0[j].getX();
      q_pts[j][1] = q_pt0[j].getY();
    }

    for (j = 0; j < klen; ++j) {
      k_pts[j][0] = k_pt0[j].getX();
      k_pts[j][1] = k_pt0[j].getY();
    }

    for (j = 0; j < clique.length; ++j) {
      clique[j] = c.get(j);
    }
  }

  public void estimate() throws NotEnoughDataPointsException, IllDefinedDataPointsException {
    int j;
    int csize = clique.length;
    ArrayList<PointMatch> corr_k2q = new ArrayList<>();
    ArrayList<PointMatch> corr_q2k = new ArrayList<>();
    for (j = 0; j < csize; ++j) {
      int z = clique[j];
      /* save the indices of corresponding points */
      qc_ind.add(z / k_pts.length);
      kc_ind.add(z % k_pts.length);

      /* save the corresponding points */
      qc[j][0] = q_pts[z / k_pts.length][0];
      qc[j][1] = q_pts[z / k_pts.length][1];
      kc[j][0] = k_pts[z % k_pts.length][0];
      kc[j][1] = k_pts[z % k_pts.length][1];

      /* save the forward mapping */
      corr_k2q.add(new PointMatch(new mpicbg.models.Point(kc[j]), new mpicbg.models.Point(qc[j])));

      /* save the backward mapping */
      corr_q2k.add(new PointMatch(new mpicbg.models.Point(qc[j]), new mpicbg.models.Point(kc[j])));
    }
    tform_q2k.fit(corr_q2k);
    tform_k2q.fit(corr_k2q);
  }

  void mapImage(ImageProcessor img, boolean fromQToK, ImageProcessor result) {
    mpicbg.ij.TransformMapping<T> mapping;
    /* we are doing an inverse interpolation, similar to scipy.warp */
    if (fromQToK) {
      mapping = new TransformMapping<>(this.tform_k2q);
    } else {
      mapping = new TransformMapping<>(this.tform_q2k);
    }
    mapping.mapInverseInterpolated(img, result);
  }

  double[][] mapPoints(double[][] pts, boolean fromQToK) {
    double[][] res = new double[pts.length][2];
    double[] z;
    T tform;
    if (fromQToK) {
      tform = tform_q2k;
    } else {
      tform = tform_k2q;
    }
    for (int j = 0; j < pts.length; ++j) {
      z = tform.apply(pts[j]);
      res[j][0] = z[0];
      res[j][1] = z[1];
    }
    return res;
  }

  PolygonRoi mapPolygonRoi(PolygonRoi pts, boolean fromQToK) {
    double[] pt = new double[2];
    double[] z;
    FloatPolygon src = pts.getFloatPolygon();
    FloatPolygon res = new FloatPolygon();
    T tform;
    if (fromQToK) {
      tform = tform_q2k;
    } else {
      tform = tform_k2q;
    }
    for (int j = 0; j < src.npoints; ++j) {
      pt[0] = src.xpoints[j];
      pt[1] = src.ypoints[j];
      z = tform.apply(pt);
      res.addPoint(z[0], z[1]);
    }

    return new PolygonRoi(res, Roi.POLYGON);
  }

  PointRoi mapPointsAsRoi(double[][] pts, boolean fromQToK) {
    PointRoi res = new PointRoi();
    double[] z;
    T tform;
    if (fromQToK) {
      tform = tform_q2k;
    } else {
      tform = tform_k2q;
    }
    for (int j = 0; j < pts.length; ++j) {
      z = tform.apply(pts[j]);
      res.addPoint(z[0], z[1]);
    }
    return res;
  }

  private PointRoi getPointsAsRoi(double[][] pts) {
    PointRoi res = new PointRoi();
    for (double[] pt : pts) {
      res.addPoint(pt[0], pt[1]);
    }
    return res;
  }

  double[][] getQ_pts() {
    return this.q_pts;
  }

  double[][] getK_pts() {
    return this.k_pts;
  }

  double[][] getMappedQ_pts() {
    return this.mapPoints(this.q_pts, true);
  }

  double[][] getMappedK_pts() {
    return this.mapPoints(this.k_pts, false);
  }

  PointRoi getMappedQ_ptsAsRoi() {
    return this.mapPointsAsRoi(this.q_pts, true);
  }

  PointRoi getMappedK_ptsAsRoi() {
    return this.mapPointsAsRoi(this.k_pts, false);
  }

  ArrayList<Integer> getCorrQ_ind() {
    return this.qc_ind;
  }

  ArrayList<Integer> getCorrK_ind() {
    return this.kc_ind;
  }

  JSONObject pointData(double[][] pts, ArrayList<Integer> indices) {
    JSONObject res = new JSONObject();

    JSONArray jcp = new JSONArray();
    for (double[] z : pts) {
      JSONArray tmp = new JSONArray();
      tmp.put(z[0]);
      tmp.put(z[1]);
      jcp.put(tmp);
    }
    res.put("points", jcp);

    /* indices are of no use unless you have an un-modified
    markup.json, but we save them anyway, in case there is
    some post-processing help */
    JSONArray jci = new JSONArray();
    for (Integer i : indices) {
      jci.put(i);
    }
    res.put("indices", jci);
    return res;
  }

  public JSONObject toJSON() {
    JSONObject result = new JSONObject();
    result.put("Q", pointData(q_pts, qc_ind));
    result.put("K", pointData(k_pts, kc_ind));
    /* TODO: include transformation coefficients? transformed point clouds? */
    return result;
  }
}
