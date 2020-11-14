package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.RectRegion;
import model.Trajectory;
import org.lwjgl.Sys;
import processing.core.PApplet;
import processing.opengl.PJOGL;
import select.TimeProfileManager;
import util.PSC;

import java.awt.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;


public class FullTimeProfiler extends PApplet {
    UnfoldingMap map;
    UnfoldingMap mapClone;

    @Override
    public void settings() {
        size(1000, 800, P2D);
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);
        map.setBackgroundColor(255);
        MapUtils.createDefaultEventDispatcher(this, map);

        mapClone = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapClone.setZoomRange(0, 20);
        mapClone.zoomAndPanTo(20, PRESENT);

        loadData("data/GPS/porto_full.txt");
//        loadData("data/GPS/Porto5w/Porto5w.txt");

    }

    private int ZOOMLEVEL = 11;
    private int rectRegionID = 0;
    private double recNumId = 1.0 / 64;
    private Location[] rectRegionLoc = {new Location(41.315205, -8.629877), new Location(41.275997, -8.365519),
            new Location(41.198544, -8.677942), new Location(41.213013, -8.54542),
            new Location(41.1882, -8.35178), new Location(41.137554, -8.596918),
            new Location(41.044403, -8.470575), new Location(40.971338, -8.591425)};
    private Location PRESENT = rectRegionLoc[rectRegionID]/*new Location(41.206, -8.627)*/;

    private RectRegion rectRegion = new RectRegion();

    @Override
    public void draw() {
        if (!map.allTilesLoaded()) {
            map.draw();
        } else {
            map.zoomAndPanTo(ZOOMLEVEL, rectRegionLoc[rectRegionID]);
            map.draw();

            float x = (float) (500 * recNumId);
            float y = (float) (400 * recNumId);
            rectRegion.setLeftTopLoc(map.getLocation(500 - x, 400 - y));
            rectRegion.setRightBtmLoc(map.getLocation(500 + x, 400 + y));


            long wayPointBegin = System.currentTimeMillis();
            startCalWayPoint(); //waypoint
            long wayPointCost = (System.currentTimeMillis() - wayPointBegin);

            trajShow.clear();
            ArrayList<Trajectory> trajShows = new ArrayList<>();
            for (Trajectory[] trajList : TimeProfileSharedObject.getInstance().trajRes) {
                Collections.addAll(trajShows, trajList);
            }

            long t0 = System.currentTimeMillis();
            Trajectory[] tmpRes = util.VFGS.getCellCover(trajShows.toArray(new Trajectory[0]), mapClone, 0.01, 32);
            long VfgsTime = (System.currentTimeMillis() - t0);

            this.trajShow.addAll(Arrays.asList(tmpRes));

            noFill();
            strokeWeight(1);
            stroke(new Color(190, 46, 29).getRGB());
            long[] timeRender = drawCPU();
            drawRect();

            //info
            StringBuilder info = new StringBuilder();
            info.append(ZOOMLEVEL).append(",").append(rectRegionID).append(",").append(recNumId).append(",").append(trajShows.size())
                    .append(",").append(VfgsTime).append(",").append(wayPointCost).append(",")
                    .append(timeRender[0]).append(",").append(timeRender[1]);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter("data/localRec/FullVfgs+32ProfilerRecord.txt", true));
                writer.write(info.toString());
                writer.newLine();
                writer.close();
                String[] infoList = info.toString().split(",");
                String[] titleList = "zoomlevel,regionId,regionSize,waypointNum,Vfgs+32Cost,waypointCost,mappingCost,renderingCost".split(",");
                for (int i = 0; i < titleList.length; i++) {
                    System.out.print(titleList[i] + " = " + infoList[i] + ", ");
//                title = title + infoList[i] + "_";
                }
                System.out.println();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (recNumId == 0.5) {
                recNumId = 1.0 / 64;
                if (rectRegionID == rectRegionLoc.length - 1) {
                    rectRegionID = 0;
                    if (ZOOMLEVEL == 11) {
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>done>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                        noLoop();
                        exit();
                    } else {
                        ZOOMLEVEL++;
                    }
                } else {
                    rectRegionID++;
                }
            } else {
                recNumId *= 2;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            saveFrame("data/picture/FullTimeProfiler/" + title + ".png");
        }
    }

    private void drawGPU() {
        shaderInit();
        FloatBuffer vertexDataBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        vboHandles = new int[1];
        gl3.glGenBuffers(1, vboHandles, 0);

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboHandles[0]);
        gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexDataBuffer.capacity() * 4, vertexDataBuffer, GL3.GL_STATIC_DRAW);
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        vertexDataBuffer = null;

        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));

        gl3.glUseProgram(shaderProgram);

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboHandles[0]);
        gl3.glEnableVertexAttribArray(0);
        gl3.glEnableVertexAttribArray(1);
        gl3.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 0, 0);
        gl3.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 0, 0);

        gl3.glDrawArrays(GL3.GL_LINES, 0, vertexData.length / 2);
        gl3.glDisableVertexAttribArray(0);
        gl3.glDisableVertexAttribArray(1);
    }

    private void drawRect() {
        noFill();
        strokeWeight(2);
        stroke(new Color(19, 149, 186).getRGB());

        ScreenPosition src1 = map.getScreenPosition(rectRegion.getLeftTopLoc());
        ScreenPosition src2 = map.getScreenPosition(rectRegion.getRightBtmLoc());
        rect(src1.x, src1.y, src2.x - src1.x, src2.y - src1.y);
    }

    boolean noLoop = false;
    boolean thread = false;

    @Override
    public void keyPressed() {
        if (key == 'q') {
            trajShow.clear();
            noLoop = false;
            thread = false;
            loop();
        } else if (key == 'w') {
            Collections.addAll(trajShow, trajFull);
            noLoop = true;
        } else if (key == 'e') {
            float x = (float) (500 * recNumId);
            float y = (float) (400 * recNumId);
            rectRegion.setLeftTopLoc(map.getLocation(500 - x, 400 - y));
            rectRegion.setRightBtmLoc(map.getLocation(500 + x, 400 + y));
            System.out.println(rectRegion.getLeftTopLoc());
            System.out.println(rectRegion.getRightBtmLoc());

            System.out.println("using: " + recNumId);
            recNumId *= 2;

            startCalWayPoint();

            trajShow.clear();
            noLoop = true;
            thread = true;
            loop();
        }
        System.out.println(key);
    }

    Trajectory[] trajFull;
    ArrayList<Trajectory> trajShow = new ArrayList<>();

    private void loadData(String filePath) {
        try {
            ArrayList<String> trajStr = new ArrayList<>(2400000);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajStr.add(line);
            }
            reader.close();
            System.out.println("load done");
            int j = 0;

            trajFull = new Trajectory[trajStr.size()];

            for (String trajM : trajStr) {
                String[] item = trajM.split(";");
                String[] data = item[1].split(",");
                Trajectory traj = new Trajectory(j);
//                ArrayList<Location> locations = new ArrayList<>();
                Position[] positions = new Position[data.length / 2 - 1];
//                Position[] metaGPS = new Position[data.length / 2 - 1];
                for (int i = 0; i < data.length - 2; i = i + 2) {
//                    Location loc = new Location(Float.parseFloat(data[i + 1]),
//                            Float.parseFloat(data[i]));
//                    locations.add(loc);
                    positions[i / 2] = new Position(Float.parseFloat(data[i + 1]),
                            Float.parseFloat(data[i]));
//                    metaGPS[i / 2] = new Position(Float.parseFloat(data[i + 1]), Float.parseFloat(data[i]));
                }
//                traj.setLocations(locations.toArray(new Location[0]));
                traj.setPositions(positions);
                traj.setScore(Integer.parseInt(item[0]));
//                traj.setMetaGPS(metaGPS);
                trajFull[j++] = traj;
            }
            trajStr.clear();
            System.out.println("load done");
            System.out.println("traj number: " + trajFull.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Trajectory[] selectTrajList;

    private void startCalWayPoint() {
        TimeProfileManager tm = new TimeProfileManager(1, trajFull, rectRegion);
        tm.startRun();
    }

    int[][] VFGSIdList;
    int VFGS = 0;


    private int[][] loadVfgs(String filePath) {
        int[][] resId = new int[8][];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            ArrayList<Integer> id = new ArrayList<>();
            int i = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(",")) {
                    resId[i] = new int[id.size()];
                    System.out.println(id.size());
                    int j = 0;
                    for (Integer e : id) {
                        resId[i][j++] = e;
                    }
                    i++;
                    id.clear();
                } else {
                    id.add(Integer.parseInt(line.split(",")[0]));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resId;
    }

    private void loadTrajList() {
        selectTrajList = new Trajectory[VFGSIdList[VFGS].length];
        int i = 0;
        for (Integer e : VFGSIdList[VFGS]) {
            selectTrajList[i++] = trajFull[e];
        }
    }


    GL3 gl3;
    int[] vboHandles;
    int shaderProgram, vertShader, fragShader;
    int vertexBufferObject;

    IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
    float[] vertexData = {};

    private void shaderInit() {
        // initializeProgram

        shaderProgram = gl3.glCreateProgram();

        fragShader = gl3.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl3.glShaderSource(fragShader, 1,
                new String[]{
                        "#ifdef GL_ES\n" +
                                "precision mediump float;\n" +
                                "precision mediump int;\n" +
                                "#endif\n" +
                                "\n" +
                                "varying vec4 vertColor;\n" +
                                "\n" +
                                "void main() {\n" +
                                "  gl_FragColor = vec4(1.0,0.0,0.0,1.0);\n" +
                                "}"
                }, null);
        gl3.glCompileShader(fragShader);

        vertShader = gl3.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl3.glShaderSource(vertShader, 1,
                new String[]{
                        "#version 330 \n"
                                + "layout (location = 0) in vec4 position;"
                                + "layout (location = 1) in vec4 color;"
                                + "smooth out vec4 theColor;"
                                + "void main(){"
                                + "gl_Position.x = position.x / 500.0 - 1;"
                                + "gl_Position.y = -1 * position.y / 400.0 + 1;"
                                + "theColor = color;"
                                + "}"
                }, null);
        gl3.glCompileShader(vertShader);


        // attach and link
        gl3.glAttachShader(shaderProgram, vertShader);
        gl3.glAttachShader(shaderProgram, fragShader);
        gl3.glLinkProgram(shaderProgram);

        // program compiled we can free the object
        gl3.glDeleteShader(vertShader);
        gl3.glDeleteShader(fragShader);

    }

    private void vertexInit() {
        int line_count = 0;
        for (Trajectory traj : trajShow) {
            line_count += (traj.locations.length);
        }
        vertexData = new float[line_count * 2 * 2];

        int j = 0;
        for (Trajectory traj : trajShow) {
            for (int i = 0; i < traj.locations.length - 2; i++) {
                ScreenPosition pos = map.getScreenPosition(traj.locations[i]);
                ScreenPosition pos2 = map.getScreenPosition(traj.locations[i + 1]);
                vertexData[j++] = pos.x;
                vertexData[j++] = pos.y;
                vertexData[j++] = pos2.x;
                vertexData[j++] = pos2.y;
            }
        }

    }

    private long[] drawCPU() {
        long[] timeList = new long[2];
        ArrayList<ArrayList<Point>> trajPointList = new ArrayList<>();
        long t0 = System.currentTimeMillis();

        for (Trajectory traj : trajShow) {
            ArrayList<Point> pointList = new ArrayList<>();
            for (Location loc : traj.getLocations()) {
                ScreenPosition pos = map.getScreenPosition(loc);
                pointList.add(new Point(pos.x, pos.y));
            }
            trajPointList.add(pointList);
        }
        timeList[0] = (System.currentTimeMillis() - t0);
        long t1 = System.currentTimeMillis();
        for (ArrayList<Point> traj : trajPointList) {
            beginShape();
            for (Point pos : traj) {
                vertex(pos.x, pos.y);
            }
            endShape();
        }
        timeList[1] = (System.currentTimeMillis() - t1);
        return timeList;
    }

    private Trajectory[] getRan(Trajectory[] trajectories, double rate) {
        int size = (int) (rate * trajectories.length);
        Trajectory[] res = new Trajectory[size];
        HashSet<Integer> set = new HashSet<>();
        Random ran = new Random();
        while (set.size() != size) {
            set.add(ran.nextInt(trajectories.length - 1));
        }
        int i = 0;
        for (Integer e : set) {
            res[i++] = trajectories[e];
        }
        return res;
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{FullTimeProfiler.class.getName()});
    }

    class Point {
        float x;
        float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
