package com.chess.engine;
import com.chess.engine.board.Board;

public class Chessv2 {
    public static void main(String[] args){
        Board board = Board.createStandardBoard();
        //Driver Code for the Chess.
        //This will be configured when we publish the application.
        System.out.println(board);
        //Prints out the board.
    }
}