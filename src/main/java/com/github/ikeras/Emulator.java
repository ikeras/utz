package com.github.ikeras;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Emulator {
    private short[] memory = new short[4 * 1024];
    private CPU cpu = new CPU(memory);
    private boolean isExecuting = false;

    public void loadRom(String romPath) throws IOException {
        byte[] rom = Files.readAllBytes(Paths.get(romPath));
        for (int i = 0; i < rom.length; i++) {
            memory[0x200 + i] = (short) (rom[i] & 0xFF);
        }        
    }

    public byte[] getDisplay() {
        return cpu.getDisplay();
    }

    public int getDisplayWidth() {
        return cpu.getDisplayWidth();
    }

    public int getDisplayHeight() {
        return cpu.getDisplayHeight();
    }

    public void pressKey(int key) {
        cpu.pressKey(key);
    }

    public void releaseKey(int key) {
        cpu.releaseKey(key);
    }

    public void tick() {
        cpu.tick();
    }

    public void startOrContinue(int instructionsPerSecond) {
        isExecuting = true;

        double delayTime = getDelayBetweenInstructions(instructionsPerSecond);
        if (delayTime < 0) {
            delayTime = 0;
        }

        while (isExecuting) {
            try {                
                Thread.sleep((long) (delayTime * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            cpu.executeNextInstruction();
        }
    }

    public double getDelayBetweenInstructions(int instructionsPerSecond) {
        long startTime = System.nanoTime();

        for (int i = 0; i < instructionsPerSecond; i++) {
            cpu.executeNextInstruction();
        }

        long endTime = System.nanoTime();

        return (1 - (endTime - startTime) / 1e9) / instructionsPerSecond;
    }

    public void stop() {
        isExecuting = false;
    }
}