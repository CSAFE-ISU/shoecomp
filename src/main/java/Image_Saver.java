import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Objects;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Image_Saver {

  private Image_SaverGUI gui;
  private JFileChooser chooser;
  private boolean img_valid;
  private boolean markup_valid;

  public Image_Saver() {
    gui = new Image_SaverGUI();
    this.chooser = new JFileChooser();
    loadReactions();
  }

  public static void callFromMacro() {
    Image_Saver x = new Image_Saver();
    x.run("");
  }

  private void loadReactions() {
    Preferences prefs = Preferences.userNodeForPackage(Image_Saver.class);
    gui.getImgs().addActionListener(
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
            String prev = prefs.get("PreviousImageSave", System.getProperty("user.home").toString());
            chooser = new JFileChooser(prev);
            String validPath = checkFileSave("tiff", "jpg", "png");
            if (!validPath.endsWith(".tiff") && !validPath.endsWith(".tif")
             && !validPath.endsWith(".jpg") && !validPath.endsWith(".png")) {
              validPath += ".tiff";
            }
            if (validPath == null || validPath.isEmpty()) {
              JOptionPane.showMessageDialog(null, "Invalid File!");
              img_valid = false;
            } else {
              gui.getImgPath().setText(validPath);
              img_valid = true;
              File file = new File(validPath);
              if (file.exists()) {
                int save_image = JOptionPane.showConfirmDialog(null, "File Already Exists!\n" +
                        "Do you want to overwrite?", "Alert", JOptionPane.YES_NO_OPTION);
                if (save_image == JOptionPane.NO_OPTION) {
                  img_valid = false;
                  gui.getImgPath().setText("");
                }
              }
              String selected = new File(validPath).getParent();
              prefs.put("PreviousImageSave", selected);
            }
          }
        });

    gui.getMarkupSaveButton().addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {

            String prev = prefs.get("PreviousJSONSave", System.getProperty("user.home").toString());
            chooser = new JFileChooser(prev);
            String validPath = checkFileSave("json", "txt");
            if (!validPath.endsWith(".json") && !validPath.endsWith(".txt")) {
              validPath += ".json";
            }
            if (validPath == null || validPath.isEmpty()) {
              JOptionPane.showMessageDialog(null, "Invalid File!");
              markup_valid = false;
            } else {
              File file = new File(validPath);
              if (file.exists()) {
                int save_markup = JOptionPane.showConfirmDialog(null, "File Already Exists!\n" +
                        "Do you want to overwrite?", "Alert", JOptionPane.YES_NO_OPTION);
                if (save_markup == JOptionPane.NO_OPTION) {
                  markup_valid = false;
                  gui.getMarkupPath().setText("");
                }
              }
              gui.getMarkupPath().setText(validPath);
              markup_valid = true;
              String selected = new File(validPath).getParent();
              if (selected != null)
                prefs.put("PreviousJSONSave", selected);
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
    int p =
        JOptionPane.showConfirmDialog(
            null, gui.getPanel(), "Save Image and Markup", JOptionPane.OK_CANCEL_OPTION);
    if (p != JOptionPane.OK_OPTION) return;
    if (!img_valid && !markup_valid) return;

    ImagePlus tmp = gui.getImgMap().get(Objects.requireNonNull(gui.getImgs().getSelectedItem()).toString());
    if (img_valid) {
      IJ.save(tmp, gui.getImgPath().getText());
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
}
