# Changelog

## 1.0.0

Перший реліз NoE (Next Object Engine).

- Кастомні предмети, зброя (firearm/bow/melee), броня, блоки, їжа з YAML-конфігурацією.
- Валідація конфігів: помилка в одному файлі не зупиняє сервер, у консоль йдуть файл, id, параметр, причина й порада.
- `noe:item_id` у PersistentDataContainer; `NoEAPI` для інших плагінів.
- Генерація resource pack (моделі, `custom_model_data`, `item_model`), SHA-1, вбудований HTTP-хост.
- Адмін-GUI `/noe menu` (сторінки, пошук, видача, інфо, reload).
- Команди `/noe give|list|info|reload|pack|menu` з tab completion та permissions.
- Події `NoEItemUseEvent`, `NoEItemCreateEvent`, `NoEItemBreakEvent`, `NoEWeaponShootEvent`,
  `NoEWeaponReloadEvent`, `NoEArmorEquipEvent`, `NoECustomBlockPlaceEvent`, `NoECustomBlockBreakEvent`,
  `NoECustomBlockInteractEvent`.
- Hot reload без перезапуску сервера; автоматичний reload при зміні файлів.
