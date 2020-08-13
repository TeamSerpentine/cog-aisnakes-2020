package snakes;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Implements training dojo of the snake game
 */
public class TrainingDojoMain {
    private static final String LOG_DIRECTORY_PATH = "logs";
    private static FileWriter results_fw;
    private static int[][] total_results_table;

    private static final int MAX_GAME_STEP = 1000;
    private static final int MAX_EPISODES = 100000;
    private static final int REPETITIONS = 1;

    private static AtomicInteger gameId = new AtomicInteger(0);
    private final Bot[] opponents;

    /**
     * UI Entry point
     *
     * @param args Two classes implementing the Bot interface
     */
    public static void main(String[] args) {

        Bot[] bots = new Bot[args.length];
        BotLoader loader = new BotLoader();

        for (int i = 0; i < args.length; i++) {
            bots[i] = loader.getBotClass(args[i]);
        }

        Bot trainingBot = bots[0];

        TrainingDojoMain dojo = new TrainingDojoMain(bots);

        for (int episode = 0; episode < MAX_EPISODES; episode++) {
            System.out.println("Start of episode: " + episode);
            trainingBot.setTrainingMode(true);
            // Play a Tournament and save the games for training
            playedGame[] results = dojo.run_tournament(trainingBot);
            System.out.println("Episode: " + episode + " out of " + MAX_EPISODES +
                    " has finished with average game length: " +
                    Arrays.stream(results).mapToInt(i -> i.states.size()).average().orElse(0) +
                    " average score for snake0: " + Arrays.stream(results).mapToInt(i -> i.score0).average().orElse(0) +
                    " average score for snake1: " + Arrays.stream(results).mapToInt(i -> i.score1).average().orElse(0));
            trainingBot.update(results);
            if (episode % 32 == 0) {
                trainingBot.setTrainingMode(false);
                dojo.run_game(trainingBot, trainingBot, 10000, true);
            }
        }
        System.out.println("END OF ALL GAMES");
    }

    public TrainingDojoMain(Bot[] opponents) {
        this.opponents = new Bot[opponents.length * REPETITIONS];
        for (int i = 0; i < this.opponents.length; i++) {
            this.opponents[i] = opponents[i % opponents.length];
        }
    }

    /**
     * @param trainingBot
     * @return
     */
    public playedGame[] run_tournament(Bot trainingBot) {
        // Plays all bots against all other bots (including itself)
        Stream<Object> results0 = Arrays.stream(opponents)
                .parallel().map(matchUp -> run_game(matchUp, trainingBot, MAX_GAME_STEP, false));

        Stream<Object> results1 = Arrays.stream(opponents)
                .parallel().map(matchUp -> run_game(trainingBot, matchUp, MAX_GAME_STEP, false));

        return Stream.concat(results0, results1).toArray(playedGame[]::new);
    }

    private playedGame run_game(Bot[] bots, int maxGameStep) {
        return run_game(bots[0], bots[1], maxGameStep, false);
    }

    private playedGame run_game(Bot bot0, Bot bot1, int maxGameStep, boolean show) {
        // init game settings
        Coordinate mazeSize = new Coordinate(6, 6);
        Coordinate head0 = new Coordinate(2, 5);
        Direction tailDirection0 = Direction.DOWN;
        Coordinate head1 = new Coordinate(2, 2);
        Direction tailDirection1 = Direction.UP;
        int snakeSize = 3;
        ArrayList<state> states = new ArrayList<>();
        ArrayList<Direction> actions0 = new ArrayList<>();
        ArrayList<Direction> actions1 = new ArrayList<>();
        int id = gameId.incrementAndGet();

        SnakeGame game = new SnakeGame(mazeSize, head0, tailDirection0, head1, tailDirection1, snakeSize, bot0, bot1, true);
        if (show) {
            try {
                SnakesWindow window = new SnakesWindow(game);
                Thread t = new Thread(window);
                t.start();
                t.join();
                window.closeWindow();
            } catch (InterruptedException e) {
            }
            playedGame playedGame = new playedGame(game, null, null, null, null, null);
            return playedGame;
        } else {


            int gameStep = 0;
        /*
        Run the game until it should be stopped
         */
            String reason;
            try {
                while (game.runOneStep() && gameStep < maxGameStep) {
                    states.add(new state(game.snake0, game.snake1, game.appleCoordinate));
                    Direction[] lastActions = game.getLastActions();
                    actions0.add(lastActions[0]);
                    actions1.add(lastActions[1]);
                    gameStep++;
                }
            } catch (InterruptedException e) {
                System.out.println(e);
            }
            //System.out.println("Game " + id + " ended, because: " + (gameStep < maxGameStep ? game.endReason() : "Max game steps reached"));

            return new playedGame(game, states, actions0, actions1, bot0, bot1);
        }
    }

    public class state {
        public final Snake snake0;
        public final Snake snake1;
        public final Coordinate apple;

        public state(Snake snake0, Snake snake1, Coordinate apple) {
            this.snake0 = snake0.clone();
            this.snake1 = snake1.clone();
            this.apple = apple;
        }
    }

    public class playedGame {
        public final ArrayList<state> states;
        public final ArrayList<Direction> actions0, actions1;
        public final Bot b0, b1;
        public final int winner;
        public final Coordinate mazeSize;
        public final int score0, score1;
        public final boolean s0dead, s1dead;

        public playedGame(SnakeGame game, ArrayList<state> states, ArrayList<Direction> actions0, ArrayList<Direction> actions1,
                          Bot b0, Bot b1) {
            this.states = states;
            this.mazeSize = game.mazeSize;
            this.actions0 = actions0;
            this.actions1 = actions1;
            this.b0 = b0;
            this.b1 = b1;

            s0dead = game.s0dead;
            s1dead = game.s1dead;

            score0 = game.appleEaten0;
            score1 = game.appleEaten1;


            if (s0dead == s1dead) {
                if (score0 == score1) {
                    //Both dead and equal score
                    this.winner = 0;
                } else if (score0 > score1) {
                    this.winner = -1;
                } else {
                    this.winner = 1;
                }
            } else if (s0dead) {
                //Check if one of the 2 is dead
                this.winner = 1;
            } else {
                this.winner = -1;
            }
        }

        @Override
        public String toString() {
            return "playedGame{" +
                    "states=" + states +
                    ", actions0=" + actions0 +
                    ", actions1=" + actions1 +
                    ", b0=" + b0 +
                    ", b1=" + b1 +
                    ", winner=" + winner +
                    ", score0=" + score0 +
                    ", score1=" + score1 +
                    '}';
        }
    }
}
