// ECS629/759 Assignment 2 - ID3 Skeleton Code
// Author: Simon Dixon

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


class ID3 {
    // TODO: 3/4/2018 refactor data structures to allow simplified algorithms
    static final double LOG2 = Math.log(2.0);
    private int attributes;             // Number of attributes (including the class)
    private int examples;               // Number of training examples
    private TreeNode decisionTree;      // Tree learnt in training, used for classifying
    private String[][] data;            // Training data indexed by example, attribute
    private String[][] labels;          // Unique labels for each attribute
    private int[] stringCount;          // Number of unique labels for each attribute

    // temporary
    String[] lastClassification;
    int classCount;

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

        // initiate ID3 with list of attribute indices that includes all indices
        decisionTree = id3(
                new Dataset(data, labels),
                IntStream.range(0, labels.length - 1).boxed().collect(Collectors.toList())
        );
    }

    /**
     * Execute the decision tree on the given examples in testData, and print
     * the resulting class names, one to a line, for each example in testData.
     **/
    public void classify(String[][] testData) {
        if (decisionTree == null) error("Please run training phase before classification");

        for (int i = 1; i < testData.length; i++) {
            String[] example = testData[i];
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


    /**
     * Recursively computes a class for the given example by traversing the decision
     * tree.
     *
     * @param example data point to classify
     * @param node    node from which to classify, should be root for initial call
     * @return class of data point
     */
    private int classify(String[] example, TreeNode node) {
        // leaf node
        if (node.children == null || node.children.length == 0) return node.value;
        // non-leaf node
        else return classify(example, node.children[indexOf(example[node.value], labels[node.value])]);
    }

    /**
     * Executes the iterative dichotomizer 3 algorithm on the training dataset.
     * Returns the root node of the resulting decision tree.
     */
    private TreeNode id3(Dataset dataset, List<Integer> attributeIndices) {
        // all examples have same class OR no more attributes to split on
        if (dataset.isPerfectlyClassified() || attributeIndices.isEmpty()) {
            return new TreeNode(null, dataset.majorityClass());
        }
        // else split by best attribute and handle subsets
        else {
            int question = nextQuestion(dataset, attributeIndices);

            List<Dataset> subsets = dataset.splitByAttribute(question);

            attributeIndices = new ArrayList<>(attributeIndices);
            attributeIndices.remove((Integer) question);

            // if a subset is empty make leaf node with current majority class, else recurse
            TreeNode[] children = new TreeNode[subsets.size()];

            for (int i = 0; i < children.length; i++)
                children[i] = subsets.get(i).isEmpty() ?
                        new TreeNode(null, dataset.majorityClass()) :
                        id3(subsets.get(i), attributeIndices);

            return new TreeNode(children, question);
        }
    }

    /**
     * Given a 2D array of examples and a 2D array of the remaining untested
     * attributes, returns the index to the row in the attribute matrix that
     * identifies the most significant attribute for splitting the examples.
     */
    private int nextQuestion(Dataset dataset, List<Integer> attributeIndices) {
        double maxInformationGain = Double.MIN_VALUE;
        int bestAttribute = attributeIndices.get(0);

        for (Integer att : attributeIndices) {
            double informationGain = informationGain(dataset, att);

            if (informationGain > maxInformationGain) {
                maxInformationGain = informationGain;
                bestAttribute = att;
            }
        }

        return bestAttribute;
    }

    /**
     * Computes the information gain for splitting the dataset on the given
     * attribute. Used to select the next best attribute to split on in the
     * decision tree.
     */
    private double informationGain(Dataset data, int attributeIndex) {
        List<Dataset> subsets = data.splitByAttribute(attributeIndex);

        double subsetsEntropy = 0;

        for (Dataset subset : subsets)
            subsetsEntropy += ((double) subset.size() / (double) data.size()) * subset.entropy();

        // change in entropy from splitting on this attribute
        return data.entropy() - subsetsEntropy;
    }

    /**
     * HELPER: determines the index of a particular string in a given array.
     */
    private int indexOf(String string, String[] strings) {
        for (int i = 0; i < strings.length; i++)
            if (strings[i].equals(string)) return i;
        return -1;
    }

    /**
     * HELPER: used to compute p(x) lg p(x) for entropy
     */
    private double xlogx(double x) {
        return x == 0 ? 0 : x * Math.log(x) / LOG2;
    }

    /**
     * Given a 2-dimensional array containing the training data, numbers each
     * unique value that each attribute has, and stores these Strings in
     * instance variables; for example, for attribute 2, its first value
     * would be stored in labels[2][0], its second value in labels[2][1],
     * and so on; and the number of different values in stringCount[2].
     **/
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

    /**
     * DEBUGGING: prints the list of attribute values for each attribute
     * and their index values.
     */
    private void printStrings() {
        for (int attr = 0; attr < attributes; attr++)
            for (int index = 0; index < stringCount[attr]; index++)
                System.out.println(data[0][attr] + " value " + index +
                        " = " + labels[attr][index]);
    }

    /**
     * Reads a text file containing a fixed number of comma-separated values
     * on each line, and returns a two dimensional array of these values,
     * indexed by line number and position in line.
     */
    // todo change back to private
    public static String[][] parseCSV(String fileName) throws FileNotFoundException, IOException {
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

    /**
     * HELPER: Print error message and exit.
     */
    private static void error(String msg) {
        System.err.println("Error: " + msg);
        System.exit(1);
    }


    /**
     * Each node of the tree contains either the attribute number (for non-leaf
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
     * to the class label labels[attributes-1][3].
     */
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


    /**
     * The Dataset class partially abstracts the details of classes, attributes, examples,
     * and their handling during the training phase. Its purpose is to make the implementation
     * of the ID3 algorithm more legible.
     * <p>
     * A Dataset represents a collection of example data points, and a set of attributes
     * and classes defining the space within which those data points reside. A Dataset object
     * provides methods for querying the dataset's properties and manipulating it. In particular,
     * the computation of a dataset's entropy, as well as the splitting of a dataset into
     * subsets based on some attribute are implemented in this class.
     */
    private class Dataset {
        private List<String> classes;
        private List<List<String>> attributes;
        private List<Example> examples;

        Dataset(String[][] trainingData, String[][] labels) {
            examples = new ArrayList<>();
            for (int i = 1; i < trainingData.length; i++) {
                String[] example = trainingData[i];
                examples.add(new Example(example));
            }

            attributes = new ArrayList<>();

            for (int i = 0; i < labels.length; i++) {
                // class labels
                if (i == labels.length - 1) classes = new ArrayList<>(Arrays.asList(labels[i]));
                    // attribute labels
                else attributes.add(new ArrayList<>(Arrays.asList(labels[i])));
            }

            // remove nulls (the labels[][] matrix usually contains null cells)
            classes.removeAll(Collections.singleton(null));
            attributes.forEach(attribute -> attribute.removeAll(Collections.singleton(null)));
        }

        private Dataset(List<String> classes, List<List<String>> attributes, List<Example> examples) {
            this.classes = classes;
            this.attributes = attributes;
            this.examples = examples;
        }


        /**
         * Returns the size of the data set.
         *
         * @return number of examples in data set
         */
        int size() {
            return examples.size();
        }

        /**
         * Returns true if the data set contains no examples.
         *
         * @return true if empty, false otherwise
         */
        boolean isEmpty() {
            return examples.size() == 0;
        }

        /**
         * Returns the number of attributes in the data set.
         *
         * @return number of attributes in data set
         */
        int attributesSize() {
            return attributes.size();
        }

        /**
         * Returns the number of classes in this dataset
         *
         * @return number of classes
         */
        int classesSize() {
            return classes.size();
        }

        /**
         * Returns new datasets obtained by splitting the current dataset by its values
         * for the given attribute.
         *
         * @param attributeIndex the attribute to use for splitting
         * @return list of new datasets
         */
        List<Dataset> splitByAttribute(int attributeIndex) {
            List<String> values = attributes.get(attributeIndex);
            List<List<Example>> subsets = new ArrayList<>();

            for (int i = 0; i < values.size(); i++)
                subsets.add(new ArrayList<>());

            // split into subsets
            for (Example ex : examples)
                subsets.get(values.indexOf(ex.attribute(attributeIndex))).add(ex);

            // create new datasets from subset lists
            Dataset[] result = new Dataset[subsets.size()];
            for (int i = 0; i < subsets.size(); i++)
                result[i] = new Dataset(classes, attributes, subsets.get(i));

            return new ArrayList<>(Arrays.asList(result));
        }

        /**
         * Returns the entropy of the dataset.
         */
        double entropy() {
            // entropy of empty dataset is trivially zero
            if (isEmpty()) return 0;

            double entropy = 0;
            int[] frequencies = new int[classes.size()];

            // compute frequencies
            for (Example ex : examples)
                frequencies[classes.indexOf(ex.cls())]++;

            // compute entropy
            for (int freq : frequencies)
                entropy -= xlogx((double) freq / (double) size());

            return entropy;
        }

        /**
         * Returns true if the dataset only contains examples of a single class.
         *
         * @return true if all examples have same class, false otherwise
         */
        boolean isPerfectlyClassified() {
            Set<String> classes = new HashSet<>();

            // empty dataset is trivially perfectly classified
            if (examples.size() == 0) return true;

            classes.add(examples.get(0).cls());
            // iterate over examples and find non-duplicates
            for (Example ex : examples)
                if (classes.add(ex.cls()))
                    return false;

            // if only duplicates found
            return true;
        }

        /**
         * Return the class index with the highest frequency in the dataset. If
         * there are multiple classes with equal frequency, returns the first one
         * in the list.
         *
         * @return class with highest frequency in the dataset
         */
        int majorityClass() {
            int[] frequencies = new int[classes.size()];
            int majorityClass = 0;

            // compute frequencies
            for (Example ex : examples)
                frequencies[classes.indexOf(ex.cls())]++;

            // find most frequent class
            int highestSoFar = 0;
            for (int i = 0; i < frequencies.length; i++) {
                if (frequencies[i] > highestSoFar) {
                    majorityClass = i;
                    highestSoFar = frequencies[i];
                }
            }

            return majorityClass;
        }

        @Override
        public String toString() {
            return "\nDATASET (" + size() + " examples, " + classesSize() + "classes, "
                    + attributesSize() + " attributes):\n"
                    + "Classes: " + classes.toString() + "\n"
                    + "Attributes: " + attributes.toString() + "\n"
                    + "Examples: " + examples.toString() + "\n"
                    + "==============================================================";
        }

        private class Example {
            String[] attributes;
            String exampleClass;


            Example(String[] data) {
                exampleClass = data[data.length - 1];
                attributes = new String[data.length - 1];
                System.arraycopy(data, 0, attributes, 0, data.length - 1);
            }

            Example(String exampleClass, String[] attributes) {
                this.exampleClass = exampleClass;
                this.attributes = attributes;
            }

            String cls() {
                return exampleClass;
            }

            String attribute(int index) {
                return attributes[index];
            }

            @Override
            public String toString() {
                return "Ex: {" + Arrays.asList(attributes) + "}";
            }
        }
    }
}
