package com.example.farming;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import java.util.ArrayList;
import java.util.List;

public class SpriteLoader {
    /** Load frames from a sprite sheet (columns x rows) of frameWidth x frameHeight */
    public static Image[] loadFrames(String imagePath, int frameWidth, int frameHeight, int columns, int rows) {
        List<Image> frames = new ArrayList<>();
        try {
            String path = imagePath.startsWith("/") ? imagePath : "/" + imagePath;
            var stream = SpriteLoader.class.getResourceAsStream(path);
            if (stream == null) {
                System.err.println("❌ Sprite sheet not found at: " + path);
                return new Image[0];
            }
            Image sheet = new Image(stream);
            int sheetW = (int) sheet.getWidth();
            int sheetH = (int) sheet.getHeight();

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    int x = c * frameWidth;
                    int y = r * frameHeight;
                    if (x + frameWidth > sheetW || y + frameHeight > sheetH) continue;
                    frames.add(new WritableImage(sheet.getPixelReader(), x, y, frameWidth, frameHeight));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frames.toArray(new Image[0]);
    }
}
