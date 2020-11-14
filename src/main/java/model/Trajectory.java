package model;

import de.fhpotsdam.unfolding.geo.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Trajectory {
    public Location[] locations;
    private double score;
    private double metaScore;
    private int trajId;

    private Position[] metaGPS;
    private int[] latOrder;
    private int[] lonOrder;

    public void setMetaGPS(Position[] metaGPS) {
        this.metaGPS = metaGPS;
        latOrder = new int[metaGPS.length];
        lonOrder = new int[metaGPS.length];
    }

    public Trajectory(int trajId) {
        score = 0;
        this.trajId = trajId;
    }

    public Trajectory(double score) {
        this.score = score;
    }

    Trajectory() {
        score = 0;
    }

    public void setScore(double score) {
        this.score = score;
        metaScore = score;
    }

    public void updateScore(double score){
        this.score = score;
    }
    public double getScore() {
        return score;
    }

    public int getTrajId() {
        return trajId;
    }

    public void setLocations(Location[] locations) {
        this.locations = locations;
    }

    public Location[] getLocations() {
        return locations;
    }


//    public Location[] getSubTraj(RectRegion rectRegion) {
//        float leftLat = rectRegion.getLeftTopLoc().getLat();
//        float leftLon = rectRegion.getLeftTopLoc().getLon();
//        float rightLon = rectRegion.getRightBtmLoc().getLon();
//        float rightLat = rectRegion.getRightBtmLoc().getLat();
//
//        HashSet<Integer> latSet = getLatSubTraj(leftLat, rightLat);
//        HashSet<Integer> lonSet = getLonSubTraj(leftLon, rightLon);
//
//        latSet.retainAll(lonSet);
//        return timeOrder(latSet);
//    }

//    private HashSet<Integer> getLatSubTraj(float leftLat, float rightLat) {
//        int begion = getFirstSubId(leftLat, latOrder);
//        int end = getLastSubId(rightLat, latOrder);
//
//        HashSet<Integer> latSet = new HashSet<>();
//        for (int i = begion; i < end; i++) {
//            latSet.add(metaGPS[i].timeOrder);
//        }
//        return latSet;
//    }

//    private HashSet<Integer> getLonSubTraj(float leftLon, float rightLon) {
//        int begion = getFirstSubId(leftLon, latOrder);
//        int end = getLastSubId(rightLon, latOrder);
//
//        HashSet<Integer> lonSet = new HashSet<>();
//        for (int i = begion; i < end; i++) {
//            lonSet.add(metaGPS[i].timeOrder);
//        }
//        return lonSet;
//    }

    private int getFirstSubId(float x, int[] list) {
        int lo = 0, hi = locations.length - 1;

        while (lo <= hi) {
            int mi = (lo + hi) / 2; // 2
            if (!(list[mi] >= x)) {
                lo = mi + 1;
            } else {
                hi = mi - 1;
            }
        }

        if (lo >= locations.length) {
            return -1;
        }
        return lo;
    }

    private int getLastSubId(float x, int[] list) {
        int lo = 0, hi = locations.length - 1;

        while (lo <= hi) {
            int mi = (lo + hi) / 2; // 2
            if (list[mi] >= x) {
                lo = mi + 1;
            } else {
                hi = mi - 1;
            }
        }

        return hi;
    }

//    private Location[] timeOrder(HashSet<Integer> set) {
//        ArrayList<Position> tmp_list = new ArrayList<>();
//        for (Integer e : set) {
//            tmp_list.add(metaGPS[e]);
//        }
//        Collections.sort(tmp_list);
//        Location[] loc = new Location[tmp_list.size()];
//        int i = 0;
//        for (Position pos : tmp_list) {
//            loc[i++] = locations[pos.timeOrder];
//        }
//        return loc;
//    }

    private Position[] positions;
    public void setPositions(Position[] posi){
        positions = posi;
    }
    public Position[] getPositions(){
        return positions;
    }
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (Location p : this.locations) {
            res.append(",").append(p.y).append(",").append(p.x);
        }
        return res.substring(1);
    }

    public void scoreInit() {
        score = metaScore;
    }
}
