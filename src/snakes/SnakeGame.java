package snakes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

/**
 * Implements main game flow
 * Run game for two bots
 */
public class SnakeGame {
    private static final String LOG_FILE = "log.txt";
    private static final long TIMEOUT_THRESHOLD = 1;// timeout threshold for taking a decision in seconds
    public final Snake snake0, snake1;
    public final Coordinate mazeSize;
    private final Bot bot0, bot1;
    private final Random rnd = new Random();
    private final Boolean parallel;
    public Coordinate appleCoordinate;
    public String gameResult = "0 - 0";
    public int appleEaten0 = 0;
    public int appleEaten1 = 0;
    private int snakeSize;
    public String name0, name1;
    public long startTime;

    private SnakesRunner bot0_runner, bot1_runner;

    /*
    START ADDED BY SERPENTINE
    */
    //Keep track of last taken action
    private Direction d0, d1;
    private String endReason;
    public boolean s0dead, s1dead;
    /*
    END ADDED BY SERPENTINE
    */

    /**
     * Constructs SnakeGame class
     *
     * @param mazeSize size of the game board
     * @param head0    initial coordinate of first snake's head
     * @param tailDir0 initial direction of first snake's tail
     * @param head1    initial coordinate of second snake's head
     * @param tailDir1 initial direction of first snake's tail
     * @param size     initial length of snakes
     * @param bot0     first smart snake bot
     * @param bot1     second smart snake bot
     */
    public SnakeGame(Coordinate mazeSize, Coordinate head0, Direction tailDir0, Coordinate head1, Direction tailDir1, int size,
                     Bot bot0, Bot bot1) {
        snakeSize = size;
        this.startTime = System.currentTimeMillis();
        this.mazeSize = mazeSize;
        this.snake0 = new Snake(head0, tailDir0, size, mazeSize);
        this.snake1 = new Snake(head1, tailDir1, size, mazeSize);
        this.bot0 = bot0;
        this.bot1 = bot1;
        this.name0 = bot0.getClass().getSimpleName();
        this.name1 = bot1.getClass().getSimpleName();

        /*
        START ADDED BY SERPENTINE
         */
        /*
         Default not parallel running
         Setting this to true will alter the behaviour of the snakegame in a few key ways.
         It allows for thread safe running of a game, and bot. The assumption is made that a bot is memoryless and
         thus only takes the current state into account when deciding on which action to take. The limit on time is
         removed because
         */

        this.parallel = false;


        /*
        END ADDED BY SERPENTINE
         */

        appleCoordinate = randomNonOccupiedCell();

        this.bot0_runner = new SnakesRunner(bot0, snake0, snake1, mazeSize, appleCoordinate);
        this.bot1_runner = new SnakesRunner(bot1, snake1, snake0, mazeSize, appleCoordinate);
    }

    /**
     * Constructs SnakeGame class
     *
     * @param mazeSize size of the game board
     * @param head0    initial coordinate of first snake's head
     * @param tailDir0 initial direction of first snake's tail
     * @param head1    initial coordinate of second snake's head
     * @param tailDir1 initial direction of first snake's tail
     * @param size     initial length of snakes
     * @param bot0     first smart snake bot
     * @param bot1     second smart snake bot
     * @param parallel run games in parallel WARNING: will not terminate on turn taken to long. Only use to train a bot!
     */
    public SnakeGame(Coordinate mazeSize, Coordinate head0, Direction tailDir0, Coordinate head1, Direction tailDir1, int size,
                     Bot bot0, Bot bot1, Boolean parallel) {
        snakeSize = size;
        this.startTime = System.currentTimeMillis();
        this.mazeSize = mazeSize;
        this.snake0 = new Snake(head0, tailDir0, size, mazeSize);
        this.snake1 = new Snake(head1, tailDir1, size, mazeSize);
        this.bot0 = bot0;
        this.bot1 = bot1;
        this.name0 = bot0.getClass().getSimpleName();
        this.name1 = bot1.getClass().getSimpleName();
        this.parallel = parallel;

        appleCoordinate = randomNonOccupiedCell();


        this.bot0_runner = new SnakesRunner(bot0, snake0, snake1, mazeSize, appleCoordinate);
        this.bot1_runner = new SnakesRunner(bot1, snake1, snake0, mazeSize, appleCoordinate);

    }


    /**
     * Converts game to string representation
     *
     * @return game state as a string
     */
    public String toString() {
        char[][] cc = new char[mazeSize.x][mazeSize.y];
        for (int x = 0; x < mazeSize.x; x++)
            for (int y = 0; y < mazeSize.y; y++)
                cc[x][y] = '.';

        // Coordinate of head of first snake on board
        Coordinate h0 = snake0.getHead();
        cc[h0.x][h0.y] = 'h';

        // Coordinate of head of second snake on board
        Coordinate h1 = snake1.getHead();
        cc[h1.x][h1.y] = 'H';

        Iterator<Coordinate> it = snake0.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = 'b';
        }

        it = snake1.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = 'B';
        }

        cc[appleCoordinate.x][appleCoordinate.y] = 'X';

        StringBuilder sb = new StringBuilder();
        for (int y = mazeSize.y - 1; y >= 0; y--) {
            for (int x = 0; x < mazeSize.x; x++)
                sb.append(cc[x][y]);
            if (y != 0)
                sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Outputs text to stdout and file
     *
     * @param text text that should be displayed
     */
    private void output(String text) {
        FileWriter fw;
        try {
            fw = new FileWriter(LOG_FILE, true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            fw.write(text + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Run one game step, return whether to continue the game
     *
     * @return whether to continue the game
     */
    public boolean runOneStep() throws InterruptedException {
        output(toString());

        if (this.parallel) {
            /*
            START ADDED BY SERPENTINE
            previously only the code within the else statement existed
            */
            synchronized (this) {
                d0 = bot0.chooseDirection(this.snake0, this.snake1, this.mazeSize, this.appleCoordinate);
            }
            synchronized (this) {
                d1 = bot1.chooseDirection(this.snake1, this.snake0, this.mazeSize, this.appleCoordinate);
            }
            /*
            END ADDED BY SERPENTINE
             */
        } else {
            // the first bot takes a decision of next move

            bot0_runner.apple = appleCoordinate;
            Thread bot0_thread = new Thread(bot0_runner);

            bot0_thread.start();
            bot0_thread.join(TIMEOUT_THRESHOLD * 1000);
            boolean s0timeout = false;
            if (bot0_thread.isAlive()) {
                bot0_thread.interrupt();
                s0timeout = true;
                System.out.println(bot0.getClass().getSimpleName() + " took too long to make a decision");
            }

            d0 = bot0_runner.chosen_direction;

            // the second bot takes a decision of next move
            bot1_runner.apple = appleCoordinate;
            Thread bot1_thread = new Thread(bot1_runner);

            bot1_thread.start();
            bot1_thread.join(TIMEOUT_THRESHOLD * 1000);

            boolean s1timeout = false;
            if (bot1_thread.isAlive()) {
                bot1_thread.interrupt();
                s1timeout = true;
                System.out.println(bot1.getClass().getSimpleName() + " took too long to make a decision");
            }

            d1 = bot1_runner.chosen_direction;

            /*
                Stopping game condition
                - one of the snakes decides what it's next move too long
             */
            boolean timeout = s0timeout || s1timeout;
            if (timeout) {
                gameResult = "";
                String result = (s0timeout ? 0 : 1) + " - " + (s1timeout ? 0 : 1);
                gameResult += result;
                return false;
            }
        }


        output("snake0->" + d0 + ", snake1->" + d1);
        output("Apples eaten: " + appleEaten0 + " - " + appleEaten1);

        //var grow = move % 3 == 2;
        boolean grow0 = snake0.getHead().moveTo(d0).equals(appleCoordinate);
        boolean grow1 = snake1.getHead().moveTo(d1).equals(appleCoordinate);

        boolean wasGrow = grow0 || grow1;

        s0dead = !snake0.moveTo(d0, grow0);
        s1dead = !snake1.moveTo(d1, grow1);

        if (wasGrow || appleCoordinate == null) {
            appleEaten0 = snake0.body.size() - snakeSize;
            appleEaten1 = snake1.body.size() - snakeSize;
            appleCoordinate = randomNonOccupiedCell();
        }
        s0dead |= snake0.headCollidesWith(snake1);
        s1dead |= snake1.headCollidesWith(snake0);

        /* stopping game condition
            - one of snakes collides with something
        */
        boolean cont = !(s0dead || s1dead);

        if (!cont) {
            gameResult = "";
            String result = "0 - 0";
            if (s0dead ^ s1dead)
                result = (s0dead ? 0 : 1) + " - " + (s1dead ? 0 : 1);
            else if (s0dead && s1dead)
                result = (appleEaten0 > appleEaten1 ? 1 : 0) + " - " + (appleEaten1 > appleEaten0 ? 1 : 0);

            endReason = "Bot 0 is " + (s0dead ? "dead" : "alive") + " Bot 1 is " + (s1dead ? "dead" : "alive");
            gameResult += result;
        }
        return cont;
    }

    /**
     * Run the game
     */
    public void run() throws InterruptedException {
        while (runOneStep())
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }

        output(gameResult);
    }

    /**
     * Selects random non-occupied cell of maze
     *
     * @return random non-occupied coordinate of the game board
     */
    private Coordinate randomNonOccupiedCell() {
        while (true) {
            Coordinate c = new Coordinate(rnd.nextInt(mazeSize.x), rnd.nextInt(mazeSize.y));
            if (snake0.elements.contains(c))
                continue;
            if (snake1.elements.contains(c))
                continue;

            return c;
        }
    }

    /*
    START ADDED BY SERPENTINE
     */

    /**
     * @return Array of taken action this turn [d0, d1]
     */
    public Direction[] getLastActions() {
        return new Direction[]{this.d0, this.d1};
    }

    /**
     * @return Reason why the game ended
     */
    public String endReason() {
        return this.endReason;
    }

    /**
     * Converts game to int board representation
     * Body is represented by 1 and head by a 2
     * Snake0 is represented by the negative numbers
     * Snake1 is represented by the positive numbers
     * The apple is represented by 10
     *
     * @return game state as a 2d int array
     */
    public int[][] toIntArray() {
        int[][] cc = new int[mazeSize.x][mazeSize.y];
        for (int x = 0; x < mazeSize.x; x++)
            for (int y = 0; y < mazeSize.y; y++)
                cc[x][y] = 0;

        // Coordinate of head of first snake on board
        Coordinate h0 = snake0.getHead();
        cc[h0.x][h0.y] = -2;

        // Coordinate of head of second snake on board
        Coordinate h1 = snake1.getHead();
        cc[h1.x][h1.y] = 2;

        Iterator<Coordinate> it = snake0.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = -1;
        }

        it = snake1.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = 1;
        }

        cc[appleCoordinate.x][appleCoordinate.y] = 3;

        return cc;
    }

    /*
    END ADDED BY SERPENTINE
     */
}
