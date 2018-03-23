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
    static final double LOG2 = Math.log(2.0);
    private int attributes;             // attribute number (including class)
    private int examples;               // Number of training examples
    private TreeNode decisionTree;      // Tree learnt in training
    private String[][] data;            // data indexed as [example, attribute]
    private String[][] labels;          // Unique labels for each attribute
    private int[] stringCount;          // Number of unique labels


    /**
     * Classifier constructor.
     */
    public ID3() {
        attributes = 0;
        examples = 0;
        decisionTree = null;
        data = null;
        labels = null;
        stringCount = null;
    }


    /**
     * Application entry point for classifier testing.
     *
     * @param args file names for training and test data
     * @throws FileNotFoundException if file not found
     * @throws IOException           generic IO exception
     */
    public static void main(String[] args)
            throws FileNotFoundException, IOException {
        if (args.length != 2)
            error("Expected 2 arguments: file names of training and test data");

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

        // after calling indexStrings(), trainingData[][] is converted to a
        // Dataset object which abstracts array manipulations in order to make
        // the code directly implementing ID3 more legible
        decisionTree = id3(
                new Dataset(data, labels),
                IntStream.range(0, labels.length - 1)
                        .boxed()
                        .collect(Collectors.toList())
        );
    }

    /**
     * Output a classification result for each data point in the testData[][]
     * matrix.
     *
     * @param testData 2D array of data points, indexed as [example][attribute]
     */
    public void classify(String[][] testData) {
        if (decisionTree == null)
            error("Please run training phase before classification");

        String[] classes = labels[attributes - 1];
        for (int i = 1; i < testData.length; i++)
            System.out.println(classes[classify(testData[i], decisionTree)]);
    }

    /**
     * Prints the learned decision tree to standard output.
     */
    public void printTree() {
        if (decisionTree == null) error("Attempted to print null Tree");
        else System.out.println(decisionTree);
    }


    /**
     * Recursively computes a class for the given example by traversing the
     * decision tree.
     *
     * @param example data point to classify
     * @param node    node from which to classify, pass root for initial call
     * @return class of data point
     */
    private int classify(String[] example, TreeNode node) {
        // leaf node
        if (node.children == null || node.children.length == 0)
            return node.value;
        // inner node
        else
            return classify(
                    example,
                    node.children[indexOf(example[node.value], labels[node.value])]
            );
    }

    /**
     * Executes the iterative dichotomizer 3 algorithm on the training dataset.
     * Returns root node of the resulting decision tree. The attributeIndices
     * parameter is used to indicate which attributes are still to be tested at
     * the current sub-tree.
     * <p>
     * That is, for example, if the data set originally had
     * 4 distinct attributes, and attribute 3 has been used, the argument
     * to attributeIndices should be [0, 1, 2].
     *
     * @param dataset          data points to elicit nodes for
     * @param attributeIndices remaining attributes for splitting
     * @return TreeNode representing decision tree
     */
    private TreeNode id3(Dataset dataset, List<Integer> attributeIndices) {
        // all examples have same class
        if (dataset.isPerfectlyClassified()) {
            return new TreeNode(null, dataset.firstClass());
        }
        // no more attributes to split by
        else if (attributeIndices.isEmpty()) {
            return new TreeNode(null, dataset.majorityClass());
        }
        // else split by best attribute and handle subsets (including empty)
        else {
            int question = nextQuestion(dataset, attributeIndices);

            List<Dataset> subsets = dataset.splitByAttribute(question);
            attributeIndices = new ArrayList<>(attributeIndices);
            attributeIndices.remove((Integer) question);

            // empty subset -> make leaf node with current majority class
            // else recurse
            TreeNode[] children = new TreeNode[subsets.size()];

            for (int i = 0; i < children.length; i++)
                children[i] = subsets.get(i).isEmpty() ?
                        new TreeNode(null, dataset.majorityClass()) :
                        id3(subsets.get(i), attributeIndices);

            return new TreeNode(children, question);
        }
    }

    /**
     * Given a dataset of examples and a list of the remaining untested
     * attributes, returns the index to the row in the attribute matrix that
     * identifies the attribute with the highest information gain.
     *
     * @param dataset          remaining data points
     * @param attributeIndices remaining attributes to test
     * @return index of attribute with highest information gain
     */
    private int nextQuestion(Dataset dataset, List<Integer> attributeIndices) {
        double maxInformationGain = Double.MIN_VALUE;
        int bestAttribute = attributeIndices.get(0);

        // compute information gain for splitting dataset using each attribute
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
     *
     * @param data           dataset to split
     * @param attributeIndex attribute to split with
     * @return information gained by splitting with this attribute
     */
    private double informationGain(Dataset data, int attributeIndex) {
        List<Dataset> subsets = data.splitByAttribute(attributeIndex);
        double subsetsEntropy = 0;

        // sum of entropy of each subset
        for (Dataset subset : subsets)
            subsetsEntropy +=
                    ((double) subset.size() / (double) data.size())
                    * subset.entropy();

        // overall change in entropy from splitting on this attribute
        return data.entropy() - subsetsEntropy;
    }

    /**
     * HELPER: determines the index of a particular string in a given array.
     * Adheres to the same contract as the Java standard library method
     * list.indexOf(Object o), but works over String arrays.
     */
    private int indexOf(String string, String[] strings) {
        for (int i = 0; i < strings.length; i++)
            if (strings[i].equals(string)) return i;
        return -1;
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
                if (index == stringCount[attr])
                    labels[attr][stringCount[attr]++] = data[ex][attr];
            }
        }
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
    private static String[][] parseCSV(String fileName)
            throws FileNotFoundException, IOException {
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
            } else {
                return indent + "Class: " + labels[attributes - 1][value] + "\n";
            }
        }
    }


    /**
     * The Dataset class partially abstracts the details of classes, attributes,
     * examples, and their handling during the training phase. Its purpose is
     * to make the implementation of the ID3 algorithm more legible.
     * <p>
     * A Dataset represents a collection of example data points, and a set of
     * attributes and classes defining the space within which those data points
     * reside. A Dataset object provides methods for querying the dataset's
     * properties and manipulating it. In particular, the computation of a
     * dataset's entropy, as well as the splitting of a dataset into subsets
     * based on some attribute are implemented in this class.
     */
    private class Dataset {
        private List<String> classes;
        private List<List<String>> attributes;
        private List<Example> examples;


        /**
         * Constructs a Dataset from trainingData[][] and labels[][] matrices,
         * where both matrices are formatted as produced by the method
         * indexStrings().
         *
         * @param trainingData examples
         * @param labels       attribute and class labels
         */
        Dataset(String[][] trainingData, String[][] labels) {
            // extract examples from trainingData[][]
            // first row skipped as it contains column names
            examples = new ArrayList<>();
            for (int i = 1; i < trainingData.length; i++) {
                String[] example = trainingData[i];
                examples.add(new Example(example));
            }

            // extract attributes from labels[][] matrix
            attributes = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                // class labels
                if (i == labels.length - 1)
                    classes = new ArrayList<>(Arrays.asList(labels[i]));
                    // attribute labels
                else attributes.add(new ArrayList<>(Arrays.asList(labels[i])));
            }

            // remove nulls (the labels[][] matrix usually contains null cells)
            classes.removeAll(Collections.singleton(null));
            attributes.forEach(attribute
                    -> attribute.removeAll(Collections.singleton(null)));
        }

        /**
         * Private constructor for internal use.
         *
         * @param classes    list of class labels
         * @param attributes list of attribute labels
         * @param examples   data points
         */
        private Dataset(List<String> classes, List<List<String>> attributes,
                        List<Example> examples) {
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
         * Returns new datasets obtained by splitting the current dataset
         * by its values for the given attribute.
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

        /**
         * Returns the class index for the class of the first data point in the
         * dataset. Useful when dataset is perfectly classified and it is
         * unnecessary to iterate again over entire dataset to determine
         * majority class.
         *
         * @return class index for first data point's class
         */
        int firstClass() {
            return classes.indexOf(examples.get(0).cls());
        }

        @Override
        public String toString() {
            return "\nDATASET ("
                    + size() + " examples, "
                    + classesSize() + "classes, "
                    + attributesSize() + " attributes):\n"
                    + "Classes: " + classes.toString() + "\n"
                    + "Attributes: " + attributes.toString() + "\n"
                    + "Examples: " + examples.toString() + "\n"
                    + "=======================================================";
        }

        /**
         * HELPER: used to compute p(x) lg p(x) for entropy
         */
        private double xlogx(double x) {
            return x == 0 ? 0 : x * Math.log(x) / LOG2;
        }


        private class Example {
            String[] attributes;
            String exampleClass;


            Example(String[] data) {
                exampleClass = data[data.length - 1];
                attributes = new String[data.length - 1];
                System.arraycopy(data, 0, attributes, 0, data.length - 1);
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
