package bram;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 * implementation of snake bot
 * this bot does not want to draw
 * it avoids the locations where the head of the opponent snake could be next
 */
public class AvoidDrawBot implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    /**
     * Choose the direction
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

        // Serpentine: determine where opponents head could be next turn
        HashSet<Coordinate> opponentsHeadOptions = new HashSet<>();
        for (Direction d : DIRECTIONS) {
            opponentsHeadOptions.add(opponent.getHead().moveTo(d));
        }

        // Serpentine: remove directions which directly cause us to lose or draw
        Direction[] notDrawingOrLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))           // maze bounds
                .filter(d -> !opponent.elements.contains(head.moveTo(d))) // opponent body
                .filter(d -> !snake.elements.contains(head.moveTo(d)))    // and yourself
                .filter(d -> !opponentsHeadOptions.contains(head.moveTo(d)))  // Bram: and opponent head options
                .sorted().toArray(Direction[]::new);

        // Serpentine: chooses direction to apple, if possible
        if (notDrawingOrLosing.length > 0) {
            Direction toApple = directionFromTo(head, apple);
            for (Direction d : notDrawingOrLosing) {
                if (toApple == d) {
                    return toApple;
                }
            }
            // if toApple is losing or drawing, do something else randomly
            Random random = new Random();
            return notDrawingOrLosing[random.nextInt(notDrawingOrLosing.length)];
        } else return validMoves[0];
    }

    /**
     * Set direction from one point to other point
     * added by Serpentine
     * @param start point to begin
     * @param other point to move
     * @return direction
     */
    public Direction directionFromTo(Coordinate start, Coordinate other) {
        final Coordinate vector = new Coordinate(other.x - start.x, other.y - start.y);
        if (vector.x > 0) {
            return Direction.RIGHT;
        } else if (vector.x < 0) {
            return Direction.LEFT;
        }
        if (vector.y > 0) {
            return Direction.UP;
        } else if (vector.y < 0) {
            return Direction.DOWN;
        }
        for (Direction direction : Direction.values())
            if (direction.dx == vector.x && direction.dy == vector.y)
                return direction;
        return null;
    }


}