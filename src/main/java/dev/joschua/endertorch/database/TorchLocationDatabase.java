package dev.joschua.endertorch.database;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.database.SimpleDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;

public class TorchLocationDatabase extends SimpleDatabase {

    @Getter
    private final static TorchLocationDatabase instance = new TorchLocationDatabase();

    private final HashSet<TorchLocation> cachedLocations = new HashSet<>();

    public final void connect() {
        getInstance().connect("jdbc:sqlite:torch_locations.db", null, null, "torch_locations");
    }

    @SneakyThrows
    @Override
    protected void onConnected() {
        getInstance().update("CREATE TABLE IF NOT EXISTS {table} (id INTEGER PRIMARY KEY AUTOINCREMENT , world_name varchar(64), torch_id int, x int, y int, z int, UNIQUE (x,y,z))");
        getInstance().update("CREATE TABLE IF NOT EXISTS LocationPairs (torch_id int PRIMARY KEY NOT NULL, location_a int NOT NULL, location_b int DEFAULT NULL)");
        getInstance().cacheAllLocations();
    }

    private void cacheAllLocations() throws SQLException {
        getInstance().cachedLocations.clear();
        final ResultSet locations = getInstance().query("SELECT * FROM {table}");

        while (locations.next()) {
            final TorchLocation torchLocation = torchLocationFromResultSet(locations);
            getInstance().cachedLocations.add(torchLocation);
        }

        Common.log("Cached locations: " + getInstance().cachedLocations.size());
    }

    private TorchLocation torchLocationFromResultSet(final ResultSet resultSet) throws SQLException {
        final TorchLocation torchLocation = new TorchLocation();
        torchLocation.setId(resultSet.getInt("id"));
        torchLocation.setWorldName(resultSet.getString("world_name"));
        torchLocation.setTorchId(resultSet.getInt("torch_id"));
        torchLocation.setX(resultSet.getInt("x"));
        torchLocation.setY(resultSet.getInt("y"));
        torchLocation.setZ(resultSet.getInt("z"));

        return torchLocation;
    }

    public Optional<TorchLocation> getTorchLocationByLocation(final Location location) throws SQLException {

        final Optional<TorchLocation> tl = getInstance().cachedLocations.stream().filter(t -> {
            final Location l = t.getLocation();

            return l.getWorld() == location.getWorld()
                    && l.getBlockX() == location.getBlockX()
                    && l.getBlockY() == location.getBlockY()
                    && l.getBlockZ() == location.getBlockZ();
        }).findFirst();

        return tl;
    }

    public Optional<TorchLocation> getTorchLocationByData(final String world, final int x, final int y, final int z) throws SQLException {

        final Optional<TorchLocation> tl = getInstance().cachedLocations.stream().filter(t -> {
            final Location l = t.getLocation();

            return l.getWorld().getName().equals(world)
                    && l.getBlockX() == x
                    && l.getBlockY() == y
                    && l.getBlockZ() == z;
        }).findFirst();

        return tl;
    }

    public Optional<TorchLocation> getTorchLocationById(final int id) throws SQLException {

        final Optional<TorchLocation> tl = getInstance().cachedLocations.stream().filter(t -> t.id == id).findFirst();

        return tl;
    }


    public Optional<TorchLocation> getDestinationTorch(final Location location) throws SQLException{
        final Optional<TorchLocation> torchA_loc = getTorchLocationByLocation(location);
        if(!torchA_loc.isPresent()){
            return Optional.empty();
        }

        final PreparedStatement statement = getInstance().prepareStatement("SELECT location_a, location_b FROM LocationPairs WHERE location_a = ? OR location_b = ?");
        statement.setInt(1, torchA_loc.get().id);
        statement.setInt(2, torchA_loc.get().id);
        final ResultSet rs = statement.executeQuery();
        rs.next();
        final int loc_a_id = rs.getInt("location_a");
        final int loc_b_id = rs.getInt("location_b");


        if(rs.wasNull()){
            Common.log("This torch is not linked yet");
            return Optional.empty();
        }

        if(loc_a_id != torchA_loc.get().id){
            return getTorchLocationById(loc_a_id);
        }
        else {
            return getTorchLocationById(loc_b_id);
        }
    }

    public void insertTorchLocation(final TorchLocation torchLocation) throws SQLException {
        PreparedStatement statement = getInstance().prepareStatement("INSERT INTO {table} (world_name, torch_id, x, y, z) VALUES (?, ?, ?, ?, ?)");

        Common.broadcast("INSERTING...");

        statement.setString(1, torchLocation.worldName);
        statement.setInt(2, torchLocation.torchId);
        statement.setInt(3, torchLocation.x);
        statement.setInt(4, torchLocation.y);
        statement.setInt(5, torchLocation.z);
        statement.execute();
        getInstance().cacheAllLocations();
        final TorchLocation newLoc = getInstance().getTorchLocationByData(torchLocation.worldName,torchLocation.x,torchLocation.y,torchLocation.z).get();




        statement = getInstance().prepareStatement("INSERT OR IGNORE INTO LocationPairs (torch_id, location_a) VALUES (?, ?)");
        statement.setInt(1, newLoc.torchId);
        statement.setInt(2, newLoc.id);
        final int rowsAffected = statement.executeUpdate();

        Common.broadcast("Rows affected " + rowsAffected);

        if(rowsAffected == 0){
            statement = getInstance().prepareStatement("UPDATE LocationPairs SET location_b = ? WHERE torch_id = ?");
            statement.setInt(1, newLoc.id);
            statement.setInt(2, newLoc.torchId);
            statement.executeUpdate();
            Common.broadcast("Updating...");
        }

    }
}
