package vqgs.draw;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import vqgs.model.Trajectory;
import processing.core.PGraphics;

import java.awt.*;
import java.util.Vector;

/**
 * Handler class for traj painting.
 * <p>
 * This class draw a part of trajectories to flash.
 *
 */
public class TrajDrawHandler extends Thread {
    private final UnfoldingMap map;
    private final PGraphics pg;     // temp image that this thread paint on
    private final PGraphics[] trajImages;   // all traj image parts
    private final Trajectory[] trajList;    // all traj
    private final Vector<Long> newestId;    // the newest id of outer frame
    private final boolean colored;        // traj color shader for VFGS+ CE
    /** Just use local settings for now. See {@link vqgs.util.PSC#BREAKS} */
    private final int[] breaks;
    private final Color[] colors;       /** {@link vqgs.util.PSC#COLORS} */
    private final int[] trajCnt;        // record the # of painted traj
    private final int partIdx;          // the assignment idx in trajImages
    private final int index, threadNum;      // the begin and the step for select traj
    private final long selfId;      // mark to check whether it is newest

    public TrajDrawHandler(UnfoldingMap map, PGraphics pg, PGraphics[] trajImages,
                           Trajectory[] trajList, Vector<Long> newestId,
                           boolean colored, int[] breaks, Color[] colors,
                           int[] trajCnt, int partIdx, int index, int threadNum, long selfId) {
        this.map = map;
        this.pg = pg;
        this.trajImages = trajImages;
        this.trajList = trajList;
        this.newestId = newestId;
        this.colored = colored;
        this.breaks = new int[]{1, 2, 3, 4, 5};
        this.colors = colors;
        this.trajCnt = trajCnt;
        this.partIdx = partIdx;
        this.index = index;
        this.threadNum = threadNum;
        this.selfId = selfId;

        // init priority
        this.setPriority(index % 9 + 1);
    }

    @Override
    public void run() {
        pg.beginDraw();

        float x = map.getScreenPosition(map.getTopLeftBorder()).x;
        float y = map.getScreenPosition(map.getTopLeftBorder()).y;
        pg.translate(-1 * x, -1 * y);

        pg.noFill();
        pg.strokeWeight(1);

        // change the assignment strategy of traj.
        int segLen = trajList.length / threadNum;
        int begin = segLen * index;
        int end = Math.min(begin + segLen, trajList.length);    // exclude

        for (int i = begin; i < end; i++) {
            pg.beginShape();

            // set color
            if (!colored) {
                pg.stroke(255, 0, 0);
            } else {
                Color c = TrajShader.getColor(breaks, colors, trajList[i].getScore());
                pg.stroke(c.getRGB());
            }

            // draw the traj
            for (Location loc : trajList[i].getLocations()) {
                // stop the thread if it is interrupted
                if (Thread.currentThread().isInterrupted()
                        || newestId.get(0) != selfId) {
                    pg.endShape();
                    pg.endDraw();
                    return;
                }

                ScreenPosition pos = map.getScreenPosition(loc);
                pg.vertex(pos.x, pos.y);

            }
            pg.endShape();

            trajCnt[partIdx]++;
        }

        if (newestId.get(0) == selfId) {
            trajImages[partIdx] = pg;
        }
    }
}
