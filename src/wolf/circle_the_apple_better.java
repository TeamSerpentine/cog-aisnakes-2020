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
public class circle_the_apple_better implements Bot {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    //public Coordinate[] corners = new Coordinate[12];
    public Coordinate[] circle_positions = new Coordinate[6];
    public int circle_positions_length=0;
    public int circle_positions_iterator=0;
    public Coordinate prev_apple=null;
    public int circle_type=0;

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

        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))           // maze bounds
                .filter(d -> !opponent.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(opponent.body.getLast())) // opponent body without ass
                .filter(d -> !snake.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(snake.body.getLast())) //own body without ass
                .sorted()
                .toArray(Direction[]::new);


        if(prev_apple!=apple) {
                circle_type = circle_type(apple, snake, opponent, mazeSize);
                if(circle_type>0){
                    setCircle_positions(apple,circle_type,mazeSize);
                }

        }

        if(circle_type>0){

                    notLosing = Arrays.stream(validMoves)
                    .filter(d -> head.moveTo(d).inBounds(mazeSize))           // maze bounds
                    .filter(d -> !opponent.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(opponent.body.getLast())) // opponent body without ass
                    .filter(d -> !snake.elements.contains(head.moveTo(d)) || head.moveTo(d).equals(snake.body.getLast())) //own body without ass
                    .filter(d -> !(head.moveTo(d).equals(apple))) // when circling, don't eat the apple
                    .sorted()
                            .toArray(Direction[]::new);

                    chosenDirection = circle_direction(apple,circle_type,snake,head);


        }else{
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

        if(notLosing.length>0) {
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
                    return 1;
                }
            }

            }
        return 0;
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

    public Direction circle_direction(Coordinate apple,int circle_type,Snake snake,Coordinate head){

            if(head.equals(circle_positions[circle_positions_iterator%circle_positions_length])){
                circle_positions_iterator++;
            }
        return directionFromTo(head,circle_positions[circle_positions_iterator%circle_positions_length]);
    }
}
