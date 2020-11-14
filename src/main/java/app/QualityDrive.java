package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import index.SearchRegionPart;
import index.VfgsForIndex;
import index.VfgsForIndexPart;
import model.*;
import org.lwjgl.Sys;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PJOGL;
import util.PSC;
import util.VFGS;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;

import static index.QuadTree.getWayPointPos;

public class QualityDrive extends PApplet {
    UnfoldingMap map;


    int wight = 1200, hight = 800;

    @Override
    public void settings() {
        size(wight, hight, P2D);
    }

    private boolean isDataLoadDone = false;
    QuadRegion quadRegionRoot = null;
    private PImage mapImage = null;

    TrajectoryMeta[] trajMetaFull;
    TrajectoryMeta[] trajShowMeta;
    int quadQuality = 4;
    //    String quadFilePath = "data/GPS/QuadTreeIndex/cd/cd_quad_tree_quality" + quadQuality + "_info.txt";
//    String quadFilePath = "data/GPS/QuadTreeIndex/sz/sz_quad_tree_quality" + quadQuality + "_info.txt";
//    String quadFilePath = "data/GPS/QuadTreeIndex/quad_tree_quality" + quadQuality + "_info.txt";

    //cd注意zoomlevel12
    String cdPath = "E:\\zcz\\dbgroup\\DTW\\data\\sz_cd\\cd_new_score.txt";
    String szPath = "E:\\zcz\\dbgroup\\DTW\\data\\sz_cd\\sz_score.txt";
    private String fullFile = "data/GPS/porto_full.txt";
    private String partFilePath = "data/GPS/Porto5w/Porto5w.txt";

    private String filePath = szPath;

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(12,
                new Location(30.658524, 104.065747)); //cd
//                new Location(22.629, 114.029)); // sz
//                new Location(41.150, -8.639)); // porto

        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread() {
            @Override
            public void run() {
                try {
                    trajMetaFull = QuadTree.loadData(new double[4], filePath);
                    int cnt = 0;
                    int max = 0;
                    for (TrajectoryMeta trajectoryMeta : trajMetaFull) {
                        cnt += trajectoryMeta.getPositions().length;
                        max = Math.max(max, trajectoryMeta.getPositions().length);
                    }
                    System.out.println("cnt: " + cnt);
                    System.out.println("max: " + max);
                    TimeProfileSharedObject.getInstance().trajMetaFull = trajMetaFull;
                    QuadTree.trajMetaFull = trajMetaFull;

                    System.out.println("total load done: " + trajMetaFull.length);

//                    QuadTree.loadTreeFromFile(quadFilePath);
//                    quadRegionRoot = QuadTree.quadRegionRoot;

//                    System.out.println("index done");
                    isDataLoadDone = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        gl3 = ((PJOGL) beginPGL()).gl.getGL3();
        endPGL();//?
    }

    int zoomCheck = -1;
    Location centerCheck = new Location(-1, -1);

    //    int[] qualityList = {/*60, 70, 80, 90,*/ 95, 97, 98, 99, 100};
//    int[] qualityList = {70, 90};
    int quality = 9;
    int qualityId = 0;
    double regionSize = 1.0;

    @Override
    public void draw() {
        if (!(zoomCheck == map.getZoomLevel() && centerCheck.equals(map.getCenter()))) {
            map.draw();
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else {
                zoomCheck = map.getZoomLevel();
                centerCheck = map.getCenter();
                System.out.println("load map done");
                map.draw();
                map.draw();
                map.draw();
                map.draw();
            }
        } else {
            if (isDataLoadDone) {
                System.out.println("calculating......");

                float x = (float) (600 * regionSize);
                float y = (float) (400 * regionSize);

                RectRegion rectRegion = new RectRegion();

                rectRegion.setLeftTopLoc(map.getLocation(600 - x, 400 - y));
                rectRegion.setRightBtmLoc(map.getLocation(600 + x, 400 + y));
//                rectRegion.setLeftTopLoc(map.getLocation(0, 0));
//                rectRegion.setRightBtmLoc(map.getLocation(1200, 800));

                double leftLat = rectRegion.getLeftTopLoc().getLat();
                double leftLon = rectRegion.getLeftTopLoc().getLon();
                double rightLon = rectRegion.getRightBtmLoc().getLon();
                double rightLat = rectRegion.getRightBtmLoc().getLat();

                double minLat = Math.min(leftLat, rightLat);
                double maxLat = Math.max(leftLat, rightLat);
                double minLon = Math.min(leftLon, rightLon);
                double maxLon = Math.max(leftLon, rightLon);

                double qualitySearch = quality / 10.0;
//                double qualitySearch = qualityList[qualityId] / 100.0;
//                long t0 = System.currentTimeMillis();

//                trajShowMeta = SearchRegionPart.searchRegion(minLat, maxLat, minLon, maxLon, quadRegionRoot, qualitySearch);

//                System.out.println(qualitySearch + ", " + regionSize + "," + "search time: " + (System.currentTimeMillis() - t0));
//                System.out.println(trajShowMeta.length);
//                System.out.println(qualitySearch + ", " + trajShowMeta.length * 1.0 / trajMetaFull.length);

//                if (qualityId == qualityList.length - 1) {
//                    exit();
//                } else {
//                    qualityId++;
//                }
//                if (quality == 10) {
//                    exit();
//                } else {
//                    quality++;
//                }


//                long mappingTime = vertexInit(trajShowMeta);
//                long t1 = System.currentTimeMillis();
//                drawGPU();
//                long renderTime = System.currentTimeMillis() - t1;
//                System.out.println(trajShowMeta.length);
//                System.out.println("regionSize: " + regionSize + ", quality" + qualitySearch +
//                        ", mapping time: " + mappingTime / 1000.0 + ", rendering time: " + renderTime / 1000.0 +
//                        ", total time: " + (mappingTime + renderTime) / 1000.0);
//                saveFrame("data/test_" + qualitySearch + "_" + regionSize + ".png");
//                exit();

                long beginTime = System.currentTimeMillis();
                TrajectoryMeta[] wayPointTrajMeta = getWayPointPos(trajMetaFull, minLat, maxLat, minLon, maxLon);
                TrajToSubpart[] trajToSubparts = VfgsForIndexPart.getVfgs(wayPointTrajMeta, 4, qualitySearch);

//                vertexInit(trajMetaFull);
                vertexInit(trajToSubparts);
                drawGPU();

                System.out.println(qualitySearch + ", " + regionSize + ", fly time: " + (System.currentTimeMillis() - beginTime) + " ms");
                saveFrame("data/test1.png");
                exit();
                int[] tmpList = new int[20000000];
                for (int i = 0; i < tmpList.length; i++) {
                    tmpList[i] = i;
                }
                if (regionSize == 1) {
                    regionSize = 1.0 / 64.0;
                    if (quality == 7) {
                        System.out.println("done");
                        exit();
                    } else {
                        quality -= 2;
                    }
                } else {
                    regionSize *= 2;
                }

            }
        }
    }

    GL3 gl3;
    int[] vboHandles;
    int shaderProgram, vertShader, fragShader;
    int vertexBufferObject;

    IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
    float[] vertexData = {};

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

    private long vertexInit(TrajectoryMeta[] trajShowMeta) {
        int line_count = 0;
        for (TrajectoryMeta traj : trajShowMeta) {
            line_count += (traj.getPositions().length);
        }
        System.out.println(line_count);
        float[] tmpCertex = new float[line_count * 2 * 2];
        long t0 = System.currentTimeMillis();
        int j = 0;
        for (TrajectoryMeta traj : trajShowMeta) {
            for (int i = 0; i < traj.getPositions().length - 1; i++) {
                Location loc1 = new Location(traj.getPositions()[i].x / 10000.0, traj.getPositions()[i].y / 10000.0);
                Location loc2 = new Location(traj.getPositions()[i + 1].x / 10000.0, traj.getPositions()[i + 1].y / 10000.0);
                ScreenPosition pos = map.getScreenPosition(loc1);
                ScreenPosition pos2 = map.getScreenPosition(loc2);
                tmpCertex[j++] = pos.x;
                tmpCertex[j++] = pos.y;
                tmpCertex[j++] = pos2.x;
                tmpCertex[j++] = pos2.y;
            }
        }
        vertexData = tmpCertex;
        return (System.currentTimeMillis() - t0);
    }

    private long vertexInit(TrajToSubpart[] trajShowMeta) {
        int line_count = 0;
        for (TrajToSubpart traj : trajShowMeta) {
            line_count += ((traj.getEndPosIdx() - traj.getBeginPosIdx() + 1));
        }
        System.out.println(line_count);
        float[] tmpCertex = new float[line_count * 2 * 2];
        long t0 = System.currentTimeMillis();
        int j = 0;
        for (TrajToSubpart trajToSubpart : trajShowMeta) {
            TrajectoryMeta traj = trajMetaFull[trajToSubpart.getTrajId()];
            for (int i = trajToSubpart.getBeginPosIdx(); i < trajToSubpart.getEndPosIdx() - 1; i++) {
                Location loc1 = new Location(traj.getPositions()[i].x / 10000.0, traj.getPositions()[i].y / 10000.0);
                Location loc2 = new Location(traj.getPositions()[i + 1].x / 10000.0, traj.getPositions()[i + 1].y / 10000.0);
                ScreenPosition pos = map.getScreenPosition(loc1);
                ScreenPosition pos2 = map.getScreenPosition(loc2);
                tmpCertex[j++] = pos.x;
                tmpCertex[j++] = pos.y;
                tmpCertex[j++] = pos2.x;
                tmpCertex[j++] = pos2.y;
            }
        }
        vertexData = tmpCertex;
        return (System.currentTimeMillis() - t0);
    }

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

    public static void main(String[] args) {
        PApplet.main(new String[]{
                QualityDrive.class.getName()
        });
    }
}
