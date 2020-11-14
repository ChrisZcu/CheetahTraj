package select;

import app.CircleRegionControl;
import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * implements all the backend select algorithms.
 */
public class SelectAlg {

    /**
     * Based on the begin and end index to calculate the sub-array trajList
     * with the particular origin and destination.
     * <br>
     * regions are all in the SO, regionO, regionD are exclusive.
     *
     * @param begin    the begin index, included.
     * @param end      the end index, not included.
     * @param trajList the trajList based, including Full, VFGS, Random
     */
    public static Trajectory[] getODTraj(int begin, int end, Trajectory[] trajList, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        RegionModel regionO;
        RegionModel regionD;

        if (instance.isCircleRegion()) {
            regionO = CircleRegionControl.getCircleRegionControl().getCircleO();
            regionD = CircleRegionControl.getCircleRegionControl().getCircleD();
        } else {
            regionO = instance.getRegionOList()[optIndex];
            regionD = instance.getRegionDList()[optIndex];
        }
        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList[i];
            Location[] locations = traj.locations;
            if (instance.isCircleRegion()) {
                assert regionD instanceof CircleRegion;
                if (inCheck((CircleRegion) regionO, locations[0], map)
                        && inCheck((CircleRegion) regionD, locations[locations.length - 1], map)) {
                    res.add(traj);
                }
            } else {
                assert regionD instanceof RectRegion;
                if (inCheck((RectRegion) regionO, locations[0], map)
                        && inCheck((RectRegion) regionD, locations[locations.length - 1], map)) {
                    res.add(traj);
                }
            }
        }
        return res.toArray(new Trajectory[0]);
    }


    /**
     * calculates the sub-array of trajectory based on way-point region, on the same layer.
     */
    private static ArrayList<Trajectory> getWayPointTraj(int begin, int end, Trajectory[] trajList,
                                                         ArrayList<RectRegion> regionWList, int optIndex) {
        ArrayList<Trajectory> res = new ArrayList<>();
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList[i];
            Location[] locations = traj.locations;
            for (int j = 1, bound = locations.length - 1; j < bound; j++) {
                if (inCheck(regionWList, locations[j], map)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return res;
    }

    private static ArrayList<Trajectory> getCircleWayPointTraj(int begin, int end, Trajectory[] trajList,
                                                               ArrayList<CircleRegion> circleRegions, int optIndex) {
        ArrayList<Trajectory> res = new ArrayList<>();
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList[i];
            Location[] locations = traj.locations;
            for (int j = 1, bound = locations.length - 1; j < bound; j++) {
                if (inCheckCircleList(circleRegions, locations[j], map)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return res;
    }

    private static ArrayList<Trajectory> getWayPointTraj(int begin, int end, ArrayList<Trajectory> trajList,
                                                         ArrayList<RectRegion> regionWList, int optIndex) {
        ArrayList<Trajectory> res = new ArrayList<>();
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
//        float xOff = instance.getMapLocInfo()[0][optIndex];
//        float yOff = instance.getMapLocInfo()[1][optIndex];

        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList.get(i);
            Location[] locations = traj.locations;
            for (int j = 1, bound = locations.length - 1; j < bound; j++) {
                if (inCheck(regionWList, locations[j], map)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return res;
    }

    private static ArrayList<Trajectory> getCircleWayPointTraj(int begin, int end, ArrayList<Trajectory> trajList,
                                                               ArrayList<CircleRegion> circleRegions, int optIndex) {
        ArrayList<Trajectory> res = new ArrayList<>();
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
//        float xOff = instance.getMapLocInfo()[0][optIndex];
//        float yOff = instance.getMapLocInfo()[1][optIndex];

        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList.get(i);
            Location[] locations = traj.locations;
            for (int j = 1, bound = locations.length - 1; j < bound; j++) {
                if (inCheckCircleList(circleRegions, locations[j], map)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Calculate way point result according to multi-level layers in {@link SharedObject}.
     * This method will call {@link #getWayPointTraj} as underlying method.
     */
    public static Trajectory[] getWayPointTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {
        ArrayList<Trajectory> res = new ArrayList<>();
        if (SharedObject.getInstance().isCircleRegion()) {
            for (CircleRegionGroup circleRegionGroup : CircleRegionControl.getCircleRegionControl().getCircleWayPointList()[optIndex]) {
                res.addAll(getCircleWayPointTraj(begin, end, trajectory, optIndex, circleRegionGroup.getWayPointLayerList()));
            }
        } else {
            for (WayPointGroup wayPointGroup : SharedObject.getInstance().getWayPointGroupList()[optIndex]) {
                res.addAll(getWayPointTraj(begin, end, trajectory, optIndex, wayPointGroup.getWayPointLayerList()));
            }
        }
        return res.toArray(new Trajectory[0]);
    }

    private static ArrayList<Trajectory> getWayPointTraj(int begin, int end, Trajectory[] trajectory, int optIndex, ArrayList<ArrayList<RectRegion>> regionWList) {

        ArrayList<Trajectory> res = getWayPointTraj(begin, end, trajectory, regionWList.get(0), optIndex);

        for (int i = 1; i < regionWList.size(); i++) {
            res = getWayPointTraj(0, res.size(), res, regionWList.get(i), optIndex);
        }
        return res;
    }

    private static ArrayList<Trajectory> getCircleWayPointTraj(int begin, int end, Trajectory[] trajectory, int optIndex, ArrayList<ArrayList<CircleRegion>> regionWList) {

        ArrayList<Trajectory> res = getCircleWayPointTraj(begin, end, trajectory, regionWList.get(0), optIndex);

        for (int i = 1; i < regionWList.size(); i++) {
            res = getCircleWayPointTraj(0, res.size(), res, regionWList.get(i), optIndex);
        }
        return res;
    }

    /**
     * calculates the sub-array of trajectory based on all-in region.
     */
    public static Trajectory[] getAllIn(int begin, int end, Trajectory[] trajectory, RectRegion r, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        float xOff = instance.getMapLocInfo()[0][optIndex];
        float yOff = instance.getMapLocInfo()[1][optIndex];

        List<Trajectory> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            boolean inFlag = true;
            for (Location loc : traj.getLocations()) {
                if (!inCheck(r, loc, map)) {
                    inFlag = false;
                    break;
                }
            }
            if (inFlag) {
                res.add(traj);
            }
        }
        Trajectory[] trajAry = new Trajectory[res.size()];
        int i = 0;
        for (Trajectory traj : res) {
            trajAry[i] = traj;
            i++;
        }
        return trajAry;
    }

    public static Trajectory[] getODWTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {
        Trajectory[] ODTraj = getODTraj(begin, end, trajectory, optIndex);
        return getWayPointTraj(0, ODTraj.length, ODTraj, optIndex);
    }

    /**
     * checks whether the location is in the region
     */
    private static boolean inCheck(RectRegion r, Location loc, UnfoldingMap map) {
        if (r == null) {
            return true;
        }

        r.updateScreenPosition(map);    // TODO How about run it before the whole alg ?

        ScreenPosition sp = map.getScreenPosition(loc);
        double px = sp.x;
        double py = sp.y;

        Position leftTop = r.leftTop;
        Position rightBtm = r.rightBtm;

        return (px >= leftTop.x && px <= rightBtm.x)
                && (py >= leftTop.y && py <= rightBtm.y);
    }

    private static boolean inCheck(CircleRegion circleRegion, Location loc, UnfoldingMap map) {
        if (circleRegion == null) {
            return true;
        }

        circleRegion.updateCircleScreenPosition();    // TODO How about run it before the whole alg ?

        ScreenPosition sp = map.getScreenPosition(loc);

        double px = sp.x;
        double py = sp.y;

        float radius = circleRegion.getRadius();
        ScreenPosition circleCenter = map.getScreenPosition(circleRegion.getCircleCenter());
        float centerX = circleCenter.x;
        float centerY = circleCenter.y;

        return Math.pow((Math.pow(centerX - px, 2) + Math.pow(centerY - py, 2)), 0.5) < radius;
    }

    //16.26
    private static boolean inCheck(ArrayList<RectRegion> rList, Location loc, UnfoldingMap map) {
        if (rList == null) {
            return false;
        }
        ScreenPosition sp = map.getScreenPosition(loc);
        double px = sp.x;
        double py = sp.y;
        for (RectRegion r : rList) {
            r.updateScreenPosition(map);        // TODO How about run it before the whole alg ?
            Position leftTop = r.leftTop;
            Position rightBtm = r.rightBtm;
            if ((px >= leftTop.x && px <= rightBtm.x)
                    && (py >= leftTop.y && py <= rightBtm.y)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inCheckCircleList(ArrayList<CircleRegion> circleList, Location loc, UnfoldingMap map) {
        if (circleList == null) {
            return false;
        }
        ScreenPosition sp = map.getScreenPosition(loc);
        double px = sp.x;
        double py = sp.y;
        for (CircleRegion circleRegion : circleList) {
            circleRegion.updateCircleScreenPosition();        // TODO How about run it before the whole alg ?
            float radius = circleRegion.getRadius();
            ScreenPosition circleCenter = map.getScreenPosition(circleRegion.getCircleCenter());
            float centerX = circleCenter.x;
            float centerY = circleCenter.y;

            if (Math.pow((Math.pow(centerX - px, 2) + Math.pow(centerY - py, 2)), 0.5) < radius) {
                return true;
            }
        }

        return false;
    }
}
