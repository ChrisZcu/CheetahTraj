package vqgs.util;

import de.fhpotsdam.unfolding.geo.Location;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static vqgs.util.WF.*;

/**
 * Parameters Setting Class
 * <p>
 * Store and provide the setting params using
 * from data processing to UI displaying.
 * <p>
 * Use {@link ParamOptimize} to detect data and find better params.
 */
public final class PSC {

    /* visual settings */

    // window
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 800;

    // map
    // the center when display the map
    public static final int BEGIN_CENTER_ID = 0;
    public static final Location[] PRESET_CENTER = {
            /* porto */
            new Location(41.142063, -8.639684),     // 0 (pid, same below): row 1 & 2.
            new Location(41.23, -8.63),             // 1: row 3
            new Location(41.14571, -8.638836),      // 2: fig 4
            new Location(41.215004, -8.487775),     // 3: fig 6a
            new Location(41.20363, -8.303903),      // 4: fig 6b
            new Location(41.193108, -8.520111),     // 5: fig 6c
            new Location(41.1437, -8.638987),       // 6: fig 6d
            /* shenzhen*/
//			new Location(22.637138, 113.836464),    // row 1, level=11
//			new Location(22.640322, 113.835434),    // row 2, level=13
//			new Location(22.629078, 114.02894),     // row 3, level=15
    };

    public static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    // color
    // null BREAKS for auto-compute
    public static final int[] BREAKS = null; /*{1, 2, 3, 4, 5};*/    // (x, y]
    // six provided color to algorithm
    public static final Color[] COLORS = {
            new Color(15, 91, 120),
            new Color(19, 149, 186),
            new Color(162, 184, 108),
            new Color(235, 200, 68),
            new Color(241, 108, 32),
            new Color(190, 46, 29),
    };

    // app
    // recommend: core # * 2 or little higher
    public static final int THREAD_NUM = 10;


    /* VFGS algorithm settings */

    public static final WF[] PROCESS_LIST = {
            PRE_PROCESS,
            VFGS_CAL,
            VFGS_COLOR_CAL,
            QUALITY_CAL,
    };

    // the center for computing
    public static final Location CELL_CENTER =
            new Location(42.0273551940919, -9.179738998413087);     // porto
//			new Location(22.999967575073243, 113.10578155517577);   // shenzhen

    // i.e. alpha.
    // These two settings must match the results
    public static double[] RATE_LIST
            = {0.05, 0.01, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001};
    public static int[] DELTA_LIST
            = {0, 4, 8, 16, 32, 50, 64, 128};

    public static boolean LOAD_VFGS = true;
    public static boolean LOAD_COLOR = true;

    public static final int[] ZOOM_RANGE = {11, 20};    // [begin, end]


    /* data and file settings */

    // traj limit for full set. -1 for no limit
    public static final int LIMIT = -1;
    // save __***.txt. If run them separately, this must be true
    public static final boolean SAVE_TEMP = false;
    // see me an email when program finished / failed
    public static final boolean SEND_EMAIL = false;

    // origin data src path
    public static String ORIGIN_PATH
            = "data/porto_5k/porto_5k.txt";

    public static String PATH_PREFIX
            = "data/tmp/";

    public static final String LOG_PATH = PATH_PREFIX + "log.txt";

    /**
     * Position set of all origin trajs (i.e. C in paper).
     * <br>Format: traj id, pos1, pos2 ...
     */
    public static final String POS_INFO_PATH = PATH_PREFIX + "__pos_info.csv";

    /**
     * Temp output for old-version quality function
     */
    public static String SCORE_PATH = PATH_PREFIX + "__score_set.txt";

    /**
     * VFGS result set (i.e. R / R+ in paper).
     * <br>Must contain %d for delta.
     * <br>Format: traj id, score
     */
    public static final String RES_PATH = PATH_PREFIX + "vfgs_%d.csv";

    /**
     * VFGS+ color set (i.e. set of tr in paper)
     * <br>Must contain %d for delta.
     * The order can't change.
     * <br>Format: traj id, tr
     */
    public static final String COLOR_PATH = PATH_PREFIX + "color_%d.csv";

    /**
     * Quality record file.
     */
    public static final String QUALITY_PATH = PATH_PREFIX + "quality.csv";


    /* PSC function */

    /**
     * Always call and print it when program runs.
     */
    public static String str() {
        String ret = "====== PSC Settings ======\n\n";

        ret += "ORIGIN_PATH=" + ORIGIN_PATH + "\n";
        ret += "LIMIT=" + LIMIT + "\n";
        ret += "RATE_LIST=" + Arrays.toString(RATE_LIST) + "\n";
        ret += "DELTA_LIST=" + Arrays.toString(DELTA_LIST) + "\n";
        ret += "ZOOM_RANGE=" + Arrays.toString(ZOOM_RANGE) + "\n";
        ret += "SAVE_TEMP=" + SAVE_TEMP + "\n";
        ret += "SEND_EMAIL=" + SEND_EMAIL + "\n";
        ret += "POS_INFO_PATH=" + POS_INFO_PATH + "\n";
        ret += "SCORE_PATH=" + SCORE_PATH + "\n";
        ret += "RES_PATH=" + RES_PATH + "\n";
        ret += "COLOR_PATH=" + COLOR_PATH + "\n";
        ret += "QUALITY_PATH=" + QUALITY_PATH + "\n";
        ret += "LOG_PATH=" + LOG_PATH + "\n";
        ret += "PROCESS_LIST=" + Arrays.toString(PROCESS_LIST);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        ret += "\nTimestamp: " + formatter.format(date) + "\n";

        return ret;
    }
}
