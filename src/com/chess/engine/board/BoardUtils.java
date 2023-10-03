package com.chess.engine.board;

public class BoardUtils {
    public static final boolean [] FIRST_COLUMN = null;

    private BoardUtils(){
        throw new RuntimeException("You cannot instantiate me :(");
    }
    private static boolean isValidTileCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate < 64;
    }
}
