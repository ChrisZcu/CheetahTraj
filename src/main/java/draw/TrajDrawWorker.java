package draw;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PGraphics;

import java.awt.*;

/**
 * Draw the trajectory to the buffer images. Started and Managed by {@link TrajDrawManager}.
 */
public class TrajDrawWorker extends Thread {
    private final TrajDrawManager manager;
    private final UnfoldingMap map;
    // temp image that this thread paint on
    // the pg has already be translated.
    private final PGraphics pg;
    private final Trajectory[] trajList;    // all traj
    private final int layer;
    private final int[] trajCnt;    // record the # of painted traj
    private final int mapIdx, index;        // param to locate the pg this worker dealing with
    private final float offsetX, offsetY;   // offset of the map
    private final int begin, end;       // the param for select traj
    private final Color color;      // the color for the traj
    // id for determine whether the result is newest
    private final int id;

    public volatile boolean stop = false;

    public TrajDrawWorker(TrajDrawManager manager, String name,
                          UnfoldingMap map, PGraphics pg,
                          Trajectory[] trajList, int layer, int[] trajCnt,
                          int mapIdx, int index, float offsetX, float offsetY,
                          int begin, int end, Color color, int id) {
        super(name);
        this.manager = manager;
        this.map = map;
        this.pg = pg;
        this.trajList = trajList;
        this.layer = layer;
        this.trajCnt = trajCnt;
        this.mapIdx = mapIdx;
        this.index = index;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.begin = begin;
        this.end = end;
        this.color = color;
        this.id = id;

        // init priority
        this.setPriority(9);
    }

    @Override
    public void run() {
        pg.beginDraw();
        pg.translate(-1 * offsetX, -1 * offsetY);
        pg.noFill();
        pg.strokeWeight(1);
        pg.stroke(color.getRGB());

//        System.out.println(this.getName() + " start");
//        System.out.println(this.getName() + ", begin=" + begin + " end=" + end);

        for (int i = begin; i < end; i++) {
            pg.beginShape();

            // draw the traj
            for (Location loc : trajList[i].getLocations()) {

                // stop the thread if it is interrupted
                if (this.stop) {
                    System.out.println(this.getName() + " cancel");
                    pg.endShape();
                    pg.endDraw();
                    return;
                }

                ScreenPosition pos = map.getScreenPosition(loc);
                pg.vertex(pos.x, pos.y);
            }
            pg.endShape();

//            trajCnt[index] ++;
        }

        System.out.println(this.getName() + " finished");
        manager.setTrajImageResult(mapIdx, index, layer, pg, id);
    }
}
