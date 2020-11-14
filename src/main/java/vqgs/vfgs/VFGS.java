package vfgs;

import de.fhpotsdam.unfolding.UnfoldingMap;
import model.Position;
import model.Trajectory;
import processing.core.PApplet;
import util.DM;
import util.PSC;
import util.TrajHeap;
import util.WF;

import java.util.Arrays;
import java.util.HashSet;

import static util.PSC.*;

/**
 * VFGS Main Algorithm
 * <p>
 * To fill result list {@link DM#vfgsResList}.
 * First dim is delta. The res with diff rate are stored together.
 * <p>
 * <br> input:
 * <ul><li>The positions field of trajs and {@link DM#totPosSet},
 * stored in {@link PSC#POS_INFO_PATH}</li></ul>
 * output:
 * <ul><li>{@link DM#vfgsResList}, stored in {@link PSC#RES_PATH}</li></ul>
 */
public final class VFGS extends PApplet {
	@Override
	public final void setup() {
		UnfoldingMap map = new UnfoldingMap(this);
		DM.printAndLog(LOG_PATH, PSC.str());
		main(map);
		exit();
	}

	public static void main(UnfoldingMap map) {
		map.zoomAndPanTo(20, CELL_CENTER);
		map.setZoomRange(1, 20);

		if (WF.status != WF.PRE_PROCESS) {
			// load data if not run PRE_PROCESS first
			boolean success = DM.loadPosInfo(POS_INFO_PATH);
			if (!success) {
				// load failed
				WF.error = true;
				return;
			}
		}

		// PRE_PROCESS / data load ran first
		// release some useless (w.r.t. this step) src
		DM.releaseLocations();
		System.gc();

		WF.status = WF.VFGS_CAL;

		try {
			run(map);
		} catch (Exception e) {
			e.printStackTrace();
			WF.error = true;
		}
	}

	private static void run(UnfoldingMap map) {
		DM.printAndLog(LOG_PATH, "\n====== Begin " + WF.status + " ======\n"
		+ "\nalpha, delta, time\n");
		System.out.println();

		// get from DM / PSC
		Trajectory[] trajFull = DM.trajFull;
		HashSet<Position> totPosSet = DM.totPosSet;
		int[] deltaList = DELTA_LIST;
		double[] rateList = RATE_LIST;

		int trajNum = trajFull.length;
		int dLen = deltaList.length;
		int rLen = rateList.length;

		int[] rateCntList = DM.translateRate(trajNum, rateList);

		// max-heap for greedy choosing
		TrajHeap heap = new TrajHeap(trajFull);
		// covered position set
		HashSet<Position> posSet = new HashSet<>();
		// delta-alpha matrix to store R / R+
		int[][] vfgsResList = new int[dLen][];
		Arrays.fill(vfgsResList, new int[rateCntList[0]]);

		long startTime = System.currentTimeMillis();


		for (int dIdx = 0; dIdx < dLen; dIdx++) {
			int cnt = 0;    // have taken cnt trajectories.
			int delta = deltaList[dIdx];
			heap.refresh();
			posSet.clear();
			System.out.println("delta=" + delta + " begin");
			long deltaStartTime = System.currentTimeMillis();


			for (int rIdx = rLen - 1; rIdx >= 0; rIdx--) {
				int curTrajNum = rateCntList[rIdx];
				DM.printAndLog(LOG_PATH, rateList[rIdx] + ", " + delta);

				/* VFGS algorithm core */

				while (cnt < curTrajNum) {
					// update score of top traj
					Trajectory topTraj = heap.getTopTraj();
					int score = reCalTrajScore(topTraj.getPositions(), delta, posSet);
					topTraj.setScore(score);

					if (heap.orderIsGreat()) {
						heap.deleteMax();

						// add this traj to R and begin to find next traj
						vfgsResList[dIdx][cnt] = topTraj.getTid();
						cnt++;

						// update R+
						updatePosSet(posSet, topTraj.getPositions(), delta, totPosSet);
					} else {
						heap.shiftDown(1);
					}
				}

				long rateEndTime = System.currentTimeMillis();
				DM.printAndLog(LOG_PATH, ", " + (rateEndTime - deltaStartTime) + "\n");
			}

			long deltaEndTime = System.currentTimeMillis();
			DM.printAndLog(LOG_PATH, "tot_cost_" + delta + ", "
					+ (deltaEndTime - deltaStartTime) + "\n");
			System.out.println();
		}


		long endTime = System.currentTimeMillis();
		DM.printAndLog(LOG_PATH, "total_case_num, " + (dLen * rLen) +
				"\ntotal_time_cost, " + (endTime - startTime) + "\n");

		DM.vfgsResList = vfgsResList;
		DM.saveResList(RES_PATH, DELTA_LIST);
	}

	/**
	 * Recalculate contribute score of the target traj if take it.
	 *
	 * @param trajPos   the positions of target traj (is heap top here)
	 * @param delta  delta param used now
	 * @param posSet covered position set
	 * @return the new score
	 */
	private static int reCalTrajScore(Position[] trajPos, int delta, HashSet<Position> posSet) {
		int score = 0;

		next:
		for (Position p : trajPos) {
			if (posSet.contains(p)) {
				// in R+ (can't in self point set)
				continue;
			}
			int incr = 1;   // score increase
			int px = p.getX();
			int py = p.getY();
			for (int i = -delta; i <= delta; i++) {
				for (int j = -delta; j <= delta; j++) {
					if (posSet.contains(new Position(px + i, py + j))) {
						// incr is not valid
						continue next;
					}
				}
			}
			score += incr;
		}
		return score;
	}

	/**
	 * Update the R+ pixel set (posSet) when taking a new traj
	 *
	 * @param posSet covered position set
	 * @param trajPos   the positions of new traj added to R
	 * @param delta  delta param used now
	 * @param totPosSet ref set to removed invalid position
	 */
	private static void updatePosSet(HashSet<Position> posSet, Position[] trajPos,
	                                 int delta, HashSet<Position> totPosSet) {
		for (Position p : trajPos) {
			int px = p.getX();
			int py = p.getY();
			for (int i = -delta; i <= delta; i++) {
				for (int j = -delta; j <= delta; j++) {
					Position pos = new Position(px + i, py + j);
					if (!totPosSet.contains(pos)) {
						continue;   // no other traj pass this position
					}
					posSet.add(pos);
				}
			}
		}
	}

	public static void main(String[] args) {
		if (args.length > 0){
			DELTA_LIST = new int[]{Integer.parseInt(args[0])};
			RATE_LIST = new double[]{Double.parseDouble(args[1])};
		}

		PApplet.main(VFGS.class.getName());
	}
}
