package com.example.faceswapapp.ui

val ANIME_SHADER = """
precision mediump float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform float textureWidth;
uniform float textureHeight;

void main() {
    float dx = 1.0 / textureWidth;
    float dy = 1.0 / textureHeight;

    // Palette anime: pochi colori (4 livelli)
    float levels = 4.0;
    vec3 color = texture2D(inputImageTexture, textureCoordinate).rgb;
    color = floor(color * levels) / levels;

    // Cel shading: aggiungi una finta ombra
    float light = dot(color, vec3(0.299, 0.587, 0.114));
    if (light < 0.5) color *= 0.85;

    // Edge detection solo sulla luminanza per effetto più “disegnato”
    float centerLum = dot(texture2D(inputImageTexture, textureCoordinate).rgb, vec3(0.299, 0.587, 0.114));
    float leftLum = dot(texture2D(inputImageTexture, textureCoordinate + vec2(-dx, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
    float rightLum = dot(texture2D(inputImageTexture, textureCoordinate + vec2(dx, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
    float upLum = dot(texture2D(inputImageTexture, textureCoordinate + vec2(0.0, -dy)).rgb, vec3(0.299, 0.587, 0.114));
    float downLum = dot(texture2D(inputImageTexture, textureCoordinate + vec2(0.0, dy)).rgb, vec3(0.299, 0.587, 0.114));

    float edge = 0.0;
    edge += abs(leftLum - centerLum);
    edge += abs(rightLum - centerLum);
    edge += abs(upLum - centerLum);
    edge += abs(downLum - centerLum);

    if (edge > 0.15) {
        color = vec3(0.0); // Bordo nero
    } else {
        // Boost saturazione per look anime
        float avg = (color.r + color.g + color.b) / 3.0;
        color = mix(vec3(avg), color, 1.4);
    }

    gl_FragColor = vec4(color, 1.0);
}
""".trimIndent()