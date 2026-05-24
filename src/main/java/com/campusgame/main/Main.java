package com.campusgame.main;

import com.campusgame.engine.GameLoop;

/**
 * MAIN ENTRY POINT
 * ----------------
 * Launches the game window and starts the game loop.
 * This is the only class with a main() method.
 *
 * To run: Execute this class in IntelliJ.
 */
public class Main {
    public static void main(String[] args) {
        GameLoop game = new GameLoop();
        game.start();
    }
}