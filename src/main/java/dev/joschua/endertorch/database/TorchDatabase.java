package dev.joschua.endertorch.database;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import org.bukkit.Location;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.database.SimpleDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class TorchDatabase extends SimpleDatabase {
    @Getter
    private final static TorchDatabase instance = new TorchDatabase();

    private LocationPairCache locationPairCache = new LocationPairCache();

    private TorchDatabase() {
    }

    public final void connect() {
        connect("jdbc:sqlite:torch.db", null, null, "Location");
    }

    @Override
    protected void onConnected() {
        update("PRAGMA foreign_keys = ON");
        update("CREATE TABLE IF NOT EXISTS Location (id INTEGER PRIMARY KEY AUTOINCREMENT, world_name varchar(64), x int, y int, z int, UNIQUE (x,y,z))");
        update("CREATE TABLE IF NOT EXISTS LocationPairs (pair_id int PRIMARY KEY NOT NULL, location_a int NOT NULL, location_b int DEFAULT NULL, " +
                "FOREIGN KEY(location_a) REFERENCES Location(id) ON DELETE CASCADE," +
                "FOREIGN KEY(location_b) REFERENCES Location(id) ON DELETE CASCADE)");
        update("CREATE TABLE IF NOT EXISTS Ids (name varchar(64) PRIMARY KEY, value INTEGER)");
        update("INSERT OR IGNORE INTO Ids (name, value) VALUES ('currentTorchId', 0)");
        try {
            cacheAllLocations();
        } catch (SQLException exception) {
            Common.error(exception);
        }
    }

    private void cacheAllLocations() throws SQLException {
        locationPairCache.clear();
        final ResultSet locations = query("SELECT lp.pair_id, " +
                "la.id, la.world_name, la.x, la.y, la.z, " +
                "lb.id, lb.world_name, lb.x, lb.y, lb.z " +
                "FROM LocationPairs lp JOIN Location la ON lp.location_a = la.id " +
                "LEFT JOIN Location lb ON lp.location_b = lb.id");

        while (locations.next()) {
            addResultSetToCachedPairs(locations);
        }

        Common.log("Cached locations: " + locationPairCache.countLocations());
        Common.log("Cached pairs: " + locationPairCache.countPairs());
    }

    private void addResultSetToCachedPairs(ResultSet rs) throws SQLException {
        int pairID = rs.getInt(1);
        final TorchLocation torchLocationA = new TorchLocation();
        TorchLocation torchLocationB = null;

        int locationA_id = rs.getInt(2);

        torchLocationA.setTorchId(pairID);
        torchLocationA.setId(locationA_id);

        torchLocationA.setWorldName(rs.getString(3));
        torchLocationA.setX(rs.getInt(4));
        torchLocationA.setY(rs.getInt(5));
        torchLocationA.setZ(rs.getInt(6));

        int locationB_id = rs.getInt(7);
        if(!rs.wasNull()){
            torchLocationB = new TorchLocation();

            torchLocationB.setTorchId(pairID);
            torchLocationB.setId(locationB_id);

            torchLocationB.setWorldName(rs.getString(8));
            torchLocationB.setX(rs.getInt(9));
            torchLocationB.setY(rs.getInt(10));
            torchLocationB.setZ(rs.getInt(11));
        }
        locationPairCache.addTorchPair(torchLocationA, torchLocationB);
    }

    public int insertLocation(TorchLocation location) throws SQLException {
        PreparedStatement statement = this.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO Location (world_name, x, y, z) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

        statement.setString(1, location.worldName);
        statement.setInt(2, location.x);
        statement.setInt(3, location.y);
        statement.setInt(4, location.z);
        statement.executeUpdate();
        cacheAllLocations();

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
            else {
                throw new SQLException("Creating TorchLocation failed, no ID obtained.");
            }
        }
    }

    public void deleteLocation(int id) throws SQLException {
        PreparedStatement statement = prepareStatement("DELETE FROM Location WHERE id = ?");
        statement.setInt(1, id);
        statement.execute();
        cacheAllLocations();
    }

    public void insertOrUpdateLocationPair(TorchLocation newLoc) throws SQLException {
        int locID = insertLocation(newLoc);

        PreparedStatement statement = prepareStatement("INSERT OR IGNORE INTO LocationPairs (pair_id, location_a) VALUES (?, ?)");
        statement.setInt(1, newLoc.torchId);
        statement.setInt(2, locID);
        final int rowsAffected = statement.executeUpdate();

        if(rowsAffected == 0){
            Common.log("Updating...");
            statement = prepareStatement("UPDATE LocationPairs SET location_b = ? WHERE pair_id = ?");
            statement.setInt(1, locID);
            statement.setInt(2, newLoc.torchId);
            statement.executeUpdate();
        }
        cacheAllLocations();
    }

    public void deleteLocationPair(int pairId) throws SQLException {
        PreparedStatement statement = prepareStatement("DELETE FROM LocationPairs WHERE pair_id = ?");
        statement.setInt(1, pairId);
        statement.execute();
        cacheAllLocations();
    }

    public Optional<TorchLocation> getTorchAtLocation(final Location location){
        return locationPairCache.getTorchAtLocation(location);
    }

    public Optional<TorchLocation> getDestinationTorch(final TorchLocation torchLocation){
        return locationPairCache.getDestinationTorch(torchLocation);
    }

    public int getNextId() throws SQLException {
        update("UPDATE Ids SET value = value + 1 WHERE name = 'currentTorchId'");
        ResultSet queryResult = query("SELECT value FROM Ids WHERE name = 'currentTorchId'");
        if(!queryResult.next()){
            throw new SQLException("Expected torchId to exist");
        }
        return queryResult.getInt(1);
    }
}
