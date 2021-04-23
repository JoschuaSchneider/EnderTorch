package dev.joschua.endertorch.item;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.Optional;

public class EnderTorchItem {

    int internalId = 0;
    public static NamespacedKey enderTorchIdKey = new NamespacedKey(SimplePlugin.getInstance(), "ender_torch_id");
    public static final NamespacedKey enderTorchRecipeKey = new NamespacedKey(SimplePlugin.getInstance(), "ender_torch");
    public static final ShapedRecipe enderTorchRecipe = createRecipe();

    @Getter
    private static final NamespacedKey pairIdKey = new NamespacedKey(SimplePlugin.getInstance(), "pair_id");

    public EnderTorchItem(final Plugin plugin) {
    }

    public static ItemStack createPair(final int pairId) {
        final ItemStack itemStack = new ItemStack(EnderTorchRecipe.getRecipe().getResult().getType());

        itemStack.setAmount(2);
        setPairId(itemStack, pairId);

        return ItemCreator.of(itemStack)
                .name("Ender Torch")
                .lore("&fEnder pair " + pairId)
                .hideTags(true)
                .glow(true)
                .build()
                .make();
    }

    public static Optional<Integer> getPairId(final ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        Valid.checkNotNull(itemMeta);
        final PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

        final Integer pairId = persistentDataContainer.get(getPairIdKey(), PersistentDataType.INTEGER);

        return pairId == null ? Optional.empty() : Optional.of(pairId);
    }

    public static void setPairId(final ItemStack itemStack, final int pairId) {
        final ItemMeta itemMeta = itemStack.getItemMeta();

        Valid.checkNotNull(itemMeta);
        final PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

        persistentDataContainer.set(getPairIdKey(), PersistentDataType.INTEGER, pairId);

        itemStack.setItemMeta(itemMeta);
    }

    public static boolean isEnderTorch(final ItemStack itemStack) {
        if (itemStack.getType() != EnderTorchRecipe.getRecipe().getResult().getType()) {
            return false;
        }
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return false;

        final PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

        return persistentDataContainer.has(getPairIdKey(), PersistentDataType.INTEGER);
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
