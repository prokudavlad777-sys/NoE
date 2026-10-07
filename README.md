# NoE — Next Object Engine

NoE — власний framework-плагін для **Paper 26.3** (Java 25), що додає кастомні предмети, зброю, броню,
блоки, їжу та генерацію resource pack. Увесь код написано з нуля; він не містить коду ItemsAdder, Oraxen
чи інших плагінів.

> **Ліцензія:** власна **NoE License 1.0** (див. [`LICENSE`](LICENSE)): вільне використання, зміна й
> поширення (у тому числі комерційне) із збереженням ліцензії та копірайту; заборонено продавати
> незмінений NoE як окремий продукт. Це не юридична консультація.

## Можливості

- Предмети, зброя (`FIREARM`, `PISTOL`, `REVOLVER`, `SHOTGUN`, `RIFLE`, `BOW`, `MELEE`), броня, блоки, їжа.
- Усе описується YAML-файлами; помилки в конфігах не валять сервер.
- `noe:item_id` у PDC кожного предмета; публічний `NoEAPI` та події.
- Resource pack: генерація моделей, SHA-1, вбудований HTTP-хост, відправка при вході.
- Адмін-GUI, tab completion, permissions, hot reload і автоматичний reload при зміні файлів.

## Встановлення

1. Потрібно: Paper 26.3, Java 25.
2. Збірка: `./gradlew build` → `build/libs/NoE-1.0.0.jar`.
3. Покладіть JAR у `plugins/` і запустіть сервер. Створиться `plugins/NoE/` з прикладами.

Залежностей від інших плагінів немає.

## Структура папок

```
plugins/NoE/
├── config.yml
├── messages.yml
├── items/       # звичайні предмети
├── weapons/     # зброя
├── armor/       # броня
├── blocks/      # кастомні блоки
├── food/        # їжа
├── resourcepack/  # ваші текстури/моделі (assets/noe/...)
└── cache/       # згенерований NoE-pack.zip
```

Категорія предмета визначається **папкою**, в якій лежить файл. У файлі може бути один предмет (з
`id:` зверху) або кілька (`<id>:` секціями).

## Перший предмет

`plugins/NoE/items/tiger_sword.yml`:

```yaml
id: tiger_sword
material: DIAMOND_SWORD
name: "&6Tiger Sword"
lore:
  - "&7Зроблений із матеріалів тигра."
model-data: 1001
durability: 500
damage: 12
enchants:
  sharpness: 2
```

Застосуйте: `/noe reload`, потім `/noe give <гравець> tiger_sword`.

Для предметів з `damage` NoE додає атрибути `attack_damage` (= `damage`) та `attack_speed` (-2.4, як у меча);
щоб змінити швидкість, додайте власний `attributes.attack_speed`.

## Команди

| Команда | Опис | Permission |
|---|---|---|
| `/noe` | довідка | — |
| `/noe give <гравець> <предмет> [кількість]` | видати предмет | `noe.give` |
| `/noe list [сторінка]` | список предметів | `noe.list` |
| `/noe info <предмет>` | характеристики | `noe.info` |
| `/noe reload` | перезавантажити все | `noe.reload` |
| `/noe pack [send [гравець]]` | згенерувати / надіслати pack | `noe.pack` |
| `/noe menu [пошук]` | адмін-GUI | `noe.menu` |

`noe.admin` дає всі права (за замовчуванням — оператори).

## Приклади YAML

### Зброя

```yaml
id: revolver
type: FIREARM          # FIREARM, PISTOL, REVOLVER, SHOTGUN, RIFLE, BOW, MELEE
material: IRON_HORSE_ARMOR
name: "&7Revolver"
damage: 25
ammo: pistol_ammo      # id NoE-предмета; без ammo — нескінченний запас
magazine: 6
reload-time: 40        # тіки
fire-rate: 12          # мін. тіків між пострілами
range: 60
pellets: 1             # для дробовика, напр. 8
spread: 0              # розкид у градусах
model-data: 2001
sounds:
  shoot: "minecraft:entity.firework_rocket.blast"
  reload: "minecraft:item.crossbow.loading_end"
```

ПКМ — постріл, **F** (обмін рук) — перезарядка. Патрони беруться з інвентаря. Стан магазина зберігається
в самому предметі (`noe:ammo`).

### Броня

```yaml
id: tiger_poncho
type: ARMOR
slot: CHEST            # HEAD, CHEST, LEGS, FEET
material: LEATHER_CHESTPLATE
melee-protection: 0.75     # частка зменшення шкоди 0.0-1.0
firearm-protection: 0.45
```

Захист складається по всіх надітих частинах і обмежується `armor.max-protection` (за замовчуванням 0.95).
Куля NoE-зброї вважається firearm-шкодою, удар моба/гравця — melee.

### Блок

```yaml
id: tropical_workbench
material: CRAFTING_TABLE
model-data: 4001
hardness: 3.0
placed-material: CRAFTING_TABLE   # який ванільний блок реально ставиться
display: false                    # true — поверх блока малюється ItemDisplay з моделлю
display-scale: 1.0
drops:
  - self                 # сам блок
  - tiger_fang:1-3       # NoE-предмет, кількість або діапазон
  - STICK:2              # ванільний матеріал
interact:
  vanilla: true          # false — заборонити ванільну дію ПКМ
  commands:              # консольні команди при ПКМ
    - "say %player% used the workbench"
```

### Їжа

```yaml
id: tropical_fruit
material: APPLE
name: "&aTropical Fruit"
food:
  nutrition: 6
  saturation: 1.2
effects:
  regeneration:
    duration: 100    # тіки
    amplifier: 0
```

### Інші властивості

`item-model`, `unbreakable`, `flags`, `attributes`, `consume-seconds`, `equipment` (слот і модель одягу) —
див. [`docs/configuration.md`](docs/configuration.md).

## Resource pack

Покладіть текстури в `plugins/NoE/resourcepack/`:

- `assets/noe/textures/item/<id>.png` — NoE сам згенерує модель `noe:item/<id>`;
- або власна модель `assets/noe/models/item/<id>.json` (пріоритет).

Для предмета з `model-data` NoE генерує `assets/minecraft/items/<material>.json` (`range_dispatch` по
`custom_model_data`); для `item-model` — `assets/<ns>/items/<path>.json`. Предмети без моделі пропускаються
з попередженням. Детально: [`docs/resource-pack.md`](docs/resource-pack.md).

Налаштування у `config.yml` (`resource-pack:` — `enabled`, `auto-generate`, `send-on-join`, `required`,
`url`, `host`). Гравцям потрібен URL: вкажіть свій `url` або увімкніть `host.enabled` і відкрийте порт.

## API для плагінів

```java
ItemStack sword = NoEAPI.createItem("tiger_sword");
NoEItem def = NoEAPI.getItem(itemStack);          // null, якщо не NoE-предмет
boolean ours = NoEAPI.isNoEItem(itemStack);
String id = NoEAPI.getItemId(itemStack);
NoEWeapon revolver = NoEAPI.getWeapon("revolver");
NoEArmor poncho = NoEAPI.getArmor("tiger_poncho");
double p = NoEAPI.getArmorProtection(player, ProtectionType.FIREARM);
```

Додайте `NoE` у `depend`/`softdepend` вашого плагіна. Події (`ua.noe.event.*`) можна скасовувати. Повний
опис — [`docs/api.md`](docs/api.md).

## Відомі обмеження

- Швидкість ламання блоків залишається ванільною для `placed-material`; `hardness` зберігається та
  доступний через API, але Paper API не дозволяє змінювати швидкість ламання без додаткових механізмів.
- Для матеріалів-блоків fallback-модель pack-а — `minecraft:block/<name>`; для предметів зі спеціальними
  моделями (щити, луки, фарбована шкіра) fallback може відрізнятися від ванільного вигляду.
- Зброя працює з основної руки.

## Збірка та розробка

```
./gradlew build      # компіляція, тести, JAR
./gradlew test
```

Версію Paper API задано в `gradle.properties` (`paperApiVersion`). Можна зафіксувати точну збірку.
CI: `.github/workflows/build.yml`.
