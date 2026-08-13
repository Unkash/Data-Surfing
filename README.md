# 🏄 Surf Málaga

App Android para consultar condiciones de surf en Guadalmar (Málaga) con alertas configurables.

## Datos

- **Fuente**: [Open-Meteo](https://open-meteo.com) — gratuito, sin API key, actualizado cada 6h
- **Parámetros**: altura de ola, período, dirección, swell, viento (velocidad, rachas, dirección)
- **Previsión**: gráfica de 48 horas
- **Coordenadas**: Guadalmar, Málaga (36.68°N, 4.51°O)

---

## Cómo obtener el APK sin instalar nada

### 1. Crear cuenta en GitHub (si no tienes)
Ve a https://github.com y crea una cuenta gratuita.

### 2. Crear repositorio nuevo
- Botón **New repository**
- Nombre: `surf-malaga`
- Visibilidad: **Private** (recomendado) o Public
- **No** marques "Add README" (ya viene incluido)
- Clic en **Create repository**

### 3. Subir el código
En la página del repo recién creado verás instrucciones. Desde tu máquina:

```bash
cd /ruta/donde/descomprimiste/surf-malaga
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/surf-malaga.git
git push -u origin main
```

### 4. Esperar la compilación automática
- Ve a la pestaña **Actions** de tu repositorio en GitHub
- Verás un workflow llamado **Build APK** ejecutándose (tarda 3-5 minutos)
- Cuando termine (✅ verde), haz clic en él

### 5. Descargar el APK
- En la página del workflow completado, busca la sección **Artifacts**
- Descarga **SurfMalaga-debug**
- Descomprime el .zip → obtendrás `app-debug.apk`

### 6. Instalar en Android
- Copia el APK a tu móvil (por cable, Telegram, Drive…)
- En Ajustes → Seguridad → activa **Instalar apps de origen desconocido** para tu gestor de archivos
- Abre el APK y sigue el proceso de instalación

---

## Compilar en local (opcional)

Si tienes Android Studio instalado:

```bash
./gradlew assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk
```

---

## Configurar alertas

1. En la app, menú superior → **Alertas**
2. Activa el switch principal
3. Configura los umbrales que quieras:
   - **Olas**: altura mínima/máxima y período mínimo
   - **Viento**: velocidad mínima/máxima
   - **Swell**: altura mínima
   - **Dirección de viento**: rango en grados (offshore Málaga ≈ 270-340°)
4. Define cada cuántas horas comprobar (mínimo 1h por limitaciones de Android)
5. Guarda — la app comprobará en background y te notificará si se cumplen las condiciones

---

## Estructura del proyecto

```
surf-malaga/
├── app/src/main/
│   ├── java/es/unkash/surfmalaga/
│   │   ├── data/
│   │   │   ├── SurfData.java          # Modelo de datos
│   │   │   ├── OpenMeteoRepository.java # Llamadas a la API
│   │   │   ├── AlertConfig.java       # Configuración de alertas
│   │   │   └── AlertStorage.java      # Persistencia de alertas
│   │   ├── notifications/
│   │   │   ├── SurfCheckWorker.java   # Worker periódico
│   │   │   └── BootReceiver.java      # Reanudar tras reinicio
│   │   ├── ui/
│   │   │   ├── MainActivity.java      # Pantalla principal
│   │   │   └── AlertsActivity.java    # Configuración de alertas
│   │   └── utils/
│   │       └── WorkerScheduler.java   # Gestión de WorkManager
│   └── res/
├── .github/workflows/build.yml        # CI/CD → genera el APK
└── README.md
```

---

## Limitaciones conocidas

- WorkManager en Android no garantiza exactitud horaria (puede variar ±15 min)
- En algunos fabricantes (Xiaomi, Huawei) hay que desactivar la optimización de batería para la app
- Los datos de Open-Meteo son de modelo numérico, no de boya local — pueden diferir de las condiciones reales

---

## Actualizar la app

Modifica el código, haz `git push` y GitHub Actions compilará un nuevo APK automáticamente.
