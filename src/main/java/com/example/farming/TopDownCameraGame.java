package com.example.farming;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class TopDownCameraGame extends Application {

    // match your 32×32, 40×30 map
    private static final int TILE_SIZE = 32;
    private static final int MAP_W = 40;
    private static final int MAP_H = 30;

    // on-screen viewport (in tiles)
    private static final int VIEW_TILES_W = 15;
    private static final int VIEW_TILES_H = 10;
    private double SCALE_ZOOM = 3.0;   // <<— NEW: how much to scale the entire game (was 2.0 before)


    private static final int CANVAS_W = 1280;   // was VIEW_TILES_W * TILE_SIZE * 2
    private static final int CANVAS_H = 820;    // or 1080 if you want full-HD
    // resources
    private static final String TMJ_PATH = "/assets/maps/map1.tmj";
    private static final String TILESET_PNG = "/assets/maps/tileset.png";

    private Pane root;
    private Canvas canvas;
    private GraphicsContext gc;
    private final Affine camera = new Affine();

    private TileMap tileMap;

    // sprite player
    private Player player;

    private final Set<KeyCode> keys = new HashSet<>();

    @Override
    public void start(Stage stage) throws Exception {
        root = new Pane();
        canvas = new Canvas(CANVAS_W, CANVAS_H);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        // load map + tileset
        InputStream mapStream = getClass().getResourceAsStream(TMJ_PATH);
        InputStream tilesetStream = getClass().getResourceAsStream(TILESET_PNG);
        if (mapStream == null || tilesetStream == null) {
            throw new FileNotFoundException("Missing resources. TMJ=" + TMJ_PATH + " tileset=" + TILESET_PNG);
        }
        tileMap = new TileMap(mapStream, tilesetStream);

        // player spawn
        player = new Player(1 * TILE_SIZE, 15.5 * TILE_SIZE, tileMap);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));
        stage.setScene(scene);
        stage.setTitle("TopDownCameraGame");
        stage.show();

        final long[] last = {System.nanoTime()};
        new AnimationTimer() {
            @Override public void handle(long now) {
                double dt = (now - last[0]) / 1_000_000_000.0;
                last[0] = now;
                update(dt);
                render();
            }
        }.start();
    }

    private void update(double dt) {
        boolean up    = keys.contains(KeyCode.W) || keys.contains(KeyCode.UP);
        boolean down  = keys.contains(KeyCode.S) || keys.contains(KeyCode.DOWN);
        boolean left  = keys.contains(KeyCode.A) || keys.contains(KeyCode.LEFT);
        boolean right = keys.contains(KeyCode.D) || keys.contains(KeyCode.RIGHT);
        player.update(dt, up, down, left, right);
    }

    private void render() {
        // background (screen space)
        gc.setTransform(new Affine());
        gc.setFill(Color.web("#0f1220"));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);

        // camera center on player (world space)
        double viewW = VIEW_TILES_W * TILE_SIZE;
        double viewH = VIEW_TILES_H * TILE_SIZE;
        double pCx = player.getSprite().getX() + TILE_SIZE / 2.0;
        double pCy = player.getSprite().getY() + TILE_SIZE / 2.0;
        double offX = clamp(pCx - viewW / 2.0, 0, MAP_W * TILE_SIZE - viewW);
        double offY = clamp(pCy - viewH / 2.0, 0, MAP_H * TILE_SIZE - viewH);

        // set camera (scale 2x then translate)
        camera.setToIdentity();
        camera.appendScale(SCALE_ZOOM, SCALE_ZOOM);   // <<— use the new zoom value
        camera.appendTranslation(-offX, -offY);
        gc.setTransform(camera);

        // draw map
        tileMap.drawMap(gc);

        // draw player (32x32)
        gc.drawImage(
                player.getSprite().getImage(),
                player.getSprite().getX(),
                player.getSprite().getY(),
                TILE_SIZE, TILE_SIZE
        );

        // debug overlay
        gc.setTransform(new Affine());
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(14));
        gc.fillText("pos=(" + (int)player.getSprite().getX() + "," + (int)player.getSprite().getY()
                + ") cam=(" + (int)offX + "," + (int)offY + ")", 10, 20);
    }

    private static double clamp(double v, double min, double max) { return v < min ? min : Math.min(v, max); }
    public static void main(String[] args) { launch(args); }
}
