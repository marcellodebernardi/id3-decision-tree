import org.junit.Test;
import org.math.plot.*;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;


/**
 * Black-box unit testing to assess the behavior of the learning algorithm.
 */
public class BehaviorTests {
    private ID3 classifier;


    public BehaviorTests() {
        classifier = new ID3();
    }


    /**
     * Draw learning curve for the learning algorithm.
     */
    @Test
    public void learningCurves() throws IOException {
        ID3 classifier = new ID3();

        String[][] dataset = ID3.parseCSV("./data/realEstateTrain.csv");
        String[][] testData = ID3.parseCSV("./data/realEstateTest.csv");
        drawLearningCurve(classifier, dataset, testData);

        dataset = ID3.parseCSV("./data/simpleTrain.csv");
        testData = ID3.parseCSV("./data/simpleTest.csv");
        drawLearningCurve(classifier, dataset, testData);
    }


    // HELPER: draw a learning curve for the classifier
    private void drawLearningCurve(ID3 classifier, String[][] trainData, String[][] testData) {
        double[] datasetSize = new double[trainData.length];
        double[] accuracy = new double[trainData.length];
        int trials = 20;

        // train and evaluate for each slice of dataset
        // todo split into trainAndEval()
        for (int size = 1; size <= datasetSize.length; size++) {
            int testAccuracy = 0;

            // multiple trials per size
            for (int trial = 0; trial < trials; trial++) {
                classifier.train(Arrays.copyOfRange(trainData, 0, size)); // TODO: 3/3/2018 shuffle
                classifier.classify(testData);

                // compute classification accuracy as ratio of correct classifications to total classifications
                testAccuracy += (int) IntStream
                        .range(0, testData.length)
                        .filter(result ->
                                classifier
                                        .classification()[result]
                                        .equals(testData[result][testData[0].length - 1]))
                        .count() / testData.length;
            }

            // write results
            testAccuracy /= trials;
            datasetSize[size - 1] = size;
            accuracy[size - 1] = testAccuracy;
        }

        // draw plot
        Plot2DPanel plot = new Plot2DPanel();
        plot.addLinePlot("Learning curve", datasetSize, accuracy);

        JFrame frame = new JFrame("Plot panel");
        frame.setContentPane(plot);
        frame.setVisible(true);
    }
}
