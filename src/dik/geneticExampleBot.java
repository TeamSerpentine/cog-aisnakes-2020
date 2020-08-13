//package dik;
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
// * Sample implementation of snake bot
// */
//public class geneticExampleBot implements Bot {
//    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
//    private double[] DNA;
//    private Random rng = new Random();
//
//    public geneticExampleBot(double[] DNA) {
//        this.DNA = DNA;
//    }
//
//    public geneticExampleBot() {
//        this.DNA = this.randomDNA();
//    }
//
//    private double[] randomDNA() {
//        double[] DNA = new double[10];
//        for (int i = 0; i < 10; i++) {
//            DNA[i] = rng.nextDouble();
//        }
//        return DNA;
//    }
//
//    /**
//     * Choose the direction (not rational - silly). The output of this function can only be depended on the
//     * input of this function. This means that a previous state does not influence the output of this state.
//     * Given state A and B should always return a action independently of the order of the given states.
//     *
//     * @param snake    Your snake's body with coordinates for each segment
//     * @param opponent Opponent snake's body with coordinates for each segment
//     * @param mazeSize Size of the board
//     * @param apple    Coordinate of an apple
//     * @return Direction of bot's move
//     */
//    @Override
//    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
//        Coordinate head = snake.getHead();
//
//        /* Get the coordinate of the second element of the snake's body
//         * to prevent going backwards */
//        Coordinate afterHeadNotFinal = null;
//        if (snake.body.size() >= 2) {
//            Iterator<Coordinate> it = snake.body.iterator();
//            it.next();
//            afterHeadNotFinal = it.next();
//        }
//
//        final Coordinate afterHead = afterHeadNotFinal;
//
//        /* The only illegal move is going backwards. Here we are checking for not doing it */
//        Direction[] validMoves = Arrays.stream(DIRECTIONS)
//                .filter(d -> !head.moveTo(d).equals(afterHead)) // Filter out the backwards move
//                .sorted()
//                .toArray(Direction[]::new);
//
//        /* Just naïve greedy algorithm that tries not to die at each moment in time */
//        Direction[] notLosing = Arrays.stream(validMoves)
//                .filter(d -> head.moveTo(d).inBounds(mazeSize))             // Don't leave maze
//                .filter(d -> !opponent.elements.contains(head.moveTo(d)))   // Don't collide with opponent...
//                .filter(d -> !snake.elements.contains(head.moveTo(d)))      // and yourself
//                .sorted()
//                .toArray(Direction[]::new);
//        if (DNA[0] > rng.nextDouble()) {
//            if (notLosing.length > 0) return notLosing[0];
//            else return validMoves[0];
//        }
//        // Suicide, test to see if DNA[0] will get higher then 0.5 on all snakes
//        validMoves = Arrays.stream(DIRECTIONS)
//                .filter(d -> head.moveTo(d).equals(afterHead)) // Filter out the backwards move
//                .sorted()
//                .toArray(Direction[]::new);
//        return validMoves[0];
//    }
//
//    @Override
//    public Bot giveBirth(double[][] parentDna) {
//        double[][] DNA = parentDna[0];
//        DNA[0][0] += Math.min(0, Math.max(1, rng.nextDouble() - 0.5));
//        return new geneticExampleBot(DNA[0]);
//    }
//
//    @Override
//    public double[][] getDNA() {
//        return DNA;
//    }
//}