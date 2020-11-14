package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.RectRegion;
import model.Trajectory;
import processing.core.PApplet;
import select.TimeProfileManager;
import util.PSC;

import java.awt.*;
import java.io.*;
import java.util.*;

public class TimeProfile extends PApplet {
    UnfoldingMap map;
    private int ZOOMLEVEL = 11;
    private Location PRESENT = new Location(41.151, -8.634);

    @Override
    public void settings() {
        size(1000, 800, P2D);
    }

    int[][] VFGSIdList;

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);
        map.setBackgroundColor(255);
        MapUtils.createDefaultEventDispatcher(this, map);

//        VFGSIdList = loadVfgs("data/GPS/vfgs_0.csv");

        loadData("data/GPS/porto_full.txt");
//        loadData("data/GPS/Porto5w/Porto5w.txt");
        initRandomIdList();
        loadRandomTrajList();
//        loadTrajList();
    }

    int VFGS = 0;
    double[] rate = {0.05, 0.01, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001};

    @Override
    public void draw() {
        if (!map.allTilesLoaded()) {
            map.draw();
        } else {
            map.draw();
            float x = (float) (500 * recNumId);
            float y = (float) (400 * recNumId);
            rectRegion.setLeftTopLoc(map.getLocation(500 - x, 400 - y));
            rectRegion.setRightBtmLoc(map.getLocation(500 + x, 400 + y));

            System.out.println("using: " + recNumId);
            recNumId *= 2;

            startCalWayPoint();

            trajShow.clear();
            noLoop = true;
            thread = true;

            if (thread) {
                for (Trajectory[] trajList : TimeProfileSharedObject.getInstance().trajRes) {
                    Collections.addAll(trajShow, trajList);
                }
            }

            noFill();
            strokeWeight(1);
            stroke(new Color(190, 46, 29).getRGB());

            long t0 = System.currentTimeMillis();
            drawCPU();
            long time = (System.currentTimeMillis() - t0);
            System.out.println("CPU draw time for rate " + rate[VFGS] + " of VFGS: " + time + "ms");
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter("data/record_ran_rate_" + rate[VFGS] + ".txt", true));
                writer.write(time + "\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (recNumId > 1.1) {
                recNumId = 1.0 / 128;
                VFGS++;
            }

            if (VFGS == randomIdList.length)
                noLoop();
            else {
//                loadTrajList();
                loadRandomTrajList();
            }
        }
    }

    private void drawCPU() {
        for (Trajectory traj : trajShow) {
            beginShape();
            for (Location loc : traj.locations) {
                ScreenPosition src = map.getScreenPosition(loc);
                vertex(src.x, src.y);
            }
            endShape();
        }
        if (thread)
            drawRect();
    }

    private void drawRect() {
        noFill();
        strokeWeight(2);
        stroke(new Color(19, 149, 186).getRGB());

        ScreenPosition src1 = map.getScreenPosition(rectRegion.getLeftTopLoc());
        ScreenPosition src2 = map.getScreenPosition(rectRegion.getRightBtmLoc());
        rect(src1.x, src1.y, src2.x - src1.x, src2.y - src1.y);
    }

    boolean noLoop = false;
    double recNumId = 1.0 / 128;
    float baseX = 500;
    float baseY = 400;
    boolean thread = false;

    @Override
    public void keyPressed() {
        if (key == 'q') {
            System.out.println(map.getCenter());
            System.out.println(map.getZoomLevel());
            trajShow.clear();
            noLoop = false;
            thread = false;
            loop();
        } else if (key == 'w') {
            Collections.addAll(trajShow, trajFull);
            noLoop = true;
        } else if (key == 'e') {
            float x = (float) (500 * recNumId);
            float y = (float) (400 * recNumId);
            rectRegion.setLeftTopLoc(map.getLocation(500 - x, 400 - y));
            rectRegion.setRightBtmLoc(map.getLocation(500 + x, 400 + y));
            System.out.println(rectRegion.getLeftTopLoc());
            System.out.println(rectRegion.getRightBtmLoc());

            System.out.println("using: " + recNumId);
            recNumId *= 2;

            startCalWayPoint();

            trajShow.clear();
            noLoop = true;
            thread = true;
            loop();
        }
        System.out.println(key);
    }


    Trajectory[] trajFull;
    ArrayList<Trajectory> trajShow = new ArrayList<>();

    private void loadData(String filePath) {
        try {
            ArrayList<String> trajStr = new ArrayList<>(2400000);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajStr.add(line);
            }
            reader.close();
            System.out.println("load done");
            int j = 0;

            trajFull = new Trajectory[trajStr.size()];

            for (String trajM : trajStr) {
                String[] data = trajM.split(";")[1].split(",");

                Trajectory traj = new Trajectory(j);
                ArrayList<Location> locations = new ArrayList<>();
                for (int i = 0; i < data.length - 2; i = i + 2) {
                    locations.add(new Location(Float.parseFloat(data[i + 1]),
                            Float.parseFloat(data[i])));
                }
                traj.setLocations(locations.toArray(new Location[0]));

                trajFull[j++] = traj;
            }
            trajStr.clear();
            System.out.println("load done");
            System.out.println("traj number: " + trajFull.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    RectRegion rectRegion = new RectRegion();
    Trajectory[] selectTrajList;

    private void startCalWayPoint() {
        System.out.println("length: " + selectTrajList.length);
        TimeProfileManager tm = new TimeProfileManager(1, selectTrajList, rectRegion);
        tm.startRun();
    }

    private int[][] loadVfgs(String filePath) {
        int[][] resId = new int[8][];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            ArrayList<Integer> id = new ArrayList<>();
            int i = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(",")) {
                    resId[i] = new int[id.size()];
                    System.out.println(id.size());
                    int j = 0;
                    for (Integer e : id) {
                        resId[i][j++] = e;
                    }
                    i++;
                    id.clear();
                } else {
                    id.add(Integer.parseInt(line.split(",")[0]));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resId;
    }

    private void loadTrajList() {
        selectTrajList = new Trajectory[VFGSIdList[VFGS].length];
        int i = 0;
        for (Integer e : VFGSIdList[VFGS]) {
            selectTrajList[i++] = trajFull[e];
        }
    }

    int[][] randomIdList;

    private void initRandomIdList() {
        randomIdList = new int[rate.length][];
        int i = 0;
        Random ran = new Random(1);
        HashSet<Integer> tmp = new HashSet<Integer>((int) (trajFull.length * 0.05));

        for (double rate : rate) {
            tmp.clear();
            int num = (int) (rate * trajFull.length);
            randomIdList[i] = new int[num];
            int j = 0;
            while (tmp.size() < num) {
                tmp.add(ran.nextInt(trajFull.length - 1));
            }
            for (Integer e : tmp) {
                randomIdList[i][j++] = e;
            }
            i++;
        }
    }

    private void loadRandomTrajList() {
        selectTrajList = new Trajectory[randomIdList[VFGS].length];
        int i = 0;
        for (Integer e : randomIdList[VFGS]) {
            selectTrajList[i++] = trajFull[e];
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{TimeProfile.class.getName()});
    }
}
