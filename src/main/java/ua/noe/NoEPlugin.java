package ua.noe;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ua.noe.api.NoEAPI;
import ua.noe.armor.ArmorListener;
import ua.noe.armor.ArmorService;
import ua.noe.block.BlockListener;
import ua.noe.block.CustomBlockStore;
import ua.noe.command.NoECommand;
import ua.noe.config.ConfigLoader;
import ua.noe.config.ConfigReport;
import ua.noe.config.DirectoryLayout;
import ua.noe.config.MessageService;
import ua.noe.config.NoESettings;
import ua.noe.food.FoodListener;
import ua.noe.gui.AdminMenu;
import ua.noe.gui.MenuListener;
import ua.noe.internal.DamageContext;
import ua.noe.internal.ReloadWatcher;
import ua.noe.item.ItemCategory;
import ua.noe.item.ItemFactory;
import ua.noe.item.ItemIdentifier;
import ua.noe.item.ItemListener;
import ua.noe.registry.ItemRegistry;
import ua.noe.resourcepack.ResourcePackManager;
import ua.noe.util.Keys;
import ua.noe.weapon.WeaponService;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** NoE - Next Object Engine. Wires the modules together; all logic lives in the subpackages. */
public final class NoEPlugin extends JavaPlugin {

    /** Summary of one (re)load. */
    public record ReloadResult(int items, int weapons, int armor, int blocks, int food, int errors, int warnings) {
    }

    private Keys keys;
    private ItemIdentifier identifier;
    private ItemFactory factory;
    private DirectoryLayout layout;
    private ConfigLoader loader;
    private final MessageService messages = new MessageService();
    private volatile NoESettings settings;
    private final DamageContext damageContext = new DamageContext();
    private WeaponService weapons;
    private ArmorService armor;
    private CustomBlockStore blocks;
    private ResourcePackManager pack;
    private AdminMenu menu;
    private NoECommand command;
    private ReloadWatcher watcher;
    private CompletableFuture<ReloadResult> inFlight;

    @Override
    public void onEnable() {
        getLogger().info("NoE " + getPluginMeta().getVersion() + " starting...");

        keys = new Keys(this);
        identifier = new ItemIdentifier(keys.itemId);
        factory = new ItemFactory(keys);
        layout = new DirectoryLayout(getDataFolder().toPath());
        layout.install(getLogger());
        loader = new ConfigLoader(layout, m -> m.isItem(), m -> m.isBlock());

        saveDefaultConfig();
        loadSettingsAndMessages();

        weapons = new WeaponService(this);
        armor = new ArmorService(this);
        blocks = new CustomBlockStore(keys.blocks);
        pack = new ResourcePackManager(this);
        menu = new AdminMenu(this);
        command = new NoECommand(this);
        NoEAPI.init(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new ItemListener(this), this);
        pm.registerEvents(new ArmorListener(this), this);
        pm.registerEvents(new FoodListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(pack, this);
        registerCommand("noe", "NoE admin command", List.of(), command);

        ConfigReport report = new ConfigReport();
        ItemRegistry registry;
        try {
            registry = loader.load(report);
        } catch (RuntimeException ex) {
            report.error("plugins/NoE", null, null, "Unexpected error while loading: " + ex, "Report this stack trace to the NoE author");
            getLogger().log(java.util.logging.Level.SEVERE, "Loading failed", ex);
            registry = ItemRegistry.EMPTY;
        }
        applyRegistry(registry, report);

        startWatcher();
        getLogger().info("NoE " + getPluginMeta().getVersion() + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (watcher != null) {
            watcher.close();
            watcher = null;
        }
        if (weapons != null) {
            weapons.shutdown();
        }
        if (pack != null) {
            pack.shutdown();
        }
        if (blocks != null) {
            blocks.clear();
        }
        NoEAPI.shutdown();
    }

    // ------------------------------------------------------------------ loading

    private void loadSettingsAndMessages() {
        reloadConfig();
        settings = NoESettings.from(getConfig());

        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        try (Reader defaults = new InputStreamReader(getResource("messages.yml"), StandardCharsets.UTF_8)) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(defaults));
            cfg.options().copyDefaults(true);
        } catch (IOException | RuntimeException ex) {
            getLogger().warning("Could not read bundled messages.yml defaults: " + ex.getMessage());
        }
        messages.load(cfg);
    }

    /** Applies a freshly loaded registry. Main thread only. */
    private ReloadResult applyRegistry(ItemRegistry registry, ConfigReport report) {
        weapons.shutdown();
        factory.rebuild(registry, report);
        report.logTo(getLogger());

        ItemRegistry active = factory.registry();
        getLogger().info("Loaded " + active.count(ItemCategory.ITEM) + " items.");
        getLogger().info("Loaded " + active.count(ItemCategory.WEAPON) + " weapons.");
        getLogger().info("Loaded " + active.count(ItemCategory.ARMOR) + " armor pieces.");
        getLogger().info("Loaded " + active.count(ItemCategory.BLOCK) + " custom blocks.");
        getLogger().info("Loaded " + active.count(ItemCategory.FOOD) + " food items.");

        if (settings.pack().enabled() && settings.pack().autoGenerate()) {
            pack.regenerate();
        } else if (settings.pack().enabled()) {
            pack.loadCached();
        } else {
            getLogger().info("Resource Pack disabled in config.yml.");
        }
        return new ReloadResult(active.count(ItemCategory.ITEM), active.count(ItemCategory.WEAPON),
                active.count(ItemCategory.ARMOR), active.count(ItemCategory.BLOCK), active.count(ItemCategory.FOOD),
                report.errorCount(), report.warningCount());
    }

    /**
     * Reloads config, messages and every definition without a restart. Files are parsed off the main thread;
     * the new registry is swapped in on the main thread. Listeners are never re-registered.
     */
    public synchronized CompletableFuture<ReloadResult> reload() {
        if (inFlight != null && !inFlight.isDone()) {
            return inFlight;
        }
        CompletableFuture<ReloadResult> future = new CompletableFuture<>();
        inFlight = future;
        loadSettingsAndMessages();
        startWatcher();

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            ConfigReport report = new ConfigReport();
            ItemRegistry registry;
            try {
                registry = loader.load(report);
            } catch (RuntimeException ex) {
                report.error("plugins/NoE", null, null, "Unexpected error while loading: " + ex,
                        "Report this stack trace to the NoE author");
                registry = factory.registry(); // keep the previous definitions
            }
            ItemRegistry loaded = registry;
            if (!isEnabled()) {
                future.cancel(false);
                return;
            }
            getServer().getScheduler().runTask(this, () -> {
                try {
                    future.complete(applyRegistry(loaded, report));
                } catch (RuntimeException ex) {
                    future.completeExceptionally(ex);
                }
            });
        });
        return future;
    }

    private void startWatcher() {
        if (watcher != null) {
            watcher.close();
            watcher = null;
        }
        if (!settings.autoReload()) {
            return;
        }
        try {
            List<String> folders = java.util.Arrays.stream(ItemCategory.values()).map(ItemCategory::folder).collect(
                    java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            folders.add("resourcepack");
            watcher = new ReloadWatcher(this, layout.root(), folders, () -> {
                getLogger().info("Change detected, reloading...");
                reload();
            });
            watcher.start();
        } catch (IOException ex) {
            getLogger().warning("Auto-reload is unavailable: " + ex.getMessage()
                    + " | fix: set 'auto-reload: false' or use /noe reload");
        }
    }

    // ------------------------------------------------------------------ accessors

    public Keys keys() {
        return keys;
    }

    public ItemIdentifier identifier() {
        return identifier;
    }

    public ItemFactory factory() {
        return factory;
    }

    public DirectoryLayout layout() {
        return layout;
    }

    public MessageService messages() {
        return messages;
    }

    public NoESettings settings() {
        return settings;
    }

    public DamageContext damageContext() {
        return damageContext;
    }

    public WeaponService weapons() {
        return weapons;
    }

    public ArmorService armor() {
        return armor;
    }

    public CustomBlockStore blocks() {
        return blocks;
    }

    public ResourcePackManager pack() {
        return pack;
    }

    public AdminMenu menu() {
        return menu;
    }

    public NoECommand command() {
        return command;
    }
}
