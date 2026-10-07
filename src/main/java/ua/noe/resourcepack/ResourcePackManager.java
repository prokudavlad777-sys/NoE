package ua.noe.resourcepack;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ua.noe.NoEPlugin;
import ua.noe.config.NoESettings;
import ua.noe.util.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Generates, hosts and sends the resource pack. Heavy work runs off the main thread. */
public final class ResourcePackManager implements Listener {

    /** Outcome of one generation. {@code error} is null on success. */
    public record Outcome(PackGenerator.PackResult result, List<String> warnings, String error) {
        public boolean ok() {
            return error == null;
        }
    }

    private final NoEPlugin plugin;
    private PackHost host;
    private volatile PackGenerator.PackResult current;
    private volatile String url = "";
    private volatile UUID packId = UUID.nameUUIDFromBytes("noe-pack".getBytes());

    public ResourcePackManager(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    public PackGenerator.PackResult current() {
        return current;
    }

    public String url() {
        return url;
    }

    /** Regenerates the pack asynchronously and completes on the main thread. */
    public CompletableFuture<Outcome> regenerate() {
        CompletableFuture<Outcome> future = new CompletableFuture<>();
        NoESettings.PackSettings settings = plugin.settings().pack();
        if (!settings.enabled()) {
            future.complete(new Outcome(null, List.of(), "disabled"));
            return future;
        }
        var items = List.copyOf(plugin.factory().registry().all());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Outcome outcome;
            try {
                List<String> warnings = new ArrayList<>();
                PackGenerator generator = new PackGenerator(m -> m.isBlock());
                var result = generator.generate(items, plugin.layout().resourcePackDir(), plugin.layout().packFile(),
                        settings, warnings);
                outcome = new Outcome(result, warnings, null);
            } catch (IOException | RuntimeException ex) {
                outcome = new Outcome(null, List.of(), ex.getMessage() == null ? ex.toString() : ex.getMessage());
            }
            Outcome finalOutcome = outcome;
            Bukkit.getScheduler().runTask(plugin, () -> {
                apply(finalOutcome, settings);
                future.complete(finalOutcome);
            });
        });
        return future;
    }

    /** Uses an existing cached pack (when auto-generate is off) without rebuilding it. */
    public void loadCached() {
        NoESettings.PackSettings settings = plugin.settings().pack();
        java.nio.file.Path file = plugin.layout().packFile();
        if (!settings.enabled() || !java.nio.file.Files.isRegularFile(file)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                byte[] data = java.nio.file.Files.readAllBytes(file);
                var result = new PackGenerator.PackResult(file, PackGenerator.sha1(data), data.length, 0);
                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTask(plugin, () -> apply(new Outcome(result, List.of(), null), settings));
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not read the cached resource pack: " + ex.getMessage());
            }
        });
    }

    private void apply(Outcome outcome, NoESettings.PackSettings settings) {
        if (!outcome.ok()) {
            plugin.getLogger().severe("Resource pack generation failed: " + outcome.error()
                    + " | fix: check that plugins/NoE/resourcepack is readable and the disk is not full");
            return;
        }
        outcome.warnings().forEach(w -> plugin.getLogger().warning("Resource pack: " + w));
        boolean changed = current == null || !current.sha1().equals(outcome.result().sha1());
        current = outcome.result();
        ensureHost(settings);
        url = settings.url().isBlank() ? hostUrl(settings) : settings.url();
        plugin.getLogger().info("Resource Pack initialized (sha1 " + current.sha1() + ", " + current.size() + " bytes).");
        if (changed && settings.sendOnJoin()) {
            sendToAll();
        }
    }

    private void ensureHost(NoESettings.PackSettings s) {
        if (!s.hostEnabled() || !s.url().isBlank()) {
            stopHost();
            return;
        }
        try {
            if (host == null) {
                host = new PackHost(s.hostBind(), s.hostPort(), current.file());
            } else {
                host.setFile(current.file());
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not start the pack host on port " + s.hostPort() + ": " + ex.getMessage()
                    + " | fix: choose a free port in resource-pack.host.port");
        }
    }

    private static String hostUrl(NoESettings.PackSettings s) {
        return s.hostEnabled() ? "http://" + s.hostPublicAddress() + ":" + s.hostPort() + "/noe-pack.zip" : "";
    }

    public void stopHost() {
        if (host != null) {
            host.stop();
            host = null;
        }
    }

    public void shutdown() {
        stopHost();
    }

    /** @return number of players the pack was sent to */
    public int sendToAll() {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (send(p)) {
                count++;
            }
        }
        return count;
    }

    public boolean send(Player player) {
        PackGenerator.PackResult pack = current;
        NoESettings.PackSettings s = plugin.settings().pack();
        if (pack == null || url.isBlank() || !s.enabled()) {
            return false;
        }
        Component prompt = s.prompt().isBlank() ? null : Text.color(s.prompt());
        player.setResourcePack(packId, url, HexFormat.of().parseHex(pack.sha1()), prompt, s.required());
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.settings().pack().sendOnJoin()) {
            send(event.getPlayer());
        }
    }
}
