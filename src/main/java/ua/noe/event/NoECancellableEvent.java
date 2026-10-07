package ua.noe.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/** Base class of all cancellable NoE events. */
public abstract class NoECancellableEvent extends Event implements Cancellable {

    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
