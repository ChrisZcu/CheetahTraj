package vqgs.origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.MapUtils;
import vqgs.origin.model.Position;
import vqgs.origin.model.Trajectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import processing.core.PApplet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * input:
 * porto_part.txt, original traj data
 *
 * output:
 * portoScore.txt, (score, traj) paris -> {@link deltaRecordCal}
 *
 * score: # of points in map for a specific traj (i.e. {@link #id2score})
 *
 * change file and limit
 */
public class scoreFileCal extends PApplet {
    private static String dataPath = "data/porto_full.txt";
    private static String outPath = "data/porto_test_old/__score_set.txt"; //"data/portoPartScore.txt";
    public static final int LIMIT = 5_0000;

    private static UnfoldingMap map;
    private static ArrayList<Trajectory> TrajTotal = new ArrayList<>();
    private HashMap<Integer, Integer> id2score = new HashMap<>();

    @Override
    public void setup() {
        map = new UnfoldingMap(this);
        map.zoomAndPanTo(20, new Location(41.141412, -8.618499));
        scoreCal(dataPath, outPath);
        map.setZoomRange(1, 20);
        MapUtils.createDefaultEventDispatcher(this, map);
        System.out.println("score cal done");
        exit();
    }

    // It doesn't matters
    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    private void scoreCal(String dataPath, String outPath) {
        HashSet<Position> trajSet = new HashSet<>();
        try {
            File file = new File(outPath);
            file.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (Trajectory traj : TrajTotal) {
                HashSet<Position> poi = new HashSet<>();
                for (Location p : traj.getPoints()) {
                    double px = map.getScreenPosition(p).x;
                    double py = map.getScreenPosition(p).y;
                    trajSet.add(new Position(px, py));
                    poi.add(new Position(px, py));
                }
                id2score.put(traj.getTrajId(), poi.size());
            }
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            int i = 0;
            while (it.hasNext() && (LIMIT == -1 || i < LIMIT)) {
                String line = it.nextLine();
                writer.write(id2score.get(i) + ";" + line.split(";")[1]);
                writer.newLine();
                i++;
            }
            System.out.println(i);
            LineIterator.closeQuietly(it);
            writer.close();
        } catch (IOException ignored) {

        }
    }

    private static void preProcess() throws IOException {
        File theFile = new File(dataPath);
        LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
        String line;
        String[] data;
        int trajId = 0;
        try {
            while (it.hasNext() && (LIMIT == -1 || trajId < LIMIT)) {
                line = it.nextLine();
                String[] item = line.split(";");
                data = item[1].split(",");
                Trajectory traj = new Trajectory(trajId);
                trajId++;
                for (int i = 0; i < data.length - 2; i = i + 2) {
                    traj.points.add(new Location(Double.parseDouble(data[i + 1]), Double.parseDouble(data[i])));
                }
                TrajTotal.add(traj);
            }
        } finally {
            LineIterator.closeQuietly(it);
            System.out.println(TrajTotal.size());
        }
        System.out.println("preProcessing done");
    }

    public static void runCalFileScore(String in, String out) {
        dataPath = in;
        outPath = out;
        runCalFileScore();
    }

    public static void runCalFileScore() {
        try {
            preProcess();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String title = "origin.util.scoreFileCal";
        PApplet.main(new String[]{title});
    }

    public static void main(String[] args) {
        runCalFileScore();
    }
}

