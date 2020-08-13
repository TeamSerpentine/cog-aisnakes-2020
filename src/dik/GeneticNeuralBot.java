package dik;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GeneticNeuralBot implements Bot {
    double[] DNA;
    NeuralNet model;

    int input_size = 29 * 29;
    int[] layer_heights = {32, 16, 8, 4};

    Random rng = new Random();
    double mutation_rate = 0.005;
    double mutation_chance = 0.05;
    double change_change = 0.001;

    Direction[] directions = Direction.values();


    public GeneticNeuralBot(double[] DNA) {
        this.DNA = DNA;
        this.model = new NeuralNet(input_size, layer_heights, this.DNA);
    }

    public GeneticNeuralBot() {
        this.model = new NeuralNet(input_size, layer_heights);
        this.DNA = model.getWeights();
    }

    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        int[][] obs = headCenteredBoard(snake, opponent, mazeSize, apple);
        double[] flatObs = Stream.of(obs)
                .flatMapToInt(Arrays::stream)
                .asDoubleStream()
                .toArray();
        double[] QValues = model.feedForward(flatObs);

        Coordinate head = snake.getHead();

        /* Get the coordinate of the second element of the snake's body
         * to prevent going backwards */
        Coordinate afterHeadNotFinal = null;
        if (snake.body.size() >= 2) {
            Iterator<Coordinate> it = snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }

        final Coordinate afterHead = afterHeadNotFinal;

        /* The only illegal move is going backwards. Here we are checking for not doing it */
        Set<Direction> validMoves = Arrays.stream(Direction.values())
                .filter(d -> !head.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .collect(Collectors.toSet());

        for (int i = 0; i < 5; i++) {
            int q = this.argmax(QValues);
            Direction action = directions[q];
            if (validMoves.contains(action)) {
                return action;
            } else {
                QValues[q] = Double.NEGATIVE_INFINITY;
            }
        }
        return directions[rng.nextInt(directions.length)];
    }

    private int argmax(double[] array) {
        return IntStream.range(0, array.length)
                .reduce((i, j) -> array[i] > array[j] ? i : j)
                .orElse(-1);
    }

    @Override
    public double[] getDNA() {
        return this.model.getWeights();
    }

    @Override
    public Bot giveBirth(double[][] parentsDna) {

        double[] childDNA = this.DNA.clone();
        for (double[] parentDna : parentsDna) {
            for (int i = 0; i < parentDna.length; i++) {
                childDNA[i] += parentDna[i] / (parentDna.length + 1);
            }
        }

        // Add random mutation
        for (int i = 0; i < childDNA.length; i++) {
            double r = rng.nextDouble();
            if (r < mutation_chance) {
                childDNA[i] = mutate(childDNA[i]);
            } else if (r < (change_change + mutation_chance)) {
                childDNA[i] = change();
            }
        }
        return new GeneticNeuralBot(childDNA);
    }

    private double mutate(double prev_gen) {
        return prev_gen * (this.rng.nextGaussian() * this.mutation_rate + 1);
    }

    private double change() {
        return this.rng.nextDouble() * 2 - 1;
    }

    private static int[][] toBoard(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        int[][] cc = new int[mazeSize.x][mazeSize.y];
        for (int x = 0; x < mazeSize.x; x++)
            for (int y = 0; y < mazeSize.y; y++)
                cc[x][y] = 0;

        // Coordinate of head of first snake on board
        Coordinate h0 = snake.getHead();
        cc[h0.x][h0.y] = -2;

        // Coordinate of head of second snake on board
        Coordinate h1 = opponent.getHead();
        cc[h1.x][h1.y] = 2;

        Iterator<Coordinate> it = snake.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = -1;
        }

        it = opponent.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            cc[bp.x][bp.y] = 1;
        }

        cc[apple.x][apple.y] = 10;

        return cc;
    }

    private int[][] headCenteredBoard(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        int[][] board = new int[2 * mazeSize.x + 1][2 * mazeSize.y + 1];
        Coordinate head0 = snake.getHead();
        int offsetX = (mazeSize.x + 1) - head0.x;
        int offsetY = (mazeSize.y + 1) - head0.y;

        //Make everything opponent snake (Impassable)
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                board[i][j] = -1;
            }
        }

        //Clear maze
        for (int i = offsetX; i < mazeSize.x + offsetX; i++) {
            for (int j = offsetY; j < mazeSize.y + offsetY; j++) {
                board[i][j] = 0;
            }
        }

        // Coordinate of head of first snake on board
        Coordinate h0 = snake.getHead();
        board[h0.x + offsetX][h0.y + offsetY] = -2;

        // Coordinate of head of second snake on board
        Coordinate h1 = opponent.getHead();
        board[h1.x + offsetX][h1.y + offsetY] = -2;

        Iterator<Coordinate> it = snake.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            board[bp.x + offsetX][bp.y + offsetY] = -1;
        }

        it = opponent.body.stream().skip(1).iterator();
        while (it.hasNext()) {
            Coordinate bp = it.next();
            board[bp.x + offsetX][bp.y + offsetY] = -1;
        }

        board[apple.x + offsetX][apple.y + offsetY] = 2;

        return board;
    }


    class NeuralNet {
        private Layer[] layers;

        NeuralNet(int input_size, int[] layers) {
            this.layers = new Layer[layers.length];
            int prev_size = input_size;
            for (int i = 0; i < layers.length; i++) {
                this.layers[i] = new Layer(prev_size, layers[i]);
                prev_size = layers[i];
            }
        }

        NeuralNet(int input_size, int[] layers, double[] DNA) {
            this.layers = new Layer[layers.length];
            int prev_size = input_size;
            int dna_used = 0;
            for (int i = 0; i < layers.length; i++) {
                int len_weights = prev_size * layers[i];
                this.layers[i] = new Layer(prev_size, layers[i],
                        Arrays.copyOfRange(DNA, dna_used, dna_used + len_weights));
                dna_used += len_weights;
                prev_size = layers[i];
            }
        }

        double[] getWeights() {
            ArrayList<Double> weights = new ArrayList<>();
            for (int i = 0; i < this.layers.length; i++) {
                for (double weight : this.layers[i].getWeights()) {
                    weights.add(weight);
                }
            }

            return weights.stream().mapToDouble(x -> (double) x).toArray();
        }

        double[] feedForward(double[] inputs) {
            double[] hidden = inputs;
            for (Layer layer : layers) {
                hidden = layer.feedForward(hidden);
            }
            return hidden;
        }

        class Layer {
            private Neuron[] neurons;
            final int input_size;
            final int output_size;

            Layer(int input_size, int output_size) {
                this.input_size = input_size;
                this.output_size = output_size;
                neurons = new Neuron[output_size];
                for (int i = 0; i < output_size; i++) {
                    neurons[i] = new Neuron(input_size);
                }
            }

            Layer(int input_size, int output_size, double[] weights) {
                this.input_size = input_size;
                this.output_size = output_size;
                neurons = new Neuron[output_size];
                for (int i = 0; i < output_size; i++) {
                    neurons[i] = new Neuron(Arrays.copyOfRange(weights, input_size * i, input_size * (i + 1)));
                }
            }

            double[] feedForward(double[] inputs) {
                return Arrays.stream(neurons).mapToDouble(neuron -> neuron.feedForward(inputs)).toArray();
            }

            double[] getWeights() {
                double[] weights = new double[input_size * output_size];
                for (int i = 0; i < output_size; i++) {
                    double[] neuron_weights = neurons[i].getWeights();
                    for (int j = 0; j < input_size; j++) {
                        weights[i * input_size + j] = neuron_weights[j];
                    }
                }
                return weights;
            }

            class Neuron {
                private double[] weights;

                Neuron(int input_size) {
                    weights = new Random().doubles(input_size, -1, 1).toArray();
                }

                Neuron(double[] weights) {
                    this.weights = weights;
                }

                double feedForward(double[] inputs) {
                    return activation(IntStream.range(0, weights.length).parallel().mapToDouble(i -> inputs[i] * weights[i]).sum());
                }

                double activation(double out) {
                    return out;
                }

                double[] getWeights() {
                    return weights;
                }
            }
        }
    }
}
