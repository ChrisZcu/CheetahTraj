package index;

import model.QuadRegion;
import model.TrajectoryMeta;

public class IndexStructWorker extends Thread {
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    private TrajectoryMeta[] trajFull;
    private int H;
    private IndexStructManager manager;
    private int id;

    public IndexStructWorker(double minLat, double maxLat, double minLon, double maxLon, TrajectoryMeta[] trajectoryMeta,
                             int H, IndexStructManager manager, int id) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.manager = manager;
        this.trajFull = trajectoryMeta;
        this.H = H;
        this.id = id;
    }

    @Override
    public void run() {
        QuadRegion quadRegion = QuadTree.getQuadIndex(minLat, maxLat, minLon, maxLon, trajFull, H);
        manager.setQuadRegion(quadRegion, id);
    }
}
