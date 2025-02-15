import ij.ImagePlus;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/* TODO: have a generalized approach for different scores */

public class ScoreViewer {

  static final float[] dash1 = {10.0f};
  static final BasicStroke dashed =
      new BasicStroke(2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
  static Color MATCH_COLOR = new Color(0xf8, 0x5d, 0x19, 0xff);
  static Color NONMATCH_COLOR = new Color(0x13, 0x83, 0xbd, 0xff);
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
    HistogramDataset ds = new HistogramDataset();
    ds.setType(HistogramType.RELATIVE_FREQUENCY);
    ds.addSeries("match", match_scores, 25, 0.0, 1.0);
    ds.addSeries("nonmatch", nonmatch_scores, 25, 0.0, 1.0);
    JFreeChart chart =
        ChartFactory.createHistogram(
            "SCORE NOT BASED ON REFERENCE DATABASE",
            "similarity score",
            "relative frequency",
            ds,
            PlotOrientation.VERTICAL,
            true,
            false,
            false);
    XYPlot plot = (XYPlot) chart.getPlot();
    XYTextAnnotation anno =
        new XYTextAnnotation(
            String.format("You are here:\n\t%.3f", score),
            score,
            plot.getRangeAxis().getUpperBound() * 0.75);
    anno.setFont(new Font("anno", Font.PLAIN, 16));
    ValueMarker xmark = new ValueMarker(score);

    xmark.setPaint(Color.BLACK);
    xmark.setStroke(dashed);
    plot.setForegroundAlpha(0.75F);
    plot.setBackgroundAlpha(0.0F);
    plot.addDomainMarker(xmark);
    plot.addAnnotation(anno);
    ImagePlus z = new ImagePlus("Score Histogram", chart.createBufferedImage(960, 720));
    return z;
  }
}
