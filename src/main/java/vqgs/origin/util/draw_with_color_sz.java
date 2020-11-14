package origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import de.tototec.cmdoption.CmdlineParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import processing.core.PApplet;
import origin.model.Trajectory;
import util.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class draw_with_color_sz extends PApplet {

    private static final int ZOOMLEVEL = 11;
    private static final Location PortugalCenter = new Location(22.641, 113.835);
    private static UnfoldingMap map;
    private static List<List<Trajectory>> TrajShow = new ArrayList<>();
    private static HashMap<Integer, Double> colorSet = new HashMap<>();
    final static Integer[] COLORS = {
            15, 91, 120,
            19, 149, 186,
            162, 184, 108,
            235, 200, 68,
            241, 108, 32,
            190, 46, 29
    };
    private static final int[] ZOOMLIST = {11, 12, 13, 14, 15, 16, 17};
    private static final Location[] porto_center = {new Location(41.144, -8.639), new Location(41.093, -8.589), new Location(41.112, -8.525),
            new Location(41.193, -8.520), new Location(41.23, -8.63), new Location(41.277, -8.281),
            new Location(41.18114, -8.414669), new Location(41.2037, -8.3045), new Location(41.2765, -8.3762),
            new Location(41.72913, -8.67484), new Location(41.529533, -8.676072), new Location(41.4784759, -8.404744), new Location(41.451811, -8.655799),
            new Location(41.215459, -7.70277508), new Location(41.14514, -7.996912), new Location(41.10344, -8.181597),
            new Location(41.4819594, -8.0645941)
    };

    private static final Location[] sz_center =

            /*shenzhen*/
            {/**/new Location(22.641, 113.835), new Location(22.523, 113.929),
                    new Location(22.691, 113.929), new Location(22.533, 114.014), new Location(22.629, 114.029),
                    new Location(22.535, 114.060), new Location(22.544, 114.117), new Location(22.717, 114.269)};

    /*chengdu*/
//            {new Location(30.670, 104.063), new Location(30.708, 104.068),new Location(30.691, 104.092),
//                    new Location(30.704, 104.105), new Location(30.699, 104.049), new Location(30.669, 104.105)};


    private static double sampleRate = 0.01;
    private static int delta = 128;
    private static int offset = 0;


    private static int zoomidx = 0;
    private static int centeridx = 0;
    //  119493 0.05  23898 0.01  11949 0.005  2389 0.001  1194 0.0005 238 0.0001 119 0.00005 23 0.00001
    private static HashMap<Double, Integer> rate2num = new HashMap<Double, Integer>() {
        {
            put(0.01, 4285);
            put(0.005, 2142);
        }
    };
    private static Double[] segmentPoint = {0.0, 0.0, 0.0, 0.0, 0.0};


    private static double getMeans(ArrayList<Double> arr) {
        double sum = 0.0;
        for (double val : arr) {
            sum += val;
        }
        return sum / arr.size();
    }

    private static double getStandadDiviation(ArrayList<Double> arr) {
        int len = arr.size();
        double avg = getMeans(arr);
        double dVar = 0.0;
        for (double val : arr) {
            dVar += (val - avg) * (val - avg);
        }
        return Math.sqrt(dVar / len);
    }

    private static void initColorSet(String path) throws IOException {
        int num = rate2num.get(sampleRate);
        ArrayList<Double> valueArr = new ArrayList<>();
        path = path + "color_" + delta + ".csv";
        System.out.println(path);
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        for (int i = 0; i < num; i++) {
            line = reader.readLine();
            try {
                String[] item = line.split(";");
                int trajId = Integer.valueOf(item[0]);
                double value = Double.valueOf(item[1]);
                valueArr.add(value);
                colorSet.put(trajId, value);
            } catch (NullPointerException e) {
                System.out.println("......." + line);
            }

        }
        Collections.sort(valueArr);
        double outlier = getMeans(valueArr) + 3 * getStandadDiviation(valueArr);
        int i;
        for (i = 0; i < valueArr.size(); i++) {
            if (valueArr.get(i) > outlier) {
                break;
            }
        }
        i -= 1;
        i /= 5;
        segmentPoint[0] = valueArr.get(i);
        segmentPoint[1] = valueArr.get(i * 2);
        segmentPoint[2] = valueArr.get(i * 3);
        segmentPoint[3] = valueArr.get(i * 4);
        segmentPoint[4] = valueArr.get(i * 5);
    }

    private static void preProcess(String dataPath) {
        for (int i = 0; i < 6; i++) {
            TrajShow.add(new ArrayList<>());
        }
        int trajId = 0;
        try {
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            String line;
            String[] data;
            try {
                while (it.hasNext()) {
                    line = it.nextLine();
                    data = line.split(";")[1].split(",");
                    if (colorSet.containsKey(trajId)) {
                        Trajectory traj = new Trajectory(trajId);
                        for (int i = 0; i < data.length - 2; i = i + 2) {
                            Location point = new Location(Double.parseDouble(data[i + 1]), Double.parseDouble(data[i]));
                            traj.points.add(point);
                        }
                        double color = colorSet.get(traj.getTrajId());
                        if (color <= segmentPoint[0]) {
                            TrajShow.get(0).add(traj);
                        } else if (color <= segmentPoint[1]) {
                            TrajShow.get(1).add(traj);
                        } else if (color <= segmentPoint[2]) {
                            TrajShow.get(2).add(traj);
                        } else if (color <= segmentPoint[3]) {
                            TrajShow.get(3).add(traj);
                        } else if (color <= segmentPoint[4]) {
                            TrajShow.get(4).add(traj);
                        } else {
                            TrajShow.get(5).add(traj);
                        }
                    }
                    trajId++;
                }
            } finally {
                LineIterator.closeQuietly(it);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void settings() {
        size(1200, 800, P2D);
        smooth();
    }

    Location[] CENTERLIST = {};

    public void setup() {
        CENTERLIST = type == 0 ? sz_center : porto_center;
        String whiteMapPath = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";
        map = new UnfoldingMap(this, "draw_with_color2", new MapBox.CustomMapBoxProvider(whiteMapPath));
        MapUtils.createDefaultEventDispatcher(this, map);
        map.setZoomRange(1, 20);
        map.zoomAndPanTo(ZOOMLEVEL, PortugalCenter);
        map.zoomAndPanTo(ZOOMLIST[zoomidx], CENTERLIST[centeridx]);
        System.out.println("SET UP DONE");
    }

    private int done = 0;
    String temppaht = "data/figure_result/";

    public void draw() { // 有地图
        map.draw();

        if (map.allTilesLoaded()) {
            map.draw();
            map.draw();
            map.draw();
            map.draw();
            map.draw();

            if (done == 10) {
                done = 0;
                noFill();
                strokeWeight(1);
                for (int i = 0; i < 6; i++) {
                    stroke(COLORS[3 * i], COLORS[3 * i + 1], COLORS[3 * i + 2]);    // set color here
                    for (Trajectory traj : TrajShow.get(i)) {
                        beginShape();
                        for (Location loc : traj.points) {
                            ScreenPosition pos = map.getScreenPosition(loc);
                            vertex(pos.x, pos.y);
                        }
                        endShape();
                    }
                }


                saveFrame(temppaht + "color/p" + (centeridx + offset) + "/sample_rate" + (sampleRate) + "_delta" + delta + "_zoom" + map.getZoomLevel() + "_center_p" + (centeridx + offset) + ".png");
                if (zoomidx + 1 < ZOOMLIST.length) {
                    zoomidx += 1;
                    map.zoomToLevel(ZOOMLIST[zoomidx]);
                } else {
                    zoomidx = 0;
                    centeridx += 1;
                    if (centeridx < CENTERLIST.length) {
                        map.zoomAndPanTo(ZOOMLIST[zoomidx], CENTERLIST[centeridx]);
                    } else {
                        System.out.println("draw done");
                        noLoop();
                    }
                }

            }
            done += 1;
        }

    }

    private static int type = 0;

    public static void main(String[] args) {
        Config config = new Config();
        CmdlineParser cp = new CmdlineParser(config);
        cp.setAggregateShortOptionsWithPrefix("-");
        cp.setProgramName("Screenshot");
        cp.parse(args);

        if (config.help)
            cp.usage();
        else if (config.dataset == null || config.vfgs == null) {
            System.err.println("Dataset file path and VFGS/VFGS+ result file path is necessary!!!");
        } else {
            type = config.type;
            try {
                initColorSet(config.vfgs);
                preProcess(config.dataset);
                delta = config.delta;
                System.out.println("init done");
            } catch (IOException e) {
                e.printStackTrace();
            }
            String title = "origin.util.draw_with_color_sz";
            PApplet.main(new String[]{title});
        }
    }

}

