package draw;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.Trajectory;
import model.TrajectoryMeta;
import processing.core.PGraphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrajDrawWorkerSingleMap extends Thread {
    private PGraphics pg;
    private UnfoldingMap map;
    private int begin;
    private int end;
    private Trajectory[] trajList;
    private TrajectoryMeta[] trajMetaList;
    private int id;

    private TrajectoryMeta[] trajMetaFull;

    public volatile boolean stop = false;

    public TrajDrawWorkerSingleMap(PGraphics pg, UnfoldingMap map, int begin, int end, Trajectory[] trajList) {
        this.pg = pg;
        this.map = map;
        this.begin = begin;
        this.end = end;
        this.trajList = trajList;
        this.stop = stop;

        this.setPriority(9);
    }

    public TrajDrawWorkerSingleMap(PGraphics pg, UnfoldingMap map, int begin, int end, TrajectoryMeta[] trajMetaList) {
        this.pg = pg;
        this.map = map;
        this.begin = begin;
        this.end = end;
        this.trajMetaList = trajMetaList;
        this.stop = stop;

        this.trajMetaFull = TimeProfileSharedObject.getInstance().trajMetaFull;

        this.setPriority(9);
    }

    @Override
    public void run() {
        try {
            long t0 = System.currentTimeMillis();

            pg.beginDraw();
            pg.noFill();
            pg.strokeWeight(0.5f);
            pg.stroke(new Color(255, 0, 0).getRGB());

            /*
            ArrayList<ArrayList<Point>> trajPointList = new ArrayList<>();
            for (int i = begin; i < end; i++) {
                ArrayList<Point> pointList = new ArrayList<>();
                for (Position position : trajList[i].getPositions()) {
                    if (this.stop) {
                        System.out.println(this.getName() + " cancel");
                        return;
                    }
                    Location loc = new Location(position.x / 10000.0, position.y / 10000.0);
//                    System.out.println(loc);
                    ScreenPosition pos = map.getScreenPosition(loc);
                    pointList.add(new Point(pos.x, pos.y));
                }
                trajPointList.add(pointList);
            }
            */
/*
        for (int i = begin; i < end; i++) {
            ArrayList<Point> pointList = new ArrayList<>();
            for (Location loc : trajList[i].getLocations()) {
                if (this.stop) {
                    System.out.println(this.getName() + " cancel");
                    return;
                }
                ScreenPosition pos = map.getScreenPosition(loc);
                pointList.add(new Point(pos.x, pos.y));
            }
            trajPointList.add(pointList);
        }


 */
/*
            for (ArrayList<Point> traj : trajPointList) {
                pg.beginShape();
                for (Point pos : traj) {
                    if (this.stop) {
                        System.out.println(this.getName() + " cancel");
                        pg.endShape();
                        pg.endDraw();
                        return;
                    }
                    pg.vertex(pos.x, pos.y);
                }
                pg.endShape();
            }
            */
/*
            for (int i = begin; i < end; i++) {
                Trajectory traj = trajList[i];

                pg.beginShape();
                for (Position position : traj.getPositions()) {
                    Location loc = new Location(position.x / 10000.0, position.y / 10000.0);
                    ScreenPosition pos = map.getScreenPosition(loc);
                    pg.vertex(pos.x, pos.y);
                }
                pg.endShape();
            }

 */
/*
            for (int i = begin; i < end; i++) {
                TrajectoryMeta traj = trajMetaList[i];

                pg.beginShape();
                for (Position position : traj.getPositions()) {
                    Location loc = new Location(position.x / 10000.0, position.y / 10000.0);
                    ScreenPosition pos = map.getScreenPosition(loc);
                    pg.vertex(pos.x, pos.y);
                }
                pg.endShape();
            }
            */
/*
            for (int i = begin; i < end; i++) {
                TrajectoryMeta traj = trajMetaList[i];

                pg.beginShape();
                for (GpsPosition gpsPosition : traj.getGpsPositions()) {
                    Location loc = new Location(gpsPosition.lat, gpsPosition.lon);
                    ScreenPosition pos = map.getScreenPosition(loc);
                    pg.vertex(pos.x, pos.y);
                }
                pg.endShape();
            }
            */
            int cnt = 0;
            ArrayList<ArrayList<ScreenPosition>> trajPointList = new ArrayList<>();
            for (int i = begin; i < end; i++) {
                ArrayList<ScreenPosition> pointList = new ArrayList<>();
                for (Position pos : generatePosList(trajMetaList[i])) {
                    cnt += 1;
                    if (this.stop) {
                        System.out.println(this.getName() + " cancel");
                        return;
                    }
                    Location loc = new Location(pos.x / 10000.0, pos.y / 10000.0);
                    ScreenPosition screenPos = map.getScreenPosition(loc);
                    pointList.add(screenPos);
                }
                trajPointList.add(pointList);
            }

            for (ArrayList<ScreenPosition> traj : trajPointList) {
                pg.beginShape();
                for (ScreenPosition pos : traj) {
                    if (this.stop) {
                        System.out.println(this.getName() + " cancel");
                        pg.endShape();
                        pg.endDraw();
                        return;
                    }
                    pg.vertex(pos.x, pos.y);
//                    pg.point(pos.x, pos.y);
//                    System.out.println(pos.x + ", " + pos.y);
                }
                pg.endShape();
            }
            System.out.println("point num: " + cnt);
            System.out.println(">>>>render time: " + (System.currentTimeMillis() - t0) + " ms");
            TimeProfileSharedObject.getInstance().drawDone = true;
            TimeProfileSharedObject.getInstance().setTrajMatrix(pg, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Position> generatePosList(TrajectoryMeta trajMeta) {
        int trajId = trajMeta.getTrajId();
        int begin = trajMeta.getBegin();
        int end = trajMeta.getEnd();      // notice that the end is included

        return Arrays.asList(trajMetaFull[trajId].getPositions()).subList(begin, end + 1);
    }

    public static class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
