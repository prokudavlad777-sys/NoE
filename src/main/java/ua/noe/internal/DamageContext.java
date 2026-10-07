package ua.noe.internal;

/**
 * Marks damage dealt by NoE firearms while it is being applied, so armor can tell bullets from melee hits.
 * Only touched from the main thread.
 */
public final class DamageContext {

    private boolean firearm;

    public boolean isFirearm() {
        return firearm;
    }

    public void setFirearm(boolean firearm) {
        this.firearm = firearm;
    }
}
