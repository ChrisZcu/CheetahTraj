package vqgs.app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import vqgs.draw.TrajDrawHandler2;
import vqgs.model.Trajectory;
import processing.core.PApplet;
import processing.core.PGraphics;
import vqgs.util.DM;

import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Draw a full-data traj map with multi-thread.
 * <p>
 * Use {@link DM} to load data.<br>
 * Here, <b>the traj score is not used</b>.
 * <b>The stuck problem is almost solved here</b>.
 * <s>However, you should avoid operate too fast. (reason unknown)</s>
 * This bug is fixed.
 * <p>
 * JVM options: -Xmn2048m -Xms8192m -Xmx8192m
 *
 * @see TrajDrawHandler2
 */
public class ThreadMapApp extends PApplet {
    private static final String APP_NAME = "ThreadMapApp";
    private static final String DATA_PATH
//			= "data/porto_full.txt";
            = "C:\\LocalDocument\\LocalDBGroup\\origin_data\\clean_sz.txt";
    // traj limit when loading. -1 for no limit
    public static final int LIMIT = -1;
    // recommend: core # * 2 or little higher
    public static final int THREAD_NUM = 10;

    public static final int WIDTH = 1200;
    public static final int HEIGHT = 800;

    // map params
    private static final Location PORTO_CENTER
//			= new Location(41.14, -8.639);          // porto
//			= new Location(30.730, 104.038);        // chengdu
            = new Location(22.577456, 113.97001);   // shenzhen

    private static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    private UnfoldingMap map;
    private Trajectory[] trajList = null;

    private int pid;
    private static final Location[] CENTER = {
            /* porto */
//			new Location(41.14, -8.639),        // row 1 & 2.
//			new Location(41.277, -8.281),       // row 3
//			new Location(41.140556, -8.642269), // fig 4
//			new Location(41.215004, -8.487775), // fig 6
//			new Location(41.20363, -8.303903),   // fig 6 right
//			new Location(41.193108, -8.520111),  // fig 6 left below
//			new Location(41.1437, -8.638987),    // fig 6 right below
            /*new Location()*/
            /* shenzhen*/
            new Location(22.637138, 113.836464),    // row 1, level=11
            new Location(22.640322, 113.835434),    // row 2, level=13
            new Location(22.629078, 114.02894),     // row 3, level=15
    };


    // var to check changes
    private int nowZoomLevel;
    private Location nowCenter;
    private boolean mouseDown;

    // multi-thread for part image painting
    private final ExecutorService threadPool = new ThreadPoolExecutor(THREAD_NUM, THREAD_NUM, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {{
        // drop last thread
        this.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }};
    private final TrajDrawHandler2[] handlers = new TrajDrawHandler2[THREAD_NUM];

    // single thread pool for images controlling (may be redundant ?)
    private final ExecutorService controlPool = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {{
        // drop last thread and begin next
        this.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }};
    private Thread controlThread;

    private final PGraphics[] trajImages = new PGraphics[THREAD_NUM];
    private final Vector<Long> newestId = new Vector<>(Collections.singletonList(0L));
    private final int[] trajCnt = new int[THREAD_NUM];

    @Override
    public void settings() {
        // window settings
        size(WIDTH, HEIGHT, P2D);
    }

    @Override
    public void setup() {
        // frame settings
        textSize(20);

        // map settings
        map = new UnfoldingMap(this, APP_NAME, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));
        map.setZoomRange(1, 20);
        map.zoomAndPanTo(12, PORTO_CENTER);
        map.setBackgroundColor(255);

        // thread("loadTraj");     // looks not good
        (new Thread(this::loadTraj)).start();

        // add mouse and keyboard interactions
        MapUtils.createDefaultEventDispatcher(this, map);

        refreshTitle();
    }

    /**
     * Load trajectory data from file and draw it on the map.
     */
    public void loadTraj() {
        // load features from text
        DM.loadRowData(DATA_PATH, LIMIT);
        trajList = DM.trajFull;
        if (trajList == null) {
            exit();
            return;
        }
        System.out.println("Total load: " + trajList.length);
    }

    public void refreshTitle() {
        int loadCnt = 0;    // # of painted traj
        int partCnt = 0;    // # of part images that have finished painting.
        for (int i = 0; i < THREAD_NUM; ++i) {
            partCnt += (trajImages[i] == null) ? 0 : 1;
            loadCnt += trajCnt[i];
        }
        double percentage = (trajList == null) ?
                0.0 : Math.min(100.0, 100.0 * loadCnt / trajList.length);

        String str = String.format(APP_NAME + " [%5.2f fps] [%.1f%% loaded] " +
                        "[%d parts] [zoom=%d] [pid=%d] ",
                frameRate, percentage, partCnt, map.getZoomLevel(), pid);

        if (trajList == null) {
            // reading data
            surface.setTitle(str + "Reading data from file...");
            return;
        }

        if (partCnt != THREAD_NUM) {
            // painting traj
            surface.setTitle(str + "Painting trajectories...");
            return;
        }

        if (!map.allTilesLoaded()) {
            str += "Loading map...";
        } else {
            str += "Completed";
        }
        surface.setTitle(str);
    }

    @Override
    public void draw() {
        refreshTitle();
        map.draw();

        if (trajList != null) {
            boolean changed = nowZoomLevel != map.getZoomLevel()
                    || !nowCenter.equals(map.getCenter());
            if (changed) {
                // use for delay verifying
                mouseDown = mousePressed;

                if (!mouseDown) {
                    // is zoom. update now
                    // otherwise update when mouse is released
                    updateTrajImages();
                }

                // drop all traj images
                Arrays.fill(trajImages, null);
                Arrays.fill(trajCnt, 0);
            }
            // show all valid traj images
            for (PGraphics pg : trajImages) {
                if (pg != null) {
                    image(pg, 0, 0);
                }
            }
        }
    }

    @Override
    public void mouseReleased() {
        if (trajList != null && !nowCenter.equals(map.getCenter())) {
            // was dragged and center changed
            mouseDown = false;
            updateTrajImages();
        }
    }

    /**
     * Start multi-thread and paint traj to flash images separately
     */
    public void updateTrajImages() {
        // update mark
        nowZoomLevel = map.getZoomLevel();
        nowCenter = map.getCenter();
        newestId.set(0, newestId.get(0) + 1);

        if (controlThread != null) {
            controlThread.interrupt();
        }

        // create new control thread
        controlThread = new Thread(() -> {
            // kill all exist threads
            for (TrajDrawHandler2 tdh : handlers) {
                if (tdh != null) {
                    tdh.interrupt();
                }
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }

            // add new threads
            for (int i = 0; i < THREAD_NUM; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                trajImages[i] = null;
                TrajDrawHandler2 tdh = new TrajDrawHandler2(map, createGraphics(WIDTH, HEIGHT),
                        trajImages, trajList, newestId, false, 0, 0, trajCnt,
                        i, THREAD_NUM, newestId.get(0));
                handlers[i] = tdh;
                threadPool.submit(tdh);
            }
        });

        controlThread.setPriority(10);
        controlPool.submit(controlThread);
    }

    @Override
    public void exit() {
        super.exit();
        threadPool.shutdownNow();
    }

    @Override
    public void keyReleased() {
        switch (key) {
            case 'q':
                exit();
                return;
        }
        customKeyReleased();
    }

    private void customKeyReleased() {
        switch (key) {
			/*case 'p':       // save pic
				String path = PATH_PREFIX + "\\delta" + deltaList[dPtr]
						+ "_rate" + (rateList[rPtr])
						+ "_zoom" + map.getZoomLevel()
						+ "p" + pid + ".png";
				saveFrame(path);
				System.out.println(path);
				pid++;
				return;*/
            case ',':       // next center
                if (pid != 0) {
                    map.panTo(CENTER[pid]);
                    pid--;
                    updateTrajImages();
                }
                return;
            case '.':       // last center
                if (pid != CENTER.length - 1) {
                    map.panTo(CENTER[pid]);
                    pid++;
                    updateTrajImages();
                }
                return;
            case '/':       // reset to cur center
                map.panTo(CENTER[pid]);
                updateTrajImages();
                return;
            case '-':       // zoom out
                if (map.getZoomLevel() > 11) {
                    map.zoomToLevel(map.getZoomLevel() - 1);
                    updateTrajImages();
                }
                return;
            case '=':       // zoom in
                if (map.getZoomLevel() < 20) {
                    map.zoomToLevel(map.getZoomLevel() + 1);
                    updateTrajImages();
                }
                return;
            case 'z':
                map.zoomToLevel(15);
                updateTrajImages();
                return;
            case 'x':
                map.zoomToLevel(11);
                updateTrajImages();
                return;
            case 'o':
                Location loc = map.getCenter();
                System.out.println(loc.x + " " + loc.y);
                return;
            default:
                System.out.println("Null-opt. key=" + key
                        + " keyCode=" + keyCode);
        }
    }

    public static void main(String[] args) {
        PApplet.main(ThreadMapApp.class.getName());
    }
}