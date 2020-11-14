package index;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.geo.Location;
import model.QuadRegion;
import model.RectRegion;
import model.TrajToQuality;
import model.TrajectoryMeta;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static index.QuadTree.getWayPointPos;

public class IndexStructManager {
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    private TrajectoryMeta[] trajFull;
    private int H;

    public IndexStructManager(double minLat, double maxLat, double minLon, double maxLon, TrajectoryMeta[] trajFull, int h) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.trajFull = trajFull;
        H = h;
    }

    public QuadRegion startIndexStructure() {
        // init root
        RectRegion rectRegion = new RectRegion();
        rectRegion.initLoc(new Location(minLat, minLon), new Location(maxLat, maxLon));
        TimeProfileSharedObject.getInstance().addQuadRectRegion(rectRegion);
        QuadRegion quadRegion = new QuadRegion(minLat, maxLat, minLon, maxLon);
        TrajToQuality[] trajToQualities = VfgsForIndex.getVfgs(trajFull);
        quadRegion.setTrajQuality(trajToQualities);

        if (H > 1) {
            //start children
            ExecutorService indexPool = new ThreadPoolExecutor(4, 4,
                    0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

            double latOff = (maxLat - minLat) / 2;
            double lonOff = (maxLon - minLon) / 2;
            for (int i = 0; i < 4; i++) {
                int laxId = i / 2;
                int lonId = i % 2;
                double tmpLatMin = minLat + latOff * laxId;
                double tmpLonMin = minLon + lonOff * lonId;

                TrajectoryMeta[] wayPoint = getWayPointPos(trajFull, tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff);
                IndexStructWorker indexStructWorker = new IndexStructWorker(tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff,
                        wayPoint, H - 1, this, i);
                indexPool.submit(indexStructWorker);
            }

            indexPool.shutdown();
            try {
                indexPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
            quadRegion.setQuadRegionChildren(childrenQuad);
            System.out.println("index done");
        }

        return quadRegion;
    }

    private QuadRegion[] childrenQuad = new QuadRegion[4];

    public void setQuadRegion(QuadRegion quadRegion, int id) {
        childrenQuad[id] = quadRegion;
    }
}
