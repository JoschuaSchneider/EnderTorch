package dev.joschua.endertorch.event;

import dev.joschua.endertorch.database.TorchLocation;
import dev.joschua.endertorch.database.TorchLocationDatabase;
import dev.joschua.endertorch.item.EnderTorchItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

    TorchLocationDatabase torchLocationDatabase = new TorchLocationDatabase();

    private static int currentId = 0;

    public static int getNextId() {
        return currentId++;
    }


    private final CompMaterial teleportPriceMaterial = CompMaterial.fromMaterial(Material.DIAMOND);
    private final int teleportPriceAmount = 1;
    private final String teleportPriceTextual = "20 Diamonds";
    @EventHandler
    public void onBlockPlaced(final BlockPlaceEvent event) {

        final Player player = event.getPlayer();

        Common.tell(player, "You placed the Block", event.getBlockPlaced().getBlockData().getMaterial().name());

        if (event.getItemInHand().getItemMeta().getPersistentDataContainer().has(EnderTorchItem.enderTorchIdKey, PersistentDataType.INTEGER)) {
            final int id = event.getItemInHand().getItemMeta().getPersistentDataContainer().get(EnderTorchItem.enderTorchIdKey, PersistentDataType.INTEGER);
            Common.tell(player, "Torch placed with ID:" + id);
            final TorchLocation torchLocation = TorchLocation.fromLocation(event.getBlock().getLocation(), id);
            try {
                TorchLocationDatabase.getInstance().insertTorchLocation(torchLocation);
            } catch (final SQLException exception) {
                Common.error(exception);
            }
        }
    }

    @EventHandler
    public void onItemCrafted(final CraftItemEvent event) {
        Common.log("Crafting!!!!");
        if (event.getRecipe().getResult().isSimilar(EnderTorchItem.enderTorchRecipe.getResult())) {
            Common.log("Recipe found");
            final ItemStack itemStack = event.getCurrentItem();
            final ItemMeta meta = itemStack.getItemMeta();
            final int id = getNextId();
            EnderTorchItem.setIdOnName(meta, id);
            meta.getPersistentDataContainer().set(EnderTorchItem.enderTorchIdKey, PersistentDataType.INTEGER, id);
            itemStack.setItemMeta(meta);
            event.setCurrentItem(itemStack);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteraction(final PlayerInteractEvent event) {
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if(event.getHand() != EquipmentSlot.HAND) return;
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        Common.tell(player, "Thanks for interacting with your " + event.getHand() + "...");
        if (block != null && block.getType() == Material.SOUL_TORCH){
            event.setCancelled(true);
            final Location blockLocation = block.getLocation();
            try{
                final Optional<TorchLocation> destinationTorchLocationFromDB = TorchLocationDatabase.getInstance().getDestinationTorch(blockLocation);
                //Common.tell(player, "Present:", torchLocationFromDB.isPresent() ? "true" : "false");
                if(destinationTorchLocationFromDB.isPresent()){
                    Common.tell(player, "Paying...");
                    if(!playerPayForTeleport(player)){
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BAMBOO_HIT, 5, 1);
                        //Common.tell(player, "Sorry you cannot afford to teleport, please come back with " + teleportPriceTextual + ".");
                        return;
                    }
                    final Location playerLocation = player.getLocation();
                    final Location targetLocation = destinationTorchLocationFromDB.get().getLocation();
                    targetLocation.setDirection(playerLocation.getDirection());
                    Common.tell(player, "Teleporting...");
                    player.getWorld().playSound(playerLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 10, 1);
                    player.getWorld().playSound(targetLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 10, 1);
                    event.getPlayer().teleport(targetLocation);
                }
            }
            catch (final SQLException exception) {
                Common.error(exception);
            }
        }
    }

    public boolean playerPayForTeleport(final Player player){
        return PlayerUtil.take(player, teleportPriceMaterial, teleportPriceAmount);
    }
}
