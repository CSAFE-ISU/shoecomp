import java.awt.image.IndexColorModel;

public class CustomColorModelFactory {
    public static IndexColorModel getModel(int r, int g, int b, int a, int bar) {
        // all pixel values above bar are transparent
        // rest are filled with r,g,b,a
        byte[] red = new byte[256];
        byte[] green = new byte[256];
        byte[] blue = new byte[256];
        byte[] alpha = new byte[256];

        for (int i = 0; i < 256; ++i) {
            red[i] = (byte) r;
            green[i] = (byte) g;
            blue[i] = (byte) b;
            if (i < bar) {
                alpha[i] = (byte) a;
            } else {
                alpha[i] = (byte) 0;
            }
        }

        return new IndexColorModel(8, 256, red, green, blue, alpha);
    }

    public static IndexColorModel getDefaultModel() {
        int r = 0x9f;
        int g = 0x14;
        int b = 0x96;
        int a = 151;
        int bar = 110; // all pixel values above this are transparent
        return getModel(r, g, b, a, bar);
    }

    public static IndexColorModel getModelThreshed(int bar) {
        int r = 0x9f;
        int g = 0x14;
        int b = 0x96;
        int a = 151;
        return getModel(r, g, b, a, bar);
    }
}

