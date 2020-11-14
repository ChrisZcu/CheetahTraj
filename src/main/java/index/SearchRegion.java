package index;

import model.QuadRegion;
import model.TrajToQuality;
import model.TrajectoryMeta;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Stack;

public class SearchRegion extends PApplet {
    public static TrajectoryMeta[] searchRegion(double minLat, double maxLat, double minLon, double maxLon,
                                            QuadRegion quadRegion, double quality) {
        ArrayList<TrajectoryMeta> trajectories = new ArrayList<>();
        Stack<QuadRegion> regionStack = new Stack<QuadRegion>();
        regionStack.push(quadRegion);
        while (!regionStack.isEmpty()) {
            QuadRegion quadRegionHead = regionStack.pop();
            if (isContained(minLat, maxLat, minLon, maxLon, quadRegionHead)) {//全包含
                if (quadRegionHead.getQuadRegionChildren() == null) {
                    for (TrajToQuality trajToQuality : quadRegionHead.getTrajQuality()) {
                        if (trajToQuality.getQuality() < quality)
                            trajectories.add(trajToQuality.getTrajectory());
                        else
                            break;
                    }
                } else {
                    for (QuadRegion quadRegionTmp : quadRegionHead.getQuadRegionChildren()) {
                        regionStack.push(quadRegionTmp);
                    }
                }
            } else {
                //full contain
                if (isFullContain(minLat, maxLat, minLon, maxLon, quadRegionHead)) {
                    for (TrajToQuality trajToQuality : quadRegionHead.getTrajQuality()) {
                        trajectories.add(trajToQuality.getTrajectory());
                    }
                } else if (isInteractive(minLat, maxLat, minLon, maxLon, quadRegionHead)) {
                    //interact
                    if (quadRegionHead.getQuadRegionChildren() == null) {
                        for (TrajToQuality trajToQuality : quadRegionHead.getTrajQuality()) {
                            trajectories.add(trajToQuality.getTrajectory());
                        }
                    } else {
                        for (QuadRegion quadRegionTmp : quadRegionHead.getQuadRegionChildren()) {
                            regionStack.push(quadRegionTmp);
                        }
                    }
                }
            }
        }
        return trajectories.toArray(new TrajectoryMeta[0]);
    }

    private static boolean isContained(double minLat, double maxLat, double minLon, double maxLon, QuadRegion quadRegion) {//quadregion is large
        return minLat > quadRegion.getMinLat() && maxLat < quadRegion.getMaxLat() && minLon > quadRegion.getMinLon() && maxLon < quadRegion.getMaxLon();
    }

    private static boolean isFullContain(double minLat, double maxLat, double minLon, double maxLon, QuadRegion
            quadRegion) {//quadregion is small
        return minLat <= quadRegion.getMinLat() && maxLat >= quadRegion.getMaxLat()
                && minLon <= quadRegion.getMinLon() && maxLon >= quadRegion.getMaxLon();
    }

    private static boolean isInteractive(double minLat, double maxLat, double minLon, double maxLon, QuadRegion
            quadRegion) {
        double minx = Math.max(minLat, quadRegion.getMinLat());
        double miny = Math.max(minLon, quadRegion.getMinLon());
        double maxx = Math.min(maxLat, quadRegion.getMaxLat());
        double maxy = Math.min(maxLon, quadRegion.getMaxLon());
        return !(minx > maxx || miny > maxy);
    }

    public static void main(String[] args) {
        String partFilePath = "data/GPS/Porto5w/Porto5w.txt";
        String fullFilePath = "data/GPS/porto_full.txt";
        long t0 = System.currentTimeMillis();
        QuadRegion quadRegionRoot = QuadTree.getQuadIndex(fullFilePath, 3);
        System.out.println("index time: " + (System.currentTimeMillis() - t0));

        double minLat = 41.137554, maxLat = 41.198544, minLon = -8.596918, maxLon = -8.677942;

        TrajectoryMeta[] trajectories = searchRegion(minLat, maxLat, minLon, maxLon, quadRegionRoot, 1);

        PApplet.main(new String[]{SearchRegion.class.getName()});
    }
}
