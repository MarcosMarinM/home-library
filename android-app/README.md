# Libros Android App

Esta es una app Android nativa que **lee directamente la hoja de Google Sheets** y muestra un buscador de libros similar a la Shiny.

## Cómo configurar la hoja de datos

1. Abre `app/src/main/java/com/marcosmarin/libros/data/SheetRepository.kt`.
2. Cambia la constante `GOOGLE_SHEET_ID` por el ID de tu hoja de cálculo.

   Ejemplo de URL de Google Sheets:

   ```txt
   https://docs.google.com/spreadsheets/d/1Wf...XYZ/edit#gid=0
   ```

   El ID es la parte `1Wf...XYZ`.

## Cómo ejecutar

### 1) Abrir en Android Studio

1. Abre Android Studio.
2. Selecciona **Open** y elige la carpeta `android-app` de este repositorio.
3. Android Studio descargará dependencias y configurará el proyecto.

### 2) Configurar la hoja de datos (ya viene incluida)

El ID de la hoja ya está tomado del archivo `passwords.R`, por lo que no necesitas hacer nada extra. El código usa el mismo Google Sheet que la Shiny.

### 3) Ejecutar en emulador o dispositivo

- Selecciona un emulador Android o un dispositivo físico.
- Presiona **Run** (▶) o ejecuta:

```bash
./gradlew installDebug
```

### 3) Generar APK

El APK generado se encuentra en:

`android-app/app/build/outputs/apk/debug/app-debug.apk`

## Qué hace la app

- Descarga los datos de la hoja de cálculo (pestañas `libros` y `colores`).
- Muestra un buscador de títulos/autores/colores con filtrado instantáneo.
- Permite ver detalles de cada libro y un listado completo.
