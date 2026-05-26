package com.lost.database.controller;

import com.lost.database.app.LostDatabaseApp;
import com.lost.database.dao.GameSaveDao;
import com.lost.database.entity.GameSave;
import com.lost.database.entity.Player;
import com.lost.database.game.entity.GamePlayer;
import com.lost.database.game.world.JungleMap;
import com.lost.database.game.world.TileMapRenderer;
import com.lost.database.game.world.TileType;
import com.lost.database.infrastructure.OnlineService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
    private Image antennaImage; // The big tower on the map
    private Image itemAntennaImage; // The small part for the inventory
    private Image itemMedkitImage; // Medkit icon
    private Image spikesImage;

    // NPC Portraits
    private Image jackPortrait;
    private Image katePortrait;
    private Image sayidPortrait;

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
    private int maxLevelReached = 1;
    private static final int MAX_LEVELS = 4;
    private boolean levelComplete = false;
    private double levelCompleteTimer = 0;
    private boolean gameWon = false;
    
    // --- FINAL LEVEL MECHANICS ---
    private boolean isTerminalActive = false;
    private StringBuilder terminalInput = new StringBuilder();
    private boolean terminalError = false;
    private double terminalErrorTimer = 0;
    private Image teleportSprite;
    private Image hatchSprite;
    private Image terminalBgSprite;
    private Image terminalPasswordPanelImage;
    private Image bunkerTileset;
    private Image bunkerWallBg;
    private boolean canInteractTeleport = false;
    private boolean canInteractHatch = false;

    // Sprite render size (visual)
    private static final double SPRITE_RENDER_W = 40;
    private static final double SPRITE_RENDER_H = 40;

    // --- ENEMY SYSTEM ---
    private List<Enemy> enemies = new ArrayList<>();

    // --- MULTIPLAYER ---
    private List<com.lost.database.game.entity.RemotePlayer> remotePlayers = new ArrayList<>();
    private com.lost.database.infrastructure.MultiplayerService multiplayerService = com.lost.database.infrastructure.MultiplayerService.getInstance();
    private double multiplayerSyncTimer = 0;

    // --- ITEM SYSTEM ---
    private List<ItemDrop> items = new ArrayList<>();

    // --- NPC SYSTEM (universal) ---
    private Image kateIdleSprite;
    private Image sayidIdleSprite;
    private Image benIdleSprite;
    private Image npcCurrentSprite; // currently active NPC sprite
    private String npcName = "Кейт";
    private double kateX, kateY;
    private boolean kateExists = false;
    private boolean kateTalkedTo = false;
    private static final double KATE_W = 80;
    private static final double KATE_H = 120;
    private static final double KATE_INTERACT_RANGE = 100;
    // NPC appearance colors (change per level)
    private Color npcHairColor = Color.rgb(101, 67, 33);
    private Color npcSkinColor = Color.rgb(222, 184, 150);
    private Color npcShirtColor = Color.rgb(160, 160, 155);
    private Color npcPantsColor = Color.rgb(95, 110, 70);
    private Color npcEyeColor = Color.rgb(60, 100, 60);

    // --- GAME STATE ---
    private boolean isPaused = false;
    private boolean gameOver = false;
    private double gameOverTimer = 0;
    private double damageFlashTimer = 0;
    private double sanity = 100.0;
    private double gameElapsedTime = 0; // Час гри в секундах (для score)
    private double attackTimer = 0.0;
    private java.util.Set<Integer> locallyKilledEnemies = new java.util.HashSet<>();
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
        bunkerTileset = safeLoad("/assets/levels/Platformer_Dungeon Asset Pack/TileSet/TileSet.png");
        bunkerWallBg = safeLoad("/assets/levels/Platformer_Dungeon Asset Pack/Wall/Wall.png");
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
        antennaImage = safeLoad("/images/radio_tower.png");
        itemAntennaImage = safeLoad("/images/item_antenna.png");
        itemMedkitImage = safeLoad("/images/item_medkit.png");
        spikesImage = safeLoad("/images/jungle_spikes.png");

        // NPC Portraits
        jackPortrait = safeLoad("/assets/sprites/sayid_serious.png");
        katePortrait = safeLoad("/assets/sprites/dialog/kate_portrait.png");

        // Kate idle sprite for world rendering
        kateIdleSprite = safeLoad("/assets/sprites/dialog/kate_idle.png");
        sayidIdleSprite = safeLoad("/assets/sprites/sayid_idle.png");
        benIdleSprite = safeLoad("/assets/sprites/ben_idle.png");
        
        // Final Level Objects
        teleportSprite = safeLoad("/assets/sprites/teleport.png");
        hatchSprite = safeLoad("/assets/sprites/hatch.png");
        terminalBgSprite = safeLoad("/assets/sprites/terminal.png");
        terminalPasswordPanelImage = safeLoad("/images/terminal_password_panel.png");

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
        gameCanvas.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                triggerAttack();
            }
        });

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

        // Spawn NPC for current level
        spawnNpcForLevel(currentLevel);
        
        // Restore save state if loaded from save
        javafx.application.Platform.runLater(this::applyPendingSave);
    }

    // --- DIALOGUE SYSTEM ---
    private void startLevelCutscene() {
        List<String[]> dialog = new ArrayList<>();
        switch (currentLevel) {
            case 1:
                dialog.add(
                        new String[] {
                            "Кейт",
                            "kate",
                            "Гей, обережніше там! Дехто з тих, хто пішов за водою, так і не повернувся."
                        });
                dialog.add(
                        new String[] {"Кейт", "kate", "Кажуть, вони бачили... щось серед дерев."});
                dialog.add(
                        new String[] {
                            "Гравець",
                            "jack",
                            "Я мушу знайти кабіну пілотів, Кейт. Без трансивера ми тут назавжди."
                        });
                dialog.add(
                        new String[] {
                            "Кейт",
                            "kate",
                            "Тримай очі відкритими. Якщо почуєш дивні звуки — краще біжи."
                        });
                break;
            case 2:
                dialog.add(
                        new String[] {
                            "Саїд",
                            "sayid",
                            "Ти знайшов трансивер? Добре. Але він зламаний — без антени марний."
                        });
                dialog.add(
                        new String[] {
                            "Саїд", "sayid", "Я обстежив територію на півночі. Там є щось дивне..."
                        });
                dialog.add(
                        new String[] {
                            "Саїд",
                            "sayid",
                            "Металевий люк у землі. Він веде кудись під землю. Можливо, там є обладнання."
                        });
                dialog.add(new String[] {"Гравець", "jack", "Люк? Що ще за люк посеред джунглів?"});
                dialog.add(
                        new String[] {
                            "Саїд",
                            "sayid",
                            "Не знаю. Але на ньому вигравіювані цифри. Будь обережний."
                        });
                break;
            case 3:
                dialog.add(new String[] {"Бен", "ben", "Стій. Ти не повинен був сюди потрапити."});
                dialog.add(new String[] {"Гравець", "jack", "Хто ти? Ти живеш тут?"});
                dialog.add(
                        new String[] {
                            "Бен",
                            "ben",
                            "Цей острів — особливе місце. Він не відпускає тих, хто сюди потрапив."
                        });
                dialog.add(
                        new String[] {
                            "Бен",
                            "ben",
                            "Бачиш той комп'ютер? Кожні 108 хвилин хтось має ввести числа. Інакше... все закінчиться."
                        });
                dialog.add(new String[] {"Гравець", "jack", "Які числа?"});
                dialog.add(
                        new String[] {
                            "Бен",
                            "ben",
                            "4... 8... 15... 16... 23... 42. Тільки в правильному порядку."
                        });
                break;
        }
        startDialogue(dialog);
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
        } else if (portraitKey.equals("sayid")) {
            portraitImage.setImage(sayidPortrait != null ? sayidPortrait : jackPortrait);
            portraitImage.setVisible(true);
        } else if (portraitKey.equals("ben")) {
            portraitImage.setImage(jackPortrait);
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
                    enemies.add(new Enemy(px, py, px - 3 * TILE_SIZE, px + 3 * TILE_SIZE, false));
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

    private void spawnItemsForLevel(int level) {
        // Scatter some medkits across the level
        switch (level) {
            case 1:
                items.add(new ItemDrop(20 * TILE_SIZE, 7 * TILE_SIZE, "health_pack", 30));
                items.add(new ItemDrop(45 * TILE_SIZE, 7 * TILE_SIZE, "health_pack", 30));
                break;
            case 2:
                items.add(new ItemDrop(15 * TILE_SIZE, 7 * TILE_SIZE, "health_pack", 30));
                items.add(new ItemDrop(35 * TILE_SIZE, 4 * TILE_SIZE, "health_pack", 30));
                items.add(new ItemDrop(55 * TILE_SIZE, 7 * TILE_SIZE, "health_pack", 30));
                break;
            case 3:
                items.add(new ItemDrop(10 * TILE_SIZE, 7 * TILE_SIZE, "health_pack", 30));
                items.add(new ItemDrop(30 * TILE_SIZE, 3 * TILE_SIZE, "health_pack", 30));
                items.add(new ItemDrop(50 * TILE_SIZE, 7 * TILE_SIZE, "health_pack", 30));
                break;
        }
    }

    private void spawnGhostsForLevel(int level) {
        int mapW = jungleMap.getWidth() * TILE_SIZE;

        switch (level) {
            case 1:
                // level 1 (5 ghosts)
                enemies.add(
                        new Enemy(15 * TILE_SIZE, 6 * TILE_SIZE, 12 * TILE_SIZE, 22 * TILE_SIZE));
                enemies.add(
                        new Enemy(25 * TILE_SIZE, 4 * TILE_SIZE, 20 * TILE_SIZE, 30 * TILE_SIZE));
                enemies.add(
                        new Enemy(35 * TILE_SIZE, 5 * TILE_SIZE, 30 * TILE_SIZE, 45 * TILE_SIZE));
                enemies.add(
                        new Enemy(45 * TILE_SIZE, 6 * TILE_SIZE, 40 * TILE_SIZE, 50 * TILE_SIZE));
                enemies.add(
                        new Enemy(55 * TILE_SIZE, 7 * TILE_SIZE, 50 * TILE_SIZE, 65 * TILE_SIZE));
                break;
            case 2:
                // level 2 (7 ghosts)
                enemies.add(
                        new Enemy(10 * TILE_SIZE, 4 * TILE_SIZE, 5 * TILE_SIZE, 20 * TILE_SIZE));
                enemies.add(
                        new Enemy(18 * TILE_SIZE, 5 * TILE_SIZE, 14 * TILE_SIZE, 22 * TILE_SIZE));
                enemies.add(
                        new Enemy(25 * TILE_SIZE, 3 * TILE_SIZE, 20 * TILE_SIZE, 35 * TILE_SIZE));
                enemies.add(
                        new Enemy(33 * TILE_SIZE, 4 * TILE_SIZE, 28 * TILE_SIZE, 38 * TILE_SIZE));
                enemies.add(
                        new Enemy(40 * TILE_SIZE, 5 * TILE_SIZE, 35 * TILE_SIZE, 50 * TILE_SIZE));
                enemies.add(
                        new Enemy(55 * TILE_SIZE, 4 * TILE_SIZE, 48 * TILE_SIZE, 60 * TILE_SIZE));
                enemies.add(
                        new Enemy(62 * TILE_SIZE, 6 * TILE_SIZE, 58 * TILE_SIZE, 68 * TILE_SIZE));
                break;
            case 3:
                // level 3 (9 ghosts)
                enemies.add(new Enemy(8 * TILE_SIZE, 3 * TILE_SIZE, 3 * TILE_SIZE, 15 * TILE_SIZE));
                enemies.add(
                        new Enemy(14 * TILE_SIZE, 4 * TILE_SIZE, 10 * TILE_SIZE, 18 * TILE_SIZE));
                enemies.add(
                        new Enemy(20 * TILE_SIZE, 5 * TILE_SIZE, 15 * TILE_SIZE, 28 * TILE_SIZE));
                enemies.add(
                        new Enemy(30 * TILE_SIZE, 2 * TILE_SIZE, 25 * TILE_SIZE, 38 * TILE_SIZE));
                enemies.add(
                        new Enemy(40 * TILE_SIZE, 6 * TILE_SIZE, 35 * TILE_SIZE, 48 * TILE_SIZE));
                enemies.add(
                        new Enemy(45 * TILE_SIZE, 5 * TILE_SIZE, 40 * TILE_SIZE, 50 * TILE_SIZE));
                enemies.add(
                        new Enemy(50 * TILE_SIZE, 4 * TILE_SIZE, 45 * TILE_SIZE, 55 * TILE_SIZE));
                enemies.add(
                        new Enemy(58 * TILE_SIZE, 3 * TILE_SIZE, 53 * TILE_SIZE, 63 * TILE_SIZE));
                enemies.add(
                        new Enemy(65 * TILE_SIZE, 5 * TILE_SIZE, 60 * TILE_SIZE, 70 * TILE_SIZE));
                break;
            case 4:
                // level 4 (6 ghosts protecting the bunker terminal hallway)
                enemies.add(
                        new Enemy(6 * TILE_SIZE, 14 * TILE_SIZE, 3 * TILE_SIZE, 10 * TILE_SIZE));
                enemies.add(
                        new Enemy(14 * TILE_SIZE, 13 * TILE_SIZE, 10 * TILE_SIZE, 18 * TILE_SIZE));
                enemies.add(
                        new Enemy(20 * TILE_SIZE, 14 * TILE_SIZE, 16 * TILE_SIZE, 24 * TILE_SIZE));
                enemies.add(
                        new Enemy(28 * TILE_SIZE, 13 * TILE_SIZE, 24 * TILE_SIZE, 32 * TILE_SIZE));
                enemies.add(
                        new Enemy(36 * TILE_SIZE, 14 * TILE_SIZE, 32 * TILE_SIZE, 40 * TILE_SIZE));
                enemies.add(
                        new Enemy(42 * TILE_SIZE, 13 * TILE_SIZE, 38 * TILE_SIZE, 45 * TILE_SIZE));
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
        // Scatter medkits for the current level (after items.clear)
        spawnItemsForLevel(currentLevel);
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

    private void triggerAttack() {
        if (attackTimer > 0 || gameOver || isPaused || isDialogueActive || isTerminalActive) return;
        attackTimer = 0.15;
        
        // Attack hit check
        double centerX = player.getX() + PLAYER_W / 2.0;
        double centerY = player.getY() + PLAYER_H / 2.0;
        
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.isDead) continue;
            
            double enemyCenterX = enemy.x + enemy.w / 2.0;
            double enemyCenterY = enemy.y + enemy.h / 2.0;
            double dx = enemyCenterX - centerX;
            double dy = Math.abs(enemyCenterY - centerY);
            
            boolean inDirection = facingRight ? (dx > -20 && dx < 110) : (dx < 20 && dx > -110);
            if (inDirection && dy < 60) {
                // Connecting hit
                enemy.health -= 50;
                if (enemy.health <= 0) {
                    enemy.isDead = true;
                    locallyKilledEnemies.add(i);
                    System.out.println("[Attack] Killed enemy at index " + i);
                } else {
                    // Knock back physical enemies slightly on hit
                    if (!enemy.isGhost) {
                        enemy.x += facingRight ? 25 : -25;
                    }
                    System.out.println("[Attack] Hit enemy at index " + i + " health=" + enemy.health);
                }
            }
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

        // Toggle God Mode cheat
        if (e.getCode() == KeyCode.G) {
            player.setGodMode(!player.isGodMode());
            System.out.println("God Mode: " + player.isGodMode());
        }

        if (levelComplete) {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) loadNextLevel();
            return;
        }

        // Intercept input for Terminal
        if (isTerminalActive) {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.E) {
                // Exit terminal view
                isTerminalActive = false;
            } else if (e.getCode() == KeyCode.BACK_SPACE) {
                if (terminalInput.length() > 0) {
                    terminalInput.setLength(terminalInput.length() - 1);
                }
            } else if (e.getCode() == KeyCode.ENTER) {
                // Check code
                if ("4 8 15 16 23 42".equals(terminalInput.toString().trim())) {
                    System.out.println("CODE ACCEPTED. GAME WON!");
                    gameWon = true;
                    showVictoryScreen();
                } else {
                    terminalError = true;
                    terminalErrorTimer = 1.5;
                    terminalInput.setLength(0); // clear input
                }
            } else {
                String text = e.getText();
                if (text != null && text.matches("[0-9 ]")) {
                    terminalInput.append(text);
                }
            }
            return; // Block other inputs
        }

        // Intercept input for dialogue
        if (isDialogueActive) {
            if (e.getCode() == KeyCode.E) {
                advanceDialogue();
            }
            return; // Block other inputs while dialogue is active
        }

        if (e.getCode() == KeyCode.F) {
            triggerAttack();
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
        if (isTerminalActive) {
            if (terminalErrorTimer > 0) terminalErrorTimer -= dt;
            return;
        }

        // Dash cooldown
        if (dashCooldown > 0) dashCooldown -= dt;
        if (landingTimer > 0) landingTimer -= dt;
        if (attackTimer > 0) attackTimer -= dt;
        gameElapsedTime += dt; // Відстежуємо час гри

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

            // Integrate X (ignore slopes for horizontal checks)
            double nextX = player.getX() + player.getVx() * dt;
            if (collidesAtIgnoreSlopes(nextX, player.getY(), player.getVx())) {
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

            // Snap to slope surface after horizontal movement
            if (player.isGrounded() || wasGroundedLastFrame) {
                double snappedY = snapToSlopeY(player.getX(), player.getY());
                if (snappedY != player.getY()) {
                    player.setY(snappedY);
                    player.setGrounded(true);
                }
            }

            // Integrate Y
            double nextY = player.getY() + player.getVy() * dt;
            player.setGrounded(false);

            if (collidesAt(player.getX(), nextY)) {
                if (player.getVy() > 0) {
                    // Check if landing on a slope tile
                    int footTileX = (int) ((player.getX() + PLAYER_W / 2.0) / TILE_SIZE);
                    int footTileY = (int) ((nextY + PLAYER_H) / TILE_SIZE);
                    if (jungleMap.isSlope(footTileX, footTileY)) {
                        double playerCenterX = player.getX() + PLAYER_W / 2.0;
                        double slopeHeight =
                                jungleMap.getSlopeHeight(
                                        footTileX, footTileY, playerCenterX, TILE_SIZE);
                        double slopeTop = (footTileY + 1) * TILE_SIZE - slopeHeight;
                        nextY = slopeTop - PLAYER_H - 0.1;
                    } else {
                        int tileY = (int) ((nextY + PLAYER_H) / TILE_SIZE);
                        nextY = tileY * TILE_SIZE - PLAYER_H - 0.1;
                    }
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
            player.takeDamage(100); // Instant death / extremely high fall damage
            damageFlashTimer = 0.3;
            player.resetToSpawn();
            jumpsUsed = 0;
        }

        // Hazard check (spikes)
        if (isTouchingHazard() && !player.isGodMode()) {
            if (player.takeDamage(50)) { // 50% HP damage for spikes
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

        // Update enemies (fully synchronized via NTP system time)
        double systemGameTime = System.currentTimeMillis() / 1000.0;
        for (Enemy enemy : enemies) {
            if (enemy.isDead) continue;
            enemy.update(dt, jungleMap, TILE_SIZE, systemGameTime);
            if (!player.isInvincible() && !player.isGodMode()
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
                    sanity -= 90 * dt; // Rapid sanity drain in contact with ghosts
                    if (sanity <= 0) {
                        sanity = 0;
                        gameOver = true;
                        gameOverTimer = 0;
                    }
                    // Ghosts now also do physical damage and knockback
                    if (player.takeDamage(25)) { // Doubled damage
                        damageFlashTimer = 0.3;
                        double knockDir = (player.getX() < enemy.x) ? -1 : 1;
                        player.setVx(knockDir * 200);
                        player.setVy(-200);
                        player.setGrounded(false);
                    }
                } else if (player.takeDamage(35)) { // Highly punishing physical enemy damage
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

        // Check if player reached right edge of map → level complete
        if (!levelComplete && !missionComplete) {
            double mapRightEdge = (jungleMap.getWidth() - 3) * TILE_SIZE;
            if (player.getX() >= mapRightEdge) {
                if (currentLevel == 2) {
                    // level 2 waits for teleport 'E' instead of edge walk, so don't auto-complete
                } else if (currentLevel == 3 || currentLevel == 4) {
                    // level 3 and 4 waiting for manual trigger
                } else {
                    missionComplete = true;
                    levelComplete = true;
                    levelCompleteTimer = 0;
                    System.out.println("✓ Level " + currentLevel + " completed!");
                    maxLevelReached = Math.max(maxLevelReached, currentLevel + 1);

                    submitScoreOnline();

                    if (currentLevel == 1) {
                        showMission("📡 Рівень пройдено! Натисни ENTER");
                    } else if (currentLevel == 2) {
                        showMission("🚪 Люк знайдено! Натисни ENTER");
                    } else {
                        showMission("🏝 Сигнал відправлено! Натисни ENTER");
                    }
                }
            }
        }

        // --- MULTIPLAYER SYNC ---
        if (multiplayerService.getCurrentSessionId() != null) {
            multiplayerSyncTimer += dt;
            if (multiplayerSyncTimer >= 0.2) {
                multiplayerSyncTimer = 0;
                int dir = facingRight ? 1 : -1;
                String animStateStr = animState.name();
                if (!locallyKilledEnemies.isEmpty()) {
                    StringBuilder sb = new StringBuilder(animState.name());
                    sb.append(":");
                    boolean first = true;
                    for (Integer id : locallyKilledEnemies) {
                        if (!first) sb.append(",");
                        sb.append(id);
                        first = false;
                    }
                    animStateStr = sb.toString();
                }
                multiplayerService.syncPosition(player.getX(), player.getY(), player.getHealth(), dir, animStateStr);
                multiplayerService.fetchSessionStateAsync();
                
                List<java.util.Map<String, String>> state = multiplayerService.getLastCachedState();
                long myId = com.lost.database.infrastructure.OnlineService.getInstance().getOnlinePlayerId();
                
                remotePlayers.removeIf(rp -> {
                    for (java.util.Map<String, String> p : state) {
                        if (p.containsKey("playerId") && Long.parseLong(p.get("playerId")) == rp.getPlayerId()) return false;
                    }
                    return true;
                });
                
                for (java.util.Map<String, String> p : state) {
                    if (!p.containsKey("playerId")) continue;
                    long pid = Long.parseLong(p.get("playerId"));
                    if (pid == myId) continue;
                    
                    com.lost.database.game.entity.RemotePlayer rp = remotePlayers.stream().filter(r -> r.getPlayerId() == pid).findFirst().orElse(null);
                    if (rp == null) {
                        rp = new com.lost.database.game.entity.RemotePlayer(pid);
                        rp.setSprites(playerIdleGif, playerRunGif);
                        remotePlayers.add(rp);
                    }
                    
                    double px = Double.parseDouble(p.getOrDefault("positionX", "0"));
                    double py = Double.parseDouble(p.getOrDefault("positionY", "0"));
                    int hp = Integer.parseInt(p.getOrDefault("health", "100"));
                    boolean alive = Boolean.parseBoolean(p.getOrDefault("alive", "true"));
                    int direction = Integer.parseInt(p.getOrDefault("direction", "1"));
                    String aState = p.getOrDefault("animationState", "IDLE");
                    String parsedAnimState = aState;
                    if (aState.contains(":")) {
                        String[] parts = aState.split(":");
                        parsedAnimState = parts[0];
                        if (parts.length > 1) {
                            String[] deadIds = parts[1].split(",");
                            for (String idStr : deadIds) {
                                try {
                                    int deadIdx = Integer.parseInt(idStr.trim());
                                    if (deadIdx >= 0 && deadIdx < enemies.size()) {
                                        enemies.get(deadIdx).isDead = true;
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    String uname = p.getOrDefault("username", "Player " + pid);
                    
                    rp.updateState(px, py, hp, alive, direction, parsedAnimState);
                    rp.setUsername(uname);
                }
            }
            
            for (com.lost.database.game.entity.RemotePlayer rp : remotePlayers) {
                rp.update(dt);
            }
        }
    }

    // --- RENDER ---
    private void render() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        double canvasW = gameCanvas.getWidth();
        double canvasH = gameCanvas.getHeight();

        if (canvasW <= 0 || canvasH <= 0) return;

        // Disable smoothing for crisp retro pixel art
        gc.setImageSmoothing(false);

        // Clear background (jungle sky)
        gc.setFill(Color.web("#1B3A2D"));
        gc.fillRect(0, 0, canvasW, canvasH);
        
        // --- LEVEL 4 (TERMINAL) RENDER ---
        if (isTerminalActive) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, canvasW, canvasH);
            
            if (isValidImage(terminalPasswordPanelImage)) {
                gc.drawImage(terminalPasswordPanelImage, 0, 0, canvasW, canvasH);
            } else if (isValidImage(terminalBgSprite)) {
                gc.drawImage(terminalBgSprite, 0, 0, canvasW, canvasH);
            }
            
            // Apply a sci-fi overlay (dark scanlines/tint)
            gc.setFill(Color.rgb(0, 20, 10, 0.45));
            gc.fillRect(0, 0, canvasW, canvasH);
            
            // Draw a high-tech glassmorphic display box shifted to the top half to prevent blocking background keypad
            double boxW = 500;
            double boxH = 200;
            double boxX = (canvasW - boxW) / 2;
            double boxY = 40; // Shift to top!
            
            // Outer glow
            gc.setStroke(Color.rgb(0, 255, 100, 0.3));
            gc.setLineWidth(6);
            gc.strokeRoundRect(boxX, boxY, boxW, boxH, 15, 15);
            
            // Inner panel background
            gc.setFill(Color.rgb(5, 15, 8, 0.85));
            gc.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);
            
            // Panel border
            gc.setStroke(Color.rgb(0, 255, 100, 0.8));
            gc.setLineWidth(2);
            gc.strokeRoundRect(boxX, boxY, boxW, boxH, 15, 15);
            
            // Decorative corners/brackets
            gc.setStroke(Color.rgb(0, 255, 100, 0.9));
            gc.setLineWidth(3);
            // Top-left
            gc.strokeLine(boxX, boxY + 20, boxX, boxY);
            gc.strokeLine(boxX, boxY, boxX + 20, boxY);
            // Top-right
            gc.strokeLine(boxX + boxW - 20, boxY, boxX + boxW, boxY);
            gc.strokeLine(boxX + boxW, boxY, boxX + boxW, boxY + 20);
            // Bottom-left
            gc.strokeLine(boxX, boxY + boxH - 20, boxX, boxY + boxH);
            gc.strokeLine(boxX, boxY + boxH, boxX + 20, boxY + boxH);
            // Bottom-right
            gc.strokeLine(boxX + boxW - 20, boxY + boxH, boxX + boxW, boxY + boxH);
            gc.strokeLine(boxX + boxW, boxY + boxH - 20, boxX + boxW, boxY + boxH);

            // Draw glowing retro text inside the terminal box
            gc.setTextAlign(TextAlignment.CENTER);
            
            if (terminalError) {
                // Glow effect for error
                gc.setFill(Color.rgb(255, 50, 50, 0.2));
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
                gc.fillText("ACCESS DENIED", canvasW / 2 + 2, boxY + 62);
                
                gc.setFill(Color.rgb(255, 50, 50));
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
                gc.fillText("ACCESS DENIED", canvasW / 2, boxY + 60);
                
                gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 14));
                gc.setFill(Color.rgb(255, 100, 100));
                gc.fillText("INVALID KEY SEQUENCE DETECTED", canvasW / 2, boxY + 110);
                gc.fillText("RESETTING DECRYPTION LOCKS...", canvasW / 2, boxY + 150);
            } else {
                // Header
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
                gc.setFill(Color.rgb(0, 255, 100, 0.7));
                gc.fillText("DHARMATECH MAIN FRAME v4.815", canvasW / 2, boxY + 30);
                
                // Prompt
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
                gc.setFill(Color.rgb(0, 255, 100));
                gc.fillText("ENTER SECURITY PROTOCOL SEQUENCE", canvasW / 2, boxY + 70);
                
                // Input box background
                double inputW = 400;
                double inputH = 50;
                double inputX = (canvasW - inputW) / 2;
                double inputY = boxY + 95;
                gc.setFill(Color.rgb(0, 40, 15, 0.5));
                gc.fillRoundRect(inputX, inputY, inputW, inputH, 5, 5);
                gc.setStroke(Color.rgb(0, 255, 100, 0.5));
                gc.setLineWidth(1);
                gc.strokeRoundRect(inputX, inputY, inputW, inputH, 5, 5);
                
                // Input text
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
                
                // Blinking cursor
                String cursor = (System.currentTimeMillis() % 1000 < 500) ? "_" : "";
                String displayText = "> " + terminalInput.toString() + cursor;
                
                // Text glow shadow
                gc.setFill(Color.rgb(0, 255, 100, 0.3));
                gc.fillText(displayText, canvasW / 2 + 2, inputY + 33 + 2);
                
                gc.setFill(Color.rgb(100, 255, 180));
                gc.fillText(displayText, canvasW / 2, inputY + 33);
                
                // Footer hint
                gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 11));
                gc.setFill(Color.rgb(0, 255, 100, 0.5));
                gc.fillText("PRESS [ENTER] TO SUBMIT  |  [ESC] OR [E] TO CLOSE", canvasW / 2, boxY + 180);
            }

            // High-visibility Exit prompt at the bottom of the screen
            gc.setFont(Font.font("System", FontWeight.BOLD, 14));
            gc.setFill(Color.rgb(0, 255, 100, 0.95));
            gc.fillText("НАТИСНИ [E] АБО [ESC] ЩОБ ВИЙТИ З ТЕРМІНАЛУ", canvasW / 2, canvasH - 30);
            
            // Reset text alignment for other drawing code
            gc.setTextAlign(TextAlignment.LEFT);
            return;
        }

        if (currentLevel == 4) {
            // Bunker static background tiling
            if (isValidImage(bunkerWallBg)) {
                double iw = bunkerWallBg.getWidth() * 2; // scale by 2 for retro look
                double ih = bunkerWallBg.getHeight() * 2;
                double offsetX = -(cameraX % iw);
                double offsetY = -(cameraY % ih);
                if (offsetX > 0) offsetX -= iw;
                if (offsetY > 0) offsetY -= ih;

                for (double x = offsetX; x < canvasW; x += iw) {
                    for (double y = offsetY; y < canvasH; y += ih) {
                        gc.drawImage(bunkerWallBg, x, y, iw, ih);
                    }
                }
            } else {
                gc.setFill(Color.web("#1c1c1c"));
                gc.fillRect(0, 0, canvasW, canvasH);
            }
        } else {
            // Draw parallax background layers (farthest → nearest) for jungle
            drawParallax(gc, bgLayer0, 0.0, canvasW, canvasH); // Static sky
            drawParallax(gc, bgLayer1, 0.05, canvasW, canvasH); // Far trees
            drawParallax(gc, bgLayer2, 0.1, canvasW, canvasH); // Mid trees
            drawParallax(gc, bgLayer3, 0.2, canvasW, canvasH); // Near trees
            drawParallax(gc, bgLayer4, 0.3, canvasW, canvasH); // Bushes
        }

        // 1. Draw tiles from TMX
        if (jungleMap.isTmx()) {
            TileMapRenderer renderer = new TileMapRenderer();
            Image activeTileset = (currentLevel == 4) ? bunkerTileset : tmxTileset;
            int srcSize = (currentLevel == 4) ? 16 : 32;
            int srcCols = (currentLevel == 4) ? 11 : 16;
            
            for (int[][] layer : jungleMap.getTmxLayers()) {
                renderer.render(gc, layer, activeTileset, cameraX, cameraY, canvasW, canvasH, srcSize, srcCols);
            }

            // Draw any custom programmatic grid tiles (like SPIKES) on top of the TMX layers
            int startTileX = Math.max(0, (int) (cameraX / TILE_SIZE) - 1);
            int startTileY = Math.max(0, (int) (cameraY / TILE_SIZE) - 1);
            int endTileX = Math.min(jungleMap.getWidth(), startTileX + (int) (canvasW / TILE_SIZE) + 2);
            int endTileY = Math.min(jungleMap.getHeight(), startTileY + (int) (canvasH / TILE_SIZE) + 2);

            for (int mx = startTileX; mx < endTileX; mx++) {
                for (int my = startTileY; my < endTileY; my++) {
                    TileType t = jungleMap.getTile(mx, my);
                    if (t == TileType.SPIKES) {
                        double destX = mx * TILE_SIZE - cameraX;
                        double destY = my * TILE_SIZE - cameraY;
                        drawSpikes(gc, destX, destY);
                    }
                }
            }
            // Draw cockpit wreckage on level 1 only
            if (currentLevel == 1 && !missionComplete) {
                double cockpitDrawX = jungleMap.getCockpitX() * TILE_SIZE - cameraX;
                double cockpitDrawY = jungleMap.getCockpitY() * TILE_SIZE - cameraY;
                if (cockpitDrawX > -128
                        && cockpitDrawX < canvasW + 128
                        && cockpitDrawY > -128
                        && cockpitDrawY < canvasH + 128) {
                    drawCockpit(gc, cockpitDrawX, cockpitDrawY);
                }
            }
            // Draw radio tower on level 2 only (stays even after collecting part)
            if (currentLevel == 2) {
                double antennaDrawX = jungleMap.getCockpitX() * TILE_SIZE - cameraX;
                double antennaDrawY = jungleMap.getCockpitY() * TILE_SIZE - cameraY;
                if (antennaDrawX > -128
                        && antennaDrawX < canvasW + 128
                        && antennaDrawY > -192
                        && antennaDrawY < canvasH + 128) {
                    drawAntenna(gc, antennaDrawX, antennaDrawY);
                }
            }
            // Draw Hatch on Level 3
            if (currentLevel == 3) {
                // Adjust position to make it larger and lower
                double hatchW = 128;
                double hatchH = 64;
                double hatchX = 55 * TILE_SIZE - cameraX - 32; // Center it over the tile
                double hatchY = 10 * TILE_SIZE - cameraY - hatchH + 16; // Rest exactly on the grass
                if (isValidImage(hatchSprite)) {
                    gc.drawImage(hatchSprite, hatchX, hatchY, hatchW, hatchH);
                } else {
                    gc.setFill(Color.GRAY);
                    gc.fillRect(hatchX, hatchY, hatchW, hatchH);
                }
            }
            // Draw Terminal on Level 4
            if (currentLevel == 4) {
                double terminalSize = 128;
                double terminalComputerX = (jungleMap.getWidth() / 2.0) * TILE_SIZE - cameraX - (terminalSize / 2.0) + 16;
                // Lift Y up so that the bottom of the 128px image rests exactly on the floor at row 12!
                double terminalComputerY = 12 * TILE_SIZE - cameraY - 112; 
                
                if (isValidImage(terminalBgSprite)) {
                    gc.save();
                    gc.translate(terminalComputerX + (terminalSize / 2.0), terminalComputerY + (terminalSize / 2.0));
                    gc.scale(-1, 1);
                    gc.drawImage(terminalBgSprite, -(terminalSize / 2.0), -(terminalSize / 2.0), terminalSize, terminalSize);
                    gc.restore();
                } else {
                    gc.setFill(Color.GREEN);
                    gc.fillRect(terminalComputerX, terminalComputerY, terminalSize, terminalSize);
                }
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
            if (enemy.isDead) continue;
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

        // 5.8 Draw Remote Players
        for (com.lost.database.game.entity.RemotePlayer rp : remotePlayers) {
            rp.render(gc, cameraX, cameraY);
        }

        // 6. Draw player
        drawPlayer(gc, canvasW, canvasH);

        // Draw attack visual swipe/slash crescent
        if (attackTimer > 0) {
            double playerScreenX = player.getX() - cameraX;
            double playerScreenY = player.getY() - cameraY;
            double centerX = playerScreenX + PLAYER_W / 2.0;
            double centerY = playerScreenY + PLAYER_H / 2.0;
            
            gc.save();
            gc.setStroke(Color.rgb(200, 255, 255, 0.85));
            gc.setLineWidth(4);
            if (facingRight) {
                gc.strokeArc(centerX - 10, centerY - 45, 90, 90, -60, 120, javafx.scene.shape.ArcType.OPEN);
            } else {
                gc.strokeArc(centerX - 80, centerY - 45, 90, 90, 120, 120, javafx.scene.shape.ArcType.OPEN);
            }
            gc.restore();
        }

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
        if (isValidImage(spikesImage)) {
            gc.drawImage(spikesImage, destX, destY, TILE_SIZE, TILE_SIZE);
        } else {
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
    }

    private void drawCockpit(GraphicsContext gc, double destX, double destY) {
        if (isValidImage(cockpitImage)) {
            gc.drawImage(
                    cockpitImage,
                    destX - TILE_SIZE,
                    destY - TILE_SIZE + 48,
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

    private void drawAntenna(GraphicsContext gc, double destX, double destY) {
        if (isValidImage(antennaImage)) {
            gc.drawImage(
                    antennaImage,
                    destX - TILE_SIZE / 2,
                    destY - TILE_SIZE * 2,
                    TILE_SIZE * 2,
                    TILE_SIZE * 3);
        } else {
            // Fallback: draw a simple tower
            gc.setFill(Color.rgb(120, 100, 80));
            gc.fillRect(destX + 12, destY - 64, 8, 96);
            gc.fillRect(destX + 4, destY - 48, 24, 4);
            gc.fillRect(destX + 8, destY - 32, 16, 4);
            // Red blinking light
            double blink = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0);
            gc.setFill(Color.rgb(255, 50, 50, blink));
            gc.fillOval(destX + 12, destY - 72, 8, 8);
        }
        // Glowing marker
        double glow = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 200.0);
        gc.setFill(Color.rgb(50, 200, 255, glow * 0.6));
        gc.fillOval(destX + 8, destY - TILE_SIZE * 2 - 20, 16, 16);
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
        // Draw collected items in inventory slots
        if (player.hasItem("transceiver") && isValidImage(transceiverImage)) {
            double sx = slotStartX + 2;
            gc.drawImage(transceiverImage, sx, slotY + 2, slotSize - 4, slotSize - 4);
        }
        if (player.hasItem("antenna")) {
            double sx = slotStartX + (slotSize + 4) + 2; // second slot
            if (isValidImage(itemAntennaImage)) {
                gc.drawImage(itemAntennaImage, sx, slotY + 2, slotSize - 4, slotSize - 4);
            } else {
                gc.setFill(Color.rgb(100, 200, 255));
                gc.fillRect(sx + 14, slotY + 4, 8, 32);
                gc.setFill(Color.RED);
                gc.fillOval(sx + 14, slotY + 2, 8, 8);
            }
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
                "A/D - рух | SPACE - стрибок | SHIFT - ривок | E - взаємодія | ESC - пауза | G - чити",
                dashX,
                canvasH - 8);

        // God Mode indicator
        if (player.isGodMode()) {
            gc.save();
            // Pulse animation based on system time
            double alpha = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 200.0);
            gc.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
            gc.setFill(Color.color(0.0, 1.0, 0.0, alpha));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText("[GOD MODE ACTIVE]", canvasW - 20, 30);
            gc.restore();
        }

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

    /** Відправляє score на онлайн сервер після завершення рівня */
    private void submitScoreOnline() {
        OnlineService online = OnlineService.getInstance();
        if (!online.isOnline()) {
            System.out.println("[Online] Offline mode — score not submitted");
            return;
        }

        // Рахуємо score: здоров'я * 10 + розум * 5 + бонус за швидкість
        int healthScore = (int) (player.getHealth() * 10);
        int sanityScore = (int) (sanity * 5);
        int timeBonus = Math.max(0, 1000 - (int) gameElapsedTime);
        int totalScore = healthScore + sanityScore + timeBonus;
        int timeSec = (int) gameElapsedTime;

        System.out.println("[Online] Submitting score: " + totalScore
                + " (HP:" + healthScore + " SAN:" + sanityScore + " TIME:" + timeBonus + ")");

        new Thread(() -> {
            boolean success = online.syncScore(totalScore, maxLevelReached, timeSec);
            if (success) {
                System.out.println("[Online] ✓ Score submitted successfully!");
            } else {
                System.out.println("[Online] ✗ Failed to submit score");
            }
        }).start();
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
            System.out.println("🏆 Game completed! Showing victory screen...");
            gameWon = true;
            showVictoryScreen();
        }
    }

    private void showVictoryScreen() {
        if (gameLoop != null) gameLoop.stop();
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.rgb(0, 255, 100, 0.8));
        gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        gc.fillText("📡 Сигнал відправлено...", w / 2, h / 2 - 120);
        gc.setFill(Color.rgb(200, 200, 200));
        gc.setFont(Font.font("System", FontWeight.NORMAL, 18));
        gc.fillText("Через 3 дні на горизонті з'явився гвинтокрил...", w / 2, h / 2 - 70);
        gc.setFill(Color.rgb(255, 220, 80));
        gc.setFont(Font.font("System", FontWeight.BOLD, 24));
        gc.fillText("🏝 Ви вижили.", w / 2, h / 2 - 20);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Impact", FontWeight.BOLD, 72));
        gc.fillText("LOST", w / 2, h / 2 + 60);
        gc.setFill(Color.rgb(150, 150, 150));
        gc.setFont(Font.font("System", FontWeight.NORMAL, 14));
        gc.fillText("Курсова робота — 2026", w / 2, h / 2 + 110);
        gc.fillText("Натисни ENTER щоб повернутися в меню", w / 2, h / 2 + 150);
        gameCanvas.setOnKeyPressed(
                e -> {
                    if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                        try {
                            Parent root =
                                    FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                            gameCanvas.getScene().setRoot(root);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
    }

    private void loadLevel(int levelNum) {
        String path = "/assets/levels/level" + levelNum + ".tmx";
        JungleMap newMap = new JungleMap(path);

        // If TMX failed (no solid tiles), keep current map
        if (newMap.getWidth() > 0) {
            jungleMap = newMap;
        }

        if (levelNum == 1) {
            jungleMap.setCockpitPosition(40); // Cockpit on level 1
        } else if (levelNum == 2) {
            jungleMap.setCockpitPosition(43, 9); // Radio tower on level 2 exactly on ground
        }

        double spawnX = jungleMap.getSpawnX() * TILE_SIZE;
        double spawnY = (jungleMap.getSpawnY() - 1) * TILE_SIZE;

        if (player == null) {
            player = new GamePlayer(0, spawnX, spawnY);
            player.setSpawnPosition(spawnX, spawnY);
            player.fullReset();
            sanity = 100;
        } else {
            player.setSpawnPosition(spawnX, spawnY);
            if (levelNum > 1 && player.getHealth() > 0) {
                player.resetToSpawn();
            } else {
                player.fullReset();
                sanity = 100;
            }
        }
        jumpsUsed = 0;
        isDashing = false;
        dashCooldown = 0;
        attackTimer = 0;
        locallyKilledEnemies.clear();
        gameOver = false;
        gameOverTimer = 0;
        levelComplete = false;
        levelCompleteTimer = 0;
        missionComplete = false;
        cameraX = 0;
        cameraY = 0;

        initEnemiesFromMap();
        initItemsFromMap();

        // Spawn NPC for current level
        kateExists = false;
        kateTalkedTo = false;
        spawnNpcForLevel(levelNum);
        
        // Terminal level initialization
        isTerminalActive = false;
        terminalInput.setLength(0);
        terminalError = false;
        
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

        int centerTx = (int) Math.floor((px + PLAYER_W / 2.0) / TILE_SIZE);

        for (int tx = minTx; tx <= maxTx; tx++) {
            for (int ty = minTy; ty <= maxTy; ty++) {
                if (jungleMap.isSlope(tx, ty)) {
                    if (tx == centerTx) {
                        double playerCenterX = px + PLAYER_W / 2.0;
                        double slopeHeight = jungleMap.getSlopeHeight(tx, ty, playerCenterX, TILE_SIZE);
                        double slopeTop = (ty + 1) * TILE_SIZE - slopeHeight;
                        if (bottom > slopeTop) {
                            return true;
                        }
                    }
                } else if (jungleMap.isSolid(tx, ty)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Same as collidesAt but ignores slope tiles — used for horizontal movement */
    private boolean collidesAtIgnoreSlopes(double px, double py, double vx) {
        double left = px;
        double right = px + PLAYER_W;
        double top = py;
        // Subtract a small step margin so we don't hit tiny bumps
        double bottom = py + PLAYER_H - 12.0;

        int minTx = (int) Math.floor(left / TILE_SIZE);
        int maxTx = (int) Math.floor((right - 0.01) / TILE_SIZE);
        int minTy = (int) Math.floor(top / TILE_SIZE);
        int maxTy = (int) Math.floor((bottom - 0.01) / TILE_SIZE);

        // Only check the leading edge based on velocity
        int checkTxStart = minTx;
        int checkTxEnd = maxTx;
        if (vx > 0) {
            checkTxStart = maxTx; // Only check right edge
        } else if (vx < 0) {
            checkTxEnd = minTx; // Only check left edge
        }

        for (int tx = checkTxStart; tx <= checkTxEnd; tx++) {
            for (int ty = minTy; ty <= maxTy; ty++) {
                if (jungleMap.isSlope(tx, ty)) {
                    continue; // Skip slopes for horizontal checks
                } else if (jungleMap.isSolid(tx, ty)) {
                    // If this solid tile is at the foot level (bottom row)
                    // and has a slope adjacent in the direction we came from,
                    // skip it — the player is walking from slope onto flat ground
                    if (ty == maxTy) {
                        int adjacentX = (vx > 0) ? tx - 1 : tx + 1;
                        if (jungleMap.isSlope(adjacentX, ty)) {
                            continue; // Allow transition from slope to ground
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkSpikesCollision() {
        double px = player.getX();
        double py = player.getY();
        double pw = PLAYER_W;
        double ph = PLAYER_H;

        int minTx = (int) Math.floor(px / TILE_SIZE);
        int maxTx = (int) Math.floor((px + pw - 0.01) / TILE_SIZE);
        int minTy = (int) Math.floor(py / TILE_SIZE);
        int maxTy = (int) Math.floor((py + ph - 0.01) / TILE_SIZE);

        for (int tx = minTx; tx <= maxTx; tx++) {
            for (int ty = minTy; ty <= maxTy; ty++) {
                if (jungleMap.isHazard(tx, ty)) {
                    // Inset the hitbox slightly horizontally, and only check the bottom 40% of the tile visually
                    double spikeX = tx * TILE_SIZE + 4;
                    double spikeH = TILE_SIZE * 0.4;
                    double spikeY = ty * TILE_SIZE + TILE_SIZE - spikeH;
                    double spikeW = TILE_SIZE - 8;
                    if (rectsOverlap(px, py, pw, ph, spikeX, spikeY, spikeW, spikeH)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Snap the player's Y position to the slope surface if standing on a slope */
    private double snapToSlopeY(double px, double py) {
        double playerCenterX = px + PLAYER_W / 2.0;
        int footTileX = (int) (playerCenterX / TILE_SIZE);

        // Check the tile at player's feet level and one below
        for (int checkY = (int) ((py + PLAYER_H - 2) / TILE_SIZE);
                checkY <= (int) ((py + PLAYER_H + 4) / TILE_SIZE);
                checkY++) {
            if (jungleMap.isSlope(footTileX, checkY)) {
                double slopeHeight =
                        jungleMap.getSlopeHeight(footTileX, checkY, playerCenterX, TILE_SIZE);
                double slopeTop = (checkY + 1) * TILE_SIZE - slopeHeight;
                double snappedY = slopeTop - PLAYER_H - 0.1;
                // Allow snapping both up and down (for going up and down slopes)
                return snappedY;
            }
        }
        return py;
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
        // Check proximity to mission objects per level
        boolean nearCockpit = false;
        if (currentLevel == 1 && !player.hasItem("transceiver")) {
            int cx = jungleMap.getCockpitX();
            int cy = jungleMap.getCockpitY();
            double tileX = cx * TILE_SIZE;
            double tileY = cy * TILE_SIZE;

            nearCockpit =
                    rectsOverlap(
                            player.getX() - 32,
                            player.getY() - 32,
                            PLAYER_W + 64,
                            PLAYER_H + 64,
                            tileX,
                            tileY,
                            TILE_SIZE,
                            TILE_SIZE);
        }

        // Check proximity to antenna on level 2
        boolean nearAntenna = false;
        if (currentLevel == 2 && !player.hasItem("antenna")) {
            int cx = jungleMap.getCockpitX();
            int cy = jungleMap.getCockpitY();
            double tileX = cx * TILE_SIZE;
            double tileY = cy * TILE_SIZE;

            nearAntenna =
                    rectsOverlap(
                            player.getX() - 48,
                            player.getY() - 64,
                            PLAYER_W + 96,
                            PLAYER_H + 128,
                            tileX,
                            tileY,
                            TILE_SIZE,
                            TILE_SIZE);
        }

        // Check proximity to Kate NPC
        boolean nearKate = kateExists && !kateTalkedTo && isNearKate();

        // Check proximity to Teleport (Level 2 end)
        canInteractTeleport = false;
        if (currentLevel == 2) {
            double mapRightEdge = (jungleMap.getWidth() - 12) * TILE_SIZE;
            if (player.getX() >= mapRightEdge - TILE_SIZE * 3) {
                canInteractTeleport = true;
            }
        }

        // Check proximity to Hatch (Level 3 center)
        canInteractHatch = false;
        if (currentLevel == 3) {
            double hatchX = 55 * TILE_SIZE;
            double hatchY = 9 * TILE_SIZE;
            if (Math.abs(player.getX() - hatchX) < 100 && Math.abs(player.getY() - hatchY) < 150) {
                canInteractHatch = true;
            }
        }
        
        // Check proximity to Terminal Computer (Level 4 center)
        boolean canInteractTerminalComputer = false;
        if (currentLevel == 4 && !isTerminalActive) {
            double terminalComputerX = (jungleMap.getWidth() / 2.0) * TILE_SIZE;
            double terminalComputerY = 12 * TILE_SIZE;
            if (Math.abs(player.getX() - terminalComputerX) < 100 && Math.abs(player.getY() - terminalComputerY) < 150) {
                canInteractTerminalComputer = true;
            }
        }

        if ((nearCockpit && !missionComplete) || nearAntenna || nearKate || canInteractTeleport || canInteractHatch || canInteractTerminalComputer) {
            if (interactLabel != null) {
                if (canInteractTeleport) {
                    interactLabel.setText("Натисни [E] щоб телепортуватись");
                } else if (canInteractHatch) {
                    interactLabel.setText("Натисни [E] щоб відкрити люк");
                } else if (canInteractTerminalComputer) {
                    interactLabel.setText("Натисни [E] щоб ввести код");
                } else {
                    interactLabel.setText(
                            nearKate ? "Натисни [E] щоб поговорити" : "Натисни [E] для взаємодії");
                }
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
        // Try talking to NPC
        if (kateExists && !kateTalkedTo && isNearKate()) {
            kateTalkedTo = true;
            startLevelCutscene();
            return;
        }

        // Level 1: cockpit interaction gives transceiver
        if (currentLevel == 1 && !player.hasItem("transceiver")) {
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
                    tileY - 32 + 48,
                    TILE_SIZE + 64,
                    TILE_SIZE + 64)) {
                player.addItem("transceiver");
                showMission("📡 Трансивер знайдено! Тепер біжи до кінця!");
                System.out.println("✓ Transceiver collected!");
            }
        }

        // Level 2: radio tower interaction gives antenna
        if (currentLevel == 2 && !player.hasItem("antenna")) {
            int cx = jungleMap.getCockpitX();
            int cy = jungleMap.getCockpitY();
            double tileX = cx * TILE_SIZE;
            double tileY = cy * TILE_SIZE;

            if (rectsOverlap(
                    player.getX(),
                    player.getY(),
                    PLAYER_W,
                    PLAYER_H,
                    tileX - 48,
                    tileY - 64,
                    TILE_SIZE + 96,
                    TILE_SIZE + 128)) {
                player.addItem("antenna");
                showMission("📻 Антену знайдено! Тепер Саїд зможе підсилити сигнал!");
                System.out.println("✓ Antenna collected!");
            }
        }

        if (canInteractTeleport) {
            System.out.println("Teleporting to Level 3!");
            loadNextLevel();
            return;
        }

        if (canInteractHatch) {
            System.out.println("Entering the Bunker (Level 4)!");
            loadNextLevel();
            return;
        }

        if (currentLevel == 4 && !isTerminalActive) {
            double terminalComputerX = (jungleMap.getWidth() / 2.0) * TILE_SIZE;
            if (Math.abs(player.getX() - terminalComputerX) < 100) {
                isTerminalActive = true;
                terminalInput.setLength(0);
                return;
            }
        }
    }

    // --- NPC SYSTEM ---
    private void spawnNpcForLevel(int level) {
        int tilesRight = (level == 1) ? 3 : 5;
        int npcTileX = jungleMap.getSpawnX() + tilesRight;
        // Find main ground surface: scan from bottom, skip solid mass, stop at first
        // air gap
        int groundSurface = jungleMap.getHeight() - 2;
        boolean foundSolid = false;
        for (int sy = jungleMap.getHeight() - 1; sy >= 0; sy--) {
            if (jungleMap.isSolid(npcTileX, sy)) {
                foundSolid = true;
            } else if (foundSolid) {
                groundSurface = sy + 1; // top of ground mass
                break;
            }
        }
        double spawnX = npcTileX * TILE_SIZE;
        // NPC feet on top of ground surface
        // Per-level Y offset (Kate=18 was perfect, Sayid/Ben need less)
        int yOffset = (level == 1) ? 18 : 8;
        double spawnY = groundSurface * TILE_SIZE - KATE_H + yOffset;
        kateX = spawnX;
        kateY = spawnY;
        kateExists = true;
        kateTalkedTo = false;
        switch (level) {
            case 1:
                npcName = "Кейт";
                npcCurrentSprite = kateIdleSprite;
                npcHairColor = Color.rgb(101, 67, 33);
                npcSkinColor = Color.rgb(222, 184, 150);
                npcShirtColor = Color.rgb(160, 160, 155);
                npcPantsColor = Color.rgb(95, 110, 70);
                npcEyeColor = Color.rgb(60, 100, 60);
                break;
            case 2:
                npcName = "Саїд";
                npcCurrentSprite = sayidIdleSprite;
                npcHairColor = Color.rgb(30, 25, 20);
                npcSkinColor = Color.rgb(180, 140, 100);
                npcShirtColor = Color.rgb(85, 100, 60);
                npcPantsColor = Color.rgb(70, 70, 65);
                npcEyeColor = Color.rgb(50, 40, 30);
                break;
            case 3:
                npcName = "Бен";
                npcCurrentSprite = benIdleSprite;
                npcHairColor = Color.rgb(80, 65, 50);
                npcSkinColor = Color.rgb(230, 200, 175);
                npcShirtColor = Color.rgb(190, 175, 140);
                npcPantsColor = Color.rgb(160, 150, 120);
                npcEyeColor = Color.rgb(80, 120, 180);
                break;
            case 4:
                kateExists = false;
                return;
        }
        System.out.println("✓ NPC " + npcName + " spawned at (" + kateX + ", " + kateY + ")");
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
        if (isValidImage(npcCurrentSprite)) {
            gc.drawImage(npcCurrentSprite, x, y, KATE_W, KATE_H);
        } else {
            // --- Programmatic fallback: Draw Kate as a character ---

            // Hair
            gc.setFill(npcHairColor);
            gc.fillOval(x + 10, y - 2, 28, 28); // Main hair
            gc.fillRoundRect(x + 28, y + 4, 14, 18, 6, 6); // Ponytail
            gc.setFill(Color.rgb(80, 50, 25));
            gc.fillOval(x + 30, y + 2, 10, 10); // Ponytail knot

            // Head (skin tone)
            gc.setFill(npcSkinColor);
            gc.fillOval(x + 13, y + 4, 22, 22); // Face

            // Eyes
            gc.setFill(npcEyeColor);
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

            // Shirt
            gc.setFill(npcShirtColor);
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

            // Pants
            gc.setFill(npcPantsColor);
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

        // --- Name label above NPC ---
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setFill(Color.rgb(255, 220, 80));
        gc.fillText(npcName, x + KATE_W / 2, y - 12);

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
        double speed = 150; // Doubled speed to prevent speedrunning
        boolean movingRight = true;
        double minX, maxX;
        boolean isGhost = true;
        int spriteVariant = 0; // which ghost sprite to show
        int animDirection = 1; // +1 forward, -1 backward (ping-pong)
        double animTimer = 0; // timer for switching frames
        static final double ANIM_SPEED = 0.4; // seconds per frame change
        int health = 50; // HP
        boolean isDead = false;

        Enemy(double x, double y, double minX, double maxX) {
            this(x, y, minX, maxX, true);
        }

        Enemy(double x, double y, double minX, double maxX, boolean isGhost) {
            this.x = x;
            this.y = y;
            this.minX = minX;
            this.maxX = maxX;
            this.isGhost = isGhost;
            this.health = isGhost ? 50 : 100; // Ghosts take 1 hit (50 HP), physical takes 2 hits (100 HP)
            this.isDead = false;
            // Each ghost gets a unique starting sprite
            this.spriteVariant = (int) (Math.abs(x * 7 + y * 13)) % 5;
        }

        void update(double dt, JungleMap map, int tileSize, double gameTime) {
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

            if (isGhost) {
                // Deterministic time-based position sync
                double L = maxX - minX;
                if (L > 0) {
                    double totalTime = L / speed;
                    double cycleTime = 2 * totalTime;
                    double t = gameTime % cycleTime;
                    if (t < totalTime) {
                        x = minX + t * speed;
                        movingRight = true;
                    } else {
                        x = maxX - (t - totalTime) * speed;
                        movingRight = false;
                    }
                }
            } else {
                double dx = speed * dt * (movingRight ? 1 : -1);
                x += dx;

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
        // Зберігаємо в таблицю game_saves
        try {
            GameSaveDao saveDao = new GameSaveDao(LostDatabaseApp.getConnectionPool());
            GameSave save = new GameSave();
            save.setPlayerId(dbPlayer != null ? dbPlayer.getId() : 1L);
            save.setCurrentLevel(currentLevel);
            save.setHealth(player.getHealth());
            save.setMaxHealth(player.getMaxHealth());
            save.setSanity(sanity);
            save.setPositionX(player.getX());
            save.setPositionY(player.getY());
            save.setSaveName("Level " + currentLevel + " - " + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm")));
            saveDao.save(save);
            System.out.println("[Save] ✓ Saved to game_saves: id=" + save.getId());
        } catch (Exception e) {
            System.err.println("Save error: " + e.getMessage());
            e.printStackTrace();
        }

        // Зберігаємо онлайн якщо залогінені
        OnlineService online = OnlineService.getInstance();
        if (online.isOnline()) {
            new Thread(() -> {
                String saveName = "Level " + currentLevel + " - " + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm"));
                boolean ok = online.saveGameOnline(
                        currentLevel,
                        player.getHealth(),
                        player.getMaxHealth(),
                        (int) sanity,
                        player.getX(),
                        player.getY(),
                        saveName);
                System.out.println(ok ? "[Online] ✓ Game saved online!" : "[Online] ✗ Online save failed");
            }).start();
            showMission("💾 Гру збережено (онлайн + локально)!");
        } else {
            showMission("💾 Гру збережено!");
        }
        togglePauseMenu();
    }

    public void setDbPlayer(Player player) {
        this.dbPlayer = player;
    }

    /** Завантажити стан гри з збереження */
    public void loadFromSave(GameSave save) {
        if (save == null) return;
        this.pendingSave = save;
    }

    private GameSave pendingSave;

    /** Викликається після initialize() — застосовує збережений стан */
    public void applyPendingSave() {
        if (pendingSave == null) return;
        currentLevel = pendingSave.getCurrentLevel();
        loadLevel(currentLevel);
        player.setX(pendingSave.getPositionX());
        player.setY(pendingSave.getPositionY());
        player.setHealth(pendingSave.getHealth());
        sanity = pendingSave.getSanity();
        System.out.println("[Save] ✓ Loaded save: Level " + currentLevel
                + " HP:" + pendingSave.getHealth()
                + " Pos:(" + (int) pendingSave.getPositionX() + "," + (int) pendingSave.getPositionY() + ")");
        pendingSave = null;
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
