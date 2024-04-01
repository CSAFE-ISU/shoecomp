import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public class Image_Saver {

    private final JButton imgSaveButton;
    private final JTextArea imgPath;
    private final JButton markupSaveButton;
    private final JTextArea markupPath;
    private final JPanel panel;
    private final HashMap<String, ImagePlus> imgmap;
    private final JComboBox<String> imgs;
    private final JTextArea dummy;
    private final JFileChooser chooser;

    private boolean img_valid;
    private boolean markup_valid;

    public Image_Saver() {
        this.panel = new JPanel(new GridLayout(6, 2));
        this.dummy = new JTextArea();
        dummy.setText("Save Image + Markup");
        dummy.setEditable(false);

        this.imgmap = new HashMap<>();
        this.imgs = new JComboBox<>();

        this.imgSaveButton = new JButton("Save Image to:");
        this.imgPath = new JTextArea();
        imgPath.setEditable(false);
        imgPath.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        this.markupSaveButton = new JButton();
        markupSaveButton.setText("Save Markup To:");
        this.markupPath = new JTextArea();
        markupPath.setEditable(false);
        markupPath.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        this.chooser = new JFileChooser();

        loadUI();
        loadReactions();
    }

    public static void callFromMacro() {
        Image_Saver x = new Image_Saver();
        x.run("");
    }

    private void loadUI() {
        panel.add(dummy);
        panel.add(new JLabel());

        int[] idList = WindowManager.getIDList();
        if (idList == null || idList.length == 0) {
            dummy.setText("no Images to save!");
            return;
        }
        ImagePlus tmp;
        for (int id : idList) {
            tmp = WindowManager.getImage(id);
            imgmap.put(tmp.getShortTitle(), tmp);
            imgs.addItem(tmp.getShortTitle());
        }

        panel.add(new JLabel("Select Image:"));
        panel.add(imgs);
        panel.add(imgSaveButton);
        panel.add(imgPath);
        panel.add(new JLabel());
        panel.add(new JLabel());
        panel.add(markupSaveButton);
        panel.add(markupPath);

        markupPath.setEnabled(false);
    }

    private void loadReactions() {
        imgs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String item = Objects.requireNonNull(imgs.getSelectedItem()).toString();
                ImagePlus tmp = imgmap.get(item);
            }
        });

        imgSaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String validPath = checkFileSave("tiff", "jpg", "png");
                if (validPath == null || validPath.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Invalid File!");
                    img_valid = false;
                } else {
                    imgPath.setText(validPath);
                    img_valid = true;
                }
            }
        });

        markupSaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String validPath = checkFileSave("json", "txt");
                if (validPath == null || validPath.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Invalid File!");
                    markup_valid = false;
                } else {
                    markupPath.setText(validPath);
                    markup_valid = true;
                }
            }
        });
    }

    String checkFileSave(String... fileTypes) {
        chooser.setFileFilter(new FileNameExtensionFilter("*.*", fileTypes));
        int returnValue = chooser.showSaveDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            return file.getAbsolutePath();
        }
        /* TODO:
            show some sort of progress when saving,
            and close the frame at the end
         */
        return "";
    }

    public void run(String arg) {
        int p = JOptionPane.showConfirmDialog(null, this.panel,
                "Save Image and Markup", JOptionPane.OK_CANCEL_OPTION);
        if (p == JOptionPane.CANCEL_OPTION) return;
        if (!img_valid) return;

        ImagePlus tmp = imgmap.get(Objects.requireNonNull(imgs.getSelectedItem()).toString());
        IJ.save(tmp, imgPath.getText());
        if (markup_valid) {
            PolygonRoi pol = (PolygonRoi) tmp.getProperty("bounds");
            PointRoi pts = (PointRoi) tmp.getProperty("points");
            if (pol != null && pts != null) {
                MarkupData m = MarkupData.fromROIPair(pol, pts);
                m.toFile(markupPath.getText());
            } else {
                System.out.println("Unable to save markup!!!");
            }
        }
    }

}
