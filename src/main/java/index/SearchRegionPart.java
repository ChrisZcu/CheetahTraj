package index;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.geo.Location;
import model.*;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class SearchRegionPart extends PApplet {
    private static TrajectoryMeta[] trajMetaFull;

    public static TrajectoryMeta[] searchRegion(double minLat, double maxLat, double minLon, double maxLon,
                                                QuadRegion quadRegion, double quality) {
        ArrayList<TrajectoryMeta> trajectories = new ArrayList<>();
        Stack<QuadRegion> regionStack = new Stack<QuadRegion>();
        regionStack.push(quadRegion);

        trajMetaFull = TimeProfileSharedObject.getInstance().trajMetaFull;

        while (!regionStack.isEmpty()) {
            QuadRegion quadRegionHead = regionStack.pop();
            if (isContained(minLat, maxLat, minLon, maxLon, quadRegionHead)) {//全包含
                if (quadRegionHead.getQuadRegionChildren() == null) {
                    // is leaf.
                    addAllSubMetaTraj(trajectories, quadRegionHead, quality);

                    RectRegion rec = new RectRegion();
                    rec.initLoc(new Location(quadRegionHead.getMinLat(), quadRegionHead.getMinLon()),
                            new Location(quadRegionHead.getMaxLat(), quadRegionHead.getMaxLon()));
                    TimeProfileSharedObject.getInstance().searchRegions.add(rec);

                } else {
                    for (QuadRegion quadRegionTmp : quadRegionHead.getQuadRegionChildren()) {
                        regionStack.push(quadRegionTmp);
                    }
                }
            } else {
                //full contain
                if (isFullContain(minLat, maxLat, minLon, maxLon, quadRegionHead)) {
                    addAllSubMetaTraj(trajectories, quadRegionHead, quality);
                    RectRegion rec = new RectRegion();
                    rec.initLoc(new Location(quadRegionHead.getMinLat(), quadRegionHead.getMinLon()),
                            new Location(quadRegionHead.getMaxLat(), quadRegionHead.getMaxLon()));
                    TimeProfileSharedObject.getInstance().searchRegions.add(rec);

                } else if (isInteractive(minLat, maxLat, minLon, maxLon, quadRegionHead)) {
                    //interact
                    if (quadRegionHead.getQuadRegionChildren() == null) {
                        addAllSubMetaTraj(trajectories, quadRegionHead, quality);
                        RectRegion rec = new RectRegion();
                        rec.initLoc(new Location(quadRegionHead.getMinLat(), quadRegionHead.getMinLon()),
                                new Location(quadRegionHead.getMaxLat(), quadRegionHead.getMaxLon()));
                        TimeProfileSharedObject.getInstance().searchRegions.add(rec);

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

    private static void addAllSubMetaTraj(List<TrajectoryMeta> trajectories, QuadRegion quadRegionHead, double quality) {
        for (TrajToSubpart trajToSubpart : quadRegionHead.getTrajToSubparts()) {
            if (trajToSubpart.quality >= quality) {
                break;
            }
            // now just create the part of it and not consider this traj in other cells.
            TrajectoryMeta trajTmp = new TrajectoryMeta(trajToSubpart.getTrajId());
            trajTmp.setBegin(trajToSubpart.getBeginPosIdx());
            trajTmp.setEnd(trajToSubpart.getEndPosIdx());
            trajTmp.setPositions(generatePosList(trajToSubpart).toArray(new Position[0]));
            trajectories.add(trajTmp);
//            trajectories.add(trajMetaFull[trajToSubpart.getTrajId()]);
        }
    }

    private static boolean isContained(double minLat, double maxLat, double minLon, double maxLon, QuadRegion quadRegion) {//quadregion is large
        return minLat >= quadRegion.getMinLat() && maxLat <= quadRegion.getMaxLat()
                && minLon >= quadRegion.getMinLon() && maxLon <= quadRegion.getMaxLon();
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

    private static List<Position> generatePosList(TrajToSubpart trajToSubpart) {
        int trajId = trajToSubpart.getTrajId();
        int begin = trajToSubpart.getBeginPosIdx();
        int end = trajToSubpart.getEndPosIdx();      // notice that the end is included

        return Arrays.asList(trajMetaFull[trajId].getPositions()).subList(begin, end + 1);
    }

    public static void main(String[] args) {
        String partFilePath = "data/GPS/Porto5w/Porto5w.txt";
        String fullFilePath = "data/GPS/porto_full.txt";
        long t0 = System.currentTimeMillis();
        QuadRegion quadRegionRoot = QuadTree.getQuadIndex(fullFilePath, 3);
        System.out.println("index time: " + (System.currentTimeMillis() - t0));

        double minLat = 41.137554, maxLat = 41.198544, minLon = -8.596918, maxLon = -8.677942;

        TrajectoryMeta[] trajectories = searchRegion(minLat, maxLat, minLon, maxLon, quadRegionRoot, 1);

        PApplet.main(new String[]{SearchRegionPart.class.getName()});
    }
}
