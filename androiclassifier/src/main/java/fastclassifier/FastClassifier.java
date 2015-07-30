/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fastclassifier;


import fastclassifier.dataTypes.DataBoard;
import fastclassifier.dataTypes.FeatureDefinition;
import fastclassifier.dataTypes.SegmentedClassification;
import ace.datatypes.TrainedModel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import fastclassifier.options.Parameters;
import weka.core.Instances;

/**
 *
 * @author Bruno
 */
public class FastClassifier {


    /**
     * @param args the command line arguments
     */
  public static void main(String[] args) {
        /*Controller control = Controller.getInstance();
        
        double[] samples = control.readTxtFile("wolfcrab.txt");
        System.out.println(samples.length);
        
        final long startTime = System.currentTimeMillis();
        double[][][] features = null;
        try {
            features = control.ExtractFeatures(samples, null);
        } catch (Exception ex) {
            Logger.getLogger(FastClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //first indice is the aggregator, the second the feature
        double[][] overallFeatures = control.getOverallFeatures(features);
        
        
        System.out.println(features[1][0].length);
        System.out.println(overallFeatures.length);
        //printing mfcc dimension 2
        System.out.println(overallFeatures[0].length);  
        
        //GOT THE FEATURES, CLASSIFY NOW BITCH.
        createDataBoard(overallFeatures);
        createInstances();
        try {
            createTrainedModel("assobio-teste-mfcc.model");
        } catch (IOException ex) {
            Logger.getLogger(FastClassifier.class.getName()).log(Level.SEVERE, null, ex);
        } 
        classify();
        final long endTime = System.currentTimeMillis();
        System.out.println(resulting_classifications[0].classifications.length);
        System.out.println(resulting_classifications[0].classifications[0]);
        System.out.println("Total execution time: " + (endTime - startTime) + "milliseconds" );
        
    */}


    /**
     * categorizes audiofile according to choosen classifier
     *
     * @param samples the samples representing the audio to categorize
     * @return
     */
    public static String classifyAudio(double[]samples) throws Exception{
        Controller control = Controller.getInstance();

        double[][][] features = control.ExtractFeatures(samples, null);
        control.calcOverallFeatures(features);
        //the above method is required to run the below 2
        double[][] overallFeatures = control.getOverallFeatureValues();
        FeatureDefinition[] defs = control.getOverallFeatureDefinitions();
        //creates a databoard
        DataBoard db = control.createDataBoard(overallFeatures, defs);
        Instances inst = control.createInstances(db);
        TrainedModel model = null;
        try {
            model = control.createTrainedModel(Parameters.getClassifierPath());
        } catch (IOException ex) {
            Logger.getLogger(FastClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }

        SegmentedClassification[] classifications = control.classify(model,db,inst);

        //return only first classification
        return classifications[0].classifications[0];

    }

    public static String classifyAudio(double[]samples,TrainedModel model) throws Exception{
        Controller control = Controller.getInstance();

        double[][][] features = control.ExtractFeatures(samples, null);
        control.calcOverallFeatures(features);
        //the above method is required to run the below 2
        double[][] overallFeatures = control.getOverallFeatureValues();
        FeatureDefinition[] defs = control.getOverallFeatureDefinitions();
        //creates a databoard
        DataBoard db = control.createDataBoard(overallFeatures, defs);
        Instances inst = control.createInstances(db);

        SegmentedClassification[] classifications = control.classify(model,db,inst);

        //return only first classification
        return classifications[0].classifications[0];

    }

    
    
}
