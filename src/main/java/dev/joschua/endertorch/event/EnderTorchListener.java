package dev.joschua.endertorch.event;

import dev.joschua.endertorch.database.TorchDatabase;
import dev.joschua.endertorch.database.TorchLocation;
import dev.joschua.endertorch.item.EnderTorchItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
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
            Location location = event.getBlock().getLocation();
            final TorchLocation torchLocation = TorchLocation.fromLocation(location, id);
            try {
                TorchDatabase.getInstance().insertOrUpdateLocationPair(torchLocation);
                spawnParticleAtLocation(Particle.CRIT_MAGIC, makeLocationCentered(location), 20);
            } catch (final SQLException exception) {
                Common.error(exception);
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onBlockBroken(final BlockBreakEvent event) {
        if(event.getBlock().getType().equals(Material.SOUL_TORCH)){
            Common.broadcast("BROKEN");
            Location location = event.getBlock().getLocation();
            Optional<TorchLocation> torch = TorchDatabase.getInstance().getTorchAtLocation(location);
            if(torch.isPresent()){
                try {
                    Optional<TorchLocation> destinationTorch = TorchDatabase.getInstance().getDestinationTorch(torch.get());
                    int pairId = torch.get().getTorchId();
                    int id = torch.get().getId();
                    TorchDatabase.getInstance().deleteLocationPair(pairId);
                    TorchDatabase.getInstance().deleteLocation(id);
                    event.setDropItems(false);
                    event.getBlock().getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 1, 0.7f);
                    if(destinationTorch.isPresent()){
                        TorchDatabase.getInstance().deleteLocation(destinationTorch.get().getId());
                        Location destinationLocation = destinationTorch.get().getLocation();
                        Block destinationBlock = destinationLocation.getBlock();
                        if (destinationBlock.getType().equals(Material.SOUL_TORCH)){
                            destinationBlock.setType(Material.AIR);
                            destinationBlock.getWorld().playSound(destinationLocation, Sound.BLOCK_GLASS_BREAK, 1, 0.7f);
                        }
                    }
                } catch (SQLException exception) {
                    Common.error(exception);
                }
            }
        }
    }

    @EventHandler
    public void onItemCrafted(final CraftItemEvent event) {
        try {
            if (event.getRecipe().getResult().isSimilar(EnderTorchItem.enderTorchRecipe.getResult())) {
                if(event.isShiftClick() || event.getClick().equals(ClickType.MIDDLE)){
                    event.setCancelled(true);
                    return;
                }
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
            makeLocationCentered(targetLocation);
            targetLocation.setDirection(playerLocation.getDirection());
            Common.tell(player, "Teleporting...");
            spawnParticleAtLocation(Particle.PORTAL, playerLocation, 40);
            player.getWorld().playSound(playerLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 10, 1);
            event.getPlayer().teleport(targetLocation);
            player.getWorld().playSound(targetLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 10, 1);
        }
    }

    private boolean playerPayForTeleport(final Player player) {
        return PlayerUtil.take(player, teleportPriceMaterial, teleportPriceAmount);
    }

    private Location makeLocationCentered(Location location){
        location.setX(location.getBlockX());
        location.setZ(location.getBlockZ());
        Vector offset = new Vector(0.5, 0, 0.5);
        return location.add(offset);
    }

    private void spawnParticleAtLocation(Particle particle, Location location, int count){
        location.getWorld().spawnParticle(particle, location, count);
    }
}
