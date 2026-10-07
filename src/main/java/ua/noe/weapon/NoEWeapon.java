package ua.noe.weapon;

import ua.noe.item.ItemData;
import ua.noe.item.NoEItem;

/** A custom weapon: firearm, bow or melee. */
public class NoEWeapon extends NoEItem {

    private final WeaponType type;
    private final String ammo;
    private final int magazine;
    private final int reloadTime;
    private final int fireRate;
    private final double range;
    private final int pellets;
    private final double spread;
    private final double projectileSpeed;
    private final String shootSound;
    private final String reloadSound;
    private final String emptySound;

    public NoEWeapon(ItemData data, WeaponType type, String ammo, int magazine, int reloadTime, int fireRate,
                     double range, int pellets, double spread, double projectileSpeed,
                     String shootSound, String reloadSound, String emptySound) {
        super(data);
        this.type = type;
        this.ammo = ammo;
        this.magazine = magazine;
        this.reloadTime = reloadTime;
        this.fireRate = fireRate;
        this.range = range;
        this.pellets = pellets;
        this.spread = spread;
        this.projectileSpeed = projectileSpeed;
        this.shootSound = shootSound;
        this.reloadSound = reloadSound;
        this.emptySound = emptySound;
    }

    public WeaponType getType() {
        return type;
    }

    public boolean isRanged() {
        return type.isRanged();
    }

    /** NoE item id of the ammunition, or {@code null} if the weapon needs no ammo item. */
    public String getAmmo() {
        return ammo;
    }

    public int getMagazine() {
        return magazine;
    }

    /** Reload duration in ticks. */
    public int getReloadTime() {
        return reloadTime;
    }

    /** Minimum ticks between two shots. */
    public int getFireRate() {
        return fireRate;
    }

    public double getRange() {
        return range;
    }

    public int getPellets() {
        return pellets;
    }

    /** Random spread in degrees. */
    public double getSpread() {
        return spread;
    }

    public double getProjectileSpeed() {
        return projectileSpeed;
    }

    public String getShootSound() {
        return shootSound;
    }

    public String getReloadSound() {
        return reloadSound;
    }

    public String getEmptySound() {
        return emptySound;
    }

    /** Damage per shot (per pellet for shotguns). */
    public double getShotDamage() {
        Double d = getDamage();
        return d == null ? 1.0 : d;
    }
}
