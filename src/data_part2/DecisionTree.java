package data_part2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

public class DecisionTree {

    //start decision tree
    public static void startDT(String trainingFile, String outputFile) throws FileNotFoundException {
        List<Decisions> decisions = getFields(trainingFile);
        List<String> attributes = new ArrayList<>(decisions.get(0).fields.keySet());
        attributes.remove("class");

        Node n = calculateDT(decisions, attributes, null);

        try {
            FileWriter fWr = new FileWriter(outputFile); 
            double accuracy = calculateAccuracy(n); //calculate accuracy
            fWr.write("Decision Tree Accuracy: " + accuracy + "%\n\n"); //write accuracy to output
            generateDT(n, fWr, ""); //write to output
            fWr.close(); //close writer
            
            System.out.println("DT Output to: " + outputFile); 
            System.out.println("Decision Tree Accuracy: " + accuracy); //print accuracy
        } catch (IOException e) {
            //error in creating file
            System.out.println("Error in creating " + outputFile);
            System.exit(1);
        }
    }

    //get all fields from the file
    public static List<Decisions> getFields(String trainFile)throws FileNotFoundException {
        List<Decisions> allFields = new ArrayList<>(); //list to store fields
        Map<Integer, String> fieldIndexMap = new HashMap<>(); //store fieldindex to fieldname
        
        try {
            File f = new File(trainFile);
            Scanner fileScanner = new Scanner(f);  //read file
            if (fileScanner.hasNextLine()) { //check if file has at least one line
                String header = fileScanner.nextLine(); //get the header line
                String[] fields = header.split(","); //split the header line by comma
                
                //iterate through fields and add field key to field index to map
                for (int i = 0; i < fields.length; i++) {
                    fieldIndexMap.put(i, fields[i]);
                }
                
                while (fileScanner.hasNextLine()) { //read line by line
                    String nextLine = fileScanner.nextLine(); //get next line
                    String[] split = nextLine.split(","); //split line by comma
                    
                    Map<String, String> fieldToValue = new HashMap<>(); //store fields to values
                    for (int i = 0; i < split.length; i++) { //iterate through fields
                        String fieldName = fieldIndexMap.get(i); //get field name using field index
                        if (fieldName.equals("class")) { //if field is class
                            fieldToValue.put("class", split[i]); //store class with value
                        } else {
                            fieldToValue.put("att" + i, split[i]); //store attribute with value
                        }
                    }
                    allFields.add(new Decisions(fieldToValue)); //add decision to list
                }
            }
            fileScanner.close(); //close file scanner
        } catch (FileNotFoundException e) {System.out.println("File not found");}
        
        return allFields;
    }  

    //calculate entropy
    public static double calculateEntropy(List<Decisions> decisions) {
        Map<String, Integer> categoryToCount = new HashMap<>(); //store category to count 
        
        for (Decisions decision : decisions) { //iterate through decisions
            String category = decision.fields.get("class"); //get category of class
            int count = categoryToCount.getOrDefault(category, 0); //get count of category class
            categoryToCount.put(category, count + 1); //add to map and increment class count
        }
    
        double entropy = 0; 
        int decisionCount = decisions.size(); //total decisions in decision list
        for (int c : categoryToCount.values()) { //iterate through category map
            double probability = (double) c / decisionCount; //calculate probability
            entropy-= probability * Math.log(probability) / Math.log(2); //perform entropy calculation
        }
        return entropy;
    }
    
    //calculate information gain
    public static double calculateIG(Map<String, List<Decisions>> attributeToDecisions, List<Decisions> decisions) {
        double IG = calculateEntropy(decisions); //calculate entropy of decision records
        for (List<Decisions> d : attributeToDecisions.values()) { //iterate through attributetodecisions values
            IG -= ( (double) d.size() / decisions.size()) * calculateEntropy(d); //perform IG calculation
        }
        return IG;
    }

    //split decision by attribute
    public static Map<String, List<Decisions>> decisionToAttribute(List<Decisions> decision, String field) {
        Map<String, List<Decisions>> attribute = new HashMap<>(); //store attribute to decisions

        for (Decisions d : decision) { //iterate through decisions
            String att = d.fields.get(field); //get attribute of field

            //check if the key exists in the map
            if (!attribute.containsKey(att)) { //if attribute does not exist
                attribute.put(att, new ArrayList<>()); //create a new list and put it into the map
            }

            attribute.get(att).add(d); //add decision to attribute
        }
        return attribute;
    }

    //class count for a given node
    private static Map<String, Integer> classCount(Node n) {
        Map<String, Integer> classCount = new HashMap<>(); //store count of each class
        List<Decisions> decision = n.decisions; //list of node decisions

        for (Decisions d : decision) { //iterate through decisions
            String decisionClass = d.fields.get("class"); //get class of decision
            
            if (decisionClass != null) { //increment class count in map
                int count = classCount.getOrDefault(decisionClass, 0); //get count of class
                classCount.put(decisionClass, count + 1); //add class to map and increment count
            }
        }

        Set<String> allClasses = new HashSet<>(); //set of all classes
        allClasses.add("0"); //add class 0
        allClasses.add("1"); //add class 1
        allClasses.removeAll(classCount.keySet()); //remove all classes from class count

        for (String classNumber : allClasses) {
            classCount.put(classNumber, 0); //add class to class count
        }
        return classCount;
    }

    //generate decision tree
    public static Node calculateDT(List<Decisions> decisions, List<String> attribute, String parentC) {
        String attIG = "null"; 
        double IG = Integer.MIN_VALUE;
        List<Node> childNodes = null; //list of child nodes

        for (String att : attribute) { //iterate through attributes
            Map<String, List<Decisions>> attCategory = decisionToAttribute(decisions, att); //split by attribute

            double calculatedIG = calculateIG(attCategory, decisions); //calculate information gain
            if (calculatedIG > IG) { 
                IG = calculatedIG; //update IG
                attIG = att; //update attribute
            }
        }

        Map<String, List<Decisions>> splitByIG = decisionToAttribute(decisions, attIG); //split by most information gain
        
        if (IG >= 0.00001) { //if IG is greater than 0.00001 
            childNodes = new ArrayList<>(); //initialize child nodes list 
            
            for (String c : splitByIG.keySet()) { //iterate through split IG
                List<String> checkAttribute = new ArrayList<>(attribute); //check attribute list
                checkAttribute.remove(attIG); //remote attribute with most IG
                
                Node n = calculateDT(splitByIG.get(c), checkAttribute, c); //generate decision tree
                childNodes.add(n); //add node to child nodes
            }
        }
        //return new node
        return new Node(attIG, parentC, IG, calculateEntropy(decisions), childNodes, decisions);
    }

    //write node to file
    public static void generateDT(Node node, FileWriter fWr, String indent) throws IOException {
        if (node.child == null) { //if child is null and exclude class
            fWr.write(indent + "leaf {"); //start leaf
            boolean first = true;
            Map<String, Integer> classCounter = classCount(node); //get class count
            for (String classNumber : classCounter.keySet()) { //iterate through class counter
                if(first){
                    fWr.write(classNumber + ": " + classCounter.get(classNumber) + ", "); //write class with count
                    first = false;
                } else {
                    fWr.write(classNumber + ": " + classCounter.get(classNumber)); //write class with count
                }
            }
    
            fWr.write("}\n"); //close leaf
            return;
        }
    
        String split = node.split; //get split
        fWr.write(indent + split + " (IG: " + node.IG + ", Entropy: " + node.E + ")\n"); //print IG and entropy of node
    
        for (Node child : node.child) { //loop through each child node
            fWr.write(indent + "-- " + split + " == " + child.category + " --\n"); //write split and category
            generateDT(child, fWr, indent + "    "); //increase indent for each child node
        }
    }    

    //predict the majority class for a given node
    private static String predictMajorityClass(Node node) {
        Map<String, Integer> classCount = classCount(node); //get class counts for the node
        String majorityClass = "";
        int maxCount = Integer.MIN_VALUE;

        for (Map.Entry<String, Integer> classEntry : classCount.entrySet()) { //iterate through class counts
            if (classEntry.getValue() > maxCount) { //if classCount is greater than max count
                maxCount = classEntry.getValue(); //update max count
                majorityClass = classEntry.getKey(); //update majority class
            }
        }
        return majorityClass;
    }

    //calculate accuracy of the decision tree
    private static int calculateAccuracy(Node node) {
        int totalDecisions = 0;
        int correctPredictions = 0;

        Queue<Node> queue = new LinkedList<>(); //queue to store nodes
        queue.add(node); //add root node to queue
        while (!queue.isEmpty()) {
            Node current = queue.poll(); //get node from queue
            
            if (current.child == null) { //if child is null
                totalDecisions += current.decisions.size(); //increment total decisions
                String majorityClass = predictMajorityClass(current); //predict majority class
                
                for (Decisions d : current.decisions) { //iterate through decisions
                    if (d.fields.get("class").equals(majorityClass)) { //if class is equal to majority class
                        correctPredictions++; //increment correct predictions
                    } else {
                        System.out.println("Low accuracy calculation. Class not majority class");
                    }
                }
            } else {
                queue.addAll(current.child); //else add all child nodes to queue
            }
        }

        return correctPredictions / totalDecisions * 100; //return accuracy
    } 

    //main method
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("CMD: java DecisionTree trainingFile outputFile");
            System.exit(1);
        }

        String trainingFile = args[0];
        String outputFile = args[1];

        startDT(trainingFile, outputFile);
    }
    
    //node class to store the decision tree
    static class Node {
        String split; //attribute to split on
        String category; //category of the split
        double IG; //information gain 
        double E; //entropy
        List<Node> child; //child nodes
        List<Decisions> decisions; //decisions at the node
        Map<String, Integer> predictedClassCounts; //predicted class counts

        //constructor 
        public Node(String split, String category, double informationGain, double entropy, List<Node> child, List<Decisions> decisions) {
            this.split = split;
            this.category = category;
            this.IG = informationGain;
            this.E = entropy;
            this.child = child;
            this.decisions = decisions;
            this.predictedClassCounts = new HashMap<>();
        }
    }

    //class to store the decision records
    static class Decisions {
        Map<String, String> fields; //fields of decision 

        //constructor
        public Decisions(Map<String, String> fields) {
            this.fields = fields;
        }
    }
}