package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import draw.TrajDrawHandler2;
import draw.TrajShader;
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

/**
 * Draw a FULL / VFGS & VFGS+ / VFGS+(CE) / RAND traj map with multi-thread.
 * Support for switching in each mode.
 * Now, all the data with diff delta and simple rate will be loaded
 * <p>
 * Interaction:
 * <ul><li>'1/2/3/4': change data set.</li>
 * <li>'W/S/A/D': change delta or rate by keyboard input .</li>
 * <li>',' & '.': change to preset center {@link #presetCenter}[pid].</li>
 * <li>'/': reset to preset center of current pid.</li>
 * <li>'O': print location of center.</li>
 * <li>'P': get a screenshot and save to {@link PSC#PATH_PREFIX}.</li>
 * <li>'Esc': quit.</li></ul>
 * <p>
 * Use settings in {@link PSC} and these existed files:
 * <ul><li>{@link PSC#ORIGIN_PATH}: Raw data</li>
 * <li>{@link PSC#RES_PATH}: VFGS / VFGS+ result</li>
 * <li>{@link PSC#COLOR_PATH}: Color encoding result</li></ul>
 * <p>
 * JVM options:
 * -Xmn2048m -Xms8192m -Xmx8192m
 * -Xmn2g -Xms6g -Xmx6g
 *
 * @version 3.2 Multi-dataset & support for delta/rate changing.
 * @see DM
 * @see PSC
 * @see TrajDrawHandler2
 * @see TrajShader
 */
public class VFGSMapApp extends PApplet {
	private static final String[] SET_NAMES
			= {"----", "FULL", "VFGS", "VFGS(CE)", "RAND"};

	private UnfoldingMap map;
	private Trajectory[] trajShowNow = null;
	private Trajectory[] trajFull = null;
	private Trajectory[][][] trajVfgsMatrix = null;     // trajVfgs for delta X rate
	private Trajectory[][] trajRandList = null;         // trajRand for rate

	// traj color shader
	/*private TrajShader shader;*/  // drop it after init
	private boolean colored;

	// var to check changes
	private int nowZoomLevel;
	private Location nowCenter;
	private boolean mouseDown;
	private int mode;

	private int dPtr = 0;
	private int rPtr = 0;
	private final int[] deltaList = DELTA_LIST;
	private final double[] rateList = RATE_LIST;
	private final boolean loadVfgsRes = LOAD_VFGS;
	private final boolean loadColorRes = LOAD_COLOR;
	// because now color output doesn't cover all vfgs res, so need this option

	// multi-thread for part image painting
	private final ExecutorService threadPool;
	private final TrajDrawHandler2[] handlers = new TrajDrawHandler2[THREAD_NUM];

	// single thread pool for images controlling
	private final ExecutorService controlPool;
	private Thread controlThread;

	private final PGraphics[] trajImages = new PGraphics[THREAD_NUM];
	private final Vector<Long> newestId = new Vector<>(Collections.singletonList(0L));
	private final int[] trajCnt = new int[THREAD_NUM];

	private int pid = PSC.BEGIN_CENTER_ID;        // use it to def. no the center in PSC
	private static final Location[] presetCenter = PSC.PRESET_CENTER;


	public VFGSMapApp() {
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
		System.out.println(PSC.str());

		// frame settings
		textSize(20);

		// map settings
		map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));

		map.setZoomRange(1, 20);
		map.zoomAndPanTo(11, presetCenter[pid]);
		map.setBackgroundColor(255);

		// thread("loadTraj");     // looks not good
		(new Thread(this::initData)).start();

		// add mouse and keyboard interactions
		MapUtils.createDefaultEventDispatcher(this, map);

		refreshTitle();
	}

	/**
	 * Load trajectory data from file (FULL)
	 * Then generate VFGS and RAND
	 */
	public void initData() {
		mode = 1;
		System.out.println();

		// load data from files. if load color res then load it
		boolean success = DM.loadRowData(ORIGIN_PATH, LIMIT)
				&& (!loadVfgsRes || DM.loadResList(RES_PATH, deltaList, rateList))
				&& (!loadColorRes
				|| DM.loadRepScoresMatrix(COLOR_PATH, deltaList, rateList));

		if (!success) {
			exit();
			return;
		}

		System.out.println();

		trajFull = DM.trajFull;
		int[] rateCntList = DM.translateRate(trajFull.length, rateList);

		// generate VFGS: trajVfgsMatrix
		if (loadVfgsRes) {
			mode = 2;
			initTrajVfgsMatrix(rateCntList);
		}

		// generate VFGS (CE)
		if (loadColorRes) {
			mode = 3;
			initTrajColor();
		}

		// generate RAND
		mode = 4;
		initTrajRandList(rateCntList);

		// default
		mode = 2;
		trajShowNow = trajVfgsMatrix[dPtr][rPtr];
		updateTrajImages();
	}

	/**
	 * Init {@link #trajVfgsMatrix} from {@link DM#vfgsResList}
	 */
	private void initTrajVfgsMatrix(int[] rateCntList) {
		int dLen = deltaList.length;
		int rLen = rateList.length;
		trajVfgsMatrix = new Trajectory[dLen][rLen][];

		for (int dIdx = 0; dIdx < dLen; dIdx++) {
			int[] vfgsRes = DM.vfgsResList[dIdx];

			for (int rIdx = 0; rIdx < rLen; rIdx++) {
				int rateCnt = rateCntList[rIdx];
				Trajectory[] trajVfgs = new Trajectory[rateCnt];
				for (int i = 0; i < rateCnt; i++) {
					trajVfgs[i] = trajFull[vfgsRes[i]];
				}
				trajVfgsMatrix[dIdx][rIdx] = trajVfgs;
			}
		}
	}

	private void initTrajColor() {
		int[][][] repScoresMatrix = DM.repScoresMatrix;
		TrajShader.initTrajColorMatrix(trajVfgsMatrix, repScoresMatrix,
				COLORS, BREAKS);

		// sort vfgs to show it properly
		for (int dIdx = 0; dIdx < deltaList.length; dIdx++) {
			for (int rIdx = 0; rIdx < rateList.length; rIdx++) {
				Trajectory[] trajVfgs = trajVfgsMatrix[dIdx][rIdx];
				for (Trajectory trajVfg : trajVfgs) {
					Color c = trajVfg.getColorMatrix()[dIdx][rIdx];
					int j;
					for (j = 0; j < 5; j++) {
						if (COLORS[j] == c) {
							break;
						}
					}
					trajVfg.setScore(j);        // reuse this field. no special meaning
				}
				Arrays.sort(trajVfgs, Comparator.comparingInt(Trajectory::getScore));
			}
		}
	}

	private void initTrajRandList(int[] rateCntList) {
		Random rand = new Random();
		int dLen = rateCntList.length;
		trajRandList = new Trajectory[dLen][];

		for (int dIdx = 0; dIdx < dLen; dIdx++) {
			int rateCnt = rateCntList[dIdx];
			Trajectory[] trajRand = new Trajectory[rateCnt];
			HashSet<Integer> set = new HashSet<>(rateCnt * 4 / 3 + 1);

			int cnt = 0;
			while (cnt < trajRand.length) {
				int idx = rand.nextInt(trajFull.length);
				if (set.contains(idx)) {
					continue;
				}
				set.add(idx);
				trajRand[cnt++] = trajFull[idx];
			}

			trajRandList[dIdx] = trajRand;
		}
	}

	private void refreshTitle() {
		int loadCnt = 0;    // # of painted traj
		int partCnt = 0;    // # of part images that have finished painting.
		for (int i = 0; i < THREAD_NUM; ++i) {
			partCnt += (trajImages[i] == null) ? 0 : 1;
			loadCnt += trajCnt[i];
		}
		double percentage = (trajShowNow == null) ?
				0.0 : Math.min(100.0, 100.0 * loadCnt / trajShowNow.length);

		String shownSet = SET_NAMES[mode];

		// main param str shown in title
		String str = String.format(this.getClass().getSimpleName()
						+ " [%5.2f fps] [%s] [%.1f%%] [%d parts] [zoom=%d] [pid=%d] ",
				frameRate, shownSet, percentage, partCnt, map.getZoomLevel(), pid);

		if (trajShowNow == null) {
			// generating traj data
			surface.setTitle(str + "Generate " + shownSet + " set...");
			return;
		}

		if (mode == 2 || mode == 3 || mode == 4) {
			// VFGS / VFGS+ or VFGS with CE
			str += String.format("[delta=%d rate=%s] ",
					deltaList[dPtr], rateList[rPtr]);
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
				} else {
					// drop all traj images first if drag
					Arrays.fill(trajImages, null);
					Arrays.fill(trajCnt, 0);
				}
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
		// drop all traj images
		Arrays.fill(trajImages, null);
		Arrays.fill(trajCnt, 0);

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
						trajImages, trajShowNow, newestId, colored, dPtr, rPtr, trajCnt,
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
		threadPool.shutdownNow();
		controlPool.shutdownNow();
		super.exit();
	}

	@Override
	public void keyReleased() {
		if (key == '\u001B') {  // press default key: esc, quit
			exit();
			return;
		}
		if (consumeMoveOpt()) {
			return;
		}
		if (consumeChangeDataSetOpt()) {
			return;
		}
		System.out.println("Unknown key operation: " +
				"key=" + key + ", keyCode=" + keyCode);
	}
	
	private boolean consumeMoveOpt() {
		Trajectory[] trajList;
		boolean consumed = true;
		switch (key) {
			case '1':       // change to FULL
				if (trajShowNow != trajFull) {
					mode = 1;
					trajShowNow = trajFull;
					colored = false;
					updateTrajImages();
				}
				break;
			case '2':       // change to VFGS / VFGS+
				trajList = trajVfgsMatrix[dPtr][rPtr];
				if (trajShowNow != trajList || colored) {
					mode = 2;
					trajShowNow = trajList;
					colored = false;
					updateTrajImages();
				}
				break;
			case '3':       // change to VFGS / VFGS+ CE
				trajList = trajVfgsMatrix[dPtr][rPtr];
				if (trajShowNow != trajList || !colored) {
					mode = 3;
					trajShowNow = trajList;
					colored = true;
					updateTrajImages();
				}
				break;
			case '4':       // change to RAND
				trajList = trajRandList[rPtr];
				if (trajShowNow != trajList) {
					mode = 4;
					trajShowNow = trajList;
					colored = false;
					updateTrajImages();
				}
				break;
			case 'd':       // increase dPtr
				if (mode != 2 && mode != 3) {
					break;     // not vfgs
				}
				if (dPtr != deltaList.length - 1) {
					dPtr++;
					trajShowNow = trajVfgsMatrix[dPtr][rPtr];
					updateTrajImages();
				}
				break;
			case 'a':       // decrease dPtr
				if (mode != 2 && mode != 3) {
					break;     // not vfgs
				}
				if (dPtr != 0) {
					dPtr--;
					trajShowNow = trajVfgsMatrix[dPtr][rPtr];
					updateTrajImages();
				}
				break;
			case 's':       // increase rPtr
				if (rPtr != rateList.length - 1) {
					rPtr++;
					if (mode == 4) {    // is rand now
						trajShowNow = trajRandList[rPtr];
					} else {
						trajShowNow = trajVfgsMatrix[dPtr][rPtr];
					}
					updateTrajImages();
				}
				break;
			case 'w':       // decrease rPtr
				if (rPtr != 0) {
					rPtr--;
					if (mode == 4) {    // is rand now
						trajShowNow = trajRandList[rPtr];
					} else {
						trajShowNow = trajVfgsMatrix[dPtr][rPtr];
					}
					updateTrajImages();
				}
				break;
			default:
				consumed = false;
		}

		return consumed;
	}

	private boolean consumeChangeDataSetOpt() {
		boolean consumed = true;
		switch (key) {
			case 'p':       // save pic
				saveCurFrame();
				break;
			case 'o':
				Location loc = map.getCenter();
				System.out.println(loc.x + " " + loc.y);
				break;
			case ',':       // next center
				if (pid != 0) {
					pid--;
					map.panTo(presetCenter[pid]);
					updateTrajImages();
				}
				break;
			case '.':       // last center
				if (pid != presetCenter.length - 1) {
					pid++;
					map.panTo(presetCenter[pid]);
					updateTrajImages();
				}
				break;
			case '/':       // reset to cur center
				map.panTo(presetCenter[pid]);
				updateTrajImages();
				break;
			case '-':       // zoom out
				if (map.getZoomLevel() > 11) {
					map.zoomToLevel(map.getZoomLevel() - 1);
					updateTrajImages();
				}
				break;
			case '=':       // zoom in
				if (map.getZoomLevel() < 20) {
					map.zoomToLevel(map.getZoomLevel() + 1);
					updateTrajImages();
				}
				break;
			default:
				consumed = false;
		}
		return consumed;
	}

	public void saveCurFrame() {
		String path = PATH_PREFIX + SET_NAMES[mode].toLowerCase();
		path += (mode == 1) ? "" : "_delta" + deltaList[dPtr];
		path += (mode == 1 || mode == 4) ? "" : "_rate" + (rateList[rPtr]);
		path +=	"_zoom" + map.getZoomLevel() + "_pid" + pid + ".png";
		saveFrame(path);
		System.out.println(path);
	}

	public static void main(String[] args) {
		PApplet.main(VFGSMapApp.class.getName());
	}
}