package draw;

import model.Trajectory;

import java.awt.*;
import java.util.Arrays;

/**
 * The traj color selector
 * <p>
 * All color encoding info will be kept in the {@code colorMatrix}
 * field of {@link Trajectory}.
 * <p>
 * After initialization, this class obj can be release.
 */
public class TrajShader {
	/**
	 * Call this to write colorMatrix to each traj.
	 */
	public static void initTrajColorMatrix(Trajectory[][][] trajVfgsMatrix,
	                                       int[][][] repScoresMatrix,
	                                       Color[] colors, int[] breaks) {
		int dLen = repScoresMatrix.length;
		int rLen = repScoresMatrix[0].length;

		boolean computeBreaks = (breaks == null);
		if (breaks == null) {
			// not have def breaks, compute it
			breaks = new int[5];
		}

		for (int dIdx = 0; dIdx < dLen; dIdx++) {
			for (int rIdx = 0; rIdx < rLen; rIdx++) {
				// compute for one matrix element delta X rate
				Trajectory[] trajVfgs = trajVfgsMatrix[dIdx][rIdx];
				int[] repScores = repScoresMatrix[dIdx][rIdx];

				if (computeBreaks) {
					initBreaks(breaks, repScores.clone());
				}

				for (int i = 0; i < trajVfgs.length; i++) {
					// for one traj in R
					Trajectory traj = trajVfgs[i];
					Color[][] colorMatrix = traj.getColorMatrix();
					if (colorMatrix == null) {
						colorMatrix = new Color[dLen][rLen];
						traj.setColorMatrix(colorMatrix);
					}
					colorMatrix[dIdx][rIdx]
							= getColor(breaks, colors, repScores[i]);
				}
				// save all traj's color at delta X rate
			}
		}
	}

	/**
	 * Here can be optimize by binary search.
	 */
	private static void initBreaks(int[] breaks, int[] repScores) {
		Arrays.sort(repScores); // can be changed because it is clone
		int idx;
		double out = getMeans(repScores) + 3 * getStandardDeviation(repScores);
		for (idx = 0; idx < repScores.length; idx++) {
			if (repScores[idx] > out) {
				break;
			}
		}
		idx -= 1;
		idx /= 5;
		for (int i = 0; i < 5; i++) {
			breaks[i] = repScores[idx * (i + 1)];
		}
	}

	private static double getMeans(int[] arr) {
		double sum = 0.0;
		for (int val : arr) {
			sum += val;
		}
		return sum / arr.length;
	}

	private static double getStandardDeviation(int[] arr) {
		int len = arr.length;
		double avg = getMeans(arr);
		double dVar = 0.0;
		for (double val : arr) {
			dVar += (val - avg) * (val - avg);
		}
		return Math.sqrt(dVar / len);
	}

	public static Color getColor(int[] breaks, Color[] colors, int score) {
		for (int i = 0; i < breaks.length; ++i) {
			if (score <= breaks[i]) {
				return colors[i];
			}
		}
		return colors[colors.length - 1];
	}
}
