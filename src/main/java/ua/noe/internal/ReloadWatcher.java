package ua.noe.internal;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Watches the NoE data folders and triggers one reload shortly after the last change.
 * One daemon thread, no polling; closed on disable.
 */
public final class ReloadWatcher implements AutoCloseable {

    private static final long DEBOUNCE_TICKS = 40L;

    private final Plugin plugin;
    private final Path root;
    private final List<String> watchedFolders;
    private final Runnable onChange;
    private final AtomicBoolean pending = new AtomicBoolean();
    private WatchService service;
    private Thread thread;

    public ReloadWatcher(Plugin plugin, Path root, List<String> watchedFolders, Runnable onChange) {
        this.plugin = plugin;
        this.root = root;
        this.watchedFolders = watchedFolders;
        this.onChange = onChange;
    }

    public void start() throws IOException {
        service = FileSystems.getDefault().newWatchService();
        for (String folder : watchedFolders) {
            registerTree(root.resolve(folder));
        }
        thread = new Thread(this::run, "NoE-Watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private void registerTree(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.filter(Files::isDirectory).toList()) {
                p.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
            }
        }
    }

    private void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = service.take();
                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path changed = dir.resolve((Path) event.context());
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
                        registerTree(changed);
                    }
                    schedule();
                }
                key.reset();
            }
        } catch (InterruptedException | ClosedWatchServiceException ignored) {
            // normal shutdown
        } catch (IOException ex) {
            plugin.getLogger().warning("Auto-reload watcher stopped: " + ex.getMessage());
        }
    }

    private void schedule() {
        if (pending.compareAndSet(false, true) && plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                pending.set(false);
                onChange.run();
            }, DEBOUNCE_TICKS);
        }
    }

    @Override
    public void close() {
        try {
            if (service != null) {
                service.close();
            }
        } catch (IOException ignored) {
            // nothing useful to do on shutdown
        }
        if (thread != null) {
            thread.interrupt();
        }
    }
}
