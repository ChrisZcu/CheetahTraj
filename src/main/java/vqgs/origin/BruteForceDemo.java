package vqgs.origin;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.events.EventDispatcher;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import vqgs.origin.model.Trajectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import processing.core.PApplet;
import processing.core.PImage;
import vqgs.origin.util.MyKeyboardHandler;
import vqgs.origin.util.GreedyChoose;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户交互demo
 */
public class BruteForceDemo extends PApplet {
    private static final Location PORTO_CENTER = new Location(41.14, -8.639);//维度经度
    private static final int AVERAGE_TIMES = 10; //重复测试次数

    private static GreedyChoose greedyChoose; //最大堆
    private static UnfoldingMap map;
    private List<Trajectory> trajShow = new ArrayList<>(); //要展示的traj
    private static Trajectory[] trajShowArr;
    private static List<Trajectory> trajTotal; //所有traj
    private static int curTimes = 0; //当前执行次数
    private static long totalCost = 0;

    /**
     * 将轨迹数据载入内存。数据格式：
     * score;double,double...
     *
     * 疑点1：(data[i + 1], data[i]) ? (貌似运行没有问题)
     */
    private static void preProcess() {
        String dataPath = "dal/porto_part.txt";
        trajTotal = new ArrayList<>();
        try {
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            ArrayList<Location> list;
            String line = null;
            String[] data;
            int trajId = 0;
            try {
                while (it.hasNext()) {
                    line = it.nextLine();
                    String[] item = line.split(";");
                    double score = Double.parseDouble(item[0]);
                    data = item[1].split(",");
                    Trajectory traj = new Trajectory(trajId);
                    trajId++;
                    list = new ArrayList<>();
                    for (int i = 0; i < data.length - 2; i = i + 2) {
                        Location point = new Location(Double.parseDouble(data[i + 1]), Double.parseDouble(data[i]));
                        list.add(point);
                    }
                    traj.points = list;
                    traj.setScore(score);
                    trajTotal.add(traj);

                    break;
                }

                for (int i = 0; i < 10_0000; i++) {
                    String[] item = line.split(";");
                    double score = Double.parseDouble(item[0]);
                    data = item[1].split(",");
                    Trajectory traj = new Trajectory(trajId);
                    trajId++;
                    list = new ArrayList<>();
                    for (int j = 0; j < data.length - 2; j = j + 2) {
                        Location point = new Location(Double.parseDouble(data[j + 1]) + 0.0000004 * i, Double.parseDouble(data[j]) + 0.0000004 * i);
                        list.add(point);
                    }
                    traj.points = list;
                    traj.setScore(score);
                    trajTotal.add(traj);
                }

            } finally {
                LineIterator.closeQuietly(it);
            }
            //System.out.println("processing done");
            //System.out.println("max heap init done");
            //System.out.println("total trajectory num: " + TrajTotal.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    /**
     * 使用Mapbox的API
     */
    private String blackMapPath = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck7dbtz3e13lj1ip8r5x12bsy/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";
    private String whiteMapPath = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    /**
     * 初始化地图并移动至目标位置
     * 设置事件监听
     */
    @Override
    public void setup() {
        map = new UnfoldingMap(this);

        String mapStyle = whiteMapPath;

        map = new UnfoldingMap(this, "VIS_Demo", new MapBox.CustomMapBoxProvider(mapStyle));

        map.setZoomRange(1, 20);

        EventDispatcher ed = MapUtils.createDefaultEventDispatcher(this, map);
        ed.addBroadcaster(new MyKeyboardHandler(this, map));

        map.zoomAndPanTo(12, PORTO_CENTER);
        map.setBackgroundColor(255);

        fill(0, 102, 153, 2);     // ?
        textSize(20);

        // add some traj to show
        trajShow.addAll(trajTotal.subList(0, 10_0000));

        trajShowArr = trajShow.toArray(new Trajectory[trajShow.size()]);

        trajShow = null;
    }

    private int rectIndex = 0;
    private int[][] region = new int[4][2];
    private int[][] rect = new int[4][2];
    private boolean regionFinish = false;

    private PImage mapImage = null;
    private boolean totalLoad = false;
    private int checkLevel = -1;
    private Location checkCenter = new Location(-1, -1);

    @Override
    public void keyPressed() {
        System.out.println(keyCode);
        if (key == '1') {
            System.out.println(1);
        } else if (key == '2') {
            System.out.println(2);
        } else if (key == '3') {
            System.out.println(3);
        } else if (key == 'x') {
            System.out.println('x');
        }
    }

    @Override
    public void draw() {
        if (checkLevel != map.getZoomLevel() || !checkCenter.equals(map.getCenter())) {
            totalLoad = false;
            checkLevel = map.getZoomLevel();
            checkCenter = map.getCenter();
//            loop();
        }
        if (!totalLoad) {
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else {
                totalLoad = true;
            }
            map.draw();
        } else {
            map.draw();
        }

            long t1 = System.currentTimeMillis();
            noFill();
            stroke(255, 0, 0);
            strokeWeight(1);
            for (Trajectory traj : trajShowArr) {
                beginShape();
                for (Location loc : traj.points) {
                    ScreenPosition pos = map.getScreenPosition(loc);
                    vertex(pos.x, pos.y);
                }
                endShape();
            }
            //点
            stroke(74, 113, 155);
            strokeWeight(10);
            for (int i = 0; i < rectIndex; i++) {
                int x = rect[i][0];
                int y = rect[i][1];
                point(x, y);
            }
            strokeWeight(5);
            if (regionFinish) {
                int length = Math.abs(rect[1][0] - rect[0][0]);
                int width = Math.abs(rect[2][1] - rect[0][1]);
                rect(rect[0][0], rect[0][1], length, width);
            }
//        }
        fill(50);
        text(map.allTilesLoaded() ? "Completed" : "Loading...", 10, 24);
        noFill();
    }


    public static void main(String[] args) {
        preProcess();
        String title = "tempbin.BruteForceDemo";
        PApplet.main(new String[]{title});
    }
}
