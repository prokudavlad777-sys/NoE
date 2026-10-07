package ua.noe.weapon;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ua.noe.NoEPlugin;
import ua.noe.event.NoEWeaponReloadEvent;
import ua.noe.event.NoEWeaponShootEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Shooting and reloading logic. Reload timers are one-shot tasks that are cancelled on switch, death and quit. */
public final class WeaponService {

    private final NoEPlugin plugin;
    private final Map<UUID, BukkitTask> reloads = new HashMap<>();

    public WeaponService(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ magazine state

    public int getLoaded(ItemStack stack, NoEWeapon weapon) {
        if (stack == null || !stack.hasItemMeta()) {
            return 0;
        }
        return stack.getItemMeta().getPersistentDataContainer()
                .getOrDefault(plugin.keys().ammo, PersistentDataType.INTEGER, weapon.getMagazine());
    }

    private void setLoaded(Player player, NoEWeapon weapon, int rounds) {
        PlayerInventory inv = player.getInventory();
        ItemStack held = inv.getItem(EquipmentSlot.HAND);
        if (held.getType().isAir()) {
            return;
        }
        ItemStack updated = held.clone();
        updated.editMeta(m -> m.getPersistentDataContainer().set(plugin.keys().ammo, PersistentDataType.INTEGER,
                Math.max(0, Math.min(rounds, weapon.getMagazine()))));
        inv.setItem(EquipmentSlot.HAND, updated);
    }

    public boolean isReloading(Player player) {
        return reloads.containsKey(player.getUniqueId());
    }

    // ------------------------------------------------------------------ shooting

    public void shoot(Player player, NoEWeapon weapon) {
        if (!weapon.isRanged() || isReloading(player)) {
            return;
        }
        ItemStack held = player.getInventory().getItem(EquipmentSlot.HAND);
        if (player.hasCooldown(held)) {
            return;
        }
        int loaded = getLoaded(held, weapon);
        if (loaded <= 0) {
            plugin.messages().send(player, "weapon-empty");
            playSound(player.getLocation(), weapon.getEmptySound(), 1f, 1f);
            player.setCooldown(held, 10);
            return;
        }
        NoEWeaponShootEvent event = new NoEWeaponShootEvent(player, weapon, held);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        setLoaded(player, weapon, loaded - 1);
        if (weapon.getFireRate() > 0) {
            player.setCooldown(player.getInventory().getItem(EquipmentSlot.HAND), weapon.getFireRate());
        }
        playSound(player.getLocation(), weapon.getShootSound(), 1f, 1f);

        if (weapon.getType() == WeaponType.BOW) {
            launchArrow(player, weapon);
        } else {
            fireHitscan(player, weapon);
        }
        player.sendActionBar(plugin.messages().component("weapon-ammo-status",
                "current", String.valueOf(loaded - 1), "max", String.valueOf(weapon.getMagazine())));
    }

    private void launchArrow(Player player, NoEWeapon weapon) {
        double speed = weapon.getProjectileSpeed();
        Vector velocity = player.getEyeLocation().getDirection().multiply(speed);
        Arrow arrow = player.launchProjectile(Arrow.class, velocity);
        arrow.setDamage(weapon.getShotDamage() / speed);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.getPersistentDataContainer().set(plugin.keys().projectile, PersistentDataType.BYTE, (byte) 1);
    }

    private void fireHitscan(Player player, NoEWeapon weapon) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        Map<LivingEntity, Double> damage = new LinkedHashMap<>();
        for (int i = 0; i < weapon.getPellets(); i++) {
            Vector direction = spreadDirection(eye, weapon.getSpread());
            RayTraceResult result = world.rayTrace(eye, direction, weapon.getRange(), FluidCollisionMode.NEVER,
                    true, 0.2, entity -> isTarget(entity, player));
            if (result == null) {
                continue;
            }
            if (result.getHitEntity() instanceof LivingEntity target) {
                damage.merge(target, weapon.getShotDamage(), Double::sum);
                world.spawnParticle(Particle.CRIT, result.getHitPosition().toLocation(world), 4, 0.05, 0.05, 0.05, 0.0);
            }
        }
        for (Map.Entry<LivingEntity, Double> hit : damage.entrySet()) {
            plugin.damageContext().setFirearm(true);
            try {
                hit.getKey().damage(hit.getValue(), player);
            } finally {
                plugin.damageContext().setFirearm(false);
            }
        }
    }

    private static boolean isTarget(Entity entity, Player shooter) {
        if (entity.equals(shooter) || !(entity instanceof LivingEntity living) || living.isDead()) {
            return false;
        }
        return !(entity instanceof Player p && p.getGameMode() == GameMode.SPECTATOR);
    }

    private static Vector spreadDirection(Location eye, double spreadDegrees) {
        if (spreadDegrees <= 0) {
            return eye.getDirection();
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location l = eye.clone();
        l.setYaw(l.getYaw() + (float) ((random.nextDouble() * 2 - 1) * spreadDegrees));
        l.setPitch((float) Math.max(-90, Math.min(90, l.getPitch() + (random.nextDouble() * 2 - 1) * spreadDegrees)));
        return l.getDirection();
    }

    // ------------------------------------------------------------------ reloading

    public void startReload(Player player, NoEWeapon weapon) {
        if (!weapon.isRanged() || isReloading(player)) {
            return;
        }
        ItemStack held = player.getInventory().getItem(EquipmentSlot.HAND);
        int need = weapon.getMagazine() - getLoaded(held, weapon);
        if (need <= 0) {
            plugin.messages().send(player, "weapon-full");
            return;
        }
        int available = weapon.getAmmo() == null ? need : countAmmo(player, weapon.getAmmo());
        if (available <= 0) {
            plugin.messages().send(player, "weapon-no-ammo");
            return;
        }
        int toLoad = Math.min(need, available);
        NoEWeaponReloadEvent event = new NoEWeaponReloadEvent(player, weapon, held, toLoad);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        player.sendActionBar(plugin.messages().component("weapon-reloading"));
        if (weapon.getReloadTime() <= 0) {
            finishReload(player, weapon);
            return;
        }
        reloads.put(player.getUniqueId(), plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> finishReload(player, weapon), weapon.getReloadTime()));
    }

    private void finishReload(Player player, NoEWeapon weapon) {
        reloads.remove(player.getUniqueId());
        if (!player.isOnline() || player.isDead()) {
            return;
        }
        ItemStack held = player.getInventory().getItem(EquipmentSlot.HAND);
        if (!weapon.getId().equals(plugin.identifier().getId(held))) {
            return;
        }
        int loaded = getLoaded(held, weapon);
        int need = weapon.getMagazine() - loaded;
        if (need <= 0) {
            return;
        }
        int take = weapon.getAmmo() == null ? need : Math.min(need, countAmmo(player, weapon.getAmmo()));
        if (take <= 0) {
            plugin.messages().send(player, "weapon-no-ammo");
            return;
        }
        if (weapon.getAmmo() != null) {
            removeAmmo(player, weapon.getAmmo(), take);
        }
        setLoaded(player, weapon, loaded + take);
        playSound(player.getLocation(), weapon.getReloadSound(), 1f, 1f);
        player.sendActionBar(plugin.messages().component("weapon-reloaded"));
    }

    public void cancelReload(Player player) {
        BukkitTask task = reloads.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /** Cancels every pending reload. Called on disable and reload so no task outlives the plugin. */
    public void shutdown() {
        reloads.values().forEach(BukkitTask::cancel);
        reloads.clear();
    }

    private int countAmmo(Player player, String ammoId) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && ammoId.equals(plugin.identifier().getId(stack))) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeAmmo(Player player, String ammoId, int amount) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !ammoId.equals(plugin.identifier().getId(stack))) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            remaining -= take;
            if (take == stack.getAmount()) {
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - take);
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    private static void playSound(Location loc, String key, float volume, float pitch) {
        if (key == null || key.isBlank() || loc.getWorld() == null) {
            return;
        }
        try {
            loc.getWorld().playSound(Sound.sound(Key.key(key), Sound.Source.PLAYER, volume, pitch),
                    loc.getX(), loc.getY(), loc.getZ());
        } catch (InvalidKeyException ignored) {
            // an invalid sound key in config must never break shooting
        }
    }
}
