package snakes;

/**
 * This interface provides functions that should be implemented
 * to create smart snake bot for the game
 */
public interface Bot {
    /**
     * Smart snake bot (brain of your snake) should choose step (direction where to go)
     * on each game step until the end of game
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segme
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction in which snake should crawl next game step
     */
    public Direction chooseDirection(final Snake snake, final Snake opponent, final Coordinate mazeSize, final Coordinate apple);

    /*
    START ADDED BY SERPENTINE
    To enable the genetic algorithm we need to implement these methods
     */

    /**
     * get the DNA of this snake
     * For the genetic algorithm we need the parameters needed to replicate this snake
     *
     * @return
     */
    default double[] getDNA() {
        return new double[]{0};
    }

    /**
     * Makes a baby based on the given DNA
     *
     * @param parentDna
     * @return A new bot based on the DNA
     */
    default Bot giveBirth(double[][] parentDna) {
        return this;
    }

    /**
     *
     */
    default void save() {
    }

    default void update(TrainingDojoMain.playedGame[] results) {
    }

    default void setTrainingMode(boolean trainingMode){
    }
}

