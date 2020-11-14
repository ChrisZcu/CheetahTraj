package util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.Trajectory;

import java.util.ArrayList;
import java.util.HashSet;

public class VFGS {
    private static HashSet<Position> totalTrajPos = new HashSet<>();
    public static UnfoldingMap map;
    public static Trajectory[] trajFull;


    public static void initTrajFull(Trajectory[] trajFulls) {
        trajFull = trajFulls;
        for (Trajectory traj : trajFull) {
            traj.scoreInit();
        }
    }

    private VFGS() {
    }

    public static VFGS instance = new VFGS();

    public static VFGS getInstance() {
        return instance;
    }

    public static void trajPosInfoInit(Trajectory[] TrajTotal) {
        totalTrajPos.clear();
        HashSet<Position> posSet = new HashSet<>();
        int tid = 0;
        for (Trajectory traj : TrajTotal) {
            posSet.clear();
            for (Location loc : traj.locations) {
                ScreenPosition sp = map.getScreenPosition(loc);
                Position pos = new Position((int) sp.x, (int) sp.y); // float -> int directly
                posSet.add(pos);
                totalTrajPos.add(pos);
            }
            traj.setPositions(posSet.toArray(new Position[0]));
//            traj.locations = null;      // release it
        }
        System.gc();
    }

    private static GreedyChoose GreedyChoose;

    private static HashSet<Position> trajSet = new HashSet<>();    // R+

    public static Trajectory[] getCellCover(Trajectory[] trajFull, UnfoldingMap maps, double rate, int delta) {//record cal
        GreedyChoose = new GreedyChoose(trajFull.length);
        map = maps;
        map.zoomTo(20);

        initTrajFull(trajFull);
        trajPosInfoInit(trajFull);

        ArrayList<Trajectory> cellList = new ArrayList<>();

        int TRAJNUM = trajFull.length;
        trajSet.clear();
        heapInit();

        int trajNum = (int) (rate * TRAJNUM);
//        if (TRAJNUM < 2500) {
//            trajNum = TRAJNUM;
//        } else if (trajNum < 2500) {
//            trajNum = 2500;
//        }

        for (int i = 0; i < trajNum; i++) {
            while (true) {
                Traj2CellScore(GreedyChoose.getHeapHead());
                if (GreedyChoose.GreetOrder()) {
                    Trajectory traj = GreedyChoose.getMaxScoreTraj();   // deleteMax
                    cellList.add(traj);    // take this, add to R
                    CellGridUpdate(traj, delta);        // update R+
                    break;
                } else {
                    GreedyChoose.orderAdjust();
                }
            }
        }

        trajSet.clear();
        return cellList.toArray(new Trajectory[0]);
    }


    private static void heapInit() {
        GreedyChoose.clean();
        for (Trajectory traj : trajFull) {
            GreedyChoose.addNewTraj(traj);
        }
    }

    private static void CellGridUpdate(Trajectory traj, int delta) {
//        for (Location p : traj.locations) {
//            double px = map.getScreenPosition(p).x;
//            double py = map.getScreenPosition(p).y;
//            for (int i = -DELTA; i <= DELTA; i++) {
//                for (int j = -DELTA; j <= DELTA; j++) {
//                    Position pos = new Position(px + i, py + j);
//                    if (totalTrajPos.contains(pos))
//                        trajSet.add(pos);
//                }
//            }
//        }
        for (Position p : traj.getPositions()) {
            int px = p.x;
            int py = p.y;
            for (int i = -delta; i <= delta; i++) {
                for (int j = -delta; j <= delta; j++) {
                    Position pos = new Position(px + i, py + j);
                    if (!totalTrajPos.contains(pos)) {
                        continue;   // no other traj pass this position
                    }
                    trajSet.add(pos);
                }
            }
        }
    }

    private static void Traj2CellScore(Trajectory traj) {
//        traj.setScore(0);
//        for (Location p : traj.locations) {
//            double px = map.getScreenPosition(p).x;
//            double py = map.getScreenPosition(p).y;
//            if (trajSet.contains(new Position(px, py)))  // R+
//                continue;
//            // not in R+ or self point set
//            traj.setScore(traj.getScore() + 1);
//        }
        int score = 0;
        Position[] trajPos = traj.getPositions();
        for (Position p : trajPos) {
            if (trajSet.contains(p)) {
                // incr is not valid
                continue;
            }
            score++;
        }
        traj.setScore(score);
    }


    public static double getQuality(Trajectory[] origin, Trajectory[] res, UnfoldingMap map, int delta) {
        HashSet<Position> totalPositionSet = getPositionSet(origin, map);
        HashSet<Position> trajSet = new HashSet<>();

        double totalScore = 0;
        for (Trajectory traj : res) {
            for (Location loc : traj.locations) {
                ScreenPosition pos = map.getScreenPosition(loc);
                for (int i = -delta; i < delta + 1; i++) {
                    for (int j = -delta; j < delta + 1; j++) {
                        Position position = new Position(pos.x + i, pos.y + j);
                        if (!trajSet.contains(position) && totalPositionSet.contains(position)) {
                            trajSet.add(position);
                            totalScore++;
                        }
                    }
                }
            }
        }
        if (totalPositionSet.size() == 0)
            return 0;
        return totalScore / totalPositionSet.size();
    }

    public static double getQuality(Trajectory[] origin, Trajectory[] res, UnfoldingMap map, int zoomLevel, int delta) {
        map.zoomAndPanTo(zoomLevel, new Location(41.315205, -8.629877));
//        System.out.println(map.getZoomLevel() + ", " + delta + "......");

        HashSet<Position> totalPositionSet = getPositionSet(origin, map);
        HashSet<Position> trajSet = new HashSet<>();

        double totalScore = 0;
        for (Trajectory traj : res) {
            for (Location loc : traj.locations) {
                ScreenPosition pos = map.getScreenPosition(loc);
                for (int i = -delta; i < delta + 1; i++) {
                    for (int j = -delta; j < delta + 1; j++) {
                        Position position = new Position(pos.x + i, pos.y + j);
                        if (!trajSet.contains(position) && totalPositionSet.contains(position)) {
                            trajSet.add(position);
                            totalScore++;
                        }
                    }
                }
            }
        }
        if (totalPositionSet.size() == 0)
            return 0;
        return totalScore / totalPositionSet.size();
    }

    private static HashSet<Position> getPositionSet(Trajectory[] trajectories, UnfoldingMap map) {
        HashSet<Position> totalSet = new HashSet<>();
        for (Trajectory traj : trajectories) {
            for (Location loc : traj.locations) {
                ScreenPosition pos = map.getScreenPosition(loc);
                totalSet.add(new Position(pos.x, pos.y));
            }
        }
        return totalSet;
    }


}
