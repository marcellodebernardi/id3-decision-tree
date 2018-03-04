// ECS629/759 Assignment 2 - ID3 Skeleton Code
// Author: Simon Dixon

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;


class ID3 {
    static final double LOG2 = Math.log(2.0);
    private int attributes;             // Number of attributes (including the class)
    private int examples;               // Number of training examples
    private TreeNode decisionTree;      // Tree learnt in training, used for classifying
    private String[][] data;            // Training data indexed by example, attribute
    private String[][] labels;         // Unique labels for each attribute
    private int[] stringCount;          // Number of unique labels for each attribute
    private int classCount;

    // temporary
    private String[] lastClassification;


    public ID3() {
        attributes = 0;
        examples = 0;
        decisionTree = null;
        data = null;
        labels = null;
        stringCount = null;
    }


    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length != 2) error("Expected 2 arguments: file names of training and test data");

        String[][] trainingData = parseCSV(args[0]);
        String[][] testData = parseCSV(args[1]);

        ID3 classifier = new ID3();
        classifier.train(trainingData);
        classifier.printTree();
        classifier.classify(testData);
    }

    /**
     * Carry out construction of the decision tree based on the examples in
     * trainingData.
     *
     * @param trainingData dataset for tree construction
     */
    public void train(String[][] trainingData) {
        indexStrings(trainingData);
        decisionTree = id3(trainingData, Arrays.copyOf(labels, labels.length));
    }

    /**
     * Execute the decision tree on the given examples in testData, and print
     * the resulting class names, one to a line, for each example in testData.
     **/
    public void classify(String[][] testData) {
        if (decisionTree == null) error("Please run training phase before classification");

        for (String[] example : testData) {
            int predictedClass = classify(example, decisionTree);
            System.out.println(labels[attributes - 1][predictedClass]);
        }

        lastClassification = new String[0];
    }

    public String[] classification() {
        if (lastClassification == null) throw new NullPointerException("No classification has been run yet.");
        else return lastClassification;
    }

    public void printTree() {
        if (decisionTree == null) error("Attempted to print null Tree");
        else System.out.println(decisionTree);
    }


    private int classify(String[] example, TreeNode node) {
        // leaf node
        if (node.children == null || node.children.length == 0) return node.value;
        // non-leaf node
        else return classify(example, node.children[indexOf(example[node.value], labels[node.value])]);
    }

    /* Executes the iterative dichotomizer 3 algorithm on the training dataset.
     * Returns the root node of the resulting decision tree. */
    private TreeNode id3(String[][] examples, String[][] attributes) {
        // all examples have same class, return node with that class
        if (perfectlyClassified(examples)) {
            return new TreeNode(null, indexOf(examples[0][this.attributes - 1], labels[this.attributes - 1]));
        }
        // no more attributes, return node with majority class
        else if (attributes.length == 1) {
            return new TreeNode(null, majorityClass(examples));
        }
        // split by attribute, recurse on each subset
        else {
            int question = nextQuestion(examples, attributes);
            String[][][] subsets = splitByAttribute(examples, question);
            TreeNode[] children = new TreeNode[subsets.length];

            for (int i = 0; i < children.length; i++)
                children[i] = id3(subsets[i], attributes);

            return new TreeNode(children, question);
        }
    }

    /* Given a 2D array of examples and a 2D array of the remaining untested
     * attributes, returns the index to the row in the attribute matrix that
     * identifies the most significant attribute for splitting the examples. */
    private int nextQuestion(String[][] examples, String[][] attributes) {
        double maxInformationGain = Double.MIN_VALUE;
        int bestAttribute = 0;

        for (int attribute = 0; attribute < attributes.length; attribute++) {
            double informationGain = informationGain(examples, attribute);

            if (informationGain > maxInformationGain) {
                maxInformationGain = informationGain;
                bestAttribute = attribute;
            }
        }

        return bestAttribute;
    }

    /* Computes the information gain for splitting the dataset on the given
     * attribute. Used to select the next best attribute to split on in the
     * decision tree. */
    private double informationGain(String[][] examples, int attributeIndex) {
        String[][][] subsets = splitByAttribute(examples, attributeIndex);
        double sum = 0;

        for (String[][] subset : subsets)
            sum += (subset.length / examples.length) * entropy(subset);

        return entropy(examples) - sum;
    }

    /* Computes the entropy of the remaining dataset. */
    private double entropy(String[][] examples) {
        int[] frequencies = new int[classCount];
        double entropy = 0;

        for (String[] example : examples)
            frequencies[indexOf(example[attributes - 1], labels[attributes - 1])]++;

        for (int classFrequency : frequencies)
            entropy -= xlogx(classFrequency / classCount);

        return entropy;
    }

    private boolean perfectlyClassified(String[][] examples) {
        for (int i = 1; i < examples.length; i++) {
            if (!examples[i][attributes - 1].equals(examples[i - 1][attributes - 1])) return false;
        }
        return true;
    }

    private int majorityClass(String[][] examples) {
        int[] frequencies = new int[classCount];
        int majorityClass = 0;
        int highestFreq = Integer.MIN_VALUE;

        for (String[] example : examples)
            frequencies[indexOf(example[attributes - 1], labels[attributes - 1])]++;

        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] > highestFreq) {
                majorityClass = i;
                highestFreq = frequencies[majorityClass];
            }
        }

        return majorityClass;
    }

    private String[][][] splitByAttribute(String[][] examples, int attributeIndex) {
        String[][][] subsets = new String[labels[attributeIndex].length][][];

        for (int i = 0; i < subsets.length; i++) {
            int occurrences = 0;

            for (int j = 0; j < examples.length; j++)
                if (examples[j][attributeIndex].equals(labels[attributeIndex][i])) occurrences++;

            subsets[i] = new String[occurrences][];

            for (int j = 0, count = 0; j < examples.length; j++)
                if (examples[j][attributeIndex].equals(labels[attributeIndex][i])) {
                    subsets[i][count] = examples[j];
                    count++;
                }
        }

        return subsets;
    }

    /* HELPER: determines the index of a particular string in a given array. */
    private int indexOf(String string, String[] strings) {
        for (int i = 0; i < strings.length; i++)
            if (strings[i].equals(string)) return i;
        return -1;
    }

    /* HELPER: used to compute p(x) lg p(x) for entropy */
    private double xlogx(double x) {
        return x == 0 ? 0 : x * Math.log(x) / LOG2;
    }

    /* Given a 2-dimensional array containing the training data, numbers each
     * unique value that each attribute has, and stores these Strings in
     * instance variables; for example, for attribute 2, its first value
     * would be stored in labels[2][0], its second value in labels[2][1],
     * and so on; and the number of different values in stringCount[2]. **/
    private void indexStrings(String[][] inputData) {
        data = inputData;
        examples = data.length;
        attributes = data[0].length;
        stringCount = new int[attributes];
        labels = new String[attributes][examples];// might not need all columns

        int index = 0;
        for (int attr = 0; attr < attributes; attr++) {
            stringCount[attr] = 0;

            for (int ex = 1; ex < examples; ex++) {
                for (index = 0; index < stringCount[attr]; index++)
                    // seen this string before
                    if (data[ex][attr].equals(labels[attr][index])) break;

                // new string found
                if (index == stringCount[attr]) labels[attr][stringCount[attr]++] = data[ex][attr];
            }
        }

        classCount = stringCount[attributes - 1];
    }

    /* DEBUGGING: prints the list of attribute values for each attribute
     * and their index values. */
    private void printStrings() {
        for (int attr = 0; attr < attributes; attr++)
            for (int index = 0; index < stringCount[attr]; index++)
                System.out.println(data[0][attr] + " value " + index +
                        " = " + labels[attr][index]);
    }


    /* Reads a text file containing a fixed number of comma-separated values
     * on each line, and returns a two dimensional array of these values,
     * indexed by line number and position in line. */
    private static String[][] parseCSV(String fileName) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));

        // compute number of fields per line and number of lines
        String s = br.readLine();
        int fields = 1;
        int index = 0;
        int lines = 1;

        while ((index = s.indexOf(',', index) + 1) > 0) fields++;
        while (br.readLine() != null) lines++;
        br.close();

        // scan through file again to obtain data
        String[][] data = new String[lines][fields];
        Scanner sc = new Scanner(new File(fileName));
        sc.useDelimiter("[,\n]");

        for (int l = 0; l < lines; l++)
            for (int f = 0; f < fields; f++)
                if (sc.hasNext()) data[l][f] = sc.next();
                else error("Scan error in " + fileName + " at " + l + ":" + f);

        sc.close();
        return data;
    }

    /* HELPER: Print error message and exit. */
    private static void error(String msg) {
        System.err.println("Error: " + msg);
        System.exit(1);
    }


    /* Each node of the tree contains either the attribute number (for non-leaf
     * nodes) or class number (for leaf nodes) in <b>value</b>, and an array of
     * tree nodes in <b>children</b> containing each of the children of the
     * node (for non-leaf nodes).
     * The attribute number corresponds to the column number in the training
     * and test files. The children are ordered in the same order as the
     * Strings in labels[][]. E.g., if value == 3, then the array of
     * children correspond to the branches for attribute 3 (named data[0][3]):
     * children[0] is the branch for attribute 3 == labels[3][0]
     * children[1] is the branch for attribute 3 == labels[3][1]
     * children[2] is the branch for attribute 3 == labels[3][2]
     * etc.
     * The class number (leaf nodes) also corresponds to the order of classes
     * in labels[][]. For example, a leaf with value == 3 corresponds
     * to the class label labels[attributes-1][3]. */
    class TreeNode {
        TreeNode[] children;
        int value;


        public TreeNode(TreeNode[] ch, int val) {
            value = val;
            children = ch;
        }


        @Override
        public String toString() {
            return toString("");
        }

        String toString(String indent) {
            if (children != null) {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < children.length; i++) {
                    s.append(indent)
                            .append(data[0][value])
                            .append("=")
                            .append(labels[value][i])
                            .append("\n")
                            .append(children[i].toString(indent + '\t'));
                }
                return s.toString();
            }
            else {
                return indent + "Class: " + labels[attributes - 1][value] + "\n";
            }
        }

    }
}
