# FaceSwapApp - Architettura aggiornata (con schema logico dettagliato delle funzioni)

## Overview e Moduli
Vedi inizio file.

---

## Schema logico delle principali classi/moduli e delle funzioni

### MainActivity
**Scopo:** Entry point, gestione permessi, scelta immagine, lancio schermate principali.

- **onCreate(savedInstanceState: Bundle?)**
  - Parametri: savedInstanceState (Bundle)
  - Chi la chiama: Sistema Android
  - Da chi è chiamata: N/A
  - Scopo: Setup UI, verifica permessi, avvia FaceSwapScreen o PhotoEditorActivity
- **checkAndRequestPermission()**
  - Parametri: nessuno
  - Chi la chiama: onCreate, azioni utente
  - Da chi è chiamata: MainActivity
  - Scopo: Chiede permessi runtime per storage/camera
- **onImageSelected(uri: Uri?)**
  - Parametri: uri (Uri)
  - Chi la chiama: callback scelta immagine/foto
  - Da chi è chiamata: MainActivity
  - Scopo: Avvia FaceSwapScreen con immagine selezionata

---

### FaceSwapScreen (Composable)
**Scopo:** UI principale per face detection, swap, sticker, preview.

- **FaceSwapScreen(...)**
  - Parametri: bitmap, onEdit, onSave, ecc.
  - Chi la chiama: MainActivity
  - Da chi è chiamata: Compose runtime
  - Scopo: Visualizza immagine, rileva volti, mostra pulsanti swap/sticker/editor
- **detectFaces(bitmap: Bitmap): List<FaceLandmarks>**
  - Parametri: bitmap
  - Chi la chiama: FaceSwapScreen (on image load/change)
  - Da chi è chiamata: ImageUtils/FaceSwapUtils
  - Scopo: Rileva 468 landmark via MediaPipe
- **onSwapFaces()**
  - Parametri: nessuno
  - Chi la chiama: utente (button)
  - Da chi è chiamata: FaceSwapScreen
  - Scopo: Esegue swap tra due volti selezionati, aggiorna preview

---

### PhotoEditorActivity & PhotoEditorScreen
**Scopo:** Schermata di editing avanzato.

- **PhotoEditorActivity**
  - Avvia PhotoEditorScreen con bitmap ricevuto
- **PhotoEditorScreen(...)**
  - Parametri: bitmap, callback onSave, ecc.
  - Chi la chiama: PhotoEditorActivity
  - Da chi è chiamata: Compose runtime
  - Scopo: UI per crop, filtri, brush, sfondo, rimozione oggetto

---

### Componenti UI principali

- **MovableCropBox**
  - Parametri: boxRect, onBoxChanged
  - Chi la chiama: PhotoEditorScreen
  - Scopo: Gestisce crop interattivo via touch
- **StickerOverlay**
  - Parametri: lista sticker, landmark facciali
  - Chi la chiama: FaceSwapScreen
  - Scopo: Overlay sticker su volto in base ai landmark
- **BrushMaskOverlay**
  - Parametri: imageBitmap, brushPathList, onPathAdded, brushSize
  - Chi la chiama: PhotoEditorScreen
  - Scopo: Permette disegno maschere per inpainting

---

### Utility

#### ImageUtils
**Scopo:** Funzioni di caricamento/salvataggio bitmap, filtri, segmentazione, crop, compositing.

- **loadBitmapFromUri(context, uri): Bitmap**
  - Parametri: context, uri
  - Chi la chiama: MainActivity, PhotoEditorViewModel
  - Scopo: Decodifica bitmap da file/uri
- **saveToGallery(context, bitmap, callback)**
  - Parametri: context, bitmap, callback
  - Chi la chiama: PhotoEditorViewModel, FaceSwapScreen
  - Scopo: Salva bitmap su MediaStore

#### FaceSwapUtils
**Scopo:** Algoritmi di face swap OpenCV

- **swapFaces(bitmap, landmarksList): Bitmap**
  - Parametri: bitmap, landmarksList
  - Chi la chiama: FaceSwapScreen
  - Scopo: Esegue triangolazione e blending per scambio volto

#### OpenCVHelper
**Scopo:** Wrapper per funzioni native OpenCV

- **inpaintWithOpenCV(context, image, mask): Bitmap**
  - Parametri: context, image, mask
  - Chi la chiama: PhotoEditorViewModel
  - Scopo: Rimozione oggetti tramite inpainting locale

---

### ViewModel: PhotoEditorViewModel
**Scopo:** Gestione stato editor avanzato, logica reactive per UI Compose.

- **funzioni principali:**
  - loadImage(context, uri)
  - rotate()
  - enableCrop()/applyCrop()
  - showFilter()/onFilterApplied()
  - startSegmentPerson()
  - setBackgroundBitmap()
  - enableBrushRemove()/addBrushPath(…)/applyBrushRemove()
  - save(context)

Per ogni funzione:
- Parametri: vedi definizione
- Chi la chiama: UI Compose (PhotoEditorScreen)
- Da chi è chiamata: ViewModel/Compose runtime
- Scopo: Aggiorna stato, lancia coroutine, chiama utility

---

### BrushMaskOverlayHelper (utils)
**Scopo:** Genera bitmap maschera per inpainting da path brush

- **generateMaskBitmap(imageWidth, imageHeight, brushPathList, canvasSize, imageOffset, imageSize): Bitmap**
  - Parametri: dimensioni, lista tratti, scaling
  - Chi la chiama: PhotoEditorViewModel
  - Da chi è chiamata: PhotoEditorViewModel
  - Scopo: Crea bitmap binaria per maschera inpainting

---

## Come interagiscono i moduli

1. **MainActivity** → FaceSwapScreen → su azione utente → **PhotoEditorActivity** → PhotoEditorScreen
2. **PhotoEditorScreen** invoca metodi su **PhotoEditorViewModel** (stato via StateFlow)
3. **PhotoEditorViewModel** chiama funzioni utility (ImageUtils, OpenCVHelper, ecc.)
4. **UI Compose** aggiorna la schermata in base allo stato del ViewModel

---

## Note aggiuntive

- Tutte le funzioni di image processing sono testabili in isolamento (input/output bitmap).
- La UI è completamente Compose, ogni azione utente modifica lo stato (unidirezionale).
- Permessi e intent sono gestiti solo nei layer Android (Activity).

---

**Vedi anche flowchart.kt per lo schema a blocchi.**