package index;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.geo.Location;
import model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuadTree {
    private static double minGLat = Float.MAX_VALUE;
    private static double maxGLat = -Float.MAX_VALUE;
    private static double minGLon = Float.MAX_VALUE;
    private static double maxGLon = -Float.MAX_VALUE;

    public static QuadRegion quadRegionRoot;

    public static TrajectoryMeta[] trajMetaFull;

    public QuadTree() {
    }

    private static double preSion = 10000.0;

    public static TrajectoryMeta[] loadData(double[] latLon, String filePath) {
        TrajectoryMeta[] trajFull;
        ArrayList<String> trajFullStr = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajFullStr.add(line);
            }
            reader.close();
            System.out.println("Read done");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        trajFull = new TrajectoryMeta[trajFullStr.size()];
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        int i = 0;
        for (String line : trajFullStr) {
            String[] metaData = line.split(";");
            double score = Double.parseDouble(metaData[0]);
            String[] item = metaData[1].split(",");
            if (item.length % 2 == 1) {
                continue;
            }
            boolean next = false;
            Position[] positions = new Position[item.length / 2];
            for (int j = 0; j < item.length - 1; j += 2) {
//                int srcX = Integer.parseInt(item[j]);
//                int srcY = Integer.parseInt(item[j + 1]);
                float lat = Float.parseFloat(item[j + 1]);
                float lon = Float.parseFloat(item[j]);
                //debug
//                if (lat > 42.184 || lat < 39.628 || lon < -9.095 || lon > -6.568) {
//                if (lat > 41.346 || lat < 40.948 || lon < -8.806 || lon > -8.229) { // debug
//                if (lat < 38.429 || lon > -6.595) {
//                    j = item.length;
//                    next = true;
//                    continue;
//                }
                //debug done
//                if (i > 10){
//                    j = item.length;
//                    next = true;
//                    continue;
//                }
                try {
                    positions[j / 2] = new Position((int) (lat * preSion), (int) (lon * preSion));
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(line);
                    break;
                }
                minGLat = Math.min(lat, minGLat);
                maxGLat = Math.max(lat, maxGLat);
                minGLon = Math.min(lon, minGLon);
                maxGLon = Math.max(lon, maxGLon);
            }
//
            if (next) {
                continue;
            }
            TrajectoryMeta traj = new TrajectoryMeta(i);
            traj.setScore(score);
            traj.setPositions(positions);

            traj.setBegin(0);
            traj.setEnd(positions.length - 1);
//            trajFull[i] = traj;
            i++;
            res.add(traj);
        }
        trajFull = res.toArray(new TrajectoryMeta[0]);
        System.out.println("Transfer done " + trajFull.length);
        System.out.println(minGLat + ", " + maxGLat + ", " + minGLon + ", " + maxGLon);
        latLon[0] = minGLat;
        latLon[1] = maxGLat;
        latLon[2] = minGLon;
        latLon[3] = maxGLon;
        trajFullStr.clear();

        return trajFull;
    }


    private static QuadRegion createFromTrajList(double minLat, double maxLat, double minLon, double maxLon, int H,
                                                 TrajectoryMeta[] trajFull) {
        RectRegion rectRegion = new RectRegion();
        rectRegion.initLoc(new Location(minLat, minLon), new Location(maxLat, maxLon));
        TimeProfileSharedObject.getInstance().addQuadRectRegion(rectRegion);
        QuadRegion quadRegion = new QuadRegion(minLat, maxLat, minLon, maxLon);
        TrajToQuality[] trajToQualities = VfgsForIndex.getVfgs(trajFull);
        quadRegion.setTrajQuality(trajToQualities);
        if (H > 1) {
            QuadRegion[] quadChildren = new QuadRegion[4];
            double latOff = (maxLat - minLat) / 2;
            double lonOff = (maxLon - minLon) / 2;
            for (int i = 0; i < 4; i++) {
                int laxId = i / 2;
                int lonId = i % 2;
                double tmpLatMin = minLat + latOff * laxId;
                double tmpLonMin = minLon + lonOff * lonId;
                TrajectoryMeta[] wayPoint = getWayPointPos(trajFull, tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff);
                quadChildren[i] = createFromTrajList(tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff, H - 1, wayPoint);
            }
            quadRegion.setQuadRegionChildren(quadChildren);
        }
        return quadRegion;
    }

    /**
     * Generate the quad tree by {@link VfgsForIndexPart}
     */
    private static QuadRegion createPartlyFromTrajList(double minLat, double maxLat, double minLon, double maxLon, int H,
                                                       TrajectoryMeta[] trajMetaList, int delta) {
        RectRegion rectRegion = new RectRegion();
        rectRegion.initLoc(new Location(minLat, minLon), new Location(maxLat, maxLon));
        TimeProfileSharedObject.getInstance().addQuadRectRegion(rectRegion);
        QuadRegion quadRegion = new QuadRegion(minLat, maxLat, minLon, maxLon);

        TrajToSubpart[] trajToSubparts = VfgsForIndexPart.getVfgs(trajMetaList, delta);

        quadRegion.setTrajToSubparts(trajToSubparts);
        if (H > 1) {
            QuadRegion[] quadChildren = new QuadRegion[4];
            double latOff = (maxLat - minLat) / 2;
            double lonOff = (maxLon - minLon) / 2;
            for (int i = 0; i < 4; i++) {
                int laxId = i / 2;
                int lonId = i % 2;
                double tmpLatMin = minLat + latOff * laxId;
                double tmpLonMin = minLon + lonOff * lonId;
                TrajectoryMeta[] wayPoint = getWayPointPos(trajMetaList, tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff);
                quadChildren[i] = createPartlyFromTrajList(tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff, H - 1, wayPoint, delta);
            }
            quadRegion.setQuadRegionChildren(quadChildren);
        }
        return quadRegion;
    }


    public static TrajectoryMeta[] getWayPointPos(TrajectoryMeta[] trajFull, double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (TrajectoryMeta traj : trajFull) {
            for (Position position : generatePosList(traj)) {
                if (inCheck(position, minLat, maxLat, minLon, maxLon)) {
                    res.add(traj);
                    break;
                }
            }
        }
//        return res.toArray(new TrajectoryMeta[0]);
        return cutTrajsPos(res.toArray(new TrajectoryMeta[0]), minLat, maxLat, minLon, maxLon);
    }

    private static boolean inCheck(Position position, double minLat, double maxLat, double minLon, double maxLon) {
        return position.x / preSion >= minLat && position.x / preSion <= maxLat
                && position.y / preSion >= minLon && position.y / preSion <= maxLon;
    }

    /**
     * Run {@link #getRegionInTrajPos} (which cut the traj into subpart) on all trajs.
     */
    private static TrajectoryMeta[] cutTrajsPos(TrajectoryMeta[] trajectories, double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (TrajectoryMeta traj : trajectories) {
            res.addAll(getRegionInTrajPos(traj, minLat, maxLat, minLon, maxLon));
        }
        return res.toArray(new TrajectoryMeta[0]);
    }

    /**
     * Divide traj into trajMeta according to the giving region.
     */
    private static ArrayList<TrajectoryMeta> getRegionInTrajPos(TrajectoryMeta traj,
                                                                double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        int trajId = traj.getTrajId();
        List<Position> partPosList = generatePosList(traj);

        for (int i = 0; i < partPosList.size(); i++) {
            if (inCheck(partPosList.get(i), minLat, maxLat, minLon, maxLon)) {
                TrajectoryMeta trajTmp = new TrajectoryMeta(trajId);
                /* add */
                int begin = i;
//                trajTmp.setBegin(i);
                trajTmp.setBegin(i + traj.getBegin());
                /* add end */
                Position position = partPosList.get(i++);
                while (inCheck(position, minLat, maxLat, minLon, maxLon) && i < partPosList.size()) {
                    position = partPosList.get(i++);
                }
                /* add */
//                if (i - 1 - begin != locTmp.size()) {
//                    System.err.println("Error!");
//                }
                trajTmp.setScore(i - 1 - begin);
                trajTmp.setEnd(i - 1 + traj.getBegin());
                /* add end */
                res.add(trajTmp);

                i = partPosList.size() + 1;
            }
        }
        return res;
    }

    public static QuadRegion getQuadIndex(String filePath, int height) {
        TrajectoryMeta[] trajectories = loadData(new double[4], filePath);
        return createFromTrajList(minGLat, maxGLat, minGLon, maxGLon, height, trajectories);
    }

    public static QuadRegion getQuadIndex(double minLat, double maxLat, double minLon, double maxLon,
                                          TrajectoryMeta[] trajectories, int height) {
        return createFromTrajList(minLat, maxLat, minLon, maxLon, height, trajectories);
    }

    public static QuadRegion getQuadIndexPart(String filePath, int height, int delta) {
        TrajectoryMeta[] trajectories = loadData(new double[4], filePath);
        return createPartlyFromTrajList(minGLat, maxGLat, minGLon, maxGLon, height, trajectories, delta);
    }

    public static QuadRegion getQuadIndexPart(double minLat, double maxLat, double minLon, double maxLon,
                                              TrajectoryMeta[] trajectories, int height, int delta) {
        return createPartlyFromTrajList(minLat, maxLat, minLon, maxLon, height, trajectories, delta);
    }

    private static List<Position> generatePosList(TrajectoryMeta trajMeta) {
        int trajId = trajMeta.getTrajId();
        int begin = trajMeta.getBegin();
        int end = trajMeta.getEnd();      // notice that the end is included

        return Arrays.asList(trajMetaFull[trajId].getPositions()).subList(begin, end + 1);
    }

    public static void saveTreeToFile(String filePath) {
        System.out.print("Write position info to " + filePath + " ...");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            QuadRegion[] parents = new QuadRegion[]{quadRegionRoot};
            saveLevelRecurse(writer, parents);
            writer.close();
            System.out.println("\b\b\bfinished.");

        } catch (IOException e) {
            System.out.println("\b\b\bfailed.");
            e.printStackTrace();
        }
    }

    /**
     * Save all nodes in same level (they are the children of {@code parents}).
     * Suppose that the {@code parents} will never be invalid.
     */
    private static void saveLevelRecurse(BufferedWriter writer, QuadRegion[] parents) throws IOException {
        boolean hasChildren = (parents[0].getQuadRegionChildren() != null);

        if (!hasChildren) {
            for (QuadRegion parent : parents) {
                // save parent
                List<String> strList = QuadRegion.serialize(parent);
                saveOneStrList(writer, strList);
            }
            return;
        }

        // has next level

        int cldIdx = 0;     // index for children
        QuadRegion[] children = new QuadRegion[parents.length * 4];

        for (QuadRegion parent : parents) {
            // save parent
            List<String> strList = QuadRegion.serialize(parent);
            saveOneStrList(writer, strList);

            // generate children
            System.arraycopy(parent.getQuadRegionChildren(), 0, children, cldIdx, 4);
            cldIdx += 4;
        }

        saveLevelRecurse(writer, children);
    }

    /**
     * Save full string data for one {@link QuadRegion}.
     */
    private static void saveOneStrList(BufferedWriter writer, List<String> strList) throws IOException {
        for (String s : strList) {
            writer.write(s);
            writer.newLine();
        }
    }

    /**
     * Load quad tree from file and save root to static field
     */
    public static void loadTreeFromFile(String filePath) {
        LineIterator it = null;

        System.out.print("Read quad tree data from " + filePath + " ...");

        try {
            it = FileUtils.lineIterator(new File(filePath), "UTF-8");
            quadRegionRoot = loadLevelRecurse(it, null);
            System.out.println("\b\b\bfinished.");

        } catch (Exception e) {
            System.out.println("\b\b\bfailed.");
            e.printStackTrace();
        } finally {
            if (it != null) {
                LineIterator.closeQuietly(it);
            }
        }
    }

    /**
     * Create tree by breath first iterate. Each recurse is one level.
     */
    private static QuadRegion loadLevelRecurse(LineIterator lit, QuadRegion[] parents) {
        if (parents == null) {
            // is root
            List<String> strList = loadOneStrList(lit);
            QuadRegion root = QuadRegion.antiSerialize(strList);
            root.setLocation(minGLat, maxGLat, minGLon, maxGLon);

            RectRegion rectRegion = new RectRegion();
            rectRegion.initLoc(new Location(minGLat, minGLon), new Location(maxGLat, maxGLon));
            TimeProfileSharedObject.getInstance().addQuadRectRegion(rectRegion);

            if (lit.hasNext()) {
                // has next level
                QuadRegion[] nxtParents = new QuadRegion[]{root};
                loadLevelRecurse(lit, nxtParents);
            }

            return root;
        }

        // not root, load whole level
        // lit.hasNext() must be true
        int prtCnt = 0;     // idx for nxtParents
        QuadRegion[] nxtParents = new QuadRegion[parents.length * 4];
        for (QuadRegion parent : parents) {
            double latOff = (parent.getMaxLat() - parent.getMinLat()) / 2;
            double lonOff = (parent.getMaxLon() - parent.getMinLon()) / 2;
            QuadRegion[] children = new QuadRegion[4];
            for (int idx = 0; idx < 4; idx++) {

                int laxId = idx / 2;
                int lonId = idx % 2;
                double tmpLatMin = parent.getMinLat() + latOff * laxId;
                double tmpLonMin = parent.getMinLon() + lonOff * lonId;

                List<String> strList = loadOneStrList(lit);
                QuadRegion child = QuadRegion.antiSerialize(strList);

                child.setLocation(tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff);

                RectRegion rectRegion = new RectRegion();
                rectRegion.initLoc(new Location(tmpLatMin, tmpLonMin), new Location(tmpLatMin + latOff, tmpLonMin + lonOff));
                TimeProfileSharedObject.getInstance().addQuadRectRegion(rectRegion);

                children[idx] = child;
                nxtParents[prtCnt++] = child;
            }
            parent.setQuadRegionChildren(children);
        }

        if (lit.hasNext()) {
            // has next level
            loadLevelRecurse(lit, nxtParents);
        }
        return null;        // not root, no need to return anything
    }

    /**
     * Read full string data for one {@link QuadRegion}, but not to anti-serialize it
     */
    private static List<String> loadOneStrList(LineIterator lit) {
        String str = lit.nextLine();
        int lineNum = Integer.parseInt(str);
        List<String> ret = new ArrayList<>(lineNum);
        ret.add(str);       // also add first line to result
        for (int i = 0; i < lineNum; i++) {
            ret.add(lit.nextLine());
        }
        return ret;
    }

    //lat41 lon8
    public static void main(String[] args) {
        String cdPath = "E:\\zcz\\dbgroup\\DTW\\data\\sz_cd\\cd_new_score.txt";
        String szPath = "E:\\zcz\\dbgroup\\DTW\\data\\sz_cd\\sz_score.txt";
        String partPortoPath = "data/GPS/Porto5w/Porto5w.txt";

        String filePath = cdPath;
        String storeFilePath = "data/GPS/porto5w/sz_quad_tree_info.txt";
        int height = 1;
        int delta = 0;
        if (args.length > 0) {
            filePath = args[0];
            storeFilePath = args[1];
            height = Integer.parseInt(args[2]);
            delta = Integer.parseInt(args[3]);
            preSion = Double.parseDouble(args[4]);
        }
        TrajectoryMeta[] trajectories = loadData(new double[4], filePath);

        TimeProfileSharedObject.getInstance().trajMetaFull = trajectories;
        trajMetaFull = trajectories;

        long t0 = System.currentTimeMillis();
        System.out.println("begin index");
        QuadTree.quadRegionRoot = createPartlyFromTrajList(minGLat, maxGLat, minGLon, maxGLon, height, trajectories, delta);
        System.out.println("index time: " + (System.currentTimeMillis() - t0));

//        QuadTree.saveTreeToFile(storeFilePath);

        System.out.println("save done");
    }
}
