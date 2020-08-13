package imre;

import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;

import snakes.Bot;

import javax.sound.midi.SysexMessage;

/**
 * The current BFS strategy I use for all the bots
 */
public class bfsTweaked implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    private Map <Coordinate, Coordinate> snake_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> snake_depth = new HashMap<Coordinate, Integer>();
    private Map <Coordinate, Coordinate> opponent_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> opponent_depth = new HashMap<Coordinate, Integer>();
    private Coordinate snake_head;
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
        snake_head = snake.getHead();
        makeOpponentBFSGraph(snake, opponent, mazeSize, apple);
        makeBFSGraph(snake, opponent, mazeSize, apple);
        Direction move = chooseMove(snake, opponent, mazeSize, apple);
        return move;
    }



    /**
     * Makes a BFS graph
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return this does not have a return function but this:
     * makes a BFS graph of the current stat: snake_cameFrom,
     * marks the depth (distance) for each reachable coordinate
     * notes the old size number of positions reachable for the snake
     */

    private void makeBFSGraph(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        snake_cameFrom.clear();
        snake_depth.clear();
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

        snake.body.getLast();
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
        // Depth = number of step required to get to coordinate
        snake_cameFrom.put(head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(head); // Add head as starting point for the queue
        snake_depth.put(head, 0);
        int depth_level_handled = 0; // Tells if we already tog
        int weCanReachApple = 0; // Turns one if true
        int opponentCanReachApple = 0;
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = snake_depth.get(start) + 1; // The depth level
            if (apple.inBounds(mazeSize)) {
                if (snake_depth.containsKey(apple)) {
                    if (depth_level >= snake_depth.get(apple)) {
                        weCanReachApple = 1;
                    }
                }
                if (opponent_depth.containsKey(apple)) {
                    if (depth_level >= opponent_depth.get(apple)) {
                        opponentCanReachApple = 1;
                    }
                }
            }
            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    }
                }
                if (depth_level < opponent.body.size() + opponentCanReachApple) {
                    if (opponentCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() + opponentCanReachApple - depth_level];
                        arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                    }
                }
                depth_level_handled++;
            }


            /* We skip this, because we want a full BFS
            if (snake_cameFrom.containsKey(apple)) {
                break; // Exit the loop, because we don't need to find the apple anymore
            }

             */
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!snake_cameFrom.containsKey(newPos)) {
                            snake_cameFrom.put(newPos,start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            snake_depth.put(newPos, depth_level);
                        }
                    }
                }
                /* We skip this, because we want to have an full BFS graph
                if (newPos == apple) {
                    break; // We found the apple in the fastes route, no need to look further
                } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }
    }

    // Almost the same as makeBFS graph, but from the opponents perspective
    private void makeOpponentBFSGraph(Snake opponent, Snake snake, Coordinate mazeSize, Coordinate apple) {
        opponent_cameFrom.clear();
        opponent_depth.clear();
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

        // This is disabled, because we cannot assume the opposing snake does this (we have to be prepared for every scenario)
        /*
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }
         */
        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of step required to get to coordinate
        opponent_cameFrom.put(head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(head); // Add head as starting point for the queue
        opponent_depth.put(head, 0);
        int weCanReachApple = 0; // Turns one if true
        int depth_level_handled = 0; // Turns one if true
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = opponent_depth.get(start) + 1; // The depth level
            if (opponent_depth.containsKey(apple)) {
                    if (depth_level >= opponent_depth.get(apple)) {
                        weCanReachApple = 1;
                }
            }
            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    }
                }
                if (depth_level < opponent.body.size()) {
                    Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() - depth_level];
                    arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                }
                depth_level_handled++;
            }

            /* We skip this, because we want a full BFS
            if (opponent_cameFrom.containsKey(apple)) {
                break; // Exit the loop, because we don't need to find the apple anymore
            }

             */
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!opponent_cameFrom.containsKey(newPos)) {
                            opponent_cameFrom.put(newPos,start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            opponent_depth.put(newPos, depth_level);
                        }
                    }
                }
                /* We skip this, because we want to have an full BFS graph
                if (newPos == apple) {
                    break; // We found the apple in the fastes route, no need to look further
                } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }
    }

    private Direction chooseMove(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        Direction move = pathTo(apple);
        if (move == null){ // Can't reach the apple
            Coordinate furthestPoint = getFurthestPoint(mazeSize);
            move = pathTo(furthestPoint);
            if (move == null) {
                move = notLosingAlgorithm(snake, opponent, mazeSize);
            }
        }
        return move;
    }

    /**
     * Requires @param snake_cameFrom which is made with the BFS graph
     * Returns the step toward your goal
     * @param goal  coordinate of the place where you want to go
     * @return Direction, the step to take or null if there is no route
     */
    private Direction pathTo(Coordinate goal){
        ArrayList path = new ArrayList(); // This will be the route from the Apple to the head
        if (snake_cameFrom.containsKey(goal)){ // If this is not the case, the apple is enclosed by a snake
            path.add((goal));
        } else {
            return null;
        }

        Direction move; // The command to move to
        while (path.get(path.size() - 1) != snake_head) {
            path.add(snake_cameFrom.get(path.get(path.size() - 1)));
        }
        move = directionFromTo(snake_head, (Coordinate) path.get(path.size() - 2));
        return move;
    }

    /**
     * Determine the furtest reachable point of the BFS graph
     * @param mazeSize needed to check the whole grid
     * @return Coordinate, the furthest reachable point
     */
    private Coordinate getFurthestPoint(Coordinate mazeSize) {
        Coordinate destination = null; //
        int distance = 0;
        // TODO make it search for the longest path
        for (int x  = 0 ; x < mazeSize.x; x++)
            for (int y  = 0 ; y < mazeSize.y; y++) {
                Coordinate check = new Coordinate(x,y);
                if (snake_depth.containsKey(check)) {
                    if (snake_depth.get(check) > distance) {
                        destination = check;
                        distance = snake_depth.get(check);
                    }
                }
            }
        return destination;
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

    private Direction notLosingAlgorithm(Snake snake, Snake opponent, Coordinate mazeSize) {
        Coordinate head = snake.getHead();
        Coordinate afterHeadNotFinal = null;
        Direction move;
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
                .filter(d -> !snake.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(snake.body.getLast()))     // and yourself
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

}