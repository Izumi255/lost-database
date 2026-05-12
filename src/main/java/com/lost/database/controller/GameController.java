package com.lost.database.controller;

import com.lost.database.app.LostDatabaseApp;
import com.lost.database.dao.GameSessionDao;
import com.lost.database.entity.GameSession;
import com.lost.database.entity.Player;
import com.lost.database.game.entity.GamePlayer;
import com.lost.database.game.world.JungleMap;
import com.lost.database.game.world.TileMapRenderer;
import com.lost.database.game.world.TileType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class GameController {

    @FXML private Canvas gameCanvas;
    @FXML private StackPane gameContainer;
    @FXML private StackPane pauseMenuOverlay;

    @FXML private Label missionLabel;
    @FXML private Label interactLabel;

    // Dialogue UI
    @FXML private HBox dialogueContainer;
    @FXML private ImageView portraitImage;
    @FXML private Label speakerNameLabel;
    @FXML private Label dialogueTextLabel;

    private static final int TILE_SIZE = 64;
    private static final int TILE_SRC = 16;

    private JungleMap jungleMap;
    private GamePlayer player;
    private Player dbPlayer; // DB player for saves
    private boolean missionComplete = false;
    private AnimationTimer gameLoop;
    private Set<KeyCode> keys = new HashSet<>();

    // Physics
    private static final double GRAVITY = 1500.0;
    private static final double MOVE_SPEED = 220.0;
    private static final double JUMP_IMPULSE = 600.0;

    // Player visual size (hitbox)
    private static final double PLAYER_W = 60;
    private static final double PLAYER_H = 76;

    // Camera
    private double cameraX = 0;
    private double cameraY = 0;

    // --- IMAGES ---
    // Parallax backgrounds
    private Image bgLayer0; // BACKGROUND.png (farthest)
    private Image bgLayer1; // WOODS - Third.png
    private Image bgLayer2; // WOODS - Second.png
    private Image bgLayer3; // WOODS - First.png
    private Image bgLayer4; // BUSH - BACKGROUND.png (nearest)

    // Jungle tileset
    private Image jungleTileset;
    private Image jungleCanopy;
    private Image tmxTileset;

    // Decoration sprites
    private Image[] bushSprites = new Image[7];
    private Image[] grassSprites = new Image[6];
    private Image[] mushroomSprites = new Image[3];

    // Player animation sprites (GIF = animated automatically in JavaFX canvas)
    private Image playerIdleGif; // idle outline.gif
    private Image playerRunGif; // run outline.gif
    private Image playerJumpGif; // jump outline.png
    private Image playerMidAirGif; // mid air outline.gif
    private Image playerLandingPng; // landing outline.png
    private Image playerLedgeGif; // ledge grab outline.gif

    // Mission items
    private Image cockpitImage;
    private Image transceiverImage;

    // NPC Portraits
    private Image jackPortrait;
    private Image katePortrait;

    // Dialogue System
    private boolean isDialogueActive = false;
    private List<String[]> currentDialogueSequence = null; // [SpeakerName, PortraitName, Text]
    private int currentDialogueIndex = 0;

    // Animation
    private enum AnimState {
        IDLE,
        WALK,
        RUN,
        JUMP,
        MIDAIR,
        LANDING,
        LEDGE_GRAB,
        HURT
    }

    private AnimState animState = AnimState.IDLE;
    private int animFrame = 0;
    private double animTimer = 0;
    private double frameDuration = 0.12;
    private boolean facingRight = true;
    private boolean wasGroundedLastFrame = true;
    private double airborneTimer = 0; // coyote time for animation
    private double landingTimer = 0.0; // how long to show landing anim

    // Double jump
    private int jumpsUsed = 0;
    private static final int MAX_JUMPS = 2;

    // Dash
    private boolean isDashing = false;
    private double dashTimer = 0;
    private double dashCooldown = 0;
    private static final double DASH_DURATION = 0.18;
    private static final double DASH_COOLDOWN = 0.8;
    private static final double DASH_SPEED = 600;

    // Level management
    private int currentLevel = 1;
    private static final int MAX_LEVELS = 3;
    private boolean levelComplete = false;
    private double levelCompleteTimer = 0;

    // Sprite render size (visual)
    private static final double SPRITE_RENDER_W = 40;
    private static final double SPRITE_RENDER_H = 40;

    // --- ENEMY SYSTEM ---
    private List<Enemy> enemies = new ArrayList<>();

    // --- ITEM SYSTEM ---
    private List<ItemDrop> items = new ArrayList<>();

    // --- KATE NPC ---
    private Image kateIdleSprite;
    private double kateX, kateY;
    private boolean kateExists = false;
    private boolean kateTalkedTo = false;
    private static final double KATE_W = 80;
    private static final double KATE_H = 120;
    private static final double KATE_INTERACT_RANGE = 100;

    // --- GAME STATE ---
    private boolean isPaused = false;
    private boolean gameOver = false;
    private double gameOverTimer = 0;
    private double damageFlashTimer = 0;
    private double sanity = 100.0;
    private Image[] ghostFrames; // 6 frames: sprite_0..5.png (facing left)

    // Simple HUD colors
    private static final Color HP_BAR_BG = Color.rgb(40, 40, 40, 0.8);
    private static final Color HP_BAR_FILL = Color.rgb(220, 50, 50);
    private static final Color HP_BAR_FILL_LOW = Color.rgb(255, 80, 30);
    private static final Color HP_BAR_BORDER = Color.rgb(200, 200, 200, 0.6);

    @FXML
    public void initialize() {
        // Load parallax backgrounds from textures/Backgrounds/
        bgLayer0 = safeLoadTexture("Backgrounds/BACKGROUND.png");
        bgLayer1 = safeLoadTexture("Backgrounds/WOODS - Third.png");
        bgLayer2 = safeLoadTexture("Backgrounds/WOODS - Second.png");
        bgLayer3 = safeLoadTexture("Backgrounds/WOODS - First.png");
        bgLayer4 = safeLoadTexture("Backgrounds/WOODS - Fourth.png");

        // Load jungle tileset
        jungleTileset = safeLoad("/images/Jungle.png");
        jungleCanopy = safeLoad("/images/Jungle_Canopy.png");
        tmxTileset = safeLoadTexture("Tilesheet - WOODS.png");
        System.out.println("✓ Tileset loaded: " + (tmxTileset != null && !tmxTileset.isError()));

        // Load decoration sprites
        for (int i = 0; i < 7; i++) {
            bushSprites[i] = safeLoadTexture("Decorations/BUSH FOREGROUND 1-" + (i + 1) + ".png");
        }
        for (int i = 0; i < 6; i++) {
            int group = i / 2 + 1;
            int variant = i % 2 + 1;
            grassSprites[i] =
                    safeLoadTexture("Decorations/GRASS " + group + "-" + variant + ".png");
        }
        for (int i = 0; i < 3; i++) {
            String key = (i < 2) ? "MUSHROOM 1-" + (i + 1) : "MUSHROOM 2-1";
            mushroomSprites[i] = safeLoadTexture("Decorations/" + key + ".png");
        }

        // Load player sprites from /assets/sprites/
        // JavaFX Image supports animated GIFs — they auto-animate when redrawn in
        // AnimationTimer
        playerIdleGif = safeLoad("/assets/sprites/idle outline.gif");
        playerRunGif = safeLoad("/assets/sprites/run outline.gif");
        playerJumpGif = safeLoad("/assets/sprites/jump outline.png");
        playerMidAirGif = safeLoad("/assets/sprites/mid air outline.gif");
        playerLandingPng = safeLoad("/assets/sprites/landing outline.png");
        playerLedgeGif = safeLoad("/assets/sprites/ledge grab outline.gif");
        System.out.println(
                "Sprites loaded: idle="
                        + (playerIdleGif != null)
                        + " run="
                        + (playerRunGif != null)
                        + " jump="
                        + (playerJumpGif != null)
                        + " midair="
                        + (playerMidAirGif != null));

        // Mission items
        cockpitImage = safeLoad("/images/cockpit_wreckage.png");
        transceiverImage = safeLoad("/images/item_transceiver.png");

        // NPC Portraits
        jackPortrait = safeLoad("/assets/sprites/sayid_serious.png");
        katePortrait = safeLoad("/assets/sprites/dialog/kate_portrait.png");

        // Kate idle sprite for world rendering
        kateIdleSprite = safeLoad("/assets/sprites/dialog/kate_idle.png");

        // Load ghost animation frames (facing left)
        ghostFrames = new Image[6];
        for (int i = 0; i < 6; i++) {
            ghostFrames[i] = safeLoadTexture("Ghosts/sprite_" + i + ".png");
        }

        // Init HUD icons array if needed, handled in logic below

        // Create map and player from TMX level
        currentLevel = 1;
        jungleMap = new JungleMap("/assets/levels/level1.tmx");
        System.out.println("✓ Map loaded: " + jungleMap.getWidth() + "x" + jungleMap.getHeight());
        System.out.println("✓ Map is TMX: " + jungleMap.isTmx());
        if (jungleMap.isTmx()) {
            System.out.println("✓ TMX layers: " + jungleMap.getTmxLayers().size());

            // Debug: print first layer sample
            if (!jungleMap.getTmxLayers().isEmpty()) {
                int[][] firstLayer = jungleMap.getTmxLayers().get(0);
                System.out.println(
                        "First layer size: "
                                + firstLayer.length
                                + " rows x "
                                + firstLayer[0].length
                                + " cols");

                // Count non-zero tiles
                int nonZeroCount = 0;
                int maxTileId = 0;
                for (int r = 0; r < firstLayer.length; r++) {
                    for (int c = 0; c < firstLayer[r].length; c++) {
                        if (firstLayer[r][c] > 0) {
                            nonZeroCount++;
                            maxTileId = Math.max(maxTileId, firstLayer[r][c]);
                        }
                    }
                }
                System.out.println(
                        "Non-zero tiles: " + nonZeroCount + ", Max tile ID: " + maxTileId);
            }
        }

        // Use JungleMap spawn point (in tiles) and convert to pixels
        double spawnX = jungleMap.getSpawnX() * TILE_SIZE;
        double spawnY = (jungleMap.getSpawnY() - 1) * TILE_SIZE; // stand on top of spawn tile

        player = new GamePlayer(0, spawnX, spawnY);
        player.setSpawnPosition(spawnX, spawnY);
        player.resetToSpawn();

        // Initialize enemies from map
        initEnemiesFromMap();
        // Initialize items from map
        initItemsFromMap();

        // Resize canvas
        gameContainer
                .widthProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            gameCanvas.setWidth(newVal.doubleValue());
                        });
        gameContainer
                .heightProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            gameCanvas.setHeight(newVal.doubleValue());
                        });

        gameCanvas.setFocusTraversable(true);
        gameCanvas.setOnKeyPressed(this::handleKeyPressed);
        gameCanvas.setOnKeyReleased(this::handleKeyReleased);

        // Start game loop
        gameLoop =
                new AnimationTimer() {
                    private long last = -1;

                    @Override
                    public void handle(long now) {
                        if (last < 0) last = now;
                        double dt = (now - last) / 1_000_000_000.0;
                        if (dt > 0.05) dt = 0.05; // cap delta time
                        update(dt);
                        render();
                        last = now;
                    }
                };
        gameLoop.start();

        // Spawn Kate NPC for Level 1
        if (currentLevel == 1) {
            spawnKateNPC();
        }
    }

    // --- DIALOGUE SYSTEM ---
    private void startLevel1Cutscene() {
        List<String[]> kateDialog = new ArrayList<>();
        kateDialog.add(
                new String[] {
                    "Кейт",
                    "kate",
                    "Гей, обережніше там! Дехто з тих, хто пішов за водою, так і не повернувся."
                });
        kateDialog.add(new String[] {"Кейт", "kate", "Кажуть, вони бачили... щось серед дерев."});
        kateDialog.add(
                new String[] {
                    "Гравець",
                    "jack",
                    "Я мушу знайти кабіну пілотів, Кейт. Без трансивера ми тут назавжди."
                });
        kateDialog.add(
                new String[] {
                    "Кейт", "kate", "Тримай очі відкритими. Якщо почуєш дивні звуки — краще біжи."
                });

        startDialogue(kateDialog);
    }

    private void startDialogue(List<String[]> sequence) {
        if (sequence == null || sequence.isEmpty()) return;

        currentDialogueSequence = sequence;
        currentDialogueIndex = 0;
        isDialogueActive = true;
        keys.clear(); // Stop player movement

        dialogueContainer.setVisible(true);
        showCurrentDialogueLine();
    }

    private void advanceDialogue() {
        currentDialogueIndex++;
        if (currentDialogueIndex >= currentDialogueSequence.size()) {
            endDialogue();
        } else {
            showCurrentDialogueLine();
        }
    }

    private void showCurrentDialogueLine() {
        String[] line = currentDialogueSequence.get(currentDialogueIndex);
        String speaker = line[0];
        String portraitKey = line[1];
        String text = line[2];

        speakerNameLabel.setText(speaker);
        dialogueTextLabel.setText(text);

        if (portraitKey.equals("jack")) {
            portraitImage.setImage(jackPortrait);
            portraitImage.setVisible(true);
        } else if (portraitKey.equals("kate")) {
            portraitImage.setImage(katePortrait);
            portraitImage.setVisible(true);
        } else {
            portraitImage.setVisible(false);
        }
    }

    private void endDialogue() {
        isDialogueActive = false;
        dialogueContainer.setVisible(false);
        currentDialogueSequence = null;
    }

    // --- LOADING HELPERS ---
    private Image safeLoad(String path) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            return new Image(is);
        } catch (Exception e) {
            return null;
        }
    }

    private Image safeLoadTexture(String relativePath) {
        // Load from /assets/textures/ path
        return safeLoad("/assets/textures/" + relativePath);
    }

    private boolean isValidImage(Image img) {
        return img != null && !img.isError();
    }

    // --- ENEMY / ITEM INIT ---
    private void initEnemiesFromMap() {
        enemies.clear();
        boolean foundAny = false;

        for (int x = 0; x < jungleMap.getWidth(); x++) {
            for (int y = 0; y < jungleMap.getHeight(); y++) {
                if (jungleMap.getTile(x, y) == TileType.ENEMY_PATROL) {
                    double px = x * TILE_SIZE;
                    double py = (y) * TILE_SIZE - TILE_SIZE;
                    enemies.add(new Enemy(px, py, px - 3 * TILE_SIZE, px + 3 * TILE_SIZE));
                    jungleMap.setTile(x, y, null);
                    foundAny = true;
                }
            }
        }

        // If no enemies found in map (TMX levels), spawn ghosts programmatically
        if (!foundAny) {
            spawnGhostsForLevel(currentLevel);
        }
    }

    private void spawnGhostsForLevel(int level) {
        int mapW = jungleMap.getWidth() * TILE_SIZE;

        switch (level) {
            case 1:
                // level 1
                enemies.add(
                        new Enemy(15 * TILE_SIZE, 6 * TILE_SIZE, 12 * TILE_SIZE, 22 * TILE_SIZE));
                enemies.add(
                        new Enemy(35 * TILE_SIZE, 5 * TILE_SIZE, 30 * TILE_SIZE, 45 * TILE_SIZE));
                enemies.add(
                        new Enemy(55 * TILE_SIZE, 7 * TILE_SIZE, 50 * TILE_SIZE, 65 * TILE_SIZE));
                break;
            case 2:
                // level 2
                enemies.add(
                        new Enemy(10 * TILE_SIZE, 4 * TILE_SIZE, 5 * TILE_SIZE, 20 * TILE_SIZE));
                enemies.add(
                        new Enemy(25 * TILE_SIZE, 3 * TILE_SIZE, 20 * TILE_SIZE, 35 * TILE_SIZE));
                enemies.add(
                        new Enemy(40 * TILE_SIZE, 5 * TILE_SIZE, 35 * TILE_SIZE, 50 * TILE_SIZE));
                enemies.add(
                        new Enemy(55 * TILE_SIZE, 4 * TILE_SIZE, 48 * TILE_SIZE, 60 * TILE_SIZE));
                enemies.add(
                        new Enemy(62 * TILE_SIZE, 6 * TILE_SIZE, 58 * TILE_SIZE, 68 * TILE_SIZE));
                break;
            case 3:
                // level 3 (hardest)
                enemies.add(new Enemy(8 * TILE_SIZE, 3 * TILE_SIZE, 3 * TILE_SIZE, 15 * TILE_SIZE));
                enemies.add(
                        new Enemy(20 * TILE_SIZE, 5 * TILE_SIZE, 15 * TILE_SIZE, 28 * TILE_SIZE));
                enemies.add(
                        new Enemy(30 * TILE_SIZE, 2 * TILE_SIZE, 25 * TILE_SIZE, 38 * TILE_SIZE));
                enemies.add(
                        new Enemy(40 * TILE_SIZE, 6 * TILE_SIZE, 35 * TILE_SIZE, 48 * TILE_SIZE));
                enemies.add(
                        new Enemy(50 * TILE_SIZE, 4 * TILE_SIZE, 45 * TILE_SIZE, 55 * TILE_SIZE));
                enemies.add(
                        new Enemy(58 * TILE_SIZE, 3 * TILE_SIZE, 53 * TILE_SIZE, 63 * TILE_SIZE));
                enemies.add(
                        new Enemy(65 * TILE_SIZE, 5 * TILE_SIZE, 60 * TILE_SIZE, 70 * TILE_SIZE));
                break;
        }
    }

    private void initItemsFromMap() {
        items.clear();
        for (int x = 0; x < jungleMap.getWidth(); x++) {
            for (int y = 0; y < jungleMap.getHeight(); y++) {
                TileType t = jungleMap.getTile(x, y);
                if (t == TileType.HEALTH_PACK) {
                    items.add(new ItemDrop(x * TILE_SIZE, y * TILE_SIZE, "health_pack", 30));
                    jungleMap.setTile(x, y, null);
                } else if (t == TileType.FOOD_ITEM) {
                    items.add(new ItemDrop(x * TILE_SIZE, y * TILE_SIZE, "food", 10));
                    jungleMap.setTile(x, y, null);
                }
            }
        }
    }

    private static class ItemDrop {
        double x, y;
        String type;
        int value;

        ItemDrop(double x, double y, String type, int value) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.value = value;
        }
    }

    // --- INPUT ---
    private void handleKeyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getCode() == KeyCode.R) restartGame();
            return;
        }

        // Intercept ESC for Pause Menu
        if (e.getCode() == KeyCode.ESCAPE) {
            togglePauseMenu();
            return;
        }

        if (levelComplete) {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) loadNextLevel();
            return;
        }

        // Intercept input for dialogue
        if (isDialogueActive) {
            if (e.getCode() == KeyCode.E) {
                advanceDialogue();
            }
            return; // Block other inputs while dialogue is active
        }

        keys.add(e.getCode());

        if (e.getCode() == KeyCode.E) tryInteract();

        // Jump — only SPACE (W is for movement, not jump)
        if (e.getCode() == KeyCode.SPACE) {
            if (jumpsUsed < MAX_JUMPS) {
                player.setVy(-JUMP_IMPULSE);
                player.setGrounded(false);
                jumpsUsed++;
            }
        }

        // Dash — Shift key
        if ((e.getCode() == KeyCode.SHIFT || e.getCode() == KeyCode.C)
                && dashCooldown <= 0
                && !isDashing) {
            isDashing = true;
            dashTimer = DASH_DURATION;
            dashCooldown = DASH_COOLDOWN;
        }
    }

    private void handleKeyReleased(KeyEvent e) {
        keys.remove(e.getCode());
    }

    // --- UPDATE ---
    private void update(double dt) {
        if (dt <= 0 || isPaused) return;

        if (gameOver) {
            gameOverTimer += dt;
            return;
        }
        if (levelComplete) {
            levelCompleteTimer += dt;
            return;
        }

        // Dash cooldown
        if (dashCooldown > 0) dashCooldown -= dt;
        if (landingTimer > 0) landingTimer -= dt;

        // Update damage cooldown
        player.updateCooldown(dt);
        if (damageFlashTimer > 0) damageFlashTimer -= dt;

        boolean currentlyGrounded = player.isGrounded();

        // Reset jumps when grounded
        if (currentlyGrounded) jumpsUsed = 0;

        // DASH movement
        if (isDashing) {
            dashTimer -= dt;
            double dashDir = facingRight ? 1 : -1;
            double nextXDash = player.getX() + dashDir * DASH_SPEED * dt;
            if (!collidesAt(nextXDash, player.getY())) {
                player.setX(nextXDash);
            }
            if (dashTimer <= 0) isDashing = false;
        } else {
            // Normal horizontal input
            double targetVx = 0;
            if (keys.contains(KeyCode.A)) targetVx -= MOVE_SPEED;
            if (keys.contains(KeyCode.D)) targetVx += MOVE_SPEED;
            player.setVx(targetVx);

            // Apply gravity
            player.setVy(player.getVy() + GRAVITY * dt);

            // Integrate X
            double nextX = player.getX() + player.getVx() * dt;
            if (collidesAt(nextX, player.getY())) {
                if (player.getVx() > 0) {
                    int tileX = (int) ((nextX + PLAYER_W) / TILE_SIZE);
                    nextX = tileX * TILE_SIZE - PLAYER_W - 0.1;
                } else if (player.getVx() < 0) {
                    int tileX = (int) (nextX / TILE_SIZE);
                    nextX = (tileX + 1) * TILE_SIZE + 0.1;
                }
                player.setVx(0);
            }
            player.setX(nextX);

            // Integrate Y
            double nextY = player.getY() + player.getVy() * dt;
            player.setGrounded(false);

            if (collidesAt(player.getX(), nextY)) {
                if (player.getVy() > 0) {
                    int tileY = (int) ((nextY + PLAYER_H) / TILE_SIZE);
                    nextY = tileY * TILE_SIZE - PLAYER_H - 0.1;
                    player.setGrounded(true);
                    // Landing animation trigger — only after real jump
                    if (!wasGroundedLastFrame && airborneTimer > 0.05) {
                        landingTimer = 0.2;
                    }
                } else if (player.getVy() < 0) {
                    int tileY = (int) (nextY / TILE_SIZE);
                    nextY = (tileY + 1) * TILE_SIZE + 0.1;
                }
                player.setVy(0);
            }
            player.setY(nextY);
        }

        wasGroundedLastFrame = player.isGrounded();

        // Fall into void = damage + respawn
        if (player.getY() > jungleMap.getHeight() * TILE_SIZE + 100) {
            player.takeDamage(25);
            damageFlashTimer = 0.3;
            player.resetToSpawn();
            jumpsUsed = 0;
        }

        // Hazard check (spikes)
        if (isTouchingHazard()) {
            if (player.takeDamage(20)) {
                damageFlashTimer = 0.3;
                player.resetToSpawn();
                jumpsUsed = 0;
            }
        }

        // Check death
        if (!player.isAlive()) {
            gameOver = true;
            gameOverTimer = 0;
            return;
        }

        // Update enemies
        for (Enemy enemy : enemies) {
            enemy.update(dt, jungleMap, TILE_SIZE);
            if (!player.isInvincible()
                    && rectsOverlap(
                            player.getX(),
                            player.getY(),
                            PLAYER_W,
                            PLAYER_H,
                            enemy.x,
                            enemy.y,
                            enemy.w,
                            enemy.h)) {
                if (enemy.isGhost) {
                    sanity -= 20 * dt;
                    if (sanity <= 0) {
                        sanity = 0;
                        gameOver = true;
                        gameOverTimer = 0;
                    }
                } else if (player.takeDamage(15)) {
                    damageFlashTimer = 0.3;
                    double knockDir = (player.getX() < enemy.x) ? -1 : 1;
                    player.setVx(knockDir * 300);
                    player.setVy(-200);
                    player.setGrounded(false);
                }
            }
        }

        // Check item pickups
        items.removeIf(
                item -> {
                    if (rectsOverlap(
                            player.getX(),
                            player.getY(),
                            PLAYER_W,
                            PLAYER_H,
                            item.x,
                            item.y,
                            24,
                            24)) {
                        if (item.type.equals("health_pack")) {
                            player.heal(item.value);
                            showMission("❤ Аптечка! +" + item.value + " HP");
                        } else if (item.type.equals("food")) {
                            player.heal(item.value);
                            showMission("🍎 Їжа! +" + item.value + " HP");
                        }
                        return true;
                    }
                    return false;
                });

        // Update camera
        double canvasW = gameCanvas.getWidth();
        double canvasH = gameCanvas.getHeight();
        cameraX = player.getX() - canvasW / 2 + PLAYER_W / 2.0;
        cameraY = player.getY() - canvasH / 2 + PLAYER_H / 2.0;
        double maxCamX = jungleMap.getWidth() * TILE_SIZE - canvasW;
        double maxCamY = jungleMap.getHeight() * TILE_SIZE - canvasH;
        cameraX = (maxCamX < 0) ? maxCamX / 2 : Math.max(0, Math.min(cameraX, maxCamX));
        cameraY = (maxCamY < 0) ? maxCamY : Math.max(0, Math.min(cameraY, maxCamY));

        // Track airborne time for coyote animation
        if (!player.isGrounded()) {
            airborneTimer += dt;
        } else {
            airborneTimer = 0;
        }

        // Animation state
        AnimState newState;
        if (!player.isAlive() || damageFlashTimer > 0) {
            newState = AnimState.HURT;
        } else if (isDashing) {
            newState = AnimState.RUN;
        } else if (!player.isGrounded() && airborneTimer > 0.05) {
            // Only show air anim after 0.15s airborne (coyote time)
            newState = (player.getVy() < 0) ? AnimState.JUMP : AnimState.MIDAIR;
        } else if (keys.contains(KeyCode.A) || keys.contains(KeyCode.D)) {
            newState = AnimState.RUN;
        } else if (landingTimer > 0) {
            newState = AnimState.LANDING;
        } else {
            newState = AnimState.IDLE;
        }

        if (player.getVx() > 0.1) facingRight = true;
        else if (player.getVx() < -0.1) facingRight = false;

        if (newState != animState) {
            animState = newState;
            animFrame = 0;
            animTimer = 0;
        }

        animTimer += dt;
        if (animTimer >= frameDuration) {
            animTimer -= frameDuration;
            animFrame++;
        }

        // Check proximity to cockpit / level exit
        checkProximity();
    }

    // --- RENDER ---
    private void render() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        double canvasW = gameCanvas.getWidth();
        double canvasH = gameCanvas.getHeight();

        if (canvasW <= 0 || canvasH <= 0) return;

        // Clear background (jungle sky)
        gc.setFill(Color.web("#1B3A2D"));
        gc.fillRect(0, 0, canvasW, canvasH);

        // Draw parallax background layers (farthest → nearest)
        drawParallax(gc, bgLayer0, 0.0, canvasW, canvasH); // Static sky
        drawParallax(gc, bgLayer1, 0.05, canvasW, canvasH); // Far trees
        drawParallax(gc, bgLayer2, 0.1, canvasW, canvasH); // Mid trees
        drawParallax(gc, bgLayer3, 0.2, canvasW, canvasH); // Near trees
        drawParallax(gc, bgLayer4, 0.3, canvasW, canvasH); // Bushes

        // 1. Draw tiles from TMX
        if (jungleMap.isTmx()) {
            TileMapRenderer renderer = new TileMapRenderer();
            for (int[][] layer : jungleMap.getTmxLayers()) {
                renderer.render(gc, layer, tmxTileset, cameraX, cameraY, canvasW, canvasH);
            }
        } else {
            // Fallback procedurally generated map drawing logic
            int startTileX = Math.max(0, (int) (cameraX / TILE_SIZE) - 1);
            int startTileY = Math.max(0, (int) (cameraY / TILE_SIZE) - 1);
            int endTileX =
                    Math.min(jungleMap.getWidth(), startTileX + (int) (canvasW / TILE_SIZE) + 2);
            int endTileY =
                    Math.min(jungleMap.getHeight(), startTileY + (int) (canvasH / TILE_SIZE) + 2);

            for (int mx = startTileX; mx < endTileX; mx++) {
                for (int my = startTileY; my < endTileY; my++) {
                    TileType t = jungleMap.getTile(mx, my);
                    if (t == null) continue;

                    double destX = mx * TILE_SIZE - cameraX;
                    double destY = my * TILE_SIZE - cameraY;

                    switch (t) {
                        case GROUND:
                            drawGroundTile(gc, mx, my, destX, destY);
                            break;
                        case FLOATING_PLATFORM:
                            drawPlatformTile(gc, destX, destY);
                            break;
                        case DECORATION:
                            drawDecoration(gc, mx, my, destX, destY);
                            break;
                        case SPIKES:
                            drawSpikes(gc, destX, destY);
                            break;
                        case COCKPIT_WRECKAGE:
                            drawCockpit(gc, destX, destY);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        // 4. Draw items
        for (ItemDrop item : items) {
            double ix = item.x - cameraX;
            double iy = item.y - cameraY;
            if (ix < -32 || ix > canvasW + 32 || iy < -32 || iy > canvasH + 32) continue;

            // Floating animation
            double floatOffset = Math.sin(System.currentTimeMillis() / 300.0 + item.x) * 4;
            iy += floatOffset;

            if (item.type.equals("health_pack")) {
                // Red cross
                gc.setFill(Color.WHITE);
                gc.fillRoundRect(ix, iy, 24, 24, 6, 6);
                gc.setFill(Color.rgb(220, 40, 40));
                gc.fillRect(ix + 9, iy + 3, 6, 18);
                gc.fillRect(ix + 3, iy + 9, 18, 6);
            } else if (item.type.equals("food")) {
                // Simple food icon (green circle with leaf)
                gc.setFill(Color.rgb(50, 180, 50));
                gc.fillOval(ix + 2, iy + 4, 20, 18);
                gc.setFill(Color.rgb(30, 140, 30));
                gc.fillOval(ix + 6, iy + 1, 12, 8);
            }
        }

        // 5. Draw enemies
        for (Enemy enemy : enemies) {
            double ex = enemy.x - cameraX;
            double ey = enemy.y - cameraY;
            if (ex < -64 || ex > canvasW + 64 || ey < -64 || ey > canvasH + 64) continue;

            if (enemy.isGhost && ghostFrames != null) {
                Image ghostSprite = ghostFrames[enemy.spriteVariant % ghostFrames.length];
                if (!isValidImage(ghostSprite)) continue;

                // No bob — ghost position is stable

                // Subtle opacity pulse for spooky effect (0.7 — 1.0)
                double flicker =
                        0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 400.0 + enemy.y * 0.3);
                gc.setGlobalAlpha(flicker);

                double gWidth = 72;
                double gHeight = gWidth * (ghostSprite.getHeight() / ghostSprite.getWidth());

                // Sprites face LEFT — flip when moving right
                if (enemy.movingRight) {
                    gc.drawImage(ghostSprite, ex + gWidth, ey - 20, -gWidth, gHeight);
                } else {
                    gc.drawImage(ghostSprite, ex, ey - 20, gWidth, gHeight);
                }
                gc.setGlobalAlpha(1.0);
            } else {
                // Enemy body
                gc.setFill(Color.rgb(80, 30, 30));
                gc.fillRoundRect(ex + 4, ey + 8, enemy.w - 8, enemy.h - 8, 8, 8);
                // Eyes
                gc.setFill(Color.RED);
                double eyeDir = enemy.movingRight ? 1 : -1;
                gc.fillOval(ex + 12 + eyeDir * 3, ey + 14, 6, 6);
                gc.fillOval(ex + 24 + eyeDir * 3, ey + 14, 6, 6);
                // Angry eyebrows
                gc.setStroke(Color.rgb(60, 20, 20));
                gc.setLineWidth(2);
                gc.strokeLine(ex + 10, ey + 12, ex + 18, ey + 14);
                gc.strokeLine(ex + 30, ey + 14, ex + 38, ey + 12);
            }
        }

        // 5.5 Draw Kate NPC
        drawKateNPC(gc, canvasW, canvasH);

        // 6. Draw player
        drawPlayer(gc, canvasW, canvasH);

        // 8. Draw HUD
        drawHUD(gc, canvasW, canvasH);

        // 9. Draw minimap
        drawMiniMap(gc);

        // 10. Game Over overlay
        if (gameOver) {
            drawGameOver(gc, canvasW, canvasH);
        }

        // 11. Level Complete overlay
        if (levelComplete) {
            drawLevelComplete(gc, canvasW, canvasH);
        }
    }

    // --- DRAW HELPERS ---
    private void drawParallax(
            GraphicsContext gc, Image layer, double speedFactor, double canvasW, double canvasH) {
        if (!isValidImage(layer)) return;

        double imgW = layer.getWidth();
        double imgH = layer.getHeight();
        double scale = canvasH / imgH;
        double scaledW = imgW * scale;

        double offset = cameraX * speedFactor;
        double x = -(offset % scaledW);
        if (x > 0) x -= scaledW;

        while (x < canvasW) {
            gc.drawImage(layer, x, 0, scaledW, canvasH);
            x += scaledW;
        }
    }

    private void drawGroundTile(GraphicsContext gc, int mx, int my, double destX, double destY) {
        if (isValidImage(jungleTileset)) {
            boolean isTopSurface =
                    (my > 0 && jungleMap.getTile(mx, my - 1) == null)
                            || (my > 0 && jungleMap.getTile(mx, my - 1) == TileType.DECORATION);

            if (isTopSurface) {
                int variation = (mx + my * 3) % 3;
                drawTileFromTileset(gc, jungleTileset, 4 + variation, 0, destX, destY);
            } else {
                int variation = (mx * 7 + my * 11) % 4;
                drawTileFromTileset(gc, jungleTileset, 4 + variation, 1, destX, destY);
            }
        } else {
            // Fallback colors
            boolean isTop = (my > 0 && jungleMap.getTile(mx, my - 1) == null);
            gc.setFill(isTop ? Color.rgb(80, 140, 50) : Color.rgb(101, 67, 33));
            gc.fillRect(destX, destY, TILE_SIZE, TILE_SIZE);
            if (isTop) {
                gc.setFill(Color.rgb(60, 120, 30));
                gc.fillRect(destX, destY, TILE_SIZE, 4);
            }
        }
    }

    private void drawPlatformTile(GraphicsContext gc, double destX, double destY) {
        if (isValidImage(jungleTileset)) {
            drawTileFromTileset(gc, jungleTileset, 0, 2, destX, destY);
        } else {
            gc.setFill(Color.rgb(130, 90, 50));
            gc.fillRect(destX, destY, TILE_SIZE, TILE_SIZE);
            gc.setFill(Color.rgb(100, 70, 40));
            gc.fillRect(destX, destY, TILE_SIZE, 4);
            gc.fillRect(destX, destY + TILE_SIZE - 4, TILE_SIZE, 4);
        }
    }

    private void drawDecoration(GraphicsContext gc, int mx, int my, double destX, double destY) {
        // Use decoration sprites from Decorations folder
        int seed = (mx * 73 + my * 97) & 0x7FFFFFFF;

        // Check if multi-tile decoration (tree) or single (grass/bush)
        boolean hasDecoAbove = my > 0 && jungleMap.getTile(mx, my - 1) == TileType.DECORATION;
        boolean hasDecoBelow =
                my < jungleMap.getHeight() - 1
                        && jungleMap.getTile(mx, my + 1) == TileType.DECORATION;

        if (!hasDecoAbove && hasDecoBelow) {
            // Top of tree — use bush sprite
            Image bush = bushSprites[seed % bushSprites.length];
            if (isValidImage(bush)) {
                gc.drawImage(bush, destX - 8, destY - 8, TILE_SIZE + 16, TILE_SIZE + 8);
            }
        } else if (hasDecoAbove) {
            // Bottom/middle of tree — trunk
            gc.setFill(Color.rgb(90, 60, 30, 0.6));
            gc.fillRect(destX + 10, destY, 12, TILE_SIZE);
        } else {
            // Single decoration — grass or mushroom
            if (seed % 4 == 0) {
                Image mush = mushroomSprites[seed % mushroomSprites.length];
                if (isValidImage(mush)) {
                    gc.drawImage(mush, destX + 4, destY + 4, 24, 24);
                    return;
                }
            }
            Image grass = grassSprites[seed % grassSprites.length];
            if (isValidImage(grass)) {
                gc.drawImage(grass, destX, destY + 8, TILE_SIZE, TILE_SIZE - 8);
            } else {
                // Fallback
                gc.setFill(Color.rgb(60, 150, 40, 0.7));
                gc.fillRect(destX + 8, destY + 16, 16, 16);
            }
        }
    }

    private void drawSpikes(GraphicsContext gc, double destX, double destY) {
        gc.setFill(Color.rgb(150, 50, 50));
        // Draw triangle spikes
        for (int i = 0; i < 3; i++) {
            double sx = destX + i * 11;
            gc.fillPolygon(
                    new double[] {sx + 1, sx + 5.5, sx + 10},
                    new double[] {destY + TILE_SIZE, destY + 4, destY + TILE_SIZE},
                    3);
        }
    }

    private void drawCockpit(GraphicsContext gc, double destX, double destY) {
        if (isValidImage(cockpitImage)) {
            gc.drawImage(
                    cockpitImage,
                    destX - TILE_SIZE,
                    destY - TILE_SIZE,
                    TILE_SIZE * 3,
                    TILE_SIZE * 3);
        } else {
            gc.setFill(Color.SILVER);
            gc.fillRect(destX, destY, TILE_SIZE, TILE_SIZE);
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(destX + 4, destY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
        }

        // Glowing marker above cockpit
        double glow = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 200.0);
        gc.setFill(Color.rgb(255, 200, 50, glow * 0.6));
        gc.fillOval(destX + 8, destY - 20, 16, 16);
    }

    private void drawPlayer(GraphicsContext gc, double canvasW, double canvasH) {
        double playerScreenX = player.getX() - cameraX;
        double playerScreenY = player.getY() - cameraY;

        // Dash afterimage effect
        if (isDashing) {
            gc.setGlobalAlpha(0.25);
            gc.setFill(Color.CYAN);
            gc.fillRoundRect(
                    playerScreenX + 8, playerScreenY + 4, PLAYER_W - 16, PLAYER_H - 4, 6, 6);
            gc.setGlobalAlpha(1.0);
        }

        // Damage flash
        if (damageFlashTimer > 0 && ((int) (damageFlashTimer * 10)) % 2 == 0) {
            gc.setGlobalAlpha(0.4);
        }

        Image currentGif = getCurrentSprite();
        // GIF images in JavaFX animate automatically when the canvas redraws each frame
        if (isValidImage(currentGif)) {
            // Auto-scale by ~2.0x pixel ratio to match the 2x tile scale
            double scale = 2.0;
            double dw = currentGif.getWidth() * scale;
            double dh = currentGif.getHeight() * scale;

            if (dw <= 0 || dh <= 0) {
                dw = SPRITE_RENDER_W;
                dh = SPRITE_RENDER_H;
            }

            // Center sprite horizontally, attach to bottom of hitbox vertically
            double drawX = playerScreenX - (dw - PLAYER_W) / 2.0;
            double drawY = playerScreenY - (dh - PLAYER_H); // align bottom feet

            if (facingRight) {
                gc.drawImage(currentGif, drawX, drawY, dw, dh);
            } else {
                // Flip horizontally by using negative width
                gc.drawImage(currentGif, drawX + dw, drawY, -dw, dh);
            }
        } else {
            // Fallback: simple stick figure
            gc.setFill(Color.rgb(200, 150, 80));
            gc.fillRoundRect(playerScreenX + 8, playerScreenY, PLAYER_W - 16, PLAYER_H, 10, 10);
            gc.setFill(Color.rgb(230, 180, 120));
            gc.fillOval(playerScreenX + 12, playerScreenY - 8, 24, 24);
        }

        // Double-jump air indicator (small circle above player)
        if (!player.isGrounded() && jumpsUsed > 1) {
            gc.setGlobalAlpha(0.7);
            gc.setFill(Color.CYAN);
            gc.fillOval(playerScreenX + PLAYER_W / 2 - 4, playerScreenY - 14, 8, 8);
        }

        gc.setGlobalAlpha(1.0);
    }

    private Image getCurrentSprite() {
        switch (animState) {
            case IDLE:
                return isValidImage(playerIdleGif) ? playerIdleGif : null;
            case WALK:
                return isValidImage(playerRunGif) ? playerRunGif : null; // walk uses run
            case RUN:
                return isValidImage(playerRunGif) ? playerRunGif : null;
            case JUMP:
                return isValidImage(playerJumpGif) ? playerJumpGif : null;
            case MIDAIR:
                return isValidImage(playerMidAirGif) ? playerMidAirGif : null;
            case LANDING:
                return isValidImage(playerLandingPng) ? playerLandingPng : null;
            case LEDGE_GRAB:
                return isValidImage(playerLedgeGif) ? playerLedgeGif : null;
            case HURT:
                return isValidImage(playerIdleGif) ? playerIdleGif : null;
            default:
                return playerIdleGif;
        }
    }

    private void drawHUD(GraphicsContext gc, double canvasW, double canvasH) {
        gc.save();
        gc.setTextAlign(TextAlignment.LEFT);

        // ======= TOP CENTER: ORBS + INVENTORY BAR =======
        double barW = 320;
        double barH = 52;
        double barX = canvasW / 2 - barW / 2;
        double barY = 8;
        double orbRadius = 32;

        // --- Inventory bar frame (dark metallic) ---
        gc.setFill(Color.rgb(30, 28, 25, 0.9));
        gc.fillRoundRect(barX, barY + 10, barW, barH, 8, 8);
        gc.setStroke(Color.rgb(90, 85, 70));
        gc.setLineWidth(2);
        gc.strokeRoundRect(barX, barY + 10, barW, barH, 8, 8);
        // Inner bevel
        gc.setStroke(Color.rgb(55, 50, 40));
        gc.setLineWidth(1);
        gc.strokeRoundRect(barX + 2, barY + 12, barW - 4, barH - 4, 6, 6);

        // --- Inventory slots ---
        int slotCount = 6;
        double slotSize = 40;
        double slotsW = slotCount * (slotSize + 4);
        double slotStartX = canvasW / 2 - slotsW / 2;
        double slotY = barY + 16;
        for (int i = 0; i < slotCount; i++) {
            double sx = slotStartX + i * (slotSize + 4);
            gc.setFill(Color.rgb(15, 14, 12, 0.85));
            gc.fillRect(sx, slotY, slotSize, slotSize);
            gc.setStroke(Color.rgb(60, 55, 45));
            gc.setLineWidth(1.5);
            gc.strokeRect(sx, slotY, slotSize, slotSize);
        }

        // --- LEFT ORB (HEALTH — red) ---
        double orbLeftX = barX - orbRadius - 8;
        double orbLeftY = barY + barH / 2 + 10;
        drawOrb(
                gc,
                orbLeftX,
                orbLeftY,
                orbRadius,
                Color.rgb(180, 30, 30),
                Color.rgb(100, 10, 10),
                Color.rgb(40, 5, 5),
                (double) player.getHealth() / player.getMaxHealth());
        // HP text inside orb
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(player.getHealth() + "%", orbLeftX, orbLeftY + 5);

        // --- RIGHT ORB (SANITY — green) ---
        double orbRightX = barX + barW + orbRadius + 8;
        double orbRightY = barY + barH / 2 + 10;
        drawOrb(
                gc,
                orbRightX,
                orbRightY,
                orbRadius,
                Color.rgb(30, 180, 60),
                Color.rgb(10, 100, 25),
                Color.rgb(5, 40, 10),
                sanity / 100.0);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText((int) sanity + "%", orbRightX, orbRightY + 5);

        // ======= LEVEL INDICATOR (top center above bar) =======
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        gc.setFill(Color.rgb(200, 190, 140, 0.9));
        gc.fillText("Рівень " + currentLevel + " / " + MAX_LEVELS, canvasW / 2, barY + 6);

        // ======= BOTTOM-RIGHT: PORTRAIT + BARS =======
        double panelX = canvasW - 230;
        double panelY = canvasH - 75;
        double portraitSize = 50;

        // Portrait circle frame
        gc.setFill(Color.rgb(25, 23, 20, 0.9));
        gc.fillOval(panelX - 5, panelY - 5, portraitSize + 10, portraitSize + 10);
        gc.setStroke(Color.rgb(100, 95, 75));
        gc.setLineWidth(3);
        gc.strokeOval(panelX - 5, panelY - 5, portraitSize + 10, portraitSize + 10);
        // Inner circle (placeholder face)
        gc.setFill(Color.rgb(45, 42, 38));
        gc.fillOval(panelX, panelY, portraitSize, portraitSize);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        gc.setFill(Color.rgb(160, 155, 130));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("P1", panelX + portraitSize / 2, panelY + portraitSize / 2 + 5);

        // Health bar (right of portrait)
        double barsX = panelX + portraitSize + 12;
        double barsW = 150;
        double hpH = 14;
        double sanH = 10;
        double hpPercent = (double) player.getHealth() / player.getMaxHealth();
        double sanPercent = sanity / 100.0;

        // HP bar
        gc.setFill(Color.rgb(20, 18, 15, 0.85));
        gc.fillRoundRect(barsX, panelY + 5, barsW, hpH, 4, 4);
        gc.setFill(hpPercent > 0.3 ? Color.rgb(200, 45, 40) : Color.rgb(255, 100, 30));
        gc.fillRoundRect(barsX, panelY + 5, barsW * hpPercent, hpH, 4, 4);
        gc.setStroke(Color.rgb(80, 75, 60));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(barsX, panelY + 5, barsW, hpH, 4, 4);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(
                player.getHealth() + " / " + player.getMaxHealth(),
                barsX + barsW / 2,
                panelY + 5 + 11);

        // Sanity bar
        gc.setFill(Color.rgb(20, 18, 15, 0.85));
        gc.fillRoundRect(barsX, panelY + 5 + hpH + 6, barsW, sanH, 4, 4);
        gc.setFill(Color.rgb(50, 160, 220));
        gc.fillRoundRect(barsX, panelY + 5 + hpH + 6, barsW * sanPercent, sanH, 4, 4);
        gc.setStroke(Color.rgb(80, 75, 60));
        gc.setLineWidth(1);
        gc.strokeRoundRect(barsX, panelY + 5 + hpH + 6, barsW, sanH, 4, 4);

        // ======= DASH INDICATOR (bottom-left) =======
        double dashX = 20;
        double dashY = canvasH - 40;
        double dashW = 80;
        double dashH = 6;
        gc.setFill(Color.rgb(20, 18, 15, 0.7));
        gc.fillRoundRect(dashX, dashY, dashW, dashH, 3, 3);
        if (dashCooldown > 0) {
            gc.setFill(Color.rgb(0, 180, 230, 0.6));
            gc.fillRoundRect(dashX, dashY, dashW * (1 - dashCooldown / DASH_COOLDOWN), dashH, 3, 3);
        } else {
            gc.setFill(Color.rgb(0, 220, 255, 0.9));
            gc.fillRoundRect(dashX, dashY, dashW, dashH, 3, 3);
        }
        gc.setStroke(Color.rgb(0, 180, 230, 0.5));
        gc.setLineWidth(1);
        gc.strokeRoundRect(dashX, dashY, dashW, dashH, 3, 3);
        gc.setFont(Font.font("System", 9));
        gc.setFill(Color.rgb(0, 210, 255, 0.7));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("DASH", dashX, dashY - 4);

        // Controls hint
        gc.setFont(Font.font("System", 10));
        gc.setFill(Color.rgb(180, 180, 180, 0.4));
        gc.fillText(
                "A/D - рух | SPACE - стрибок | SHIFT - ривок | E - взаємодія | ESC - пауза",
                dashX,
                canvasH - 8);

        gc.restore();
    }

    /** Draw a 3D looking orb (sphere) with gradient and glow */
    private void drawOrb(
            GraphicsContext gc,
            double cx,
            double cy,
            double r,
            Color bright,
            Color mid,
            Color dark,
            double fillRatio) {
        // Outer ring / frame
        gc.setFill(Color.rgb(50, 47, 40));
        gc.fillOval(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2);
        gc.setStroke(Color.rgb(90, 85, 70));
        gc.setLineWidth(3);
        gc.strokeOval(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2);

        // Dark inner background
        gc.setFill(dark);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Filled portion (bottom-up fill based on ratio)
        if (fillRatio > 0) {
            gc.save();
            double fillH = r * 2 * fillRatio;
            double clipY = cy + r - fillH;
            gc.beginPath();
            gc.rect(cx - r, clipY, r * 2, fillH);
            gc.clip();
            gc.setFill(mid);
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);
            gc.restore();
        }

        // Specular highlight (top-left)
        gc.setGlobalAlpha(0.35);
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - r * 0.55, cy - r * 0.7, r * 0.7, r * 0.5);
        gc.setGlobalAlpha(1.0);

        // Glass rim
        gc.setStroke(Color.rgb(30, 28, 22));
        gc.setLineWidth(2);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawMiniMap(GraphicsContext gc) {
        int miniSize = 2;
        int mapW = jungleMap.getWidth();
        int mapH = jungleMap.getHeight();
        double miniX = gameCanvas.getWidth() - mapW * miniSize - 15;
        double miniY = 15;

        // Background
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillRoundRect(miniX - 4, miniY - 4, mapW * miniSize + 8, mapH * miniSize + 8, 6, 6);

        for (int x = 0; x < mapW; x++) {
            for (int y = 0; y < mapH; y++) {
                TileType t = jungleMap.getTile(x, y);
                if (t == null) continue;

                switch (t) {
                    case GROUND:
                        gc.setFill(Color.rgb(80, 50, 30));
                        break;
                    case FLOATING_PLATFORM:
                        gc.setFill(Color.SIENNA);
                        break;
                    case DECORATION:
                        gc.setFill(Color.rgb(40, 100, 30));
                        break;
                    case SPIKES:
                        gc.setFill(Color.rgb(200, 60, 60));
                        break;
                    case COCKPIT_WRECKAGE:
                        gc.setFill(Color.SILVER);
                        break;
                    default:
                        gc.setFill(Color.rgb(60, 60, 60));
                        break;
                }
                gc.fillRect(miniX + x * miniSize, miniY + y * miniSize, miniSize, miniSize);
            }
        }

        // Enemy dots
        gc.setFill(Color.RED);
        for (Enemy enemy : enemies) {
            int etx = (int) (enemy.x / TILE_SIZE);
            int ety = (int) (enemy.y / TILE_SIZE);
            gc.fillRect(miniX + etx * miniSize, miniY + ety * miniSize, miniSize + 1, miniSize + 1);
        }

        // Player dot
        int ptx = (int) (player.getX() / TILE_SIZE);
        int pty = (int) (player.getY() / TILE_SIZE);
        gc.setFill(Color.rgb(50, 200, 50));
        gc.fillOval(miniX + ptx * miniSize - 1, miniY + pty * miniSize - 1, 4, 4);

        // Border
        gc.setStroke(Color.rgb(200, 200, 200, 0.3));
        gc.setLineWidth(1);
        gc.strokeRoundRect(miniX - 4, miniY - 4, mapW * miniSize + 8, mapH * miniSize + 8, 6, 6);
    }

    private void drawGameOver(GraphicsContext gc, double canvasW, double canvasH) {
        double alpha = Math.min(0.8, gameOverTimer * 0.5);
        gc.setFill(Color.rgb(0, 0, 0, alpha));
        gc.fillRect(0, 0, canvasW, canvasH);

        if (gameOverTimer > 0.5) {
            gc.setFill(Color.rgb(220, 50, 50));
            gc.setFont(Font.font("Impact", FontWeight.BOLD, 72));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("GAME OVER", canvasW / 2, canvasH / 2 - 40);

            gc.setFill(Color.rgb(200, 200, 200));
            gc.setFont(Font.font("System", 20));
            gc.fillText("Натисни [R] щоб спробувати знову", canvasW / 2, canvasH / 2 + 10);
        }
    }

    private void drawLevelComplete(GraphicsContext gc, double canvasW, double canvasH) {
        double alpha = Math.min(0.75, levelCompleteTimer * 0.6);
        gc.setFill(Color.rgb(10, 30, 10, alpha));
        gc.fillRect(0, 0, canvasW, canvasH);

        if (levelCompleteTimer > 0.4) {
            // Title
            gc.setFont(Font.font("Impact", FontWeight.BOLD, 64));
            gc.setTextAlign(TextAlignment.CENTER);
            // Pulsing glow
            double glow = 0.6 + 0.4 * Math.sin(levelCompleteTimer * 5);
            gc.setFill(Color.rgb(50, 230, 100, glow));
            gc.fillText("РІВЕНЬ " + currentLevel + " ПРОЙДЕНО!", canvasW / 2, canvasH / 2 - 40);

            gc.setFill(Color.rgb(200, 200, 200, 0.9));
            gc.setFont(Font.font("System", 22));
            if (currentLevel < MAX_LEVELS) {
                gc.fillText("[ENTER / SPACE] → Наступний рівень", canvasW / 2, canvasH / 2 + 20);
            } else {
                gc.setFill(Color.rgb(255, 220, 80, 0.9));
                gc.fillText("🏝 Ти вижив на острові! Вітаємо!", canvasW / 2, canvasH / 2 + 20);
                gc.setFont(Font.font("System", 16));
                gc.setFill(Color.rgb(180, 180, 180, 0.7));
                gc.fillText("[R] — зіграти знову", canvasW / 2, canvasH / 2 + 55);
            }
        }
    }

    private void restartGame() {
        gameOver = false;
        gameOverTimer = 0;
        levelComplete = false;
        levelCompleteTimer = 0;
        sanity = 100;
        jumpsUsed = 0;
        isDashing = false;
        dashCooldown = 0;
        loadLevel(currentLevel);
    }

    private void loadNextLevel() {
        if (currentLevel < MAX_LEVELS) {
            currentLevel++;
            System.out.println("→ Loading level " + currentLevel + "...");
            loadLevel(currentLevel);
        } else {
            // Restart from level 1
            currentLevel = 1;
            System.out.println("→ Restarting from level 1...");
            loadLevel(currentLevel);
        }
    }

    private void loadLevel(int levelNum) {
        String path = "/assets/levels/level" + levelNum + ".tmx";
        JungleMap newMap = new JungleMap(path);

        // If TMX failed (no solid tiles), keep current map
        if (newMap.getWidth() > 0) {
            jungleMap = newMap;
        }

        double spawnX = jungleMap.getSpawnX() * TILE_SIZE;
        double spawnY = (jungleMap.getSpawnY() - 1) * TILE_SIZE;

        if (player == null) {
            player = new GamePlayer(0, spawnX, spawnY);
        }
        player.setSpawnPosition(spawnX, spawnY);
        player.fullReset();

        sanity = 100;
        jumpsUsed = 0;
        isDashing = false;
        dashCooldown = 0;
        gameOver = false;
        gameOverTimer = 0;
        levelComplete = false;
        levelCompleteTimer = 0;
        missionComplete = false;
        cameraX = 0;
        cameraY = 0;

        initEnemiesFromMap();
        initItemsFromMap();

        // Spawn Kate for Level 1
        kateExists = false;
        kateTalkedTo = false;
        if (levelNum == 1) {
            spawnKateNPC();
        }
        System.out.println("Loaded level " + levelNum + " spawn=(" + spawnX + "," + spawnY + ")");
    }

    // --- TILE DRAWING ---
    private void drawTileFromTileset(
            GraphicsContext gc, Image tileset, int tileX, int tileY, double drawX, double drawY) {
        if (isValidImage(tileset)) {
            gc.drawImage(
                    tileset,
                    tileX * TILE_SRC,
                    tileY * TILE_SRC,
                    TILE_SRC,
                    TILE_SRC,
                    drawX,
                    drawY,
                    TILE_SIZE,
                    TILE_SIZE);
        }
    }

    // --- COLLISION ---
    private boolean collidesAt(double px, double py) {
        double left = px;
        double right = px + PLAYER_W;
        double top = py;
        double bottom = py + PLAYER_H;

        int minTx = (int) Math.floor(left / TILE_SIZE);
        int maxTx = (int) Math.floor((right - 0.01) / TILE_SIZE);
        int minTy = (int) Math.floor(top / TILE_SIZE);
        int maxTy = (int) Math.floor((bottom - 0.01) / TILE_SIZE);

        for (int tx = minTx; tx <= maxTx; tx++) {
            for (int ty = minTy; ty <= maxTy; ty++) {
                if (jungleMap.isSolid(tx, ty)) return true;
            }
        }
        return false;
    }

    private boolean isTouchingHazard() {
        double left = player.getX();
        double right = player.getX() + PLAYER_W;
        double top = player.getY();
        double bottom = player.getY() + PLAYER_H;

        int minTx = (int) Math.floor(left / TILE_SIZE);
        int maxTx = (int) Math.floor((right - 0.01) / TILE_SIZE);
        int minTy = (int) Math.floor(top / TILE_SIZE);
        int maxTy = (int) Math.floor((bottom - 0.01) / TILE_SIZE);

        for (int tx = minTx; tx <= maxTx; tx++) {
            for (int ty = minTy; ty <= maxTy; ty++) {
                if (jungleMap.isHazard(tx, ty)) return true;
            }
        }
        return false;
    }

    private boolean rectsOverlap(
            double ax,
            double ay,
            double aw,
            double ah,
            double bx,
            double by,
            double bw,
            double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    // --- INTERACTION ---
    private void checkProximity() {
        // Check proximity to cockpit / level exit
        int cx = jungleMap.getCockpitX();
        int cy = jungleMap.getCockpitY();
        double tileX = cx * TILE_SIZE;
        double tileY = cy * TILE_SIZE;

        boolean nearCockpit =
                rectsOverlap(
                        player.getX() - 32,
                        player.getY() - 32,
                        PLAYER_W + 64,
                        PLAYER_H + 64,
                        tileX,
                        tileY,
                        TILE_SIZE,
                        TILE_SIZE);

        // Check proximity to Kate NPC
        boolean nearKate = kateExists && !kateTalkedTo && isNearKate();

        if ((nearCockpit && !missionComplete) || nearKate) {
            if (interactLabel != null) {
                interactLabel.setText(
                        nearKate ? "Натисни [E] щоб поговорити" : "Натисни [E] для взаємодії");
                interactLabel.setVisible(true);
            }
        } else if (interactLabel != null) {
            interactLabel.setVisible(false);
        }
    }

    private boolean isNearKate() {
        if (!kateExists) return false;
        double dx = Math.abs((player.getX() + PLAYER_W / 2) - (kateX + KATE_W / 2));
        double dy = Math.abs((player.getY() + PLAYER_H / 2) - (kateY + KATE_H / 2));
        return dx < KATE_INTERACT_RANGE && dy < KATE_INTERACT_RANGE;
    }

    private void tryInteract() {
        // Try talking to Kate first
        if (kateExists && !kateTalkedTo && isNearKate()) {
            kateTalkedTo = true;
            startLevel1Cutscene();
            return;
        }

        if (missionComplete) return;

        int cx = jungleMap.getCockpitX();
        int cy = jungleMap.getCockpitY();
        double tileX = cx * TILE_SIZE;
        double tileY = cy * TILE_SIZE;

        if (rectsOverlap(
                player.getX(),
                player.getY(),
                PLAYER_W,
                PLAYER_H,
                tileX - 32,
                tileY - 32,
                TILE_SIZE + 64,
                TILE_SIZE + 64)) {
            player.addItem("transceiver");
            missionComplete = true;

            // Trigger level complete
            levelComplete = true;
            levelCompleteTimer = 0;
            System.out.println("✓ Level " + currentLevel + " completed!");
            showMission(
                    currentLevel < MAX_LEVELS
                            ? "📡 Трансивер знайдено! Натисни ENTER → наступний рівень"
                            : "🏝 Ти вижив! Гра пройдена!");
        }
    }

    // --- KATE NPC ---
    private void spawnKateNPC() {
        // Place Kate 3 tiles to the right of player spawn
        double spawnX = jungleMap.getSpawnX() * TILE_SIZE + 3 * TILE_SIZE;
        double spawnY = jungleMap.getSpawnY() * TILE_SIZE - KATE_H + 82;
        kateX = spawnX;
        kateY = spawnY;
        kateExists = true;
        kateTalkedTo = false;
        System.out.println("✓ Kate NPC spawned at (" + kateX + ", " + kateY + ")");
    }

    private void drawKateNPC(GraphicsContext gc, double canvasW, double canvasH) {
        if (!kateExists) return;

        double kx = kateX - cameraX;
        double ky = kateY - cameraY;

        // Skip if off screen
        if (kx < -100 || kx > canvasW + 100 || ky < -100 || ky > canvasH + 100) return;

        // Gentle breathing animation
        double breathe = Math.sin(System.currentTimeMillis() / 800.0) * 2;

        double x = kx;
        double y = ky + breathe;

        // Try to use sprite first, fallback to programmatic drawing
        if (isValidImage(kateIdleSprite)) {
            gc.drawImage(kateIdleSprite, x, y, KATE_W, KATE_H);
        } else {
            // --- Programmatic fallback: Draw Kate as a character ---

            // Hair (brown, messy ponytail)
            gc.setFill(Color.rgb(101, 67, 33));
            gc.fillOval(x + 10, y - 2, 28, 28); // Main hair
            gc.fillRoundRect(x + 28, y + 4, 14, 18, 6, 6); // Ponytail
            gc.setFill(Color.rgb(80, 50, 25));
            gc.fillOval(x + 30, y + 2, 10, 10); // Ponytail knot

            // Head (skin tone)
            gc.setFill(Color.rgb(222, 184, 150));
            gc.fillOval(x + 13, y + 4, 22, 22); // Face

            // Eyes
            gc.setFill(Color.rgb(60, 100, 60)); // Green eyes
            gc.fillOval(x + 18, y + 12, 5, 5);
            gc.fillOval(x + 26, y + 12, 5, 5);
            gc.setFill(Color.BLACK);
            gc.fillOval(x + 19, y + 13, 3, 3);
            gc.fillOval(x + 27, y + 13, 3, 3);

            // Eyebrows
            gc.setStroke(Color.rgb(80, 50, 25));
            gc.setLineWidth(1.5);
            gc.strokeLine(x + 17, y + 10, x + 23, y + 11);
            gc.strokeLine(x + 25, y + 11, x + 31, y + 10);

            // Mouth
            gc.setStroke(Color.rgb(180, 100, 80));
            gc.setLineWidth(1);
            gc.strokeLine(x + 21, y + 20, x + 27, y + 20);

            // Neck
            gc.setFill(Color.rgb(210, 170, 140));
            gc.fillRect(x + 20, y + 24, 8, 6);

            // Tank top (dirty grey)
            gc.setFill(Color.rgb(160, 160, 155));
            gc.fillRoundRect(x + 12, y + 28, 24, 22, 4, 4);
            // Dirt stains on top
            gc.setFill(Color.rgb(130, 125, 110));
            gc.fillOval(x + 16, y + 32, 6, 4);
            gc.fillOval(x + 26, y + 36, 5, 3);
            // Straps
            gc.setFill(Color.rgb(140, 140, 135));
            gc.fillRect(x + 14, y + 26, 5, 6);
            gc.fillRect(x + 29, y + 26, 5, 6);

            // Arms (skin)
            gc.setFill(Color.rgb(215, 178, 145));
            gc.fillRoundRect(x + 6, y + 30, 8, 20, 4, 4); // Left arm
            gc.fillRoundRect(x + 34, y + 30, 8, 20, 4, 4); // Right arm
            // Dirt on arms
            gc.setFill(Color.rgb(185, 150, 120));
            gc.fillOval(x + 8, y + 38, 4, 3);
            gc.fillOval(x + 36, y + 42, 3, 3);

            // Cargo pants (olive green)
            gc.setFill(Color.rgb(95, 110, 70));
            gc.fillRoundRect(x + 12, y + 48, 11, 20, 3, 3); // Left leg
            gc.fillRoundRect(x + 25, y + 48, 11, 20, 3, 3); // Right leg
            // Belt
            gc.setFill(Color.rgb(80, 60, 40));
            gc.fillRect(x + 12, y + 48, 24, 3);
            // Belt buckle
            gc.setFill(Color.rgb(180, 160, 100));
            gc.fillRect(x + 22, y + 48, 4, 3);
            // Cargo pockets
            gc.setStroke(Color.rgb(75, 90, 55));
            gc.setLineWidth(1);
            gc.strokeRect(x + 14, y + 56, 7, 5);
            gc.strokeRect(x + 27, y + 56, 7, 5);

            // Boots (dark brown)
            gc.setFill(Color.rgb(60, 40, 25));
            gc.fillRoundRect(x + 10, y + 66, 13, 6, 3, 3); // Left boot
            gc.fillRoundRect(x + 25, y + 66, 13, 6, 3, 3); // Right boot

            // --- Character outline for pixel-art feel ---
            gc.setStroke(Color.rgb(30, 20, 10));
            gc.setLineWidth(1.2);
            // Body outline
            gc.strokeRoundRect(x + 12, y + 28, 24, 22, 4, 4);
            gc.strokeRoundRect(x + 12, y + 48, 11, 20, 3, 3);
            gc.strokeRoundRect(x + 25, y + 48, 11, 20, 3, 3);
        } // end else (programmatic fallback)

        // --- Name label above Kate ---
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setFill(Color.rgb(255, 220, 80));
        gc.fillText("Кейт", x + KATE_W / 2, y - 12);

        // Draw interact prompt if near and not talked to
        if (!kateTalkedTo && isNearKate()) {
            gc.setFont(Font.font("System", FontWeight.NORMAL, 11));
            gc.setFill(Color.rgb(200, 200, 200, 0.8));
            gc.fillText("[E]", x + KATE_W / 2, y - 26);
        }
    }

    private void showMission(String text) {
        if (missionLabel != null) {
            missionLabel.setText(text);
            missionLabel.setVisible(true);
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> missionLabel.setVisible(false));
            pause.play();
        }
    }

    // --- INNER CLASSES ---

    /** Simple enemy that patrols left-right */
    private static class Enemy {
        double x, y;
        double w = 40, h = 40;
        double speed = 80;
        boolean movingRight = true;
        double minX, maxX;
        boolean isGhost = true;
        int spriteVariant = 0; // which ghost sprite to show
        int animDirection = 1; // +1 forward, -1 backward (ping-pong)
        double animTimer = 0; // timer for switching frames
        static final double ANIM_SPEED = 0.4; // seconds per frame change

        Enemy(double x, double y, double minX, double maxX) {
            this.x = x;
            this.y = y;
            this.minX = minX;
            this.maxX = maxX;
            // Each ghost gets a unique starting sprite
            this.spriteVariant = (int) (Math.abs(x * 7 + y * 13)) % 5;
        }

        void update(double dt, JungleMap map, int tileSize) {
            // Animate: cycle through sprite variants
            animTimer += dt;
            if (animTimer >= ANIM_SPEED) {
                animTimer -= ANIM_SPEED;
                spriteVariant += animDirection;
                if (spriteVariant >= 5) {
                    spriteVariant = 5;
                    animDirection = -1;
                } else if (spriteVariant <= 0) {
                    spriteVariant = 0;
                    animDirection = 1;
                }
            }

            double dx = speed * dt * (movingRight ? 1 : -1);
            x += dx;

            if (isGhost) {
                // Ghosts float freely — ignore walls, only respect patrol bounds
                if (x >= maxX) movingRight = false;
                if (x <= minX) movingRight = true;
                // No gravity for ghosts
            } else {
                // Reverse at patrol boundaries or walls
                if (x >= maxX || checkWall(x + w, y, map, tileSize)) {
                    movingRight = false;
                }
                if (x <= minX || checkWall(x, y, map, tileSize)) {
                    movingRight = true;
                }

                // Simple gravity — snap to ground
                int tileBelow = (int) ((y + h + 4) / tileSize);
                int tileX = (int) ((x + w / 2) / tileSize);
                if (!map.isSolid(tileX, tileBelow)) {
                    y += 200 * dt; // fall
                } else {
                    y = (tileBelow) * tileSize - h;
                }
            }
        }

        private boolean checkWall(double cx, double cy, JungleMap map, int tileSize) {
            int tx = (int) (cx / tileSize);
            int ty = (int) (cy / tileSize);
            return map.isSolid(tx, ty);
        }
    }

    // --- HUD ACTION HANDLERS ---
    @FXML
    private void resumeGame() {
        togglePauseMenu();
    }

    @FXML
    private void saveGame() {
        System.out.println("Game saved!");
        // Save session to DB
        try {
            GameSessionDao dao = new GameSessionDao(LostDatabaseApp.getConnectionPool());
            GameSession session = new GameSession();
            session.setSessionCode("SAVE-" + System.currentTimeMillis() % 100000);
            session.setHostPlayerId(dbPlayer != null ? dbPlayer.getId() : 1L);
            session.setMaxPlayers(1);
            session.setStatus("SAVED | LVL1");
            dao.save(session);
        } catch (Exception e) {
            System.err.println("Save error: " + e.getMessage());
        }
        showMission("💾 Гру збережено!");
        togglePauseMenu();
    }

    public void setDbPlayer(Player player) {
        this.dbPlayer = player;
    }

    @FXML
    private void exitToMenu() {
        System.out.println("Exiting to Lobby...");
        if (gameLoop != null) gameLoop.stop();
        javafx.application.Platform.runLater(
                () -> {
                    try {
                        LobbyController lobby = new LobbyController();
                        Player p = dbPlayer != null ? dbPlayer : new Player();
                        if (p.getUsername() == null) p.setUsername("Player");
                        javafx.scene.layout.StackPane lobbyRoot = lobby.buildView(p);
                        gameCanvas.getScene().setRoot(lobbyRoot);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void togglePauseMenu() {
        isPaused = !isPaused;
        if (pauseMenuOverlay != null) {
            pauseMenuOverlay.setVisible(isPaused);
        }
    }
}
