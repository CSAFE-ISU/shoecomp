import ij.IJ;
import ij.plugin.PlugIn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.*;

class OptionPanel extends JPanel {

  private JButton loadImage;
  private JButton saveMarkup;
  private JButton runAlignment;
  private JButton IJSettings;
  private JButton aboutButton;
  private JButton exitButton;

  public OptionPanel() {
    super();
    loadUI();
    loadReactions();
  }

  private static JButton getButtonMade(Path path, String tip) {
    ImageIcon img = new ImageIcon(path.toAbsolutePath().toString());
    JButton result = new JButton();
    result.setIcon(img);
    result.setToolTipText(tip);
    return result;
  }

  private void loadUI() {
    Path assetDir = Paths.get(IJ.getDirectory("macros"));
    this.setLayout(new GridLayout(2, 3));

    loadImage = getButtonMade(assetDir.resolve("LoadImage.png"), "Load an Image");
    saveMarkup = getButtonMade(assetDir.resolve("SaveMarkup.png"), "Save Image + Markup");
    runAlignment = getButtonMade(assetDir.resolve("RunAlignment.png"), "Align two images");
    IJSettings = getButtonMade(assetDir.resolve("Settings.png"), "ImageJ Settings");
    aboutButton = getButtonMade(assetDir.resolve("About.png"), "About ShoeComp");
    exitButton = getButtonMade(assetDir.resolve("Exit.png"), "Exit ShoeComp");

    this.add(loadImage, "grow");
    this.add(saveMarkup, "grow");
    this.add(runAlignment, "grow");
    this.add(IJSettings, "grow");
    this.add(aboutButton, "grow");
    this.add(exitButton, "grow");
  }

  private void loadReactions() {
    loadImage.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            Image_Loader.callFromMacro();
          }
        });
    saveMarkup.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            Image_Saver.callFromMacro();
          }
        });
    runAlignment.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            Align_Runner.callFromMacro();
          }
        });
    aboutButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            About_Page.callFromMacro();
          }
        });
    exitButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            IJ.run("Quit");
          }
        });

    IJSettings.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            IJ.getInstance().setVisible(!IJ.getInstance().isVisible());
          }
        });
  }
}

public class LandingPage implements PlugIn {

  JFrame mainFrame;

  public static void callFromMacro() {
    LandingPage x = new LandingPage();
    x.run("");
  }

  void loadUI() {
    mainFrame = new JFrame("Main Window");
    mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    OptionPanel mainPanel = new OptionPanel();
    mainFrame.setContentPane(mainPanel);
    mainFrame.setSize(675, 450);
    mainFrame.pack();
  }

  void loadReactions() {}

  public void run(String arg) {
    loadUI();
    loadReactions();
    mainFrame.setVisible(true);
  }

  public void exit() {
    mainFrame.dispose();
    ij.IJ.run("Quit");
  }
}
