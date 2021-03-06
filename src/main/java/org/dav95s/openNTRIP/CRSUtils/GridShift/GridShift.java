package org.dav95s.openNTRIP.CRSUtils.GridShift;

import com.harium.storage.kdtree.KDTree;
import com.harium.storage.kdtree.KeyDuplicateException;
import com.harium.storage.kdtree.KeySizeException;
import org.dav95s.openNTRIP.CRSUtils.Geoids.GGF;
import org.dav95s.openNTRIP.CRSUtils.Geoids.IGeoid;
import org.dav95s.openNTRIP.Databases.Models.GridModel;
import org.dav95s.openNTRIP.Tools.NMEA;
import org.dav95s.openNTRIP.Tools.RTCMStream.BitUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GridShift {
    private final Logger logger = LoggerFactory.getLogger(GridShift.class.getName());
    //crs boundary box
    double area_top;
    double area_bottom;
    double area_left;
    double area_right;
    //number of points in the grid
    int colCount;
    int rowCount;
    //resolution of the grid
    double dLat0 = 0.004167;
    double dLon0 = 0.008333;

    IGeoid geoid;

    GridNode[][] grid;

    public GridShift(int crs_id, JSONObject validArea, String geoidPath) {
        geoid = initGeoid(geoidPath);
        double latC = validArea.getDouble("LatCenter");
        double lonC = validArea.getDouble("LonCenter");
        double height = validArea.getDouble("Height");
        double width = validArea.getDouble("Width");

        //crs boundary
        area_top = latC + height / 2;
        area_bottom = latC - height / 2;
        area_left = lonC - width / 2;
        area_right = lonC + width / 2;

        if (area_left < -180)
            area_left += 360;

        if (area_left > 180)
            area_left -= 360;

        if (area_right < -180)
            area_right += 360;

        if (area_right > 180)
            area_right -= 360;

        //resolution of crs grid
        colCount = (int) (Math.abs(area_right - area_left) / dLon0);
        rowCount = (int) (Math.abs(area_top - area_bottom) / dLat0);

        grid = new GridNode[rowCount][colCount];

        initGrid(crs_id);

    }

    private void initGrid(int crs_id) {
        KDTree<GeodeticPoint> kdTree = new KDTree<>(2);
        GridModel gridModel = new GridModel();
        try {
            //get from db all geodetic point
            ArrayList<GeodeticPoint> points = gridModel.getAddGeodeticPointByCrsId(crs_id);
            //create spatial index
            for (GeodeticPoint point : points) {
                try {
                    kdTree.insert(new double[]{point.north, point.east}, point);
                } catch (KeySizeException | KeyDuplicateException e) {
                    e.printStackTrace();
                }
            }

            //delete array from memory
            points = null;

            //generate grid
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    try {
                        double nodeLat = area_top - row * dLat0;
                        double nodeLon = area_left + col * dLon0;
                        //get nearest geodetic point for node of grid
                        List<GeodeticPoint> nearestPoints = kdTree.nearest(new double[]{nodeLat, nodeLon}, 5);
                        //init node of grid
                        grid[row][col] = IDW(nearestPoints, nodeLat, nodeLon);
                        grid[row][col].height = geoid.getValueByPoint(nodeLat, nodeLon);
                    } catch (KeySizeException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private GridNode IDW(List<GeodeticPoint> gridNodes, double nodeLat, double nodeLon) {
        double sum = 0;
        for (GeodeticPoint gridNode : gridNodes) {
            sum += gridNode.distance(nodeLat, nodeLon);
        }

        sum = 1 / sum;

        for (GeodeticPoint GeodeticPoint : gridNodes) {
            GeodeticPoint.distance = 1 / GeodeticPoint.distance / sum;
        }

        GridNode response = new GridNode();
        response.north = BitUtils.normalize(nodeLat, 9);
        response.east = BitUtils.normalize(nodeLon, 9);
        response.dEast = 0;
        response.dNorth = 0;

        for (GeodeticPoint gridNode : gridNodes) {
            response.dNorth += gridNode.dNorth * gridNode.distance;
            response.dEast += gridNode.dEast * gridNode.distance;
        }

        response.dNorth = BitUtils.normalize(response.dNorth, 9);
        response.dEast = BitUtils.normalize(response.dEast, 9);

        return response;
    }

    public GridNode[] get16PointsAroundUser(NMEA.GPSPosition user) {
        ArrayList<GridNode> grid16 = new ArrayList<>(16);
        int lon = (int) BitUtils.normalize((user.lon - area_left) / dLon0, 4) - 1;
        int lat = (int) BitUtils.normalize((user.lat - area_top) / dLat0, 4) - 1;
        for (int x = 0; x < 4; x++) {
            grid16.addAll(Arrays.asList(grid[lat + x]).subList(lon, 4 + lon));
        }
        return grid16.toArray(new GridNode[16]);
    }

    private IGeoid initGeoid(String geoidPath) {
        return new GGF(geoidPath);
    }
}
