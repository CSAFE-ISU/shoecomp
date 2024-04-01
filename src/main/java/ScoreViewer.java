import ij.ImagePlus;
import ij.gui.Plot;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/* TODO: have a generalized approach for different scores */

public class ScoreViewer {

    static Color MATCH_COLOR = new Color(255, 0, 0, 77);
    static Color NONMATCH_COLOR = new Color(0, 0, 255, 77);
    double[] match_scores;
    double[] nonmatch_scores;
    String name;

    public ScoreViewer(double[] ms, double[] ns, String name) {
        this.match_scores = new double[ms.length];
        this.nonmatch_scores = new double[ns.length];
        this.name = name;
        System.arraycopy(ms, 0, match_scores, 0, match_scores.length);
        System.arraycopy(ns, 0, nonmatch_scores, 0, nonmatch_scores.length);
    }

    public static ScoreViewer fromJSON(String filename) throws FileNotFoundException {
        double[] ms;
        double[] ns;
        String name;
        JSONObject reader = new JSONObject(new JSONTokener(new FileReader(filename)));

        JSONArray ms0 = reader.getJSONArray("match_scores");
        JSONArray ns0 = reader.getJSONArray("nonmatch_scores");
        name = reader.getString("name");

        ms = new double[ms0.length()];
        for (int i = 0; i < ms.length; ++i) {
            ms[i] = ms0.getDouble(i);
        }
        ns = new double[ns0.length()];
        for (int i = 0; i < ns.length; ++i) {
            ns[i] = ns0.getDouble(i);
        }
        return new ScoreViewer(ms, ns, name);
    }

    public static ScoreViewer fromJSONInFolder(String scoreName) throws FileNotFoundException {
        Path fpath = Paths.get(ij.IJ.getDirectory("macros"), scoreName + ".json");
        return ScoreViewer.fromJSON(fpath.toAbsolutePath().toString());
    }

    public ImagePlus showScoreWithReference(double score) {
        Plot p = new Plot(name + " Score For Comparison", "score", "frequency");
        p.setLineWidth(1.5F);
        p.setBackgroundColor(Color.WHITE);
        p.setColor(MATCH_COLOR);
        p.setLimits(0, 1, 0, match_scores.length + nonmatch_scores.length);
        p.addHistogram(this.match_scores, 0.1, 0.05);
        p.setColor(NONMATCH_COLOR);
        p.addHistogram(this.nonmatch_scores, 0.1, 0.05);

        p.setColor(Color.BLACK, null);
        p.addLegend("match\tnonmatch");
        double[] lims = p.getLimits(); /* xmin, xmax, ymin, ymax */
        p.setLineWidth(2.3F);
        p.drawDottedLine(score, lims[2], score, lims[3], 10);
        p.addLabel((score - lims[0]) / (lims[1] - lims[0]), 0.35, String.format("You are here:\n\t%.3f", score));
        p.setScale(2.5F);
        return p.getImagePlus();
    }
}
