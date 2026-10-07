# NoE API

Пакет `ua.noe.api`. Додайте в `paper-plugin.yml` вашого плагіна:

```yaml
dependencies:
  server:
    NoE:
      load: BEFORE
      required: true
```

(або `softdepend` у `plugin.yml`, якщо не використовуєте paper-plugin.yml.) Скомпілюйте проти `NoE-<версія>.jar` як `compileOnly`.

## NoEAPI

| Метод | Результат |
|---|---|
| `isAvailable()` | чи увімкнений NoE |
| `getItem(String id)` | `NoEItem` або `null` |
| `getItem(ItemStack)` | визначення предмета або `null` |
| `createItem(String id [, int amount])` | `ItemStack`; викликає `NoEItemCreateEvent`; `null` для невідомого id чи скасування |
| `isNoEItem(ItemStack)` | чи є тег `noe:item_id` |
| `getItemId(ItemStack)` | id або `null` |
| `getWeapon / getArmor / getBlock / getFood(String id)` | відповідний тип або `null` |
| `getCustomBlock(Block)` | визначення кастомного блока у світі або `null` |
| `getArmorProtection(Player, ProtectionType)` | сума захисту (`MELEE`/`FIREARM`) надітої NoE-броні |
| `getItemIds()` | усі id, відсортовані |

Методи викликайте з головного потоку. Якщо NoE вимкнений, вони кидають `IllegalStateException`.

## Моделі даних

- `NoEDefinition` — інтерфейс: `getId()`, `getCategory()`, `getMaterial()`.
- `NoEItem` — база. Підкласи: `NoEWeapon`, `NoEArmor`, `NoEBlock`, `NoEFood`.
- `NoEWeapon`: `getType()`, `getAmmo()`, `getMagazine()`, `getReloadTime()`, `getFireRate()`, `getRange()`, `getPellets()`, `getSpread()`, `getShotDamage()`.
- `NoEArmor`: `getSlot()`, `getMeleeProtection()`, `getFirearmProtection()`.
- `NoEBlock`: `getHardness()`, `getPlacedMaterial()`, `getDrops()`, `usesDisplay()`.

Усі визначення незмінні. Після `/noe reload` реєстр замінюється новим об'єктом — не кешуйте `NoEItem` надовго,
зберігайте id.

## ItemBuilder

`ua.noe.item.ItemBuilder` не прив'язаний до NoE-предметів і підходить для будь-яких стеків:

```java
ItemStack stack = ItemBuilder.of(Material.STICK)
        .name("&bWand")
        .lore(List.of("&7Magic"))
        .modelData(1)
        .unbreakable(true)
        .build();
```

## Події

Усі успадковують `NoECancellableEvent`; скасування зупиняє дію.

| Подія | Коли | Що робить `setCancelled(true)` |
|---|---|---|
| `NoEItemUseEvent` | клік з NoE-предметом | забороняє використання предмета |
| `NoEItemCreateEvent` | видача/створення | скасовує створення; можна замінити `ItemStack` |
| `NoEItemBreakEvent` | предмет ось-ось зламається | запобігає втраті міцності |
| `NoEWeaponShootEvent` | перед пострілом | скасовує постріл |
| `NoEWeaponReloadEvent` | початок перезарядки | скасовує перезарядку |
| `NoEArmorEquipEvent` | броню вдягнено | повертає частину в інвентар |
| `NoECustomBlockPlaceEvent` | гравець ставить кастомний блок | скасовує встановлення |
| `NoECustomBlockBreakEvent` | блок ламається (гравець `null` при вибуху) | скасовує ламання |
| `NoECustomBlockInteractEvent` | ПКМ по кастомному блоку | блокує взаємодію |

```java
@EventHandler
public void onShoot(NoEWeaponShootEvent e) {
    if (isSafeZone(e.getPlayer().getLocation())) e.setCancelled(true);
}
```
