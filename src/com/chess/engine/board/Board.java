package com.chess.engine.board;

import com.chess.engine.pieces.Piece;

public class Board {
    private Board(Builder builder){

    }
    public Tile getTile(final int tileCoordinate){
        return null;
    }
    public static class Builder{
        Map<Integer, Piece>
        public Board build(){
            return new Board(this);
        }
    }
}
