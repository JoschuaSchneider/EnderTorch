package dev.joschua.endertorch.tools;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.mineacademy.fo.Common;

import java.util.HashMap;
import java.util.Map;

public class StructureChecker {
    @Setter
    @Getter
    String[][] layers;

    @Setter
    @Getter
    Vector offset = new Vector().zero();

    @Setter
    @Getter
    Map<Character, Material> buildingMaterials = new HashMap();


    public boolean checkAllStructureRotations(Location location) {
        for (int rotations = 0; rotations < 4; rotations++) {
            if (checkStrucktureAtLocationWithRotation(location, rotations)) return true;
        }
        return false;
    }

    public boolean checkStructureNoRotation(Location location) {
        return checkStrucktureAtLocationWithRotation(location, 0);
    }

    private boolean checkStrucktureAtLocationWithRotation(Location location, int rotations90degs) {
        Vector rotatedOffset = rotateVector90Degrees(offset.clone(), rotations90degs);
        Location locationAtZeroIndex = location.clone().add(rotatedOffset);
        for (int y = 0; y > -layers.length; y--) {
            String[] currentLayer = layers[-y];
            for (int x = 0; x > -currentLayer.length; x--) {
                String currentRow = currentLayer[-x];
                for (int z = 0; z < currentRow.length(); z++) {
                    Character characterAtPosition = currentRow.charAt(z);
                    if (characterAtPosition.equals(' ')) continue;
                    Vector checkOffset = new Vector(x, y, z);
                    Location locationToCheck = locationAtZeroIndex.clone().add(rotateVector90Degrees(checkOffset, rotations90degs));
                    Material expectedMaterial = buildingMaterials.get(characterAtPosition);
                    if (!locationToCheck.getBlock().getType().equals(expectedMaterial)) return false;
                }
            }
        }
        return true;
    }

    private Vector rotateVector90Degrees(Vector v, int times) {
        times %= 4;
        int oldX = v.getBlockX();
        switch (times) {
            case 0:
                return v;
            case 1:
                v.setX(-v.getZ());
                v.setZ(oldX);
                break;
            case 2:
                v.setX(-oldX);
                v.setZ(-v.getZ());
                break;
            case 3:
                v.setX(v.getZ());
                v.setZ(-oldX);
                break;
        }
        return v;
    }
}
