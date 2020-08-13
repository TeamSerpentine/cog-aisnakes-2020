package humans;

import snakes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class Human implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    private long moveTime = 100; // This one is only relevant when we are the blue snake (Why? QbyBramG)
    Direction move;

    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < moveTime) {
                // wait a bit
            }

            if (SnakeCanvas.move_queue.isEmpty()) {
                return goStraight(snake);
            } else {
                move = SnakeCanvas.move_queue.get(0);
            }

            while (!isMoveValid(move, snake, opponent, mazeSize)) {
                // while the move is not valid, keep trying the next move from the queue
                if (!SnakeCanvas.move_queue.isEmpty()) {
                    move = SnakeCanvas.move_queue.get(0);
                    SnakeCanvas.move_queue.remove(0);
                } else {
                    return goStraight(snake);
                }
            }

            SnakeCanvas.move_queue.remove(0);
            return move;
    }

    private Direction goStraight(Snake snake1) {
        Coordinate snakehead = snake1.getHead();
        Coordinate afterHeadNotFinal = null;
        if (snake1.body.size() >= 2) {
            Iterator<Coordinate> it = snake1.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }
        final Coordinate afterHead = afterHeadNotFinal;
        Direction[] goodMove = Arrays.stream(DIRECTIONS)
                .filter(d -> afterHead.moveTo(d).equals(snakehead))
                .sorted()
                .toArray(Direction[]::new);
        return goodMove[0];
    }

    // to make sure we don't go backwards
    private boolean isMoveValid(Direction move1, Snake snake1, Snake opponent1, Coordinate mazeSize) {
        for (Direction step: doValidMove(snake1, opponent1, mazeSize)) {
            if (move1 == step) {
                return true;
            }
        }
        return false;
    }

    // this returns an array with all valid directions to move in
    private Direction[] doValidMove(Snake snake1, Snake opponent1, Coordinate mazeSize) {
        Coordinate snakehead = snake1.getHead();

        // Get the coordinate of the second element of the snake's body to prevent going backwards
        Coordinate afterHeadNotFinal = null;
        if (snake1.body.size() >= 2) {
            Iterator<Coordinate> it = snake1.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }
        final Coordinate afterHead = afterHeadNotFinal;

        // The only illegal move is going backwards. Here we are checking for not doing it
        Direction[] validMoves = Arrays.stream(DIRECTIONS)
                .filter(d -> !snakehead.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);

        return validMoves;
    }
}
