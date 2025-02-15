import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import javax.swing.*;

public class Align_RunnerGUI {
  private final JComboBox<String> Q_imgs;
  private final JComboBox<String> K_imgs;
  private final JFormattedTextField minRatioT;
  private final JFormattedTextField maxRatioT;
  private final JSlider deltaT;
  private final JSlider epsilonT;
  private final JLabel deltaTVal;
  private final JLabel epsilonTVal;
  private final JFormattedTextField lowerBoundT;
  private final JLabel Qimg_points;
  private final JLabel Kimg_points;
  private final JCheckBox showScoreT;
  private final JComboBox<String> scoreNamesT;
  private final JPanel panel;
  private final JTextArea dummy;
  private final HashMap<String, ImagePlus> imgmap;
  private boolean uiLoaded;

  public Align_RunnerGUI() {
    this.panel = new JPanel(new GridLayout(7, 4));
    this.dummy = new JTextArea();
    this.imgmap = new HashMap<>();
    this.Q_imgs = new JComboBox<>();
    Q_imgs.setEditable(false);
    this.Qimg_points = new JLabel();
    this.K_imgs = new JComboBox<>();
    K_imgs.setEditable(false);
    this.Kimg_points = new JLabel();
    this.minRatioT = new JFormattedTextField(NumberFormat.getInstance());
    this.maxRatioT = new JFormattedTextField(NumberFormat.getInstance());
    this.deltaT = new JSlider(1, 25);
    this.deltaT.setValue(10);
    this.epsilonT = new JSlider(1, 25);
    this.epsilonT.setValue(10);
    this.deltaTVal = new JLabel((deltaT.getValue() / 10.0) + " degrees");
    this.epsilonTVal = new JLabel((epsilonT.getValue() / 10.0) + " units");
    this.lowerBoundT = new JFormattedTextField(NumberFormat.getInstance());
    this.showScoreT = new JCheckBox("Similarity Score?");
    this.scoreNamesT = new JComboBox<>();
    this.uiLoaded = false;
    this.loadUI();
  }

  void cannotStart() {
    dummy.setText("You need to have 2 valid images open!");
    uiLoaded = false;
  }

  boolean UICheck() {
    int[] idList = WindowManager.getIDList();
    int valid_images = 0;
    if (idList == null || idList.length < 2) {
      cannotStart();
      return false;
    }
    for (int id : idList) {
      ImagePlus img = WindowManager.getImage(id);
      if (img == null) {
        continue;
      }
      PolygonRoi pol = (PolygonRoi) img.getProperty("bounds");
      PointRoi pts = (PointRoi) img.getProperty("points");
      String name = (String) img.getProperty("name");
      if (pol == null || pts == null || name == null) {
        continue;
      }
      valid_images += 1;
    }
    if (valid_images < 2) {
      cannotStart();
      return false;
    }
    return true;
  }

  void loadUI() {
    if (!UICheck()) {
      panel.add(dummy);
      return;
    }

    int[] idList = WindowManager.getIDList();
    ImagePlus tmp;
    for (int id : idList) {
      tmp = WindowManager.getImage(id);
      if (tmp.getProperty("points") == null) continue;
      if (tmp.getProperty("bounds") == null) continue;
      if (tmp.getProperty("name") == null) continue;
      imgmap.put((String) tmp.getProperty("name"), tmp);
      Q_imgs.addItem((String) tmp.getProperty("name"));
      K_imgs.addItem((String) tmp.getProperty("name"));
    }
    panel.add(new JLabel("Questioned Image:"));
    panel.add(Q_imgs);
    panel.add(new JLabel("Reference Image:"));
    panel.add(K_imgs);

    panel.add(new JLabel(""));
    panel.add(Qimg_points);
    panel.add(new JLabel(""));
    panel.add(Kimg_points);

    panel.add(new JLabel("Scale difference is around:"));
    panel.add(minRatioT);
    panel.add(new JLabel("and"));
    panel.add(maxRatioT);

    Dictionary dict = new Hashtable();
    dict.put(1, new JLabel("0.1"));
    dict.put(25, new JLabel("2.5"));
    this.deltaT.setLabelTable(dict);
    this.epsilonT.setLabelTable(dict);
    this.deltaT.setPaintLabels(true);
    this.epsilonT.setPaintLabels(true);

    deltaT.addChangeListener(
        e -> {
          if (deltaT.getValueIsAdjusting()) {
            deltaTVal.setText((deltaT.getValue() / 10.0) + " degrees");
          }
        });

    epsilonT.addChangeListener(
        e -> {
          if (epsilonT.getValueIsAdjusting()) {
            epsilonTVal.setText((epsilonT.getValue() / 10.0) + " units");
          }
        });

    panel.add(new JLabel("Maximum Angular Distortion"));
    panel.add(deltaT);
    panel.add(deltaTVal);
    panel.add(new JLabel(""));

    panel.add(new JLabel("Maximum Scaling Distortion"));
    panel.add(epsilonT);
    panel.add(epsilonTVal);
    panel.add(new JLabel(""));

    panel.add(new JLabel("Must Have At Least"));
    panel.add(lowerBoundT);
    panel.add(new JLabel("points in common"));
    panel.add(new JLabel(""));

    panel.add(showScoreT);
    panel.add(scoreNamesT);
    scoreNamesT.addItem("clique_fraction"); // ///////
    panel.add(new JLabel(""));
    panel.add(new JLabel(""));

    uiLoaded = true;
  }

  void missingMarkup(ImagePlus img) {
    this.dummy.setText("The Image: " + img.getProperty("name") + " is not loaded properly!");
    this.uiLoaded = false;
  }

  void getNumPoints(ImagePlus img, JLabel targ) {
    if (img == null) return;
    PointRoi r = (PointRoi) img.getProperty("points");
    targ.setText(r.size() + " points");
  }

  void loadReactions() {
    this.minRatioT.setText("0.8");
    this.maxRatioT.setText("1.2");
    this.lowerBoundT.setText("5");
    this.showScoreT.setSelected(true);
    this.Q_imgs.setSelectedIndex(0);
    this.K_imgs.setSelectedIndex(1);

    this.Q_imgs.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            ImagePlus z;
            PointRoi r;
            z = imgmap.get(Q_imgs.getSelectedItem());
            r = (PointRoi) z.getProperty("points");
            Qimg_points.setText(r.size() + " points");
          }
        });

    this.Q_imgs.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            getNumPoints(imgmap.get(Q_imgs.getSelectedItem()), Qimg_points);
          }
        });

    this.K_imgs.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            getNumPoints(imgmap.get(K_imgs.getSelectedItem()), Kimg_points);
          }
        });

    this.showScoreT.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scoreNamesT.setEnabled(showScoreT.isSelected());
          }
        });

    this.getNumPoints(imgmap.get(K_imgs.getSelectedItem()), Kimg_points);
    this.getNumPoints(imgmap.get(Q_imgs.getSelectedItem()), Qimg_points);
  }

  public JPanel getPanel() {
    return panel;
  }

  public boolean isUiLoaded() {
    return uiLoaded;
  }

  public ImagePlus getQImg() {
    return imgmap.get(Q_imgs.getSelectedItem());
  }

  public ImagePlus getKImg() {
    return imgmap.get(K_imgs.getSelectedItem());
  }

  public double getMinRatio() {
    return Double.parseDouble(minRatioT.getText());
  }

  public double getMaxRatio() {
    return Double.parseDouble(maxRatioT.getText());
  }

  public double getDelta() {
    /* angular distortion: convert degrees to radians */
    return (deltaT.getValue() / 10.0) * (Math.PI) / 180.0;
  }

  public double getEpsilon() {
    /* scaling distortion: unclear what units this is */
    return epsilonT.getValue() / 10.0;
  }

  public int getLowerBound() {
    return Integer.parseInt(lowerBoundT.getText());
  }

  public boolean getShowScore() {
    return showScoreT.isSelected();
  }

  public String getScoreName() {
    return (String) scoreNamesT.getSelectedItem();
  }
}
