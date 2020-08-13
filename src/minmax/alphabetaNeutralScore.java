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
public class alphabetaNeutralScore implements Bot {
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
    private int scr_AteApple = 500;
    private int scr_losingLead = -1250;
    private int scr_CantMove = -20000;
    private int scr_OppCanatMove = 7500;
    private int scr_GoodHeadColl = 20;
    private int scr_BadHeadColl = -20000;
    private int scr_ReachedCentre = 100;

    private int fct_distanceToApple = -5;
    private int fct_distanceToCentre = -2;

    /**
     * Choose the direction and clones the snakes so we don't mess with the ingame snake when removing elements of its body
     * @param snake1    Your snake's body with coordinates for each segment
     * @param opponent1 Opponent snake's body with coordinates for each segment
     * @param mazeSize1 Size of the board
     * @param apple1    Coordinate of an apple
     * @return Direction of bot's move
     */
    @Override
    public Direction chooseDirection(Snake snake1, Snake opponent1, Coordinate mazeSize1, Coordinate apple1) {
        preVariableAssignment(snake1.clone(), opponent1.clone(), mazeSize1, apple1);
        return makeMinMax();
    }

    /**
     * Resets the variables
     * @param snake1    Your snake's body with coordinates for each segment
     * @param opponent1 Opponent snake's body with coordinates for each segment
     * @param mazeSize1 Size of the board
     * @param apple1    Coordinate of an apple
     */
    private void preVariableAssignment(Snake snake1, Snake opponent1, Coordinate mazeSize1, Coordinate apple1) {
        if (snake1.body.size() < 5) { // Reset variables at new snake run
            maxDepth = 16;
            chase_tail = false;
        } else {
            maxDepth = 16;
        }

        snake = snake1;
        opponent = opponent1;
        mazeSize = mazeSize1;
        apple = apple1;
        centre = new Coordinate(mazeSize.x/2,mazeSize.y/2);

        bestMove = null;
        bestScore = Integer.MIN_VALUE;
        appleSpots = null;
    }

    /**
     * The instruction to make a minmax graph
     * This uses the snake, opponent, we stored in makeminMax
     * @return Direction of bot's move
     */
    private Direction makeMinMax() {
        if (chase_tail) { // We won
            return directionFromTo(snake.getHead(), snake.body.getLast());
        } else { // Do the normal strategy
            long startTime = System.currentTimeMillis(); // We have this to check the runtime
            getAppleSpots();
            Object[] cur_state = {snake.clone(), opponent.clone(), -1, -1, -1, -1};
            minmax(cur_state, cur_state, maxDepth, Integer.MIN_VALUE, Integer.MAX_VALUE ,true); // minimax(state, newstate, depth, alpha, beta, maximizing player)
            long endTime = System.currentTimeMillis();
            long timeElapsed = endTime - startTime;
            System.out.println("Going " + bestMove + ", score:" + bestScore + ", runtime (ms): " + timeElapsed);
            if (distanceBetween(snake.getHead(), snake.body.getLast()) == 1 && onRightSpotForCircle(snake) == 2) {
                chase_tail = true;
                return directionFromTo(snake.getHead(), snake.body.getLast());
            } else {
                if (bestMove != null) {
                    return bestMove;
                } else {
                    System.out.println("We had a good run son");
                    return Direction.UP;
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
     * @param state             The new positions of the snake after a move. state[0] = our_snake, state[1] = opponent_snake, state[2] = depth_WeAteApple, state[3] = depth_OppAteApple
     * @param newState          The new proposed state which will replace state if depth is equal: both players have made a move
     * @param depth             The depth of the minmax graph, this is not equal to the distance like BFS
     * @param maximizingPlayer  Is true when we are looking at out snake (thus maximsing our score), is false otherwise
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
        int depth_OppReachedCentre = (int) state[5];
        Direction[] moves;

        shead = our_snake.body.getFirst();
        moves = notLosingAlgorithm(state, maximizingPlayer); // Get moves for our snake

        // Calculate score when we reached a dead end or the max depth
        if (depth == 0 || moves == null) {
            int score = getRightScore(state, maximizingPlayer, moves, depth);
            return score;
        }

        // Determine the move where we can get the maximum score giving the current state
        if (maximizingPlayer) { // Our snake
            int calc_score = Integer.MIN_VALUE; // The worst score possible
            for (Direction move : moves) { // Each child (also includes the childs killing itself, we filter this out by calculating the score)
                Coordinate newHead = Coordinate.add(shead, move.v);
                Object[] updatedState = newState.clone(); // We clone newState, otherwise editing the updatedState will also edit newState
                Snake newSnake = our_snake.clone(); // We do the same thing as above
                // TODO find a better way for the below if the statements
                if (!newHead.equals(apple)) { // If this is not true we keep the last block of our snake, because we have eaten the apple
                    newSnake.body.removeLast(); // Remove the last block of our snake if we have not eaten the apple
                } else {
                    if (depth_OppAteApple == -1) { // If the opponent has not eaten the apple before we reached it
                        updatedState[2] = depth/2;
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
                        newState[3] = (depth+1)/2 ;
                    }
                }
                if (newHead.equals(centre)) { // We reached the centre
                    updatedState[5] = depth / 2;

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

    private int getRightScore(Object[] state, boolean maximizingPlayer, Direction[] moves, int depth) {
        if (!maximizingPlayer) {
            // Flip state and return negative score
            int score = -calculateScore(new Object[] {state[1], state[0], state[3], state[2], state[5], state[4]}, moves, depth);
            return score;
        } else {
            return calculateScore(state, moves, depth);
        }

    }

    /**
     * Calculates the heuristic score of the state for
     * @param state             The new positions of the snake after a move. state[0] = our_snake, state[1] = opponent_snake, state[2] = weAteApple, state[3] = oppAteApple
     * @param isOurSnake        Is true when we are looking at out snake (thus maximsing our score), is false otherwise
     * @param moves             The moves that makes the snake not collide with itself, or the opponent
     * @param depth             Depth of graph --> depth/2 = How many moves foward we are currently looking
     * @return The heuristic score of the state
     */
    private int calculateScore(Object[] state, Direction[] moves, int depth) {
        int score = 0;

        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];
        int depth_weAteApple = (int) state[2]; // We keep these for clearity purposes
        int depth_oppAteApple = (int) state[3];
        int depth_WeReachedCentre = (int) state[4];
        boolean noOneAteApple = ((int) depth_weAteApple == -1 && depth_oppAteApple == -1);
        Coordinate snake_head = our_snake.body.getFirst();
        Coordinate opp_head = opp_snake.body.getFirst();

        // Check circle enclosing
        score += getScoreCircleStrat(our_snake, opp_snake, noOneAteApple, depth_weAteApple, depth_oppAteApple);

        // Give an incentive to move towards the apple if it has not been eaten
        score += getScoreAppleFactor(snake_head, opp_head, noOneAteApple, state);

        // Substract score if this state leads to a dead end
        if (moves == null) {
            score += scr_CantMove;
        }

        // Check for head collision and if we win with this headcollision
        score -= getScoreHeadCollision(snake_head, opp_head, our_snake.body.size(), opp_snake.body.size());

        // Check for Snake collisions and the outcome of them
        score += getScoreCollisions(our_snake, opp_snake, snake_head, opp_head);

        return score;
        // TODO fill in more parameters
    }

    private int getScoreCollisions(Snake our_snake, Snake opp_snake, Coordinate snake_head, Coordinate opp_head) {
        int WeCantMove = 0; // moves = null or our head is in our snake or the opponent --> defeat

        Deque our_snake_nohead = our_snake.body; // Make a new snake where the head is removed, to check if the head is somewhere else in the snake
        our_snake_nohead.removeFirst();            // NOTE: check_snake now also does not have a head
        Deque opp_snake_nohead = opp_snake.body;
        opp_snake_nohead.removeFirst();              // NOTE: opp_snake now also does not have a head

        if (our_snake_nohead.contains(snake_head) || opp_snake_nohead.contains(snake_head)) { // We collide with something
            WeCantMove += 1;
        }

        if (our_snake_nohead.contains(opp_head) || opp_snake_nohead.contains(opp_head)) { // The opponent collided with itself, so this is good
            WeCantMove -= 1 ;
        }


        // Add head back again
        our_snake_nohead.addFirst(snake_head);
        opp_snake_nohead.addFirst(opp_head);

        return (scr_CantMove * WeCantMove);
    }

    private int getScoreHeadCollision(Coordinate snake_head, Coordinate opp_head, int snake_size, int opp_snake_size) {
        int GoodHeadColl = 0;

        if (snake_head.equals(opp_head)) {
                if (snake_size > opp_snake_size) {
                    GoodHeadColl += 1;
                } else if (!(snake_size == opp_snake_size)){
                    GoodHeadColl -= 1;
                }
        }
        return (scr_GoodHeadColl * GoodHeadColl);
    }

    private int getScoreAppleFactor(Coordinate snake_head, Coordinate opp_head,  boolean noOneAteApple, Object[] state) {
        int distanceToApple = 0;
        int distanceToCentre = 0;
        int score = 0;
        int depth_weAteApple = (int) state[2];
        int depth_oppAteApple = (int) state[3];
        int depth_WeReachedCentre = (int) state[4];
        int depth_OppReachedCentre = (int) state[5];

        if (noOneAteApple) {
            distanceToApple += distanceBetween(snake_head, apple);
            distanceToApple -= distanceBetween(opp_head, apple);
        } else { // Give an incentive to move towards the centre so there is a better chance to reach the next apple
            if (depth_weAteApple == -1) { // The opponent ate the apple
                score -= (scr_AteApple + depth_oppAteApple);
                if (!(depth_WeReachedCentre == -1)) { // We have reached the centre
                    score += (scr_ReachedCentre - depth_WeReachedCentre);
                }
                distanceToCentre += distanceBetween(snake_head, centre);
            } else { // Score for opponent
                score += (scr_AteApple + depth_weAteApple);
                if (!(depth_OppReachedCentre == -1)) { // Opp hass reached the centre
                    score -= (scr_ReachedCentre - depth_OppReachedCentre);
                }
                distanceToCentre -= distanceBetween(opp_head, centre);
            }
        }
        score += (fct_distanceToApple * distanceToApple
                + fct_distanceToCentre * distanceToCentre);
        return score;
    }

    /**
     * TODO check if the we ate the apple --> circling is useless
     * Calculate if this state is a circle state and returns the score of the circle state
     * @param our_snake
     * @param opp_snake
     * @param noOneAteApple
     * @param depth_oppAteApple
     * @return score
     */
    private int getScoreCircleStrat(Snake our_snake, Snake opp_snake, boolean noOneAteApple, int depth_WeAteApple, int depth_oppAteApple) {
        int our_snake_size = our_snake.body.size();
        int opp_snake_size = opp_snake.body.size();
        int diff_size = our_snake_size - opp_snake_size;
        int losingLead = 0;
        int circle_part1 = 0;
        int circle_part2 = 0;

        if (diff_size >= 1) { // We're in the lead, no need to eat the apple
            if (depth_oppAteApple > -1) {
                losingLead += 1; // Because the opponent ate the apple we lost the lead
            } else if (noOneAteApple && our_snake_size >= 8 && our_snake.body.size()%2 == 0) { // We can possibly encircle the position
                // TODO: define onRightSpotForCircle(check_snake) with Wolf's algrotihm
                circle_part1 = onRightSpotForCircle(our_snake); // Calculate the probability of the circle working
                if (circle_part1 > 0) { // There is a chance to circle
                    if (distanceBetween(our_snake.body.getFirst(), our_snake.body.getLast()) == 1) { // TODO edit this, so it will follow its tail
                        circle_part2 = 1;
                    }
                }
            }
        } else if (diff_size <= -1) {
            if (depth_WeAteApple > -1) {
                losingLead -= 1; // Because the opponent ate the apple we lost the lead
            } else if (noOneAteApple && opp_snake_size >= 8 && opp_snake.body.size()%2 == 0) { // We can possibly encircle the position
                // TODO: define onRightSpotForCircle(check_snake) with Wolf's algrotihm
                circle_part1 = -onRightSpotForCircle(opp_snake); // Calculate the probability of the circle working
                if (circle_part1 < 0) { // There is a chance to circle
                    if (distanceBetween(opp_snake.body.getFirst(), our_snake.body.getLast()) == 1) { // TODO edit this, so it will follow its tail
                        circle_part2 = -1;
                    }
                }
            }
        }
        return (scr_losingLead * losingLead
                + scr_circlePart1 * circle_part1
                + scr_circlePart2 * circle_part2);

    }


    /**
     * Set direction from one point to other point
     * @param our_snake point where we want to know the distance towards the apple from
     * appleSpots: the coordinates the snake needs to cover
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

        if (spotsCovered >= arraySize - 1) { // We covered at least all but one spot > exiting is hard
            if (spotsCovered == arraySize) {
                return 2; // Best scenario
            }
            return 1; // Good scenario
        } else {
            return 0; // Nothing special
        }

    }


    /**
     * Calculates manhattan distance between point 1, and point 2
     * @param point1 Coordinate
     * @param point2 Coodrinate
     * @return distance
     */
    private int distanceBetween(Coordinate point1, Coordinate point2) {
        return Math.abs(point1.x - point2.x) + Math.abs(point1.y - point2.y);
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
     * The possible moves the snake can use
     * @param cur_state the current state
     * @param isOurSnake if we want to determine the moves for our snake
     * @return direction
     */
    private Direction[] notLosingAlgorithm(Object[] cur_state, Boolean isOurSnake) {
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
}



