package app;

import model.RectRegion;
import model.Trajectory;
import model.TrajectoryMeta;
import processing.core.PGraphics;

import java.util.ArrayList;

public class TimeProfileSharedObject {
    private static TimeProfileSharedObject instance = new TimeProfileSharedObject();
    private ArrayList<RectRegion> qudaRegion = new ArrayList<>();
    public boolean drawDone = false;

    public void addQuadRectRegion(RectRegion rectRegion) {
        qudaRegion.add(rectRegion);
    }

    public ArrayList<RectRegion> getQudaRegion() {
        return qudaRegion;
    }

    public static TimeProfileSharedObject getInstance() {
        return instance;
    }

    private TimeProfileSharedObject() {
    }

    public PGraphics[] trajImageMtx;
    public Trajectory[][] trajRes;

    public TrajectoryMeta[][] trajMetaRes;

    public Trajectory[] trajShow;
    public TrajectoryMeta[] trajectoryMetas;
    boolean calDone = false;

    public TrajectoryMeta[] trajMetaFull;

    public ArrayList<RectRegion> searchRegions = new ArrayList<>();

    public void setTrajMatrix(PGraphics pg, int id) {
        trajImageMtx[id] = pg;
    }
}
