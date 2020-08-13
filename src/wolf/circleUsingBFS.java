package wolf;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;

/**
 * Sample implementation of snake bot
 */
public class circleUsingBFS implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};


    // to circle the apple
    public Coordinate[] circle_positions = new Coordinate[6];
    public int circle_positions_length=0;
    public int circle_positions_iterator=0;
    public Coordinate prev_apple=null;
    public int circle_type=0;

    // for BFS
    private Map<Coordinate, Coordinate> snake_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> snake_depth = new HashMap<Coordinate, Integer>();
    private Map <Coordinate, Coordinate> opponent_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> opponent_depth = new HashMap<Coordinate, Integer>();
    private int oldReachable;
    private int opponent_oldReachable;
    private boolean checkCanEnclose;

    public Coordinate head;



    /**
     * Choose the direction
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        head = snake.getHead();       // set coordinates of the head of our snake
        Direction chosenDirection = null; //The variable to be able to run more code before returning the direction

        Direction[] validMoves = removeBackwardsDirection(snake, opponent, mazeSize);
        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))           // maze bounds
                .filter(d -> !opponent.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(opponent.body.getLast())) // opponent body without ass
                .filter(d -> !snake.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(snake.body.getLast())) //own body without ass
                .sorted().toArray(Direction[]::new);

        //makeOpponentBFSGraph(snake, opponent, mazeSize, apple); // We do this before making our BFS because we want to calculate the distance to the apple
        makeBFSGraph(snake, opponent, mazeSize, apple);


        if (prev_apple!=apple) {  // the apple has moved!
            // so, we need to determine the new circle_type
            circle_type = circle_type(apple, snake, opponent, mazeSize);
            if (circle_type>0) {
                setCircle_positions(apple,circle_type,mazeSize);
            }

            System.out.println("Apple has changed position, circle_type is now: " + circle_type);
        }
        prev_apple = apple;

        // circle_type = 0 means we're not circling the apple
        if (circle_type>0) {
            // here we are circling
            notLosing = Arrays.stream(notLosing)
                .filter(d -> !(head.moveTo(d).equals(apple))) // when circling, don't eat the apple
                .sorted().toArray(Direction[]::new);

            chosenDirection = circle_direction(apple,circle_type,snake,head);

        } else {
            // here we are not circling
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
                Random random = new Random();
                chosenDirection = validMoves[random.nextInt(validMoves.length)];
                return chosenDirection;
            }
        }

        if (notLosing.length>0) {
            for (Direction d : notLosing) {
                if (chosenDirection == d) {
                    return chosenDirection;
                }
            }
            Random random = new Random();
            chosenDirection = notLosing[random.nextInt(notLosing.length)];

        }

        for (Direction d : validMoves) {
            if (chosenDirection == d) {
                return chosenDirection;
            }
        }

        Random random = new Random();
        chosenDirection = validMoves[random.nextInt(validMoves.length)];
        return chosenDirection;
    }

    /**
     * this removes the backwards direction like it was done from the beginning
     * It assigns the original validMoves and notLosing arrays of directions
     */
    public Direction[] removeBackwardsDirection(Snake snake, Snake opponent, Coordinate mazeSize) {
        // find the backwards direction (afterhead), which we're not allowed to go towards
        Coordinate afterHeadNotFinal = null;
        if (snake.body.size() >= 2) {
            Iterator<Coordinate> it = snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }
        final Coordinate afterHead = afterHeadNotFinal;

        // remove backwards direction from valid moves
        return Arrays.stream(DIRECTIONS).filter(d -> !head.moveTo(d).equals(afterHead))
                .sorted().toArray(Direction[]::new);
    }

    /**
     * Set direction from one point to other point, very simplistic
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

    public int circle_type(Coordinate apple,Snake snake,Snake opponent,Coordinate mazeSize){
        int body=snake.body.size();
        if(body%2==0 && body>opponent.body.size() && body>5){
            boolean in_corner=false;
            boolean closed_circle=true;
            if(body>(opponent.body.size()+1)){
                closed_circle=false;
            }
            if((apple.x<3 && apple.y==0)||(apple.y<3 && apple.x==0)||(apple.x>(mazeSize.x-4) && apple.y==0)||(apple.y<3 && apple.x==(mazeSize.x-1))||(apple.x>(mazeSize.x-4) && apple.y==(mazeSize.y-1))||(apple.x==(mazeSize.x-1) && apple.y>(mazeSize.y-4))||(apple.x<3 && apple.y==(mazeSize.y-1))||(apple.x==0 && apple.y>(mazeSize.y-4))){
                    in_corner = true;
            }
            //for (int i = 0; i < 12; i++) { //check if in a corner
            //    if (apple.equals(corners[i])) {
            //        in_corner = true;
            //        break;
            //    }
            //}
            if(in_corner){
                if(apple.x==0 && apple.y<3){ //upper left 1
                    if(!closed_circle){
                        return 2;
                    }else if(body>7){
                        if(apple.y==0){
                            return 10;
                        }
                        if(body>9){
                            if(apple.y==1){
                                return 14;
                            }
                            if(body>11){
                                return 22;
                            }
                        }
                    }
                }else if(apple.x<3 && apple.y==0){ //upper left 2
                    if(!closed_circle){
                        return 6;
                    }else if(body>9){
                            if(apple.x==1){
                                return 18;
                            }
                            if(body>11){
                                return 26;
                            }
                        }
                }else if(apple.x==(mazeSize.x-1)&&apple.y<3){ //upper right 1
                    if(!closed_circle){
                        return 3;
                    }else if(body>7){
                        if(apple.y==0){
                            return 11;
                        }
                        if(body>9){
                            if(apple.y==1){
                                return 15;
                            }
                            if(body>11){
                                return 23;
                            }
                        }
                    }
                }else if(apple.x>(mazeSize.x-4) && apple.y==0) { //upper right 2
                    if (!closed_circle) {
                        return 7;
                    } else if (body > 9) {
                        if (apple.x==mazeSize.x-2) {
                            return 19;
                        }
                        if (body > 11) {
                            return 27;
                        }
                    }
                }else if(apple.x==(mazeSize.x-1)&&apple.y>(mazeSize.y-4)){ //bottom right 1
                    if(!closed_circle){
                        return 4;
                    }else if(body>7){
                        if(apple.y==(mazeSize.y-1)){
                            return 12;
                        }
                        if(body>9){
                            if(apple.y==(mazeSize.y-2)){
                                return 16;
                            }
                            if(body>11){
                                return 24;
                            }
                        }
                    }
                }else if(apple.x>(mazeSize.x-4) && apple.y==(mazeSize.y-1)) { //bottom right 2
                    if (!closed_circle) {
                        return 8;
                    } else if (body > 9) {
                        if (apple.x==mazeSize.x-2) {
                            return 20;
                        }
                        if (body > 11) {
                            return 28;
                        }
                    }
                }else if(apple.x==0 && apple.y>(mazeSize.y-4)){ //bottom left 1
                    if(!closed_circle){
                        return 5;
                    }else if(body>7){
                        if(apple.y==(mazeSize.y-1)){
                            return 13;
                        }
                        if(body>9){
                            if(apple.y==(mazeSize.y-2)){
                                return 17;
                            }
                            if(body>11){
                                return 25;
                            }
                        }
                    }
                }else if(apple.x<3 && apple.y==(mazeSize.y-1)) { //bottom left 2
                    if (!closed_circle) {
                        return 9;
                    } else if (body > 9) {
                        if (apple.x==1) {
                            return 21;
                        }
                        if (body > 11) {
                            return 29;
                        }
                    }
                }
                }else {
                if ((body > 7 && !closed_circle) || (body > 13)) { //sides
                    if (apple.x == 0) {
                        if (closed_circle) {
                            return 41;
                        } else {
                            return 35; // OR 36!
                        }
                    }
                    if (apple.x == (mazeSize.x - 1)) {
                        if (closed_circle) {
                            return 40;
                        } else {
                            return 34; // OR 37!
                        }
                    }
                    if (apple.y == (mazeSize.y - 1)) {
                        if (closed_circle) {
                            return 38;
                        } else {
                            return 30; // OR 31!
                        }
                    }
                    if (apple.y == 0) {
                        if (closed_circle) {
                            return 39;
                        } else {
                            return 32; // OR 33!
                        }
                    }
                }
                if(body>7){
                    return 1;  // 1 means: apple is somewhere not on the edge, and we're gonna circle it
                }
            }

            }
        return 0;  // 0 means: we're not going to circle the apple
        }

    public void setCircle_positions(Coordinate apple,int circle_type,Coordinate mazeSize){
        circle_positions_iterator=0;
            switch(circle_type){
                case 1:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate((apple.x-1),(apple.y-1));
                    circle_positions[1]=new Coordinate((apple.x+1),(apple.y+1));
                    break;
                case 2:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate(1,2);
                    circle_positions[1]=new Coordinate(2,0);
                    break;
                case 3:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate((mazeSize.x-2),2);
                    circle_positions[1]=new Coordinate((mazeSize.x-3),0);
                    break;
                case 4:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate((mazeSize.x-2),(mazeSize.y-3));
                    circle_positions[1]=new Coordinate((mazeSize.x-3),(mazeSize.y-1));
                    break;
                case 5:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate(1,(mazeSize.y-3));
                    circle_positions[1]=new Coordinate(2,(mazeSize.y-1));
                    break;
                case 6:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate(2,1);
                    circle_positions[1]=new Coordinate(0,2);
                    break;
                case 7:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate((mazeSize.x-3),1);
                    circle_positions[1]=new Coordinate((mazeSize.x-1),2);
                    break;
                case 8:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate((mazeSize.x-3),(mazeSize.y-2));
                    circle_positions[1]=new Coordinate((mazeSize.x-1),(mazeSize.y-3));
                    break;
                case 9:
                    circle_positions_length=2;
                    circle_positions[0]=new Coordinate(2,(mazeSize.y-2));
                    circle_positions[1]=new Coordinate(0,(mazeSize.y-3));
                    break;
                case 10:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(0,1);
                    circle_positions[1]=new Coordinate(1,0);
                    circle_positions[2]=new Coordinate(2,2);
                    break;
                case 11:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-2),0);
                    circle_positions[1]=new Coordinate((mazeSize.x-1),1);
                    circle_positions[2]=new Coordinate((mazeSize.x-3),2);
                    break;
                case 12:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-2),(mazeSize.y-1));
                    circle_positions[1]=new Coordinate((mazeSize.x-1),(mazeSize.y-2));
                    circle_positions[2]=new Coordinate((mazeSize.x-3),(mazeSize.y-3));
                    break;
                case 13:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(1,(mazeSize.y-1));
                    circle_positions[1]=new Coordinate(0,(mazeSize.y-2));
                    circle_positions[2]=new Coordinate(2,(mazeSize.y-3));
                    break;
                case 14:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(0,2);
                    circle_positions[1]=new Coordinate(1,0);
                    circle_positions[2]=new Coordinate(2,3);
                    break;
                case 15:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-1),2);
                    circle_positions[1]=new Coordinate((mazeSize.x-2),0);
                    circle_positions[2]=new Coordinate((mazeSize.x-3),3);
                    break;
                case 16:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-1),(mazeSize.y-3));
                    circle_positions[1]=new Coordinate((mazeSize.x-2),(mazeSize.y-1));
                    circle_positions[2]=new Coordinate((mazeSize.x-3),(mazeSize.y-4));
                    break;
                case 17:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(0,(mazeSize.y-3));
                    circle_positions[1]=new Coordinate(1,(mazeSize.y-1));
                    circle_positions[2]=new Coordinate(2,(mazeSize.y-4));
                    break;
                case 18:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(2,0);
                    circle_positions[1]=new Coordinate(0,1);
                    circle_positions[2]=new Coordinate(3,2);
                    break;
                case 19:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-3),0);
                    circle_positions[1]=new Coordinate((mazeSize.x-1),1);
                    circle_positions[2]=new Coordinate((mazeSize.x-4),2);
                    break;
                case 20:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-3),(mazeSize.y-1));
                    circle_positions[1]=new Coordinate((mazeSize.x-1),(mazeSize.y-2));
                    circle_positions[2]=new Coordinate((mazeSize.x-4),(mazeSize.y-3));
                    break;
                case 21:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(2,(mazeSize.y-1));
                    circle_positions[1]=new Coordinate(0,(mazeSize.y-2));
                    circle_positions[2]=new Coordinate(3,(mazeSize.y-3));
                    break;
                case 22:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(0,3);
                    circle_positions[1]=new Coordinate(1,0);
                    circle_positions[2]=new Coordinate(2,4);
                    break;
                case 23:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-1),3);
                    circle_positions[1]=new Coordinate((mazeSize.x-2),0);
                    circle_positions[2]=new Coordinate((mazeSize.x-3),4);
                    break;
                case 24:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-1),(mazeSize.y-4));
                    circle_positions[1]=new Coordinate((mazeSize.x-2),(mazeSize.y-1));
                    circle_positions[2]=new Coordinate((mazeSize.x-3),(mazeSize.y-5));
                    break;
                case 25:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(0,(mazeSize.y-4));
                    circle_positions[1]=new Coordinate(1,(mazeSize.y-1));
                    circle_positions[2]=new Coordinate(2,(mazeSize.y-5));
                    break;
                case 26:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(3,0);
                    circle_positions[1]=new Coordinate(0,1);
                    circle_positions[2]=new Coordinate(4,2);
                    break;
                case 27:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-4),0);
                    circle_positions[1]=new Coordinate((mazeSize.x-1),1);
                    circle_positions[2]=new Coordinate((mazeSize.x-5),2);
                    break;
                case 28:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-4),(mazeSize.y-1));
                    circle_positions[1]=new Coordinate((mazeSize.x-1),(mazeSize.y-2));
                    circle_positions[2]=new Coordinate((mazeSize.x-5),(mazeSize.y-3));
                    break;
                case 29:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(3,(mazeSize.y-1));
                    circle_positions[1]=new Coordinate(0,(mazeSize.y-2));
                    circle_positions[2]=new Coordinate(4,(mazeSize.y-3));
                    break;
                case 30:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((apple.x-1),(mazeSize.y-1));
                    circle_positions[1]=new Coordinate(apple.x,(mazeSize.y-2));
                    circle_positions[2]=new Coordinate((apple.x-2),(mazeSize.y-3));
                    break;
                case 31:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((apple.x+1),(mazeSize.y-1));
                    circle_positions[1]=new Coordinate(apple.x,(mazeSize.y-2));
                    circle_positions[2]=new Coordinate((apple.x+2),(mazeSize.y-3));
                    break;
                case 32:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((apple.x+1),0);
                    circle_positions[1]=new Coordinate(apple.x,1);
                    circle_positions[2]=new Coordinate((apple.x+2),2);
                    break;
                case 33:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((apple.x-1),0);
                    circle_positions[1]=new Coordinate(apple.x,1);
                    circle_positions[2]=new Coordinate((apple.x-2),2);
                    break;
                case 34:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-1),(apple.y-1));
                    circle_positions[1]=new Coordinate((mazeSize.x-2),apple.y);
                    circle_positions[2]=new Coordinate((mazeSize.x-3),(apple.y-2));
                    break;
                case 35:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(0,(apple.y-1));
                    circle_positions[1]=new Coordinate(1,apple.y);
                    circle_positions[2]=new Coordinate(2,(apple.y-2));
                    break;
                case 36:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate(0,(apple.y+1));
                    circle_positions[1]=new Coordinate(1,apple.y);
                    circle_positions[2]=new Coordinate(2,(apple.y+2));
                    break;
                case 37:
                    circle_positions_length=3;
                    circle_positions[0]=new Coordinate((mazeSize.x-1),(apple.y+1));
                    circle_positions[1]=new Coordinate((mazeSize.x-2),apple.y);
                    circle_positions[2]=new Coordinate((mazeSize.x-3),(apple.y+2));
                    break;
                case 38:
                    circle_positions_length=5;
                    circle_positions[0]=new Coordinate((apple.x+1),(mazeSize.y-1));
                    circle_positions[1]=new Coordinate((apple.x-1),(mazeSize.y-2));
                    circle_positions[2]=new Coordinate((apple.x-1),(mazeSize.y-1));
                    circle_positions[3]=new Coordinate((apple.x-2),(mazeSize.y-3));
                    circle_positions[4]=new Coordinate((apple.x+2),(mazeSize.y-3));
                    break;
                case 39:
                    circle_positions_length=5;
                    circle_positions[0]=new Coordinate((apple.x+1),0);
                    circle_positions[1]=new Coordinate((apple.x-1),1);
                    circle_positions[2]=new Coordinate((apple.x-1),0);
                    circle_positions[3]=new Coordinate((apple.x-2),2);
                    circle_positions[4]=new Coordinate((apple.x+2),2);
                    break;
                case 40:
                    circle_positions_length=5;
                    circle_positions[0]=new Coordinate((mazeSize.x-1),(apple.y+1));
                    circle_positions[1]=new Coordinate((mazeSize.x-2),(apple.y-1));
                    circle_positions[2]=new Coordinate((mazeSize.x-1),(apple.y-1));
                    circle_positions[3]=new Coordinate((mazeSize.x-3),(apple.y-2));
                    circle_positions[4]=new Coordinate((mazeSize.x-3),(apple.y+2));
                    break;
                case 41:
                    circle_positions_length=5;
                    circle_positions[0]=new Coordinate(0,(apple.y+1));
                    circle_positions[1]=new Coordinate(1,(apple.y-1));
                    circle_positions[2]=new Coordinate(0,(apple.y-1));
                    circle_positions[3]=new Coordinate(2,(apple.y-2));
                    circle_positions[4]=new Coordinate(2,(apple.y+2));
                    break;
                default: break;
            }
        }

    public Direction circle_direction(Coordinate apple,int circle_type,Snake snake,Coordinate head) {
        if(head.equals(circle_positions[circle_positions_iterator%circle_positions_length])){
            circle_positions_iterator++;
        }
        return pathTo(circle_positions[circle_positions_iterator%circle_positions_length]);
    }


    /**
     * Makes a BFS graph
     * this does not have a return function but this:
     * makes a BFS graph of the current stat: init_cameFrom,
     * marks the depth (distance) for each reachable coordinate
     * notes the old size number of positions reachable for the snake
     */
    private void makeBFSGraph(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        oldReachable = snake_cameFrom.size();
        snake_cameFrom.clear();
        snake_depth.clear();
        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[head.x][head.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x,y)) || opponent.elements.contains(new Coordinate(x,y)) ) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        //TODO: deze regel hieronder doet niks toch? Kan weg volgens mij (BramG, vraag aan Imre)
        snake.body.getLast();
        // Also ignore possible locations for the next head for the opponent to avoid tieing
        // Only if the player is behind, otherwise it's worth the risk
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of step required to get to coordinate
        snake_cameFrom.put(head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(head); // Add head as starting point for the queue
        snake_depth.put(head, 0);
        int depth_level_handled = 0; // Tells if we already tog
        int weCanReachApple = 0; // Turns one if true
        int opponentCanReachApple = 0;
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = snake_depth.get(start) + 1; // The depth level
            if (apple.inBounds(mazeSize)) {
                if (snake_depth.containsKey(apple)) {
                    if (depth_level >= snake_depth.get(apple)) {
                        weCanReachApple = 1;
                    }
                }
                if (opponent_depth.containsKey(apple)) {
                    if (depth_level >= opponent_depth.get(apple)) {
                        opponentCanReachApple = 1;
                    }
                }
            }
            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    } else { // Now we are at a depth higher than the snake size, thu
                        for (Coordinate empty : findDepth(depth_level - snake.body.size() - weCanReachApple, snake_depth, mazeSize)){
                            arrayVisited[empty.x][empty.y] = false;
                        }
                    }
                }
                if (depth_level < opponent.body.size() + opponentCanReachApple) {
                    if (opponentCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() + opponentCanReachApple - depth_level];
                        arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                    }
                } else {
                    for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), opponent_depth, mazeSize)){
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                depth_level_handled++;
            }

            /* We skip this, because we want a full BFS
            if (snake_cameFrom.containsKey(apple)) {
                break; // Exit the loop, because we don't need to find the apple anymore
            }

             */
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!snake_cameFrom.containsKey(newPos)) {
                            snake_cameFrom.put(newPos,start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            snake_depth.put(newPos, depth_level);
                        }
                    }
                }
                /* We skip this, because we want to have an full BFS graph
                if (newPos == apple) {
                    break; // We found the apple in the fastes route, no need to look further
                } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }
    }

    /**
     * Determines all the blocks reachable point of the BFS graph at a certain depth
     * @param depth The depth of which you want to find all the cords
     * @param depth_grid the BFS graph which you want to check
     * @param mazeSize needed to check the whole grid
     * @return Coordinate array, with all tiles of requested depth
     */
    private Coordinate[] findDepth(int depth, HashMap depth_grid, Coordinate mazeSize) {
        ArrayList sol = new ArrayList();
        for (int x  = 0 ; x < mazeSize.x; x++)
            for (int y  = 0 ; y < mazeSize.y; y++) {
                Coordinate check = new Coordinate(x,y);
                if (depth_grid.containsKey(check)) {
                    if (depth_grid.get(check).equals(depth)) {
                        sol.add(check);
                    }
                }
            }
        return (Coordinate[]) sol.toArray(new Coordinate[sol.size()]);
    }

    /**
     * Returns the step toward your goal
     * @param goal  coordinate of the place where you want to go
     * @return Direction, the step to take or null if there is no route
     */
    private Direction pathTo(Coordinate goal){
        ArrayList path = new ArrayList(); // This will be the route from the goal to the head
        if (snake_cameFrom.containsKey(goal)){ // If this is not the case, the goal is enclosed by a snake
            path.add((goal));
        } else {
            return null; // Path is unreachable
        }

        while (path.get(path.size() - 1) != head) {
            path.add(snake_cameFrom.get(path.get(path.size() - 1)));
        }
        return directionFromTo(head, (Coordinate) path.get(path.size() - 2));
    }



}
