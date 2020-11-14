package util;

import de.fhpotsdam.unfolding.geo.Location;
import model.Position;
import model.Trajectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Data Manager Class for transferring common data
 * to different part of vfgs calculations.
 */
public final class DM {
	/** All original loaded trajs */
	public static Trajectory[] trajFull;
	/** Set of all positions (in terns of hash) */
	public static HashSet<Position> totPosSet;
	/** R / R+ according to delta & alpha. 1st dim: delta */
	public static int[][] vfgsResList;
	/** The representativeness scores of the trajs.
	 * 1st dim: delta, 2nd dim: rate. */
	public static int[][][] repScoresMatrix;

	/**
	 * Release all locations fields in traj object
	 */
	public static void releaseLocations() {
		for (Trajectory traj : trajFull) {
			traj.setLocations(null);
		}
	}

	/**
	 * Release all positions field in traj object
	 */
	public static void releasePositions() {
		for (Trajectory traj : trajFull) {
			traj.setPositions(null);
		}
	}

	/**
	 * Save log info to the {@link PSC#LOG_PATH}.
	 * No newline adds.
	 */
	public static void printAndLog(String filePath, String msg) {
		try (BufferedWriter writer
				     = new BufferedWriter(new FileWriter(filePath, true))) {
			System.out.print(msg);
			writer.write(msg);
		} catch (IOException | IllegalArgumentException e) {
			System.err.println("Save log failed.");
			e.printStackTrace();
		}
	}

	/**
	 * Load traj raw data or with score (version problem)
	 * {@link #trajFull} from file {@link PSC#ORIGIN_PATH}.
	 * <p>
	 * format: (score;)double1,double2...
	 */
	public static boolean loadRowData(String filePath, int limit) {
		List<Trajectory> res = new ArrayList<>();
		LineIterator it = null;
		boolean ret = true;
		int cnt = 0;

		System.out.print("Read raw data from " + filePath + " ...");

		try {
			it = FileUtils.lineIterator(new File(filePath), "UTF-8");

			while (it.hasNext() && (limit == -1 || cnt < limit)) {
				String line = it.nextLine();
				String[] item = line.split(";");
				String[] data = item[item.length - 1].split(",");

				Trajectory traj = new Trajectory(cnt);
				if (item.length == 2) {
					// when data contain "score;"
					traj.setOriginScore(Integer.parseInt(item[0]));
				}
				ArrayList<Location> locations = new ArrayList<>();
				for (int i = 0; i < data.length - 2; i = i + 2) {      // FIXME
					// the longitude and latitude are reversed
					locations.add(new Location(Double.parseDouble(data[i + 1]),
							Double.parseDouble(data[i])));
				}
				traj.setLocations(locations.toArray(new Location[0]));
				res.add(traj);
				++ cnt;
			}
			System.out.println("\b\b\bfinished.");

		} catch (IOException | NoSuchElementException e) {
			System.out.println("\b\b\bfailed. \nProblem line: " + cnt);
			e.printStackTrace();
			ret = false;
		} finally {
			if (it != null) {
				LineIterator.closeQuietly(it);
			}
		}

		trajFull = res.toArray(new Trajectory[0]);
		return ret;
	}

	/**
	 * Write positions field of traj & {@link #totPosSet}
	 * to file {@link PSC#POS_INFO_PATH}.
	 * <p>
	 * Because {@link #totPosSet} can be produced by previous one,
	 * so only save all positions.
	 * The data will be kept in the memory.
	 */
	public static boolean savePosInfo(String filePath) {
		boolean ret = true;
		System.out.print("Write position info to " + filePath + " ...");

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

			/*writer.write(String.valueOf(trajFull.length));   // write the traj #
			writer.newLine();*/

			for (Trajectory traj : trajFull) {
				Position[] ps = traj.getPositions();
				int cnt = ps.length;
				for (Position p : ps) {
					writer.write(p.getX() + "," + p.getY() + ((cnt == 1) ? "" : ","));
					cnt --;
				}
				writer.newLine();
			}

			writer.close();
			System.out.println("\b\b\bfinished.");

		} catch (IOException e) {
			System.out.println("\b\b\bfailed.");
			e.printStackTrace();
			ret = false;
		}
		return ret;
	}

	/**
	 * Temp output for old-version quality function {@link origin.util.qualityCal}
	 */
	public static boolean saveScoreFile(String srcPath, String toPath, int limit) {
		LineIterator it = null;
		boolean ret = true;
		System.out.print("Write temp score file data to " + toPath);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(toPath));
			it = FileUtils.lineIterator(new File(srcPath), "UTF-8");
			int cnt = 0;
			while (it.hasNext() && (limit == -1 || cnt < limit)) {
				String line = it.nextLine();
				String[] data = line.split(";");
				writer.write(trajFull[cnt].getOriginScore() + ";" + data[data.length - 1]);
				writer.newLine();
				cnt++;
			}
			LineIterator.closeQuietly(it);
			writer.close();
			System.out.println(" finished.");

		} catch (IOException | NoSuchElementException e) {
			System.out.println(" failed.");
			e.printStackTrace();
			ret = false;
		} finally {
			if (it != null) {
				LineIterator.closeQuietly(it);
			}
		}

		return ret;
	}

	/**
	 * Read positions field of traj & {@link #totPosSet}
	 * from file {@link PSC#POS_INFO_PATH}.
	 * <p>
	 * The origin score (i.e. positions.length) and {@link #totPosSet}
	 * will be also computed.
	 * <p>
	 * If {@link #trajFull} is null, create it.
	 * Otherwise save positions in to traj's field,
	 * and you should guarantee that the data is matched.
	 */
	public static boolean loadPosInfo(String filePath) {
		LineIterator it = null;
		int tid = 0;
		boolean ret = true;

		System.out.print("Load position info from " + filePath + " ...");

		try{
			it = FileUtils.lineIterator(new File(filePath), "UTF-8");

			/*int trajNum = Integer.parseInt(it.nextLine());*/

			totPosSet = new HashSet<>();
			ArrayList<Position[]> positionsList = new ArrayList<>();

			while (it.hasNext()){
				String line = it.nextLine();
				String[] data = line.split(",");

				Position[] positions = new Position[data.length / 2];
				for (int i = 0, j = 0; i < positions.length; i += 1, j += 2) {
					Position pos = new Position(Integer.parseInt(data[j]),
							Integer.parseInt(data[j + 1]));
					positions[i] = pos;
					totPosSet.add(pos);
				}
				positionsList.add(positions);

				++ tid;
			}

			// tie positions to the traj obj
			if (trajFull == null) {
				trajFull = new Trajectory[positionsList.size()];
				for (int i = 0; i < trajFull.length; i++) {
					trajFull[i] = new Trajectory(i);
				}
			}
			for (int i = 0; i < trajFull.length; i++) {
				Position[] positions = positionsList.get(i);
				trajFull[i].setPositions(positions);
				trajFull[i].setOriginScore(positions.length);
			}

			System.out.print("\b\b\bfinished. \nLoaded size :" + tid);

		} catch (IOException | NoSuchElementException e) {
			System.out.println("\b\b\bfailed. \nProblem line: " + tid);
			e.printStackTrace();
			ret = false;
		} finally {
			if (it != null) {
				LineIterator.closeQuietly(it);
			}
		}

		return ret;
	}

	/**
	 * Save vfgs result {@link #vfgsResList} to {@link PSC#RES_PATH}
	 * <br> format: tid, score (not need for now)
	 */
	public static boolean saveResList(String filePath, int[] deltaList) {
		boolean ret = true;
		System.out.print("Write vfgs result to " + filePath.replace("%d", "*"));
		try {
			for (int dIdx = 0; dIdx < deltaList.length; dIdx++) {
				String path = String.format(filePath, deltaList[dIdx]);
				BufferedWriter writer = new BufferedWriter(new FileWriter(path));

				for (int tid : vfgsResList[dIdx]) {
					writer.write(tid + "," + 0);
					writer.newLine();
				}

				writer.close();
			}
			System.out.println(" finished.");

		} catch (IOException e) {
			System.out.println(" failed.");
			e.printStackTrace();
			ret = false;
		}
		return ret;
	}

	/**
	 * Read vfgs result {@link #vfgsResList} from {@link PSC#RES_PATH}
	 * <br> format: tid, score (not need for now)
	 * <p>
	 * Before calling it, the {@link #trajFull}
	 * should have been loaded.
	 */
	public static boolean loadResList(String filePath, int[] deltaList,
	                                  double[] rateList) {
		int dLen = deltaList.length;
		vfgsResList = new int[dLen][];
		int trajNum = trajFull.length;
		int[] rateCntList = translateRate(trajNum, rateList);
		LineIterator it = null;
		int lineNum = 0;
		boolean ret = true;

		System.out.print("Read vfgs result from " + filePath.replace("%d", "*"));

		try {
			for (int dIdx = 0; dIdx < dLen; ++dIdx) {
				int delta = deltaList[dIdx];
				String path = String.format(filePath, delta);
				it = FileUtils.lineIterator(new File(path), "UTF-8");

				// load the R / R+ for this delta
				int[] vfgsRes = new int[rateCntList[0]];
				for (int i = 0; i < vfgsRes.length; i++) {
					String line = it.nextLine();
					String[] data = line.split(",");
					vfgsRes[i] = Integer.parseInt(data[0]);

					++lineNum;
				}
				vfgsResList[dIdx] = vfgsRes;
			}
			System.out.println(" finished.");

		} catch (IOException | NoSuchElementException e) {
			System.out.println(" failed. \nProblem line: " + lineNum);
			e.printStackTrace();
			ret = false;
		} finally {
			if (it != null) {
				LineIterator.closeQuietly(it);
			}
		}

		return ret;
	}

	/**
	 * Save represent traj result {@link #repScoresMatrix} to {@link PSC#COLOR_PATH}
	 * <br> format: tid, repScore
	 * <br> <s>a blank line when a matrix element ends.</s> No blank line now.
	 */
	public static boolean saveRepScoresMatrix(String filePath, int[] deltaList) {
		boolean ret = true;
		int dLen = repScoresMatrix.length;
		int rLen = repScoresMatrix[0].length;

		System.out.print("Write rep score to " + filePath.replace("%d", "*"));

		try {
			for (int dIdx = 0; dIdx < dLen; dIdx++) {
				String path = String.format(filePath, deltaList[dIdx]);
				BufferedWriter writer = new BufferedWriter(new FileWriter(path));

				int[] vfgsRes = vfgsResList[dIdx];

				for (int rIdx = 0; rIdx < rLen; rIdx++) {
					// write score data for this delta-rate cell
					int[] repScores = repScoresMatrix[dIdx][rIdx];
					for (int i = 0; i < repScores.length; i++) {
						writer.write(vfgsRes[i] + "," + repScores[i]);
						writer.newLine();
					}
					/*writer.newLine();*/
				}

				writer.close();
			}
			System.out.println(" finished.");

		} catch (IOException e) {
			System.out.println(" failed.");
			e.printStackTrace();
			ret = false;
		}
		return ret;
	}

	/**
	 * Read rep data {@link #repScoresMatrix}from {@link PSC#COLOR_PATH}.
	 * <br> The score will also be written into traj obj
	 * <br> format: tid, repScore
	 * <p>
	 * Before call it, the {@link #trajFull} must have been loaded.
	 */
	public static boolean loadRepScoresMatrix(String filePath, int[] deltaList,
	                                          double[] rateList) {
		LineIterator it = null;
		int lineNum = 0;
		boolean ret = true;

		int dLen = deltaList.length;
		int rLen = rateList.length;
		int[] rateCntList = DM.translateRate(trajFull.length, rateList);
		repScoresMatrix = new int[dLen][rLen][];

		System.out.print("Read rep score from " + filePath.replace("%d", "*"));

		try {
			for (int dIdx = 0; dIdx < dLen; dIdx++) {
				String path = String.format(filePath, deltaList[dIdx]);
				it = FileUtils.lineIterator(new File(path), "UTF-8");

				for (int rIdx = 0; rIdx < rLen; rIdx++) {
					// read score data for this delta-rate cell
					String line;
					ArrayList<Integer> repScores = new ArrayList<>();

					for (int i = 0; i < rateCntList[rIdx]; i++) {
						line = it.nextLine();
						String[] data = line.split(",");
						int rep = Integer.parseInt(data[1]);
						// the order is same in vfgs res
						/*trajFull[Integer.parseInt(data[0])].setScore(rep);*/
						repScores.add(rep);
					}

					repScoresMatrix[dIdx][rIdx] = repScores.stream()
							.mapToInt(Integer::valueOf).toArray();
				}

				LineIterator.closeQuietly(it);
			}
			System.out.println(" finished.");

		} catch (IOException | IllegalArgumentException e) {
			System.out.println(" failed. \nProblem line: " + lineNum);
			e.printStackTrace();
			ret = false;
		} finally {
			if (it != null) {
				LineIterator.closeQuietly(it);
			}
		}
		return ret;
	}

	/**
	 * Save quality result to file {@link PSC#QUALITY_PATH}
	 * <br> format: zoom, delta, rate, score, tot_score, quality
	 */
	public static boolean saveQualityResult(String filePath, int[] zoomRange, int[] deltaList,
	                                        double[] rateList, int[][][] qualityCube, int[] totScoreList) {
		boolean ret = true;
		int dLen = deltaList.length;
		int rLen = rateList.length;

		System.out.print("Write quality record to " + filePath);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write("zoom,delta,rate,score,tot_score,quality");
			writer.newLine();

			for (int zoom = zoomRange[0], zIdx = 0; zoom <= zoomRange[1]; zoom++, zIdx++) {
				int totScore = totScoreList[zIdx];

				for (int dIdx = 0; dIdx < dLen; dIdx++) {
					int delta = deltaList[dIdx];

					for (int rIdx = 0; rIdx < rLen; rIdx++) {
						double rate = rateList[rIdx];
						int score = qualityCube[zIdx][dIdx][rIdx];
						writer.write(String.format("%d,%d,%s,%d,%d,%.10f",
								zoom, delta, rate, score, totScore, 1.0 * score / totScore));
						writer.newLine();
					}
				}
			}
			System.out.println(" finished.");

		} catch (IOException | IllegalArgumentException e) {
			System.out.println(" failed.");
			e.printStackTrace();
			ret = false;
		}

		return ret;
	}

	/** @deprecated */
	public static Trajectory binarySearch(Trajectory[] list, int tid)
			throws IllegalArgumentException {
		int lo = 0, hi = list.length - 1;
		while (lo <= hi) {
			int mi = lo + (hi - lo) / 2;
			int cmp = list[mi].getTid() - tid;
			if (cmp < 0) {
				lo = mi + 1;
			} else if (cmp > 0) {
				hi = mi - 1;
			} else {
				return list[mi];
			}
		}
		throw new IllegalArgumentException("Data of vfgs_*.txt and color_*.txt " +
				"are not matched. Please check.\n Problem tid: " + tid);
	}

	/**
	 * Translate simple rate to real traj count.
	 */
	public static int[] translateRate(int trajNum, double[] rateList) {
		int len = rateList.length;
		int[] ret = new int[len];
		for (int i = 0; i < len; i++) {
			ret[i] = (int) Math.round(trajNum * rateList[i]);
		}
		return ret;
	}
}
