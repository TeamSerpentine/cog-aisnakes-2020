//package bram;
//
//import snakes.Bot;
//import snakes.Coordinate;
//import snakes.Direction;
//import snakes.Snake;
//
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.Random;
//
///**
// * This bot uses a genetic algorithm.
// * The DNA of a snake is represented as:
// *
// * a value for each possible distance to apple and the snakes (divided into buckets)
// *
// */
//public class GeneticBot2 implements Bot {
//    public GeneticBot2(double[][] DNA) {
//        this.distanceDNA = DNA;
//    }
//    public GeneticBot2() {
//        this.giveRandomDNA();
//    }
//
//    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
//    public int turn = 0;
//
//    // 2D array of possible distances (first to apple, second to opponent)
//    int nrBuckets = 100;
//    double[][] distanceDNA = new double[nrBuckets][nrBuckets];
//
//    // useful variables, only declared once here (instead of many times in the functions)
//    double maxDistanceSquared;
//    double distanceToApple;
//    double distanceToOpponent;
//    double distanceToBodypart;
//    int indexApple;
//    int indexOpponent;
//
//    // 2D arrays size of the board
//    double[][] action;
//
//    // variables to find the best move
//    double[] valuesOfOptions = new double[4];
//    int indexMaxOption = 0;
//    double maxOption = Double.NEGATIVE_INFINITY;
//
//    // observation constants
//    double APPLE_VALUE = 10.0;
//    double MY_SNAKE_VALUE = -100.0;
//    double OTHER_SNAKE_VALUE = -100.0;
//
//
//    /**
//     * Choose the direction, with a genetic algorithm based on distanceDNA
//     * @param snake    Your snake's body with coordinates for each segment
//     * @param opponent Opponent snake's body with coordinates for each segment
//     * @param mazeSize Size of the board
//     * @param apple    Coordinate of an apple
//     * @return Direction of bot's move
//     */
//    @Override
//    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
//        if (turn == 0) {
//            giveRandomDNA();
//            //giveHumanDNA(0.7);
//            action = new double[mazeSize.x][mazeSize.y];
//            maxDistanceSquared = squaredDistance(new Coordinate(0,0), mazeSize) + 0.1;
//            // add a little bit (0.1) such that floor(dist/maxDist) will never be 1
//            // that would be a problem in getDistanceDnaValue()
//        }
//        turn++;
//
//        updateActionArray(snake, opponent, mazeSize, apple);
//
//        // take a look at the arrays if you want (if not set to -1)
//        if (turn == 10000000) {
//            printArray(distanceDNA); // this is a big one (nrBuckets x nrBuckets)
//            printArray(action); // this has the size of the board
//        }
//
//        indexMaxOption = chooseBestTile(snake, mazeSize);
//        return DIRECTIONS[indexMaxOption];
//    }
//
//
//
//    /**
//     * gives the DNA of this genetic-bot snake
//     * @return distanceDNA
//     */
//    @Override // is this necessary?
//    public double[][] getDNA() {
//        return distanceDNA;
//    }
//
//    /**
//     * gives birth to a new snake with DNA based on two parents
//     * @param snake1DNA DNA of snake1
//     * @param snake2DNA DNA of snake2
//     * @return average of DNA of the parents
//     */
//    public double[][] giveAverageBirth(double[][] snake1DNA, double[][] snake2DNA) {
//        double[][] babyDNA = new double[snake1DNA.length][snake1DNA[0].length];
//        for (int i=0; i<snake1DNA.length; i++) {
//            for (int j=0; j<snake1DNA[0].length; j++) {
//                // taking average
//                // TODO: add randomness here?
//                babyDNA[i][j] = (snake1DNA[i][j] + snake2DNA[i][j]) / 2.0;
//            }
//        }
//        return babyDNA;
//    }
//
//    /**
//     * gives birth to a new snake with DNA based on two parents
//     * for each entry of the DNA it randomly decides whether it will come from parent1 or parent2
//     * @param parentDNA DNA of snake1
//     * @return new baby snake
//     */
//    @Override // is this necessary?
//    public Bot giveBirth(double[][] parentDNA) {
//        double[][] babyDNA = new double[distanceDNA.length][distanceDNA[0].length];
//        Random r = new Random();
//        // the coinflip is now in the range (0.25 , 0.75)
//        // we don't want the babyDNA to come too much from one parent alone, right?
//        double coinflip = r.nextDouble() / 2.0 + 0.25;
//        for (int i=0; i<distanceDNA.length; i++) {
//            for (int j=0; j<distanceDNA[0].length; j++) {
//                // choosing whether to take this piece of DNA from parent1 or parent2
//                if (r.nextDouble() < coinflip) {
//                    babyDNA[i][j] = distanceDNA[i][j] + (r.nextDouble() - 0.5) / 10.0;
//                    // adding a bit of randomness (between -0.05 and +0.05)
//                } else {
//                    babyDNA[i][j] = parentDNA[i][j] + (r.nextDouble() - 0.5) / 10.0;
//                }
//            }
//        }
//        return new GeneticBot2(babyDNA);
//    }
//
//
//
//    // TODO: not necessary to update all tiles of the board! Just the 4 move options is fine...
//    /**
//     * updates the action array based on the current observation
//     * @param snake our snake
//     * @param opponent other snake
//     * @param mazeSize the board size
//     * @param apple the apple
//     */
//    public void updateActionArray(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
//        for (int i = 0; i < mazeSize.x; i++) {
//            for (int j = 0; j < mazeSize.y; j++) {
//                if (snake.elements.contains(new Coordinate(i,j))) {
//                    action[i][j] = MY_SNAKE_VALUE;
//                } else if (opponent.elements.contains(new Coordinate(i,j))) {
//                    action[i][j] = OTHER_SNAKE_VALUE;
//                } else if (apple.equals(new Coordinate(i, j))) {
//                    action[i][j] = APPLE_VALUE;
//                } else {
//                    // empty tiles get a value determined by the DNA
//                    action[i][j] = getDistanceDnaValue(new Coordinate(i,j), apple, opponent);
//                }
//                // TODO: maybe subtract value to tiles close to corners/walls?
//                // TODO: maybe add more value to tiles in large open space? (to not enclose yourself)
//            }
//        }
//    }
//
//    /**
//     * get the DNA value of a tile, based on its distance to the apple and the opponent
//     * @param tile the tile your looking at
//     * @param apple the apple
//     * @param opponent the other snake
//     * @return the DNA value
//     */
//    public double getDistanceDnaValue(Coordinate tile, Coordinate apple, Snake opponent) {
//        distanceToApple = squaredDistance(tile, apple);
//        distanceToOpponent = findMinDistanceToOpponent(tile, opponent);
//
//        indexApple = (int) Math.floor((distanceToApple/maxDistanceSquared) * nrBuckets);
//        indexOpponent = (int) Math.floor((distanceToOpponent/maxDistanceSquared) * nrBuckets);
//
//        return distanceDNA[indexApple][indexOpponent];
//    }
//
//    /**
//     * finds minimum distance from a tile to the opponent snake
//     * @param tile the coordinates of a tile on the board
//     * @param opponent the other snake
//     * @return the minimum distance
//     */
//    public double findMinDistanceToOpponent(Coordinate tile, Snake opponent) {
//        distanceToBodypart = Double.POSITIVE_INFINITY;
//        for (Coordinate bodypart : opponent.elements) {
//            if (squaredDistance(tile, bodypart) < distanceToBodypart) {
//                distanceToBodypart = squaredDistance(tile, bodypart);
//            }
//        }
//        return distanceToBodypart;
//    }
//
//
//
//    /**
//     * gives random DNA (to apply in first turn)
//     */
//    public void giveRandomDNA() {
//        for (int i = 0; i < nrBuckets; i++) {
//            for (int j = 0; j < nrBuckets; j++) {
//                // Math.random gives a nr between 0 and 1, so this gives between -1 and 1
//                distanceDNA[i][j] = 2*Math.random() - 1;
//                // TODO: check if randomness is good. Now snake is moving the same within each episode of a run..?
//            }
//        }
//    }
//
//    /**
//     * sets the distanceDNA to something that we (humans) think is smart:
//     * higher values to tiles close to apple
//     * does nothing with distance to opponent (yet)
//     *
//     * gives higher value to tiles that are close to the apple
//     * tiles next to apple get at most value: nextToApple*APPLE_VALUE
//     * for example: if nexToApple is 0.7, these tiles get at most 70% of APPLE_VALUE
//     * @param nextToApple percentage of APPLE_VALUE that tiles right next to the apple get
//     */
//    public void giveHumanDNA(double nextToApple) {
//        double nrBucketsD = nrBuckets;
//        double fraction;
//        for (int i = 0; i < nrBuckets; i++) {
//            fraction = i/nrBucketsD;
//            for (int j = 0; j < nrBuckets; j++) {
//                distanceDNA[i][j] = (1 - fraction)*APPLE_VALUE*nextToApple;
//            }
//        }
//    }
//
//    /**
//     * sets indexMaxOption to the direction of the tile with the highest action[][] value
//     * @param snake our snake
//     * @param mazeSize the board
//     * @return index of best direction to go to
//     */
//    public int chooseBestTile(Snake snake, Coordinate mazeSize) {
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
//        return indexMaxOption;
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
