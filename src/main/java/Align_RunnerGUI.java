import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

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
    private String targ_zip;
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
        this.deltaT = new JSlider(1, 50);
        this.deltaT.setValue(10);
        this.epsilonT = new JSlider(1, 50);
        this.epsilonT.setValue(10);
        this.deltaTVal = new JLabel((deltaT.getValue() / 10.0) + " degrees");
        this.epsilonTVal = new JLabel((epsilonT.getValue() / 10.0) + " units");
        this.lowerBoundT = new JFormattedTextField(NumberFormat.getInstance());
        this.showScoreT = new JCheckBox("Similarity Score?");
        this.scoreNamesT = new JComboBox<>();
        this.uiLoaded = false;
        this.targ_zip = "";
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
        dict.put(50, new JLabel("5.0"));
        this.deltaT.setLabelTable(dict);
        this.epsilonT.setLabelTable(dict);
        this.deltaT.setPaintLabels(true);
        this.epsilonT.setPaintLabels(true);

        deltaT.addChangeListener(e -> {
            if (deltaT.getValueIsAdjusting()) {
                deltaTVal.setText((deltaT.getValue() / 10.0) + " degrees");
            }
        });

        epsilonT.addChangeListener(e -> {
            if (epsilonT.getValueIsAdjusting()) {
                epsilonTVal.setText((epsilonT.getValue() / 10.0) + " units");
            }
        });

        panel.add(new JLabel("Maximum Angular Distortion"));
        //panel.add(deltaT);
        panel.add(deltaT);
        panel.add(deltaTVal);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Maximum Scaling Distortion"));
        //panel.add(epsilonT);
        panel.add(epsilonT);
        panel.add(epsilonTVal);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Must Have At Least"));
        panel.add(lowerBoundT);
        panel.add(new JLabel("points in common"));
        panel.add(new JLabel(""));

        panel.add(showScoreT);
        panel.add(scoreNamesT);
        scoreNamesT.addItem("clique_fraction"); /////////
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));

        uiLoaded = true;
    }

    public JPanel getPanel() {
        return panel;
    }

    public JTextArea getDummy() {
        return dummy;
    }

    public HashMap<String, ImagePlus> getImgMap() {
        return imgmap;
    }

    public JComboBox<String> getQImgs() {
        return Q_imgs;
    }

    public JComboBox<String> getKImgs() {
        return K_imgs;
    }

    public JFormattedTextField getMinRatioT() {
        return minRatioT;
    }

    public JFormattedTextField getMaxRatioT() {
        return maxRatioT;
    }

    public JSlider getDeltaT() { return deltaT; }

    public JSlider getEpsilonT() {
        return epsilonT;
    }

    public JFormattedTextField getLowerBoundT() {
        return lowerBoundT;
    }

    public JLabel getQimgPoints() {
        return Qimg_points;
    }

    public JLabel getKimgPoints() {
        return Kimg_points;
    }

    public JCheckBox getShowScoreT() {
        return showScoreT;
    }

    public JComboBox<String> getScoreNamesT() {
        return scoreNamesT;
    }

    public String getTargZip() {
        return targ_zip;
    }

    public void setTargZip(String str) {
        this.targ_zip = str;
    }

    public boolean isUiLoaded() {
        return uiLoaded;
    }

    public void setUiLoaded(boolean state) {
        this.uiLoaded = state;
    }

}