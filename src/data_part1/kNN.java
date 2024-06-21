package data_part1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class kNN {
    private static List<kNNAttributes> training = new ArrayList<>(); //training data
    private static List<kNNAttributes> test = new ArrayList<>(); //test data
    private static int k = 0; //k value for kNN

    //startnng class for kNN
    public static void startKNN(String trainFile, String testFile, String outputFile, int k) throws IOException {
        readData(trainFile, training);
        readData(testFile, test);
        
        performKNN(outputFile);
    }

    //populate list with knn attributes from file
    private static void readData(String file, List<kNNAttributes> data) throws FileNotFoundException {
        try {
            File f = new File(file);
            Scanner fileScaner = new Scanner(f); //create scanner for file
            String[] attributeNames = fileScaner.nextLine().split(","); //read first line for field names

            while (fileScaner.hasNextLine()) { //get data from next line
                String nextLine = fileScaner.nextLine(); //read next line
                Scanner lineScanner = new Scanner(nextLine); //create sc for new line
                lineScanner.useDelimiter(",");

                Map<String, Double> lineAttributes = new HashMap<>(); //attribute map for current line
                int fields = 0; //counter for fields

                while (lineScanner.hasNext()) {
                    double point = Double.parseDouble(lineScanner.next()); //parse double from string
                    lineAttributes.put(attributeNames[fields], point); //add attribute with value to attribute map
                    fields++;
                }

                data.add(new kNNAttributes(lineAttributes)); //add attribute map to list
                lineScanner.close(); //close line scanner
            }

            fileScaner.close(); //close file scanner

        } catch (FileNotFoundException e) {System.out.println("File not found");}
    }

    //predict class kNNAttribute instance using kNN
    private static double predictClass(kNNAttributes knnAttribute) {
        List<kNNAttributes> knnList = new ArrayList<>(k); //list of kNN with k capacity
        Map<Double, Integer> classCounter = new HashMap<>(); //map to count occurrences of each class

        for (kNNAttributes train : training) { //iterate through training data
            double euclideanDistance = knnAttribute.euclidean(train); //calculate euclidean distance between kNNAttributes

            int neighbor = 0;
            while (neighbor < knnList.size() && knnAttribute.euclidean(knnList.get(neighbor)) < euclideanDistance) { //find position of neighbor in the list
                neighbor++; //increment neighbor
            }
            knnList.add(neighbor, train); //add the neighbor to the list at position

            if (knnList.size() > k) { //list larger than k remove last neighbor
                knnList.remove(knnList.size() - 1);
            }
        }

        //count the occurrences of each class in the nearest neighbors
        for (kNNAttributes list : knnList) { //iterate through kNNList
            double neighborClass = list.attributes.get("class"); //get class neighbors in list
            classCounter.put(neighborClass, classCounter.getOrDefault(neighborClass, 0) + 1); //add class to map and increment count
        }

        double predictedClass = -1;
        int maxClassCount = 0;
        for (Double neighborClass : classCounter.keySet()) { //iterate through neighbor classes
            int classCount = classCounter.get(neighborClass); //get count of neighbor classes
            if (classCount > maxClassCount) { //if count is greater than max count
                predictedClass = neighborClass; //set predicted class to class
                maxClassCount = classCount;
            }
        }

        return predictedClass;
    }

    //test with testData and write to output file
    private static void performKNN(String outputFile)throws IOException {
        try (FileWriter wr = new FileWriter(outputFile)){
    
            double correctPrediction = 0; //correct predict counter
            for (kNNAttributes kNNTest : test) { //iterate through test data
                double predictClass = predictClass(kNNTest); //predict class for kNNTest
                double originalClass = kNNTest.attributes.get("class"); //actual class for kNNTest
    
                if (predictClass == originalClass){ //predictClass equal to originalClass increment correctP
                    correctPrediction++;
                }
                wr.write("Original: " + originalClass + ", Predicted: " + predictClass + ", "); //write classes to output file
    
                Map<Double, Double> distancesAndClasses = new HashMap<>(); //map to store distances and classes
                for (kNNAttributes kNNTrain : training) {  //iterate through training data
                    double euclideanDistance = kNNTest.euclidean(kNNTrain); //calculate euclidean of kNNTest and kNNTrain
                    double trainClasses = kNNTrain.attributes.get("class"); //get class of kNNTrain
                    distancesAndClasses.put(euclideanDistance, trainClasses); //add distance and class to map
                }
    
                List<Double> closestNeighborDistance = new ArrayList<>(distancesAndClasses.keySet()); //list of closest neighbors
                closestNeighborDistance.sort(Double::compare); //sort list of closest neighbors
    
                for (int i = 0; i < k; i++) { //iterate k closestneighbors
                    double distance = closestNeighborDistance.get(i); //get distance
                    double distanceClass = distancesAndClasses.get(distance); //get class of distance
                    wr.write(distance + ", (Class: " + distanceClass + "), "); //write distance and class to output file
                }
                
                wr.write("\n");
            }
    
            double accuracy = (correctPrediction / test.size()) * 100; //calculate accuracy
            wr.write("kNN Accuracy: " + accuracy + "%"); //write accuracy to output file
            System.out.println("Correct Prediction: " + correctPrediction + " out of " + test.size()); //print correct predictions to console
            System.out.println("kNN Accuracy: " + accuracy + "%"); //print accuracy to console
        }
    }  

    //main method
    public static void main(String[] args) throws IOException{
        if (args.length != 4) {
            System.out.println("CMD: java kNN trainFile testFile outputFile kValue");
            System.exit(1);
        }

        String trainFile = args[0];
        String testFile = args[1];
        String outputFile = args[2];
        k = Integer.parseInt(args[3]);

        startKNN(trainFile, testFile, outputFile, k);
    }

    //kNNAttributes class
    private static class kNNAttributes {
        private Map<String, Double> attributes; //attribute map for data

        //constructor
        public kNNAttributes(Map<String, Double> attribute) {
            this.attributes = attribute;
        }

        //calculate euclidean dist between kNNAttributes
        public double euclidean(kNNAttributes kValue) {
            double attributeDifference = 0; //sum of squared differences between attributes
        
            //iterate over the attributes of both instances simultaneously
            for(Map.Entry<String, Double> attributeEntry : attributes.entrySet()) {
                String attribute = attributeEntry.getKey();
                Double value = attributeEntry.getValue();
        
                //exclude the "class" attribute
                if (!attribute.equals("class")){
                    Double kValueAttribute = kValue.attributes.get(attribute);
                    if(kValueAttribute != null){ //ensure both instances have the same attribute
                        attributeDifference+= Math.pow(value - kValueAttribute, 2); //add squared difference to sum
                    }
                }
            }
            return Math.sqrt(attributeDifference); //return the square root of the sum
        }        
    }
}