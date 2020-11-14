package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PApplet;
import util.IOHandle;

import java.util.Objects;


public class MapCoordinateTest extends PApplet {
    private static final String APP_NAME = "MapCoordinateTest";
    private static final String DATA_PATH = "data/GPS/porto_full.txt";
    // traj limit when loading. -1 for no limit
    public static final int LIMIT = 1000;
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 800;

    private static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    private UnfoldingMap map;

    private Trajectory[] trajList;
    private MapPoint[][] mpMtx;

    private boolean printTime = false;
    private int methodIdx = 0;

    /* coordination param */

    private static final Location PORTO_CENTER = new Location(41.14, -8.639);
    private static final Location REF_LOC_1 = new Location(70, -170);
    private static final Location REF_LOC_2 = new Location(-70, 170);
    private float spXRef, spYRef;
    private float scaleRef = -1f;


    // inner class represent map point
    private static class MapPoint {
        float x, y;

        public MapPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MapPoint)) {
                return false;
            }
            MapPoint mp = (MapPoint) obj;
            return mp.x == x && mp.y == y;
        }

        @Override
        public String toString() {
            return "(" + x + " " + y + ")";
        }
    }


    @Override
    public void settings() {
        // window settings
        size(WIDTH, HEIGHT, P2D);
    }

    @Override
    public void setup() {
        // frame settings
        textSize(20);

        // map settings
        map = new UnfoldingMap(this, APP_NAME, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));
        map.setZoomRange(1, 20);
        map.setBackgroundColor(255);
        noFill();
        stroke(255, 0, 0);
        strokeWeight(1);

        loadTraj();
        initMPMtx();

        // add mouse and keyboard interactions
        MapUtils.createDefaultEventDispatcher(this, map);

        map.zoomAndPanTo(12, PORTO_CENTER);
        refreshTitle();
    }

    /**
     * Load trajectory data from file and draw it on the map.
     */
    public void loadTraj() {
        trajList = IOHandle.loadRowData(DATA_PATH, LIMIT);
        System.out.println("Load Finished. Traj list size: " + trajList.length);
    }

    /**
     * GPS -> {@link MapPoint}.
     * Also init scale and position ref
     */
    private void initMPMtx() {
        map.zoomAndPanTo(20, PORTO_CENTER);

        long time = -System.currentTimeMillis();
        int pointCnt = 0;

        mpMtx = new MapPoint[trajList.length][];
        int trajIdx = 0;
        for (Trajectory traj : trajList) {
            Location[] locations = traj.getLocations();
            MapPoint[] mpList = new MapPoint[locations.length];
            pointCnt += mpList.length;
            for (int i = 0; i < locations.length; i++) {
                ScreenPosition sp = map.getScreenPosition(locations[i]);
                mpList[i] = new MapPoint(sp.x, sp.y);
            }
            mpMtx[trajIdx++] = mpList;
        }

        time += System.currentTimeMillis();
        System.out.printf("GPS -> MP: %d ms, tot position cnt: %d%n", time, pointCnt);

        // init scaleRef
        ScreenPosition sp1 = map.getScreenPosition(REF_LOC_1);
        ScreenPosition sp2 = map.getScreenPosition(REF_LOC_2);
        scaleRef = (sp1.x - sp2.x) + (sp1.y - sp2.y);

        ScreenPosition spRef = map.getScreenPosition(PORTO_CENTER);
        spXRef = spRef.x;
        spYRef = spRef.y;

        map.zoomAndPanTo(12, PORTO_CENTER);
    }

    public void refreshTitle() {
        String str = String.format(APP_NAME + " [%5.2f fps] ", frameRate);
        if (!map.allTilesLoaded()) {
            str += "Loading map...";
        } else {
            str += "Completed";
        }
        surface.setTitle(str);
    }

    @Override
    public void draw() {
        refreshTitle();
        map.draw();

        switch (methodIdx) {
            case 1:
                drawTraj1();
                break;
            case 2:
                drawTraj2();
                break;
        }
    }

    /**
     * Old method
     */
    private void drawTraj1() {
        long time = -System.currentTimeMillis();

        for (Trajectory traj : trajList) {
            beginShape();
            for (Location loc : traj.getLocations()) {
                ScreenPosition pos = map.getScreenPosition(loc);
                vertex(pos.x, pos.y);
            }
            endShape();
        }

        time += System.currentTimeMillis();
        if (printTime) {
            printTime = false;
            System.out.println("drawTraj cost: " + time + " ms");
            ScreenPosition pos = map.getScreenPosition(trajList[0].getLocations()[0]);
            System.out.println(pos.x + " " + pos.y);
        }
    }

    /**
     * New method use position translate
     */
    private void drawTraj2() {
        long time = -System.currentTimeMillis();

        ScreenPosition sp1 = map.getScreenPosition(REF_LOC_1);
        ScreenPosition sp2 = map.getScreenPosition(REF_LOC_2);
        float factor = ((sp1.x - sp2.x) + (sp1.y - sp2.y)) / scaleRef;

//        setTranslate();

        for (MapPoint[] mpList : mpMtx) {
            beginShape();
            for (MapPoint mp : mpList) {
                ScreenPosition pos = computeScreenPosition(mp, factor);
                vertex(pos.x, pos.y);
            }
            endShape();
        }

        // reset translate
        translate(0, 0);

        time += System.currentTimeMillis();
        if (printTime) {
            printTime = false;
            System.out.println("drawTraj2 cost: " + time + " ms");
            ScreenPosition pos = computeScreenPosition(mpMtx[0][0], factor);
            System.out.println(pos.x + " " + pos.y);
        }
    }

    private void setTranslate() {
        ScreenPosition spCenter = map.getScreenPosition(PORTO_CENTER);
        translate(spCenter.x - spXRef, spCenter.y - spYRef);
    }

    private ScreenPosition computeScreenPosition(MapPoint mp, float factor) {
//        float x, y;
//        x = (mp.x - spXRef) * factor + spXRef;
//        y = (mp.y - spYRef) * factor + spYRef;
//        // FIXME
////        return new ScreenPosition(x, y);
//        return new ScreenPosition(Math.round(x), Math.round(y));

        ScreenPosition spCenter = map.getScreenPosition(PORTO_CENTER);
        float x, y;
        x = (mp.x - spXRef) * factor + spCenter.x;
        y = (mp.y - spYRef) * factor + spCenter.y;
        // FIXME
//        return new ScreenPosition(x, y);
        return new ScreenPosition(Math.round(x), Math.round(y));
    }

    @Override
    public void keyReleased() {
        switch (key) {
            case 't':
                printTime = true;
                break;
            case '0':
                methodIdx = 0;
                break;
            case '1':
                methodIdx = 1;
                break;
            case '2':
                methodIdx = 2;
                break;
            case '3':
                methodIdx = 3;
                break;
            case '=':
                map.zoomLevelIn();
                break;
            case '-':
                map.zoomLevelOut();
                break;
        }
    }

    public static void main(String[] args) {
        PApplet.main(MapCoordinateTest.class.getName());
    }
}