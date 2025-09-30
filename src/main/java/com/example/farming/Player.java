package com.example.farming;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Player that walks left/right/up/down using 3-frame animations.
 * No attack frames or mouse actions.
 */
public class Player {
    private static final int SPRITE_SIZE = 32;
    private static final int FRAMES = 3;
    private static final int FRAME_MS = 120;
    private static final double SPEED = 4.5 * 32.0; // pixels/sec (≈4.5 tiles/s)

    private final ImageView sprite = new ImageView();
    private final Map<String, Image[]> anim = new HashMap<>();
    private String dir = "down";
    private int frame = 0;

    private final Timeline timeline =
            new Timeline(new KeyFrame(Duration.millis(FRAME_MS), e -> nextFrame()));
    private final TileMap map;

    public Player(double x, double y, TileMap map) {
        this.map = map;
        sprite.setX(x);
        sprite.setY(y);
        sprite.setFitWidth(SPRITE_SIZE);
        sprite.setFitHeight(SPRITE_SIZE);

        anim.put("down",  SpriteLoader.loadFrames("/assets/player_sprites/down.png", 32, 32, 2, 2));
        anim.put("up",    SpriteLoader.loadFrames("/assets/player_sprites/up.png",  32, 32, 2, 2));
        anim.put("left",  SpriteLoader.loadFrames("/assets/player_sprites/left.png", 32, 32, 2, 2));
        anim.put("right", SpriteLoader.loadFrames("/assets/player_sprites/right.png",32, 32, 2, 2));

        timeline.setCycleCount(Timeline.INDEFINITE);
        updateImage();
    }

    /** Update position/animation from held-key booleans (dt in seconds). */
    public void update(double dt, boolean up, boolean down, boolean left, boolean right) {
        double dx = 0, dy = 0;
        String newDir = dir;

        if (up)    { dy -= 1; newDir = "up"; }
        if (down)  { dy += 1; newDir = "down"; }
        if (left)  { dx -= 1; newDir = "left"; }
        if (right) { dx += 1; newDir = "right"; }

        boolean moving = (dx != 0 || dy != 0);
        if (moving) {
            double len = Math.hypot(dx, dy);
            if (len != 0) { dx /= len; dy /= len; }

            double nx = sprite.getX() + dx * SPEED * dt;
            double ny = sprite.getY() + dy * SPEED * dt;

            boolean moved = false;
            if (map.isWalkable(nx, sprite.getY())) { sprite.setX(nx); moved = true; }
            if (map.isWalkable(sprite.getX(), ny)) { sprite.setY(ny); moved = true; }

            if (!dir.equals(newDir)) {
                dir = newDir;
                frame = 0;
                updateImage();
            }
            if (moved && timeline.getStatus() != Animation.Status.RUNNING) {
                timeline.play();
            }
        } else {
            if (timeline.getStatus() == Animation.Status.RUNNING) timeline.stop();
            frame = 0;
            updateImage();
        }
    }

    private void nextFrame() {
        frame = (frame + 1) % FRAMES;
        updateImage();
    }

    private void updateImage() {
        Image[] frames = anim.get(dir);
        if (frames != null && frames.length > 0) sprite.setImage(frames[frame]);
    }

    public ImageView getSprite() { return sprite; }
}
