package com.github.ikeras;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.*;

public class Chip extends Application {
    private static final Map<KeyCode, Integer> javafxKeysToChip8Keys = new HashMap<KeyCode, Integer>() {
        {
            put(KeyCode.X, 0x0);
            put(KeyCode.DIGIT1, 0x1);
            put(KeyCode.DIGIT2, 0x2);
            put(KeyCode.DIGIT3, 0x3);
            put(KeyCode.Q, 0x4);
            put(KeyCode.W, 0x5);
            put(KeyCode.UP, 0x5);
            put(KeyCode.E, 0x6);
            put(KeyCode.SPACE, 0x6);
            put(KeyCode.A, 0x7);
            put(KeyCode.LEFT, 0x7);
            put(KeyCode.S, 0x8);
            put(KeyCode.DOWN, 0x8);
            put(KeyCode.D, 0x9);
            put(KeyCode.RIGHT, 0x9);
            put(KeyCode.Z, 0xa);
            put(KeyCode.C, 0xb);
            put(KeyCode.DIGIT4, 0xc);
            put(KeyCode.R, 0xd);
            put(KeyCode.F, 0xe);
            put(KeyCode.V, 0xf);
        }
    };

    private final class UtzOptions {
        private String _romPath;
        private int _instructionsPerSecond;

        public UtzOptions(String romPath, int instructionsPerSecond) {
            _romPath = romPath;
            _instructionsPerSecond = instructionsPerSecond;
        }

        public String getRomPath() {
            return _romPath;
        }

        public int getInstructionsPerSecond() {
            return _instructionsPerSecond;
        }
    }

    private Emulator _emulator;
    private boolean _isRunning;
    private int _displayHeight;
    private int _displayWidth;

    public void start(Stage primaryStage) throws Exception {
        UtzOptions options = parseArgs(getParameters().getRaw().toArray(new String[0]));

        _displayHeight = 0;
        _displayWidth = 0;
        _emulator = new Emulator();
        _emulator.loadRom(options.getRomPath());

        Canvas canvas = new Canvas();
        StackPane root = new StackPane();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root);

        scene.setOnKeyPressed(event -> {
            if (javafxKeysToChip8Keys.containsKey(event.getCode())) {
                _emulator.pressKey(javafxKeysToChip8Keys.get(event.getCode()));
            }
        });

        scene.setOnKeyReleased(event -> {
            if (javafxKeysToChip8Keys.containsKey(event.getCode())) {
                _emulator.releaseKey(javafxKeysToChip8Keys.get(event.getCode()));
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Utz Chip-8 emulator");
        primaryStage.setResizable(false);
        primaryStage.show();

        _isRunning = true;

        Thread thread = new Thread(() -> {
            _emulator.startOrContinue(options.getInstructionsPerSecond());
        });
        thread.start();

        new AnimationTimer() {
            private GraphicsContext _gc = null;

            public void handle(long currentTime) {
                if (_isRunning) {
                    if (_displayHeight != _emulator.getDisplayHeight() || _displayWidth != _emulator.getDisplayWidth()) {
                        _gc = createDisplay(primaryStage, canvas, _emulator.getDisplayWidth(), _emulator.getDisplayHeight());
                        _gc.setImageSmoothing(false);
                    }

                    Image image = createImageFromBytes(_emulator.getDisplay(), _emulator.getDisplayWidth(), _emulator.getDisplayHeight());                    
                    _gc.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight());

                    _emulator.tick();
                }
            }
        }.start();
    }

    public void stop() {
        _emulator.stop();
        _isRunning = false;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private GraphicsContext createDisplay(Stage stage, Canvas canvas, int width, int height) {
        _displayWidth = width;
        _displayHeight = height;

        // Set the size of the Stage to account for the non-client area
        double nonClientWidth = stage.getWidth() - stage.getScene().getWidth();
        double nonClientHeight = stage.getHeight() - stage.getScene().getHeight();
        stage.setWidth(_emulator.getDisplayWidth() * 10 + nonClientWidth);
        stage.setHeight(_emulator.getDisplayHeight() * 10 + nonClientHeight);

        canvas.setWidth(width * 10);
        canvas.setHeight(height * 10);

        return canvas.getGraphicsContext2D();
    }

    private Image createImageFromBytes(byte[] bytes, int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            int rowStart = y * width;
            for (int x = 0; x < width; x++) {
                int pixelStart = rowStart + x;
                int pixel = bytes[pixelStart] == 0 ? 0xFF000000 : 0xFFFFFFFF;
                writer.setArgb(x, y, pixel);
            }
        }

        return image;
    }

    private UtzOptions parseArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print this help message");
        options.addOption("s", "speed", true, "Number of operations to emulate per second");

        try {
            CommandLine cmd = parser.parse(options, args);
            int instructionsPerSecond = 700;
            String[] appArgs = cmd.getArgs();

            if (cmd.hasOption("help") || appArgs.length < 1) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("MyApp", options);
                System.exit(0);
            }

            if (cmd.hasOption("speed")) {
                instructionsPerSecond = Integer.parseInt(cmd.getOptionValue("speed").trim());
            }

            String romPath = appArgs[0];

            return new UtzOptions(romPath, instructionsPerSecond);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            System.exit(1);
        }

        return null;
    }
}