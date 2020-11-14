package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import draw.TrajDrawHandler2;
import model.Trajectory;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.DM;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Draw a FULL/<s>VFGS</s>/RAND traj map with multi-thread.
 * <p>
 * ThreadMapApp + support for switching in FULL/<s>VFGS</s>/RAND
 * (by keyboard input 1/2/3).
 * FULL/VFGS use loaded traj data with score. RAND peeks
 * traj without repetition from FULL.
 * <p>
 * JVM options: -Xmn2048m -Xms8192m -Xmx8192m
 *
 * @see DM
 * @see TrajDrawHandler2
 */
public class ThreadMapAppPlus extends PApplet {

	private static final String APP_NAME = "ThreadMapAppPlus";
	private static final String DATA_PATH = "data/porto_full.txt";
	// traj limit when loading. -1 for no limit
	public static final int LIMIT = 1_0000;
	public static final int PEEK_SIZE = LIMIT / 1000;
	// recommend: core # * 2 or little higher
	public static final int THREAD_NUM = 10;

	public static final int WIDTH = 1200;
	public static final int HEIGHT = 800;

	// map params
	private static final Location PORTO_CENTER = new Location(41.14, -8.639);
	private static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

	private UnfoldingMap map;
	private Trajectory[] trajShowNow = null;
	private Trajectory[] trajFull = null;
	private Trajectory[] trajVfgs = null;
	private Trajectory[] trajRand = null;

	// var to check changes
	private int nowZoomLevel;
	private Location nowCenter;
	private boolean mouseDown;

	// multi-thread for part image painting
	private final ExecutorService threadPool;
	private final TrajDrawHandler2[] handlers = new TrajDrawHandler2[THREAD_NUM];

	// single thread pool for images controlling (may be redundant ?)
	private final ExecutorService controlPool;
	private Thread controlThread;

	private final PGraphics[] trajImages = new PGraphics[THREAD_NUM];
	private final Vector<Long> newestId = new Vector<>(Collections.singletonList(0L));
	private final int[] trajCnt = new int[THREAD_NUM];


	public ThreadMapAppPlus() {
		// init pool
		threadPool = new ThreadPoolExecutor(THREAD_NUM, THREAD_NUM, 0L, TimeUnit.MILLISECONDS,
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
	 * Load trajectory data from file (FULL)
	 * Then generate VFGS and RAND
	 */
	public void loadTraj() {
		// load features from text
		DM.loadRowData(DATA_PATH, LIMIT);
		trajFull = DM.trajFull;
		if (trajFull == null) { exit(); }
		System.out.println("Total load: " + trajFull.length);

		if (trajFull.length <= PEEK_SIZE) {
			// show all
			trajVfgs = trajFull;
			return;
		}

		// generate VFGS (temp version)
		trajVfgs = Arrays.copyOfRange(trajFull, 0, PEEK_SIZE);

		// generate RAND
		trajRand = new Trajectory[PEEK_SIZE];
		HashSet<Integer> set = new HashSet<>();
		Random rand = new Random();
		int cnt = 0;
		while (cnt < PEEK_SIZE) {
			int idx = rand.nextInt(trajFull.length);
			if (set.contains(idx)) {
				continue;
			}
			set.add(idx);
			trajRand[cnt++] = trajFull[idx];
		}

		// default
		trajShowNow = trajVfgs;
	}

	public void refreshTitle() {
		int loadCnt = 0;    // # of painted traj
		int partCnt = 0;    // # of part images that have finished painting.
		for (int i = 0; i < THREAD_NUM; ++i) {
			partCnt += (trajImages[i] == null) ? 0 : 1;
			loadCnt += trajCnt[i];
		}
		double percentage = (trajShowNow == null) ?
				0.0 : Math.min(100.0, 100.0 * loadCnt / trajShowNow.length);

		String shownSet = (trajShowNow == trajFull) ? "FULL" :
						(trajShowNow == trajVfgs) ? "VFGS" :
						(trajShowNow == trajRand) ? "RAND" : "----";

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

		if (trajShowNow != null) {
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
		if (trajShowNow != null && !nowCenter.equals(map.getCenter())) {
			// was dragged and center changed
			mouseDown = false;
			updateTrajImages();
		}
	}

	/**
	 * Start multi-thread (by start a control thread)
	 * and paint traj to flash images separately.
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
						trajImages, trajShowNow, newestId, false, 0, 0, trajCnt,
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
		controlPool.shutdownNow();
	}

	@Override
	public void keyReleased() {
		switch (key) {
			case 'q':
				exit();
				break;
			case '1':
				if (trajShowNow != trajFull) {
					trajShowNow = trajFull;
					updateTrajImages();
				}
				break;
			case '2':
				if (trajShowNow != trajVfgs) {
					trajShowNow = trajVfgs;
					updateTrajImages();
				}
				break;
			case '3':
				if (trajShowNow != trajRand) {
					trajShowNow = trajRand;
					updateTrajImages();
				}
				break;
		}
	}

	public static void main(String[] args) {
		PApplet.main(new String[]{ThreadMapAppPlus.class.getName()});
	}
}