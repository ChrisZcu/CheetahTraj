package vqgs.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import vqgs.model.Position;
import vqgs.model.Trajectory;
import processing.core.PApplet;

import java.util.HashSet;

import static vqgs.util.PSC.*;

/**
 * This class is for finding some features of data,
 * like the range of the geo coordinate, max size
 * of the positions, and so on. Use these features,
 * you can optimize the param setting in {@link PSC},
 * then make the VFGS run better.
 * <p>
 * Take maxLocX and minLocY can make position number smaller.
 * <p>
 * <br> Input:
 * <ul><li>Origin data. Read from {@link PSC#ORIGIN_PATH}</li></ul>
 * Output:
 * <ul><li>Anything you want to see</li></ul>
 */
public final class ParamOptimize extends PApplet {
	@Override
	public void setup() {
		UnfoldingMap map = new UnfoldingMap(this);
		System.out.println(PSC.str());
		main(map);
		exit();
	}

	public static void main(UnfoldingMap map) {
		map.zoomAndPanTo(20, CELL_CENTER);
		map.setZoomRange(1, 20);

		try {
			run(map);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void run(UnfoldingMap map) {
		System.out.println("\n ====== Begin Testing ======\n");

		/* load traj from file */

		DM.loadRowData(ORIGIN_PATH, LIMIT);
		Trajectory[] trajFull = DM.trajFull;

		/* cal pos map trajPosList & tot */

		int maxSingleTrajPosNum = -1;
		int maxX = Integer.MIN_VALUE;
		int minX = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;

		double maxLocX = Double.MIN_VALUE;
		double minLocX = Double.MAX_VALUE;
		double maxLocY = Double.MIN_VALUE;
		double minLocY = Double.MAX_VALUE;

		HashSet<Position> totPosSet = new HashSet<>();

		for (Trajectory traj : trajFull) {
			HashSet<Position> trajPos = new HashSet<>();
			for (Location loc : traj.getLocations()) {
				ScreenPosition p = map.getScreenPosition(loc);
				int px = (int) p.x;
				int py = (int) p.y;

				maxX = Math.max(maxX, px);
				minX = Math.min(minX, px);
				maxY = Math.max(maxY, py);
				minY = Math.min(minY, py);

				maxLocX = Math.max(maxLocX, loc.x);
				minLocX = Math.min(minLocX, loc.x);
				maxLocY = Math.max(maxLocY, loc.y);
				minLocY = Math.min(minLocY, loc.y);

				Position pos = new Position(px, py);
				trajPos.add(pos);
				totPosSet.add(pos);
			}
			maxSingleTrajPosNum = Math.max(maxSingleTrajPosNum, trajPos.size());
		}

		System.out.println("maxSingleTrajPosNum = " + maxSingleTrajPosNum);
		System.out.println("maxX = " + maxX);
		System.out.println("minX = " + minX);
		System.out.println("maxY = " + maxY);
		System.out.println("minY = " + minY);

		System.out.println("maxLocX = " + maxLocX);
		System.out.println("minLocX = " + minLocX);
		System.out.println("maxLocY = " + maxLocY);
		System.out.println("minLocY = " + minLocY);

		System.out.println("totPosSet.size() = " + totPosSet.size());
	}

	public static void main(String[] args) {
		PApplet.main("util.ParamOptimize");
	}
}
