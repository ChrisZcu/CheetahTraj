package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import model.Position;
import model.TrajToSubpart;
import model.Trajectory;
import model.TrajectoryMeta;
import processing.core.PApplet;
import processing.opengl.PJOGL;
import util.PSC;

import javax.transaction.TransactionRequiredException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class RenderTimeCalculate extends PApplet {

    UnfoldingMap map;
    int wight = 1200, hight = 800;
    private String fullFile = "data/GPS/porto_full.txt";

    Trajectory[] trajFull;
    private String filePath = fullFile;

    @Override
    public void settings() {
        size(wight, hight, P2D);
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
//        map.zoomAndPanTo(11, new Location(22.717, 114.269));
        map.zoomAndPanTo(11, new Location(30.658524, 104.065747));
        MapUtils.createDefaultEventDispatcher(this, map);


        new Thread() {
            @Override
            public void run() {
                String porto = "data/GPS/porto_full.txt";
                String cdPath = "E:\\zcz\\dbgroup\\DTW\\data\\sz_cd\\cd_new_score.txt";
                loadData(cdPath);
                vqgs = loadVqgs("data/GPS/cd/cd_vfgs_0.txt");
                System.out.println("load done");
                isTotalLoad = true;
                gl3 = ((PJOGL) beginPGL()).gl.getGL3();
                endPGL();//?
            }
        }.start();

    }

    int num = 1000000;
    double[] rateList = {0.01, 0.005, 0.001, 0.0005, 0.0001};
    int rateId = 0;

    @Override
    public void draw() {
        if (!map.allTilesLoaded()) {
            map.draw();
        } else {
            if (isTotalLoad) {
//                Trajectory[] traj = getRandomTraj(num);
                System.out.println("rendering......");
//                Trajectory[] traj = trajFull;
                Trajectory[] traj = loadVqgsTraj(vqgs, rateList[rateId]);
                long t0 = System.currentTimeMillis();
                int pointNum = vertexInit(traj);
                long mappingTime = System.currentTimeMillis() - t0;
                long t1 = System.currentTimeMillis();
                drawGPU();
                long renderTime = System.currentTimeMillis() - t1;
                System.out.println("trajectory number: " + traj.length + ", " + "point number: " + pointNum + ", mapping time: " +
                        mappingTime + ", rendering time: " + renderTime + ", total time: " + (mappingTime + renderTime));
                rateId++;
                if (rateId == rateList.length) {
                    exit();
                }
//                num *= 10;
//                if (num > 2000000) {
//                    System.out.println("Done");
//                    exit();
//                }
//                noLoop();
            }
        }
    }

    boolean isTotalLoad = false;

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
//                Position[] metaGPS = new Position[data.length / 2 - 1];
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

    private Trajectory[] getRandomTraj(int number) {
        Trajectory[] trajectories = new Trajectory[number];
        Random random = new Random(0);

        HashSet<Integer> idSet = new HashSet<>(number);
        while (idSet.size() != number) {
            idSet.add(random.nextInt(trajFull.length - 1));
        }
        int i = 0;
        for (Integer id : idSet) {
            trajectories[i++] = trajFull[id];
        }
        return trajectories;

    }

    int[] vqgs;

    private int[] loadVqgs(String filePath) {
        int[] vqgs = null;
        ArrayList<String> tmpList = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                tmpList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        vqgs = new int[tmpList.size()];
        int i = 0;
        for (String str : tmpList) {
            vqgs[i++] = Integer.parseInt(str.split(",")[0]);
        }
        System.out.println(trajFull.length + ", " + vqgs.length);
        return vqgs;
    }

    private Trajectory[] loadVqgsTraj(int[] vqgsList, double rate) {
        int trajNum = (int) (trajFull.length * rate);
        Trajectory[] traj = new Trajectory[trajNum];
        for (int i = 0; i < trajNum; i++) {
            traj[i] = trajFull[vqgsList[i]];
        }
        return traj;
    }

    private double[] drawTraj(Trajectory[] trajFull) {
        int pointNum = 0;
        double[] time = new double[2];

        noFill();
        strokeWeight(1);
        stroke(new Color(255, 0, 0).getRGB());

        ArrayList<ArrayList<SolutionXTimeProfile.Point>> pointTraj = new ArrayList<>();
        long t0 = System.currentTimeMillis();
        for (Trajectory trajectory : trajFull) {
            pointNum += trajectory.locations.length;
            ArrayList<SolutionXTimeProfile.Point> tmpPointList = new ArrayList<>();
            for (Location loc : trajectory.locations) {
                ScreenPosition screenPos = map.getScreenPosition(loc);
                tmpPointList.add(new SolutionXTimeProfile.Point(screenPos.x, screenPos.y));
            }
            pointTraj.add(tmpPointList);
        }
        time[0] = (System.currentTimeMillis() - t0);
        long t1 = System.currentTimeMillis();
        for (ArrayList<SolutionXTimeProfile.Point> points : pointTraj) {
            beginShape();
            for (SolutionXTimeProfile.Point point : points) {
                vertex(point.x, point.y);
            }
            endShape();
        }
        time[1] = (System.currentTimeMillis() - t1);
        System.out.println("trajectory number: " + trajFull.length + ", " + "point number: " + pointNum + ", mapping time: " +
                time[0] + ", rendering time: " + time[1] + ", total time: " + (time[0] + time[1]));
        return time;
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
            }
        }
        return line_count;
    }

    @Override
    public void mousePressed() {
        Location location = new Location(mouseX, mouseY);
        System.out.println(location);
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{
                RenderTimeCalculate.class.getName()
        });
    }
}
