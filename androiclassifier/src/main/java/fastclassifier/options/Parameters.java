/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fastclassifier.options;


import fastclassifier.featureExtraction.Aggregators.Aggregator;
import fastclassifier.featureExtraction.Aggregators.Mean;
import fastclassifier.featureExtraction.Aggregators.StandardDeviation;
import fastclassifier.featureExtraction.audioFeatures.AreaMoments;
import fastclassifier.featureExtraction.audioFeatures.BeatHistogram;
import fastclassifier.featureExtraction.audioFeatures.BeatHistogramLabels;
import fastclassifier.featureExtraction.audioFeatures.BeatSum;
import fastclassifier.featureExtraction.audioFeatures.Compactness;
import fastclassifier.featureExtraction.audioFeatures.FFTBinFrequencies;
import fastclassifier.featureExtraction.audioFeatures.FeatureExtractor;
import fastclassifier.featureExtraction.audioFeatures.FractionOfLowEnergyWindows;
import fastclassifier.featureExtraction.audioFeatures.LPC;
import fastclassifier.featureExtraction.audioFeatures.MFCC;
import fastclassifier.featureExtraction.audioFeatures.MagnitudeSpectrum;
import fastclassifier.featureExtraction.audioFeatures.Moments;
import fastclassifier.featureExtraction.audioFeatures.PowerSpectrum;
import fastclassifier.featureExtraction.audioFeatures.RMS;
import fastclassifier.featureExtraction.audioFeatures.RelativeDifferenceFunction;
import fastclassifier.featureExtraction.audioFeatures.SpectralCentroid;
import fastclassifier.featureExtraction.audioFeatures.SpectralFlux;
import fastclassifier.featureExtraction.audioFeatures.SpectralRolloffPoint;
import fastclassifier.featureExtraction.audioFeatures.SpectralVariability;
import fastclassifier.featureExtraction.audioFeatures.StrengthOfStrongestBeat;
import fastclassifier.featureExtraction.audioFeatures.StrongestBeat;
import fastclassifier.featureExtraction.audioFeatures.StrongestFrequencyViaFFTMax;
import fastclassifier.featureExtraction.audioFeatures.StrongestFrequencyViaSpectralCentroid;
import fastclassifier.featureExtraction.audioFeatures.StrongestFrequencyViaZeroCrossings;
import fastclassifier.featureExtraction.audioFeatures.ZeroCrossings;

/**
 *
 * @author Bruno
 */
public class Parameters {
    
    //variables used internally
    private static FeatureExtractor[] features;
    private static boolean[] definitions;
    
    //jaudio parameters
    /**
     * The size of the windows the audio will be divided into
     */
    public static int windowSize = 512;
    /**
     * Whether or not windows should overlap
     */
    public static double windowOverlap = 0;
    /**
     * Sampling rate the audios will be resampled to
     */
    public static double samplingRate = 16000;
    
    //TODO: normalise as different function
    /**
     * Normalise samples?
     */
    public static boolean normalise = true;
    /**
     * Get features for every window?
     */
    public static boolean perWindowStats = false;
    /**
     * Get features for all windows?
     */
    public static boolean overallStats = true;
    
    
    /**
     * returns aggregators that will be used create 
     * Overall recording features. 
     * @return array of aggregators
     */
    public static Aggregator[] getAggList(){
        Aggregator[] aggList = new Aggregator[2];
        aggList[0] = new StandardDeviation();
        aggList[1] = new Mean();
        
        return aggList;
    }
    
    /**
     * returns the path to the .model file used
     * for classification.
     * @return 
     */
    public static String getClassifierPath(){
        return "sopro-m-l-am.model";
    }


    /**
     * substitute for getTaxonomy. It should return all the possible classes
     * for the current clasifier. In the same order they were created
     */
    public static String[] getClassNames(){
        String[] classnames = {"yes","no"};
        return classnames;
    }


    /**
     * Set which features you want to extract by selecting true or false.
     * @return set of features supported by the classifier
     */
    public static FeatureExtractor[] getFeatures(){
        //resets variables
        features = new FeatureExtractor[24];
        definitions = new boolean[24];
        ArrayIndex = 0;
        
                addFeature(new MagnitudeSpectrum(),false);
                addFeature(new PowerSpectrum(),false);
                addFeature(new FFTBinFrequencies(),false);
                addFeature(new SpectralCentroid(),false);
                addFeature(new SpectralRolloffPoint(),false);
                addFeature(new SpectralFlux(),false);
                addFeature(new Compactness(),false);
                addFeature(new SpectralVariability(),false);
                addFeature(new RMS(),false);                
                addFeature(new FractionOfLowEnergyWindows(),false);
                addFeature(new ZeroCrossings(),false);
                addFeature(new BeatHistogram(),false);
                addFeature(new BeatHistogramLabels(),false);
                addFeature(new StrongestBeat(),false);
                addFeature(new BeatSum(),false);
                addFeature(new StrengthOfStrongestBeat(),false);   
                addFeature(new StrongestFrequencyViaZeroCrossings(),false);
                addFeature(new StrongestFrequencyViaSpectralCentroid(),false);
                addFeature(new StrongestFrequencyViaFFTMax(),false);
                addFeature(new MFCC(),true);
                addFeature(new LPC(),true);
                addFeature(new Moments(),false);
                addFeature(new RelativeDifferenceFunction(),false);                  
                addFeature(new AreaMoments(),true);                
               
                //these features are not implemented in FastClassifier
		// extractors.add(new HarmonicSpectralCentroid());
		// def.add(false);
		// extractors.add(new HarmonicSpectralFlux());
		// def.add(false);
		// extractors.add(new HarmonicSpectralSmoothness());
		// def.add(false);
		// extractors.add(new PeakFinder());
		// def.add(false);
		// extractors.add(new StrongestFrequencyVariability());
		// def.add(false);
                
                return features;
    }
    
    /**
     * Method that return the definitions created on getFeatures. 
     * it is not necessary to edit this function
     * @return an array of booleans that indicates which features are going to be extracted
     */
    public static boolean[] getDefinitions(){
        return definitions;
    }
    
    //private method used to simplify the addition of features to arrays.
    private static int ArrayIndex = 0;
    private static void addFeature(FeatureExtractor feat,boolean extract){
        
        features[ArrayIndex] = feat;
        definitions[ArrayIndex] = extract;
        ArrayIndex++;
        
    }
    
}
