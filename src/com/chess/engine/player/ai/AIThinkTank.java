package com.chess.engine.player.ai;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import javax.swing.SwingWorker;
import java.util.function.Consumer;

/** Runs the chosen AI strategy on a background thread, keeping the GUI responsive. */
public final class AIThinkTank extends SwingWorker<Move, Void> {
    private final Board board;
    private final MoveStrategy strategy;
    private final Consumer<Move> onComplete;
    private final boolean useOpeningBook;

    public AIThinkTank(final Board board, final MoveStrategy strategy,
                       final Consumer<Move> onComplete, final boolean useOpeningBook) {
        this.board = board;
        this.strategy = strategy;
        this.onComplete = onComplete;
        this.useOpeningBook = useOpeningBook;
    }

    @Override
    protected Move doInBackground() {
        // Try opening book first
        if(useOpeningBook) {
            final Move bookMove = OpeningBook.get().lookup(board);
            if(bookMove != null) return bookMove;
        }
        return strategy.execute(board);
    }

    @Override
    protected void done() {
        try {
            onComplete.accept(get());
        } catch(Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    public static MoveStrategy strategyForLevel(final int level) {
        return switch(level) {
            case 1  -> new RandomMoveStrategy();
            case 2  -> new GreedyStrategy();
            case 3  -> new MiniMaxStrategy();
            case 4  -> new AlphaBetaStrategy();
            case 5  -> new IterativeDeepeningStrategy();
            default -> new AdvancedAlphaBetaStrategy();
        };
    }
}
