package com.cricket.engine;

import javafx.scene.Scene;
import javafx.scene.text.Font;

/**
 * Loads JetBrains Mono font and global CSS into any scene.
 * Call FontLoader.apply(scene) in every screen's show() method.
 */
public class FontLoader {

    private static boolean fontLoaded = false;

    public static void loadFont() {
        if (!fontLoaded) {
            Font.loadFont(FontLoader.class.getResourceAsStream(
                    "/fonts/JetBrainsMono-Regular.ttf"), 12);
            Font.loadFont(FontLoader.class.getResourceAsStream(
                    "/fonts/JetBrainsMono-Bold.ttf"), 12);
            fontLoaded = true;
        }
    }

    public static void apply(Scene scene) {
        loadFont();
        var css = FontLoader.class.getResource("/global.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}