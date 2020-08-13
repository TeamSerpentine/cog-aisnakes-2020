package snakes;

import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Implements training dojo of the snake game
 */
public class GeneticDojoMain {
    private static final String LOG_DIRECTORY_PATH = "logs";
    private static FileWriter results_fw;
    private static int[][] total_results_table;

    private static final int MAX_GAME_STEP = 10000;
    private static final int MAX_EPISODES = 100000;
    private static final int POPULATION_SIZE = (int) Math.pow(2, 10);
    private static final int SURVIVALIST = (int) Math.pow(2, 6);
    private static final int PARENTS_PER_CHILD = (int) Math.pow(2, 2);

    private static AtomicInteger gameId = new AtomicInteger(0);

    Random rng = new Random();

    /**
     * UI Entry point
     *
     * @param args Two classes implementing the Bot interface
     */
    public static void main(String[] args) throws InterruptedException {

        ArrayList<Bot> singleBots = new ArrayList<>();
        ArrayList<Bot> bots = new ArrayList<>();
        BotLoader loader = new BotLoader();

        for (String arg : args) {
            singleBots.add(loader.getBotClass(arg));
        }
        System.out.println("Test");
        System.out.println(singleBots.toString());
        Bot[] population = new Bot[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = loader.getBotClass(args[0]);
        }

        GeneticDojoMain dojo = new GeneticDojoMain();

        for (int episode = 0; episode < MAX_EPISODES; episode++) {
            System.out.println("Start of episode: " + episode);
            Map<Bot, Integer> survivors = dojo.run_tournament(population);
            dojo.run_game(survivors.keySet().stream().findFirst().get(), survivors.keySet().stream().findFirst().get(), 10000, true);
            System.out.println("Episode: " + episode + " out of " + MAX_EPISODES);
            population = dojo.reproduce(survivors);
        }
        System.out.println("END OF ALL GAMES");

        Random rng = new Random();
        while (true) {
            Bot player0 = population[rng.nextInt(population.length)];
            Bot player1 = population[rng.nextInt(population.length)];
            dojo.run_game(player0, player1, 100000, true);
        }
    }

    public GeneticDojoMain() {

    }

    public Bot[] reproduce(Map<Bot, Integer> parents) {
        Bot[] newPopulation = new Bot[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            double[][] parentDNA = IntStream.range(1, PARENTS_PER_CHILD).parallel()
                    .mapToObj(dna -> pickWeightedRandom(parents).getDNA()).toArray(double[][]::new);
            newPopulation[i] = pickWeightedRandom(parents).giveBirth(parentDNA);
        }
        return newPopulation;
    }

    private Bot pickWeightedRandom(Map<Bot, Integer> parents) {
        int uppperBound = parents.values().stream().reduce(Integer::sum).orElse(0);
        int parentValue = rng.nextInt(uppperBound);
        int lowerBound = 0;
        for (Map.Entry<Bot, Integer> parent : parents.entrySet()) {
            if (parent.getValue() < lowerBound + parentValue) {
                return parent.getKey();
            } else {
                lowerBound += parent.getValue();
            }
        }
        //Should not happen
        return parents.keySet().iterator().next();
    }

    /**
     * @return
     */
    public Map<Bot, Integer> run_tournament(Bot[] players) {
        int ROUNDS = 16;
        //Create 2 player pools in which each player is 2 times so they all play 4 matches every tournament
        ArrayList<Bot> players0 = new ArrayList<>(Arrays.asList(players));
        ArrayList<Bot> players1 = new ArrayList<>(Arrays.asList(players));

        for (int i = 0; i < ROUNDS - 1; i++) {
            players0.addAll(Arrays.asList(players));
            players1.addAll(Arrays.asList(players));
        }

        Collections.shuffle(players0, rng);
        Collections.shuffle(players1, rng);

        // Plays all bots at least ones
        Stream<Bot[]> matchUps = IntStream.range(0, players0.size()).parallel()
                .mapToObj(match -> new Bot[]{players0.get(match), players1.get(match)});

        // Run the matches and collect the winners
        List<Score<Bot, Integer>> results = matchUps.map(matchUp -> run_game(matchUp, MAX_GAME_STEP))
                .flatMap(Collection::stream).collect(Collectors.toList());


        Stream<Map.Entry<Bot, Integer>> bestStream = results.stream().collect(Collectors
                //Merge the results
                .toMap(Score::getBot, Score::getScore, Integer::sum))
                //Sort the results
                .entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                //Limit the results
                .limit(SURVIVALIST).peek(System.out::println);
        //Collect the results
        //.map(Map.Entry::getKey).toArray(Bot[]::new);
        Map<Bot, Integer> bestBots = bestStream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return bestBots;
    }

    private ArrayList<Score<Bot, Integer>> run_game(Bot[] bots, int maxGameStep) {
        return run_game(bots[0], bots[1], maxGameStep, false);
    }

    private ArrayList<Score<Bot, Integer>> run_game(Bot bot0, Bot bot1, int maxGameStep, boolean show) {
        // init game settings
        Coordinate mazeSize = new Coordinate(14, 14);
        Coordinate head0 = new Coordinate(2, 2);
        Direction tailDirection0 = Direction.DOWN;
        Coordinate head1 = new Coordinate(5, 5);
        Direction tailDirection1 = Direction.UP;
        int snakeSize = 3;
        ArrayList<int[][]> states = new ArrayList<>();
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
        }

        int gameStep = 0;
        /*
        Run the game until it should be stopped
         */
        try {
            while (game.runOneStep() && gameStep < maxGameStep) {
                gameStep++;
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        //System.out.println("Game " + id + " ended, because: " + (gameStep < maxGameStep ? game.endReason() : "Max game steps reached"));

        int score0 = game.appleEaten0;
        int score1 = game.appleEaten1;

        int winner = 0;

//        if (game.s0dead == game.s1dead) {
//            if (score0 > score1) {
//                // Bot 0 has higher score
//                score0 += 9;
//            } else if (score0 < score1){
//                score1 += 9;
//            }
//            //Equal score
//        } else if (game.s0dead) {
//            //Check if one of the 2 is dead
//            score0 /= 2;
//            score1 *= 2;
//        } else {
//            score0 *= 2;
//            score1 /= 2;
//        }

        if (game.s0dead) {
            score0 /= 4;
            score1 *= 2;
        } else {
            score0 *= 2;
        }

        if (game.s1dead) {
            score1 /= 4;
            score0 *= 2;
        } else {
            score1 *= 2;
        }

        ArrayList<Score<Bot, Integer>> scoreList = new ArrayList<>();
        scoreList.add(new Score<>(bot0, score0));
        scoreList.add(new Score<>(bot1, score1));
        return scoreList;
    }

    class Score<K, V> {
        private final K bot;
        private final V score;

        public Score(K bot, V score) {
            this.bot = bot;
            this.score = score;
        }

        public K getBot() {
            return bot;
        }

        public V getScore() {
            return score;
        }
    }
}
