import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
public class About_Page {
    private JPanel panel;
    private JTextArea dummy;
    public About_Page() {
        this.panel = new JPanel(new GridLayout(2, 1));
        this.dummy = new JTextArea();
        panel.add(dummy);
        dummy.setText("ShoeComp is by CSAFE.\n\nThank you ImageJ and associated plugins!");
    }

    public void run(String arg) {
        JOptionPane.showMessageDialog(null, panel, "About ShoeComp",
                JOptionPane.INFORMATION_MESSAGE);
    }
    public static void callFromMacro() {
        About_Page x = new About_Page();
        x.run("");
    }
}
