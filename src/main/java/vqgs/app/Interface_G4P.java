package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.events.EventDispatcher;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.interactions.MouseHandler;
import de.fhpotsdam.unfolding.providers.MapBox;
import draw.TrajDrawHandler;
import g4p_controls.*;
import model.Trajectory;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.DM;
import util.PSC;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static util.PSC.*;

//import g4p_controls.GCheckbox;

public class Interface_G4P extends PApplet {
    private static final String APP_NAME = "VFGSMapApp";

    //public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int WIDTH = 1536;
    public static final int HEIGHT = 864;
    public static final int subHeight = (HEIGHT - 20 - 10 - 14) / 2;
    public static final int subWidth = (WIDTH - 290 - 10 - 16) / 2;

    public static final int FULL_THREAD_NUM = PSC.THREAD_NUM;

    private static final Location PORTO_CENTER = new Location(41.14, -8.639);

    //private UnfoldingMap map;
    private Trajectory[] trajShowNow = null;
    private Trajectory[] trajFull = null;
    private Trajectory[] trajVfgs = null;
    private Trajectory[] trajPlus = null;
    private Trajectory[] trajRand = null;

    // traj color shader
    /*private final TrajShader shader = new TrajShader(BREAKS, COLORS);*/
    /*private boolean colored;*/

    // var to check changes
    private int nowZoomLevel;
    private Location nowCenter;
    private int nowMapNum;
    private boolean mouseDown;

    // multi-thread for part image painting
    private final ExecutorService threadPool;
    private final TrajDrawHandler[] handlers = new TrajDrawHandler[FULL_THREAD_NUM + 3];

    // single thread pool for images controlling
    private final ExecutorService controlPool;
    private Thread controlThread;

    private final PGraphics[] trajImages = new PGraphics[FULL_THREAD_NUM + 3];
    private final Vector<Long> newestId = new Vector<>(Collections.singletonList(0L));//////////
    private final int[] trajCnt = new int[FULL_THREAD_NUM + 3];

    GPanel configPanel;
    GLabel data;
    GToggleGroup dataType;
    GOption proto;
    GOption shenzhen;
    GLabel dataFeature;
    GLabel trajNumber;
    GLabel pointNumber;
    GLabel pointSampling;
    GCheckbox fullDataset;
    GCheckbox uniformRandom;
    GCheckbox vfgs;
    GCheckbox vfgsPlus;
    GLabel lenthDistribution;
    GImageButton kde;
    GLabel method;
    GLabel configLable;
    GPanel mapPanel;
    GPanel map11;
    GLabel label112;
    GLabel label111;
    GPanel map12;
    GLabel label121;
    GLabel label122;
    GPanel map21;
    GLabel label211;
    GLabel label212;
    GPanel map22;
    GLabel label221;
    GLabel label222;
    GLabel mapLable;

    UnfoldingMap[] maps = new UnfoldingMap[4];
    boolean[] isMapChosen = {false, false, false, false};
    int[] mapOfMethods = {-1, -1, -1, -1};
    boolean[] isTrajDrawed = {false, false, false, false};

    public static void main(String[] args) {
        PApplet.main(Interface_G4P.class.getName());
    }

    public Interface_G4P() {
        // init pool
        threadPool = new ThreadPoolExecutor(FULL_THREAD_NUM + 3, FULL_THREAD_NUM + 3, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {{
            // drop last thread
            this.setRejectedExecutionHandler(new DiscardOldestPolicy());
        }};
        controlPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {{
            // drop last thread and begin next
            this.setRejectedExecutionHandler(new DiscardOldestPolicy());
        }};
    }

    @Override
    public void settings() {
        size(WIDTH, HEIGHT, P2D);
    }

    @Override
    public void setup() {
        createGUI();

        maps[0] = new UnfoldingMap(this, 298, 28, subWidth, subHeight, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));
        maps[1] = new UnfoldingMap(this, 298 + subWidth + 10, 28, subWidth, subHeight, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));
        maps[2] = new UnfoldingMap(this, 298, 28 + subHeight + 10, subWidth, subHeight, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));
        maps[3] = new UnfoldingMap(this, 298 + subWidth + 10, 28 + subHeight + 10, subWidth, subHeight, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));
        for (int i = 0; i < 4; ++i) {
            /*maps[i].zoomToLevel(4);*/
            maps[i].setBackgroundColor((new Color(240, 240, 240)).getRGB());
            maps[i].zoomAndPanTo(12, PORTO_CENTER);
            maps[i].setZoomRange(1, 20);
        }

        // thread("loadTraj");     // looks not good
        (new Thread(this::initData)).start();

        EventDispatcher eventDispatcher = new EventDispatcher();

        //add mouse interaction
        MouseHandler mouseHandler = new MouseHandler(this, maps);
        eventDispatcher.addBroadcaster(mouseHandler);

        //let each interaction in one map reflect in other maps
        for (int i = 0; i < 4; ++i) {
            eventDispatcher.register(maps[i], "pan", maps[0].getId(), maps[1].getId(), maps[2].getId(), maps[3].getId());
            eventDispatcher.register(maps[i], "zoom", maps[0].getId(), maps[1].getId(), maps[2].getId(), maps[3].getId());
        }

        refreshTitle();
        background(255, 255, 255);
    }

    @Override
    public void draw() {
        refreshTitle();

        /*background(255, 255, 255);*/      // move to setup()

//        noStroke();
//        fill(90, 165, 198);//dark blue
//        rect(0, 0, 290, 20);
//        fill(172, 209, 228);//light blue
//        rect(0, 20, 290, 864);
//        fill(99, 214, 99);//dark CYAN
//        rect(290, 0, 1246, 20);
////        fill(179,237,179);//light CYAN
////        rect(290,20,1246,864);


        int cnt = 0;
        for (int j = 0; j < 4; ++j) {
            if (isMapChosen[j]) {
                cnt++;
                maps[j].draw();
            }
        }

        if (nowMapNum != cnt) {
            mouseDown = mousePressed;

            if (!mouseDown) {
                if (fullDataset.isSelected() && trajImages[0] == null) {
                    updateTrajImages(0);
                }
                if (uniformRandom.isSelected() && trajImages[10] == null) {
                    updateTrajImages(1);
                }
                if (vfgs.isSelected() && trajImages[11] == null) {
                    updateTrajImages(2);
                }
                if (vfgsPlus.isSelected() && trajImages[12] == null) {
                    updateTrajImages(3);
                }

                nowMapNum = cnt;
            }
        }

        boolean changed = nowZoomLevel != maps[0].getZoomLevel()
                || !nowCenter.equals(maps[0].getCenter());

        if (changed) {
            // use for delay verifying
            mouseDown = mousePressed;

            // drop all traj images
            Arrays.fill(trajImages, null);
            Arrays.fill(trajCnt, 0);

            if (!mouseDown) {
                if (fullDataset.isSelected()) {
                    updateTrajImages(0);
                }
                if (uniformRandom.isSelected()) {
                    updateTrajImages(1);
                }
                if (vfgs.isSelected()) {
                    updateTrajImages(2);
                }
                if (vfgsPlus.isSelected()) {
                    updateTrajImages(3);
                }
            }
        }

        for (int i = 0; i < trajImages.length; ++i) {
            if (trajImages[i] != null) {
                float x, y;
                if (i >= 10 && mapOfMethods[i - 9] != -1) {
                    x = maps[mapOfMethods[i - 9]].getScreenPosition(maps[mapOfMethods[i - 9]].getTopLeftBorder()).x;
                    y = maps[mapOfMethods[i - 9]].getScreenPosition(maps[mapOfMethods[i - 9]].getTopLeftBorder()).y;
                    image(trajImages[i], x, y);
                } else if (i < 10 && mapOfMethods[0] != -1) {
                    x = maps[mapOfMethods[0]].getScreenPosition(maps[mapOfMethods[0]].getTopLeftBorder()).x;
                    y = maps[mapOfMethods[0]].getScreenPosition(maps[mapOfMethods[0]].getTopLeftBorder()).y;
                    image(trajImages[i], x, y);
                }
            }
        }
    }

    /**
     * Load trajectory data from file (FULL)
     * Then generate VFGS and RAND
     */
    public void initData() {
        // load data from files
        boolean success = DM.loadRowData(ORIGIN_PATH, LIMIT)
                && DM.loadResList(RES_PATH, DELTA_LIST, RATE_LIST)
                && DM.loadRepScoresMatrix(COLOR_PATH, DELTA_LIST, RATE_LIST);

        if (!success) {
            exit();
            return;
        }

        trajFull = DM.trajFull;
        int[][] vfgsResList = DM.vfgsResList;
        int[][][] repScoresMatrix = DM.repScoresMatrix;
        int[] rateCntList = DM.translateRate(trajFull.length, PSC.RATE_LIST);
        int dIdx = 0, rIdx = 0;     // this is the VFGS settings
        // load trajVfgs
        trajVfgs = new Trajectory[rateCntList[rIdx]];
        for (int i = 0; i < rateCntList[rIdx]; i++) {
            trajVfgs[i] = trajFull[vfgsResList[dIdx][i]];
        }
        // load trajPlus and set color score (reuse score field)
        dIdx = 1;       // this is the VFGS+ settings
        trajPlus = new Trajectory[rateCntList[rIdx]];
        int[] repScores = repScoresMatrix[dIdx][rIdx];
        for (int i = 0; i < rateCntList[rIdx]; i++) {
            trajPlus[i] = trajFull[vfgsResList[dIdx][i]];
            trajPlus[i].setScore(repScores[i]);
        }

        // rank by colorScore for easy drawing
        Arrays.sort(trajPlus, Comparator.comparingInt(Trajectory::getScore));

        // generate RAND
        trajRand = new Trajectory[trajVfgs.length];
        HashSet<Integer> set = new HashSet<>();
        Random rand = new Random();
        int cnt = 0;
        while (cnt < trajRand.length) {
            int idx = rand.nextInt(trajFull.length);
            if (set.contains(idx)) {
                continue;
            }
            set.add(idx);
            trajRand[cnt++] = trajFull[idx];
        }
    }

    public void updateTrajImages(int num) {
        // update mark
        nowZoomLevel = maps[0].getZoomLevel();
        nowCenter = maps[0].getCenter();
        newestId.set(0, newestId.get(0) + 1);

        if (controlThread != null) {
            controlThread.interrupt();
        }

        // create new control thread
        controlThread = new Thread(() -> {
            // kill all exist threads
            for (TrajDrawHandler tdh : handlers) {
                if (tdh != null) {
                    tdh.interrupt();
                }
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }

            // add new threads
            TrajDrawHandler tdh;
            if (num == 0) {
                for (int i = 0; i < FULL_THREAD_NUM; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    trajImages[i] = null;

                    tdh = new TrajDrawHandler(maps[mapOfMethods[num]],
                            createGraphics(subWidth, subHeight),
                            trajImages, trajFull, newestId, false, BREAKS, COLORS, trajCnt,
                            i, i, FULL_THREAD_NUM, newestId.get(0));
                    handlers[i] = tdh;
                    threadPool.submit(tdh);
                }
            } else if (num == 1) {
                trajImages[10] = null;

                tdh = new TrajDrawHandler(maps[mapOfMethods[num]],
                        createGraphics(subWidth, subHeight),
                        trajImages, trajRand, newestId, false, BREAKS, COLORS, trajCnt,
                        10, 0, 1, newestId.get(0));
                handlers[10] = tdh;
                threadPool.submit(tdh);
            } else if (num == 2) {
                trajImages[11] = null;

                tdh = new TrajDrawHandler(maps[mapOfMethods[num]],
                        createGraphics(subWidth, subHeight),
                        trajImages, trajVfgs, newestId, false, BREAKS, COLORS, trajCnt,
                        11, 0, 1, newestId.get(0));
                handlers[11] = tdh;
                threadPool.submit(tdh);
            } else if (num == 3) {
                trajImages[12] = null;

                tdh = new TrajDrawHandler(maps[mapOfMethods[num]],
                        createGraphics(subWidth, subHeight),
                        trajImages, trajPlus, newestId, true, BREAKS, COLORS, trajCnt,
                        12, 0, 1, newestId.get(0));
                handlers[12] = tdh;
                threadPool.submit(tdh);
            }
        });

        controlThread.setPriority(10);
        controlPool.submit(controlThread);
    }

    public void refreshTitle() {
        int loadCnt = 0;    // # of painted traj
        int partCnt = 0;    // # of part images that have finished painting.
        for (int i = 0; i < FULL_THREAD_NUM; ++i) {
            partCnt += (trajImages[i] == null) ? 0 : 1;
            loadCnt += trajCnt[i];
        }
        double percentage = (trajShowNow == null) ?
                0.0 : Math.min(100.0, 100.0 * loadCnt / trajShowNow.length);

        String shownSet = (trajShowNow == trajFull) ? "FULL" :
                (trajShowNow == trajVfgs) ? "VFGS" :
                        (trajShowNow == trajRand) ? "RAND" :
                                (trajShowNow == trajPlus) ? "VFGS+" : "----";

        // main param str shown in title
        String str = String.format(APP_NAME + " [%5.2f fps] [%s] [%.1f%% loaded] [%d parts] ",
                frameRate, shownSet, percentage, partCnt);

        if (trajShowNow == null) {
            // generating traj data

            String dataName;
            if (trajFull == null) {
                dataName = "FULL";
            } else if (trajVfgs == null) {
                dataName = "VFGS";
            } else if (trajRand == null) {
                dataName = "RAND";
            } else {
                dataName = "----";
            }

            surface.setTitle(str + "Generate " + dataName + " set...");
            return;
        }

        if (partCnt != FULL_THREAD_NUM) {
            // painting traj
            surface.setTitle(str + "Painting trajectories...");
            return;
        }

        if (!maps[0].allTilesLoaded()) {
            str += "Loading map...";
        } else {
            str += "Completed";
        }
        surface.setTitle(str);
    }

    @Override
    public void exit() {
        super.exit();
        threadPool.shutdownNow();
        controlPool.shutdownNow();
    }

    @Override
    public void mouseReleased() {
        if (trajShowNow != null && !nowCenter.equals(maps[0].getCenter())) {
            // was dragged and center changed
            mouseDown = false;

            if (fullDataset.isSelected()) {
                updateTrajImages(0);
            }
            if (uniformRandom.isSelected()) {
                updateTrajImages(1);
            }
            if (vfgs.isSelected()) {
                updateTrajImages(2);
            }
            if (vfgsPlus.isSelected()) {
                updateTrajImages(3);
            }
        }
    }


    public void protoClicked(GOption source, GEvent event) {
    }

    public void shenzhenClicked(GOption source, GEvent event) {
    }

    public void fullDatasetClicked(GCheckbox source, GEvent event) {
        if (fullDataset.isSelected()) {
            for (int i = 0; i < 4; ++i) {
                if (!isMapChosen[i] && mapOfMethods[0] == -1) {
                    isMapChosen[i] = true;
                    mapOfMethods[0] = i;
                    setLabelText(i, "Method: Full dataset", 0.05);
                    break;
                }
            }
        } else {
            isMapChosen[mapOfMethods[0]] = false;
            setLabelText(mapOfMethods[0], "Method: ", 0.05);
            mapOfMethods[0] = -1;
        }
    }

    public void uniformRandomClicked(GCheckbox source, GEvent event) {
        if (uniformRandom.isSelected()) {
            for (int i = 0; i < 4; ++i) {
                if (!isMapChosen[i] && mapOfMethods[1] == -1) {
                    isMapChosen[i] = true;
                    mapOfMethods[1] = i;
                    setLabelText(i, "Method: Uniform Random", 0.05);
                    break;
                }
            }
        } else {
            isMapChosen[mapOfMethods[1]] = false;
            setLabelText(mapOfMethods[1], "Method: ", 0.05);
            mapOfMethods[1] = -1;
        }
    }

    public void vfgsClicked(GCheckbox source, GEvent event) {
        if (vfgs.isSelected()) {
            for (int i = 0; i < 4; ++i) {
                if (!isMapChosen[i] && mapOfMethods[2] == -1) {
                    isMapChosen[i] = true;
                    mapOfMethods[2] = i;
                    setLabelText(i, "Method: VFGS", 0.05);
                    break;
                }
            }
        } else {
            isMapChosen[mapOfMethods[2]] = false;
            setLabelText(mapOfMethods[2], "Method: ", 0.05);
            mapOfMethods[2] = -1;
        }
    }

    public void vfgsPlusClicked(GCheckbox source, GEvent event) {
        if (vfgsPlus.isSelected()) {
            for (int i = 0; i < 4; ++i) {
                if (!isMapChosen[i] && mapOfMethods[3] == -1) {
                    isMapChosen[i] = true;
                    mapOfMethods[3] = i;
                    setLabelText(i, "Method: VFGS+", 0.05);
                    break;
                }
            }
        } else {
            isMapChosen[mapOfMethods[3]] = false;
            setLabelText(mapOfMethods[3], "Method: ", 0.05);
            mapOfMethods[3] = -1;
        }
    }

    public void setLabelText(int i, String text, double rate) {
        if (i == 0) {
            label111.setText(text);
            label112.setText("Sampling rate = " + rate + "%");
        } else if (i == 1) {
            label121.setText(text);
            label122.setText("Sampling rate = " + rate + "%");
        } else if (i == 2) {
            label211.setText(text);
            label212.setText("Sampling rate = " + rate + "%");
        } else if (i == 3) {
            label221.setText(text);
            label222.setText("Sampling rate = " + rate + "%");
        }
    }

    public void createGUI() {
        G4P.messagesEnabled(false);
        G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
        G4P.setMouseOverEnabled(false);
        surface.setTitle("Sketch Window");
        configPanel = new GPanel(this, 0, 0, 290, 864, "Config panel");
        configPanel.setCollapsible(false);
        configPanel.setText("Config panel");
        configPanel.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        configPanel.setOpaque(false);
        data = new GLabel(this, 8, 28, 274, 25);
        data.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
        data.setText("Data");
        data.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        data.setOpaque(true);
        dataType = new GToggleGroup();
        proto = new GOption(this, 8, 62, 200, 20);
        proto.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
        proto.setText("Proto traffic data");
        proto.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        proto.setOpaque(false);
        proto.addEventHandler(this, "protoClicked");
        shenzhen = new GOption(this, 7, 92, 200, 20);
        shenzhen.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
        shenzhen.setText("Shenzhen traffic data");
        shenzhen.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        shenzhen.setOpaque(false);
        shenzhen.addEventHandler(this, "shenzhenClicked");
        dataType.addControl(proto);
        proto.setSelected(true);
        configPanel.addControl(proto);
        dataType.addControl(shenzhen);
        configPanel.addControl(shenzhen);
        dataFeature = new GLabel(this, 8, 123, 274, 25);
        dataFeature.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
        dataFeature.setText("Data feature");
        dataFeature.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        dataFeature.setOpaque(true);
        trajNumber = new GLabel(this, 8, 158, 200, 20);
        trajNumber.setText("• Trajectory number: 2389863");
        trajNumber.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        trajNumber.setOpaque(false);
        pointNumber = new GLabel(this, 8, 416, 200, 20);
        pointNumber.setText("• Points number: 78057366");
        pointNumber.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        pointNumber.setOpaque(false);
        pointSampling = new GLabel(this, 8, 446, 200, 20);
        pointSampling.setText("• Sampling points per km: 5.82"); //0.005820297573731055
        pointSampling.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        pointSampling.setOpaque(false);
        fullDataset = new GCheckbox(this, 8, 508, 200, 20);
        fullDataset.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
        fullDataset.setText("Full dataset");
        fullDataset.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        fullDataset.setOpaque(false);
        fullDataset.addEventHandler(this, "fullDatasetClicked");
        uniformRandom = new GCheckbox(this, 8, 538, 200, 20);
        uniformRandom.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
        uniformRandom.setText("Uniform random");
        uniformRandom.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        uniformRandom.setOpaque(false);
        uniformRandom.addEventHandler(this, "uniformRandomClicked");
        vfgs = new GCheckbox(this, 8, 568, 120, 20);
        vfgs.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
        vfgs.setText("VFGS");
        vfgs.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        vfgs.setOpaque(false);
        vfgs.addEventHandler(this, "vfgsClicked");
        vfgsPlus = new GCheckbox(this, 8, 598, 120, 20);
        vfgsPlus.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
        vfgsPlus.setText("VFGS+");
        vfgsPlus.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        vfgsPlus.setOpaque(false);
        vfgsPlus.addEventHandler(this, "vfgsPlusClicked");
        lenthDistribution = new GLabel(this, 8, 190, 200, 20);
        lenthDistribution.setText("• Lenth distribution: ");
        lenthDistribution.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        lenthDistribution.setOpaque(false);
        kde = new GImageButton(this, 8, 210, 274, 193, new String[]{"image/KDE_proto_full.png", "image/KDE_proto_full.png", "image/KDE_proto_full.png"});
        method = new GLabel(this, 8, 476, 274, 20);
        method.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
        method.setText("Method");
        method.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        method.setOpaque(true);
        configLable = new GLabel(this, 0, 0, 80, 20);
        configLable.setText("Config panel");
        configLable.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        configLable.setOpaque(false);
        configPanel.addControl(data);
        configPanel.addControl(dataFeature);
        configPanel.addControl(trajNumber);
        configPanel.addControl(pointNumber);
        configPanel.addControl(pointSampling);
        configPanel.addControl(fullDataset);
        configPanel.addControl(uniformRandom);
        configPanel.addControl(vfgs);
        configPanel.addControl(vfgsPlus);
        configPanel.addControl(lenthDistribution);
        configPanel.addControl(kde);
        configPanel.addControl(method);
        configPanel.addControl(configLable);
        mapPanel = new GPanel(this, 290, 0, 1246, 864, "Map panel");
        mapPanel.setCollapsible(false);
        mapPanel.setText("Map panel");
        mapPanel.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        mapPanel.setOpaque(false);
        map11 = new GPanel(this, 8, 27, 610, 410, "map1");
        map11.setCollapsible(false);
        map11.setText("map1");
        map11.setLocalColorScheme(GCScheme.SCHEME_8);
        map11.setOpaque(false);
        label112 = new GLabel(this, 0, 20, 200, 20);
        label112.setText("Sampling rate = ");
        label112.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label112.setOpaque(true);
        label111 = new GLabel(this, 0, 0, 200, 20);
        label111.setText("Method name: ");
        label111.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label111.setOpaque(true);
        map11.addControl(label112);
        map11.addControl(label111);
        map12 = new GPanel(this, 628, 27, 610, 410, "map2");
        map12.setCollapsible(false);
        map12.setText("map2");
        map12.setLocalColorScheme(GCScheme.SCHEME_8);
        map12.setOpaque(false);
        label121 = new GLabel(this, 0, 0, 200, 20);
        label121.setText("Method name: ");
        label121.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label121.setOpaque(true);
        label122 = new GLabel(this, 0, 20, 200, 20);
        label122.setText("Sampling rate = ");
        label122.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label122.setOpaque(true);
        map12.addControl(label121);
        map12.addControl(label122);
        map21 = new GPanel(this, 8, 447, 610, 410, "map3");
        map21.setCollapsible(false);
        map21.setText("map3");
        map21.setLocalColorScheme(GCScheme.SCHEME_8);
        map21.setOpaque(false);
        label211 = new GLabel(this, 0, 0, 200, 20);
        label211.setText("Method name: ");
        label211.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label211.setOpaque(true);
        label212 = new GLabel(this, 0, 20, 200, 20);
        label212.setText("Sampling rate = ");
        label212.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label212.setOpaque(true);
        map21.addControl(label211);
        map21.addControl(label212);
        map22 = new GPanel(this, 628, 447, 610, 410, "Tab bar text");
        map22.setCollapsible(false);
        map22.setText("Tab bar text");
        map22.setLocalColorScheme(GCScheme.SCHEME_8);
        map22.setOpaque(false);
        label221 = new GLabel(this, 0, 0, 200, 20);
        label221.setText("Method name: ");
        label221.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label221.setOpaque(true);
        label222 = new GLabel(this, 0, 20, 200, 20);
        label222.setText("Sampling rate = ");
        label222.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        label222.setOpaque(true);
        map22.addControl(label221);
        map22.addControl(label222);
        mapLable = new GLabel(this, 0, 0, 80, 20);
        mapLable.setText("Map panel");
        mapLable.setLocalColorScheme(GCScheme.CYAN_SCHEME);
        mapLable.setOpaque(false);
        mapPanel.addControl(map11);
        mapPanel.addControl(map12);
        mapPanel.addControl(map21);
        mapPanel.addControl(map22);
        mapPanel.addControl(mapLable);
    }
}


