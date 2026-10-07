package ua.noe.block;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import ua.noe.NoEPlugin;
import ua.noe.event.NoECustomBlockBreakEvent;
import ua.noe.event.NoECustomBlockInteractEvent;
import ua.noe.event.NoECustomBlockPlaceEvent;
import ua.noe.item.NoEItem;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Placement, breaking, interaction and protection of NoE custom blocks. */
public final class BlockListener implements Listener {

    private final NoEPlugin plugin;

    public BlockListener(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    private NoEBlock definition(Block block) {
        String id = plugin.blocks().getId(block);
        if (id == null) {
            return null;
        }
        return plugin.factory().registry().get(id) instanceof NoEBlock b ? b : null;
    }

    // ------------------------------------------------------------------ place

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String id = plugin.identifier().getId(event.getItemInHand());
        if (id == null || !(plugin.factory().registry().get(id) instanceof NoEBlock def)) {
            return;
        }
        Block block = event.getBlockPlaced();
        NoECustomBlockPlaceEvent place = new NoECustomBlockPlaceEvent(event.getPlayer(), def, block);
        plugin.getServer().getPluginManager().callEvent(place);
        if (place.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        if (block.getType() != def.getPlacedMaterial()) {
            block.setType(def.getPlacedMaterial(), false);
        }
        plugin.blocks().put(block, def.getId());
        if (def.usesDisplay()) {
            spawnDisplay(block, def, event.getPlayer());
        }
    }

    private void spawnDisplay(Block block, NoEBlock def, Player placer) {
        ItemStack stack = plugin.factory().createSilent(def.getId(), 1);
        if (stack == null) {
            return;
        }
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        loc.setYaw(Math.round(placer.getLocation().getYaw() / 90f) * 90f + 180f);
        float s = (float) def.getDisplayScale();
        block.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(stack);
            display.setPersistent(true);
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(s, s, s), new AxisAngle4f()));
            display.getPersistentDataContainer().set(plugin.keys().blockDisplay, PersistentDataType.STRING, def.getId());
        });
    }

    private void removeDisplays(Block block) {
        World world = block.getWorld();
        for (Entity entity : world.getNearbyEntities(BoundingBox.of(block).expand(0.1),
                e -> e instanceof ItemDisplay
                        && e.getPersistentDataContainer().has(plugin.keys().blockDisplay, PersistentDataType.STRING))) {
            entity.remove();
        }
    }

    // ------------------------------------------------------------------ break

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String id = plugin.blocks().getId(block);
        if (id == null) {
            return;
        }
        if (!(plugin.factory().registry().get(id) instanceof NoEBlock def)) {
            plugin.blocks().remove(block); // definition was deleted from config: behave like a normal block
            return;
        }
        NoECustomBlockBreakEvent breakEvent = new NoECustomBlockBreakEvent(event.getPlayer(), def, block);
        plugin.getServer().getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        event.setDropItems(false);
        removeDisplays(block);
        plugin.blocks().remove(block);
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            dropLoot(def, block.getLocation().add(0.5, 0.5, 0.5));
        }
    }

    private void dropLoot(NoEBlock def, Location at) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (DropSpec drop : def.getDrops()) {
            int amount = drop.min() == drop.max() ? drop.min() : random.nextInt(drop.min(), drop.max() + 1);
            if (amount <= 0) {
                continue;
            }
            ItemStack stack = resolveDrop(def, drop, amount);
            if (stack != null) {
                at.getWorld().dropItemNaturally(at, stack);
            }
        }
    }

    private ItemStack resolveDrop(NoEBlock def, DropSpec drop, int amount) {
        if (DropSpec.SELF.equals(drop.ref())) {
            return plugin.factory().createSilent(def.getId(), amount);
        }
        NoEItem custom = plugin.factory().registry().get(drop.ref());
        if (custom != null) {
            return plugin.factory().createSilent(custom.getId(), amount);
        }
        Material material = Material.matchMaterial(drop.ref());
        return material != null && !material.isAir() ? new ItemStack(material, amount) : null;
    }

    // ------------------------------------------------------------------ explosions and pistons

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            NoEBlock def = definition(block);
            if (def == null) {
                continue;
            }
            it.remove(); // handled manually so the vanilla drop does not appear
            NoECustomBlockBreakEvent breakEvent = new NoECustomBlockBreakEvent(null, def, block);
            plugin.getServer().getPluginManager().callEvent(breakEvent);
            if (breakEvent.isCancelled()) {
                continue;
            }
            removeDisplays(block);
            plugin.blocks().remove(block);
            Location at = block.getLocation().add(0.5, 0.5, 0.5);
            block.setType(Material.AIR, false);
            dropLoot(def, at);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (plugin.blocks().isCustom(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (plugin.blocks().isCustom(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ interaction

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isSneaking() && !player.getInventory().getItemInMainHand().getType().isAir()) {
            return; // vanilla rule: sneaking with an item in hand means "place", not "use"
        }
        Block block = event.getClickedBlock();
        NoEBlock def = block == null ? null : definition(block);
        if (def == null) {
            return;
        }
        NoECustomBlockInteractEvent interact = new NoECustomBlockInteractEvent(player, def, block);
        plugin.getServer().getPluginManager().callEvent(interact);
        if (interact.isCancelled()) {
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }
        if (!def.allowsVanillaInteraction()) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
        for (String command : def.getInteractCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    // ------------------------------------------------------------------ cache housekeeping

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        plugin.blocks().unload(event.getChunk());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.blocks().unload(event.getWorld());
    }
}
