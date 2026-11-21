# Rule34Mobile

Cliente Android no oficial para Rule34.xxx.
Esta aplicación es NSFW. Solo para mayores de 18 años.
No está afiliada ni respaldada por Rule34.xxx.

## Características

- Feed de posts con paginación y botón para volver arriba
- Búsqueda por tags con autocompletado; el cursor queda al final del tag elegido
- Favoritos:
  - Organiza cada post en una o varias carpetas a la vez
  - Invierte el orden de la lista con el botón ⇅ sin perder las prioridades
  - Crea carpetas nuevas desde el propio diálogo al guardar una imagen
  - Añade o quita carpetas desde el detalle manteniendo la selección actual
  - Vista limpia de rejilla, sin tarjetas
  - Perfiles compartidos y exportación/importación en archivos `.txt`
- Tags favoritos:
  - Guarda tags y abre sus resultados directamente
  - Guarda un **marcador** en el punto exacto de scroll
  - Reanuda la paginación desde la misma imagen y desplazamiento, sin cargar todo desde el principio
  - El botón de marcador aparece en la cabecera solo cuando hay un tag seleccionado
  - El marcador guardado se muestra en el menú hamburguesa solo dentro de ese tag
- Vista de imagen:
  - Pantalla completa con zoom por pinza y desplazamiento
  - Pellizco hacia afuera para entrar en pantalla completa y ampliar
  - Pellizco hacia adentro para reducir el zoom; suelta y haz un nuevo pellizco hacia adentro para salir
  - Los mismos gestos funcionan en imágenes, vídeos y GIF
  - Mantener pulsado con **un solo dedo** guarda en favoritos y abre el selector de carpetas
  - Doble toque para recargar un GIF o reiniciar un vídeo
  - Desliza hacia los lados para ver el post anterior o siguiente
  - Compartir y abrir el original en el navegador
  - Ajuste de máxima calidad en pantalla completa
- Metadata de posts organizada por categorías: Meta, Artist, Copyright y General
- Tema oscuro / claro y colores personalizables
- Ocultar contenido AI-generated y filtrar tags negativos

## Requisitos

- Android 8.0+
- API Key y User ID de Rule34.xxx (se configuran dentro de la app)

## Construcción

```bash
./gradlew assembleDebug
```

## Notas

- El filtro por rating se retiró porque Rule34.xxx eliminó la categoría Safe.
- Las credenciales se guardan cifradas en el dispositivo; no se envían ni se almacenan fuera de la app.
