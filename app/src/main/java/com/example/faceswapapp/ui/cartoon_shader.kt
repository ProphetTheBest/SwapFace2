package com.example.faceswapapp.ui

val CARTOON_SHADER = """
precision mediump float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform float textureWidth;
uniform float textureHeight;

void main() {
    float dx = 1.0 / textureWidth;
    float dy = 1.0 / textureHeight;

    // Campionamento del colore e quantizzazione
    float levels = 4.0; // Più alto = più colori, meno effetto posterizzazione
    vec3 color = texture2D(inputImageTexture, textureCoordinate).rgb;
    color = floor(color * levels) / levels;

    // Edge detection morbida sui 4 vicini
    float edge = 0.0;
    edge += length(texture2D(inputImageTexture, textureCoordinate + vec2(-dx, 0.0)).rgb - color);
    edge += length(texture2D(inputImageTexture, textureCoordinate + vec2(dx, 0.0)).rgb - color);
    edge += length(texture2D(inputImageTexture, textureCoordinate + vec2(0.0, -dy)).rgb - color);
    edge += length(texture2D(inputImageTexture, textureCoordinate + vec2(0.0, dy)).rgb - color);

    // Soglia più alta per evitare bordi ovunque
    if (edge > 1.4)
        color = vec3(0.0);

    // Aumento leggero saturazione per colori più cartoon
    float average = (color.r + color.g + color.b) / 3.0;
    color = mix(vec3(average), color, 1.25); // 1.0 = originale, >1 più satura

    gl_FragColor = vec4(color, 1.0);
}
""".trimIndent()