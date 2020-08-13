package humans;

import snakes.*;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;
import snakes.SnakeCanvas;
import java.util.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class human_subject_2 implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    private long moveTime = 300; // This one is only relevant when we are the blue snake

    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
            long startTime = System.currentTimeMillis();
            while(System.currentTimeMillis() - startTime < moveTime){

            }
            Direction move = SnakeCanvas.human_move;
            for (Direction step: doValidMove(snake, opponent, mazeSize)) {
                if (move == step){
                    return move;
                }
            }
            return goStraight(snake);
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
                .filter(d -> afterHead.moveTo(d).equals(snakehead)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);

        return goodMove[0];
    }

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
