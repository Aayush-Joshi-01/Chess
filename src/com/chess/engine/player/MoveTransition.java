package com.chess.engine.player;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
public class MoveTransition {
    private final Board transitionBoard;
    private final Move move;
    private final MoveStatus moveStatus;
    public MoveTransition(Board transitionBoard, Move move, MoveStatus moveStatus) {
        this.transitionBoard = transitionBoard;
        this.move = move;
        this.moveStatus = moveStatus;
    }
    public MoveStatus getMoveStatus(){
        return this.moveStatus;
    }
}