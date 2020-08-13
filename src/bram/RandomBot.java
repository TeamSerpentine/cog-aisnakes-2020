package bram;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * Sample implementation of snake bot
 */
public class RandomBot implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    /**
     * Choose the direction (not rational - silly)
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        // set coordinates of the head of our snake
        Coordinate head = snake.getHead();

        // find the backwards direction (afterhead), which we're not allowed to go towards
        Coordinate afterHeadNotFinal = null;
        if (snake.body.size() >= 2) {
            Iterator<Coordinate> it = snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }
        final Coordinate afterHead = afterHeadNotFinal;

        // remove backwards direction from valid moves
        Direction[] validMoves = Arrays.stream(DIRECTIONS).filter(d -> !head.moveTo(d).equals(afterHead))
                .sorted().toArray(Direction[]::new);

        // remove directions which directly cause us to lose
        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))           // maze bounds
                .filter(d -> !opponent.elements.contains(head.moveTo(d))) // opponent body
                .filter(d -> !snake.elements.contains(head.moveTo(d)))    // and yourself
                .sorted().toArray(Direction[]::new);

        // chooses random (not losing) direction
        if (notLosing.length > 0) {
            Random random = new Random();
            return notLosing[random.nextInt(notLosing.length)];
        } else return validMoves[0];
    }
}