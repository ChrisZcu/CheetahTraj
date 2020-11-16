package vqgs.vfgs;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import vqgs.model.Position;
import vqgs.model.Trajectory;
import processing.core.PApplet;
import vqgs.util.DM;
import vqgs.util.PSC;
import vqgs.util.WF;

import java.util.HashSet;

import static vqgs.util.PSC.*;

/**
 * Evaluate the quality of VFGS output.
 * <p>
 * <br> Input:
 * <ul><li>Raw data {@link DM#trajFull}, stored in {@link PSC#ORIGIN_PATH}</li>
 * <li>{@link DM#vfgsResList}, stored in {@link PSC#RES_PATH}</li></ul>
 * Output:
 * <ul><li>{@link PSC#QUALITY_PATH}</li></ul>
 */
public final class Quality extends PApplet {
	@Override
	public void setup() {
		UnfoldingMap map = new UnfoldingMap(this);
		DM.printAndLog(LOG_PATH, PSC.str());
		main(map);
		exit();
	}

	public static void main(UnfoldingMap map) {
		map.zoomAndPanTo(20, CELL_CENTER);
		map.setZoomRange(1, 20);

		boolean success;
		if (WF.status != WF.VFGS_COLOR_CAL) {
			// load data if not run VFGS_COLOR_CAL first
			success = DM.loadRowData(ORIGIN_PATH, LIMIT)
					&& DM.loadResList(RES_PATH, DELTA_LIST, RATE_LIST);
		} else {
			success = DM.loadRowData(ORIGIN_PATH, LIMIT);
		}

		if (!success) {
			// load failed
			WF.error = true;
			return;
		}

		// VFGS_COLOR_CAL / data load ran first
		// release some useless (w.r.t. this step) src
		DM.releasePositions();
		DM.totPosSet = null;
		DM.repScoresMatrix = null;
		System.gc();

		WF.status = WF.QUALITY_CAL;

		try {
			run(map);
		} catch (Exception e) {
			e.printStackTrace();
			WF.error = true;
		}
	}

	private static void run(UnfoldingMap map) {
		DM.printAndLog(LOG_PATH, "\n====== Begin " + WF.status + " ======\n" +
				"\nzoom, alpha, delta, time\n");

		// get from DM / PSC
		Trajectory[] trajFull = DM.trajFull;
		int[][] vfgsResList = DM.vfgsResList;
		int[] deltaList = DELTA_LIST;
		double[] rateList = RATE_LIST;
		int zoomBegin = ZOOM_RANGE[0], zoomEnd = ZOOM_RANGE[1];

		int trajNum = trajFull.length;
		int dLen = deltaList.length;
		int rLen = rateList.length;

		// two aux traj position data
		// untied positions field of traj
		Position[][] trajPosList = new Position[trajFull.length][];
		// tot positions set for diff zoom
		HashSet<Position> totPosSet = new HashSet<>();

		HashSet<Position> vfgsCoverSet = new HashSet<>();

		int[] rateCntList = DM.translateRate(trajNum, rateList);

		int[][][] qualityCube = new int[zoomEnd - zoomBegin + 1][dLen][rLen];
		int[] totScoreList = new int[zoomEnd - zoomBegin + 1];

		long startTime = System.currentTimeMillis();


		for (int zoom = zoomBegin, zIdx = 0; zoom <= zoomEnd; zoom++, zIdx++) {
			map.zoomAndPanTo(zoom, CELL_CENTER);
			// init trajPosList & totPosSet
			initTrajPosInfo(trajPosList, totPosSet, map, trajFull);
			totScoreList[zIdx] = totPosSet.size();

			System.out.println();
			long zoomStartTime = System.currentTimeMillis();


			for (int dIdx = 0; dIdx < dLen; dIdx++) {
				int last = 0;
				int delta = deltaList[dIdx];
				int[] vfgsRes = vfgsResList[dIdx];
				vfgsCoverSet.clear();

				long deltaStartTime = System.currentTimeMillis();


				for (int rIdx = rLen - 1; rIdx >= 0; rIdx--) {
					DM.printAndLog(LOG_PATH, zoom + ", " + rateList[rIdx]
							+ ", " + delta);
					int rateCnt = rateCntList[rIdx];
					long rateStartTime = System.currentTimeMillis();

					/* Quality core start here with zoom-delta-rate dims. */
					int score = getQuality(last, rateCnt, trajPosList, vfgsRes, delta,
							totPosSet, vfgsCoverSet);
					qualityCube[zIdx][dIdx][rIdx] = score;
					last = rateCnt;

					long rateEndTime = System.currentTimeMillis();
					DM.printAndLog(LOG_PATH, ", " + (rateEndTime - rateStartTime) + "\n");
				}

				long deltaEndTime = System.currentTimeMillis();
				DM.printAndLog(LOG_PATH, "tot_cost_" + zoom + "_" + delta + ", "
						+ (deltaEndTime - deltaStartTime) + "\n");
			}

			// one zoom end
			System.out.println();
			long zoomEndTime = System.currentTimeMillis();
			DM.printAndLog(LOG_PATH, "tot_cost_" + zoom + ", "
					+ (zoomEndTime - zoomStartTime) + "\n");
		}

		long endTime = System.currentTimeMillis();
		DM.printAndLog(LOG_PATH, "tot_case_num, " + ((zoomEnd - zoomBegin + 1) * dLen * rLen) +
				"\ntotal_time_cost, " + (endTime - startTime) + "\n");

		// save quality result
		DM.saveQualityResult(QUALITY_PATH, ZOOM_RANGE, deltaList, rateList,
				qualityCube, totScoreList);
	}


	/**
	 * Init two aux position data structures at this zoom level.
	 * <br> If the memory is overflow, inline this calculate into
	 * update vfgsCoverSet processing.
	 */
	private static void initTrajPosInfo(Position[][] trajPosList,
	                                    HashSet<Position> totPosSet,
	                                    UnfoldingMap map,
	                                    Trajectory[] trajFull) {
		// trajPosList will be update automatically.
		totPosSet.clear();
		int tid = 0;
		for (Trajectory traj : trajFull) {
			HashSet<Position> trajPos = new HashSet<>();
			for (Location loc : traj.getLocations()) {
				ScreenPosition p = map.getScreenPosition(loc);
				int px = (int) p.x;
				int py = (int) p.y;
				Position pos = new Position(px, py);
				trajPos.add(pos);
				totPosSet.add(pos);
			}
			trajPosList[tid] = trajPos.toArray(new Position[0]);
			tid++;
		}
	}

	/**
	 * Get the quality (i.e. the # of pixels that can be covered by traj in R)
	 * @return the quality integer
	 */
	@SuppressWarnings("unchecked")
	private static int getQuality(int last, int rateCnt, Position[][] trajPosList,
	                              int[] vfgsRes, int delta, HashSet<Position> totPosSet,
	                              HashSet<Position> vfgsCoverSet) {
		// update vfgsCoverSet

		for (int idx = last; idx < rateCnt; idx++) {
			for (Position p : trajPosList[vfgsRes[idx]]) {
				// add all augment pixels
				int px = p.getX();
				int py = p.getY();
				for (int i = -delta; i <= delta; i++) {
					for (int j = -delta; j <= delta; j++) {
						Position pos = new Position(px + i, py + j);
						if (!totPosSet.contains(pos)) {
							continue;   // no other traj pass it
						}
						vfgsCoverSet.add(pos);
					}
				}
				// add finished
			}
		}

		// compute cover scores

		int quality = 0;
		HashSet<Position> restPosSet = (HashSet<Position>) vfgsCoverSet.clone();
		for (Position[] positions : trajPosList) {
			for (Position p : positions) {
				if (restPosSet.remove(p)) {
					quality++;    // remove success, plus score
				}
			}
		}

		return quality;
	}

	public static void main(String[] args) {
		PApplet.main(Quality.class.getName());
	}
}
