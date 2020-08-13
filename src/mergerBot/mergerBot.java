package mergerBot;

import imre.BFS41circles;
import minmax.alphabeta;
import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

public class mergerBot implements Bot {
    int maxAppleDistance = 8;
    Bot minimaxBot = new alphabeta();
    Bot bfs = new BFS41circles();

    /**
     * Chooses the alphaBeta tactic if the apple is close, otherwise will use BFS to go closer to apple
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segme
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return
     */
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        if(manhattanDistance(snake.getHead(), apple) < maxAppleDistance){
            return minimaxBot.chooseDirection(snake, opponent, mazeSize, apple);
        }else{
            return bfs.chooseDirection(snake, opponent, mazeSize, apple);
        }
    }

    private int manhattanDistance(Coordinate coord0, Coordinate coord1){
        return Math.abs(coord0.x - coord1.x) + Math.abs(coord0.y - coord1.y);
    }

}
