# FaceSwapApp - Architettura aggiornata

## Overview

FaceSwapApp è un’app Android moderna sviluppata in Kotlin con Jetpack Compose e Kotlin DSL.  
Le sue funzionalità principali sono:
- Selezione immagini da galleria o fotocamera
- Rilevamento volti (MediaPipe)
- Face swap tra volti tramite OpenCV (triangolazione Delaunay, seamless cloning)
- Overlay di sticker dinamici (occhiali, cappelli, baffi, ecc.) posizionati tramite landmark facciali
- Editor avanzato: crop, rotazione, filtri, cambio sfondo (segmentazione MLKit), rimozione oggetti (inpainting locale o AI)
- Salvataggio in galleria

---

## Struttura dei Moduli

- **MainActivity**: entry point, gestione permessi, scelta immagine, lancio editor
- **FaceSwapScreen**: UI principale per rilevamento facce, face swap, sticker, preview, debug
- **PhotoEditorActivity**: activity che ospita la schermata di editing avanzato
- **PhotoEditorScreen**: editor avanzato (crop, filtri, cambio sfondo, rimozione oggetto, salvataggio)
- **Componenti UI**:  
    - MovableCropBox: box di crop interattivo  
    - PhotoFilterScreen: UI filtri  
    - StickerOverlay: overlay sticker su volto  
    - FaceButtonsBarWithPreview: selezione faccia  
    - StickerPicker: selezione sticker  
    - BrushMaskOverlay: maschera per inpainting
    - PhotoEditorToolbar: toolbar comandi editor  
- **Utility**:  
    - ImageUtils: caricamento/salvataggio bitmap, landmark, segmentazione, cropping, compositing  
    - FaceSwapUtils: logica face swap OpenCV, triangolazione, blending, debug  
    - OpenCVHelper: inizializzazione e inpainting OpenCV  
    - Sticker.kt: definizione tipi, dati e lista sticker disponibili  
- **Tema e Stile**:  
    - Theme.kt, Color.kt, Shape.kt, Type.kt  
    - Supporto chiaro/scuro, tipografia, colorscheme, shape personalizzate

---

## Flusso Principale

1. MainActivity avvia FaceSwapScreen.
2. L’utente seleziona/scatta un’immagine.
3. FaceSwapScreen rileva i volti e permette swap, sticker, salvataggio, editor avanzato.
4. L’editor avanzato consente crop, rotazione, filtri, cambio sfondo (segmentazione MLKit), rimozione oggetto (inpainting), salvataggio.
5. Tutte le funzioni di image processing sono gestite nei moduli utility.
6. Interfaccia completamente Compose, UI reattiva e moderna.

---

## Integrazioni & Dipendenze

- **OpenCV**: native libs per morphing, inpainting, blending
- **MediaPipe**: rilevamento 468 landmark facciali
- **MLKit**: segmentazione persona per cambio sfondo
- **OkHttp**: chiamate HTTP per API esterne (lama-cleaner, HuggingFace)
- **Compose**: UI principale, custom component, tema
- **Android MediaStore**: salvataggio immagini

---

## Permessi e Manifest

- Camera, Read/Write External Storage (opzionale per Android 10+), gestione intent per immagini e fotocamera.

---

## Salvataggio & Debug

- Immagini di debug step-by-step (face swap, blending, maschere) salvate su storage interno per troubleshooting avanzato.
- Log file testuale per debugging delle operazioni di morphing.

---

## Estendibilità

- Modulare: nuovi sticker, filtri, backend AI possono essere aggiunti facilmente.
- Tutta la logica di image processing è separata dalla UI.
- UI Compose temabile e reattiva.

---
