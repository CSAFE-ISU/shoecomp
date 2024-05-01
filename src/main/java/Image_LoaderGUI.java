import ij.ImagePlus;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.prefs.Preferences;

public class Image_LoaderGUI {

    private final JButton imgLoadButton;
    private final JTextArea imgPath;
    private final JButton markupLoadButton;
    private final JTextArea markupPath;
    private final JPanel panel;
    ImagePlus img;

    public Image_LoaderGUI() {
        this.panel = new JPanel(new GridLayout(3, 2));

        this.imgLoadButton = new JButton();
        imgLoadButton.setText("Load Image...");
        this.imgPath = new JTextArea();
        imgPath.setEditable(false);
        imgPath.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        this.markupLoadButton = new JButton();
        markupLoadButton.setText("Markup File Location...");
        this.markupPath = new JTextArea();
        markupPath.setEditable(false);
        markupPath.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        this.img = null;
        this.loadUI();

    }

    private void loadUI() {
        panel.add(new JLabel());
        panel.add(new JLabel());
        panel.add(imgLoadButton);
        panel.add(imgPath);
        panel.add(markupLoadButton);
        panel.add(markupPath);
        markupPath.setEnabled(false);
    }

    public JButton getImgLoadButton() {
        return imgLoadButton;
    }

    public JTextArea getImgPath() {
        return imgPath;
    }

    public JButton getMarkupLoadButton() {
        return markupLoadButton;
    }

    public JTextArea getMarkupPath() {
        return markupPath;
    }

    public JPanel getPanel() {
        return panel;
    }

    public ImagePlus getImg() {
        return img;
    }

    public void setImg(ImagePlus img) {
        this.img = img;
    }

}