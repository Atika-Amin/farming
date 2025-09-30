package com.example.farming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class TileMap {
    private static final int TILE_SIZE = 32;
    private static final int MAP_WIDTH = 40;
    private static final int MAP_HEIGHT = 30;

    // You previously used a SCALE for your own canvas; here we keep an internal canvas only if you want it
    private static final int DEFAULT_VIEWPORT_WIDTH = 15;
    private static final int DEFAULT_VIEWPORT_HEIGHT = 10;
    private static final int SCALE = 2;

    private int[][][] tileLayers;
    private final List<JsonNode> objectLayers = new ArrayList<>();
    private final Map<Integer, Image> tileImages = new HashMap<>();
    private final Canvas canvas;

    public TileMap(InputStream mapStream, InputStream tilesetStream) throws IOException {
        // internal canvas size (not strictly required, but you used it before)
        this.canvas = new Canvas(DEFAULT_VIEWPORT_WIDTH * TILE_SIZE * SCALE,
                DEFAULT_VIEWPORT_HEIGHT * TILE_SIZE * SCALE);
        loadMapData(mapStream);
        loadTileset(tilesetStream);
    }

    private void loadMapData(InputStream mapStream) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(mapStream);

        JsonNode layers = rootNode.get("layers");
        int tileLayerCount = 0;
        for (JsonNode layer : layers) {
            if ("tilelayer".equals(layer.get("type").asText())) {
                tileLayerCount++;
            }
        }

        tileLayers = new int[tileLayerCount][MAP_HEIGHT][MAP_WIDTH];
        int tileIndex = 0;
        for (JsonNode layer : layers) {
            String type = layer.get("type").asText();
            if ("tilelayer".equals(type)) {
                JsonNode layerData = layer.get("data");
                for (int i = 0; i < layerData.size(); i++) {
                    int row = i / MAP_WIDTH;
                    int col = i % MAP_WIDTH;
                    tileLayers[tileIndex][row][col] = layerData.get(i).asInt();
                }
                tileIndex++;
            } else if ("objectgroup".equals(type)) {
                objectLayers.add(layer);
            }
        }
    }

    private void loadTileset(InputStream tilesetStream) {
        Image tileset = new Image(tilesetStream);
        int tilesetColumns = (int) (tileset.getWidth() / TILE_SIZE);
        int tilesetRows = (int) (tileset.getHeight() / TILE_SIZE);
        PixelReader pr = tileset.getPixelReader();

        int tileID = 1; // First GID in your TMJ is 1, so this aligns
        for (int r = 0; r < tilesetRows; r++) {
            for (int c = 0; c < tilesetColumns; c++) {
                WritableImage tile = new WritableImage(pr, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                tileImages.put(tileID++, tile);
            }
        }
    }

    public void drawMap(GraphicsContext gc) {
        // Clear the *target* canvas (use its dimensions)
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        for (int layer = 0; layer < tileLayers.length; layer++) {
            for (int row = 0; row < MAP_HEIGHT; row++) {
                for (int col = 0; col < MAP_WIDTH; col++) {
                    int gid = tileLayers[layer][row][col];
                    Image img = tileImages.get(gid);
                    if (gid != 0 && img != null) {
                        gc.drawImage(img, col * TILE_SIZE, row * TILE_SIZE);
                    }
                    // If img == null, that GID doesn't exist in the tileset image → it won't draw (expected).
                }
            }
        }
    }

    /** Basic collision against object layer "Object Layer 1" and simple wall IDs example */
    public boolean isWalkable(double x, double y) {
        double pad = 3;
        double pw = TILE_SIZE - pad * 2;
        double ph = TILE_SIZE - pad * 2;

        if (x < 0 || x + pw >= MAP_WIDTH * TILE_SIZE || y < 0 || y + ph >= MAP_HEIGHT * TILE_SIZE) return false;

        int tileX = (int) (x / TILE_SIZE);
        int tileY = (int) (y / TILE_SIZE);

        // example wall IDs (replace with your real blocking IDs if needed)
        List<Integer> wallTileIDs = List.of(1, 2, 3);
        for (int layer = 0; layer < tileLayers.length; layer++) {
            int gid = tileLayers[layer][tileY][tileX];
            if (wallTileIDs.contains(gid)) return false;
        }

        JsonNode target = null;
        for (JsonNode objectLayer : objectLayers) {
            JsonNode nameNode = objectLayer.get("name");
            if (nameNode != null && "Object Layer 1".equals(nameNode.asText())) {
                target = objectLayer;
                break;
            }
        }
        if (target != null) {
            JsonNode objects = target.get("objects");
            if (objects != null) {
                for (JsonNode obj : objects) {
                    double ox = obj.get("x").asDouble();
                    double oy = obj.get("y").asDouble();
                    double ow = obj.get("width").asDouble();
                    double oh = obj.get("height").asDouble();
                    if (x + pad < ox + ow && x + pw + pad > ox && y + pad < oy + oh && y + ph + pad > oy) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<JsonNode> getObjectLayers() { return objectLayers; }
    public Canvas getCanvas() { return canvas; }
}
