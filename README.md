# isFake

isFake es una aplicación Android para seleccionar una ubicación y publicarla como ubicación simulada mediante el proveedor de pruebas de ubicación de Android. La aplicación permite elegir coordenadas en un mapa o introducirlas manualmente, guardar ubicaciones favoritas y controlar la simulación desde una notificación persistente.

## Funciones

- Selección de una ubicación mediante un mapa basado en OpenStreetMap, o entrada manual de latitud y longitud.
- Inicio y detención de la simulación mediante un servicio en primer plano.
- Configuración del intervalo de actualización, la precisión y la altitud.
- Gestión local de ubicaciones favoritas.
- Notificación persistente con una acción para detener el servicio.
- Interfaz disponible en español e inglés según el idioma seleccionado para la aplicación en Android.

## Requisitos

- Android 12 o posterior (API 31+).
- JDK 17 para compilar el proyecto.
- Android SDK con la plataforma 36 instalada.
- Conexión a Internet para cargar los mosaicos del mapa.

## Configuración del dispositivo

Android requiere seleccionar explícitamente una aplicación de ubicación simulada:

1. Activa las Opciones para desarrolladores desde los ajustes del dispositivo. En la mayoría de los dispositivos, esto se habilita tocando siete veces el número de compilación en la sección de información del teléfono.
2. En Opciones para desarrolladores, abre **Seleccionar aplicación para ubicación simulada** y elige **isFake**. La ruta y el nombre de esta opción pueden variar según el fabricante.
3. Abre isFake, selecciona una ubicación en el mapa o introduce sus coordenadas y comienza la simulación.
4. Concede los permisos de ubicación y notificaciones solicitados por Android.

## Compilación

Desde la raíz del proyecto, ejecuta:

```bash
./gradlew :app:assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en un dispositivo conectado mediante ADB:

```bash
./gradlew :app:installDebug
```

## Tecnologías

- Kotlin 2.1.21 y Java 17.
- Jetpack Compose y Material 3.
- OSMDroid 6.1.20 para la visualización del mapa.
- Hilt para inyección de dependencias.
- AndroidX DataStore Preferences y Kotlin Coroutines/Flow para persistencia y estado.
- Android `LocationManager` y servicio en primer plano para la simulación.

La aplicación tiene `minSdk 31`, `targetSdk 35` y compila contra Android SDK 36. Las versiones de plugins y dependencias se mantienen en `gradle/libs.versions.toml`.

## Estructura

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/isfake/app/
│   ├── data/
│   │   ├── datastore/PreferencesDataStore.kt
│   │   ├── model/FakeLocation.kt
│   │   └── repository/LocationRepository.kt
│   ├── di/AppModule.kt
│   ├── service/MockLocationService.kt
│   ├── ui/
│   │   ├── components/
│   │   │   ├── BatteryOptimizationDialog.kt
│   │   │   ├── MapView.kt
│   │   │   └── SettingsDialog.kt
│   │   ├── screens/home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── IsFakeApp.kt
│   └── MainActivity.kt
└── res/
    ├── values/strings.xml
    ├── values-en/strings.xml
    └── xml/locales_config.xml
```
