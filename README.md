# BigHead Android App

## Требования
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17+
- Android SDK с API 31–36

## Сборка

### Через Android Studio:
1. Открыть папку `BigHeadApp` в Android Studio
2. Дождаться синхронизации Gradle
3. Нажать **Run ▶** или **Build → Generate Signed APK**

### Через командную строку:
```bash
cd BigHeadApp
./gradlew assembleDebug
# APK будет: app/build/outputs/apk/debug/app-debug.apk
```

> Если `local.properties` не настроен автоматически — укажи путь к SDK:
> `sdk.dir=/Users/<you>/Library/Android/sdk`  (macOS)
> `sdk.dir=/home/<you>/Android/Sdk`           (Linux)
> `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` (Windows)

---

## Архитектура

| Файл | Назначение |
|---|---|
| `MainActivity.kt` | Главный экран, логика панели, drag, переключение режимов |
| `CircleOverlayView.kt` | Кастомный View — светящаяся окружность DIGTER |
| `ParticleView.kt` | Анимированный фон (плавающие частицы) |
| `activity_main.xml` | Разметка — FrameLayout с перетаскиваемой группой |

## Функционал

- **Кнопка «ЗАПУСТИТЬ»** — появляется панель с overshoot-анимацией
- **Перетаскивание** — зажать панель и перемещать по экрану; эффект «подъёма» при нажатии
- **Переключатель BIGHEAD / DIGTER** — анимированное переключение
- **BIGHEAD** — ползунок HEAD SIZE появляется снизу панели
- **DIGTER** — в центре экрана появляется светящаяся окружность с кросс-хэйром; ползунок CIRCLE SIZE управляет её размером
- Все переходы — плавные (fade + scale + translate), работают на API 31–36
