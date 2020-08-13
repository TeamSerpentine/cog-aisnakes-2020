package dik;


import snakes.*;

import java.math.BigInteger;
import java.security.cert.TrustAnchor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QTableBot implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    private HashMap<String, double[]> QTable = new HashMap<>();
    private double epsilon = 0.5;
    private double epsilon_decay = 0.90;
    private double min_epsilon = 0.01;
    private final double gamma = 0.90;
    private final double alpha = 0.05;
    private Random rng = new Random();

    private boolean training = true;

    /**
     * Choose the direction (not rational - silly)
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        // Convert state in to useful obs
        Integer[][] obs = toBoard(snake, opponent, mazeSize, apple);
        if (rng.nextDouble() < epsilon || !QTable.containsKey(obs)) {
            Coordinate head = snake.getHead();
            /* Just naïve greedy algorithm that tries not to die at each moment in time */
            Direction[] notLosing = Arrays.stream(DIRECTIONS)
                    .filter(d -> head.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                    .filter(d -> !opponent.elements.contains(head.moveTo(d)))   // Don't collide with opponent...
                    .filter(d -> !snake.elements.contains(head.moveTo(d)))      // and yourself
                    .sorted()
                    .toArray(Direction[]::new);
//        if (!QTable.containsKey(Arrays.deepToString(obs))) {
//            // Random valid move
//            double[] smartQValues = new double[4];
//            Arrays.fill(smartQValues, -10);
//            for (Direction goodDir : notLosing) {
//                smartQValues[directionToInt(goodDir)] = 10;
//            }
//            QTable.put(Arrays.deepToString(obs), smartQValues);
//
//        }
            // Check epsilon greedy or if we have not seen it before
            if (notLosing.length > 0) return notLosing[rng.nextInt(notLosing.length)];
            return DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
        }
        if (!QTable.containsKey(Arrays.deepToString(obs)) && !training) {
            System.out.println("Unkown State");
        }
        // Decide on an action
        double[] QValues = QTable.getOrDefault(Arrays.deepToString(obs), new double[]{10, 10, 10, 10});
        int action = argmax(QValues);
        return DIRECTIONS[action];
    }

    @Override
    public void update(TrainingDojoMain.playedGame[] results) {
        for (TrainingDojoMain.playedGame result : results) {
            update_single_backwards(result);
        }
        epsilon *= epsilon_decay;
        epsilon = Math.max(epsilon, min_epsilon);
        System.out.println("QTable size:" + QTable.size() + "\n Current Epsilon:" + epsilon);
    }


    /**
     * updates the Q table by iterating backwards over the result
     * @param result
     */
    private void update_single_backwards(TrainingDojoMain.playedGame result) {
        ArrayList<TrainingDojoMain.state> states = result.states;
        ArrayList<Direction> actions0 = result.actions0;
        ArrayList<Direction> actions1 = result.actions1;

        // Handle last state as it is special
        TrainingDojoMain.state cur_game = states.get(states.size() - 1);
        Integer[][] cur_state = toBoard(cur_game.snake0, cur_game.snake1, result.mazeSize, cur_game.apple);
        Integer[][] inv_cur_state = toBoard(cur_game.snake1, cur_game.snake0, result.mazeSize, cur_game.apple);

        Direction cur_action0 = actions0.get(states.size() - 1);
        Direction cur_action1 = actions1.get(states.size() - 1);

        if (result.winner == -1) {
            updateQ(cur_state, cur_action0, 10d);
            updateQ(inv_cur_state, cur_action1, -10d);
        } else if (result.winner == 1) {
            updateQ(cur_state, cur_action0, -10d);
            updateQ(inv_cur_state, cur_action1, 10d);
        } else {
            updateQ(cur_state, cur_action0, -1);
            updateQ(inv_cur_state, cur_action1, -1);
        }

        Integer[][] next_state = cur_state;
        Integer[][] inv_next_state = inv_cur_state;

        // Looping Backwards, should improve convergence as it already
        // takes the updated value for the next_state into account
        for (int i = states.size() - 1; i > 1; i--) {
            // Load the current state and convert it to a for our QTable compatible format
            cur_game = states.get(i - 1);
            cur_state = toBoard(cur_game.snake0, cur_game.snake1, result.mazeSize, cur_game.apple);
            // Invert board (switching snake0 and 1 from position) to maximize learning from single example
            inv_cur_state = toBoard(cur_game.snake1, cur_game.snake0, result.mazeSize, cur_game.apple);

            // Calculate the reward from going from the cur_state to the next_state
            double reward0 = calculateReward(cur_state, next_state);
            double reward1 = calculateReward(inv_cur_state, inv_next_state);

            // Update the Q table with
            updateQ(cur_state, next_state, cur_action0, reward0);
            updateQ(inv_cur_state, inv_next_state, cur_action1, reward1);

            //Load values for next iteration
            next_state = cur_state;
            inv_next_state = inv_cur_state;
            cur_action0 = actions0.get(i - 1);
            cur_action1 = actions1.get(i - 1);
        }
    }

    private void update_single(TrainingDojoMain.playedGame result) {
        Iterator<TrainingDojoMain.state> states = result.states.listIterator(result.states.size());
        Iterator<Direction> actions0 = result.actions0.listIterator(result.actions0.size());
        Iterator<Direction> actions1 = result.actions1.listIterator(result.actions1.size());
        if (states.hasNext() && actions0.hasNext() && actions1.hasNext()) {
            TrainingDojoMain.state cur_game = states.next();

            Integer[][] cur_state = toBoard(cur_game.snake0, cur_game.snake1, result.mazeSize, cur_game.apple);
            Integer[][] inv_cur_state = toBoard(cur_game.snake1, cur_game.snake0, result.mazeSize, cur_game.apple);
            Integer[][] next_state;
            Integer[][] inv_next_state;
            double reward0;
            Direction cur_action0 = actions0.next();
            Direction cur_action1 = actions1.next();
            while (states.hasNext() && actions0.hasNext() && actions1.hasNext()) {
                TrainingDojoMain.state next_game = states.next();
                next_state = toBoard(next_game.snake0, next_game.snake1, result.mazeSize, next_game.apple);
                inv_next_state = toBoard(next_game.snake1, next_game.snake0, result.mazeSize, next_game.apple);

                reward0 = calculateReward(cur_state, next_state);

                updateQ(cur_state, next_state, cur_action0, reward0);
                updateQ(inv_cur_state, inv_next_state, cur_action1, -reward0);

                //Load values for next iteration
                cur_state = next_state;
                inv_cur_state = inv_next_state;
                cur_action0 = actions0.next();
                cur_action1 = actions1.next();
            }
            if (result.winner == -1) {
                updateQ(cur_state, cur_action0, 10d);
                updateQ(inv_cur_state, cur_action1, -10d);
            } else if (result.winner == 1) {
                updateQ(cur_state, cur_action0, -10d);
                updateQ(inv_cur_state, cur_action1, 10d);
            } else {
                updateQ(cur_state, cur_action0, -1);
                updateQ(inv_cur_state, cur_action1, -1);
            }
        }
    }


    private void updateQ(Integer[][] cur_state, Integer[][] next_state, Direction cur_action, double reward) {
        if (QTable.containsKey(Arrays.deepToString(cur_state))) {
            System.out.print("UPDATING KNOW STATE\r");
        } else {
            System.out.print("New State\r");
        }
        double[] oldQs = this.QTable.getOrDefault(Arrays.deepToString(cur_state), new double[]{10, 10, 10, 10});
        double[] nextQs = this.QTable.getOrDefault(Arrays.deepToString(next_state), new double[]{10, 10, 10, 10});
        double oldQ = oldQs[directionToInt(cur_action)];
        double update = reward + this.gamma * Arrays.stream(nextQs).max().orElse(0d);
        double newQ = oldQ + this.alpha * (update - oldQ);
        oldQs[directionToInt(cur_action)] = newQ;
        this.QTable.put(Arrays.deepToString(cur_state), oldQs);
    }

    private void updateQ(Integer[][] cur_state, Direction cur_action, double reward) {
        double[] oldQs = this.QTable.getOrDefault(Arrays.deepToString(cur_state), new double[]{10, 10, 10, 10});
        int dir_int = directionToInt(cur_action);
        if (dir_int == -1) {
            printBoard(cur_state);
            System.out.println(cur_action);
            System.out.println(reward);
        }
        oldQs[dir_int] = oldQs[dir_int] + this.alpha * (reward - oldQs[dir_int]);
        this.QTable.replace(Arrays.deepToString(cur_state), oldQs);
    }

    /**
     * Calculates the reward in the eyes of bot 0, the reward of bot 1 should be inverted
     *
     * @param cur_state:  the current state, used to detect where the apple is
     * @param next_state: the state after current  state, used to detect if the apple was eaten
     * @return 1 if bot0 at the apple, -1 if bot1 ate the apple and 0 if neither
     */
    private double calculateReward(Integer[][] cur_state, Integer[][] next_state) {
        for (int i = 0; i < cur_state.length; i++) {
            for (int j = 0; j < cur_state[0].length; j++) {
                // Apple on loc i, j
                if (cur_state[i][j] == 3) {
                    if (next_state[i][j] == 2) {
                        return 5;
                    }
                    if (next_state[i][j] == 4) {
                        return -5;
                    }
                }
            }
        }
        return -0.1;
    }

    private int directionToInt(Direction dir) {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (DIRECTIONS[i].equals(dir)) {
                return i;
            }
        }
        //Should not happen => will generate error
        System.out.println("The direction that was taken:" + dir);
        System.out.println("This is weird, you should never see this, blame Dik");
        return -1;
    }

    private int argmax(double[] array) {
        int maxIndex = 0;
        if (array == null) {
            System.out.println("Breakpoint");
        }
        for (int i = 1; i < array.length; i++) {
            if (array[maxIndex] < array[i]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public Integer[][] toBoard(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        Integer[][] cc = new Integer[mazeSize.x][mazeSize.y];
        for (int x = 0; x < mazeSize.x; x++)
            for (int y = 0; y < mazeSize.y; y++)
                cc[x][y] = 0;

        // Coordinate of head of first snake on board
        Coordinate h0 = snake.getHead();
        cc[h0.x][h0.y] = 2;

        // Coordinate of head of second snake on board
        Coordinate h1 = opponent.getHead();
        cc[h1.x][h1.y] = 4;

        Iterator<Coordinate> it = snake.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = 1;
        }

        it = opponent.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = 3;
        }

        cc[apple.x][apple.y] = 5;

        return cc;
    }

    private void printBoard(Integer[][] board) {
        for (Integer[] boardRow : board) {
            System.out.println(Arrays.toString(boardRow));
        }
    }


    private BigInteger encode(int[][] board) {
        String str = Arrays.stream(board)
                .flatMapToInt(Arrays::stream)
                .boxed()
                .map(String::valueOf)
                .collect(Collectors.joining());
        return new BigInteger(str, 6);
    }

    public void setTrainingMode(boolean trainingMode) {
        training = trainingMode;
    }
}

