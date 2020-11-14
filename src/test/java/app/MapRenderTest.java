package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.Trajectory;
import processing.core.PApplet;
import util.IOHandle;


public class MapRenderTest extends PApplet {
    private static final String APP_NAME = "MapCoordinateTest";
    private static final String DATA_PATH = "data/GPS/porto_full.txt";
    // traj limit when loading. -1 for no limit
    public static final int LIMIT = 10_0000;

    private static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    private UnfoldingMap map;

    private Trajectory[] trajList;
    private Position[][] posMtx;

    private boolean printTime = false;
    private int methodIdx = 0;

    /* coordination param */

    private static final Location PORTO_CENTER = new Location(41.14, -8.639);

    @Override
    public void settings() {
        // window settings
        size(1200, 800, P2D);
    }

    @Override
    public void setup() {
        // frame settings
        textSize(20);

        // map settings
        map = new UnfoldingMap(this, APP_NAME, new MapBox.CustomMapBoxProvider(WHITE_MAP_PATH));
        map.setZoomRange(1, 20);
        map.zoomAndPanTo(12, PORTO_CENTER);
        map.setBackgroundColor(255);
        noFill();
        stroke(255, 0, 0);
        strokeWeight(1);

        loadTraj();
        initPosMtx();

        // add mouse and keyboard interactions
//        MapUtils.createDefaultEventDispatcher(this, map);

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
     * GPS -> {@link model.Position}
     */
    private void initPosMtx() {
        long time = -System.currentTimeMillis();
        int posCnt = 0;

        posMtx = new Position[trajList.length][];
        int trajIdx = 0;
        for (Trajectory traj : trajList) {
            Location[] locations = traj.getLocations();
            Position[] posList = new Position[locations.length];
            posCnt += posList.length;
            for (int i = 0; i < locations.length; i++) {
                ScreenPosition sp = map.getScreenPosition(locations[i]);
                posList[i] = new Position(sp.x, sp.y);
            }
            posMtx[trajIdx++] = posList;
        }

        time += System.currentTimeMillis();
        System.out.printf("GPS -> SP: %d ms, tot position cnt: %d%n", time, posCnt);

        // drop trajList
        trajList = null;
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

        for (Position[] posList : posMtx) {
            beginShape();
            for (Position pos : posList) {
                vertex(pos.x, pos.y);
            }
            endShape();
        }

        time += System.currentTimeMillis();
        if (printTime) {
            printTime = false;
            System.out.println("drawTraj cost: " + time + " ms");
        }
    }

    private void drawTraj2() {
        long time = -System.currentTimeMillis();

        beginShape(LINES);
        Position pos;
        for (Position[] posList : posMtx) {
            if (posList.length <= 1) {
                continue;
            }

            // first one point
            pos = posList[0];
            vertex(pos.x, pos.y);

            // internal points
            for (int i = 1; i < posList.length - 1; i++) {
                pos = posList[i];
                vertex(pos.x, pos.y);
                vertex(pos.x, pos.y);
            }

            // last point
            pos = posList[posList.length - 1];
            vertex(pos.x, pos.y);
        }
        endShape();

        time += System.currentTimeMillis();
        if (printTime) {
            printTime = false;
            System.out.println("drawTraj2 cost: " + time + " ms");
        }
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
        }
    }

    /* coordinate test */

    public static void main(String[] args) {
        PApplet.main(MapRenderTest.class.getName());
    }
}