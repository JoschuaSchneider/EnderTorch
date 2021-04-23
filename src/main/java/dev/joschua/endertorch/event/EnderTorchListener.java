package dev.joschua.endertorch.event;

import dev.joschua.endertorch.database.TorchDatabase;
import dev.joschua.endertorch.database.TorchLocation;
import dev.joschua.endertorch.item.EnderTorchItem;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.remain.CompMaterial;

import java.sql.SQLException;
import java.util.Optional;

public class EnderTorchListener implements Listener {

    private final CompMaterial teleportPriceMaterial = CompMaterial.fromMaterial(Material.DIAMOND);
    private final int teleportPriceAmount = 1;
    private final String teleportPriceTextual = "1 Diamond";

    @EventHandler
    public void onBlockPlaced(final BlockPlaceEvent event) {

        final Player player = event.getPlayer();

        Common.tell(player, "You placed the Block", event.getBlockPlaced().getBlockData().getMaterial().name());

        if (event.getItemInHand().getItemMeta().getPersistentDataContainer().has(EnderTorchItem.enderTorchIdKey, PersistentDataType.INTEGER)) {
            final int id = event.getItemInHand().getItemMeta().getPersistentDataContainer().get(EnderTorchItem.enderTorchIdKey, PersistentDataType.INTEGER);
            Common.tell(player, "Torch placed with ID:" + id);
            final TorchLocation torchLocation = TorchLocation.fromLocation(event.getBlock().getLocation(), id);
            try {
                TorchDatabase.getInstance().insertOrUpdateLocationPair(torchLocation);
            } catch (final SQLException exception) {
                Common.error(exception);
            }
        }
    }

    @EventHandler
    public void onItemCrafted(final CraftItemEvent event) {
        try {
            Common.log("Crafting!!!!");
            if (event.getRecipe().getResult().isSimilar(EnderTorchItem.enderTorchRecipe.getResult())) {
                Common.log("Recipe found");
                final ItemStack itemStack = event.getCurrentItem();
                final ItemMeta meta = itemStack.getItemMeta();
                final int id = TorchDatabase.getInstance().getNextId();
                EnderTorchItem.setIdOnName(meta, id);
                meta.getPersistentDataContainer().set(EnderTorchItem.enderTorchIdKey, PersistentDataType.INTEGER, id);
                itemStack.setItemMeta(meta);
                event.setCurrentItem(itemStack);
            }
        } catch (SQLException exception) {
            Common.error(exception);
            Common.tell(event.getWhoClicked(), "Something went wrong...");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteraction(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        final Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.SOUL_TORCH) {
            final Location blockLocation = block.getLocation();
            Optional<TorchLocation> torchLocation = TorchDatabase.getInstance().getTorchAtLocation(blockLocation);

            torchLocation.ifPresent(location -> handleTorchInteracted(event, location));
        }
    }

    private void handleTorchInteracted(PlayerInteractEvent event, TorchLocation torchLocation) {
        event.setCancelled(true);
        final Player player = event.getPlayer();
        final Optional<TorchLocation> destinationTorch = TorchDatabase.getInstance().getDestinationTorch(torchLocation);
        //Common.tell(player, "Present:", torchLocationFromDB.isPresent() ? "true" : "false");
        if (destinationTorch.isPresent()) {
            Common.tell(player, "Paying...");
            if (!playerPayForTeleport(player)) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BAMBOO_HIT, 5, 1);
                //Common.tell(player, "Sorry you cannot afford to teleport, please come back with " + teleportPriceTextual + ".");
                return;
            }
            final Location playerLocation = player.getLocation();
            final Location targetLocation = destinationTorch.get().getLocation();
            targetLocation.setDirection(playerLocation.getDirection());
            Common.tell(player, "Teleporting...");
            player.getWorld().playSound(playerLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 10, 1);
            player.getWorld().playSound(targetLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 10, 1);
            event.getPlayer().teleport(targetLocation);
        }
    }

    private boolean playerPayForTeleport(final Player player) {
        return PlayerUtil.take(player, teleportPriceMaterial, teleportPriceAmount);
    }
}
