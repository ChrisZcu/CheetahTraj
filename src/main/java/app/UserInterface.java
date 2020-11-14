package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import draw.TrajDrawManager;
import draw.TrajDrawManagerSingleMap;
import draw.TrajDrawWorkerSingleMap;
import model.*;
import org.lwjgl.Sys;
import processing.core.PApplet;
import processing.core.PGraphics;
import select.TimeProfileManager;
import util.PSC;
import util.VFGS;
import util.VfgsGps;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.jar.JarOutputStream;

public class UserInterface extends PApplet {

    String partTrajFilePath = "data/GPS/Porto5w/Porto5w.txt";
    String totalFilePath = "data/GPS/porto_full.txt";

    String filePath = totalFilePath;
    UnfoldingMap map;
    UnfoldingMap mapClone;

    private boolean regionDrawing = false;

    @Override
    public void settings() {
        size(1000, 800, P2D);
    }

    private int ZOOMLEVEL = 14;
    private Location PRESENT = new Location(41.315205, -8.629877)/*new Location(41.151, -8.634)*//*new Location(41.206, -8.627)*/;
    private boolean loadDone = false;

    @Override
    public void setup() {

        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);

        mapClone = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapClone.setZoomRange(0, 20);
        mapClone.setBackgroundColor(255);
        mapClone.zoomAndPanTo(ZOOMLEVEL, PRESENT);

        MapUtils.createDefaultEventDispatcher(this, map);


        initButton();

        new Thread() {
            @Override
            public void run() {
                loadTotalData(filePath);
                loadDone = true;
                System.out.println("data done " + totalTrajector.length);
            }
        }.start();
    }

    private double regionSize = 1.0 / 2;
    private int regionId = 0;
    private int alg = 2;
    boolean cleanTime = true;
    int zoomLevel = -1;
    Location loc = new Location(-1, -1);
    boolean totalLoad = false;

    @Override
    public void draw() {
        totalLoad = totalLoad && ((map.getZoomLevel() == zoomLevel && loc.equals(map.getCenter())));
        if (!totalLoad) {
            if (!map.allTilesLoaded()) {
                map.draw();
            } else {
                totalLoad = true;
                zoomLevel = map.getZoomLevel();
                loc = map.getCenter();
            }
        } else {
            map.draw();
            StringBuilder sb = new StringBuilder();
            if (regionDrawing) {
                rectRegion = getSelectRegion(lastClick);
            }
            if (autoTimeProfile && loadDone) {
                rectRegion = new RectRegion();
                sb.append(alg).append(",").append(regionSize).append(",").append(regionId).append(",");
                String title = sb.toString();

                float x = (float) (500 * regionSize);
                float y = (float) (400 * regionSize);
                rectRegion.setLeftTopLoc(map.getLocation(500 - x, 400 - y));
                rectRegion.setRightBtmLoc(map.getLocation(500 + x, 400 + y));

                long t0 = System.currentTimeMillis();
                startCalWayPoint();
                long wayPointTime = System.currentTimeMillis() - t0;

                trajShow = TimeProfileSharedObject.getInstance().trajMetaRes[0];
                drawTrajCPU(sb);
                drawRecRegion();
                sb.append(wayPointTime).append(",");
                System.out.println(">>>waypoint size: " + trajShow.length);
                saveFrame("data/picture/20201006/waypoint/" + title + "_" +
                        trajShow.length + "_waypoint.png");
                ArrayList<TrajectoryMeta> trajShows = new ArrayList<>();
                for (TrajectoryMeta[] trajList : TimeProfileSharedObject.getInstance().trajMetaRes) {
                    Collections.addAll(trajShows, trajList);
                }


//                System.out.println(rectRegion.getLeftTopLoc().getLat() + ", " + rectRegion.getLeftTopLoc().getLon() + ", " +
//                        (-rectRegion.getRightBtmLoc().getLat() + rectRegion.getLeftTopLoc().getLat()) + ", " +
//                        (-rectRegion.getLeftTopLoc().getLon() + rectRegion.getRightBtmLoc().getLon()) + ", "
//                        + ((-rectRegion.getRightBtmLoc().getLat() + rectRegion.getLeftTopLoc().getLat()) / 0.000001));
                if (alg == 2) {
                    TimeProfileSharedObject.getInstance().trajectoryMetas = VfgsGps.getVfgs(trajShows.toArray(new TrajectoryMeta[0]), 0.01, delta,
                            rectRegion.getRightBtmLoc().getLat(), rectRegion.getLeftTopLoc().getLon(), 0.0001, 0.0001, sb, mapClone);
//                    Trajectory[] trajTemp = translateTrajArr(trajShows.toArray(new TrajectoryMeta[0]));
//                    Trajectory[] vfgsRes = VFGS.getCellCover(trajTemp, map, 0.01, delta);
//                    System.out.println("vfgs select size: " + vfgsRes.length);
//                    TimeProfileSharedObject.getInstance().trajectoryMetas = translateTrajArr(vfgsRes);
                } else if (alg == 1) {
                    TimeProfileSharedObject.getInstance().trajectoryMetas = VfgsGps.getVfgs(trajShows.toArray(new TrajectoryMeta[0]), 0.01, 0,
                            rectRegion.getRightBtmLoc().getLat(), rectRegion.getLeftTopLoc().getLon(), 0.0001, 0.0001, sb, mapClone);
//                    Trajectory[] trajTemp = translateTrajArr(trajShows.toArray(new TrajectoryMeta[0]));
//                    Trajectory[] vfgsRes = VFGS.getCellCover(trajTemp, map, 0.01, 0);
//                    TimeProfileSharedObject.getInstance().trajectoryMetas = translateTrajArr(vfgsRes);
                } else {
                    TimeProfileSharedObject.getInstance().trajectoryMetas = getRandom(trajShows.toArray(new TrajectoryMeta[0]), 0.01);
                }
                TimeProfileSharedObject.getInstance().calDone = true;
                trajShow = TimeProfileSharedObject.getInstance().trajectoryMetas;

                map.draw();
                drawTrajCPU(sb);
                drawRecRegion();

                System.out.println(sb.toString());
                saveFrame("data/picture/20201006/vfgs+32/" + title + "_vfgs_32.png");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter("data/localRec/TimeProfile20201007.txt", true));
                    writer.write(sb.toString());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (regionSize == 1.0 / 2) {
                    regionSize = 1.0 / 2;
                    if (regionId == rectRegionLoc.length - 1) {
                        regionId = 0;
                        if (alg == 2) {
                            System.out.println("-----------------------------------done-----------------------------------");
                            exit();
                        } else {
                            alg++;
                        }
                    } else {
                        regionId++;
                    }
                } else {
                    regionSize *= 2;
                }
                map.zoomAndPanTo(ZOOMLEVEL, rectRegionLoc[regionId]);

            } else {
                drawRecRegion();
                if (TimeProfileSharedObject.getInstance().calDone) {
                    trajShow = TimeProfileSharedObject.getInstance().trajectoryMetas;

                    TrajDrawManagerSingleMap trajManager = new TrajDrawManagerSingleMap(trajShow, 1, this, map);
                    trajManager.startDraw();
                    TimeProfileSharedObject.getInstance().calDone = false;
                    System.out.println(">>>>way point time: " + wayPointCalTime +
                            " ms\n" + ">>>>vfgs cal time: " + vfgsTime + " ms");
                }
                //draw traj
                long t3 = System.currentTimeMillis();
                drawTime = System.currentTimeMillis() - t3;

                drawTrajCPU();
                drawComponent();
            }
        }
    }

    /**
     * Translate {@link TrajectoryMeta} to {@link Trajectory}
     */
    private Trajectory[] translateTrajArr(TrajectoryMeta[] trajListMeta) {
        Trajectory[] ret = new Trajectory[trajListMeta.length];
        int idx = 0;
        for (TrajectoryMeta trajMt : trajListMeta) {
            Trajectory traj = new Trajectory(trajMt.getTrajId());
            GpsPosition[] gpsPositions = trajMt.getGpsPositions();
            Location[] locations = new Location[gpsPositions.length];
            int posIdx = 0;
            for (GpsPosition gpsPosition : gpsPositions) {
                locations[posIdx++] = new Location(gpsPosition.lat, gpsPosition.lon);
            }
            traj.setLocations(locations);
            ret[idx++] = traj;
        }
        return ret;
    }

    /**
     * Translate {@link Trajectory} to {@link TrajectoryMeta}
     */
    private TrajectoryMeta[] translateTrajArr(Trajectory[] trajList) {
        TrajectoryMeta[] ret = new TrajectoryMeta[trajList.length];
        int idx = 0;
        for (Trajectory traj : trajList) {
            TrajectoryMeta trajMt = new TrajectoryMeta(traj.getTrajId());
            Location[] locations = traj.locations;
            GpsPosition[] gpsPositions = new GpsPosition[locations.length];
            int posIdx = 0;
            for (Location loc : locations) {
                gpsPositions[posIdx++] = new GpsPosition(loc.y, loc.x);
            }
            trajMt.setGpsPositions(gpsPositions);
            ret[idx++] = trajMt;
        }
        return ret;
    }

    TrajectoryMeta[] trajShow = new TrajectoryMeta[0];
    RectRegion rectRegion;
    Position lastClick;

    private TrajectoryMeta[] getRandom(TrajectoryMeta[] trajectoryMeta, double rate) {
        TrajectoryMeta[] res = new TrajectoryMeta[(int) (trajectoryMeta.length * rate)];
        Random random = new Random(0);
        for (int i = 0; i < (int) (trajectoryMeta.length * rate); i++) {
            int id = random.nextInt((int) (trajectoryMeta.length * rate));
            res[i] = trajectoryMeta[id];
        }
        return res;
    }

    @Override
    public void mousePressed() {
        if (mouseButton == RIGHT) {
            regionDrawing = true;
            TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
            lastClick = new Position(mouseX, mouseY);
        } else {
            Location loc = map.getLocation(mouseX, mouseY);
            System.out.println(loc);
            buttonClickListener();
        }
    }

    @Override
    public void mouseReleased() {
        if (regionDrawing) {
            regionDrawing = false;
            rectRegion = getSelectRegion(lastClick);
        }
        if (panning || zoom) {
            panning = false;
            zoom = false;
            finishClick();
        }

    }

    private boolean panning = false;

    private Location[] rectRegionLoc = {new Location(41.315205, -8.629877), new Location(41.275997, -8.365519),
            new Location(41.198544, -8.677942), new Location(41.213013, -8.54542),
            new Location(41.1882, -8.35178), new Location(41.137554, -8.596918),
            new Location(41.044403, -8.470575), new Location(40.971338, -8.591425)};

    private boolean autoTimeProfile = true;

    @Override
    public void mouseDragged() {
        if (mouseButton == LEFT) {
            panning = true;

            TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
        }
    }

    boolean zoom = false;

    @Override
    public void mouseWheel() {
        zoom = true;
        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
    }

    private long wayPointCalTime = 0L;
    private long vfgsTime = 0L;
    private long drawTime = 0L;
    private int delta = 32;

    private void finishClick() {
        if (!loadDone) {
            System.out.println("!!!!!!Data not done, wait....");
            return;
        }
        if (!autoTimeProfile) {
            System.out.println(1);
//            TimeProfileSharedObject.getInstance().trajectoryMetas = totalTrajector;
            TimeProfileSharedObject.getInstance().trajectoryMetas = VfgsGps.getVfgs(totalTrajector, 0.01, delta,
                    minLat, minLon, 0.0001, 0.0001, new StringBuilder(), mapClone);
            TimeProfileSharedObject.getInstance().calDone = true;
            return;
        }
        new Thread() {
            @Override
            public void run() {
                System.out.println("calculating....");
                long t0 = System.currentTimeMillis();
                startCalWayPoint();
                wayPointCalTime = System.currentTimeMillis() - t0;
                long t1 = System.currentTimeMillis();
                ArrayList<TrajectoryMeta> trajShows = new ArrayList<>();
                for (TrajectoryMeta[] trajList : TimeProfileSharedObject.getInstance().trajMetaRes) {
                    Collections.addAll(trajShows, trajList);
                }
//                System.out.println(rectRegion.getLeftTopLoc().getLat() + ", " + rectRegion.getLeftTopLoc().getLon() + ", " +
//                        (-rectRegion.getRightBtmLoc().getLat() + rectRegion.getLeftTopLoc().getLat()) + ", " +
//                        (-rectRegion.getLeftTopLoc().getLon() + rectRegion.getRightBtmLoc().getLon()) + ", "
//                        + ((-rectRegion.getRightBtmLoc().getLat() + rectRegion.getLeftTopLoc().getLat()) / 0.000001));
                TimeProfileSharedObject.getInstance().trajectoryMetas = VfgsGps.getVfgs(trajShows.toArray(new TrajectoryMeta[0]), 0.01, delta,
                        rectRegion.getRightBtmLoc().getLat(), rectRegion.getLeftTopLoc().getLon(), 0.000001, 0.000001, new StringBuilder(), mapClone);
                TimeProfileSharedObject.getInstance().calDone = true;
                vfgsTime = System.currentTimeMillis() - t1;
            }
        }.start();
    }

    private void drawRecRegion() {
        if (rectRegion == null)
            return;
        noFill();
        strokeWeight(2);
        stroke(new Color(19, 149, 186).getRGB());

        ScreenPosition src1 = map.getScreenPosition(rectRegion.getLeftTopLoc());
        ScreenPosition src2 = map.getScreenPosition(rectRegion.getRightBtmLoc());
        rect(src1.x, src1.y, src2.x - src1.x, src2.y - src1.y);
    }

    private void drawTrajCPU() {
        if (TimeProfileSharedObject.getInstance().trajImageMtx == null) {
            return;
        }
        for (PGraphics pg : TimeProfileSharedObject.getInstance().trajImageMtx) {
            if (pg == null) {
                continue;
            }
            image(pg, 0, 0);
        }
        if (TimeProfileSharedObject.getInstance().drawDone) {
            saveFrame("data/picture/VfgsDelta" + delta + ".png");
            noLoop();
        }
    }

    private void drawTrajCPU(StringBuilder sb) {
        noFill();
        strokeWeight(1);
        stroke(new Color(190, 46, 29).getRGB());
        long t0 = System.currentTimeMillis();
        ArrayList<ArrayList<TrajDrawWorkerSingleMap.Point>> trajPointList = new ArrayList<>();
        for (int i = 0; i < trajShow.length; i++) {
            ArrayList<TrajDrawWorkerSingleMap.Point> pointList = new ArrayList<>();
            for (GpsPosition gpsPosition : trajShow[i].getGpsPositions()) {
                Location loc = new Location(gpsPosition.lat, gpsPosition.lon);
                ScreenPosition pos = map.getScreenPosition(loc);
                pointList.add(new TrajDrawWorkerSingleMap.Point(pos.x, pos.y));
            }
            trajPointList.add(pointList);
        }

        for (ArrayList<TrajDrawWorkerSingleMap.Point> traj : trajPointList) {
            beginShape();
            for (TrajDrawWorkerSingleMap.Point pos : traj) {
                vertex(pos.x, pos.y);
            }
            endShape();
        }

        sb.append((System.currentTimeMillis() - t0)).append(",");
        sb.append((trajShow.length)).append("\n");
        System.out.println(">>>>render time: " + (System.currentTimeMillis() - t0) + " ms");
    }

    EleButton[] dataButtonList = new EleButton[0];

    private void initButton() {
        dataButtonList = new EleButton[2];
        int dataButtonXOff = 4;
        int dataButtonYOff = 4;
        dataButtonList[0] = new EleButton(dataButtonXOff, dataButtonYOff + 5, 70, 20, 0, "Finish");
        dataButtonList[1] = new EleButton(dataButtonXOff, dataButtonYOff + 35, 70, 20, 1, "All");

    }

    private void drawComponent() {
        for (EleButton eleButton : dataButtonList) {
            eleButton.render(this);
        }
    }

    boolean finishClick = false;

    private void buttonClickListener() {
        // not in one map mode, now there are 4 map in the map
        int eleId = -1;
        for (EleButton dataButton : dataButtonList) {
            if (dataButton.isMouseOver(this, true)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        if (eleId != -1) {
            if (eleId == 0) {//finish
                finishClick();
                finishClick = true;
            } else if (eleId == 1) {
                rectRegion = null;
                finishClick();
                finishClick = true;
            }
        }
    }

    private RectRegion getSelectRegion(Position lastClick) {
        float mapWidth = 1000;
        float mapHeight = 800;

        float mx = constrain(mouseX, 3, mapWidth - 3);
        float my = constrain(mouseY, 3, mapHeight - 3);

        Position curClick = new Position(mx, my);
        RectRegion selectRegion = new RectRegion();
        if (lastClick.x < curClick.x) {//left
            if (lastClick.y < curClick.y) {//up
                selectRegion.leftTop = lastClick;
                selectRegion.rightBtm = curClick;
            } else {//left_down
                Position left_top = new Position(lastClick.x, curClick.y);
                Position right_btm = new Position(curClick.x, lastClick.y);
                selectRegion = new RectRegion(left_top, right_btm);
            }
        } else {//right
            if (lastClick.y < curClick.y) {//up
                Position left_top = new Position(curClick.x, lastClick.y);
                Position right_btm = new Position(lastClick.x, curClick.y);
                selectRegion = new RectRegion(left_top, right_btm);
            } else {
                selectRegion = new RectRegion(curClick, lastClick);
            }
        }
        selectRegion.color = PSC.COLOR_LIST[1];


        selectRegion.initLoc(map.getLocation(selectRegion.leftTop.x, selectRegion.leftTop.y),
                map.getLocation(selectRegion.rightBtm.x, selectRegion.rightBtm.y));

        return selectRegion;
    }

    //below for way-point cal
    private void startCalWayPoint() {
        TimeProfileManager tm = new TimeProfileManager(1, totalTrajector, rectRegion);
        tm.startRun();
    }

    double minLat = 100, minLon = 100;

    private TrajectoryMeta[] totalTrajector;

    private void loadTotalData(String filePath) {
        System.out.println("data pre-processing......");
        ArrayList<String> totalTraj = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                totalTraj.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        totalTrajector = new TrajectoryMeta[totalTraj.size()];
        int i = 0;
        for (String trajStr : totalTraj) {
            String[] item = trajStr.split(";");
            String[] trajPoint = item[1].split(",");

            GpsPosition[] gpsPositions = new GpsPosition[trajPoint.length / 2 - 1];
            for (int j = 0; j < trajPoint.length - 2; j += 2) {
                minLat = Math.min(Double.parseDouble(trajPoint[j + 1]), minLat);
                minLon = Math.min(Double.parseDouble(trajPoint[j]), minLon);
                gpsPositions[j / 2] = new GpsPosition(Float.parseFloat(trajPoint[j + 1]), Float.parseFloat(trajPoint[j]));
            }
            TrajectoryMeta traj = new TrajectoryMeta(i);
            traj.setGpsPositions(gpsPositions);
            traj.setScore(Double.parseDouble(item[0]));
            totalTrajector[i++] = traj;
        }
        System.out.println(minLat + ", " + minLon);
        System.out.println(totalTrajector.length);
        System.out.println("data preprocess done");
    }

    private static HashSet<Integer> Vfgs = new HashSet<>();

    private HashSet<Integer> loadVfgs(String filePath) {
        HashSet<Integer> Vfgs = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            int total = 2389863;
            int num = (int) (total * 0.01);
            while ((line = reader.readLine()) != null && num-- > 0) {
                Vfgs.add(Integer.valueOf(line.split(",")[0]));
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        return Vfgs;
    }

    public static void main(String[] args) {

        PApplet.main(new String[]{UserInterface.class.getName()});
    }
}
