import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImageSaver {

  private final ImageSaverGUI gui;
  private JFileChooser chooser;
  private boolean img_valid;
  private boolean markup_valid;

  public ImageSaver() {
    gui = new ImageSaverGUI();
    this.chooser = new JFileChooser();
    loadReactions();
  }

  public static void callFromMacro() {
    ImageSaver x = new ImageSaver();
    x.run("");
  }

  private void loadReactions() {
    Preferences prefs = Preferences.userNodeForPackage(ImageSaver.class);
    gui.getImgs()
        .addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                String item = Objects.requireNonNull(gui.getImgs().getSelectedItem()).toString();
                ImagePlus tmp = gui.getImgMap().get(item);
              }
            });

    gui.getImgSaveButton().addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                String prev = prefs.get("PreviousImageSave", System.getProperty("user.home"));
                chooser = new JFileChooser(prev);
                chooser.setAcceptAllFileFilterUsed(false);

                FileNameExtensionFilter tiffFilter = new FileNameExtensionFilter("TIFF Images", "tif", "tiff");
                FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG Images", "png");
                chooser.addChoosableFileFilter(tiffFilter);
                chooser.addChoosableFileFilter(pngFilter);
                chooser.setFileFilter(tiffFilter); // default

                int returnVal = chooser.showSaveDialog(null);
                if (returnVal != JFileChooser.APPROVE_OPTION) {
                  img_valid = false;
                  return;
                }

                File selectedFile = chooser.getSelectedFile();
                FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) chooser.getFileFilter();
                String[] extensions = selectedFilter.getExtensions();
                String chosenExt = extensions[0].toLowerCase();

                // Remove existing extension and add the correct one
                String baseName = selectedFile.getAbsolutePath().replaceAll("\\.[^.]+$", "");
                String validPath = baseName + "." + chosenExt;

                File file = new File(validPath);
                if (file.exists()) {
                  JOptionPane.showMessageDialog(null, "File Already Exists!");
                }

                gui.getImgPath().setText(validPath);
                img_valid = true;
                prefs.put("PreviousImageSave", file.getParent());
              }
            });



    gui.getMarkupSaveButton()
        .addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {

                String prev =
                    prefs.get("PreviousJSONSave", System.getProperty("user.home"));
                chooser = new JFileChooser(prev);
                String validPath = checkFileSave("json", "txt");
                if (!validPath.endsWith(".json") || !validPath.endsWith(".txt")) {
                  validPath += ".json";
                }
                if (validPath == null || validPath.isEmpty()) {
                  JOptionPane.showMessageDialog(null, "Invalid File!");
                  markup_valid = false;
                } else {
                  File file = new File(validPath);
                  if (file.exists()) {
                    JOptionPane.showMessageDialog(null, "File Already Exists!");
                  }
                  gui.getMarkupPath().setText(validPath);
                  markup_valid = true;
                  String selected = new File(validPath).getParent();
                  if (selected != null) prefs.put("PreviousJSONSave", selected);
                }
              }
            });
  }

  String checkFileSave(String... fileTypes) {
    chooser.setFileFilter(new FileNameExtensionFilter("Allowed types", fileTypes));
    int returnValue = chooser.showSaveDialog(null);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile().getAbsolutePath();
    }
    return null;
  }


  public void run(String arg) {
    int p =
        JOptionPane.showConfirmDialog(
            null, gui.getPanel(), "Save Image and Markup", JOptionPane.OK_CANCEL_OPTION);
    if (p == JOptionPane.CANCEL_OPTION) return;
    if (!img_valid && !markup_valid) return;

    ImagePlus tmp =
        gui.getImgMap().get(Objects.requireNonNull(gui.getImgs().getSelectedItem()).toString());
    if (img_valid) {
      String path = gui.getImgPath().getText();
      String ext = getFileExtension(path).toLowerCase();
      String format;
      switch (ext) {
        case "png":
          format = "png";
          break;
        case "tif":
        case "tiff":
          format = "tiff";
          break;
        default:
          format = "tiff"; // Fallback
      }
      IJ.saveAs(tmp, format, path);
    }

    if (markup_valid) {
      PolygonRoi pol = (PolygonRoi) tmp.getProperty("bounds");
      PointRoi pts = (PointRoi) tmp.getProperty("points");
      if (pol != null && pts != null) {
        MarkupData m = MarkupData.fromROIPair(pol, pts);
        m.toFile(gui.getMarkupPath().getText());
      } else {
        System.out.println("Unable to save markup!!!");
      }
    }
    JOptionPane.showMessageDialog(null, "Save complete.");
  }
  private String getFileExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
      return filename.substring(dotIndex + 1);
    }
    return "";
  }

  private class ImageSaverGUI {
    private final JButton imgSaveButton;
    private final JTextArea imgPath;
    private final JButton markupSaveButton;
    private final JTextArea markupPath;
    private final JPanel panel;
    private final HashMap<String, ImagePlus> imgmap;
    private final JComboBox<String> imgs;
    private final JTextArea dummy;

    private ImageSaverGUI() {
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

      loadUI();
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

    private JButton getImgSaveButton() {
      return imgSaveButton;
    }

    private JTextArea getImgPath() {
      return imgPath;
    }

    private JButton getMarkupSaveButton() {
      return markupSaveButton;
    }

    private JTextArea getMarkupPath() {
      return markupPath;
    }

    private JPanel getPanel() {
      return panel;
    }

    private HashMap<String, ImagePlus> getImgMap() {
      return imgmap;
    }

    private JComboBox<String> getImgs() {
      return imgs;
    }

    private JTextArea getDummy() {
      return dummy;
    }
  }
}
