package vqgs.vfgs;

import de.fhpotsdam.unfolding.UnfoldingMap;
import vqgs.model.Position;
import vqgs.model.Trajectory;
import processing.core.PApplet;
import vqgs.util.DM;
import vqgs.util.PSC;
import vqgs.util.WF;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static vqgs.util.PSC.*;

/**
 * VFGS Color Encoding Algorithm
 * <p>
 * <br>input:
 * <ul><li>The positions field of trajs and {@link DM#totPosSet},
 * stored in {@link PSC#POS_INFO_PATH}</li>
 * <li>{@link DM#vfgsResList}, stored in {@link PSC#RES_PATH}</li></ul>
 * output:
 * <ul><li>{@link DM#repScoresMatrix}, stored in {@link PSC#COLOR_PATH}</li></ul>
 */
public final class VFGSColor extends PApplet {
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

        if (WF.status != WF.VFGS_CAL) {
            // load data if not run VFGS_CAL first
            boolean success = DM.loadPosInfo(POS_INFO_PATH)
                    && DM.loadResList(RES_PATH, DELTA_LIST, RATE_LIST);
            if (!success) {
                // load failed
                WF.error = true;
                return;
            }
        }

        // VFGS_CAL / data load ran first
        // release some useless (w.r.t. this step) src
        // not need any more
		/*DM.releaseLocations();
		System.gc();*/

        WF.status = WF.VFGS_COLOR_CAL;

        try {
            run(map);
        } catch (Exception e) {
            e.printStackTrace();
            WF.error = true;
        }
    }

    private static void run(UnfoldingMap map) {
        DM.printAndLog(LOG_PATH, "\n====== Begin " + WF.status + " ======\n" +
                "\nalpha, delta, time\n");

        // get from DM / PSC
        Trajectory[] trajFull = DM.trajFull;
        HashSet<Position> totPosSet = DM.totPosSet;
        int[][] vfgsResList = DM.vfgsResList;
        int[] deltaList = DELTA_LIST;
        double[] rateList = RATE_LIST;

        int trajNum = trajFull.length;
        int dLen = deltaList.length;
        int rLen = rateList.length;

        int[] rateCntList = DM.translateRate(trajNum, rateList);

        // used to stored the trajs pass through a specific position
        HashMap<Position, HashSet<Integer>> posToTrajs = new HashMap<>();
        // used to cnt the common pixel num of a traj
        int[] pixelMap = new int[rateCntList[0]];

        // the rep scores result matrix
        int[][][] repScoresMatrix = new int[dLen][rLen][];

        long startTime = System.currentTimeMillis();


        for (int dIdx = 0; dIdx < dLen; dIdx++) {
            int[] vfgsRes = vfgsResList[dIdx];
            int delta = deltaList[dIdx];

			/*int capacity = calResPosNum(vfgsRes, trajFull, delta);
			posToTrajs = new HashMap<>(capacity * 4 / 3 + 1);*/
            posToTrajs.clear();
            int last = 0;

            System.out.println("delta=" + delta + " begin");
            long deltaStartTime = System.currentTimeMillis();


            for (int rIdx = rLen - 1; rIdx >= 0; rIdx--) {
                int rateCnt = rateCntList[rIdx];
                int[] repScores = new int[rateCnt];

                DM.printAndLog(LOG_PATH, rateList[rIdx] + ", " + delta);
                long rateStartTime = System.currentTimeMillis();

                // update pos map
                updatePosToTrajs(posToTrajs, trajFull, totPosSet,
                        vfgsRes, last, rateCnt, delta);

                // map every traj's positions to R (the map is repScores arr)
                for (Trajectory traj : trajFull) {
                    // no need for R verify cause it will not affect the res
                    Arrays.fill(pixelMap, 0, rateCnt, 0);
                    int idx = getRepTrajIdx(traj.getPositions(), posToTrajs,
                            pixelMap, rateCnt);
                    // rep score of repTid = vfgs[idx] (i.e., tr) increases 1
                    repScores[idx]++;
                }

                repScoresMatrix[dIdx][rIdx] = repScores;
                last = rateCnt;

                long rateEndTime = System.currentTimeMillis();
                DM.printAndLog(LOG_PATH, ", " + (rateEndTime - rateStartTime) + "\n");
            }

            long deltaEndTime = System.currentTimeMillis();
            DM.printAndLog(LOG_PATH, "tot_cost_" + delta + ", "
                    + (deltaEndTime - deltaStartTime) + "\n");
            System.out.println();
        }

        long endTime = System.currentTimeMillis();
        DM.printAndLog(LOG_PATH, "total_case_num, " + (dLen * rLen) +
                "\ntotal_time_cost, " + (endTime - startTime) + "\n");

        DM.repScoresMatrix = repScoresMatrix;
        DM.saveRepScoresMatrix(COLOR_PATH, deltaList);
    }

    /**
     * Count the number of possible positions. Used to init HashMap.
     */
    private static int calResPosNum(int[] vfgsRes, Trajectory[] trajFull, int delta) {
        int ret = 0;
        for (int tid : vfgsRes) {
            ret += trajFull[tid].getOriginScore();  // origin score = positions.length
        }
        return ret * (delta + 1) * (delta + 1);
    }

    /**
     * Add positions and their augment positions into posToTrajs
     *
     * @param posToTrajs update target
     * @param totPosSet  to exclude some invalid positions
     * @param start      the start index of vfgsRes to update
     * @param end        the end index (exclude) of vfgsRes to update
     */
    private static void updatePosToTrajs(HashMap<Position, HashSet<Integer>> posToTrajs,
                                         Trajectory[] trajFull,
                                         HashSet<Position> totPosSet,
                                         int[] vfgsRes,
                                         int start, int end, int delta) {

        for (int idx = start; idx < end; idx++) {
            int tid = vfgsRes[idx];
            for (Position p : trajFull[tid].getPositions()) {
                int px = p.getX();
                int py = p.getY();
                for (int i = -delta; i <= delta; i++) {
                    for (int j = -delta; j <= delta; j++) {
                        Position pos = new Position(px + i, py + i);
                        if (!totPosSet.contains(pos)) {
                            // no other traj in trajFull pass through this pos
                            continue;
                        }
                        // set of tid that pass it
                        HashSet<Integer> trajsPassThisPos = posToTrajs
                                .computeIfAbsent(pos, v -> new HashSet<>());
                        trajsPassThisPos.add(idx);
                    }
                }
            }
        }

    }

    /**
     * Iterate all the positions on a traj and find a represent traj in R
     * that has most common pixels with it
     * <p>
     * Problem: this function is called frequently. Inline may be faster.
     *
     * @param positions  the position list of this traj
     * @param posToTrajs position to traj idx in vfgs array.
     *                   These trajs pass though the position.
     * @param pixelMap   aux arr to store the pixel contribution
     * @param rateCnt    the valid pixelMap slice is [0, rateCnt)
     * @return the location of the represent traj in vfgsRes (and in pixelMap also)
     */
    private static int getRepTrajIdx(Position[] positions,
                                     HashMap<Position, HashSet<Integer>> posToTrajs,
                                     int[] pixelMap, int rateCnt) {
        // pixel statistic
        for (Position p : positions) {
            HashSet<Integer> trajsPassThisPos = posToTrajs.get(p);
            if (trajsPassThisPos == null) { // this pos makes no contribute
                continue;
            }
            for (int resIdx : trajsPassThisPos) {
                pixelMap[resIdx]++;
            }
        }

        // get represent traj
        int idx = -1;
        int repPixelCnt = -1;
        for (int i = 0; i < rateCnt; i++) {
            if (pixelMap[i] > repPixelCnt) {
                idx = i;
                repPixelCnt = pixelMap[i];
            }
        }
        return idx;
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            DELTA_LIST = new int[]{Integer.parseInt(args[0])};
            RATE_LIST = new double[]{Double.parseDouble(args[1])};
        }

        PApplet.main(VFGSColor.class.getName());
    }
}
