# Довідник конфігурації

## Спільні поля (усі категорії)

| Поле | Тип | Опис |
|---|---|---|
| `id` | string | унікальний id: `a-z`, `0-9`, `_`, `-`, до 64 символів. Якщо не вказано, береться ключ секції або ім'я файлу |
| `material` | string | **обов'язково**, Bukkit-матеріал предмета |
| `name` | string | назва, `&`-кольори |
| `lore` | list | рядки опису |
| `model-data` | int ≥ 0 | `custom_model_data` (перший float) |
| `item-model` | `ns:path` | компонент `item_model` |
| `durability` | int | максимальна міцність |
| `damage` | number | шкода (мілі: атрибут `attack_damage`; зброя дальнього бою: шкода пострілу) |
| `unbreakable` | bool | |
| `enchants` | map | `sharpness: 2` (ключ ванільного зачарування) |
| `flags` | list | `ItemFlag`, напр. `HIDE_ATTRIBUTES` |
| `attributes` | map | див. нижче |
| `food` | map | `nutrition`, `saturation`, `can-always-eat` |
| `effects` | map | ефекти при споживанні: `<effect>: {duration, amplifier}` |
| `consume-seconds` | number | час споживання |
| `equipment` | map | `slot` (HEAD/CHEST/LEGS/FEET), `model` (`ns:path` equipment asset) |

### attributes

```yaml
attributes:
  movement_speed:
    amount: 0.02
    operation: ADD_NUMBER      # ADD_NUMBER | ADD_SCALAR | MULTIPLY_SCALAR_1
    slot: mainhand             # any, mainhand, offhand, hand, head, chest, legs, feet, armor, body
```

## weapons/

`type`: `FIREARM`, `PISTOL`, `REVOLVER`, `SHOTGUN`, `RIFLE`, `BOW`, `MELEE`.

| Поле | За замовчуванням | Опис |
|---|---|---|
| `ammo` | — | id NoE-предмета-патрона; без поля — нескінченний запас |
| `magazine` | 6 | ємність магазина |
| `reload-time` | 40 | тіки |
| `fire-rate` | 10 | мін. тіків між пострілами |
| `range` | 60 | дальність hitscan |
| `pellets` | 1 | кількість дробин |
| `spread` | 0 | розкид, градуси |
| `projectile-speed` | 3.0 | швидкість стріли для `BOW` |
| `sounds.shoot/reload/empty` | ваніль | ключі звуків |

`BOW` випускає справжню стрілу; решта типів — миттєвий постріл (raytrace). `MELEE` не стріляє: його шкода
задається атрибутом.

## armor/

`slot` обов'язковий; `melee-protection` і `firearm-protection` — 0.0-1.0.

## blocks/

`hardness`, `placed-material`, `display`, `display-scale`, `drops`, `interact.vanilla`, `interact.commands`
(див. README). Позиції кастомних блоків зберігаються в PDC чанка (`noe:blocks`). Вибухи та поршні
враховуються: блок дропається за `drops`, а поршні кастомні блоки не рухають.

## food/

Обов'язкова секція `food`. Ефекти застосовуються в `PlayerItemConsumeEvent`.

## config.yml

| Ключ | Опис |
|---|---|
| `auto-reload` | стежити за файлами й перезавантажувати самостійно |
| `armor.max-protection` | стеля сумарного захисту |
| `resource-pack.*` | див. [resource-pack.md](resource-pack.md) |
