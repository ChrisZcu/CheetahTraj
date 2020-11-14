package util;

import de.fhpotsdam.unfolding.geo.Location;
import model.Trajectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static util.Util.translateRate;

/**
 * IO util class
 */
public class IOHandle {
    /**
     * Load traj raw data or with score (version problem)
     * {@code #trajFull} from file {@link PSC#ORIGIN_PATH}.
     * <p>
     * format: (score;)double1,double2...
     */
    public static Trajectory[] loadRowData(String filePath, int limit) {
        List<Trajectory> res = new ArrayList<>();
        LineIterator it = null;
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
                    traj.setScore(Integer.parseInt(item[0]));
                }
                ArrayList<Location> locations = new ArrayList<>();
                for (int i = 0; i < data.length - 2; i = i + 2) {      // FIXME
                    // the longitude and latitude are reversed
                    locations.add(new Location(Float.parseFloat(data[i + 1]),
                            Float.parseFloat(data[i])));
                }
                traj.setLocations(locations.toArray(new Location[0]));
                res.add(traj);
                ++ cnt;
            }
            System.out.println("\b\b\bfinished.");

        } catch (IOException | NoSuchElementException e) {
            System.out.println("\b\b\bfailed. \nProblem line: " + cnt);
            e.printStackTrace();
        } finally {
            if (it != null) {
                LineIterator.closeQuietly(it);
            }
        }

        return res.toArray(new Trajectory[0]);
    }

    /**
     * Read vfgs result {@code #vfgsResList} from {@link PSC#RES_PATH}
     * <br> format: tid, score (not need for now)
     * <p>
     * Before calling it, the {@link #loadRowData(String, int)}
     * should be called first.
     */
    public static Trajectory[][] loadVfgsResList(String filePath, Trajectory[] trajFull,
                                              int[] deltaList, double[] rateList) {
        int dLen = deltaList.length;
        Trajectory[][] ret = new Trajectory[dLen][];
        int trajNum = trajFull.length;
        int[] rateCntList = translateRate(trajNum, rateList);
        LineIterator it = null;
        int lineNum = 0;

        System.out.print("Read vfgs result from " + filePath.replace("%d", "*"));

        try {
            for (int dIdx = 0; dIdx < dLen; ++dIdx) {
                int delta = deltaList[dIdx];
                String path = String.format(filePath, delta);
                it = FileUtils.lineIterator(new File(path), "UTF-8");

                // load the R / R+ for this delta
                Trajectory[] vfgsRes = new Trajectory[rateCntList[0]];
                for (int i = 0; i < vfgsRes.length; i++) {
                    String line = it.nextLine();
                    String[] data = line.split(",");
                    vfgsRes[i] = trajFull[Integer.parseInt(data[0])];

                    ++lineNum;
                }
                ret[dIdx] = vfgsRes;
            }
            System.out.println(" finished.");

        } catch (IOException | NoSuchElementException e) {
            System.out.println(" failed. \nProblem line: " + lineNum);
            e.printStackTrace();
        } finally {
            if (it != null) {
                LineIterator.closeQuietly(it);
            }
        }

        return ret;
    }
}
