package model;

import java.util.ArrayList;

public class CircleRegionGroup {
    private int groupId;
    private int wayPointLayer = 0;
    private ArrayList<ArrayList<CircleRegion>> wayPointLayerList;

    public CircleRegionGroup() {
        wayPointLayerList = new ArrayList<>();
    }

    public CircleRegionGroup(int groupId) {
        this.groupId = groupId;
        wayPointLayerList = new ArrayList<>();
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getWayPointLayer() {
        return wayPointLayer;
    }

    public void setWayPointLayer(int wayPointLayer) {
        this.wayPointLayer = wayPointLayer;
    }

    public ArrayList<ArrayList<CircleRegion>> getWayPointLayerList() {
        return wayPointLayerList;
    }

    public void setWayPointLayerList(ArrayList<ArrayList<CircleRegion>> wayPointLayerList) {
        this.wayPointLayerList = wayPointLayerList;
    }

    public void cleanWayPointRegions() {
        wayPointLayerList.clear();
    }

    public void updateWayPointLayer() {
        if (wayPointLayerList.size() == wayPointLayer + 1) {
            wayPointLayer++;
        }
    }

    public ArrayList<CircleRegion> getAllRegions() {
        ArrayList<CircleRegion> res = new ArrayList<>();
        if (wayPointLayerList != null) {
            for (ArrayList<CircleRegion> circleRegionList : wayPointLayerList) {
                res.addAll(circleRegionList);
            }
        }
        return res;
    }

    public void addWayPoint(CircleRegion r) {
        if (wayPointLayerList.size() <= wayPointLayer) {
            wayPointLayerList.add(new ArrayList<>());
        }
        wayPointLayerList.get(wayPointLayer).add(r);
        //TODO update the shared object
    }

    public CircleRegionGroup getCorrCircleWayPointGroup(int mapId) {
        CircleRegionGroup circleWayPointGroup = new CircleRegionGroup(groupId);
        circleWayPointGroup.setWayPointLayer(wayPointLayer);
        for (ArrayList<CircleRegion> wList : wayPointLayerList) {
            ArrayList<CircleRegion> tmp = new ArrayList<>();
            for (CircleRegion r : wList) {
                tmp.add(r.getCrsRegionCircle(mapId));
            }
            circleWayPointGroup.getWayPointLayerList().add(tmp);
        }
        return circleWayPointGroup;
    }
}