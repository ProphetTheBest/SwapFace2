# FaceSwapApp - Flowchart Dettagliato (con note sui collegamenti funzione per funzione)

## Schema dei flussi principali

```mermaid
flowchart TD

    MainActivity["MainActivity"]
    PhotoEditorActivity["PhotoEditorActivity"]
    PhotoEditorScreen["PhotoEditorScreen"]
    FaceSwapScreen["FaceSwapScreen"]

    MainActivity --> |"Selezione immagine\ngalleria/fotocamera"| FaceSwapScreen
    MainActivity --> |"Apre editor avanzato"| PhotoEditorActivity
    PhotoEditorActivity --> PhotoEditorScreen

    subgraph FaceSwapScreen_Flow [FaceSwapScreen]
        SelImage["Seleziona Immagine"]
        CaricaBitmap["Carica Bitmap (ImageUtils.loadBitmapFromUri)"]
        DetectFaces["Rileva Volti (MediaPipe)"]
        ShowLandmarks["Visualizza Landmark"]
        FacePreview["Barra Preview Facce (FaceButtonsBarWithPreview)"]
        SelectFace["Seleziona Faccia"]
        DoFaceSwap["Face Swap (FaceSwapUtils.swapFaces)"]
        StickerOverlay["StickerOverlay"]
        StickerPicker["StickerPicker"]
        ModSticker["Modifica Sticker"]
        SaveGallery["Salva in Galleria (ImageUtils.saveToGallery)"]
        Debug["Debug/Log"]
        ToEditor["Apri PhotoEditorActivity"]

        SelImage --> CaricaBitmap --> DetectFaces --> ShowLandmarks --> FacePreview --> SelectFace
        SelectFace --> DoFaceSwap
        SelectFace --> StickerOverlay
        StickerOverlay --> StickerPicker --> ModSticker
        ModSticker --> SaveGallery
        SelectFace --> ToEditor
        SaveGallery --> Debug
    end

    FaceSwapScreen --> FaceSwapScreen_Flow

    subgraph PhotoEditorScreen_Flow [PhotoEditorScreen]
        ViewImg["Visualizza Immagine"]
        Toolbar["Toolbar (PhotoEditorToolbar)"]
        Crop["Crop (MovableCropBox)"]
        Rotate["Rotazione"]
        Filters["Filtri (PhotoFilterScreen)"]
        BgChange["Cambia Sfondo (MLKit)"]
        ObjRemove["Rimuovi Oggetto (OpenCVHelper.inpaintWithOpenCV / AI)"]
        SaveEd["Salva in Galleria (ImageUtils.saveToGallery)"]

        ViewImg --> Toolbar
        Toolbar --> Crop
        Toolbar --> Rotate
        Toolbar --> Filters
        Toolbar --> BgChange
        Toolbar --> ObjRemove
        Toolbar --> SaveEd
        BgChange --> |"Segmentazione MLKit"| BgChange
    end

    PhotoEditorScreen --> PhotoEditorScreen_Flow

    subgraph Utility_Modules ["Moduli Utility"]
        ImageUtils["ImageUtils\n(caricamento/salvataggio, segmentazione, crop, filtri, compositing)"]
        FaceSwapUtils["FaceSwapUtils\n(face swap, triangolazione, blending)"]
        OpenCVHelper["OpenCVHelper\n(inpainting, funzioni native OpenCV)"]
        StickerData["Sticker.kt\n(definizione sticker, dati sticker)"]
    end

    FaceSwapScreen_Flow --> ImageUtils
    FaceSwapScreen_Flow --> FaceSwapUtils
    FaceSwapScreen_Flow --> StickerData
    PhotoEditorScreen_Flow --> ImageUtils
    PhotoEditorScreen_Flow --> OpenCVHelper
    PhotoEditorScreen_Flow --> StickerData

    subgraph UI_Modules ["UI/Theme"]
        Theme["Theme, Color, Shape, Type"]
        ToolbarUI["PhotoEditorToolbar"]
        CropBoxUI["MovableCropBox"]
        StickerOv["StickerOverlay"]
        StickerPick["StickerPicker"]
        FaceBar["FaceButtonsBarWithPreview"]
        FilterScreen["PhotoFilterScreen"]
    end

    FaceSwapScreen_Flow --> FaceBar
    FaceSwapScreen_Flow --> StickerPick
    FaceSwapScreen_Flow --> StickerOv
    PhotoEditorScreen_Flow --> ToolbarUI
    PhotoEditorScreen_Flow --> CropBoxUI
    PhotoEditorScreen_Flow --> FilterScreen

    MainActivity --> |"Permessi: Storage, Camera"| Permessi["AndroidManifest.xml"]
```

---

## Collegamenti funzione per funzione

- **ImageUtils.loadBitmapFromUri**:  
  - Chiamata da: MainActivity, FaceSwapScreen, PhotoEditorScreen  
  - Scopo: Carica bitmap da galleria o fotocamera

- **FaceSwapUtils.swapFaces**:  
  - Chiamata da: FaceSwapScreen  
  - Scopo: Esegue triangolazione e blending tra due volti

- **ImageUtils.saveToGallery**:  
  - Chiamata da: FaceSwapScreen, PhotoEditorScreen  
  - Scopo: Salva la bitmap finale nella galleria

- **OpenCVHelper.inpaintWithOpenCV**:  
  - Chiamata da: PhotoEditorScreen (tramite ViewModel)  
  - Scopo: Rimuove oggetti tramite maschera (inpainting locale)

- **StickerOverlay / StickerPicker**:  
  - Chiamati da: FaceSwapScreen  
  - Scopo: Sovrapposizione e scelta sticker, posizionamento tramite landmark

- **MovableCropBox**:  
  - Chiamata da: PhotoEditorScreen  
  - Scopo: Crop interattivo dell’immagine

- **PhotoEditorToolbar**:  
  - Chiamata da: PhotoEditorScreen  
  - Scopo: Comandi editor avanzato

- **PhotoFilterScreen**:  
  - Chiamata da: PhotoEditorScreen  
  - Scopo: Applicazione filtri

---

## Note principali

- Tutte le funzioni di utility sono richiamate dai ViewModel o direttamente dai Composable, e hanno input/output chiaro (bitmap, dati sticker, maschere).
- Tutti i “flussi” tornano ai rispettivi schermi di preview o salvataggio.
- I permessi per camera/storage sono richiesti solo in MainActivity.

---