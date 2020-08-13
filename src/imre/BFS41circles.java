package imre;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;

/**
 * This is copied from mergedBot on 23-04-2020
 * Then added all the 41 circles strategy
 *
 */
public class BFS41circles implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    // for BFS
    private Map<Coordinate, Coordinate> snake_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> snake_depth = new HashMap<Coordinate, Integer>();
    private Map <Coordinate, Coordinate> opponent_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> opponent_depth = new HashMap<Coordinate, Integer>();
    private int oldReachable;
    private int opponent_oldReachable;
    private boolean checkCanEnclose;

    // for checking if circle is complete
    private Map <Coordinate, Coordinate> opponent_breakOnApple_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap <Coordinate, Integer> opponent_breakOnApple_depth = new HashMap<Coordinate, Integer>();

    // to circle the apple
    public Coordinate[] circle_positions = new Coordinate[6];
    public int circle_positions_length=0;
    public int circle_positions_iterator=0;
    public int circle_type=0;
    public boolean dontTakeApple = false;

    public boolean circle_complete = false;  // says if the circle is complete
    public int circle_moves_after_apple_change;
    public int max_circle_moves_after_apple_change=4;
    public boolean apple_changed_during_circle=false;
    public boolean closed_circle = true;  // says if we need to have a closed circle



    public Coordinate head;
    public Coordinate apple;
    public Coordinate prev_apple=null;


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
        this.head = snake.getHead();
        this.apple = apple;
        return chooseMove(snake, opponent, mazeSize, apple);
    }


    /**
     * The logic to decide where to move
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */
    private Direction chooseMove(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        Direction move; // The variable to be able to run more code before returning the direction

        if(apple_changed_during_circle) {
            circle_moves_after_apple_change--;
            if (circle_moves_after_apple_change == 0) {
                circle_type = circle_type(apple, snake, opponent, mazeSize);
                if (circle_type > 0) {
                    setCircle_positions(apple, circle_type, mazeSize);
                }
                System.out.println("Apple has changed position, circle_type is now: " + circle_type);
                circle_complete = false;
                circle_moves_after_apple_change = max_circle_moves_after_apple_change;
                apple_changed_during_circle=false;

            }
        }
        if (prev_apple!=apple) {  // the apple has moved!
            // so, we need to determine the new circle_type
            if(circle_complete){
                apple_changed_during_circle=true;
            }else{
                circle_type = circle_type(apple, snake, opponent, mazeSize);
                if (circle_type > 0) {
                    setCircle_positions(apple, circle_type, mazeSize);
                }
                System.out.println("Apple has changed position, circle_type is now: " + circle_type);
            }
        }
        prev_apple = apple;

        // circle_type = 0 means we're not circling the apple
        if (circle_type>0) {  // here we are circling
            if(!circle_complete){
                circle_complete=circleComplete(snake,opponent,mazeSize,apple);
            }
            // filter out the apple from BFS's paths
            dontTakeApple = true;
            makeOpponentBFSGraph(snake, opponent, mazeSize, apple); // We do this before making our BFS because we want to calculate the distance to the apple
            makeBFSGraph(snake, opponent, mazeSize, apple);
            move = circle_direction();

        } else {  // here we are not circling


            // we could do BFS or alpha-beta here, now doing BFS
            dontTakeApple = false;
            makeOpponentBFSGraph(snake, opponent, mazeSize, apple); // We do this before making our BFS because we want to calculate the distance to the apple
            makeBFSGraph(snake, opponent, mazeSize, apple);
            move = pathTo(apple);
            if (move == null || !checkMove(snake, opponent, mazeSize, apple, move)) {
                // we can't reach the apple apparently OR the move results in a problem (encloses itself)
                Coordinate furthestPoint = getFurthestPoint(snake_depth, mazeSize);
                move = pathTo(furthestPoint);
                if (move == null) {
                    move = notLosingAlgorithm(snake, opponent, mazeSize)[0];
                }
            }

        }
        return move;
    }

    /**
     * To be called upon apple position change. Looks whether circling the apple is possible
     * Body length must be at least one longer than opponent's length
     * Body length must be an even number
     * Difference between closed and open circle:
     * Closed circle: apple is fully blocked. Open circle: Opponent can get to apple but will die or tie (For sure)
     * If body length is not at least 2 longer than opponent, circle must be closed.
     * Some apple positions offer more possibilities in certain game-states
     *
     * The function returns the circle type: One of the 41 types (with some mirrored duplicates)
     * If no circle type is possible, returns 0 - Cannot or should not circle apple
     * Check the PDF for the 41 circle types
     * @param apple
     * @param snake
     * @param opponent
     * @param mazeSize
     * @return
     */
    public int circle_type(Coordinate apple, Snake snake, Snake opponent, Coordinate mazeSize){
        int body=snake.body.size();
        if(body%2==0 && body>opponent.body.size() && body>5){
            boolean in_corner=false;
            closed_circle=true;
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

    /**
     * Sets the positions that should at least be covered to ensure the circle type
     * Sets these positions in a public array
     * Sets the number of positions so circle_direction() knows what elements of the array to cover
     * @param apple
     * @param circle_type
     * @param mazeSize
     */
    public void setCircle_positions(Coordinate apple, int circle_type, Coordinate mazeSize){
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

    /**
     * Returns the direction needed to follow the circle pattern
     * Uses pathTo() to determine the direction from current position to the next circle position
     * When a position is reached, selects the next position
     * @return
     */
    public Direction circle_direction() {
        if (head.equals(circle_positions[circle_positions_iterator%circle_positions_length])) {
            circle_positions_iterator++;
        }
        if(circle_type>9 && circle_type<38){
            if(circle_positions_iterator==1 && head.equals(circle_positions[2])){
                Coordinate temp = circle_positions[0];
                circle_positions[0] = circle_positions[1];
                circle_positions[1] = temp;
                circle_positions_iterator=0;
            }
        }
        if(circle_type>37){
            if(circle_positions_iterator==1 && head.equals(circle_positions[4])){
                Coordinate temp1 = circle_positions[1];
                Coordinate temp2 = circle_positions[2];
                Coordinate temp3 = circle_positions[3];
                circle_positions[1] = circle_positions[4];
                circle_positions[2] = temp3;
                circle_positions[3] = temp2;
                circle_positions[4] = temp1;
                circle_positions_iterator=2;
            }
        }
        if(circle_positions_iterator>6){
            circle_positions_iterator=circle_positions_iterator%circle_positions_length;
        }
        return pathTo(circle_positions[circle_positions_iterator%circle_positions_length]);
    }

    /**
     * checks if the circle is complete
     * @param snake our snake
     * @param opponent other snake
     * @param mazeSize the board
     * @param apple the apple
     * @return true if complete, false if not yet
     */
    public boolean circleComplete(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        // first check if all circle positions are covered
        for (int i=0; i < circle_positions_length; i++) {
            if (!snake.elements.contains(circle_positions[i])) {
                return false;
            }
        }

        makeOpponentBFSGraphBreakOnApple(snake, opponent, mazeSize, apple);
        if (closed_circle) {
            // here the opponent cannot have a path to the apple
            // and also checks if our head is next to our tail
            if (opponent_breakOnApple_cameFrom.get(apple)==null && distanceBetween(head, snake.body.getLast()) == 1) {
                return true;
            } else {
                return false;
            }
        } else {
            // here the opponent is allowed to have only 1 path to the apple
            // so check all tiles next to apple: only 1 of them should have a value in the opponent_breakOnApple_cameFrom
            int counter = 0;
            for (Direction dir : DIRECTIONS) {
                if (apple.moveTo(dir).inBounds(mazeSize)) {
                    if (opponent_breakOnApple_cameFrom.get(apple.moveTo(dir)) != null) {
                        counter++;
                    }
                }
            }
            if (counter <= 1 && distanceBetween(head, snake.body.getLast()) == 1) {
                return true;
            } else {
                return false;
            }
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
     * Determine the furthest reachable point of the BFS graph (the highest depth)
     * @param depth_grid the BFS graph on which you want to determine the furthest point
     * @param mazeSize needed to check the whole grid
     * @return Coordinate, the furthest reachable point
     */
    private Coordinate getFurthestPoint(HashMap depth_grid, Coordinate mazeSize) {
        Coordinate destination = null; //
        int distance = 0;
        // Make it search for the longest path and make it go to that path
        for (int x  = 0 ; x < mazeSize.x; x++)
            for (int y  = 0 ; y < mazeSize.y; y++) {
                Coordinate check = new Coordinate(x,y);
                if (depth_grid.containsKey(check)) {
                    if ((int) depth_grid.get(check) > distance) {
                        destination = check;
                        distance = (int) depth_grid.get(check);
                    }
                }
            }
        return destination;
    }


    /**
     * Determines all the blocks reachable point of the BFS graph at a certain depth
     * @param depth The depth of which you want to find all the cords
     * @param depth_grid the BFS graph which you want to check
     * @param mazeSize needed to check the whole grid
     * @return Coordinate, the furthest reachable point
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

        Coordinate[] answer = (Coordinate[]) sol.toArray(new Coordinate[sol.size()]);
        return answer;
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

        Direction move; // The command to move to
        while (path.get(path.size() - 1) != head) {
            path.add(snake_cameFrom.get(path.get(path.size() - 1)));
        }
        move = directionFromTo(head, (Coordinate) path.get(path.size() - 2));
        return move;
    }


    /**
     * Makes a worst case BFS graph of the new move and compares how much array it can reach
     * TODO This can also check if the highest depth of the new BFS is lower than the snakeSize  -> enclosing the snake
     * TODO currently only works for our own snake (a fix is to have the depth and camefrom graph as input)
     * This can also be used to make a general worst case BFS graph
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @param move     The move you want to check
     * @return returns the number of reachable blocks
     */
    int reachable_part(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, Direction move) {
        Coordinate newHead;
        int correction = 0;
        if (move == null) {
            newHead = snake.getHead();
        } else {
            newHead = Coordinate.add(snake.getHead(), move.v);
            correction = 1;
        }

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[newHead.x][newHead.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x, y)) || opponent.elements.contains(new Coordinate(x, y))) {
                    arrayVisited[x][y] = true;
                }
            }
        }
        arrayVisited[snake.body.getLast().x][snake.body.getLast().y] = false; // We don't reach the tail anymore

        // Also ignore possible locations for the next head for the opponent to avoid tieing
        // Only if the player is behind, otherwise it's worth the risk
        // EDIT: never ignore new head location because that could block the snake
        for (Direction step : Direction.values()) {
            Coordinate newPos = Coordinate.add(opponent.getHead(), step.v);
            if (newPos.inBounds(mazeSize)) {
                arrayVisited[newPos.x][newPos.y] = true;
            }
        }
        arrayVisited[opponent.body.getLast().x][opponent.body.getLast().y] = false; // We don't reach the tail anymore

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        Map<Coordinate, Coordinate> cameFrom = new HashMap<Coordinate, Coordinate>();
        Map<Coordinate, Integer> depth = new HashMap<Coordinate, Integer>(); // Depth = number of step required to get to coordinate
        cameFrom.put(newHead, snake.getHead()); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(newHead); // Add head as starting point for the queue
        depth.put(newHead, 0 + correction);
        int depth_level_handled = 0 + correction; // Tells if we already tog
        int weCanReachApple = 0;
        int opponentCanReachApple = 0;
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = depth.get(start) + 1; // The depth level

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

            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once

                /* !! EXPERIMENTAL RULE !! marks all the possible head locations of the enemy snake as visited
                 * A possible tweak for this, is to only mark realistic head location as true or only upto a certain depth
                 * TODO Check if you adding this when making the BFS graph for our snake will make it become overly protective
                 * currently disabled, makes the snake overly protective
                 */
                //for (Coordinate possible_enemy_head : findDepth(depth_level, opponent_depth, mazeSize)) {
                //    arrayVisited[possible_enemy_head.x][possible_enemy_head.y] = true;
                //}

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



            for (Direction step : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, step.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!cameFrom.containsKey(newPos)) {
                            cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            depth.put(newPos, depth_level);
                        }
                    }
                }
            /*
            if (newPos == apple) {
                break; // We found the apple in the fastest route, no need to look further
            } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }
        return cameFrom.size();
    }

    /**
     * checks whether a move is safe
     * @param snake us
     * @param opponent enemy
     * @param mazeSize board
     * @param apple apple
     * @param move the move we're making
     * @return true if move is safe, false if it is dangerous (high chance of enclosing ourselves)
     */
    private boolean checkMove(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, Direction move) {
        int new_reachable = reachable_part(snake, opponent, mazeSize, apple, move);
        if (((double) new_reachable / oldReachable) < 0.8) {
            System.out.println("Let's not do that");
            // TODO check when this happens there is a real treat
            return false;
        }
        return true;
    }



    /**
     * Makes a BFS graph and
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return this does not have a return function but this:
     * makes a BFS graph of the current stat: init_cameFrom,
     * marks the depth (distance) for each reachable coordinate
     * notes the old size number of positions reachable for the snake
     */
    private void makeBFSGraph(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        oldReachable = snake_cameFrom.size();
        snake_cameFrom.clear();
        snake_depth.clear();
        Coordinate head = snake.getHead();
        /*
         * The BFS algorithm
         */
        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[head.x][head.y] = true; // No need to check head
        if (dontTakeApple) {
            arrayVisited[apple.x][apple.y] = true;  // No paths going over the apple, when we don't want to do that
        }

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x,y)) || opponent.elements.contains(new Coordinate(x,y)) ) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        //TODO: deze regel hieronder doet niks toch? Kan weg volgens mij (BramG)
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
     * Makes a BFS graph of the opponent (this will be the most unaltered BFS graph
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return this does not have a return function but this:
     * makes a BFS graph of the current stat: opponent_cameFrom,
     * marks the depth (distance) for each reachable coordinate: opponent_depth
     * notes the old size number of positions reachable for the snake
     * Also includes a predictor if the opponent can be enclosed
     */
    private void makeOpponentBFSGraph(Snake opponent, Snake snake, Coordinate mazeSize, Coordinate apple) {
        opponent_cameFrom.clear();
        opponent_depth.clear();
        Coordinate opponent_head = snake.getHead();

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[opponent_head.x][opponent_head.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x,y)) || opponent.elements.contains(new Coordinate(x,y)) ) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        // This is still enabled not, because if one of these move causes trouble for the opponent snake, this means we can
        // enclose it
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of step required to get to coordinate
        opponent_cameFrom.put(opponent_head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(opponent_head); // Add head as starting point for the queue
        opponent_depth.put(opponent_head, 0);
        int weCanReachApple = 0; // Turns one if true
        int depth_level_handled = 0; // Turns one if true
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = opponent_depth.get(start) + 1; // The depth level
            if (opponent_depth.containsKey(apple)) {
                if (depth_level >= opponent_depth.get(apple)) {
                    weCanReachApple = 1;
                }
            }
            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    }
                } else { // Now we need to edit all the places we visited in the arraylist
                    for (Coordinate empty : findDepth(depth_level - weCanReachApple -snake.body.size(), opponent_depth, mazeSize)){
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                if (depth_level < opponent.body.size()) {
                    Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() - depth_level];
                    arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                } else { // TODO does not work because this is using the old BFS graph and is thus outdated
                    //for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), snake_depth,mazeSize)){
                    //    arrayVisited[empty.x][empty.y] = false;
                    //}
                }
                depth_level_handled++;
            }


            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!opponent_cameFrom.containsKey(newPos)) {
                            opponent_cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            opponent_depth.put(newPos, depth_level);
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

        if (opponent_oldReachable == 0) {
            opponent_oldReachable = opponent_cameFrom.size();
        }
        int opponent_newReachable = opponent_cameFrom.size();
        if (((double) opponent_newReachable / opponent_oldReachable) < 0.6) { // If the enemy lost significant movement
            System.out.println("enemy snake lost a lot of movement space");
            checkCanEnclose = true;
        }
    }

    /**
     * Makes a BFS graph of the opponent, but it breaks paths when apple is reached
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return this does not have a return function but this:
     * makes a BFS graph of the current stat: opponent_breakOnApple_cameFrom,
     * marks the depth (distance) for each reachable coordinate: opponent_breakOnApple_depth
     */
    private void makeOpponentBFSGraphBreakOnApple(Snake opponent, Snake snake, Coordinate mazeSize, Coordinate apple) {
        opponent_breakOnApple_cameFrom.clear();
        opponent_breakOnApple_depth.clear();
        Coordinate opponent_head = snake.getHead();

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[opponent_head.x][opponent_head.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x,y)) || opponent.elements.contains(new Coordinate(x,y)) ) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        /*
        // This is still enabled not, because if one of these move causes trouble for the opponent snake, this means we can enclose it
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }
        */

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of step required to get to coordinate
        opponent_breakOnApple_cameFrom.put(opponent_head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(opponent_head); // Add head as starting point for the queue
        opponent_breakOnApple_depth.put(opponent_head, 0);
        int weCanReachApple = 0; // Turns one if true
        int depth_level_handled = 0; // Turns one if true
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = opponent_breakOnApple_depth.get(start) + 1; // The depth level
            if (opponent_breakOnApple_depth.containsKey(apple)) {
                if (depth_level >= opponent_breakOnApple_depth.get(apple)) {
                    weCanReachApple = 1;
                }
            }
            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    }
                } else { // Now we need to edit all the places we visited in the arraylist
                    for (Coordinate empty : findDepth(depth_level - weCanReachApple -snake.body.size(), opponent_breakOnApple_depth, mazeSize)){
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                if (depth_level < opponent.body.size()) {
                    Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() - depth_level];
                    arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                } else { // TODO does not work because this is using the old BFS graph and is thus outdated
                    //for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), snake_depth,mazeSize)){
                    //    arrayVisited[empty.x][empty.y] = false;
                    //}
                }
                depth_level_handled++;
            }


            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        if (!newPos.equals(apple)) {
                            queue.add(newPos); // If this place is not visited add it to the queue, EXCEPT IF APPLE
                        }
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!opponent_breakOnApple_cameFrom.containsKey(newPos)) {
                            opponent_breakOnApple_cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            opponent_breakOnApple_depth.put(newPos, depth_level);
                        }
                    }
                }
            }
            queue.remove(0); // Remove the checked position from the search array
        }

        /*
        if (opponent_oldReachable == 0) {
            opponent_oldReachable = opponent_cameFrom.size();
        }
        int opponent_newReachable = opponent_cameFrom.size();
        if (((double) opponent_newReachable / opponent_oldReachable) < 0.6) { // If the enemy lost significant movement
            System.out.println("enemy snake lost a lot of movement space");
            checkCanEnclose = true;
        }
         */
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

    private Direction[] notLosingAlgorithm(Snake snake, Snake opponent, Coordinate mazeSize) {
        Coordinate head = snake.getHead();
        Coordinate afterHeadNotFinal = null;
        Direction move;
        if (snake.body.size() >= 2) {
            Iterator<Coordinate> it = snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }
        final Coordinate afterHead = afterHeadNotFinal;

        /* The only illegal move is going backwards. Here we are checking for not doing it */
        Direction[] validMoves = Arrays.stream(DIRECTIONS)
                .filter(d -> !head.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);

        /* Just naïve greedy algorithm that tries not to die at each moment in time */
        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                .filter(d -> !opponent.elements.contains(head.moveTo(d)))   // Don't collide with opponent...
                .filter(d -> !snake.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(snake.body.getLast()))     // and yourself
                .sorted()
                .toArray(Direction[]::new);

        if (notLosing.length > 0) {
            move = notLosing[0];
        }
        else {
            move = validMoves[0];
        }
        return notLosing;
    }


}
