# Progreso de Vida

Aplicación Android independiente para registrar y analizar progreso personal.

## Incluye

- Registro diario de horas en **Ejercicio, Música, Vida social y Trabajo**.
- Vista de progreso por **día, semana, mes y año**.
- Días activos, días sin actividad, racha, horas acumuladas, categoría dominante y comparación contra el periodo anterior.
- Diario de cada fecha: **qué hice, cómo me fue, cómo me sentí, ánimo 1–5 y foto del día**.
- Historial día por día y resumen mes por mes.
- **Metas, estudios, habilidades, proyectos y hábitos** personalizados.
- Meta de horas, objetivo, fecha límite, notas, estado y sesiones.
- Las sesiones pueden vincularse a Música, Ejercicio, Vida social o Trabajo para sumar las horas automáticamente al registro diario.

## Compilar

El workflow `Build Progreso de Vida Android` genera un APK debug y lo publica como artifact de GitHub Actions.

También puede compilarse con:

```bash
gradle :app:assembleDebug
```

APK resultante: `app/build/outputs/apk/debug/app-debug.apk`.
