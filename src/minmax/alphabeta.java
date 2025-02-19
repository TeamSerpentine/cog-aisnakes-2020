package minmax;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;

/**
 * The default snake where I made variations on
 */
public class alphabeta implements Bot, Runnable {
    private volatile boolean exit = false;

    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    private Snake snake = null;
    private Snake opponent = null;
    private Coordinate mazeSize = null;
    private Coordinate apple = null;
    private Coordinate centre = null;
    private Coordinate[] appleSpots;
    private boolean chase_tail = false;


    private int maxDepth;  // How deep we will go with the minmax algorithm. NEEDS TO BE EVEN
    private Direction bestMove = null; // The best move we will take
    private int bestScore = Integer.MIN_VALUE; // The score of the move

    /* Score */
    private int scr_WeAteApple = 800;
    private int scr_circlePart1 = 1000; // We cover all circle points. TODO covering all entry points the circle
    private int scr_circlePart2 = 80000; // We succesfully made the circle --> The move should be follow our tail
    private int scr_OppAteApple = -500;
    private int scr_losingLead = -1250;
    private int scr_WeCantMove = -20000;
    private int scr_OppCantMove = 10000;
    private int scr_GoodHeadColl = 5000;
    private int scr_BadHeadColl = -20000;
    private int scr_weReachedCentre = 100;

    private int fct_distanceToApple = -5;
    private int fct_distanceToCentre = -2;

    public Direction final_move;

    /**
     * Choose the direction and clones the snakes so we don't mess with the ingame snake when removing elements of its body
     *
     * @param snake1    Your snake's body with coordinates for each segment
     * @param opponent1 Opponent snake's body with coordinates for each segment
     * @param mazeSize1 Size of the board
     * @param apple1    Coordinate of an apple
     * @return Direction of bot's move
     */
    @Override
    public Direction chooseDirection(Snake snake1, Snake opponent1, Coordinate mazeSize1, Coordinate apple1) {
        //System.out.println("new Bot");
        return this.chooseDirection(snake1, opponent1, mazeSize1, apple1, System.currentTimeMillis());
    }

    /** Choose a direction with alpha beta with a time less then the usual 1000 milliseconds
     *
     * @param snake     Your snake's body with coordinates for each segment
     * @param opponent  Opponent snake's body with coordinates for each segment
     * @param mazeSize  Size of the board
     * @param apple     Coordinate of an apple
     * @param startTime
     * @return
     */
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, long startTime) {
        preVariableAssignment(snake, opponent, mazeSize, apple);
        final_move = Direction.UP;
        exit = false;
        Thread t1 = new Thread(this);
        t1.start();
        while (System.currentTimeMillis() - startTime < 990) {
        }
        exit = true;
        return final_move;
    }

    public void run() {
        int i = 2;
        //System.out.println("enter loop");
        while (!exit) {
            //timeLimit = (long) (System.currentTimeMillis() + ((long) 1000 * searchfactor));
            maxDepth = i;
            //System.out.println("depth " + i);
            try {
                final_move = makeMinMax();
            } catch (NullPointerException e) {
                //System.out.println("Exit with nullpointer");
                break;
            }
            i += 2;
        }
        System.out.println("Depth level reached: " + (i - 2) / 2);
    }

    /**
     * Resets the variables
     *
     * @param snake1    Your snake's body with coordinates for each segment
     * @param opponent1 Opponent snake's body with coordinates for each segment
     * @param mazeSize1 Size of the board
     * @param apple1    Coordinate of an apple
     */
    private void preVariableAssignment(Snake snake1, Snake opponent1, Coordinate mazeSize1, Coordinate apple1) {
        if (snake1.body.size() < 5) { // Reset variables at new snake run
            chase_tail = false;
        }

        snake = snake1;
        opponent = opponent1;
        mazeSize = mazeSize1;
        apple = apple1;
        centre = new Coordinate(mazeSize.x / 2, mazeSize.y / 2);
        bestMove = null;
        bestScore = Integer.MIN_VALUE;
        appleSpots = null;
    }

    /**
     * The instruction to make a minmax graph
     * This uses the snake, opponent, we stored in makeminMax
     *
     * @return Direction of bot's move
     */
    private Direction makeMinMax() {
        if (chase_tail) { // We won
            return directionFromTo(snake.getHead(), snake.body.getLast());
        } else { // Do the normal strategy
            getAppleSpots();
            Object[] cur_state = {snake.clone(), opponent.clone(), -1, -1, -1};
            minmax(cur_state, cur_state, maxDepth, Integer.MIN_VALUE, Integer.MAX_VALUE, true); // minimax(state, newstate, depth, alpha, beta, maximizing player)

            if (distanceBetween(snake.getHead(), snake.body.getLast()) == 1 && circleScore(cur_state) >= scr_circlePart2 * 0.9) {
                chase_tail = true;
                return directionFromTo(snake.getHead(), snake.body.getLast());
            } else {
                if (bestMove != null) {
                    return bestMove;
                } else {
                    System.out.println("We had a good run son");
                    return doValidMove(snake, opponent, mazeSize);
                }
            }
        }
    }

    private void getAppleSpots() {
        if (apple.inBounds(mazeSize)) {
            ArrayList<Coordinate> spots = new ArrayList<Coordinate>();
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(apple, move.v);
                if (newPos.inBounds(mazeSize)) {
                    spots.add(newPos);
                }
            }
            appleSpots = (Coordinate[]) spots.toArray(new Coordinate[spots.size()]);
        }
    }

    /**
     * Choose the direction (not rational - silly)
     *
     * @param state            The new positions of the snake after a move. state[0] = our_snake, state[1] = opponent_snake, state[2] = depth_WeAteApple, state[3] = depth_OppAteApple
     * @param newState         The new proposed state which will replace state if depth is equal: both players have made a move
     * @param depth            The depth of the minmax graph, this is not equal to the distance like BFS
     * @param maximizingPlayer Is true when we are looking at out snake (thus maximsing our score), is false otherwise
     * @return Direction of bot's move
     */
    private int minmax(Object[] state, Object[] newState, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth % 2 == 0) { // We update the state once both snakes have made their move, otherwise the game.
            state = newState.clone();
        }
        Coordinate shead; // The head of the new snake
        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];
        int depth_WeAteApple = (int) state[2]; // These stay here for clearness sake
        int depth_OppAteApple = (int) state[3];
        int depth_WeReachedCentre = (int) state[4]; // These stay here for clearness sake
        Direction[] moves;

        // This if statement fetches the possible moves for the snake we are evaluating
        if (maximizingPlayer) {
            shead = our_snake.body.getFirst();
            moves = notLosingAlgorithm(state, maximizingPlayer); // Get moves for our snake
        } else {
            shead = opp_snake.body.getFirst();
            moves = notLosingAlgorithm(state, maximizingPlayer); // Get moves for the opponent snake
        }

        // Calculate score when we reached a dead end or the max depth
        if (depth == 0 || moves == null) {
            int score = calculateScore(state, maximizingPlayer, moves, depth);
            return score; // The heuristic value of the node
        }
        // Determine the move where we can get the maximum score giving the current state
        if (maximizingPlayer) { // Our snake
            int calc_score = Integer.MIN_VALUE; // The worst score possible
            for (Direction move : moves) { // Each child (also includes the childs killing itself, we filter this out by calculating the score)
                Coordinate newHead = Coordinate.add(shead, move.v);
                if (newHead.equals(opp_snake.body.getLast()) && distanceBetween(opp_snake.body.getFirst(), apple) == 1) {
                    return scr_WeCantMove;
                }
                Object[] updatedState = newState.clone(); // We clone newState, otherwise editing the updatedState will also edit newState

                Snake newSnake = our_snake.clone(); // We do the same thing as above
                // TODO find a better way for the below if the statements
                if (!newHead.equals(apple)) { // If this is not true we keep the last block of our snake, because we have eaten the apple
                    newSnake.body.removeLast(); // Remove the last block of our snake if we have not eaten the apple
                } else {
                    if (depth_OppAteApple == -1) { // If the opponent has not eaten the apple before we reached it
                        updatedState[2] = depth / 2;
                    }
                }
                if (newHead.equals(centre)) { // We reached the centre
                    updatedState[4] = depth / 2;
                }
                newSnake.body.addFirst(newHead); // Update the newSnake with its new head position
                updatedState[0] = newSnake; // Store the new snake in updatedState[0]

                int new_score = minmax(state, updatedState, depth - 1, alpha, beta, false); // Get the score of this move

                if (new_score > calc_score) { // If the score of this move is better than the best score we had, store the new move
                    calc_score = new_score;
                    if (depth == maxDepth) { // Store the best move for the snake to do
                        bestMove = move;
                        bestScore = new_score;
                    }
                }
                alpha = Math.max(alpha, new_score);
                if (alpha >= beta) {
                    break;
                }
            }
            return calc_score; // Return the best score for this state (the move we will most likely do)

        } else { // For the opponent we know the move to get the worst score possible for our snake, because this is the best move
            int calc_score = Integer.MAX_VALUE; // The best score possible
            for (Direction move : moves) { // The rest works the same as above unless there is a comment
                Coordinate newHead = Coordinate.add(shead, move.v);
                Object[] updatedState = newState.clone();
                Snake newSnake = opp_snake.clone();

                if (!newHead.equals(apple)) {
                    newSnake.body.removeLast();
                } else {
                    if (depth_WeAteApple == -1) { // Our snake has not yet eaten the apple
                        newState[3] = (depth + 1) / 2;
                    }
                }
                newSnake.body.addFirst(newHead);
                updatedState[1] = newSnake; // Opp snakes move
                int new_score = minmax(state, updatedState, depth - 1, alpha, beta, true);
                if (new_score < calc_score) { // If the score of this move is worse than the worst score we had, store the new move
                    calc_score = new_score;
                }
                beta = Math.min(beta, new_score);
                if (alpha >= beta) {
                    break;
                }
            }
            return calc_score; // Return the worst score possible for this state (the move the opponent will most likely do)
        }
    }

    /**
     * Calculates the heuristic score of the state
     *
     * @param state      The new positions of the snake after a move. state[0] = our_snake, state[1] = opponent_snake, state[2] = weAteApple, state[3] = oppAteApple
     * @param isOurSnake Is true when we are looking at out snake (thus maximsing our score), is false otherwise
     * @param moves      The moves that makes the snake not collide with itself, or the opponent
     * @param depth      Depth of graph --> depth/2 = How many moves foward we are currently looking
     * @return The heuristic score of the state
     */
    private int calculateScore(Object[] state, boolean isOurSnake, Direction[] moves, int depth) {
        if (exit) {
            throw new NullPointerException();
        }
        int score = 0;
        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];

        int depth_weAteApple = (int) state[2];
        int depth_oppAteApple = (int) state[3];
        int depth_WeReachedCentre = (int) state[4];
        boolean noOneAteApple = (depth_weAteApple == -1 && depth_oppAteApple == -1);
        Coordinate snake_head = our_snake.body.getFirst();
        Coordinate opp_head = opp_snake.body.getFirst();
        int our_snake_size = our_snake.body.size();
        int opp_snake_size = opp_snake.body.size();
        int diff_size = our_snake_size - opp_snake_size;

        // Give an incentive to move towards the apple if it has not been eaten
        score += circleScore(state);

        // Give an incentive to move towards the apple if it has not been eaten
        score += ScoreForGoingSomewhere(noOneAteApple, depth_weAteApple, depth_oppAteApple, depth_WeReachedCentre, snake_head);

        // If this state leads to a dead end
        score += scoreDeadEnd(isOurSnake, moves, depth);

        // Check for head collision
        score += scoreHeadCollision(snake_head, opp_head, diff_size);

        // Check for other collisions
        score += scoreCollisions(our_snake, opp_snake);

        // TODO fill in more parameters

        return score;
    }

    private int ScoreForGoingSomewhere(boolean noOneAteApple, int depth_weAteApple, int depth_oppAteApple, int depth_WeReachedCentre, Coordinate snake_head) {
        int distanceToApple = 0;
        int distanceToCentre = 0;
        int score = 0;
        if (noOneAteApple) {
            distanceToApple += distanceBetween(snake_head, apple);
        } else { // Give an incentive to move towards the centre so there is a better chance to reach the next apple
            if (depth_weAteApple == -1) { // The opponent ate the apple
                score += scr_OppAteApple - depth_oppAteApple;
                if (!(depth_WeReachedCentre == -1)) { // We have no reached the apple
                    score += scr_weReachedCentre + depth_WeReachedCentre * 10;
                }
                distanceToCentre += distanceBetween(snake_head, centre);
            } else { // We ate the apple
                score += scr_WeAteApple + depth_weAteApple;
            }
        }
        return (score + (fct_distanceToApple * distanceToApple + fct_distanceToCentre * distanceToCentre));
    }

    private int scoreHeadCollision(Coordinate snake_head, Coordinate opp_head, int diff_size) {
        int GoodHeadColl = 0;
        int BadHeadColl = 0;
        if (snake_head.equals(opp_head)) {
            if (diff_size > 0) {
                GoodHeadColl += 1;
            } else if (diff_size < 0) {
                BadHeadColl += 1;
            }
        }
        return (scr_BadHeadColl * BadHeadColl
                + scr_GoodHeadColl * GoodHeadColl);
    }

    private int scoreDeadEnd(boolean isOurSnake, Direction[] moves, int depth) {
        if (moves == null) {
            int WeCantMove = 0;
            int OppCantMove = 0;
            if (isOurSnake) { // We went to a dead end
                WeCantMove += 1;
            } else {    // The opponent went to a dead end
                OppCantMove += 1;
            }
            return (scr_WeCantMove * WeCantMove
                    + scr_OppCantMove * OppCantMove) * depth + 1;
        } else {
            return 0;
        }
    }

    private int scoreCollisions(Snake our_snake, Snake opp_snake) {
        final Coordinate snake_head = our_snake.body.getFirst();
        final Coordinate opp_head = opp_snake.body.getFirst();
        int WeCantMove = 0;
        int OppCantMove = 0;

        Deque our_snake_nohead = our_snake.body; // Make a new snake where the head is removed, to check if the head is somewhere else in the snake
        our_snake_nohead.removeFirst();            // NOTE: check_snake now also does not have a head
        Deque opp_snake_nohead = opp_snake.body;
        opp_snake_nohead.removeFirst();              // NOTE: opp_snake now also does not have a head

        //Check collisions
        if (our_snake_nohead.contains(snake_head) || opp_snake_nohead.contains(snake_head)) { // We collide with something
            WeCantMove += 1;
        }
        if (our_snake_nohead.contains(opp_head) || opp_snake_nohead.contains(opp_head)) { // The opponent collided with itself, so this is good
            OppCantMove += 1;
        }

        // Add head back again
        our_snake_nohead.addFirst(snake_head);
        opp_snake_nohead.addFirst(opp_head);

        return (scr_WeCantMove * WeCantMove
                + scr_OppCantMove * OppCantMove);
    }

    private int circleScore(Object[] state) {
        Snake our_snake = (Snake) state[0];
        int opp_snake_size = ((Snake) state[1]).body.size();
        int our_snake_size = our_snake.body.size();
        int depth_weAteApple = (int) state[2];
        int depth_oppAteApple = (int) state[3];
        int diff_size = our_snake_size - opp_snake_size;
        boolean noOneAteApple = (depth_weAteApple == -1 && depth_oppAteApple == -1);
        int losingLead = 0;
        int circle_part1 = 0;
        int circle_part2 = 0;
        Coordinate snake_head = our_snake.body.getFirst();


        if (diff_size >= 1) { // We're in the lead, no need to eat the apple
            if (depth_oppAteApple > -1) {
                losingLead += 1; // Because the opponent ate the apple we lost the lead
            } else if (noOneAteApple && our_snake_size >= 6 && our_snake.body.size() % 2 == 0) { // We can possibly encircle the position
                // TODO: define onRightSpotForCircle(check_snake) with Wolf's algrotihm
                circle_part1 = onRightSpotForCircle(our_snake); // Calculate the probability of the circle working
                if (circle_part1 > 0) { // There is a chance to circle
                    if (circle_part1 == 1) { // Not fully enclosed
                        if (diff_size >= 2 && distanceBetween(snake_head, our_snake.body.getLast()) == 1) {
                            circle_part2 = 1;
                        }
                    } else { // Fully enclosed
                        if (distanceBetween(snake_head, our_snake.body.getLast()) == 1) { // TODO edit this, so it will follow its tail
                            circle_part2 = 1;
                        }
                    }
                }
            }
        }
        return (scr_circlePart1 * circle_part1
                + scr_circlePart2 * circle_part2
                + scr_losingLead * losingLead);
    }


    /**
     * Set direction from one point to other point
     *
     * @param our_snake point where we want to know the distance towards the apple from
     *                  appleSpots: the coordinates the snake needs to cover
     * @return direction // TODO direction? (Bram)
     */
    private int onRightSpotForCircle(Snake our_snake) {
        int arraySize = appleSpots.length;
        int spotsCovered = 0;
        for (int i = 0; i < arraySize; i++) {
            if (our_snake.body.contains(appleSpots[i])) { // We are on one of the spots
                spotsCovered++;
            }
        }

        if (spotsCovered >= arraySize) { // We covered at least all but one spot > exiting is hard
            return 1; // Good scenario
        } else {
            return 0; // Nothing special
        }
    }


    /**
     * Calculates manhattan distance between point 1, and point 2
     *
     * @param point1 Coordinate
     * @param point2 Coodrinate
     * @return distance
     */
    private int distanceBetween(Coordinate point1, Coordinate point2) {
        return Math.abs(point1.x - point2.x) + Math.abs(point1.y - point2.y);
    }

    /**
     * Set direction from one point to other point
     *
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
     * The possible moves the snake can use
     *
     * @param cur_state  the current state
     * @param isOurSnake if we want to determine the moves for our snake
     * @return direction
     */
    private Direction[] notLosingAlgorithm(Object[] cur_state, Boolean isOurSnake) {
        if (exit) {
            throw new NullPointerException();
        }
        Snake move_snake; // the snake were we want to determine the possible moves of
        Snake opp_snake;
        boolean bool_weAteApple = false;
        boolean bool_oppAteApple = false;
        if (isOurSnake) { // Assign the right snakes and check if the apple has been eaten
            move_snake = (Snake) cur_state[0];
            opp_snake = (Snake) cur_state[1];
            if ((int) cur_state[2] > -1) {
                bool_weAteApple = true;
            } else if ((int) cur_state[3] > -1) {
                bool_oppAteApple = true;
            }
        } else {
            move_snake = (Snake) cur_state[1];
            opp_snake = (Snake) cur_state[0];
            if ((int) cur_state[3] > -1) {
                bool_weAteApple = true;
            } else if ((int) cur_state[2] > -1) {
                bool_oppAteApple = true;
            }
        }


        Coordinate snakehead = move_snake.getHead();

        // Get the coordinate of the second element of the snake's body to prevent going backwards
        Coordinate afterHeadNotFinal = null;
        if (move_snake.body.size() >= 2) {
            Iterator<Coordinate> it = move_snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }

        final Coordinate afterHead = afterHeadNotFinal;

        // The only illegal move is going backwards. Here we are checking for not doing it
        Direction[] validMoves = Arrays.stream(DIRECTIONS)
                .filter(d -> !snakehead.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);

        final boolean weAteApple = bool_weAteApple;
        final boolean oppAteApple = bool_oppAteApple;

        // Just naïve greedy algorithm that tries not to die at each moment in time
        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> snakehead.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                .filter(d -> (!opp_snake.body.contains(snakehead.moveTo(d))) || (snakehead.moveTo(d).equals(opp_snake.body.getLast()) && !oppAteApple))   // Don't collide with opponent...
                .filter(d -> (!move_snake.body.contains(snakehead.moveTo(d))) || (snakehead.moveTo(d).equals(move_snake.body.getLast()) && !weAteApple))     // and yourself
                .sorted()
                .toArray(Direction[]::new);

        if (notLosing.length > 0) return notLosing;
        else return null; // No possible moves
    }

    /* Do a valid move when no moves possible
     *
     */
    private Direction doValidMove(Snake snake1, Snake opponent1, Coordinate mazeSize) {
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

        return validMoves[0];
    }
}

