# SwapFaceProject - Documentazione Architetturale e Funzionale Dettagliata

---

## Indice

1. [Struttura del Progetto](#struttura-del-progetto)
2. [Responsabilità dei Moduli e Dipendenze](#responsabilità-moduli-dipendenze)
3. [Flussi Utente Principali](#flussi-utente-principali)
4. [Tabella delle Responsabilità/Interazioni](#tabella-responsabilità)
5. [Dettaglio File per File (e Funzione per Funzione)](#dettaglio-file-funzione)
6. [Debug, Logging e Policy di Aggiornamento](#debug-logging-policy)
7. [Estendibilità e Note Finali](#estendibilità-note)

---

## 1. Struttura del Progetto

```
FaceSwapApp2/
├── app/
│   ├── MainActivity.kt
│   ├── FaceSwapScreen.kt
│   ├── PhotoEditorActivity.kt
│   ├── PhotoEditorScreen.kt
│   ├── PhotoEditorToolbar.kt
│   ├── MovableCropBox.kt
│   ├── PhotoFilterScreen.kt
│   ├── ImageFilters.kt
│   ├── StickerOverlay.kt
│   ├── Sticker.kt
│   ├── StickerPicker.kt
│   ├── BrushMaskOverlay.kt
│   ├── FaceButtonsBarWithPreview.kt
│   ├── FaceLandmarkBox.kt
│   ├── ImageUtils.kt
│   ├── FaceSwapUtils.kt
│   ├── OpenCVHelper.kt
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   ├── Shape.kt
│   │   │   ├── Type.kt
│   ├── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── opencv/ (modulo libreria OpenCV)
└── ...
```

---

## 2. Responsabilità Moduli & Dipendenze

| File/Modulo            | Responsabilità Chiave                                                                                        | Dipendenze dirette                        |
|------------------------|-------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| MainActivity           | Entry point, scelta immagini, gestione permessi, lancio editor                                              | FaceSwapScreen, PhotoEditorActivity, ImageUtils, OpenCVHelper |
| FaceSwapScreen         | UI principale: selezione/scatto, rilevamento volti, face swap, sticker, salvataggio, debug                  | ImageUtils, FaceSwapUtils, StickerOverlay, StickerPicker, FaceButtonsBarWithPreview |
| PhotoEditorActivity    | Avvia l’editor avanzato                                                                                      | PhotoEditorScreen                         |
| PhotoEditorScreen      | Editing avanzato: crop, rotazione, filtri, cambio sfondo, rimozione oggetti                                 | MovableCropBox, PhotoFilterScreen, BrushMaskOverlay, ImageUtils, OpenCVHelper, FaceSwapUtils |
| PhotoEditorToolbar     | Toolbar azioni rapide editor                                                                                 | -                                         |
| MovableCropBox         | Crop box interattivo (drag/resizing)                                                                        | -                                         |
| PhotoFilterScreen      | UI filtri foto                                                                                              | ImageFilters                              |
| ImageFilters           | Applica filtri a bitmap (ColorMatrix)                                                                       | -                                         |
| StickerOverlay         | Overlay sticker su bitmap (con landmark)                                                                    | Sticker.kt                                |
| Sticker.kt             | Dati e tipi sticker                                                                                         | -                                         |
| StickerPicker          | UI scelta sticker                                                                                           | Sticker.kt                                |
| BrushMaskOverlay       | Canvas pennello per maschera inpainting                                                                     | -                                         |
| FaceButtonsBarWithPreview | Preview volti trovati                                                                                     | -                                         |
| FaceLandmarkBox        | Debug: mostra landmark facciali                                                                             | -                                         |
| ImageUtils             | Utility immagini: carica/salva bitmap, landmark, segmentazione, cropping, compositing, inpainting           | MediaPipe, MLKit, OpenCVHelper, Android MediaStore |
| FaceSwapUtils          | Logica swap facciale, triangolazione, blending, debug                                                       | OpenCV, ImageUtils                        |
| OpenCVHelper           | Inizializza OpenCV, inpainting nativo                                                                       | Libreria nativa OpenCV                    |
| Theme/Color/Shape/Type | Temi, colori, shape, typography                                                                            | -                                         |

---

## 3. Flussi Utente Principali

### Flusso "Face Swap" rapido

1. Avvio app (MainActivity)
2. Selezione/scatto immagine
3. Rilevamento automatico volti → preview volti → selezione volto
4. Face Swap: scelta seconda immagine, swap tramite triangolazione
5. Aggiunta/modifica sticker su volto (con landmark)
6. Salvataggio in galleria o passaggio a editor avanzato

### Flusso "Editor avanzato"

1. Passaggio a PhotoEditorActivity/Screen
2. Crop con crop box drag & resize
3. Ruota immagine
4. Applica filtri (preview live)
5. Cambia sfondo: segmentazione persona (MLKit), scelta nuovo sfondo, compositing
6. Rimozione oggetti: pennello, scelta backend (OpenCV locale, AI cloud), inpainting
7. Salva in galleria

### Flusso "Debug"

- Salvataggio immagini di debug (swap/intermedi)
- Visualizzazione immagini di debug
- Log avanzato per troubleshooting

---

## 4. Tabella Responsabilità/Interazioni

| Modulo               | Chi lo chiama?                 | Chi chiama?                      | Scambio dati/funzioni principali                                 |
|----------------------|-------------------------------|-----------------------------------|------------------------------------------------------------------|
| MainActivity         | Sistema Android                | FaceSwapScreen, PhotoEditorActivity | Passa Uri immagini, gestisce permessi                            |
| FaceSwapScreen       | MainActivity                   | ImageUtils, FaceSwapUtils, StickerOverlay, ... | Rileva volti, gestisce swap, sticker, salva, debug               |
| PhotoEditorScreen    | PhotoEditorActivity            | MovableCropBox, PhotoFilterScreen, BrushMaskOverlay, ... | Modifica bitmap, crop, filtri, compositing, inpainting           |
| ImageUtils           | FaceSwapScreen, PhotoEditorScreen, FaceSwapUtils | MediaPipe, MLKit, OpenCVHelper  | Carica bitmap, landmark, segmenta, crop, salva, inpaint          |
| FaceSwapUtils        | FaceSwapScreen, PhotoEditorScreen | OpenCV, ImageUtils                | Triangolazione, morphing, blending, debug                        |
| OpenCVHelper         | ImageUtils, FaceSwapUtils      | -                                 | Inizializza OpenCV, inpaint                                      |
| StickerOverlay       | FaceSwapScreen, PhotoEditorScreen | -                                 | Overlay grafico sticker                                          |
| ...                  | ...                            | ...                               | ...                                                              |

---

## 5. Dettaglio File/Funzione

### MainActivity.kt

- **onCreate**
    - Inizializza OpenCV (`OpenCVHelper.init(this)`)
    - Mostra FaceSwapScreen come contenuto principale
    - Gestisce dialog scelta tra galleria e fotocamera con launcher Compose
    - Su selezione/scatto, chiama `openPhotoEditor(imageUri)`
- **openPhotoEditor(Uri)**
    - Lancia PhotoEditorActivity passando la Uri dell’immagine tramite intent extra

---

### FaceSwapScreen.kt

- Stato Compose:
    - Gestisce bitmap corrente, landmark, sticker, faccia selezionata, dialog, errori, snackbar, preview, ecc.
- **mergeBitmapWithStickers**
    - Sovrappone gli sticker posizionati (con landmark) sulla bitmap di base, restituendo una nuova bitmap pronta per il salvataggio
- **addSticker/selectSticker/removeSticker/updateSelectedSticker/resetSelectedSticker**
    - Tutta la logica per aggiungere, selezionare, modificare, eliminare e resettare sticker su una faccia
- **fullReset/clearDebugImages**
    - Reset completo degli stati quando si cambia immagine o si vuole “ripartire da zero”. Cancella anche file di debug
- **UI**
    - Preview centrale della bitmap, barra sticker con Surface circolari, bottoni per tutte le azioni (editor, salva, selezione immagine), dialog per errori, tema e debug

---

### PhotoEditorActivity.kt

- **onCreate**
    - Riceve la Uri immagine dall’intent extra
    - Mostra `PhotoEditorScreen(Uri)` come contenuto Compose

---

### PhotoEditorScreen.kt

- Stato Compose avanzato:
    - Gestisce bitmap, stato crop, filtri, segmentazione persona e background, overlay pennello, mask, snackbar, dialog, sheet pennello, ecc.
- **MovableCropBox**
    - Crop interattivo tramite drag spigoli/centro, aggiorna bounding box e ridisegna in tempo reale
- **PhotoFilterScreen**
    - Preview e applicazione filtri tramite ColorMatrix (B/N, vintage, saturazione)
- **Cambio Sfondo**
    - Segmentazione persona (`ImageUtils.segmentPersonBitmap`) via MLKit: bitmap con alpha
    - Scelta nuovo sfondo (galleria/fotocamera)
    - Compositing (`ImageUtils.compositePersonOnBackground`)
- **Pennello Rimozione Oggetti**
    - Modalità pennello (BrushMaskOverlay): disegno maschera, gestione undo/redo/reset/applica
    - Scelta backend: OpenCV locale, Lama Cleaner, HuggingFace (API HTTP)
    - Invio bitmap+mask al backend, ricezione immagine risultante e aggiornamento stato
- **Toolbar**
    - Bottom app bar con ruota/crop/filtri/sfondo/rimozione/salva
- **Dialog**
    - Conferme, selezione sfondo, errori, avanzate per pennello (prompt, backend, dimensione)

---

### PhotoEditorToolbar.kt

- **Composable BottomAppBar**
    - IconButton per: Ruota, Crop, Filtri, Cambia Sfondo, Rimuovi Sfondo, Pennello, Salva

---

### MovableCropBox.kt

- **MovableCropBox**
    - Canvas con box mobile. Consente drag di spigoli e centro per ridimensionamento/spostamento
    - Aggiorna cropRect in tempo reale, disegna handles rossi visivi

---

### PhotoFilterScreen.kt & ImageFilters.kt

- **PhotoFilterScreen**
    - UI per preview e applicazione filtri, con preview live e slider saturazione
- **applyFilter**
    - Filtra bitmap tramite ColorMatrix: B/N (saturazione 0), vintage (toni caldi), saturazione custom

---

### StickerOverlay.kt

- **StickerOverlay**
    - Canvas/AndroidView che disegna sticker sopra bitmap, usando landmark (es: coordinate degli occhi per occhiali)
    - Matrice di trasformazione calcolata in `getStickerMatrixNoScale`
    - Supporta overlay dei landmark (debug visivo)

---

### Sticker.kt

- **StickerType**: Enum (HAT, GLASSES, MUSTACHE)
- **Sticker**: data class, definisce id, tipo, label, risorsa, anchor points
- **PlacedSticker**: data class, stato sticker piazzato (posizione, scala, selezione, rotazione)
- **availableStickers**: lista sticker disponibili per l’utente

---

### StickerPicker.kt

- Barra orizzontale di Surface circolari per selezionare sticker dal catalogo
- Gestisce evidenziazione sticker selezionato

---

### BrushMaskOverlay.kt

- **BrushMaskOverlay**
    - Canvas per disegnare maschera (path + thickness) sopra bitmap
    - Overlay verde trasparente sulle aree mascherate
    - Preview pennello (cerchio bianco, segue dito)
    - Callback dimensione canvas, offset zona immagine, ecc.
- **generateMaskBitmap**
    - Trasforma le path in bitmap di maschera allineata all’immagine reale (bianco su nero)
- **toAndroidPath**
    - Conversione path Compose in android.graphics.Path

---

### FaceButtonsBarWithPreview.kt

- Barra preview per selezionare la faccia su cui effettuare face swap
- Mostra anteprima bitmap per ogni volto rilevato

---

### FaceLandmarkBox.kt

- Composable di debug che mostra landmark facciali sopra la bitmap (punti rossi)

---

### ImageUtils.kt

- **loadBitmapFromUri**: Carica bitmap da una Uri, gestendo EXIF/orientamento
- **rotateBitmap**: Ruota bitmap di angolo specificato
- **loadLastDebugStep6Bitmap**: Carica ultima immagine blending finale (debug)
- **saveToGallery**: Salva bitmap nella galleria usando MediaStore
- **detectLandmarksForFace**: Usa MediaPipe per ottenere 468 landmark facciali (bitmap)
- **detectFaceLandmarksFromUri**: Rileva tutti i landmark (fino a 4 facce) da una immagine
- **cropFaceFromLandmarks**: Croppa il volto su bitmap usando landmark
- **compositePersonOnBackground**: Compositing bitmap persona segmentata su nuovo sfondo
- **segmentPersonBitmap**: MLKit SelfieSegmenter per bitmap alpha (persona su trasparente)
- **createImageUri**: Crea Uri temporanea per fotocamera
- **inpaintWithOpenCV**: Inpainting oggetto tramite OpenCV
- **saveDebugMaskToFiles/saveDebugInpaintResult**: Salva maschere e risultati debug

---

### FaceSwapUtils.kt

- **appendDebugLog**: Scrive log avanzato su file
- **saveBitmapDebug/saveMatDebug**: Salva immagini e matrici step intermedi per troubleshooting
- **findClosestIndex**: Trova indice punto più vicino (triangolazione)
- **calculateDelaunayTriangles**: Triangolazione Delaunay su landmark, ritorna triple di indici
- **swapFaceWithTriangles**: Esegue face swap: warping triangoli, maschera hull, seamlessClone, debug immagini/log
- **offsetsToPoints/opencvPointsToAndroid**: Conversioni coordinate

---

### OpenCVHelper.kt

- **init**: Carica libreria nativa OpenCV, verifica inizializzazione
- **inpaint**: Applica OpenCV Photo.inpaint su bitmap+maschera (Telea), gestisce conversioni e cleanup

---

### Theme/Color/Shape/Type

- **Theme.kt**: Definisce colorScheme light/dark, typography, shape per Material3
- **Color.kt/Shape.kt/Type.kt**: Palette colori custom, shape componenti, typography custom

---

## 6. Debug, Logging e Policy di Aggiornamento

- **Immagini di debug**: Ogni step swap/inpainting può essere salvato (step1, step2, ... step6_final_blended)
- **Log avanzati**: Log su file per ogni step critico di swap
- **Pulizia**: Funzioni per eliminare immagini di debug (evita saturazione storage)
- **Policy**: Aggiornare sempre i file markdown di architettura e flowchart dopo refactor o nuove feature

---

## 7. Estendibilità e Note Finali

- **Nuovi sticker**: Si aggiungono da Sticker.kt, con anchor points e risorsa PNG
- **Nuovi filtri**: Si implementano in ImageFilters.kt e aggiungono pulsanti in PhotoFilterScreen
- **Nuovi backend inpainting**: Basta estendere la logica in PhotoEditorScreen e BrushRemoveBottomSheetExtended
- **UI**: Composable temabili, struttura chiara, supporto per tema chiaro/scuro
- **Debug & Test**: Facilità di troubleshooting grazie a immagini e log dettagliati
- **Onboarding**: Con questa documentazione un nuovo sviluppatore può comprendere rapidamente i flussi e le responsabilità

---

Per domande o per approfondimenti su una funzione specifica, consulta questo file o chiedi direttamente!