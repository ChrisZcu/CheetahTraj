package select;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.geo.Location;
import model.*;

import java.util.ArrayList;

public class TimeProfileWorker extends Thread {
    private int begin;
    private int end;
    private Trajectory[] trajectory;
    private TrajectoryMeta[] trajectoryMeta;

    private int id;
    private float leftLon;
    private float leftLat;
    private float rightLon;
    private float rightLat;

    boolean isMeta = false;

    public TimeProfileWorker(int begin, int end, Trajectory[] trajectory, int id, RectRegion region) {
        this.begin = begin;
        this.end = end;
        this.trajectory = trajectory;
        this.id = id;
        leftLat = region.getLeftTopLoc().getLat();
        leftLon = region.getLeftTopLoc().getLon();
        rightLon = region.getRightBtmLoc().getLon();
        rightLat = region.getRightBtmLoc().getLat();

    }

    public TimeProfileWorker(int begin, int end, TrajectoryMeta[] trajectory, int id, RectRegion region) {
        this.begin = begin;
        this.end = end;
        this.trajectoryMeta = trajectory;
        this.id = id;
        leftLat = region.getLeftTopLoc().getLat();
        leftLon = region.getLeftTopLoc().getLon();
        rightLon = region.getRightBtmLoc().getLon();
        rightLat = region.getRightBtmLoc().getLat();
        isMeta = true;

    }

    @Override
    public void run() {
        try {
            if (!isMeta) {
                Trajectory[] res = getWayPoint();
//                Trajectory[] res = getWayPointPos();
                TimeProfileSharedObject.getInstance().trajRes[id] = res;
            } else {
                TimeProfileSharedObject.getInstance().trajMetaRes[id] = getWayPointPosMeta();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Trajectory[] getWayPointPos() {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            for (Position position : traj.getPositions()) {
                if (inCheck(position)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return cutTrajsPos(res.toArray(new Trajectory[0]));
    }

    private TrajectoryMeta[] getWayPointPosMeta() {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            TrajectoryMeta traj = trajectoryMeta[i];
            for (GpsPosition gpsPosition : traj.getGpsPositions()) {
                if (inCheck(gpsPosition)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return cutTrajsPos(res.toArray(new TrajectoryMeta[0]));
    }

    //
    private Trajectory[] getWayPoint() {
        ArrayList<Trajectory> res = new ArrayList<>();

        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            for (Location loc : traj.locations) {
                if (inCheck(loc)) {
                    res.add(traj);
                    break;
                }
            }
        }
//        return res.toArray(new Trajectory[0]);
//        System.out.println(res.size());
        return cutTrajs(res.toArray(new Trajectory[0]));
    }

    private boolean inCheck(GpsPosition position) {
        return position.lat >= Math.min(leftLat, rightLat) && position.lat <= Math.max(leftLat, rightLat)
                && position.lon >= Math.min(leftLon, rightLon) && position.lon <= Math.max(leftLon, rightLon);
    }

    private boolean inCheck(Position position) {
        return position.x >= Math.min(leftLat, rightLat) && position.x <= Math.max(leftLat, rightLat)
                && position.y >= Math.min(leftLon, rightLon) && position.y <= Math.max(leftLon, rightLon);
    }

    private boolean inCheck(Location loc) {
        return loc.getLat() >= Math.min(leftLat, rightLat) && loc.getLat() <= Math.max(leftLat, rightLat)
                && loc.getLon() >= Math.min(leftLon, rightLon) && loc.getLon() <= Math.max(leftLon, rightLon);
    }

    private Trajectory[] cutTrajs(Trajectory[] trajectories) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (Trajectory traj : trajectories) {
            res.addAll(getRegionInTraj(traj));
        }
        return res.toArray(new Trajectory[0]);
    }

    private TrajectoryMeta[] cutTrajsPos(TrajectoryMeta[] trajectories) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (TrajectoryMeta traj : trajectories) {
            res.addAll(getRegionInTrajPos(traj));
        }
        return res.toArray(new TrajectoryMeta[0]);
    }

    private Trajectory[] cutTrajsPos(Trajectory[] trajectories) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (Trajectory traj : trajectories) {
            res.addAll(getRegionInTrajPos(traj));
        }
        return res.toArray(new Trajectory[0]);
    }

    private ArrayList<Trajectory> getRegionInTraj(Trajectory traj) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = 0; i < traj.locations.length; i++) {
            if (inCheck(traj.locations[i])) {
                Trajectory trajTmp = new Trajectory(-1);
                Location loc = traj.locations[i++];
                ArrayList<Location> locTmp = new ArrayList<>();
                while (inCheck(loc) && i < traj.locations.length) {
                    locTmp.add(loc);
                    loc = traj.locations[i++];
                }
                trajTmp.locations = locTmp.toArray(new Location[0]);
                trajTmp.setScore(locTmp.size());
                res.add(trajTmp);
            }
        }
        return res;
    }

    private ArrayList<Trajectory> getRegionInTrajPos(Trajectory traj) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = 0; i < traj.getPositions().length; i++) {
            if (inCheck(traj.getPositions()[i])) {
                Trajectory trajTmp = new Trajectory(-1);
                Position position = traj.getPositions()[i++];
                ArrayList<Position> locTmp = new ArrayList<>();
                while (inCheck(position) && i < traj.locations.length) {
                    locTmp.add(position);
                    position = traj.getPositions()[i++];
                }
                trajTmp.setPositions(locTmp.toArray(new Position[0]));
                trajTmp.setScore(locTmp.size());
                res.add(trajTmp);
            }
        }
        return res;
    }

    private ArrayList<TrajectoryMeta> getRegionInTrajPos(TrajectoryMeta traj) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (int i = 0; i < traj.getGpsPositions().length; i++) {
            if (inCheck(traj.getGpsPositions()[i])) {
                TrajectoryMeta trajTmp = new TrajectoryMeta(-1);
                GpsPosition gpsPosition = traj.getGpsPositions()[i++];
                ArrayList<GpsPosition> locTmp = new ArrayList<>();
                while (inCheck(gpsPosition) && i < traj.getGpsPositions().length) {
                    locTmp.add(gpsPosition);
                    gpsPosition = traj.getGpsPositions()[i++];
                }
                trajTmp.setGpsPositions(locTmp.toArray(new GpsPosition[0]));
                trajTmp.setScore(locTmp.size());
                res.add(trajTmp);
            }
        }
        return res;
    }
}
