package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import index.SearchRegionPart;
import model.Position;
import model.QuadRegion;
import model.RectRegion;
import model.TrajectoryMeta;
import org.lwjgl.Sys;
import org.omg.PortableServer.POA;
import processing.core.PApplet;
import processing.core.PImage;
import util.PSC;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SolutionXTimeProfile extends PApplet {

    UnfoldingMap map;
    int wight = 1000, hight = 800;
    private String fullFile = "data/GPS/porto_full.txt";
    private String partFilePath = "data/GPS/Porto5w/Porto5w.txt";

    private String filePath = fullFile;

    @Override
    public void settings() {
        size(wight, hight, P2D);
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(zoomLevel, centerList[curCenterId]);
        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread() {
            @Override
            public void run() {
                trajFullMeta = QuadTree.loadData(new double[4], filePath);
                System.out.println("trajectory load done");
                TimeProfileSharedObject.getInstance().trajMetaFull = trajFullMeta;
                QuadTree.trajMetaFull = trajFullMeta;

                System.out.println("begin index");
                int quadQuality = 4;
                String quadFilePath = "data/GPS/QuadTreeIndex/quad_tree_quality" + quadQuality + "_info.txt";
                QuadTree.loadTreeFromFile(quadFilePath);
                quadRegionRoot = QuadTree.quadRegionRoot;

                System.out.println("index done");
                isDataTotalLoadDone = true;
            }
        }.start();
    }

    @Override
    public void draw() {
        map.draw();
        if (!(zoomCheck == map.getZoomLevel() && centerCheck.equals(map.getCenter()))) {
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else {
                zoomCheck = map.getZoomLevel();
                centerCheck = map.getCenter();
                System.out.println("load map done");
                map.draw();
                map.draw();
                map.draw();
                map.draw();
            }
        } else {
            if (isDataTotalLoadDone) {
                float x = (float) (500 * regionSize);
                float y = (float) (400 * regionSize);
                rectRegion.setLeftTopLoc(map.getLocation(500 - x, 400 - y));
                rectRegion.setRightBtmLoc(map.getLocation(500 + x, 400 + y));

                double leftLat = rectRegion.getLeftTopLoc().getLat();
                double leftLon = rectRegion.getLeftTopLoc().getLon();
                double rightLon = rectRegion.getRightBtmLoc().getLon();
                double rightLat = rectRegion.getRightBtmLoc().getLat();

                double minLat = Math.min(leftLat, rightLat);
                double maxLat = Math.max(leftLat, rightLat);
                double minLon = Math.min(leftLon, rightLon);
                double maxLon = Math.max(leftLon, rightLon);

                long searchTime = System.currentTimeMillis();
                TrajectoryMeta[] trajShow = SearchRegionPart.searchRegion(minLat, maxLat, minLon, maxLon, quadRegionRoot, 1);
                searchTime = System.currentTimeMillis() - searchTime;
                double[] renderTime = drawTraj(trajShow);
                StringBuilder sb = new StringBuilder();
                sb.append(zoomLevel).append(",").append(curCenterId).append(",").append(regionSize).append(",")
                        .append(trajShow.length).append(",").append(searchTime).append(",").append(renderTime[0]).append(",").append(renderTime[1]).append("\n");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter("data/localRec/solutionX4.txt", true));
                    writer.write(sb.toString());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(sb);
                if (regionSize == 1.0 / 2) {
                    regionSize = 1.0 / 64;
                    if (curCenterId == centerList.length - 1) {
                        curCenterId = 0;
                        if (zoomLevel == 16) {
                            exit();
                        } else {
                            zoomLevel++;
                        }
                    } else {
                        curCenterId++;
                    }
                    map.zoomAndPanTo(zoomLevel, centerList[curCenterId]);
                } else {
                    regionSize *= 2;
                }
//                noLoop();
            }
        }
    }

    @Override
    public void keyPressed() {
        if (key == 'q') {
            loop();
        }
    }

    private double[] drawTraj(TrajectoryMeta[] trajectoryMetas) {
        double[] time = new double[2];

        noFill();
        strokeWeight(1);
        stroke(new Color(255, 0, 0).getRGB());

        ArrayList<ArrayList<Point>> pointTraj = new ArrayList<>();
        long t0 = System.currentTimeMillis();
        for (TrajectoryMeta trajectoryMeta : trajectoryMetas) {
            ArrayList<Point> tmpPointList = new ArrayList<>();
            for (Position pos : generatePosList(trajectoryMeta)) {
                Location loc = new Location(pos.x / 10000.0, pos.y / 10000.0);
                ScreenPosition screenPos = map.getScreenPosition(loc);
                tmpPointList.add(new Point(screenPos.x, screenPos.y));
            }
            pointTraj.add(tmpPointList);
        }
        time[0] = (System.currentTimeMillis() - t0);
        long t1 = System.currentTimeMillis();
        for (ArrayList<Point> points : pointTraj) {
            beginShape();
            for (Point point : points) {
                vertex(point.x, point.y);
            }
            endShape();
        }
        time[1] = (System.currentTimeMillis() - t1);
        return time;
    }

    private List<Position> generatePosList(TrajectoryMeta trajMeta) {
        int trajId = trajMeta.getTrajId();
        int begin = trajMeta.getBegin();
        int end = trajMeta.getEnd();      // notice that the end is included

        return Arrays.asList(trajFullMeta[trajId].getPositions()).subList(begin, end + 1);
    }

    //global variable
    boolean isDataTotalLoadDone = false;
    TrajectoryMeta[] trajFullMeta;
    QuadRegion quadRegionRoot;
    PImage mapImage = null;

    int zoomCheck = -1;
    Location centerCheck = new Location(-1, -1);

    //variable
    Location[] centerList = {
            new Location(41.315205, -8.629877), new Location(41.275997, -8.365519),
            new Location(41.198544, -8.677942), new Location(41.213013, -8.54542),
            new Location(41.1882, -8.35178), new Location(41.137554, -8.596918),
            new Location(41.044403, -8.470575), new Location(40.971338, -8.591425)
    };

    private int zoomLevel = 11;
    private int curCenterId = 0;
    private double regionSize = 1.0 / 64;

    private RectRegion rectRegion = new RectRegion();

    public static void main(String[] args) {
        PApplet.main(new String[]{
                SolutionXTimeProfile.class.getName()
        });
    }

    static class Point {
        float x;
        float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
