package origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import de.tototec.cmdoption.CmdlineParser;
import origin.model.Trajectory;
import processing.core.PApplet;
import processing.core.PImage;
import util.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class screenShot extends PApplet {

//    private Set<Integer> CellCover; //最小粒度情况下的最大cover

    private List<Trajectory> TrajShow = new ArrayList<>(); //要展示的traj
    private static List<Trajectory> TrajTotal; //所有traj 等同于arr
    private static UnfoldingMap map;
    private static final int AVERAGETIMES = 10; //重复测试次数
    private static int CurTimes = 0; //当前执行次数
    private static long TotalCost = 0;

    private int centerIndex = 0;
    private static int ZOOMLEVEL = 11;
    private static final int[] DELTALIST = {0, 8, 64};
    private static final double[] RATELIST = {0.05, 0.01, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001};
    private final Location[] SZCENTER = {new Location(22.641, 113.835), new Location(22.523, 113.929),
            new Location(22.691, 113.929), new Location(22.533, 114.014), new Location(22.629, 114.029),
            new Location(22.535, 114.060), new Location(22.544, 114.117), new Location(22.717, 114.269)};
    private final Location[] PORTOCENTER = {new Location(41.72913, -8.67484), new Location(41.529533, -8.676072), new Location(41.4784759, -8.404744), new Location(41.451811, -8.655799),
            new Location(41.215459, -7.70277508), new Location(41.14514, -7.996912), new Location(41.10344, -8.181597),
            new Location(41.4819594, -8.0645941), new Location(41.144, -8.639), new Location(41.093, -8.589), new Location(41.112, -8.525),
            new Location(41.193, -8.520), new Location(41.23, -8.63), new Location(41.277, -8.281),
            new Location(41.18114, -8.414669), new Location(41.2037, -8.3045), new Location(41.2765, -8.3762)};
    private Location[] centerList = type == 0 ? SZCENTER : PORTOCENTER;

    private static List<HashMap<Integer, Double>[]> deltaCellList = new ArrayList<HashMap<Integer, Double>[]>();

    private static void preProcess(String dataPath, String recordPath) throws IOException {
        TrajTotal = new ArrayList<>();
        UTIL.totalListInit(TrajTotal, dataPath);

        for (Integer e : DELTALIST) {
            String filePath = String.format(recordPath + "/vfgs_%d.csv", e);
            HashMap[] cellList = new HashMap[RATELIST.length];
            for (int i = 0; i < RATELIST.length; i++) {
                cellList[i] = new HashMap<Integer, Double>();
            }
            readFile(filePath, cellList);
            deltaCellList.add(cellList);
        }
        System.out.println("processing done ");
        System.out.println("trajectory size " + TrajTotal.size());
    }

    private static int[] cdSizeList = {14003, 2800, 1400, 280, 140, 28, 14, 2};
    private static int[] szWeekSizeList = {153343, 30668, 15334, 3066, 1533, 306, 153, 30};
    private static int[] szDaySizeList = {21425, 4285, 2142, 428, 214, 42, 21, 4};
    private static int[] portoSzieList = {119493, 23898, 11949, 2389, 1194, 238, 119, 23};
    private static int type = 0;

    private static void readFile(String path, HashMap[] cellList) throws IOException {

        int[] sizeList = type == 0 ? szWeekSizeList : portoSzieList;
        BufferedReader reader = new BufferedReader(new FileReader(path));
        for (int j = 0; j < sizeList.length; j++) {
            for (int i = 0; i < sizeList[j]; i++) {
                String line = reader.readLine();
                String[] item = line.split(",");
                cellList[j].put(Integer.parseInt(item[0]), Double.parseDouble(item[1]));
            }
            String line = reader.readLine();
        }
        reader.close();
    }

    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    private String blackMapPath = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck7dbtz3e13lj1ip8r5x12bsy/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";
    private String whiteMapPath = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    @Override
    public void setup() {
        map = new UnfoldingMap(this);

        String mapStyle = whiteMapPath;

        map = new UnfoldingMap(this, "VIS_Demo", new MapBox.CustomMapBoxProvider(mapStyle));

        map.setZoomRange(1, 20);
        MapUtils.createDefaultEventDispatcher(this, map);
        map.zoomAndPanTo(ZOOMLEVEL, centerList[centerIndex]);

        System.out.println("SET UP DONE");
    }

    private static String PNGPATH = "data/figure_result/test.png";
    HashSet<Integer> CellCover = new HashSet<>();
    int cellSetPx = 0;
    int cellSetPy = 0;
    int rateId = 0;
    String temppaht = "data/figure_result/";

    private PImage mapImage = null;
    private boolean TotalLoad = false;
    int recNum = 0;
    int ranNum = 0;
    int cellSetPx2 = 0;

    @Override
    public void draw() { // 有地图
        if (!TotalLoad) {
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else {
                TotalLoad = true;
                System.out.println("Load Done");
            }
            map.draw();
        } else {
            map.draw();
            long t1 = System.currentTimeMillis();
            noFill();
            stroke(255, 0, 0);
            strokeWeight(1);
            for (Trajectory traj : TrajShow) {
                beginShape();
                for (Location loc : traj.points) {
                    ScreenPosition pos = map.getScreenPosition(loc);
                    vertex(pos.x, pos.y);
                }
                endShape();
            }
            TotalCost += System.currentTimeMillis() - t1;
            CurTimes += 1;
            if (CurTimes >= AVERAGETIMES) {
                String str = "rate" + RATELIST[rateId] + "_zoom" +
                        map.getZoomLevel() + "_delta" + DELTALIST[cellSetPx2] + "_centerP" + (centerIndex + 7);
                PNGPATH = temppaht + "_zoom" + map.getZoomLevel() + "/P" + (centerIndex + 7) + "/" + id + "_" + str + "_time" + TotalCost / CurTimes + ".png";
                saveFrame(PNGPATH);
                System.out.println("time used: " + TotalCost / CurTimes);
                noLoop();
                if (recNum == 2) {
                    TrajShow.clear();
                    CellCover.clear();

                    HashMap<Integer, Double> map1 = deltaCellList.get(cellSetPx)[cellSetPy];
                    cellSetPx2 = cellSetPx;
                    System.out.println(cellSetPx + "," + cellSetPy);
                    for (Map.Entry<Integer, Double> entry : map1.entrySet()) {
                        CellCover.add(entry.getKey());
                    }
                    CellCoverTopK(CellCover);

                    rateId = cellSetPy;
                    if (cellSetPy == RATELIST.length - 1) {
                        cellSetPx++;
                        cellSetPy = 0;
                    } else {
                        cellSetPy++;
                    }
                    if (cellSetPx == DELTALIST.length) {
                        recNum = 3;
//                        rateId = 0;
                    }

                    CurTimes = 0;
                    TotalCost = 0L;
                    loop();
                } else if (recNum == 0) {//random
                    rateId = ranNum;
                    Random((int) (RATELIST[ranNum] * TrajTotal.size()));
                    ranNum++;
                    if (ranNum == RATELIST.length) {
                        recNum++;
//                        rateId = 0;
                    }
                    CurTimes = 0;
                    TotalCost = 0L;
                    loop();
                } else if (recNum == 1) {
                    Total();
                    recNum = 2;
                    CurTimes = 0;
                    TotalCost = 0L;
                    loop();
                } else if (recNum == 3) {
                    if (ZOOMLEVEL == 16 && centerIndex == centerList.length - 1) {
                        System.out.println("done!");
                    } else {
                        if (centerIndex == centerList.length - 1) {
                            ZOOMLEVEL++;
                            centerIndex = 0;
                        } else {
                            centerIndex++;
                        }
                        map.zoomAndPanTo(ZOOMLEVEL, centerList[centerIndex]);
                        TotalLoad = false;
                        ranNum = 0;
                        cellSetPx2 = 0;
                        PNGPATH = "data/pic/test.png";
                        cellSetPx = 0;
                        cellSetPy = 0;
                        rateId = 0;
                        recNum = 0;
                        CurTimes = 0;
                        TotalCost = 0L;
                        loop();
                    }
                }
            }
        }
    }


    private void Total() {
        id = "total";
        TrajShow.clear();
        TrajShow.addAll(TrajTotal);
        System.out.println("total begin render");
    }

    private String id = "";

    private void Random(int KSIZE) {
        id = "random";
        int ranScore = 0;
        TrajShow.clear();
        Random r1 = new Random(1);
        for (int i = 0; i < KSIZE; i++) {
            int k = r1.nextInt(TrajTotal.size());
            if (k >= TrajTotal.size()) {
                i--;
                continue;
            }
            ranScore += TrajTotal.get(k).getScore();
            TrajShow.add(TrajTotal.get(k));
        }
        System.out.println("random score: " + ranScore + ", random begin render");
    }

    private void CellCoverTopK(HashSet<Integer> CellCover) {
        id = "greedy";
        TrajShow.clear();
        for (Integer id : CellCover) {
            TrajShow.add(TrajTotal.get(id));
        }

    }


    public static void main(String[] args) {
        Config config = new Config();
        CmdlineParser cp = new CmdlineParser(config);
        cp.setAggregateShortOptionsWithPrefix("-");
        cp.setProgramName("Screenshot");
        cp.parse(args);

        if (config.help) {
            cp.usage();
        } else if (config.dataset == null || config.vfgs == null) {
            System.err.println("Dataset file path and VFGS/VFGS+ result file path is necessary!!!");
        } else {
            type = config.type;
            try {
                preProcess(config.dataset, config.vfgs);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String title = "origin.util.screenShot";
            PApplet.main(new String[]{title});
        }
    }
}