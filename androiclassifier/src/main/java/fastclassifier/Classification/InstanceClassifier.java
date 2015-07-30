/*
 * InstanceClassifier.java
 * Version 2.2
 *
 * Last modified on April 11, 2010.
 * McGill University
 */

package fastclassifier.Classification;

import fastclassifier.dataTypes.DataBoard;
import ace.datatypes.TrainedModel;
import fastclassifier.dataTypes.SegmentedClassification;
import weka.core.*;


/**
 * Classifies a set of Weka Instances using a trained Weka Classifier of a TrainedModel object.
 *
 * @author Cory McKay (ACE 1.x) and Jessica Thompson (ACE 2.x)
 */
public class InstanceClassifier
{

    /**
     * Classify a set of instances using a trained Weka Classifier.
     *
     * @param   trained                         Object containing references to
     *                                          the Weka objects needed for Classification,
     *                                          including a trained Weka Classifier.
     * @param	data_board			Contains instances to classify and
     *                                          method to perform classification.
     * @param	instances			The Weka Instances to classify.
     * @return                                  The classifications of each instance.
     * @throws Exception                        If an error is encountered.
     */
    public static SegmentedClassification[] classify(TrainedModel trained,
            DataBoard data_board,
            Instances instances,
            String results_file,
            boolean save_intermediate_arffs)
            throws Exception
    {
        boolean use_top_level_features = true;
        boolean use_sub_section_features = true;
        // Perform the classification and store the results in array of
        // SegmentedClassification objects to be returned
        SegmentedClassification[] resulting_classifications =
                data_board.getClassifiedResults(instances,
                save_intermediate_arffs,
                trained,
                use_top_level_features,
                use_sub_section_features);

        // Merge overlapping sections with the same classifications
        for (int i = 0; i < resulting_classifications.length; i++)
            SegmentedClassification.mergeAdjacentSections(resulting_classifications[i]);

        // Print to results file. (Will be an  ACE XML Classifications file or ARFF file.)
        // Extension of given results file name is checked in CommandLine
        if (results_file != null) {
            /*if (mckay.utilities.staticlibraries.StringMethods.getExtension(results_file).equals(".arff"))
            {
                ArffSaver saver = new ArffSaver();
                saver.setInstances(instances);
                saver.setFile(new File(results_file));
                saver.setDestination(new File(results_file));
                saver.writeBatch();
            }
            else if (mckay.utilities.staticlibraries.StringMethods.getExtension(results_file).equals(".xml"))
            {
                SegmentedClassification.saveClassifications(resulting_classifications,
                        new File(results_file),
                        "");
            }*/
        }
        return resulting_classifications;
    }

    
}
