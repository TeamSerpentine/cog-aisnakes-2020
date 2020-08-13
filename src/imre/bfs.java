package imre;

import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;

import snakes.Bot;

import javax.sound.midi.SysexMessage;

/**
 * Sample implementation of snake bot
 */
public class bfs implements Bot {
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
        return breadFirstSearch(snake, opponent, mazeSize, apple);
    }

    /**
     * Choose the direction (not rational - silly)
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */

    private Direction breadFirstSearch(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        Coordinate head = snake.getHead();
        /*
         * The BFS algorithm
         */

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[head.x][head.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x,y)) || opponent.elements.contains(new Coordinate(x,y)) ) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        // Also ignore possible locations for the next head for the opponent to avoid tieing
        // Only if the player is behind, otherwise it's worth the risk
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }


        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        Map <Coordinate, Coordinate> cameFrom = new HashMap<Coordinate, Coordinate>();
        Map <Coordinate, Integer> depth = new HashMap<Coordinate, Integer>(); // Depth = number of step required to get to coordinate
        cameFrom.put(head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(head); // Add head as starting point for the queue
        depth.put(head, 0);
        int depth_level_handled = 0; // Tells if we already tog
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = depth.get(start) + 1; // The depth level

            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size()) {
                    Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() - depth_level];
                    arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                }
                if (depth_level < opponent.body.size()) {
                    Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() - depth_level];
                    arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                }
                depth_level_handled++;
            }

            if (cameFrom.containsKey(apple)) {
                break; // Exit the loop, because we don't need to find the apple anymore
            }
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!cameFrom.containsKey(newPos)) {
                            cameFrom.put(newPos,start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            depth.put(newPos, depth_level);
                        }
                    }
                }
                if (newPos == apple) {
                    break; // We found the apple in the fastes route, no need to look further
                }
            }
            queue.remove(0); // Remove the checked position from the search array
        }

        ArrayList path = new ArrayList(); // This will be the route from the Apple to the head
        if (apple != null) {
            if (cameFrom.containsKey(apple)){ // If this is not the case, the apple is enclosed by a snake
                path.add((apple));
            }
        }

        Direction move; // The command to move to
        if (path.size() == 1) {
            while (path.get(path.size() - 1) != head) {
                path.add(cameFrom.get(path.get(path.size() - 1)));
            }
            move = directionFromTo(head, (Coordinate) path.get(path.size() - 2));

        } else { // If the apple is unavailable or the apple is enclosed use the not losing algorithm
            Coordinate afterHeadNotFinal = null;
            if (snake.body.size() >= 2) {
                Iterator<Coordinate> it = snake.body.iterator();
                it.next();
                afterHeadNotFinal = it.next();
            }
            final Coordinate afterHead = afterHeadNotFinal;

            /* The only illegal move is going backwards. Here we are checking for not doing it */
            Direction[] validMoves = Arrays.stream(DIRECTIONS)
                    .filter(d -> !head.moveTo(d).equals(afterHead)) // Filter out the backwards move
                    .sorted()
                    .toArray(Direction[]::new);

            /* Just naïve greedy algorithm that tries not to die at each moment in time */
            Direction[] notLosing = Arrays.stream(validMoves)
                    .filter(d -> head.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                    .filter(d -> !opponent.elements.contains(head.moveTo(d)))   // Don't collide with opponent...
                    .filter(d -> !snake.elements.contains(head.moveTo(d)))      // and yourself
                    .sorted()
                    .toArray(Direction[]::new);

            if (notLosing.length > 0) {
                move = notLosing[0];
            }
            else {
                move = validMoves[0];
            }
            return notLosing[0];
        }
        return move;
    }

    /**
     * Set direction from one point to other point
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
