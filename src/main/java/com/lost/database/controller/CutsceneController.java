package com.lost.database.controller;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Контролер катсцен гри LOST.
 *
 * <p>Показує діалоги між персонажами з портретами, вибором відповідей та переходами між сценами.
 * Після завершення катсцени повертає до лобі.
 */
public class CutsceneController {

    private StackPane rootStack;
    private ImageView backgroundImage;
    private ImageView portraitImage;
    private Label nameLabel;
    private Label dialogueLabel;
    private Label continueLabel;
    private HBox choicesBox;
    private StackPane transitionOverlay;
    private Label transitionLabel;

    private List<DialogueLine> script = new ArrayList<>();
    private int currentLineIndex = 0;

    // Images
    private Image jackWorried;
    private Image jackSerious;
    private Image sayidSerious;
    private Image burningPlane;
    private Image nightBonfire;

    private Runnable onFinished;
    private String playerName = "PLAYER";

    /**
     * Будує UI катсцени.
     *
     * @param playerName ім'я гравця для діалогів
     * @param onFinished callback після завершення катсцени
     * @return кореневий StackPane
     */
    public StackPane buildView(String playerName, Runnable onFinished) {
        this.onFinished = onFinished;
        if (playerName != null && !playerName.isEmpty()) {
            this.playerName = playerName.toUpperCase();
        }

        rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: black;");

        loadImages();
        buildUI();
        initScript();
        showLine();

        rootStack.setOnMouseClicked(e -> onNextClick());

        return rootStack;
    }

    private void loadImages() {
        try {
            burningPlane = loadImage("/assets/cutscene/burning_plane.jpg");
            jackWorried = loadImage("/assets/cutscene/jack_worried.png");
            jackSerious = loadImage("/assets/cutscene/jack_serious.png");
            nightBonfire = loadImage("/assets/cutscene/night_bonfire.jpg");
            sayidSerious = loadImage("/assets/cutscene/sayid_serious.png");
        } catch (Exception e) {
            System.err.println("[Cutscene] Error loading images: " + e.getMessage());
        }
    }

    private Image loadImage(String path) {
        var stream = getClass().getResourceAsStream(path);
        return stream != null ? new Image(stream) : null;
    }

    private void buildUI() {
        // Background
        backgroundImage = new ImageView();
        backgroundImage.setPreserveRatio(false);
        backgroundImage.fitWidthProperty().bind(rootStack.widthProperty());
        backgroundImage.fitHeightProperty().bind(rootStack.heightProperty());
        if (burningPlane != null) {
            backgroundImage.setImage(burningPlane);
        }
        rootStack.getChildren().add(backgroundImage);

        // Dark overlay for readability
        StackPane darkOverlay = new StackPane();
        darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        rootStack.getChildren().add(darkOverlay);

        // Portrait
        portraitImage = new ImageView();
        portraitImage.setFitHeight(300);
        portraitImage.setFitWidth(200);
        portraitImage.setPreserveRatio(true);
        portraitImage.setVisible(false);

        // Name
        nameLabel = new Label("...");
        nameLabel.setFont(loadRetroFont(18));
        nameLabel.setTextFill(Color.rgb(255, 204, 0));
        nameLabel.setEffect(new DropShadow(4, Color.BLACK));

        // Dialogue text
        dialogueLabel = new Label("...");
        dialogueLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        dialogueLabel.setTextFill(Color.WHITE);
        dialogueLabel.setWrapText(true);
        dialogueLabel.setMinHeight(40);

        // Choices
        choicesBox = new HBox(20);
        choicesBox.setAlignment(Pos.CENTER_LEFT);
        choicesBox.setVisible(false);
        choicesBox.setManaged(false);

        // Continue hint
        continueLabel = new Label("▼ (Натисніть для продовження)");
        continueLabel.setFont(Font.font("System", 11));
        continueLabel.setTextFill(Color.GRAY);
        continueLabel.setMaxWidth(Double.MAX_VALUE);
        continueLabel.setAlignment(Pos.CENTER_RIGHT);

        // Dialogue box — compact, full width
        VBox dialogueBox = new VBox(8, nameLabel, dialogueLabel, choicesBox, continueLabel);
        dialogueBox.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.85);"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 15 30;");

        // Portrait above, dialogue below — full width
        VBox bottomLayout = new VBox(5, portraitImage, dialogueBox);
        bottomLayout.setAlignment(Pos.BOTTOM_LEFT);
        bottomLayout.setPadding(new Insets(0, 20, 10, 20));
        rootStack.getChildren().add(bottomLayout);

        // Transition overlay
        transitionLabel = new Label("КІЛЬКА ГОДИН ПОТОМУ...");
        transitionLabel.setFont(loadRetroFont(20));
        transitionLabel.setTextFill(Color.WHITE);
        transitionLabel.setEffect(new DropShadow(10, Color.BLACK));

        transitionOverlay = new StackPane(transitionLabel);
        transitionOverlay.setStyle("-fx-background-color: black;");
        transitionOverlay.setVisible(false);
        rootStack.getChildren().add(transitionOverlay);
    }

    // ═══════════════════════════════════════════════════════
    // СКРИПТ КАТСЦЕНИ
    // ═══════════════════════════════════════════════════════

    private void initScript() {
        // PART 1: Crash Site
        script.add(new DialogueLine("JACK", "Гей! Ти мене чуєш? Розплющ очі!", "jack_worried"));
        script.add(new DialogueLine(playerName, "Що... що сталося? Де ми?", null));
        script.add(
                new DialogueLine(
                        "JACK",
                        "Літак розбився. Ми впали на якийсь острів. Я Джек, я лікар."
                                + " Тобі пощастило, ти цілий.",
                        "jack_worried"));
        script.add(
                new DialogueLine(
                        "JACK",
                        "Слухай, там багато поранених біля води. Мені потрібна допомога."
                                + " Ти можеш рухатися?",
                        "jack_serious"));

        // CHOICE
        DialogueLine choiceLine = new DialogueLine("CHOICE", "", null);
        choiceLine.isChoice = true;
        script.add(choiceLine);
    }

    private void initNightScript() {
        script.clear();
        currentLineIndex = 0;

        script.add(
                new DialogueLine(
                        "JACK",
                        "Ми перевірили кабіну пілотів. Трансивер розбитий вщент."
                                + " Ми не можемо подати сигнал.",
                        "jack_serious"));
        script.add(
                new DialogueLine(
                        "SAYID",
                        "Якщо ми не можемо викликати допомогу звідси, треба знайти вищу точку."
                                + " Або джерело прісної води. Наших запасів надовго не вистачить.",
                        "sayid_serious"));
        script.add(
                new DialogueLine(
                        "SAYID",
                        "Я бачив, що джунглі густішають на півночі."
                                + " Можливо, там є пагорб або... щось інше.",
                        "sayid_serious"));
        script.add(
                new DialogueLine(
                        "JACK",
                        "Саїд правий. Ми не можемо просто сидіти на пляжі.",
                        "jack_serious"));
        script.add(
                new DialogueLine(
                        "JACK",
                        "Слухай, ти сьогодні добре впорався з водою. Ти виглядаєш міцним."
                                + " Піди подивись, що там за деревами. Тільки недалеко."
                                + " Знайди стежку.",
                        "jack_serious"));

        script.add(new DialogueLine("SYSTEM", "END_SCENE", null));
    }

    // ═══════════════════════════════════════════════════════
    // ЛОГІКА ВІДОБРАЖЕННЯ
    // ═══════════════════════════════════════════════════════

    private void onNextClick() {
        if (choicesBox.isVisible() || transitionOverlay.isVisible()) {
            return;
        }

        currentLineIndex++;
        if (currentLineIndex < script.size()) {
            DialogueLine line = script.get(currentLineIndex);
            if ("END_SCENE".equals(line.text)) {
                endCutscene();
            } else {
                showLine();
            }
        }
    }

    private void showLine() {
        DialogueLine line = script.get(currentLineIndex);

        if (line.isChoice) {
            setupChoices();
            return;
        }

        nameLabel.setText(line.character);
        dialogueLabel.setText(line.text);

        choicesBox.setVisible(false);
        choicesBox.setManaged(false);
        continueLabel.setVisible(true);

        // Portrait
        if (line.portrait != null && !line.portrait.isEmpty()) {
            portraitImage.setVisible(true);
            switch (line.portrait) {
                case "jack_worried":
                    portraitImage.setImage(jackWorried);
                    break;
                case "jack_serious":
                    portraitImage.setImage(jackSerious);
                    break;
                case "sayid_serious":
                    portraitImage.setImage(sayidSerious != null ? sayidSerious : jackSerious);
                    break;
                default:
                    portraitImage.setVisible(false);
                    break;
            }
        } else {
            portraitImage.setVisible(false);
        }
    }

    private void setupChoices() {
        nameLabel.setText("ВИБІР");
        dialogueLabel.setText("Що відповісти?");
        continueLabel.setVisible(false);

        choicesBox.getChildren().clear();

        Button btnHelp = new Button("Так, я допоможу.");
        btnHelp.setStyle(
                "-fx-font-size: 14px; -fx-background-color: #ffcc00;"
                        + " -fx-text-fill: black; -fx-cursor: hand;"
                        + " -fx-padding: 10 20; -fx-background-radius: 8;");
        btnHelp.setOnAction(e -> handleChoice("HELP"));

        Button btnBad = new Button("Мені самому погано...");
        btnBad.setStyle(
                "-fx-font-size: 14px; -fx-background-color: #555;"
                        + " -fx-text-fill: white; -fx-cursor: hand;"
                        + " -fx-padding: 10 20; -fx-background-radius: 8;");
        btnBad.setOnAction(e -> handleChoice("BAD"));

        choicesBox.getChildren().addAll(btnHelp, btnBad);
        choicesBox.setVisible(true);
        choicesBox.setManaged(true);
    }

    private void handleChoice(String choice) {
        choicesBox.setVisible(false);
        choicesBox.setManaged(false);
        continueLabel.setVisible(true);
        startNightTransition();
    }

    private void startNightTransition() {
        transitionOverlay.setVisible(true);
        transitionOverlay.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), transitionOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(
                e -> {
                    PauseTransition pause = new PauseTransition(Duration.seconds(3));
                    pause.setOnFinished(
                            ev -> {
                                if (nightBonfire != null) {
                                    backgroundImage.setImage(nightBonfire);
                                }
                                initNightScript();
                                showLine();

                                FadeTransition fadeOut =
                                        new FadeTransition(Duration.seconds(1), transitionOverlay);
                                fadeOut.setFromValue(1);
                                fadeOut.setToValue(0);
                                fadeOut.setOnFinished(ev2 -> transitionOverlay.setVisible(false));
                                fadeOut.play();
                            });
                    pause.play();
                });
        fadeIn.play();
    }

    private void endCutscene() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), rootStack);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(
                e -> {
                    if (onFinished != null) {
                        onFinished.run();
                    }
                });
        fadeOut.play();
    }

    private Font loadRetroFont(double size) {
        Font f =
                Font.loadFont(
                        getClass().getResourceAsStream("/assets/fonts/PressStart2P-Regular.ttf"),
                        size);
        return f != null ? f : Font.font("Courier New", FontWeight.BOLD, size);
    }

    // ═══════════════════════════════════════════════════════
    // МОДЕЛЬ ДІАЛОГУ
    // ═══════════════════════════════════════════════════════

    private static class DialogueLine {
        String character;
        String text;
        String portrait;
        boolean isChoice = false;

        public DialogueLine(String character, String text, String portrait) {
            this.character = character;
            this.text = text;
            this.portrait = portrait;
        }
    }
}
