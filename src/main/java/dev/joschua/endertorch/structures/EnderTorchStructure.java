package dev.joschua.endertorch.structures;

import dev.joschua.endertorch.tools.StructureChecker;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class EnderTorchStructure extends StructureChecker {

    /**
     * Structure Shape and materials
     */
    private static final String[][] structureShape =
            {
                    {
                            "       ",
                            " S   S "
                    },
                    {
                            "  OOO  ",
                            "IC   CI"
                    },
                    {
                            "  OOO  ",
                            "OC   CO"
                    },
                    {
                            "  OOO  ",
                            "OC   CO"
                    },
                    {
                            "       ",
                            "  G G  "
                    },
            };

    private static final HashMap<Character, Material> materials = new HashMap<Character, Material>() {
        {
            put('I', Material.IRON_BLOCK);
            put('O', Material.OBSIDIAN);
            put('S', Material.SOUL_CAMPFIRE);
            put('C', Material.CYAN_WOOL);
            put('G', Material.GLOWSTONE);
        }
    };

    private static final Vector offSetFromTorch = new Vector(1, 3, -3);


    public EnderTorchStructure() {
        setLayers(structureShape);
        setOffset(offSetFromTorch);
        setBuildingMaterials(materials);
    }
}
