import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.prefs.Preferences;

public class GUIController {

    public static void initImageLoader(JButton imgLoadButton) {
        imgLoadButton.setText("Load Image...");


    }

    private void loadReactions(JButton imgLoadButton, JButton markupLoadButton) {
        Preferences prefs = Preferences.userNodeForPackage(Image_Loader.class);
        markupLoadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String prev = prefs.get("PreviousJSONLoad", System.getProperty("user.home").toString());
                chooser = new JFileChooser(prev);
                String validPath = checkFileLoad("json", "txt");
                if (validPath == null || validPath.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Invalid File!");
                    markup_valid = false;
                } else {
                    markupPath.setText(validPath);
                    markup_valid = true;
                    String selected = new File(validPath).getParent();
                    prefs.put("PreviousJSONLoad", selected);
                }
            }
        });

        imgLoadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String prev = prefs.get("PreviousImageLoad", System.getProperty("user.home").toString());
                chooser = new JFileChooser(prev);
                String validPath = checkFileLoad("tiff", "jpg", "png");
                if (validPath == null || validPath.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Invalid File!");
                    img_valid = false;
                } else {
                    imgPath.setText(validPath);
                    img_valid = true;
                    String selected = new File(validPath).getParent();
                    prefs.put("PreviousImageLoad", selected);
                }
            }
        });

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

}
