package ua.noe.food;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.noe.NoEPlugin;
import ua.noe.item.ItemData;
import ua.noe.item.NoEItem;
import ua.noe.util.Registries;

/** Applies configured effects when a NoE item is consumed. */
public final class FoodListener implements Listener {

    private final NoEPlugin plugin;

    public FoodListener(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        String id = plugin.identifier().getId(event.getItem());
        if (id == null) {
            return;
        }
        NoEItem def = plugin.factory().registry().get(id);
        if (def == null || def.getEffects().isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        for (ItemData.EffectSpec spec : def.getEffects()) {
            PotionEffectType type = Registries.effect(spec.type());
            if (type == null) {
                plugin.getLogger().warning("Item '" + id + "' uses unknown effect '" + spec.type() + "'");
                continue;
            }
            player.addPotionEffect(new PotionEffect(type, spec.duration(), spec.amplifier()));
        }
    }
}
