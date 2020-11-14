package draw;

import app.ThreadMapApp;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PGraphics;

import java.awt.*;
import java.util.Vector;

/**
 * Handler class for traj painting.
 * <p>
 * This class draw a part of trajectories to flash.
 * Now the thread interrupt mechanism works <s>but has bug</s>.
 * This bug has been fixed. Now drag or zoom fast won't lead
 * to the slow interaction.
 * <p>
 * It will be called when the map has be changed (zoom or drag).
 * <p>
 * Notice that the index points out which traj image
 * part will be calculate by this thread.
 *
 * @see ThreadMapApp#updateTrajImages()
 */
public class TrajDrawHandler2 extends Thread {
	private final UnfoldingMap map;
	private final PGraphics pg;     // temp image that this thread paint on
	private final PGraphics[] trajImages;   // all traj image parts
	private final Trajectory[] trajList;    // all traj
	private final Vector<Long> newestId;    // the newest id of outer frame
	private final boolean colored;      // traj color shader for VFGS+ CE
	private final int dIdx, rIdx;       // the param for select color
	private final int[] trajCnt;        // record the # of painted traj
	private final int index, threadNum;     // the param for select traj
	private final long selfId;      // mark to check whether it is newest

	public TrajDrawHandler2(UnfoldingMap map, PGraphics pg, PGraphics[] trajImages,
	                        Trajectory[] trajList, Vector<Long> newestId,
	                        boolean colored, int dIdx, int rIdx, int[] trajCnt,
	                        int index, int threadNum, long selfId) {
		this.map = map;
		this.pg = pg;
		this.trajImages = trajImages;
		this.trajList = trajList;
		this.newestId = newestId;
		this.colored = colored;
		this.dIdx = dIdx;
		this.rIdx = rIdx;
		this.trajCnt = trajCnt;
		this.index = index;
		this.threadNum = threadNum;
		this.selfId = selfId;

		// init priority
		this.setPriority(index % 9 + 1);
	}

	@Override
	public void run() {
		pg.beginDraw();
		pg.noFill();
		pg.strokeWeight(1);

		int segLen = trajList.length / threadNum;
		int begin = segLen * index;
		int end = Math.min(begin + segLen, trajList.length);    // exclude

		for (int i = begin; i < end; i++) {
			pg.beginShape();

			// set color
			if (!colored) {
				pg.stroke(255, 0, 0);
			} else {
				Color c = trajList[i].getColorMatrix()[dIdx][rIdx];
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

			trajCnt[index] ++;
		}

		if (newestId.get(0) == selfId) {
			trajImages[index] = pg;
		}
	}
}
