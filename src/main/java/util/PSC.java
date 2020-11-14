package util;

import model.Colour;

import java.awt.*;

/**
 * Parameter Setting Class
 */
public class PSC {
    public static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    // i.e. alpha.
    // These two settings must match the results
    public static double[] RATE_LIST
            = {0.05, 0.01, 0.005, /*0.001, 0.0005, 0.0001, 0.00005, 0.00001*/};
    public static int[] DELTA_LIST
            = {0, 4, 8, 16, /*32, 50, 64, 128*/};

    // origin data src path
    public static String ORIGIN_PATH
            = "data/GPS/Porto5w/Porto5w.txt";

    public static String PATH_PREFIX
            = "data/GPS/porto5w/";

    // traj limit for full set. -1 for no limit
    public static final int LIMIT = 5_0000;

    // recommend: core # * 2 or little higher
    public static final int FULL_THREAD_NUM = 8;        // for both draw and select
    public static final int SAMPLE_THREAD_NUM = 2;      // for both draw and select
    public static final int SELECT_THREAD_NUM = 2;      // only for draw
    /**
     * When the traj num of one thread (tot traj # / thread num)
     * is lower than this, only one thread will run.
     * At least > max thread num * 10
     * otherwise some traj may be disappeared in select / visualization.
     */
    public static final int MULTI_THREAD_BOUND = 5000;

    /**
     * The pool size of control pool in {@link draw.TrajDrawManager}.
     */
    public static final int CONTROL_POOL_SIZE = 8;

    /**
     * VFGS result set (i.e. R / R+ in paper).
     * <br>Must contain %d for delta.
     * <br>Format: traj id, score
     */
    public static final String RES_PATH = PATH_PREFIX + "vfgs_%d.csv";

    public static final String OUTPUT_PATH = "data/output/";

    private static final int colorOff = 10;
    // six provided color to algorithm
    public static final Color[] COLOR_LIST = {
            new Color(15, 91, 120),
            new Color(19, 149, 186),
            new Color(162, 184, 108),
            new Color(235, 200, 68),
            new Color(241, 108, 32),
            new Color(190, 46, 29),
            new Color(79, 79, 79),
            new Color(0, 0, 0),
            new Color(255, 255, 255)
    };

    public static Color[][] COLOT_TOTAL_LIST;
    public static final Color RED = new Color(255, 0, 0);
    public static final Color BLUE = new Color(2, 124, 255);
    public static final Color GRAY = new Color(150, 150, 150);

    private int groupNum = 5;

    public static void initRegionColorList() {
        COLOT_TOTAL_LIST = new Color[6][4];
        Color[] pinkColor = new Color[4];
        for (int i = 0; i < 120; i += 30) {//pink list
            pinkColor[i / 30] = new Color(115, 120 - i, 115);
        }
        COLOT_TOTAL_LIST[1] = pinkColor;

        Color[] greenColor = new Color[4];
        for (int i = 0; i < 120; i += 30) {
            greenColor[i / 30] = new Color(25, 200 - i, 91);
        }
        COLOT_TOTAL_LIST[0] = greenColor;

        Color[] blueColor = new Color[4];
        for (int i = 80; i < 240; i += 40) {
            blueColor[(i - 80) / 40] = new Color(65, 65, 240 - i);
        }
        COLOT_TOTAL_LIST[2] = blueColor;

        COLOT_TOTAL_LIST[3] = new Color[]{new Color(235, 200, 68), new Color(235, 200, 68),
                new Color(235, 200, 68), new Color(235, 200, 68)};

        COLOT_TOTAL_LIST[4] = new Color[]{new Color(162, 184, 108), new Color(162, 184, 108),
                new Color(162, 184, 108), new Color(162, 184, 108),};

        COLOT_TOTAL_LIST[5] = new Color[]{new Color(241, 108, 32), new Color(241, 108, 32),
                new Color(241, 108, 32), new Color(241, 108, 32),};
    }
}