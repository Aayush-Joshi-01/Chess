package com.chess.engine.board;

public class BoardUtils {
    public static final boolean[] FIRST_COLUMN   = initColumn(0);
    public static final boolean[] SECOND_COLUMN  = initColumn(1);
    public static final boolean[] SEVENTH_COLUMN = initColumn(6);
    public static final boolean[] EIGHTH_COLUMN  = initColumn(7);
    public static final boolean[] FIRST_ROW      = initRow(0);
    public static final boolean[] SECOND_ROW     = initRow(8);
    public static final boolean[] SEVENTH_ROW    = initRow(48);
    public static final boolean[] EIGHTH_ROW     = initRow(56);
    public static final int NUM_TILES = 64;
    public static final int NUM_TILES_PER_ROW = 8;
    private BoardUtils(){
        throw new RuntimeException("You cannot instantiate me :(");
    }
    private static boolean[] initColumn(int columnNumber){
        final boolean[] column = new boolean[64];
        do{
            column[columnNumber] = true;
            columnNumber += NUM_TILES_PER_ROW;
        } while(columnNumber < NUM_TILES);
        return column;
    }
    private static boolean[] initRow(int rowNumber) {
        final boolean[] row = new boolean[NUM_TILES];
        do{
            row[rowNumber] = true;
            rowNumber++;
        } while(rowNumber % NUM_TILES_PER_ROW != 0);
        return row;
    }
    public static boolean isValidTileCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate < NUM_TILES;
    }
    public static int getCoordinateAtPosition(final String position) {
        // e.g., "e4" -> 36
        final int file = position.charAt(0) - 'a';
        final int rank = 8 - (position.charAt(1) - '0');
        return rank * 8 + file;
    }
    public static String getPositionAtCoordinate(final int coordinate) {
        return ALGEBRAIC_NOTATION[coordinate];
    }
    private static final String[] ALGEBRAIC_NOTATION = initAlgebraicNotation();
    private static String[] initAlgebraicNotation() {
        final String[] notation = new String[64];
        final char[] files = {'a','b','c','d','e','f','g','h'};
        for(int i = 0; i < 64; i++){
            notation[i] = "" + files[i % 8] + (8 - i / 8);
        }
        return notation;
    }
}
