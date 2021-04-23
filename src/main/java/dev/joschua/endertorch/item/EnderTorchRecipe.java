package dev.joschua.endertorch.item;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.HashMap;

public class EnderTorchRecipe {
    private static final SimplePlugin plugin = SimplePlugin.getInstance();
    private static final NamespacedKey recipeKey = new NamespacedKey(plugin, "ender_torch");

    @Getter
    private static final ShapedRecipe recipe = createRecipe();

    private static final Material material = Material.SOUL_TORCH;
    private static final ItemStack itemStack = new ItemStack(material);

    /**
     * Recipe Shape and ingredients
     */
    private static final String[] shape = {
            " E ",
            " O ",
            " O ",
    };
    private static final HashMap<Character, Material> ingredients = new HashMap<Character, Material>() {
        private static final long serialVersionUID = 4362445553275222462L;

        {
            put('E', Material.ENDER_PEARL);
            put('O', Material.OBSIDIAN);
        }
    };

    private static ShapedRecipe createRecipe() {
        final ShapedRecipe recipe = new ShapedRecipe(recipeKey, itemStack);
        recipe.shape(shape);

        for (final Character key : ingredients.keySet()
        ) {
            recipe.setIngredient(key, ingredients.get(key));
        }

        return recipe;
    }

    private static boolean isSameRecipe(final ShapedRecipe targetRecipe) {
        return targetRecipe.getKey().equals(recipeKey);
    }
}
