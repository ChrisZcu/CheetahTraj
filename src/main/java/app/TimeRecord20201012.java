package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.RectRegion;
import model.Trajectory;
import processing.core.PApplet;
import processing.opengl.PJOGL;
import select.TimeProfileManager;
import util.PSC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

public class TimeRecord20201012 extends PApplet {
    Trajectory[] trajFull;
    UnfoldingMap map;
    UnfoldingMap mapClone;

    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.zoomAndPanTo(12, new Location(41.150, -8.639));
        map.setBackgroundColor(255);
        MapUtils.createDefaultEventDispatcher(this, map);

        mapClone = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapClone.setZoomRange(0, 20);
        mapClone.zoomAndPanTo(20, new Location(41.150, -8.639));

        String partPorto = "data/GPS/Porto5w/Porto5w.txt";
        String fullPorto = "data/GPS/porto_full.txt";
        loadData(fullPorto);

        rectRegion = new RectRegion();
        rectRegion.initLoc(new Location(map.getLocation(0, 0)), new Location(map.getLocation(1200, 800)));

        long wayPointBegin = System.currentTimeMillis();
        startCalWayPoint(); //waypoint
        wayPointCost = (System.currentTimeMillis() - wayPointBegin);

        ArrayList<Trajectory> trajShows = new ArrayList<>();
        for (Trajectory[] trajList : TimeProfileSharedObject.getInstance().trajRes) {
            Collections.addAll(trajShows, trajList);
        }
        trajWaypoint = trajShows.toArray(new Trajectory[0]);

        System.out.println("waypoint time: " + wayPointCost + ", " + trajWaypoint.length);
        gl3 = ((PJOGL) beginPGL()).gl.getGL3();
        endPGL();//?
    }

    long wayPointCost;
    RectRegion rectRegion;
    Trajectory[] trajWaypoint;

    private void startCalWayPoint() {
        TimeProfileManager tm = new TimeProfileManager(1, trajFull, rectRegion);
        tm.startRun();
    }

    @Override
    public void mousePressed() {
        System.out.println(mouseX + ", " + mouseY);
    }

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
                ArrayList<Location> locations = new ArrayList<>();
                for (int i = 0; i < data.length - 2; i = i + 2) {
                    locations.add(new Location(Float.parseFloat(data[i + 1]),
                            Float.parseFloat(data[i])));
                }
                traj.setLocations(locations.toArray(new Location[0]));
                traj.setScore(Integer.parseInt(item[0]));
                trajFull[j++] = traj;
            }
            trajStr.clear();
            System.out.println("load done");
            System.out.println("traj number: " + trajFull.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int alg = 1; //0 for full, 1 for random, 2 for vfgs+16, 3 for vfgs+32

    @Override
    public void draw() {
        if (!map.allTilesLoaded()) {
            map.draw();
            isFirstClean = true;
        } else {
//            if (isFirstClean) {
//                map.draw();
//                isFirstClean = false;
//            }

            Trajectory[] trajShow;
            //alg,waypointCost,algCost,vis,total
            StringBuilder sb = new StringBuilder().append(alg).append(",").append(wayPointCost).append(",");
            long algCost = 0;
            if (alg == 0) {
                trajShow = trajWaypoint;
            } else if (alg == 1) { // random
                long t0 = System.currentTimeMillis();
                trajShow = getRandomTraj(trajWaypoint, 0.01);
                algCost = (System.currentTimeMillis() - t0);
            } else if (alg == 2) {
                long t0 = System.currentTimeMillis();
                trajShow = util.VFGS.getCellCover(trajWaypoint, mapClone, 0.01, 16);
                algCost = (System.currentTimeMillis() - t0);
            } else {
                long t0 = System.currentTimeMillis();
                trajShow = util.VFGS.getCellCover(trajWaypoint, mapClone, 0.01, 32);
                algCost = (System.currentTimeMillis() - t0);
            }
            System.out.println(alg + ", trajShow: " + trajShow.length);
            sb.append(algCost).append(",");
            long visT0 = System.currentTimeMillis();
            vertexInit(trajShow);
//            long mappint = (System.currentTimeMillis() - visT0);
            drawGPU();
            long visTime = (System.currentTimeMillis() - visT0);
//            System.out.println("mapping time: " + mappint);
            sb.append(visTime).append(",").append((wayPointCost + algCost + visTime));
            System.out.println(sb.toString());
//            exit();
            alg++;
            if (alg > 3) {
                exit();
            }
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
                                + "gl_Position.x = position.x / 600.0 - 1;"
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

    private int vertexInit(Trajectory[] trajShow) {
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
//                System.out.println(pos.x);
//                System.out.println(pos.y);
            }
        }
        return line_count;
    }

    private Trajectory[] getRandomTraj(Trajectory[] trajFull, double rate) {
        int trajNum = (int) (trajFull.length * rate);
        HashSet<Integer> isSet = new HashSet<>(trajNum);
        Random random = new Random(0);
        while (isSet.size() != trajNum) {
            isSet.add(random.nextInt(trajFull.length - 1));
        }
        Trajectory[] trajectories = new Trajectory[trajNum];
        int i = 0;
        for (Integer id : isSet) {
            trajectories[i++] = trajFull[id];
        }
        return trajectories;
    }

    private boolean isFirstClean = true;

    public static void main(String[] args) {
        PApplet.main(new String[]{
                TimeRecord20201012.class.getName()
        });
    }
}
