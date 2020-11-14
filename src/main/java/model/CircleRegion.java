package model;


import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;

import java.awt.*;

public class CircleRegion extends RegionModel {
    private Location circleCenter;

    public Location getRadiusLocation() {
        return radiusLocation;
    }

    public void setRadiusLocation(Location radiusLocation) {
        this.radiusLocation = radiusLocation;
    }

    private Location radiusLocation;

    private Color color;
    private int id; // click judge
    private int mapId; // map update

    private float centerX;

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    private float centerY;

    public CircleRegion() {

    }

    public CircleRegion(Location circleCenter, Location radiusLocation, int mapId) {
        this.circleCenter = circleCenter;
        this.mapId = mapId;
        this.radiusLocation = radiusLocation;
        updateCircleScreenPosition();
    }

    public Location getCircleCenter() {
        return circleCenter;
    }

    public void setCircleCenter(Location circleCenter) {
        this.circleCenter = circleCenter;
    }

    public float getRadius() {
        updateCircleScreenPosition();
        ScreenPosition lastClick = SharedObject.getInstance().getMapList()[mapId].getScreenPosition(radiusLocation);
        return (float) Math.pow((Math.pow(centerX - lastClick.x, 2) + Math.pow(centerY - lastClick.y, 2)), 0.5);
    }


    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public CircleRegion getCrsRegionCircle(int mapId) {
        UnfoldingMap map = SharedObject.getInstance().getMapList()[mapId];

        CircleRegion newCricleRegion = new CircleRegion(circleCenter, radiusLocation, mapId);
        newCricleRegion.setColor(color);
        newCricleRegion.setId(id);

        return newCricleRegion;
    }

    public void updateCircleScreenPosition() {
        UnfoldingMap map = SharedObject.getInstance().getMapList()[mapId];
        ScreenPosition pos = map.getScreenPosition(circleCenter);
        centerX = pos.x;
        centerY = pos.y;
    }

    @Override
    public String toString() {
        return "RegionCircle{" +
                "circleCenter=" + circleCenter +
                ", color=" + color +
                ", id=" + id +
                ", mapId=" + mapId +
                '}';
    }


}
