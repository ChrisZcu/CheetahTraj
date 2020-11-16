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
 * Preprocessing class to provide necessary data
 * for following step.
 * <p>
 * The function of scoreFileCal is one of the parts here.
 * (originScores)
 * <p>
 * <br> Input:
 * <ul><li>Origin data. Read from {@link PSC#ORIGIN_PATH}</li></ul>
 * Output:
 * <ul><li>{@link DM#trajFull}</li>
 * <li>Positions field of traj, stored in {@link PSC#POS_INFO_PATH}</li></ul>
 */
public final class PreProcess extends PApplet {
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

        WF.status = WF.PRE_PROCESS;

        try {
            run(map);
        } catch (Exception e) {
            e.printStackTrace();
            WF.error = true;
        }
    }

    private static void run(UnfoldingMap map) {
        DM.printAndLog(LOG_PATH, "\n====== Begin " + WF.status + " ======\n");

        /* load traj from file */

        DM.loadRowData(ORIGIN_PATH, LIMIT);
        DM.printAndLog(LOG_PATH, "\nPrePrecess size: " + DM.trajFull.length + "\n");
        Trajectory[] trajFull = DM.trajFull;

        /* cal position info */

        HashSet<Position> totPosSet = new HashSet<>();

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
            traj.setOriginScore(trajPos.size());
            traj.setPositions(trajPos.toArray(new Position[0]));
            traj.setOriginScore(traj.getPositions().length);
        }

        /* save to DM */

        DM.trajFull = trajFull;
        DM.totPosSet = totPosSet;

        System.out.println();
        DM.printAndLog(LOG_PATH, "Preprocessing done\n");

        /* save to files */

        if (SAVE_TEMP) {
            DM.savePosInfo(POS_INFO_PATH);

            // temp output: for the input of origin.util.qualityCal
            DM.saveScoreFile(ORIGIN_PATH, SCORE_PATH, LIMIT);
        }
    }

    public static void main(String[] args) {

        if (args.length > 0)
            ORIGIN_PATH = args[0];

        PApplet.main(PreProcess.class.getName());
    }
}
