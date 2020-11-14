package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MarkerManager;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;
import util.DM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple test to get familiar with Processing & Unfolding.
 * <p>
 * Use {@link DM} to load data.<br>
 * Here, <b>the traj score is not used</b>. For the tidy set, uniform selection is chosen.
 * Notice that <b>the stuck problem is not solved here</b>.
 * Temporary measure is pressing blank to reduce the traj # when moving.
 */
public class SimpleMapApp extends PApplet {
    private static final String APP_NAME = "SimpleMapApp";
    private static final String DATA_PATH = "data/porto_full.txt";
    // traj limit when loading. -1 for no limit
    public static final int LIMIT = 30_0000;
    // # of traj for tidy set
    public static final int TIDY_SIZE = 5000;

    // map params
    private static final Location PORTO_CENTER = new Location(41.14, -8.639);
    private static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    private UnfoldingMap map;
    private int status = -1;     // -1: loading, 0: stable mode, 1: move mode
    private int nextStatus = -1;
    private ArrayList<MarkerManager<Marker>> managers;      // different traj set for shown

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

        thread("loadTraj");

        // add mouse and keyboard interactions
        MapUtils.createDefaultEventDispatcher(this, map);

        refreshTitle();
    }

    /**
     * Load trajectory data from file and draw it on the map.
     */
    public void loadTraj() {
        // load features from text
        Feature[] list = /*DM.loadRowData(DATA_PATH, LIMIT)*/ null;
        if (list == null) { exit(); }
        assert list != null;
        List<Feature> transitLines = Arrays.asList(list);

        // generate tidy traj set
        List<Feature> transitLinesTidy = new ArrayList<>();
        int step = transitLines.size() / TIDY_SIZE;
        for (int i = 0; i < transitLines.size(); i += step) {
            transitLinesTidy.add(transitLines.get(i));
        }

        // generate marks and set color
        List<Marker> transitMarkers = MapUtils.createSimpleMarkers(transitLines);
        for (Marker m : transitMarkers) {
            m.setColor(color(255, 0, 0));
        }
        List<Marker> transitMarkersTidy = MapUtils.createSimpleMarkers(transitLinesTidy);
        for (Marker m : transitMarkersTidy) {
            m.setColor(color(255, 0, 0));
        }

        // tie to the map
        managers = new ArrayList<>();
        managers.add(new MarkerManager<>(transitMarkers));
        managers.add(new MarkerManager<>(transitMarkersTidy));
        map.addMarkerManager(managers.get(1));

        nextStatus = 1;

        System.out.println("Total load: " + transitMarkers.size());
    }

    public void refreshTitle() {
        String str = String.format(APP_NAME + " [%5.2f fps] ", frameRate);

        if (status == 1 && nextStatus == 0) {
            // change to non-tidy set
            surface.setTitle(str + "Painting trajectories...");
            return;
        }

        if (status == -1) {
            // reading data
            surface.setTitle(str + "Reading data from file...");
            return;
        }

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
        if (status != nextStatus) {
            // refresh status
            status = nextStatus;
            if (status == 1) {
                map.removeMarkerManager(0);
                map.addMarkerManager(managers.get(1));
            } else if (status == 0){
                map.removeMarkerManager(0);
                map.addMarkerManager(managers.get(0));
            }
        }
        map.draw();
    }

    @Override
    public void keyReleased() {
        switch (key) {
            case ' ':
                // not change if it is -1
                nextStatus ^= (nextStatus == -1) ? 0 : 1;
                break;
            case 'q':
                exit();
                break;
        }
    }
}