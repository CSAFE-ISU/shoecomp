import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MarkupData {
    float[][] bounds;
    int nbounds;
    float[][] points;
    int npoints;

    public MarkupData(int nbounds, int npoints) {
        this.nbounds = nbounds;
        this.bounds = new float[2][nbounds];
        this.npoints = npoints;
        this.points = new float[2][npoints];
    }

    public static MarkupData fromFile(String filePath) throws IOException {
        JSONObject reader = new JSONObject(new JSONTokener(new FileReader(filePath)));
        JSONArray bounds = (JSONArray) reader.get("bounds");
        JSONArray points = (JSONArray) reader.get("points");
        return MarkupData.fromJSON(bounds, points);
    }

    public static MarkupData fromJSON(JSONArray bounds, JSONArray points) {
        MarkupData res = new MarkupData(bounds.length(), points.length());
        int i;
        String z;
        /* there should be a better type-cast here, come on */
        for (i = 0; i < res.nbounds; ++i) {
            z = ((JSONArray) (bounds.get(i))).get(0).toString();
            res.bounds[0][i] = Float.parseFloat(z);
            z = ((JSONArray) (bounds.get(i))).get(1).toString();
            res.bounds[1][i] = Float.parseFloat(z);
        }
        for (i = 0; i < res.npoints; ++i) {
            z = ((JSONArray) (points.get(i))).get(0).toString();
            res.points[0][i] = Float.parseFloat(z);
            z = ((JSONArray) (points.get(i))).get(1).toString();
            res.points[1][i] = Float.parseFloat(z);
        }
        return res;
    }

    public static MarkupData fromROIPair(PolygonRoi bounds, PointRoi points) {
        FloatPolygon b = bounds.getFloatPolygon();
        FloatPolygon p = points.getFloatPolygon();
        MarkupData res = new MarkupData(b.npoints, p.npoints);
        int i;
        for (i = 0; i < res.nbounds; ++i) {
            res.bounds[0][i] = b.xpoints[i];
            res.bounds[1][i] = b.ypoints[i];
        }
        for (i = 0; i < res.npoints; ++i) {
            res.points[0][i] = p.xpoints[i];
            res.points[1][i] = p.ypoints[i];
        }
        return res;
    }

    public PolygonRoi getBoundsAsRoi() {
        return new PolygonRoi(this.bounds[0], this.bounds[1], this.nbounds, Roi.POLYGON);
    }

    public PointRoi getPointsAsRoi() {
        return new PointRoi(this.points[0], this.points[1], this.npoints);
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();

        JSONArray bounds_JSON = new JSONArray();
        JSONArray points_JSON = new JSONArray();
        for (int i = 0; i < this.nbounds; ++i) {
            JSONArray tmp = new JSONArray();
            tmp.put(bounds[0][i]);
            tmp.put(bounds[1][i]);
            bounds_JSON.put(tmp);
        }
        for (int i = 0; i < this.npoints; ++i) {
            JSONArray tmp = new JSONArray();
            tmp.put(points[0][i]);
            tmp.put(points[1][i]);
            points_JSON.put(tmp);
        }

        jsonObject.put("bounds", bounds_JSON);
        jsonObject.put("points", points_JSON);
        return jsonObject;
    }

    public void toFile(String filePath) {
        JSONObject j = this.toJSON();
        try {
            FileWriter file = new FileWriter(filePath);
            file.write(j.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
