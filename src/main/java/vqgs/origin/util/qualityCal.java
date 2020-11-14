package origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import origin.model.Trajectory;
import origin.model.Position;
import processing.core.PApplet;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * input:
 * protScore.txt, traj data with score, load to {@link #TrajTotal}
 * ResRecord_DELTA.txt, contribution of traj, from {@link deltaRecordCal}
 *
 * output:
 * portoQuality.txt, test res
 */
public class qualityCal extends PApplet {

    private static final int[] DELTALIST = {0, 4, 8, 16};
    private static final double[] RATELIST = {0.05, 0.01/*, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001*/};
    private static List<Trajectory> TrajTotal; //所有traj 等同于arr

    private static List<HashSet<Integer>[]> deltaCellList = new ArrayList<HashSet<Integer>[]>();


    private static void preProcess() throws IOException {   // !!！
//        String dataPath = "data/porto_test_old/__score_set.txt";
        String dataPath = "data/porto_test2/__score_set.txt";

        TrajTotal = new ArrayList<>();

        UTIL.totalListInit(TrajTotal, dataPath);
        for (Integer e : DELTALIST) {   // !!!
//            String filePath = String.format("data/porto_test_old/vfgs_%d.txt", e);
            String filePath = String.format("data/porto_test2/vfgs_%d.csv", e);
            HashSet[] cellList = new HashSet[RATELIST.length];
            for (int i = 0; i < RATELIST.length; i++) {
                cellList[i] = new HashSet<Integer>();
            }
            readFile(filePath, cellList);
            deltaCellList.add(cellList);
        }
        System.out.println("processing done ");

    }

    private static int[] szDaySizeList = {21425, 4285, 2142, 428, 214, 42, 21, 4};
    private static int[] portoPartList = {50, 10};

    private static void readFile(String path, HashSet<Integer>[] cellList) throws IOException {
        int[] sizeList = portoPartList;
        for (int j = 0; j < sizeList.length; j++) {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            for (int i = 0; i < sizeList[j]; i++) {
                String line = reader.readLine();
                String[] item = line.split(",");
                cellList[j].add(Integer.parseInt(item[0]));
            }
            reader.close();
        }
    }

    public void settings() {
        size(1200, 800, P2D);
    }

    private String whiteMapPath = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    private static UnfoldingMap map;

    int cellZoom = 11;

    public void setup() {
        try {
//            writer = new BufferedWriter(new FileWriter("data/portoQualityOld.txt", true));  //!!!
            writer = new BufferedWriter(new FileWriter("data/portoQualityNew.txt", true));
            writer.write("zoom,delta,sampleRate,score,totalScore,quality");
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        map = new UnfoldingMap(this);

        String mapStyle = whiteMapPath;

        map = new UnfoldingMap(this, "VIS_Demo", new MapBox.CustomMapBoxProvider(mapStyle));

        for (; cellZoom < 21; cellZoom++) {
            map.zoomAndPanTo(cellZoom, new Location(41.101, -8.58));
            TotalScoreCal();
            try {
                scoreCal();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        map.setZoomRange(1, 20);
        MapUtils.createDefaultEventDispatcher(this, map);

        System.out.println("SET UP DONE");
        exit();
    }

    BufferedWriter writer;
    private float lf = (float) 0.5;

    private void scoreCal() throws IOException {

        int j = 0;
        for (HashSet<Integer>[] cellList : deltaCellList) {
            int i = 0;
            for (HashSet<Integer> set : cellList) {     // set -> R
                long t0 = System.currentTimeMillis();
                int delta = DELTALIST[j];
                System.out.println("Current: " + delta + ", " + RATELIST[i] + ", " + set.size());
                HashSet<Position> scoreSet = new HashSet<>(17, lf);
                double rate = RATELIST[i];
                i++;
                for (Integer e : set) {
                    Trajectory traj = TrajTotal.get(e);
                    for (Location p : traj.getPoints()) {
                        int px = (int) map.getScreenPosition(p).x;
                        int py = (int) map.getScreenPosition(p).y;
                        scoreSet.add(new Position(px, py));
                    }
                }

                System.out.println("score set time: " + (System.currentTimeMillis() - t0));

                int score = scoreSet.size();
                HashSet<Position> deltaSet = new HashSet<>(TOTALSCORE, lf);
                int num0 = 0;
                System.out.println(set.size());
                for (Integer e : set) {
                    num0++;
                    long t1 = System.currentTimeMillis();
                    Trajectory traj = TrajTotal.get(e);
                    for (Location p : traj.getPoints()) {
                        int px = (int) map.getScreenPosition(p).x;
                        int py = (int) map.getScreenPosition(p).y;
                        for (int l = -delta; l < delta + 1; l++) {
                            for (int m = -delta; m < delta + 1; m++) {
                                Position poi = new Position(px + l, py + m);
                                if (TotalSet.contains(poi) && !scoreSet.contains(poi))
//                                {
                                    deltaSet.add(poi);
                            }
                        }
                    }
                    long t2 = System.currentTimeMillis();
                    System.out.println("time: " + (t2 - t1) + ", current time: " + num0 + ", delta set: " + deltaSet.size());
                }
                System.out.println("delta set: " + deltaSet.size());
                System.out.println("delta set time: " + (System.currentTimeMillis() - t0));
                for (Trajectory traj : TrajTotal) {
                    if (set.contains(traj.getTrajId()))
                        continue;
                    for (Location p : traj.getPoints()) {
                        int px = (int) map.getScreenPosition(p).x;
                        int py = (int) map.getScreenPosition(p).y;
                        Position poi = new Position(px, py);
//                        tempScore.add(poi);
                        if (deltaSet.contains(poi)) {
                            score++;
                            deltaSet.remove(poi);
                        }
                    }
                }
                System.out.println("total score time: " + (System.currentTimeMillis() - t0));
//            writer = new BufferedWriter(new FileWriter("data/portoQualityOld.txt", true));  //!!!
                writer = new BufferedWriter(new FileWriter("data/portoQualityNew.txt", true));
                writer.write(cellZoom + "," + delta + "," + rate + "," + score + "," + TOTALSCORE + "," + score * 1.0 / TOTALSCORE);
                writer.newLine();
                writer.close();
                System.out.println(cellZoom + "," + delta + "," + rate + "," + score + "," + TOTALSCORE + "," + score * 1.0 / TOTALSCORE);
            }
            j++;
        }
    }

    private int TOTALSCORE = 0;

    private HashSet<Position> TotalSet = new HashSet<>();

    private void TotalScoreCal() {
        System.out.println(map.getZoomLevel());
        long t0 = System.currentTimeMillis();
        for (Trajectory t : TrajTotal) {
            for (Location p : t.getPoints()) {
                int px = (int) map.getScreenPosition(p).x;
                int py = (int) map.getScreenPosition(p).y;
                TotalSet.add(new Position(px, py));
            }
        }
        TOTALSCORE = TotalSet.size();
        System.out.println("Total Score:::" + TOTALSCORE);
        System.out.println("time: " + (System.currentTimeMillis() - t0));
    }

    public static void main(String[] args) {
        try {
            preProcess();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String title = "origin.util.qualityCal";
        PApplet.main(new String[]{title});
    }
}
