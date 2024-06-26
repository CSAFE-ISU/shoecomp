import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.*;
import ij.io.FileInfo;
import ij.io.TiffEncoder;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import mpicbg.ij.TransformMapping;
import mpicbg.models.*;
import org.ahgamut.clqmtch.StackDFS;
import org.json.JSONArray;
import org.json.JSONObject;

public class Align_Runner implements PlugIn {

  private final Align_RunnerGUI gui;

  public Align_Runner() {
    gui = new Align_RunnerGUI();
    loadReactions();
  }

  public static void callFromMacro() {
    Align_Runner x = new Align_Runner();
    x.run("");
  }

  void missingMarkup(ImagePlus img) {
    gui.getDummy().setText("The Image: " + img.getProperty("name") + " is not loaded properly!");
    gui.setUiLoaded(false);
  }

  void loadReactions() {
    gui.getMinRatioT().setText("0.8");
    gui.getMaxRatioT().setText("1.2");
    //gui.getDeltaT().setText("5.0");
    //gui.getEpsilonT().setText("0.03");
    gui.getLowerBoundT().setText("10");
    gui.getShowScoreT().setSelected(true);
    gui.getQImgs().setSelectedIndex(0);
    gui.getKImgs().setSelectedIndex(1);

    gui.getQImgs().addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            ImagePlus z;
            PointRoi r;
            z = gui.getImgMap().get(gui.getQImgs().getSelectedItem());
            r = (PointRoi) z.getProperty("points");
            gui.getQimgPoints().setText(r.size() + " points");
          }
        });

    gui.getQImgs().addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            getNumPoints(gui.getImgMap().get(gui.getQImgs().getSelectedItem()), gui.getQimgPoints());
          }
        });

    gui.getKImgs().addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            getNumPoints(gui.getImgMap().get(gui.getKImgs().getSelectedItem()), gui.getKimgPoints());
          }
        });

    gui.getShowScoreT().addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            gui.getScoreNamesT().setEnabled(gui.getShowScoreT().isSelected());
          }
        });

    this.getNumPoints(gui.getImgMap().get(gui.getKImgs().getSelectedItem()), gui.getKimgPoints());
    this.getNumPoints(gui.getImgMap().get(gui.getQImgs().getSelectedItem()), gui.getQimgPoints());
  }

  void getNumPoints(ImagePlus img, JLabel targ) {
    if (img == null) return;
    PointRoi r = (PointRoi) img.getProperty("points");
    targ.setText(r.size() + " points");
  }

  public void run(String arg) {
    int p =
        JOptionPane.showConfirmDialog(
            null, gui.getPanel(), "Align Images with Markup", JOptionPane.OK_CANCEL_OPTION);
    if (!gui.isUiLoaded() || p == JOptionPane.CANCEL_OPTION) return;
    runWithProgress();
  }

  void runWithProgress() {
    ImagePlus q_img = gui.getImgMap().get(gui.getQImgs().getSelectedItem());
    ImagePlus k_img = gui.getImgMap().get(gui.getKImgs().getSelectedItem());
    PolygonRoi q_bounds = (PolygonRoi) q_img.getProperty("bounds");
    PolygonRoi k_bounds = (PolygonRoi) k_img.getProperty("bounds");
    Point[] q_pts = ((PointRoi) q_img.getProperty("points")).getContainedPoints();
    Point[] k_pts = ((PointRoi) k_img.getProperty("points")).getContainedPoints();
    /* convert degrees to radians */
    //double delta = Double.parseDouble(gui.getDeltaT().getText())* (Math.PI) / 180.0;
    double delta = (gui.getDeltaT().getValue() / 10.0) * (Math.PI) / 180.0;

    //double epsilon = Double.parseDouble(gui.getEpsilonT().getText());
    double epsilon = gui.getEpsilonT().getValue() / 10;
    double min_ratio = Double.parseDouble(gui.getMinRatioT().getText());
    double max_ratio = Double.parseDouble(gui.getMaxRatioT().getText());
    int lower_bound = Integer.parseInt(gui.getLowerBoundT().getText());
    int upper_bound = Math.min(q_pts.length, k_pts.length);
    boolean show_score = gui.getShowScoreT().isSelected();
    String score_name = (String) gui.getScoreNamesT().getSelectedItem();

    String[] works = {
      "Starting...",
      "Checking scales and aligning...",
      "Calculating similarity scores...",
      "Creating Overlay...",
      "Save alignment?",
      "Saving ZIP File"
    };
    int[] progressLevel = {5, 25, 50, 75, 80, 99};
    final int[] status = {0};
    Thread work = null;
    Thread ui = null;

    JFrame frame = new JFrame();
    frame.setTitle("processing....");
    frame.setSize(320, 240);

    JPanel subpanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JProgressBar bar = new JProgressBar();
    JLabel currentWork = new JLabel();
    JButton saveOK = new JButton("Save Info");
    saveOK.setEnabled(false);
    JButton cancelRun = new JButton("Cancel");
    cancelRun.setEnabled(true);

    gbc.gridx = 0;
    gbc.ipady = 40;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    subpanel.add(currentWork, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.ipady = 20;
    gbc.gridwidth = 2;
    subpanel.add(bar, gbc);

    gbc.gridx = 0;
    gbc.weightx = 0.5;
    gbc.gridy = 2;
    gbc.ipady = 30;
    gbc.gridwidth = 1;
    subpanel.add(saveOK, gbc);

    gbc.gridx = 1;
    gbc.weightx = 0.5;
    gbc.gridy = 2;
    gbc.ipady = 30;
    gbc.gridwidth = 1;
    subpanel.add(cancelRun, gbc);
    frame.setContentPane(subpanel);

    saveOK.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("*.zip", "zip"));
            chooser.setDialogTitle("Save Info into a ZIP File");
            int returnValue = chooser.showSaveDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
              File file = chooser.getSelectedFile();
              gui.setTargZip(file.getAbsolutePath());
              if (!gui.getTargZip().isEmpty()) {
                if (!gui.getTargZip().endsWith(".zip")) {
                  gui.setTargZip(gui.getTargZip() + ".zip");
                }
                synchronized (status) {
                  status[0] = 5;
                  status.notify();
                }
              }
            }
          }
        });

    cancelRun.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            synchronized (status) {
              status[0] = -1;
              status.notify();
            }
          }
        });

    work =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                q_img.lock();
                k_img.lock();
                java.util.ArrayList<Integer> clq = new ArrayList<>();
                AlignImagePairFromPoints<SimilarityModel2D> aip =
                    new AlignImagePairFromPoints<>(SimilarityModel2D::new);
                ImagePlus rimg = null;
                ImagePlus histPlot = null;
                double score = 0.0;

                try {
                  while (status[0] >= 0 && status[0] < 6) {
                    switch (status[0]) {
                      case 0:
                        synchronized (status) {
                          if (status[0] != -1) status[0] = 1;
                          status.notify();
                        }
                        break;
                      case 1:
                        /* find max clique (TODO: lower_bound) */
                        System.out.println("max clique");
                        clq = this.get_clique();
                        synchronized (status) {
                          //IJ.log("Original was " + status[0]);
                          if (status[0] == 0) status[0] = 4;
                          else if (status[0] != -1) status[0] = 2;
                          //IJ.log("Transformed into " + status[0]);
                          status.notify();
                        }
                        break;
                      case 2:
                        /* find transform fit */
                        aip.load(q_pts, k_pts, clq);
                        System.out.println("fitting tform...");
                        aip.estimate();
                        System.out.println("should be calcing scores");
                        score = calculateScore(aip, score_name);
                        synchronized (status) {
                          if (status[0] != -1) status[0] = 3;
                          status.notify();
                        }
                        break;
                      case 3:
                        rimg = createOverlay(aip);
                        histPlot = viewScoreWithHistogram(score_name, score);
                        histPlot.show();
                        saveOK.setEnabled(true);
                        cancelRun.setEnabled(true);
                        rimg.show();
                        synchronized (status) {
                          if (status[0] != -1) status[0] = 4;
                          status.notify();
                        }
                        cancelRun.setText("Don't Save");
                        break;
                      case 4:
                        synchronized (status) {
                          while (status[0] == 4) status.wait();
                        }
                        break;
                      case 5:
                        saveOK.setEnabled(false);
                        cancelRun.setEnabled(false);
                        frame.setTitle("Saving...");
                        currentWork.setText("Saving...");
                        if (!saveOverlay(aip, score_name, score)) {
                          throw new IOException("unable to save zip");
                        }
                        synchronized (status) {
                          if (status[0] != -1) status[0] = 6;
                          status.notify();
                        }
                        break;
                      default:
                        System.out.println("waiting");
                        Thread.sleep(250);
                    }
                  }
                  System.out.println("finished work thread");
                  synchronized (status) {
                    if (status[0] != -1) status[0] = 6;
                    status.notify();
                  }
                } catch (Exception e) {
                  System.out.println("failed: " + e.getMessage() + " " + e);
                  e.printStackTrace();
                  synchronized (status) {
                    status[0] = -1;
                    status.notify();
                  }
                }
                if (rimg != null) rimg.close();
                if (histPlot != null) histPlot.close();
                q_img.unlock();
                k_img.unlock();
              }

              ArrayList<Integer> get_clique() {
                Mapper3 x = new Mapper3();
                org.ahgamut.clqmtch.Graph g =
                    x.construct_graph(
                        q_pts,
                        q_pts.length,
                        k_pts,
                        k_pts.length,
                        delta,
                        epsilon,
                        min_ratio,
                        max_ratio);
                status[0] += 1;
                org.ahgamut.clqmtch.StackDFS s = new StackDFS();
                s.process_graph(g, lower_bound, upper_bound); /* warning is glitch */
                System.out.println(g.toString());
                return g.get_max_clique();
              }

              double calculateScore(AlignImagePairFromPoints<?> aip, String scoreName) {
                Point[] qp1 = q_pts;
                ArrayList<Integer> qc_ind = aip.getCorrQ_ind();
                /* TODO: score is currently set to clique_fraction always,
                have a generalized approach for different scores */
                return (1.0 * qc_ind.size()) / qp1.length;
              }

              ImagePlus viewScoreWithHistogram(String scoreName, double score)
                  throws FileNotFoundException {
                ScoreViewer s = ScoreViewer.fromJSONInFolder(scoreName);
                return s.showScoreWithReference(score);
              }

              ImagePlus createOverlay(AlignImagePairFromPoints<?> aip) {
                ij.ImageStack q_stack = q_img.getImageStack();
                Point[] qp1 = q_pts;
                ArrayList<Integer> qc_ind = aip.getCorrQ_ind();
                Stroke qs = new BasicStroke(18F);
                Color qcol = new Color(255, 0, 0, 157);

                ij.ImageStack k_stack = k_img.getImageStack();
                Point[] kp1 = aip.getMappedK_ptsAsRoi().getContainedPoints();
                PolygonRoi mapped_k_bounds = aip.mapPolygonRoi(k_bounds, false);
                ArrayList<Integer> kc_ind = aip.getCorrK_ind();
                Stroke ks = new BasicStroke(18F);
                Color kcol = new Color(0, 0, 255, 157);

                /* render necessary things on overlay */
                BufferedImage bi;
                Graphics2D g;

                bi = getWritableImage(q_stack.getProcessor(1));
                g = (Graphics2D) bi.getGraphics();
                burnPoints(g, qp1, qc_ind, qs, qcol);
                burnPoints(g, kp1, kc_ind, ks, kcol);
                ImageProcessor q1 = rasterize(bi).getProcessor();

                bi = getWritableImage(q_stack.getProcessor(2));
                g = (Graphics2D) bi.getGraphics();
                burnPoints(g, qp1, qc_ind, qs, qcol);
                burnPoints(g, kp1, kc_ind, ks, kcol);
                ImageProcessor q2 = rasterize(bi).getProcessor();

                /* transform images via fit */
                ImageProcessor k1 =
                    k_stack.getProcessor(1).createProcessor(q1.getWidth(), q1.getHeight());
                aip.mapImage(k_stack.getProcessor(1), false, k1);

                ImageProcessor k10 = k1.convertToByteProcessor().duplicate();
                ShapeRoi common_bounds = new ShapeRoi(mapped_k_bounds);
                common_bounds = common_bounds.and(new ShapeRoi(q_bounds));
                k10.setColor(Color.WHITE);
                k10.fillOutside(common_bounds);
                k10.setColorModel(CustomColorModelFactory.getDefaultModel());
                bi = getWritableImage(q_stack.getProcessor(1));
                g = (Graphics2D) bi.getGraphics();
                g.drawImage(k10.createImage(), 0, 0, null);
                burnPoints(g, qp1, qc_ind, qs, qcol);
                burnPoints(g, kp1, kc_ind, ks, kcol);
                ImageProcessor ovr = rasterize(bi).getProcessor();

                bi = getWritableImage(k1);
                g = (Graphics2D) bi.getGraphics();
                burnPoints(g, qp1, qc_ind, qs, qcol);
                burnPoints(g, kp1, kc_ind, ks, kcol);
                k1 = rasterize(bi).getProcessor();

                ij.ImageStack res = new ImageStack();
                res.addSlice(ovr);
                res.addSlice(q1);
                res.addSlice(k1);
                res.addSlice(q2);
                return new ImagePlus("Overlay", res); ////////
              }

              boolean saveOverlay(
                  AlignImagePairFromPoints<?> aip, String score_name, double score) {
                ij.ImageStack q_stack = q_img.getImageStack();
                Point[] qp0 = q_pts;
                ArrayList<Integer> qc_ind = aip.getCorrQ_ind();
                Stroke qs = new BasicStroke(18F);
                Color qcol = new Color(255, 0, 0, 157);

                ij.ImageStack k_stack = k_img.getImageStack();
                Point[] kp0 = k_pts;
                Point[] kp1 = aip.getMappedK_ptsAsRoi().getContainedPoints();
                ArrayList<Integer> kc_ind = aip.getCorrK_ind();
                Stroke ks = new BasicStroke(18F);
                Color kcol = new Color(0, 0, 255, 157);

                System.out.println("Saving to " + gui.getTargZip());
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
                        q_stack
                            .getProcessor(2)
                            .createProcessor(q_stack.getWidth(), q_stack.getHeight()));
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
                        k_stack
                            .getProcessor(2)
                            .createProcessor(k_stack.getWidth(), k_stack.getHeight()));
                g = (Graphics2D) bi.getGraphics();
                burnPoints(g, kp0, kc_ind, ks, kcol);
                res_stack.addSlice("K_points", rasterize(bi).getProcessor());

                /* add transformed K image */
                temp =
                    k_stack
                        .getProcessor(3)
                        .createProcessor(q_stack.getWidth(), q_stack.getHeight());
                aip.mapImage(k_stack.getProcessor(3), false, temp);
                bi = getWritableImage(temp);
                res_stack.addSlice("K_image_MAPPED", rasterize(bi).getProcessor());

                /* add transformed K mask */
                temp =
                    k_stack
                        .getProcessor(2)
                        .createProcessor(q_stack.getWidth(), q_stack.getHeight());
                aip.mapImage(k_stack.getProcessor(2), false, temp);
                bi = getWritableImage(temp);
                res_stack.addSlice("K_mask_MAPPED", rasterize(bi).getProcessor());

                /* add transformed K annotations */
                temp =
                    k_stack
                        .getProcessor(2)
                        .createProcessor(q_stack.getWidth(), q_stack.getHeight());
                bi = getWritableImage(temp);
                g = (Graphics2D) bi.getGraphics();
                burnPoints(g, kp1, kc_ind, ks, kcol);
                res_stack.addSlice("K_points_MAPPED", rasterize(bi).getProcessor());

                try {
                  ZipOutputStream zos =
                      new ZipOutputStream(Files.newOutputStream(Paths.get(gui.getTargZip())));
                  out = new DataOutputStream(new BufferedOutputStream(zos, 4096));
                  int n = res_stack.size();
                  for (int j = 1; j <= n; ++j) {
                    String tmp = res_stack.getSliceLabel(j);
                    currentWork.setText("Saving in ZIP: " + tmp);
                    img = new ImagePlus("", res_stack.getProcessor(j));
                    info = img.getFileInfo();
                    zos.putNextEntry(new ZipEntry(tmp + ".tiff"));
                    te = new TiffEncoder(info);
                    System.out.println(res_stack.getSliceLabel(j));
                    te.write(out);
                  }

                  m =
                      MarkupData.fromROIPair(
                          (PolygonRoi) q_img.getProperty("bounds"),
                          (PointRoi) q_img.getProperty("points"));
                  zos.putNextEntry(new ZipEntry("Q_markup.json"));
                  zos.write(m.toJSON().toString().getBytes(StandardCharsets.UTF_8));

                  m =
                      MarkupData.fromROIPair(
                          (PolygonRoi) k_img.getProperty("bounds"),
                          (PointRoi) k_img.getProperty("points"));
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

              BufferedImage getWritableImage(ImageProcessor imp) {
                BufferedImage bi =
                    new BufferedImage(imp.getWidth(), imp.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D) bi.getGraphics();
                g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(imp.createImage(), 0, 0, null);
                return bi;
              }

              ImagePlus rasterize(BufferedImage bi) {
                return new ImagePlus("", new ColorProcessor(bi));
              }

              void burnPoints(
                  Graphics2D g, Point[] pts, ArrayList<Integer> clq_ind, Stroke s, Color c) {
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
            });

    ui =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  int prevstat = -1;

                  while (status[0] >= 0 && status[0] < 6) {
                    bar.setValue(progressLevel[status[0]]);
                    if (status[0] != 5) {
                      currentWork.setText(works[status[0]]);
                    }
                    synchronized (status) {
                      while (status[0] == prevstat) {
                        status.wait();
                      }
                    }
                    prevstat = status[0];
                  }
                  frame.setVisible(false);
                  status[0] = -1;
                  System.out.println("finished ui thread");
                } catch (Exception e) {
                  frame.setVisible(false);
                  e.printStackTrace();
                }
              }
            });

    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setVisible(true);
    ui.start();
    work.start();
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
    Collections.sort(qc_ind);
    Collections.sort(kc_ind);
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

class CustomColorModelFactory {
  public static IndexColorModel getModel(int r, int g, int b, int a, int bar) {
    // all pixel values above bar are transparent
    // rest are filled with r,g,b,a
    byte[] red = new byte[256];
    byte[] green = new byte[256];
    byte[] blue = new byte[256];
    byte[] alpha = new byte[256];

    for (int i = 0; i < 256; ++i) {
      red[i] = (byte) r;
      green[i] = (byte) g;
      blue[i] = (byte) b;
      if (i < bar) {
        alpha[i] = (byte) a;
      } else {
        alpha[i] = (byte) 0;
      }
    }

    return new IndexColorModel(8, 256, red, green, blue, alpha);
  }

  public static IndexColorModel getDefaultModel() {
    int r = 0x9f;
    int g = 0x14;
    int b = 0x96;
    int a = 151;
    int bar = 110; // all pixel values above this are transparent
    return getModel(r, g, b, a, bar);
  }
}
