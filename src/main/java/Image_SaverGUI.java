import ij.ImagePlus;
import ij.WindowManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class Image_SaverGUI {
  private final JButton imgSaveButton;
  private final JTextArea imgPath;
  private final JButton markupSaveButton;
  private final JTextArea markupPath;
  private final JPanel panel;
  private final HashMap<String, ImagePlus> imgmap;
  private final JComboBox<String> imgs;
  private final JTextArea dummy;

  public Image_SaverGUI() {
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

  public JButton getImgSaveButton() {
    return imgSaveButton;
  }

  public JTextArea getImgPath() {
    return imgPath;
  }

  public JButton getMarkupSaveButton() {
    return markupSaveButton;
  }

  public JTextArea getMarkupPath() {
    return markupPath;
  }

  public JPanel getPanel() {
    return panel;
  }

  public HashMap<String, ImagePlus> getImgMap() {
    return imgmap;
  }

  public JComboBox<String> getImgs() {
    return imgs;
  }

  public JTextArea getDummy() {
    return dummy;
  }
}
