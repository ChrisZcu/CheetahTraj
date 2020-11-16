package vqgs.origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import vqgs.origin.model.Position;
import vqgs.origin.model.Trajectory;
import processing.core.PApplet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * input:
 * data/portScore.txt, {@link scoreFileCal} -> {@link #TrajTotal}
 *
 * output:
 * ResRecord_DELTA.txt, selected traj (R) in specific DELTA,
 * {@link #deltaCellList} -> {@link qualityCal}
 * also -> {@link colorCal}
 *
 * change {@link #dataPath}, and !!!
 */
public class deltaRecordCal extends PApplet {
    private static int TRAJNUM = 0; // 画出的轨迹
    private static final Location CELLCENTER = new Location(30.730, 104.038);
    private static List<Trajectory> TrajTotal; //所有traj 等同于arr
    private static UnfoldingMap map;
    private static HashSet<Position> trajSet = new HashSet<>();    // R+
    private static vqgs.origin.util.GreedyChoose GreedyChoose;
    private static HashSet<Position> totalTrajPos = new HashSet<>();

    private static final int[] DELTALIST = {0, 4, 8, 16/*, 32, 50, 64, 128*/};
    private static final double[] RATELIST = {0.05, 0.01/*, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001*/};

    // [][] for cellList (HashSet, i.e. VFGS result R).
    private static List<HashSet<Integer>[]> deltaCellList = new ArrayList<>();
    private static String dataPath = "data/porto_test_old/__score_set.txt"; //"data/portScore.txt";

    // this doesn't matters
    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    private static void preProcess() {
        for (int i = 0; i < DELTALIST.length; i++) {
            HashSet[] cellList = new HashSet[RATELIST.length];
            for (int j = 0; j < RATELIST.length; j++) {
                cellList[j] = new HashSet<Integer>();
            }
            deltaCellList.add(cellList);
        }

        TrajTotal = new ArrayList<>();

        int trajId = 0;

        try {
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            String line;
            String[] data;
            try {
                while (it.hasNext()) {
                    line = it.nextLine();
                    String[] item = line.split(";");
                    double score = Double.parseDouble(item[0]);
                    data = item[1].split(",");
                    Trajectory traj = new Trajectory(trajId);
                    trajId++;
                    for (int i = 0; i < data.length - 2; i = i + 2) {
                        Location point = new Location(Double.parseDouble(data[i + 1]), Double.parseDouble(data[i]));
                        traj.points.add(point);
                    }
                    traj.setScore(score);
                    TrajTotal.add(traj);
                }
            } finally {
                LineIterator.closeQuietly(it);
            }
            System.out.println("delta record cal pre-processing done");
            System.out.println("total trajectory num: " + TrajTotal.size());
            GreedyChoose = new GreedyChoose(TrajTotal.size());
            TRAJNUM = TrajTotal.size();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * heapify all traj, but use O(nlog)... :(
     */
    private void scoreInit() {
        GreedyChoose.clean();
        int trajId = 0;

        try {
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            String line;
            try {
                while (it.hasNext()) {
                    line = it.nextLine();
                    String[] item = line.split(";");
                    double score = Double.parseDouble(item[0]);
                    Trajectory traj = TrajTotal.get(trajId);
                    traj.setScore(score);
                    GreedyChoose.addNewTraj(traj);
                    trajId++;
                }
            } finally {
                LineIterator.closeQuietly(it);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this);
        map.zoomAndPanTo(20, CELLCENTER);

        totalTrajPosInit();     // i.e. C in paper

        try {
            GetCellCover();
        } catch (IOException e) {
            e.printStackTrace();
        }

        map.setZoomRange(1, 20);
        MapUtils.createDefaultEventDispatcher(this, map);

        System.out.println("SET UP DONE");
        exit();
    }

    private void totalTrajPosInit() {
        for (Trajectory traj : TrajTotal) {
            for (Location p : traj.getPoints()) {
                double px = map.getScreenPosition(p).x;
                double py = map.getScreenPosition(p).y;
                totalTrajPos.add(new Position(px, py));
            }
        }
    }

    /**
     * Use lemma 2 to calculate contributions
     */
    private void GetCellCover() throws IOException {//record cal
        TRAJNUM = TrajTotal.size();
        for (int j = 0; j < DELTALIST.length; j++) {
            long startTime = System.currentTimeMillis();
            int DELTA = DELTALIST[j];
            HashSet<Integer>[] cellList = deltaCellList.get(j);
            trajSet.clear();
            scoreInit();
            int n = RATELIST.length;
            int i = 0;
            long t0 = System.currentTimeMillis();
            while (n > 0) {
                n--;
                int trajNum = (int) (RATELIST[n] * TRAJNUM);
//                System.out.println("Current: " + trajNum + "," + DELTA);
                for (; i < trajNum; i++) {
                    while (true) {
                        Traj2CellScore(GreedyChoose.getHeapHead(), DELTA);
                        if (GreedyChoose.GreetOrder()) {
                            Trajectory traj = GreedyChoose.getMaxScoreTraj();   // deleteMax
                            for (int num = 0; num <= n; num++) {
                                cellList[num].add(traj.getTrajId());    // take this, add to R
                            }
                            CellGridUpdate(traj, DELTA);        // update R+
                            break;
                        } else {
                            GreedyChoose.orderAdjust();
                        }
                    }
                }
                long t2 = System.currentTimeMillis();
//                System.out.println(RATELIST[n] + ": " + (t2 - t0) + "ms");
                System.out.println("alpha=" + RATELIST[n] + " delta=" + DELTA + ": "
                        + (t2 - t0) + " ms");
            }
            long endTime = System.currentTimeMillis();
            System.out.println(/*"delta " + DELTA + " time used: " + (endTime - startTime)*/);
//            System.out.println("begin write delta " + DELTA);
            // I changed here
            for (int k = 0; k < cellList.length; k++) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("data" +
                        "/porto_test_old/vfgs_%d.txt", DELTA)));   // !!!
                for (Integer e : cellList[0]) {
                    writer.write(e + "," + 0/*TrajTotal.get(e).getScore()*/); // e: trajId
                    writer.newLine();
                }
                writer.close();
            }
//            System.out.println("delta " + DELTA + " Write done");
        }
        trajSet.clear();
    }

    private void CellGridUpdate(Trajectory traj, int DELTA) {
        for (Location p : traj.getPoints()) {
            double px = map.getScreenPosition(p).x;
            double py = map.getScreenPosition(p).y;
            for (int i = -DELTA; i <= DELTA; i++) {
                for (int j = -DELTA; j <= DELTA; j++) {
                    Position pos = new Position(px + i, py + j);
                    if (!totalTrajPos.contains(pos))
                        trajSet.add(pos);
                }
            }
        }
    }

    private void Traj2CellScore(Trajectory traj, int DELTA) {
        traj.setScore(0);
        HashSet<Location> set = new HashSet<>();
        for (Location p : traj.getPoints()) {
            double px = map.getScreenPosition(p).x;
            double py = map.getScreenPosition(p).y;
            if (set.contains(p)
                    || trajSet.contains(new Position(px, py)))  // R+
                continue;
            // not in R+ or self point set
            set.add(p);
            int score = InScoreCheck(px, py, DELTA);
            if (score == 0) {
                continue;
            }
            traj.setScore(traj.getScore() + score);
        }
        exit();
    }


    private int InScoreCheck(double px, double py, int DELTA) {
        int score = 1;
        for (int i = -DELTA; i <= DELTA; i++) {
            for (int j = -DELTA; j <= DELTA; j++) {
                if (trajSet.contains(new Position(px + i, py + j))) {
                    return 0;
                }
            }
        }
        return score;
    }

    @Override
    public void draw() {
        map.draw();
        noFill();
        stroke(255, 0, 0);
        strokeWeight(1);
        for (Trajectory traj : TrajTotal) {
            beginShape();
            for (Location loc : traj.points) {
                ScreenPosition pos = map.getScreenPosition(loc);
                vertex(pos.x, pos.y);
            }
            endShape();
        }
    }

    public static void deltaRecordCal() {
        preProcess();
        String title = "origin.util.deltaRecordCal";
        PApplet.main(new String[]{title});
    }

    public static void deltaRecordCal(String path) {
        dataPath = path;
        deltaRecordCal();
    }

    public static void main(String[] args) {
        deltaRecordCal.deltaRecordCal();
    }
}

