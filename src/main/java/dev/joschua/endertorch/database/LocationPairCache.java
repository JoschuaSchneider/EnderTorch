package dev.joschua.endertorch.database;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class LocationPairCache {
    private final Set<Vector> torchVectors = new HashSet<>();
    private final BiMap<TorchLocation, TorchLocation> torchLocationPairs = HashBiMap.create();

    public boolean locationIsStored(Location location){
        return torchVectors.contains(location.toVector());
    }

    public void clear() {
        torchVectors.clear();
        torchLocationPairs.clear();
    }

    public Optional<TorchLocation> getTorchAtLocation(final Location location){
        Optional<TorchLocation> result = torchLocationPairs.keySet().stream().filter((t) -> t.getVector().equals(location.toVector())).findFirst();
        if (result.isPresent()) return result;

        return torchLocationPairs.values().stream().filter((t) -> t.getVector().equals(location.toVector())).findFirst();
    }

    public Optional<TorchLocation> getDestinationTorch(TorchLocation startTorch){
        if(torchLocationPairs.containsKey(startTorch)){
            return Optional.of(torchLocationPairs.get(startTorch));
        }
        return Optional.ofNullable(torchLocationPairs.inverse().get(startTorch));
    }

    public void addTorchPair(TorchLocation torchLocationA, TorchLocation torchLocationB) {
        addTorchLocationToSet(torchLocationA);
        addTorchLocationToSet(torchLocationB);
        if(torchLocationB != null){
            torchLocationPairs.put(torchLocationA, torchLocationB);
        }
    }

    private void addTorchLocationToSet(TorchLocation torchLocation) {
        if (torchLocation != null)
            torchVectors.add(torchLocation.getVector());
    }

    public int countPairs(){
        return torchLocationPairs.size();
    }

    public int countLocations(){
        return torchVectors.size();
    }

}
