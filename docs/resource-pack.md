# Resource pack

## Що генерує NoE

1. Копіює все з `plugins/NoE/resourcepack/` у zip (`cache/NoE-pack.zip`). Файли, що починаються з `.`, ігноруються.
2. Якщо немає `pack.mcmeta`, створює його з `min-format`/`max-format`/`description` з `config.yml`.
3. Для кожного предмета:
   - модель `assets/noe/models/item/<id>.json` береться ваша; якщо її немає, а є текстура
     `assets/noe/textures/item/<id>.png`, створюється `parent: minecraft:item/generated`;
   - `item-model: ns:path` → `assets/<ns>/items/<path>.json`;
   - `model-data: N` → `assets/minecraft/items/<material>.json` з `range_dispatch` по `custom_model_data`;
     кожен діапазон закривається ванільною моделлю, тому значення поза списком показують ванільний вигляд.
4. Рахує SHA-1. Zip детермінований: однаковий вміст дає однаковий хеш.

Якщо у вас уже є `assets/minecraft/items/<material>.json`, NoE його не перезаписує й попереджає.

## Версія формату

Номер формату pack залежить від версії Minecraft. NoE пише `min_format` і `max_format` з `config.yml`
(за замовчуванням 84-200, широкий діапазон, щоб не з'являлося попередження про несумісність). За потреби
вкажіть точні числа для 26.3 з офіційної вікі Minecraft.

## Роздача

- `resource-pack.url: "https://..."` — ваш хостинг; завантажте туди `cache/NoE-pack.zip`.
- `resource-pack.host.enabled: true` — вбудований HTTP-сервер (`/noe-pack.zip`). Потрібно відкрити
  `host.port` і вказати `host.public-address`.

`send-on-join` надсилає pack при вході; `required: true` робить його обов'язковим. Після зміни вмісту
онлайн-гравцям pack надсилається знову.

## Команди

`/noe pack` — перегенерувати й показати SHA-1; `/noe pack send [гравець]` — надіслати поточний.
