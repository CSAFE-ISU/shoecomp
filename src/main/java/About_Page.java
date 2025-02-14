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
        this.dummy.setMaximumSize(new Dimension(400, 600));
        JLabel source = new JLabel("<html><a href='https://github.com/CSAFE-ISU/shoecomp' style='text-align: center;'>" +
                "https://github.com/CSAFE-ISU/shoecomp" + "</a></html>", JLabel.CENTER);
        panel.add(source);
        dummy.setText("ShoeComp is an ImageJ plugin written at the \n" +
                "Center for Statistics and Applications in Forensic Evidence (CSAFE), \n" +
                "for the markup and alignment of shoe-print images. We are grateful to \n" +
                "the maintainers of ImageJ, FIJI, and associated plugins.");
        this.dummy.setEditable(false);
        panel.add(dummy);
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
