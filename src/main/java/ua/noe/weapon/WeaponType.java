package ua.noe.weapon;

/** Supported weapon archetypes. */
public enum WeaponType {
    FIREARM(true),
    PISTOL(true),
    REVOLVER(true),
    SHOTGUN(true),
    RIFLE(true),
    BOW(true),
    MELEE(false);

    private final boolean ranged;

    WeaponType(boolean ranged) {
        this.ranged = ranged;
    }

    public boolean isRanged() {
        return ranged;
    }
}
