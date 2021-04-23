package dev.joschua.endertorch.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.plugin.SimplePlugin;

public class EnderTorchItem {

    int internalId = 0;
    public static NamespacedKey enderTorchIdKey = new NamespacedKey(SimplePlugin.getInstance(), "ender_torch_id");
    public static final NamespacedKey enderTorchRecipeKey = new NamespacedKey(SimplePlugin.getInstance(), "ender_torch");
    public static final ShapedRecipe enderTorchRecipe = createRecipe();

    public EnderTorchItem(final Plugin plugin) {
    }

    private static ShapedRecipe createRecipe() {
        final ShapedRecipe recipe = new ShapedRecipe(enderTorchRecipeKey, createEnderTorch());
        recipe.shape(
                " a ",
                " b ",
                " b "
        );

        recipe.setIngredient('a', Material.ENDER_PEARL);
        recipe.setIngredient('b', Material.OBSIDIAN);

        Bukkit.addRecipe(recipe);

        return recipe;
    }

    public static void setIdOnName(final ItemMeta meta, final int torchId) {
        meta.setDisplayName(meta.getDisplayName().replace("{id}", Integer.toString(torchId)));
    }

    public static ItemStack createEnderTorch() {
        final ItemStack enderTorch = new ItemStack(Material.SOUL_TORCH);

        final ItemMeta meta = enderTorch.getItemMeta();
        meta.setDisplayName("Ender Torch #{id}");
        enderTorch.setItemMeta(meta);
        enderTorch.setAmount(2);
        return enderTorch;
    }
}
