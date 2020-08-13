package enclosing;

import org.omg.CORBA.CODESET_INCOMPATIBLE;
import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;

/**
 * a snake that tries to enclose its opponent
 */
public class encloseOpponent implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    private Map<Coordinate, Coordinate> snake_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> snake_depth = new HashMap<Coordinate, Integer>();
    private Map <Coordinate, Coordinate> opponent_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> opponent_depth = new HashMap<Coordinate, Integer>();
    private Coordinate snake_head;
    private int oldReachable;
    private int opponent_oldReachable;
    private boolean checkCanEnclose;
    private Coordinate prev_apple;
    private boolean circle_start;
    private int circle_pos = 0;

    private Direction encloseMove = null;

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
        snake_head = snake.getHead();
        makeOpponentBFSGraph(snake, opponent, mazeSize, apple);
        // We do this before making our own BFS because we want to calculate the distance to the apple
        makeBFSGraph(snake, opponent, mazeSize, apple);
        Direction move = chooseMove(snake, opponent, mazeSize, apple);

        return move;
    }


    /**
     * The logic to decide where to move
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */
    private Direction chooseMove(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        Direction move = null;
        if (!checkCanEnclose) {
            move = pathTo(apple);
        } else{
            // TODO place here the code to check what to do if it can enclose the snake
            move = pathTo(apple);
        }

        if (move == null){ // Can't reach the apple
            Coordinate furthestPoint = getFurthestPoint(snake_depth, mazeSize);
            move = pathTo(furthestPoint);
            if (move == null) { // Can't even go towards the furthest point for some reason
                move = notLosingAlgorithm(snake, opponent, mazeSize)[0];
            }
        }

        if (!isThisMoveSafe_NotEnclosingOurSelf(snake, opponent, mazeSize, apple, move)) {
            // If the move results in a problem (our snake will probably enclose itself)
            Coordinate furthestPoint = getFurthestPoint(snake_depth, mazeSize);
            move = pathTo(furthestPoint);
            if (move == null) {
                move = notLosingAlgorithm(snake, opponent, mazeSize)[0];
            }
        }

        // Wolf's algorithm
//        if(snake.body.size()>7 && snake.body.size()>opponent.body.size() && snake.body.size()%2==0 && prev_apple==apple) {
//            boolean circle_possible = apple.x != 0 && apple.y != 0 && apple.x != mazeSize.x-1 && apple.y != mazeSize.y-1;
//            if(circle_possible) {
//                move = circle_apple(snake, opponent, mazeSize, apple, move);
//            }
//        }

        simpleEnclose(snake, mazeSize);
        if (encloseMove != null) {
            move = encloseMove;
        }

        prev_apple=apple;
        return move;
    }

    /**
     * Wolfs algorithm to circle the snake
     * @param mazeSize needed to check the whole grid
     * @param chosenDirection the direction we're about to go, before this function is called
     * @return Direction, to circle the snake
     */
    private Direction circle_apple(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, Direction chosenDirection) {
        boolean circle_possible = apple.x != 0 && apple.y != 0 && apple.x != mazeSize.x-1 && apple.y != mazeSize.y-1;
        Coordinate head = snake.getHead();
        Direction circle_direction = null;
        if (head.moveTo(chosenDirection).equals(apple) && circle_start==false) {
            circle_start=true;
            switch(chosenDirection) {
                case UP:
                    circle_pos = 0;
                    break;
                case RIGHT:
                    circle_pos = 1;
                    break;
                case DOWN:
                    circle_pos = 2;
                    break;
                case LEFT:
                    circle_pos = 3;
                    break;
                default:
                    break;
            }
        }
        if(circle_start) {
            switch (circle_pos) {
                case 0:
                    if (head.x < apple.x) {
                        circle_pos = 1;
                    }
                    break;
                case 1:
                    if (head.y > apple.y) {
                        circle_pos = 2;
                    }
                    break;
                case 2:
                    if (head.x > apple.x) {
                        circle_pos = 3;
                    }
                    break;
                case 3:
                    if (head.y < apple.y) {
                        circle_pos = 0;
                    }
                    break;
                default:
                    break;
            }

            switch (circle_pos) {
                case 0:
                    circle_direction = Direction.LEFT;
                    break;
                case 1:
                    circle_direction = Direction.UP;
                    break;
                case 2:
                    circle_direction = Direction.RIGHT;
                    break;
                case 3:
                    circle_direction = Direction.DOWN;
                    break;
                default:
                    break;
            }
            // chosenDirection = notLosing[0];
            boolean stillCircles = false;
            for (Direction d : notLosingAlgorithm(snake, opponent, mazeSize)) {
                if (circle_direction == d) {
                    stillCircles = true;
                    chosenDirection = circle_direction;
                    break;
                }
            }
            if (!stillCircles) { // Abort only doing the circle strat if somehow he cant circle it anymore
                circle_start = false;
            }
        }
        return chosenDirection;
    }

    /**
     * Determine the furthest reachable point of the BFS graph (the highest depth)
     * @param depth_grid the BFS graph on which you want to determine the furthest point
     * @param mazeSize needed to check the whole grid
     * @return Coordinate, the furthest reachable point
     */
    private Coordinate getFurthestPoint(HashMap depth_grid, Coordinate mazeSize) {
        Coordinate destination = null; //
        int distance = 0;
        // Make it search for the longest path and make it go to that path
        for (int x  = 0 ; x < mazeSize.x; x++)
            for (int y  = 0 ; y < mazeSize.y; y++) {
                Coordinate check = new Coordinate(x,y);
                if (depth_grid.containsKey(check)) {
                    if ((int) depth_grid.get(check) > distance) {
                        destination = check;
                        distance = (int) depth_grid.get(check);
                    }
                }
            }
        return destination;
    }


    /**
     * Determines all the blocks reachable point of the BFS graph at a certain depth
     * @param depth The depth of which you want to find all the cords
     * @param depth_grid the BFS graph which you want to check
     * @param mazeSize needed to check the whole grid
     * @return Coordinate, the furthest reachable point
     * (this function was called findDepth earlier)
     */
    private Coordinate[] findCoordinatesWithThisDepth(int depth, HashMap depth_grid, Coordinate mazeSize) {
        ArrayList sol = new ArrayList();
        for (int x  = 0 ; x < mazeSize.x; x++)
            for (int y  = 0 ; y < mazeSize.y; y++) {
                Coordinate check = new Coordinate(x,y);
                if (depth_grid.containsKey(check)) {
                    if (depth_grid.get(check).equals(depth)) {
                        sol.add(check);
                    }
                }
            }

        Coordinate[] answer = (Coordinate[]) sol.toArray(new Coordinate[sol.size()]);
        return answer;
    }


    /**
     * finds out what the depth of opponent's paths are when they pass our head
     * @param mazeSize maze
     * @return array of depths next to our head in directions: UP, DOWN, LEFT, RIGHT (in this order)
     */
    private int[] checkDepthOfOpponentPathsPassingOurHead(Coordinate mazeSize) {
        int[] depths = {-1,-1,-1,-1};  // default values

        for (int i=0; i<4; i++) {
            if (snake_head.moveTo(DIRECTIONS[i]).inBounds(mazeSize)) {
                if (opponent_depth.get(snake_head.moveTo(DIRECTIONS[i])) != null) {
                    depths[i] = opponent_depth.get(snake_head.moveTo(DIRECTIONS[i]));
                }
            }
            // else the depth value for direction i stays at -1 (default value)
        }
        return depths;
    }

    /**
     * sets the encloseMove variable to an enclosing move if there is one
     * otherwise it leaves it at null
     * @param snake our snake
     * @param mazeSize maze
     */
    private void simpleEnclose(Snake snake, Coordinate mazeSize) {
        if (encloseMove != null) {
            encloseMove = null;
            return;  // we break out of this function if we already did an enclosing move last turn
        }

        /* The only illegal move is going backwards. Here we are not including it */
        Coordinate afterHeadNotFinal = null;
        if (snake.body.size() >= 2) {
            Iterator<Coordinate> it = snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }
        final Coordinate afterHead = afterHeadNotFinal;
        Direction[] validMoves = Arrays.stream(DIRECTIONS)
                .filter(d -> !snake_head.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .sorted().toArray(Direction[]::new);


        Coordinate nextToHead;
        for (int i=0; i<validMoves.length; i++) {
            nextToHead = snake_head.moveTo(validMoves[i]);
            if (nextToHead.inBounds(mazeSize)) {
                if (opponent_depth.get(nextToHead) != null) {
                    if (opponent_depth.get(nextToHead) > 1) {
                        // depth has to be greater than 1, otherwise we might bump into the opponents head
                        if (findCoordinatesWithThisDepth(opponent_depth.get(nextToHead), opponent_depth, mazeSize).length == 1) {
                            // there is only 1 path with this depth in it!
                            System.out.println("go step " + validMoves[i] + " to enclose!");
                            System.out.println();
                            // watch out: I think directions UP and DOWN are set wrong (LEFT and RIGHT are correct)

                            encloseMove = validMoves[i];
                            return;  // we break out of the function since we found an enclosing move

                            //TODO: fix: sometimes the snake goes into its own body
                            // (has to do with new version of BFS I think)
                        }
                    }
                }
            }
        }
    }



    /**
     * Returns the step toward your goal
     * @param goal  coordinate of the place where you want to go
     * @return Direction, the step to take or null if there is no route
     */
    private Direction pathTo(Coordinate goal){
        ArrayList path = new ArrayList(); // This will be the route from the goal to the head
        if (snake_cameFrom.containsKey(goal)){ // If this is not the case, the goal is enclosed by a snake
            path.add((goal));
        } else {
            return null; // Path is unreachable
        }

        Direction move; // The command to move to
        while (path.get(path.size() - 1) != snake_head) {
            path.add(snake_cameFrom.get(path.get(path.size() - 1)));
        }
        move = directionFromTo(snake_head, (Coordinate) path.get(path.size() - 2));
        return move;
    }


    /**
     * Makes a worst case BFS graph of the new move and compares how much array it can reach
     * TODO This can also check if the highest depth of the new BFS is lower than the snakeSize  -> enclosing the snake
     * TODO currently only works for our own snake (a fix is to have the depth and camefrom graph as input)
     * This can also be used to make a general worst case BFS graph
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @param move     The move you want to check
     * @return returns the number of reachable blocks
     */
    int reachable_part(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, Direction move) {
        Coordinate newHead;
        int correction = 0;
        if (move == null) {
            newHead = snake.getHead();
        } else {
            newHead = Coordinate.add(snake.getHead(), move.v);
            correction = 1;
        }

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[newHead.x][newHead.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x, y)) || opponent.elements.contains(new Coordinate(x, y))) {
                    arrayVisited[x][y] = true;
                }
            }
        }
        arrayVisited[snake.body.getLast().x][snake.body.getLast().y] = false; // We don't reach the tail anymore

        // Also ignore possible locations for the next head for the opponent to avoid tieing
        // Only if the player is behind, otherwise it's worth the risk
        // EDIT: never ignore new head location because that could block the snake
        for (Direction step : Direction.values()) {
            Coordinate newPos = Coordinate.add(opponent.getHead(), step.v);
            if (newPos.inBounds(mazeSize)) {
                arrayVisited[newPos.x][newPos.y] = true;
            }
        }
        arrayVisited[opponent.body.getLast().x][opponent.body.getLast().y] = false; // We don't reach the tail anymore

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        Map<Coordinate, Coordinate> cameFrom = new HashMap<Coordinate, Coordinate>();
        Map<Coordinate, Integer> depth = new HashMap<Coordinate, Integer>(); // Depth = number of step required to get to coordinate
        cameFrom.put(newHead, snake.getHead()); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(newHead); // Add head as starting point for the queue
        depth.put(newHead, 0 + correction);
        int depth_level_handled = 0 + correction; // Tells if we already tog
        int weCanReachApple = 0;
        int opponentCanReachApple = 0;
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = depth.get(start) + 1; // The depth level

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

            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once

                /* !! EXPERIMENTAL RULE !! marks all the possible head locations of the enemy snake as visited
                 * A possible tweak for this, is to only mark realistic head location as true or only upto a certain depth
                 * TODO Check if you adding this when making the BFS graph for our snake will make it become overly protective
                 * currently disabled, makes the snake overly protective
                 */
                //for (Coordinate possible_enemy_head : findDepth(depth_level, opponent_depth, mazeSize)) {
                //    arrayVisited[possible_enemy_head.x][possible_enemy_head.y] = true;
                //}

                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    } else { // Now we are at a depth higher than the snake size, thu
                        for (Coordinate empty : findCoordinatesWithThisDepth(depth_level - snake.body.size() - weCanReachApple, snake_depth, mazeSize)){
                            arrayVisited[empty.x][empty.y] = false;
                        }
                    }
                }
                if (depth_level < opponent.body.size() + opponentCanReachApple) {
                    if (opponentCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() + opponentCanReachApple - depth_level];
                        arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                    }
                } else {
                    for (Coordinate empty : findCoordinatesWithThisDepth(depth_level - opponentCanReachApple - opponent.body.size(), opponent_depth, mazeSize)){
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                depth_level_handled++;
            }



            for (Direction step : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, step.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!cameFrom.containsKey(newPos)) {
                            cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            depth.put(newPos, depth_level);
                        }
                    }
                }
            /*
            if (newPos == apple) {
                break; // We found the apple in the fastest route, no need to look further
            } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }
        return cameFrom.size();
    }

    /**
     * this function checks if the move we're about to make is safe
     * safe means: not enclosing our self
     * @param snake our snake
     * @param opponent other snake
     * @param mazeSize maze
     * @param apple the apple
     * @param move the move we're about to make
     * @return true if safe, false if not safe
     * (this function was called checkMove earlier)
     */
    private boolean isThisMoveSafe_NotEnclosingOurSelf(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, Direction move) {
        int new_reachable = reachable_part(snake, opponent, mazeSize, apple, move);
        if (((double) new_reachable / oldReachable) < 0.8) {
            System.out.println("Let's not do that");
            // TODO check when this happens there is a real treat
            return false;
        }
        return true;
    }



    /**
     * Makes a BFS graph and
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return this does not have a return function but this:
     * makes a BFS graph of the current stat: init_cameFrom,
     * marks the depth (distance) for each reachable coordinate
     * notes the old size number of positions reachable for the snake
     */
    private void makeBFSGraph(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        oldReachable = snake_cameFrom.size();
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
                    } else { // Now we are at a depth higher than the snake size, thu
                        for (Coordinate empty : findCoordinatesWithThisDepth(depth_level - snake.body.size() - weCanReachApple, snake_depth, mazeSize)){
                            arrayVisited[empty.x][empty.y] = false;
                        }
                    }
                }
                if (depth_level < opponent.body.size() + opponentCanReachApple) {
                    if (opponentCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() + opponentCanReachApple - depth_level];
                        arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                    }
                } else {
                    for (Coordinate empty : findCoordinatesWithThisDepth(depth_level - opponentCanReachApple - opponent.body.size(), opponent_depth, mazeSize)){
                        arrayVisited[empty.x][empty.y] = false;
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

    /**
     * Makes a BFS graph of the opponent (this will be the most unaltered BFS graph)
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * this does not have a return function but this:
     * makes a BFS graph of the current stat: opponent_cameFrom,
     * marks the depth (distance) for each reachable coordinate: opponent_depth
     * notes the old size number of positions reachable for the snake
     * Also includes a predictor if the opponent can be enclosed
     */
    private void makeOpponentBFSGraph(Snake opponent, Snake snake, Coordinate mazeSize, Coordinate apple) {
        opponent_cameFrom.clear();
        opponent_depth.clear();
        Coordinate opponent_head = snake.getHead();
        /*
         * The BFS algorithm
         */

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[opponent_head.x][opponent_head.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x,y)) || opponent.elements.contains(new Coordinate(x,y)) ) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        // This is still enabled not, because if one of these move causes trouble for the opponent snake,
        // this means we can enclose it
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of steps required to get to coordinate
        opponent_cameFrom.put(opponent_head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(opponent_head); // Add head as starting point for the queue
        opponent_depth.put(opponent_head, 0);
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
                } else { // Now we need to edit all the places we visited in the arraylist
                    for (Coordinate empty : findCoordinatesWithThisDepth(depth_level - weCanReachApple -snake.body.size(), opponent_depth, mazeSize)){
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                if (depth_level < opponent.body.size()) {
                    Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() - depth_level];
                    arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                } else { // TODO does not work because this is using the old BFS graph and is thus outdated
                    //for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), snake_depth,mazeSize)){
                    //    arrayVisited[empty.x][empty.y] = false;
                    //}
                }
                depth_level_handled++;
            }


            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!opponent_cameFrom.containsKey(newPos)) {
                            opponent_cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            opponent_depth.put(newPos, depth_level);
                        }
                    }
                }
            /* We skip this, because we want to have an full BFS graph
            if (newPos == apple) {
                break; // We found the apple in the fastest route, no need to look further
            }
            */
            }
            queue.remove(0); // Remove the checked position from the search array
        }

        if (opponent_oldReachable == 0) {
            opponent_oldReachable = opponent_cameFrom.size();
        }
        int opponent_newReachable = opponent_cameFrom.size();
        if (((double) opponent_newReachable / opponent_oldReachable) < 0.6) { // If the enemy lost significant movement space
            System.out.println("enemy snake lost a lot of movement space");
            checkCanEnclose = true;
        }
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

    /**
     * the standard algo provided by the organisation of AI Snakes
     * @param snake our snake
     * @param opponent other snake
     * @param mazeSize the maze
     * @return a safe direction to go to
     */
    private Direction[] notLosingAlgorithm(Snake snake, Snake opponent, Coordinate mazeSize) {
        Coordinate head = snake.getHead();
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

        /* Just naive greedy algorithm that tries not to die at each moment in time */
        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                .filter(d -> !opponent.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(opponent.body.getLast()))  // Don't collide with opponent...
                .filter(d -> !snake.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(snake.body.getLast()))     // or yourself
                .sorted()
                .toArray(Direction[]::new);

        return notLosing;
    }



}
