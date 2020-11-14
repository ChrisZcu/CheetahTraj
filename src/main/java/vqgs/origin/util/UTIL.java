package origin.util;

import de.fhpotsdam.unfolding.geo.Location;
import origin.model.Trajectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.List;

public class UTIL {

    public static void totalListInit(List<Trajectory> TrajTotal, String dataPath) {
        int trajId = 0;
        try {
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            String line;
            String[] data;
            try {
                while (it.hasNext()) {
                    line = it.nextLine();
                    Trajectory traj = new Trajectory(trajId);
                    trajId++;
                    String[] item = line.split(";");
                    data = item[1].split(",");
                    double score = Double.parseDouble(item[0]);
                    int j = 0;
                    for (; j < data.length - 2; j = j + 2) {
                        Location point = new Location(Double.parseDouble(data[j + 1]), Double.parseDouble(data[j]));
                        traj.points.add(point);
                    }
                    traj.setScore(score);
                    TrajTotal.add(traj);
                }
            } finally {
                LineIterator.closeQuietly(it);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
