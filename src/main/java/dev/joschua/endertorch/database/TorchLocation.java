package dev.joschua.endertorch.database;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@Getter
@Setter
public class TorchLocation {
    public int id;
    public String worldName;
    public int x;
    public int y;
    public int z;
    public int torchId;

    public static TorchLocation fromLocation(final Location location, final int torchId) {
        final TorchLocation torchLocation = new TorchLocation();
        torchLocation.setWorldName(location.getWorld().getName());
        torchLocation.setX(location.getBlockX());
        torchLocation.setY(location.getBlockY());
        torchLocation.setZ(location.getBlockZ());
        torchLocation.setTorchId(torchId);

        return torchLocation;
    }

    public Location getLocation() {
        return new Location(Bukkit.getServer().getWorld(worldName), getX(), getY(), getZ());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
