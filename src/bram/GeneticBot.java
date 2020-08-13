//package bram;
//
//import snakes.Bot;
//import snakes.Coordinate;
//import snakes.Direction;
//import snakes.Snake;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.Random;
//
///**
// * This bot uses a genetic algorithm.
// * The DNA of a snake is represented as:
// *
// * a fixed value for each tile of the board.
// */
//public class GeneticBot implements Bot {
//    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
//    public int turn = 0;
//
//    // 2D arrays that will be the size of the board
//    double[][] snakesDNA;
//    double[][] observation;
//    double[][] action;
//
//    // variables to find the best move
//    double[] valuesOfOptions = new double[4];
//    int indexMaxOption = 0;
//    double maxOption = Double.NEGATIVE_INFINITY;
//
//    // observation constants
//    double APPLE_VALUE = 10.0;
//    double MY_SNAKE_VALUE = -5.0;
//    double OTHER_SNAKE_VALUE = -5.0;
//    double EMPTY_TILE_VALUE = 0.0;
//
//    // useful to have in the class (for other functions than chooseDirection)
//    double maxDistanceSquared;
//
//
//    /**
//     * Choose the direction, with a genetic algorithm
//     * @param snake    Your snake's body with coordinates for each segment
//     * @param opponent Opponent snake's body with coordinates for each segment
//     * @param mazeSize Size of the board
//     * @param apple    Coordinate of an apple
//     * @return Direction of bot's move
//     */
//    @Override
//    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
//        if (turn == 0) {
//            // initialize 2D arrays
//            snakesDNA = new double[mazeSize.x][mazeSize.y];
//            observation = new double[mazeSize.x][mazeSize.y];
//            action = new double[mazeSize.x][mazeSize.y];
//            maxDistanceSquared = squaredDistance(new Coordinate(0,0), mazeSize);
//            // give random DNA in the first turn
//            for (int i = 0; i < mazeSize.x; i++) {
//                for (int j = 0; j < mazeSize.y; j++) {
//                    // Math.random gives a nr between 0 and 1, so this gives between -1 and 1
//                    snakesDNA[i][j] = 2*Math.random() - 1;
//                    // TODO: check if randomness is good. Now snake is moving the same within each episode of a run..?
//                }
//            }
//        }
//        turn++;
//
//        // observation array/list update
//        for (int i = 0; i < mazeSize.x; i++) {
//            for (int j = 0; j < mazeSize.y; j++) {
//                if (snake.elements.contains(new Coordinate(i,j))) {
//                    observation[i][j] = MY_SNAKE_VALUE;
//                } else if (opponent.elements.contains(new Coordinate(i,j))) {
//                    observation[i][j] = OTHER_SNAKE_VALUE;
//                } else if (apple.equals(new Coordinate(i, j))) {
//                    observation[i][j] = APPLE_VALUE;
//                } else {
//                    observation[i][j] = closenessToApple(new Coordinate(i,j), apple, 0.7);
//                }
//                // TODO: maybe subtract value to tiles close to corners/walls/snakes?
//                // TODO: maybe add more value to tiles in large open space? (to not enclose yourself)
//                // maybe the AI should be learning/tweaking this function (closenessToApple) as DNA,
//                // instead of a fixed board with numbers
//            }
//        }
//
//        // combine snakesDNA and the observation
//        for (int i = 0; i < mazeSize.x; i++) {
//            for (int j = 0; j < mazeSize.y; j++) {
//                action[i][j] = snakesDNA[i][j] + observation[i][j];
//            }
//        }
//
//        // take a look at the arrays if you want (if not set to -1)
//        if (turn == -1) {
//            printArray(snakesDNA);
//            printArray(observation);
//            printArray(action);
//        }
//
//        // get the value of the tiles we can go to
//        Coordinate head = snake.getHead();
//        for (int i=0; i<4; i++) {
//            // check if inbounds
//            if (head.moveTo(DIRECTIONS[i]).inBounds(mazeSize)) {
//                valuesOfOptions[i] = action[head.moveTo(DIRECTIONS[i]).x][head.moveTo(DIRECTIONS[i]).y];
//            } else {
//                valuesOfOptions[i] = Double.NEGATIVE_INFINITY;
//            }
//        }
//        // choose direction with largest value
//        for (int i=0; i<4; i++) {
//            if (valuesOfOptions[i] > maxOption) {
//                indexMaxOption = i;
//                maxOption = valuesOfOptions[i];
//            }
//        }
//        // reset maxOption for next turn
//        maxOption = Double.NEGATIVE_INFINITY;
//
//        return DIRECTIONS[indexMaxOption];
//    }
//
//    /**
//     * gives the DNA of this genetic-bot snake
//     * @return snakesDNA
//     */
//    public double[][] getDNA() {
//        return snakesDNA;
//    }
//
//    /**
//     * gives birth to a new snake with DNA based on two parents
//     * @param snake1DNA DNA of snake1
//     * @param snake2DNA DNA of snake2
//     * @return average of DNA of the parents
//     */
//    public double[][] giveBirth(double[][] snake1DNA, double[][] snake2DNA) {
//        double[][] babyDNA = new double[snake1DNA.length][snake1DNA[0].length];
//        for (int i=0; i<snake1DNA.length; i++) {
//            for (int j=0; j<snake1DNA[0].length; j++) {
//                // taking average
//                babyDNA[i][j] = (snake1DNA[i][j] + snake2DNA[i][j]) / 2.0;
//            }
//        }
//        return babyDNA;
//    }
//
//    /**
//     * gives higher value to tiles that are close to the apple
//     * tiles next to apple get at most value: nextToApple*APPLE_VALUE
//     * for example: if nexToApple is 0.7, these tiles get at most 70% of APPLE_VALUE
//     * @param point a tile
//     * @param apple the coordinates of apple
//     * @param nextToApple percentage of APPLE_VALUE that tiles right next to the apple get
//     * @return a value between 0 and APPLE_VALUE, based on distance
//     */
//    public double closenessToApple(Coordinate point, Coordinate apple, double nextToApple) {
//        return (1 - (squaredDistance(point, apple) / maxDistanceSquared))*APPLE_VALUE*nextToApple;
//    }
//
//    /**
//     * computes squared distance between two points (taking square roots is a waste of time)
//     * @param pointA a coordinate
//     * @param pointB other coordinate
//     * @return the squared distance
//     */
//    public double squaredDistance(Coordinate pointA, Coordinate pointB) {
//        return Math.pow((pointA.x - pointB.x),2) + Math.pow((pointA.y - pointB.y),2);
//    }
//
//    /**
//     * can print the values of the arrays that are as big as the playing board
//     * rounds the values to 2 decimal places
//     * @param theArray an array such as snakesDNA, observation, or action
//     */
//    public void printArray(double[][] theArray) {
//        for (int i=0; i<theArray[0].length; i++) {
//            for (int j=0; j<theArray.length; j++) {
//                // round the values to 2 decimals
//                System.out.print(Math.round(theArray[j][i]*100.0) / 100.0);
//                System.out.print(" ");
//            }
//            System.out.println();
//        }
//        System.out.println();
//    }
//
//
//    /**
//     * added by Serpentine
//     * Set direction from one point to other point
//     * @param start point to begin
//     * @param other point to move
//     * @return direction
//     */
//    public Direction directionFromTo(Coordinate start, Coordinate other) {
//        final Coordinate vector = new Coordinate(other.x - start.x, other.y - start.y);
//        if (vector.x > 0) {
//            return Direction.RIGHT;
//        } else if (vector.x < 0) {
//            return Direction.LEFT;
//        }
//        if (vector.y > 0) {
//            return Direction.UP;
//        } else if (vector.y < 0) {
//            return Direction.DOWN;
//        }
//        for (Direction direction : Direction.values())
//            if (direction.dx == vector.x && direction.dy == vector.y)
//                return direction;
//        return null;
//    }
//
//    /**
//     * added by Serpentine (only last part of this function)
//     * this chooses the direction to the apple, while not losing, like the SearchBot
//     * @param snake    Your snake's body with coordinates for each segment
//     * @param opponent Opponent snake's body with coordinates for each segment
//     * @param mazeSize Size of the board
//     * @param apple    Coordinate of an apple
//     * @return Direction of bot's move
//     */
//    public Direction notLosingAndToApple(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
//        // set coordinates of the head of our snake
//        Coordinate head = snake.getHead();
//
//        // find the backwards direction (afterhead), which we're not allowed to go towards
//        Coordinate afterHeadNotFinal = null;
//        if (snake.body.size() >= 2) {
//            Iterator<Coordinate> it = snake.body.iterator();
//            it.next();
//            afterHeadNotFinal = it.next();
//        }
//        final Coordinate afterHead = afterHeadNotFinal;
//
//        // remove backwards direction from valid moves
//        Direction[] validMoves = Arrays.stream(DIRECTIONS).filter(d -> !head.moveTo(d).equals(afterHead))
//                .sorted().toArray(Direction[]::new);
//
//        // remove directions which directly cause us to lose
//        Direction[] notLosing = Arrays.stream(validMoves)
//                .filter(d -> head.moveTo(d).inBounds(mazeSize))           // maze bounds
//                .filter(d -> !opponent.elements.contains(head.moveTo(d))) // opponent body
//                .filter(d -> !snake.elements.contains(head.moveTo(d)))    // and yourself
//                .sorted().toArray(Direction[]::new);
//
//        // chooses direction to apple, if possible
//        if (notLosing.length > 0) {
//            Direction toApple = directionFromTo(head, apple);
//            for (Direction d : notLosing) {
//                if (toApple == d) {
//                    return toApple;
//                }
//            }
//            // if toApple is losing, do something else randomly
//            Random random = new Random();
//            return notLosing[random.nextInt(notLosing.length)];
//        } else return validMoves[0];
//    }
//
//}
