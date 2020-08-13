package wolf;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * Sample implementation of snake bot
 */
public class circle_the_apple_more implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    public int turn_wolf=0;
    public boolean circle_apple=false;
    public boolean circle_start=false;
    public boolean circle_possible=false;
    public int circle_pos=0;
    public Coordinate prev_apple;
    public Coordinate[] circle_coords = new Coordinate[10];
    /**
     * Choose the direction
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     *
     * @return Direction of bot's move
     */

    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        // set coordinates of the head of our snake
        Coordinate head = snake.getHead();
        Direction circle_direction = null;
        Direction chosenDirection = null; //The variable to be able to run more code before returning the direction

        // find the backwards direction (afterhead), which we're not allowed to go towards
        Coordinate afterHeadNotFinal = null;
        if (snake.body.size() >= 2) {
            Iterator<Coordinate> it = snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }
        final Coordinate afterHead = afterHeadNotFinal;

        // remove backwards direction from valid moves
        Direction[] validMoves = Arrays.stream(DIRECTIONS).filter(d -> !head.moveTo(d).equals(afterHead))
                .sorted().toArray(Direction[]::new);

        // remove directions which directly cause us to lose


        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))           // maze bounds
                .filter(d -> !opponent.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(opponent.body.getLast())) // opponent body
                .filter(d -> !snake.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(snake.body.getLast()))
                .sorted().toArray(Direction[]::new);




        // chooses direction to apple, if possible
        if (notLosing.length > 0 && chosenDirection==null) {
            Direction toApple = directionFromTo(head, apple);
            for (Direction d : notLosing) {
                if (toApple == d) {
                    chosenDirection = toApple;
                    break;
                }
            }
            
            // if toApple is losing, do something else randomly
            if(chosenDirection==null) {
                Random random = new Random();
                chosenDirection = notLosing[random.nextInt(notLosing.length)];
            }
        } else {
            chosenDirection = validMoves[0];
        }

        //check if apple is in a corner if body is at least 4 and longer than opponent
        if(snake.body.size()>3 && snake.body.size()>opponent.body.size()){
            if(apple.x==0&&apple.y<2){ //upper left #1
                circle_coords[0] = new Coordinate(1,1);
            }
        }

        if(snake.body.size()>9 && snake.body.size()>opponent.body.size() && snake.body.size()%2==0 && prev_apple==apple){
            circle_apple=true;
        }else{
            circle_apple=false;
        }

        circle_possible= apple.x != 0 && apple.y != 0 && apple.x != mazeSize.x-1 && apple.y != mazeSize.y-1;
        if(circle_apple && circle_possible) {
            if (head.moveTo(chosenDirection).equals(apple) && !circle_start) {
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
                chosenDirection = notLosing[0];
                for (Direction d : notLosing) {
                    if (circle_direction == d) {
                        chosenDirection = circle_direction;
                        break;
                    }
                }
            }
        }
        prev_apple=apple;
        for(Direction d : validMoves){
            if(chosenDirection==d){
                return chosenDirection;
            }
        }
       return validMoves[0];
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


}
