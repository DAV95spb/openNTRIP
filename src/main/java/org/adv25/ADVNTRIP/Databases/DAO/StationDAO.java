package org.adv25.ADVNTRIP.Databases.DAO;

import org.adv25.ADVNTRIP.Databases.Models.StationModel;

import java.sql.*;

public class StationDAO implements DAO<StationModel, String> {

    @Override
    public boolean create(StationModel model) {
        return false;
    }

    @Override
    public StationModel read(String s) {
        StationModel model = new StationModel();

        try (Connection con = DataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(SQL.GET.QUERY)) {

            statement.setString(1, s);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    model.setMountpoint(s);
                    model.setId(rs.getLong("id"));
                    model.setAuthentication(rs.getInt("authentication"));
                    model.setBitrate(rs.getInt("bitrate"));
                    model.setCarrier(rs.getInt("carrier"));
                    model.setCompression(rs.getString("compression"));
                    model.setCountry(rs.getString("country"));
                    model.setFee(rs.getInt("fee"));
                    model.setFormat(rs.getString("format"));
                    model.setFormatDetails(rs.getString("format-details"));
                    model.setGenerator(rs.getString("generator"));
                    model.setIdentifier(rs.getString("identifier"));
                    model.setLatitude(rs.getDouble("latitude"));
                    model.setLongitude(rs.getDouble("longitude"));
                    model.setMisc(rs.getString("misc"));
                    model.setNavSystem(rs.getString("nav-system"));
                    model.setNetwork(rs.getString("network"));
                    model.setNmea(rs.getInt("nmea"));
                    model.setSolution(rs.getInt("solution"));
                    model.setIs_online(rs.getInt("is_online"));
                    model.setPassword(rs.getString("password"));
                    model.setProperties(rs.getString("properties"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return model;
    }

    @Override
    public boolean update(StationModel model) {
        try (Connection con = DataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(SQL.UPDATE.QUERY)) {

            statement.setString(1, model.getIdentifier());
            statement.setString(2, model.getFormat());
            statement.setString(3, model.getFormatDetails());
            statement.setString(4, String.valueOf(model.getCarrier()));
            statement.setString(5, model.getNavSystem());
            statement.setString(6, model.getCountry());
            statement.setDouble(7, model.getLatitude());
            statement.setDouble(8, model.getLongitude());
            statement.setString(9, model.getGenerator());
            statement.setString(10, String.valueOf(model.getBitrate()));
            statement.setString(11, model.getMountpoint());

            if (statement.execute())
                return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setOnline(StationModel model) {
        try (Connection con = DataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(SQL.SET_STATUS.QUERY)) {

            statement.setInt(1, 1);
            statement.setString(2, model.getMountpoint());

            if (statement.execute())
                return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean setOffline(StationModel model) {
        try (Connection con = DataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(SQL.SET_STATUS.QUERY)) {

            statement.setInt(1, 0);
            statement.setString(2, model.getMountpoint());

            if (statement.execute())
                return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean setAllOffline() {
        try (Connection con = DataSource.getConnection();
             Statement statement = con.createStatement()) {

            if (statement.execute(SQL.SET_ALL_STATUS_OFFLINE.QUERY))
                return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(StationModel model) {
        return false;
    }

    enum SQL {
        SET_STATUS("UPDATE stations_info SET is_online = ? WHERE id = (select `id` FROM ntrip.stations WHERE `mountpoint` = ?);"),
        SET_ALL_STATUS_OFFLINE("UPDATE stations_info SET is_online = 0"),
        GET("SELECT stations.id, mountpoint, identifier, format, `format-details`, carrier, `nav-system`, network, country, latitude, longitude, nmea, solution, generator, compression, authentication, fee, bitrate, misc, password, is_online, properties FROM ntrip.stations LEFT JOIN ntrip.stations_info ON stations.id = stations_info.id WHERE mountpoint = ?"),
        UPDATE("UPDATE `ntrip`.`stations` SET `identifier` = ?, `format` = ?, `format-details` = ?, `carrier` = ?, `nav-system` = ?, `country` = ?, `latitude` = ?, `longitude` = ?, `generator`  = ?, `bitrate` = ? WHERE `mountpoint` = ?;");

        String QUERY;

        SQL(String query) {
            this.QUERY = query;
        }
    }
}