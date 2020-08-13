package minmax;

import com.sun.applet2.AppletParameters;
import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;
import java.lang.*;

/**
 * The default snake where I made variations on
 */
public class minmax implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    private Snake snake = null;
    private Snake opponent = null;
    private Coordinate mazeSize = null;
    private Coordinate apple = null;
    private HashMap <Object[], Object[]> wentTo = new HashMap<Object[], Object[]>(); // Went  from state x to state y
    private HashMap <Object[], Object[]> bestTo = new HashMap<Object[], Object[]>(); // The best state to go to from state x
    private HashMap <Object[], Direction> snake_moveTo = new HashMap<Object[], Direction>(); // The best move to go to from coordinate x
    private HashMap <Object[], Direction> opp_moveTo = new HashMap<Object[], Direction>();
    private HashMap <Object[], Integer> state_score = new HashMap<Object[], Integer>();
    private int maxDepth = 8;  // How deep we will go with the minmax algorithm.
    // A higher number results in a better prediction, but a slower runtime
    // How many steps we look fowards is maxDepth/2



    /**
     * Choose the direction (not rational - silly)
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

    private void preVariableAssignment(Snake snake1, Snake opponent1, Coordinate mazeSize1, Coordinate apple1) {
        snake = snake1;
        opponent = opponent1;
        mazeSize = mazeSize1;
        apple = apple1;

        // Reset the graphs
        wentTo.clear();
        wentTo.clear();
        state_score.clear();
        snake_moveTo.clear();
        opp_moveTo.clear();
    }

    private Direction makeMinMax() {
        Coordinate original_head = snake.getHead();
        //snake_depth.put(head, 0);
        Object[] cur_state = {snake.clone(), opponent.clone(), false, false};
        minmax(cur_state, maxDepth, true); // minimax(origin, depth, TRUE)
        /*Direction move = Direction.UP;
        int score = Integer.MIN_VALUE;
        Direction test = null;
        for (Direction option : notLosingAlgorithm(snake.clone(), opponent.clone())) { // Finds the move resulting in the highest score
//            snake_score = snake_score;
//            opponent_score = opponent_score;
            System.out.println("We had a good run son");
            try {
                int newScore = snake_score.get(Coordinate.add(option.v, original_head));
                if (newScore > score) {
                    move = option;
                    score = newScore;
                }
            } catch(Exception NullPointerException){ // TODO check why this happens
                System.out.println("We had a good run son");
            }
        } */
        state_score = state_score;
        bestTo = bestTo;
        Direction move = snake_moveTo.get(cur_state);
        System.out.println("Going " + snake_moveTo.get(cur_state) + ", score:" + state_score.get(cur_state));
        if (move != null) {
            return snake_moveTo.get(cur_state);
        } else {
            System.out.println("We had a good run son");
            return Direction.UP;
        }
    }

    /**
     * Choose the direction (not rational - silly)
     * @param state             The new positions of the snake after a move. state[0] = snake, state[1] = opponent
     * @param depth             The depth of the minmax graph, this is not equal to the distance like BFS
     * @param maximizingPlayer  Is true when we are looking at out snake (thus maximsing our score), is false otherwise
     * @return Direction of bot's move
     */
    private int minmax(Object[] state, int depth, boolean maximizingPlayer) {
        Coordinate shead = null; // The head of the new snake
        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];
        boolean we_ateApple = (boolean) state[2];
        boolean opp_ateApple = (boolean) state[3];
        Boolean hasEatenApple = (Boolean) state[2];
        Direction[] moves;
        if (maximizingPlayer) {
            shead = our_snake.body.getFirst();
            moves = notLosingAlgorithm(our_snake, opp_snake);
        } else {
            shead = opp_snake.body.getFirst();
            moves = notLosingAlgorithm(opp_snake, our_snake);
        }
        if (depth == 0 || moves == null) { // The node is a terminal node --> It does not have any children
            int score = calculateScore(state, maximizingPlayer, moves, depth);
            // This can be used for a better calculation of the parameters
            /*
            if (maximizingPlayer) {
                if (snake_score.containsKey(head)) { // TODO test if this works as intended
                    snake_score.put(head, Math.max(score, snake_score.get(head))); // Compares the score that we had before and strive to obtain the max score
                } else {
                    snake_score.put(head, score);
                }
            } else {
                if (opponent_score.containsKey(head)) {
                    opponent_score.put(head, Math.max(score, opponent_score.get(head)));
                }
                opponent_score.put(head, score);
            } */
            return score; // The heuristic value of the node, to be determined
        }
        if (maximizingPlayer) { // Our snake
            int calc_score = Integer.MIN_VALUE; // -infinity
            for (Direction move : moves) { // Each child (also includes the childs killing itself, we filter this out by calculating the score)
                Coordinate newHead = Coordinate.add(shead, move.v);
                boolean new_we_ateApple = we_ateApple;
                if (newHead.inBounds(mazeSize)) { // TODO we can replace this by only looking at the notLosing moves
                    Snake newSnake = our_snake.clone(); // Snake of previous depth
                    if (!newHead.equals(apple)) { // If we eat the apple, we keep our tail in the new state
                        newSnake.body.removeLast(); // Update how the new snake will look like. NOTE only snake.body is updated
                    } else {
                        new_we_ateApple = true;
                    }
                    newSnake.body.addFirst(newHead); // TODO check if this works
                    Coordinate test = newSnake.getHead();
                    Object[] newState = {newSnake, opp_snake, new_we_ateApple, opp_ateApple};
                    wentTo.put(state, newState);
                    int new_score = minmax(newState, depth-1, false);
                    if (new_score > calc_score) {
                        bestTo.put(state, newState);
                        snake_moveTo.put(state, move);
                        calc_score = new_score;
                    }
                }
            } // calc_score = the best score reachable
            state_score.put(state, calc_score); // Put the maximum points reachable with this state
            return calc_score;
        } else { // Same as above but for the opponent snake
            int calc_score = Integer.MAX_VALUE; // -infinity
            for (Direction move : moves) { // Each child (also includes the childs killing itself, we filter this out by calculating the score)
                Coordinate newHead = Coordinate.add(shead, move.v);
                boolean new_opp_ateApple = opp_ateApple;
                if (newHead.inBounds(mazeSize)) { // TODO we can replace this by only looking at the notLosing moves
                    Snake newSnake = opp_snake.clone(); // Snake of previous depth
                    if (!newHead.equals(apple)) { // If we eat the apple, we keep our tail in the new state
                        newSnake.body.removeLast(); // Update how the new snake will look like. NOTE only snake.body is updated
                    } else {
                        new_opp_ateApple = true;
                    }
                    newSnake.body.addFirst(newHead); // TODO check if this works
                    Object[] newState = {our_snake, newSnake, we_ateApple, new_opp_ateApple};
                    wentTo.put(state, newState);
                    int new_score = minmax(newState, depth-1, true);
                    if (new_score < calc_score) {
                        bestTo.put(state, newState);
                        snake_moveTo.put(state, move);
                        calc_score = new_score;
                    }
                }
            } // calc_score = the worst score reachable
            state_score.put(state, calc_score); // Put the minimum points reachable with this state
            return calc_score;
        }
    }


    /**
     * Calculates the heuristic score of the state
     * @param state             The new positions of the snake after a move. state[0] = snake, state[1] = opponent
     * @param isOurSnake        Is true when we are looking at out snake (thus maximsing our score), is false otherwise
     * @return The heuristic score of the state
     */
    private int calculateScore(Object[] state, boolean isOurSnake, Direction[] moves, int depth) {
        int score = 0;
        Snake check_snake = null;
        Snake opp_snake = null;
        boolean weHaveEatenApple = false;
        boolean oppHasEatenApple = false;
        if (isOurSnake){
            check_snake = (Snake) state[0];
            opp_snake = (Snake) state[1];
            weHaveEatenApple = (boolean) state[2];
            oppHasEatenApple = (boolean) state[3];
        } else {
            check_snake = (Snake) state[1];
            opp_snake = (Snake) state[0];
            weHaveEatenApple = (boolean) state[3];
            oppHasEatenApple = (boolean) state[2];
        }
        Coordinate snake_head = check_snake.body.getFirst();
        Coordinate opp_head = opp_snake.body.getFirst();
        Snake check_snake_nohead = check_snake.clone(); // Make a new snake where the head is removed, to check if the head is somewhere else in the snake
        check_snake_nohead.body.removeFirst();          // TODO check if it works like this, otherwise remove it
        Snake opp_snake_nohead = opp_snake.clone();
        opp_snake_nohead.body.removeFirst();

        if (moves == null) {
            score -= 100000; // No move can save him now
        }
        /*
        if (check_snake_nohead.body.contains(snake_head) || opp_snake.body.contains(snake_head)) {
            if (snake_head.equals(opp_head)){ // Head collision
                // TODO check if this happens in the same move, currently disabled to test general function
                if (snake.body.size() > opp_snake.body.size()) {
                    score += 170000; // We won, because we were bigger when we collided
                }
            } else {
                score -= 100000; // The snake lost so this is not good
            }
        } */
        // TODO check if this has become redundant
        if (check_snake.body.contains(opp_head) || opp_snake_nohead.body.contains(opp_head)) { // The opponent collided with itself, so this is good
            score += 50000;
        } // If there is a head collision then this parameter will even out

        if (snake_head.equals(apple)) { // We have eaten the apple in our path
            score += 1000; // Add 1000 points if ate the apple somewhere in our path
            // TODO edit the depth of this
        } else if (!weHaveEatenApple) {
            score -= distanceToApple(snake_head); // Rewards getting closer to the apple
        }
        if (oppHasEatenApple) {
            score -= 500; // Substracts points if the opponent has eaten the apple before us
        }
        score = (int) ((double) score * (1.00-((maxDepth - depth) / 100.0)));
        // TODO fill in more parameters
        return score;
    }

    private int distanceToApple(Coordinate position) {
        int distance = Math.abs(position.x - apple.x);
        distance += Math.abs(position.y - apple.y);
        return distance;
    }

    /*private boolean isLeaf(Direction moves) {
        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];
        Boolean hasEatenApple = (Boolean) state[2];
        if (isOurSnake) {
            return (notLosingAlgorithm(our_snake, opp_snake).length > 3); // TODO can be improved a lot
        } else {
            return (notLosingAlgorithm(opp_snake, our_snake).length > 3);
        }
    } */


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


    private Direction[] notLosingAlgorithm(Snake move_snake, Snake opp_snake) {

        Coordinate move_head = move_snake.getHead();
        // Direction[] notLosing = Direction.values();
        ArrayList notLosing = new ArrayList();
        boolean snakeAteApple = move_snake.body.contains(apple);
        boolean oppAteApple = opp_snake.body.contains(apple);
        for (Direction move : Direction.values()) {
            Coordinate newPos = Coordinate.add(move_head, move.v);

            if (newPos.inBounds(mazeSize)) { // Stays in the zone
                if (!move_snake.body.contains(newPos) || (!snakeAteApple && move_snake.body.getLast().equals(newPos))) { // Does not collide with itself
                    if (!opp_snake.body.contains(newPos) || (!oppAteApple && opp_snake.body.getLast().equals(newPos))) { // Does not collide with opponent
                        notLosing.add(move);
                    }
                }
            }
            /*
            if (newPos.inBounds(mazeSize)) { // Stays in the zone
                if (!move_snake.body.contains(newPos) || move_snake.body.getLast().equals(newPos)) { // Does not collide with itself
                    if (!opp_snake.body.contains(newPos) || opp_snake.body.getLast().equals(newPos)) { // Does not collide with opponent
                        notLosing.add(move);
                    }
                }
               } */
        }
        if (!notLosing.isEmpty()) {
            return (Direction[]) notLosing.toArray(new Direction[notLosing.size()]);
        } else{
            return null;
        }
    }

/*
        private Direction[] notLosingAlgorithm(Snake move_snake, Snake opponent_snake) {
            Coordinate snakehead = move_snake.getHead();

            // Get the coordinate of the second element of the snake's body
            // to prevent going backwards
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

            /* Just naïve greedy algorithm that tries not to die at each moment in time
            Direction[] notLosing = Arrays.stream(validMoves)
                    .filter(d -> snakehead.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                    .filter(d -> !opponent_snake.body.contains(snakehead.moveTo(d)))   // Don't collide with opponent...
                    .filter(d -> !move_snake.body.contains(snakehead.moveTo(d)))      // and yourself
                    .sorted()
                    .toArray(Direction[]::new);

            if (notLosing.length > 0) return notLosing;
            else return null;
    } */
}
