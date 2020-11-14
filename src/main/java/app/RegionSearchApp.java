package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import draw.TrajDrawManagerSingleMap;
import index.QuadTree;
import index.SearchRegionPart;
import model.*;
import org.w3c.dom.css.Rect;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.KeyEvent;
import util.PSC;

import java.awt.*;
import java.util.Scanner;


public class RegionSearchApp extends PApplet {

    UnfoldingMap map;
    private int ZOOMLEVEL = 11;
    private Location PRESENT = /*new Location(22.717, 114.269)*/ new Location(41.151, -8.616);
    private double[] latLon = new double[4];
    private TrajectoryMeta[] trajFull;

    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    String cdPath = "E:\\zcz\\dbgroup\\DTW\\data\\sz_cd\\cd_new_score.txt";
    String szPath = "E:\\zcz\\dbgroup\\DTW\\data\\sz_cd\\sz_score.txt";
    private String partFilePath = "data/GPS/Porto5w/Porto5w.txt";
    private String fullFilePath = "data/GPS/porto_full.txt";
    private String filePath = fullFilePath;
    private QuadRegion quadRegionRoot;
    private boolean indexDone = false;

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);
        map.setBackgroundColor(255);
        MapUtils.createDefaultEventDispatcher(this, map);

//        initButton();

        new Thread() {
            @Override
            public void run() {
                trajFull = QuadTree.loadData(latLon, filePath);
                TimeProfileSharedObject.getInstance().trajMetaFull = trajFull;
                QuadTree.trajMetaFull = trajFull;
                String qtPath = "data/GPS/QuadTreeIndex/quad_tree_quality" + quadQuality + "_info.txt";
//                QuadTree.saveTreeToFile(qtPath);
//                System.out.println("save finished.");
                QuadTree.loadTreeFromFile(qtPath);
                quadRegionRoot = QuadTree.quadRegionRoot;
//                quadRegionRoot = QuadTree.getQuadIndexPart(filePath, 5, 32);
                System.out.println("load finished");

//                QuadTree.quadRegionRoot = quadRegionRoot;
                loadDone = true;
                indexDone = true;

            }
        }.start();

        rectRegion = new RectRegion();
        rectRegion.initLoc(new Location(41.216, -8.734), new Location(41.074, -8.458));
    }

    private boolean regionDrawing = false;
    Position lastClick;
    RectRegion rectRegion;
    TrajectoryMeta[] trajShow = new TrajectoryMeta[0];
    boolean print = true;
    private PImage mapImage = null;


    @Override
    public void draw() {
        if (!map.allTilesLoaded()) {
            if (mapImage == null) {
                mapImage = map.mapDisplay.getInnerPG().get();
            }
            image(mapImage, 0, 0);
            map.draw();
        } else {
            map.draw();
            if (regionDrawing) {
                rectRegion = getSelectRegion(lastClick);

            }
            if (TimeProfileSharedObject.getInstance().calDone) {
                trajShow = TimeProfileSharedObject.getInstance().trajectoryMetas;
                if (trajShow == null)
                    trajShow = new TrajectoryMeta[0];
                System.out.println("trajshow number>>>>" + trajShow.length);
                TrajDrawManagerSingleMap trajManager = new TrajDrawManagerSingleMap(trajShow, 1, this, map);
                trajManager.startDraw();
                TimeProfileSharedObject.getInstance().calDone = false;
            }

            if (indexDone) {
//                System.out.println(TimeProfileSharedObject.getInstance().getQudaRegion().size());
//                System.out.println();
                for (int i = 0; i < 0; i++) {
                    RectRegion rectRegion = TimeProfileSharedObject.getInstance().getQudaRegion().get(i);
                    drawRecRegion(rectRegion);
                    if (print) {
                        System.out.println(rectRegion.getLeftTopLoc());
                        System.out.println(rectRegion.getRightBtmLoc());
                    }
                }
//                print = false;
//                ScreenPosition src = map.getScreenPosition(new Location(41.234, -8.554));
//                ScreenPosition src2 = map.getScreenPosition(new Location(41.182, -8.349));
//
//                noFill();
//                strokeWeight(10);
//                stroke(new Color(19, 149, 186).getRGB());
//                point(src.x, src.y);
//                point(src2.x, src2.y);
            }
//            if (TimeProfileSharedObject.getInstance().searchRegions.size() > 0) {
//                for (RectRegion rectRegion : TimeProfileSharedObject.getInstance().searchRegions) {
//                    drawRecRegion(rectRegion);
//                }
//            }

            drawTrajCPU();

            drawRecRegion();

            drawComponent();
            //draw lines

        }
    }

    @Override
    public void mousePressed() {
        if (mouseButton == RIGHT) {
            regionDrawing = true;
            TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
            lastClick = new Position(mouseX, mouseY);
        } else {
            Location loc = map.getLocation(mouseX, mouseY);
            System.out.println(loc);
            buttonClickListener();
            System.out.println("zoom level: " + map.getZoomLevel());
        }
    }

    @Override
    public void mouseReleased() {
        if (regionDrawing) {
            regionDrawing = false;
            rectRegion = getSelectRegion(lastClick);
        }
        if (panning || zoom) {
            panning = false;
            zoom = false;
            finishClick();
        }

    }

    private boolean panning = false;

    @Override
    public void mouseDragged() {
        if (mouseButton == LEFT) {
            panning = true;

            TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
        }
    }

    private boolean zoom = false;

    @Override
    public void mouseWheel() {
        zoom = true;
        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
    }

    private double quality = 1;
    private int quadQuality = 4;

    @Override
    public void keyPressed() {
        if (key == 'q') {
            Scanner scanner = new Scanner(System.in);
            quality = scanner.nextDouble();
            System.out.println("quality selected now: " + quality);
        } else if (key == 'w') {
            saveFrame("data/picture/tmp/quality_" + quadQuality + ".png");
        }else if (key == 'e'){
            finishClick();
            finishClick = true;
        }
    }

    private EleButton[] dataButtonList = new EleButton[0];

    private void initButton() {
        dataButtonList = new EleButton[2];
        int dataButtonXOff = 4;
        int dataButtonYOff = 4;
        dataButtonList[0] = new EleButton(dataButtonXOff, dataButtonYOff + 5, 70, 20, 0, "Finish");
        dataButtonList[1] = new EleButton(dataButtonXOff, dataButtonYOff + 35, 70, 20, 1, "All");

    }

    private RectRegion getSelectRegion(Position lastClick) {
        float mapWidth = 1200;
        float mapHeight = 800;

        int mx = (int) constrain(mouseX, 3, mapWidth - 3);
        int my = (int) constrain(mouseY, 3, mapHeight - 3);


        Position curClick = new Position(mx, my);


        RectRegion selectRegion = new RectRegion();
        if (lastClick.x < curClick.x) {//left
            if (lastClick.y < curClick.y) {//up
                selectRegion.leftTop = lastClick;
                selectRegion.rightBtm = curClick;
            } else {//left_down
                Position left_top = new Position(lastClick.x, curClick.y);
                Position right_btm = new Position(curClick.x, lastClick.y);
                selectRegion = new RectRegion(left_top, right_btm);
            }
        } else {//right
            if (lastClick.y < curClick.y) {//up
                Position left_top = new Position(curClick.x, lastClick.y);
                Position right_btm = new Position(lastClick.x, curClick.y);
                selectRegion = new RectRegion(left_top, right_btm);
            } else {
                selectRegion = new RectRegion(curClick, lastClick);
            }
        }
        selectRegion.color = PSC.COLOR_LIST[1];


        selectRegion.initLoc(map.getLocation(selectRegion.leftTop.x, selectRegion.leftTop.y),
                map.getLocation(selectRegion.rightBtm.x, selectRegion.rightBtm.y));

        return selectRegion;
    }

    private void drawRecRegion() {
        if (rectRegion == null)
            return;
        noFill();
        strokeWeight(2);
        stroke(new Color(19, 149, 186).getRGB());

        ScreenPosition src1 = map.getScreenPosition(rectRegion.getLeftTopLoc());
        ScreenPosition src2 = map.getScreenPosition(rectRegion.getRightBtmLoc());
        rect(src1.x, src1.y, src2.x - src1.x, src2.y - src1.y);
    }

    private void drawRecRegion(RectRegion rectRegion) {
        if (rectRegion == null)
            return;
        noFill();
        strokeWeight(2);
        stroke(new Color(79, 79, 79).getRGB());

        ScreenPosition src1 = map.getScreenPosition(rectRegion.getLeftTopLoc());
        ScreenPosition src2 = map.getScreenPosition(rectRegion.getRightBtmLoc());
        rect(src1.x, src1.y, src2.x - src1.x, src2.y - src1.y);
    }

    private void drawTrajCPU() {
        if (TimeProfileSharedObject.getInstance().trajImageMtx == null) {
            return;
        }
        for (PGraphics pg : TimeProfileSharedObject.getInstance().trajImageMtx) {
            if (pg == null) {
                continue;
            }
            image(pg, 0, 0);
        }
    }

    private void drawComponent() {
        for (EleButton eleButton : dataButtonList) {
            eleButton.render(this);
        }
    }

    boolean finishClick = false;

    private void buttonClickListener() {
        // not in one map mode, now there are 4 map in the map
        int eleId = -1;
        for (EleButton dataButton : dataButtonList) {
            if (dataButton.isMouseOver(this, true)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        if (eleId != -1) {
            if (eleId == 0) {//finish
                System.out.println("button finish");
                finishClick();
                finishClick = true;
            } else if (eleId == 1) {
                rectRegion = null;
                finishClick();
                TimeProfileSharedObject.getInstance().trajectoryMetas = trajFull;
                TimeProfileSharedObject.getInstance().calDone = true;
                finishClick = true;
            }
        }
    }

    private boolean loadDone = false;

    private void finishClick() {
        if (!loadDone) {
            System.out.println("!!!!!!Data not done, wait....");
            return;
        }
        if (rectRegion == null) {
//            TimeProfileSharedObject.getInstance().trajectoryMetas = trajFull;
            System.out.println("null");
            TimeProfileSharedObject.getInstance().trajectoryMetas = new TrajectoryMeta[0];

            TimeProfileSharedObject.getInstance().calDone = true;

            return;
        }
        new Thread() {
            @Override
            public void run() {
                System.out.println("calculating....");

                double leftLat = rectRegion.getLeftTopLoc().getLat();
                double leftLon = rectRegion.getLeftTopLoc().getLon();
                double rightLon = rectRegion.getRightBtmLoc().getLon();
                double rightLat = rectRegion.getRightBtmLoc().getLat();

                double minLat = Math.min(leftLat, rightLat);
                double maxLat = Math.max(leftLat, rightLat);
                double minLon = Math.min(leftLon, rightLon);
                double maxLon = Math.max(leftLon, rightLon);
                System.out.println(minLat + ", " + maxLat + ", " + minLon + ", " + maxLon);

                TimeProfileSharedObject.getInstance().searchRegions.clear();
//                TimeProfileSharedObject.getInstance().trajectoryMetas = SearchRegion.searchRegion(minLat, maxLat, minLon, maxLon, quadRegionRoot, quality);
                TimeProfileSharedObject.getInstance().trajectoryMetas = SearchRegionPart.searchRegion(minLat, maxLat, minLon, maxLon, quadRegionRoot, quality);
                TimeProfileSharedObject.getInstance().calDone = true;
                System.out.println("Calculate done!");
            }
        }.start();
    }

    public static void main(String[] args) {

        System.out.println();

        PApplet.main(new String[]{RegionSearchApp.class.getName()});
    }
}
