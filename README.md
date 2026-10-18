# loopclient — мод (Fabric 1.21.4)

Клиентская часть loopclient: HUD, ESP, хитбоксы, яркость, зум и ClickGUI.
Мод клиентский (`environment: client`), на сервер ничего не отправляет и ничего
не меняет в игровой логике — только рисует и подстраивает настройки клиента.

## Сборка

Двойной клик по **uild.bat** — он сам найдёт JDK 21 и Gradle (в ..\.tools или в PATH),
соберёт мод и скажет, куда положить .jar.

Вручную: нужен **JDK 21** и Gradle 8.10+.

```cmd
cd loopclient-mod
gradle build
```

Готовый `.jar` — `build/libs/loopclient-1.0.0.jar`.

В этом окружении JDK и Gradle уже лежат в `../.tools`:

```cmd
set JAVA_HOME=..\.tools\jdk21\jdk-21.0.12.1+1
..\.tools\gradle-8.12\bin\gradle.bat build
```

Первая сборка качает Minecraft 1.21.4, Yarn-маппинги и Fabric API (~500 МБ),
это нормально и делается один раз (кэш в `~/.gradle`).

## Установка

1. Соберите `.jar`.
2. Положите его в `%APPDATA%\.minecraft\mods` (или в папку своей сборки).
3. Нужен **Fabric Loader 0.16+** и **Fabric API** для 1.21.4.
4. Запустите игру — в правом верхнем углу появится `loopclient`.

## Управление

| Клавиша | Действие |
|---|---|
| **RightShift** | открыть / закрыть ClickGUI |
| **Esc** | закрыть меню |

В меню: вкладки категорий, клик по модулю — включить/выключить, панель справа —
настройки выбранного модуля (слайдеры тянутся кликом, переключатели — кликом).

## Модули

### HUD
| Модуль | Что делает | Настройки |
|---|---|---|
| `Watermark` | надпись клиента и FPS | FPS вкл/выкл, позиция слева/справа |
| `Coordinates` | координаты XYZ и направление | дробные значения |
| `FpsPing` | FPS и пинг до сервера | — |
| `ArmorHUD` | броня и процент прочности | — |
| `PotionHUD` | активные эффекты с таймером | — |
| `TotemCounter` | сколько тотемов в инвентаре | — |

### Визуал
| Модуль | Что делает | Настройки |
|---|---|---|
| `ESP` | коробки вокруг сущностей | дистанция, только игроки |
| `Hitboxes` | хитбоксы всех сущностей | дистанция |

### Игрок
| Модуль | Что делает | Настройки |
|---|---|---|
| `Fullbright` | полная яркость мира | уровень гаммы |
| `Zoom` | сужение FOV | целевой FOV |

## Конфиг

`config/loopclient.json` — состояние модулей и значения настроек. Сохраняется
при каждом переключении и при выходе из игры, читается на старте.

Пример:

```json
{
  "modules": {
    "Watermark": { "enabled": true, "settings": { "Показывать FPS": true, "Позиция": "слева" } },
    "ESP": { "enabled": true, "settings": { "Дистанция": 48.0, "Только игроки": true } }
  },
  "version": 1
}
```

## Как добавить модуль

```java
public static class MyModule extends Module {
    private final Setting range = num("Дистанция", 32, 8, 96, 1);

    public MyModule() {
        super("MyModule", Category.HUD);   // HUD | VISUAL | PLAYER | MISC
    }

    @Override
    public void onHud(DrawContext ctx) {
        Ui.text(ctx, "hello " + (int) range.value, 4, 4, Ui.WHITE);
    }
}
```

Зарегистрировать в `Modules.init()` — `add(new MyModule());`.
Хуки: `onEnable` / `onDisable` / `onTick` / `onHud(DrawContext)` / `onWorld(WorldRenderContext)`.

## Структура

```
loopclient-mod/
├─ build.gradle, settings.gradle, gradle.properties
└─ src/main/
   ├─ resources/fabric.mod.json
   └─ java/dev/loopclient/
      ├─ LoopClient.java        вход: клавиша, тики, HUD, мир, конфиг
      ├─ gui/ClickGui.java      меню
      ├─ module/Module.java     базовый модуль + настройки
      ├─ module/Modules.java    реестр и рассылка хуков
      ├─ modules/HudModules.java
      ├─ modules/PlayerModules.java
      ├─ modules/RenderModules.java
      └─ util/{Ui,Config}.java
```

## Честно о статусе

Сделано и собирается: ядро, ClickGUI, конфиг, 10 модулей из таблицы.

Из большого списка (Trails, JumpCircles, CustomSky, ChinaHat, TargetHUD,
Predictions, Kill Effects, друзья, конфиг-менеджер и т.д.) пока **не реализованы** —
это следующие итерации. Список всех функций и их описания лежит на сайте
(`site/public/assets/modules.js`), чтобы каталог и мод не расходились.

Что важно знать про «только визуал»: ESP и хитбоксы — это рендер, они не дают
преимущества в бою и не меняют отправляемые пакеты. Всё, что меняет геймплей
(авто-спринт, fast-exp, aim), сознательно не делается.
