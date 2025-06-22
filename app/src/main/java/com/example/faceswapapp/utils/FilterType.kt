package com.example.faceswapapp.utils

enum class FilterType(val label: String, val description: String, val hasParameter: Boolean = false) {
    //None("Normale", "Nessun filtro"),
    BlackWhite("B/N", "Trasforma la foto in bianco e nero"),
    Vintage("Vintage", "Dona un effetto retrò"),
    Saturation("Saturazione", "Regola l’intensità dei colori", true),
    Blur("Sfocatura", "Applica una sfocatura leggera", true),
    Sharpen("Nitidezza", "Aumenta la nitidezza", true),
    Cartoon("Cartoon", "Effetto cartoon (fumetto) OpenCV"),
    CartoonOpenGL("Cartoon", "Effetto cartoon (OpenGL)"),
    AnimeGAN("AnimeGAN", "Effetto anime tramite GAN"),
    Caricature("Caricatura", "Effetto caricatura OpenCV", true),
    // Parametri Cartoon — NON mostrerli come filtri selezionabili!
    CartoonBrightness("Luminosità cartoon", "Luminosità effetto cartoon", true),
    CartoonContrast("Contrasto cartoon", "Contrasto effetto cartoon", true),
    CartoonQuantLevels("Livelli quantizzazione cartoon", "Riduzione colori cartoon", true),
    CartoonBilateralSize("Bilateral cartoon", "Filtro bilateral cartoon", true),
    CartoonEdgeKernel("Kernel edge cartoon", "Kernel edge cartoon", true),
    CartoonUseCanny("Canny cartoon", "Contorno cartoon (boolean)", true);

    val showAsCheckbox: Boolean
        get() = when (this) {
            BlackWhite, Vintage, Saturation, Blur, Sharpen, Cartoon, CartoonOpenGL, AnimeGAN, Caricature -> true
            else -> false // Esclude i parametri cartoon
        }
}