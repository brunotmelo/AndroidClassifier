/*
 * @(#)FeatureProcessor.java	1.01	April 9, 2005.
 *
 * McGill Univarsity
 */

package fastclassifier.featureExtraction;

/* TODO: implemenet these control functions if needed

import jAudioFeatureExtractor.Cancel;
import jAudioFeatureExtractor.ExplicitCancel;
import jAudioFeatureExtractor.Updater;*/
import fastclassifier.dataTypes.FeatureDefinition;
import fastclassifier.featureExtraction.Aggregators.Aggregator;
import fastclassifier.featureExtraction.Aggregators.AggregatorContainer;
import fastclassifier.featureExtraction.Aggregators.Mean;
import fastclassifier.featureExtraction.Aggregators.StandardDeviation;
import fastclassifier.featureExtraction.audioFeatures.*;
import java.io.*;
import java.util.LinkedList;

/**
 * This class is used to pre-process and extract features from audio recordings.
 * An object of this class should be instantiated with parameters indicating the
 * details of how features are to be extracted.
 * <p>
 * The extractFeatures method should be called whenever recordings are available
 * to be analyzed. This mehtod should be called once for each recording. It will
 * write the extracted feature values to an XML file after each call. This will
 * also save feature definitions to another XML file.
 * <p>
 * The finalize method should be called when all features have been extracted.
 * this will finish writing the feature values to the XML file.
 * <p>
 * Features are extracted for each window and, when appropriate, the average and
 * standard deviation of each of these features is extracted for each recording.
 *
 * @author Cory McKay
 */
public class FeatureProcessor {
	/* FIELDS ***************************************************************** */

	// The window size used for dividing up the recordings to classify.
	private int window_size;

	// The number of samples that windows are offset by. A value of zero
	// means that there is no window overlap.
	private int window_overlap_offset;

	// The sampling rate that all recordings are to be converted to before
	// feature extraction.
	private double sampling_rate;

	// Whether or not to normalise recordings before feature extraction.
	private boolean normalise;

	// The features that are to be extracted.
	private FeatureExtractor[] feature_extractors;


    private FeatureDefinition[] overall_definitions;

	// The dependencies of the features in the feature_extractors field.
	// The first indice corresponds to the feature_extractors indice
	// and the second identifies the number of the dependent feature.
	// The entry identifies the indice of the feature in feature_extractors
	// that corresponds to a dependant feature. The first dimension will be
	// null if there are no dependent features.
	private int[][] feature_extractor_dependencies;

	// The longest number of windows of previous features that each feature must
	// have before it can be extracted. The indice corresponds to that of
	// feature_extractors.
	private int[] max_feature_offsets;

	// Which features are to be saved after processing. Entries correspond to
	// the
	// feature_extractors field.
	private boolean[] features_to_save;

	// Whether or not to save features individually for each window
	private boolean save_features_for_each_window;

	// Whetehr or not to save the average and standard deviation of each
	// feature accross all windows.
	private boolean save_overall_recording_features;

	// Used to write to the feature_vector_file file to save feature values to.
	private DataOutputStream values_writer;

	// Used to write to the feature_key_file file to save feature definitions
	// to.
	private DataOutputStream definitions_writer;

	// Indicates whether the feature definitions have been written by the
	// definitions_writer yet.
	private boolean definitions_written;

	// Indicates what the type of the output format is
	private int outputType;

	// Since Overall features are not recorded until after the header is
	// written, the main body needs to know if it needs to write the header or
	// not.
	private boolean isARFFOverallHeaderWritten = false;

	// hook for allowing visual updates of how far along the extraction is.
	private Updater updater;

	private AggregatorContainer aggContainer;

	/* CONSTRUCTOR ************************************************************ */
        
        /**
	 * Validates and stores the configuration to use for extracting features
	 * from audio recordings. Prepares the feature_vector_file and
	 * feature_key_file XML files for saving.
	 *
	 * @param window_size
	 *            The size of the windows that the audio recordings are to be
	 *            broken into.
	 * @param window_overlap
	 *            The fraction of overlap between adjacent windows. Must be
	 *            between 0.0 and less than 1.0, with a value of 0.0 meaning no
	 *            overlap.
	 * @param sampling_rate
	 *            The sampling rate that all recordings are to be converted to
	 *            before feature extraction
	 * @param normalise
	 *            Whether or not to normalise recordings before feature
	 *            extraction.
	 * @param all_feature_extractors
	 *            All features that can be extracted.
	 * @param features_to_save_among_all
	 *            Which features are to be saved. Entries correspond to the
	 *            all_feature_extractors parameter.
	 * @param save_features_for_each_window
	 *            Whether or not to save features individually for each window.
	 * @param save_overall_recording_features
	 *            Whether or not to save the average and standard deviation of
	 *            each feature accross all windows.
	 * @throws Exception
	 *             Throws an informative exception if the input parameters are
	 *             invalid.
	 */
	public FeatureProcessor(int window_size, double window_overlap,
			double sampling_rate, boolean normalise,
			FeatureExtractor[] all_feature_extractors,
			boolean[] features_to_save_among_all,
			boolean save_features_for_each_window,
			boolean save_overall_recording_features
			)
			throws Exception {
		//aggregator = container;
		// Throw an exception if the control parameters are invalid
		if (!save_features_for_each_window && !save_overall_recording_features)
			throw new Exception(
					"You must save at least one of the windows-based\n"
							+ "features and the overall file-based features.");
		// if (feature_values_save_path.equals(""))
		// throw new Exception("No save path specified for feature values.");
		// if (feature_definitions_save_path.equals(""))
		// throw new Exception(
		// "No save path specified for feature definitions.");
		if (window_overlap < 0.0 || window_overlap >= 1.0)
			throw new Exception("Window overlap fraction is " + window_overlap
					+ ".\n"
					+ "This value must be 0.0 or above and less than 1.0.");
		if (window_size < 3)
			throw new Exception("Window size is " + window_size + ".\n"
					+ "This value must be above 2.");
		boolean one_selected = false;
		for (int i = 0; i < features_to_save_among_all.length; i++)
			if (features_to_save_among_all[i])
				one_selected = true;
		if (!one_selected)
			throw new Exception("No features have been set to be saved.");

		definitions_written = false;

		// Save parameters as fields
		this.window_size = window_size;
		this.sampling_rate = sampling_rate;
		this.normalise = normalise;
		this.save_features_for_each_window = save_features_for_each_window;
		this.save_overall_recording_features = save_overall_recording_features;

		// Calculate the window offset
		window_overlap_offset = (int) (window_overlap * (double) window_size);

		// Find which features need to be extracted and in what order. Also find
		// the indices of dependencies and the maximum offsets for each feature.
		findAndOrderFeaturesToExtract(all_feature_extractors,
				features_to_save_among_all);
	}

	/* PUBLIC METHODS ********************************************************* */
        
        
        
        
	/**
	 * Extract the features from the provided audio. Splitting windows as part of the process.
	 *
	 * @param samples
	 *            The audio samples to extract features from.
	 */
	public double[][][] extractFeatures(double[] samples, Updater updater)
			throws Exception {
		// Pre-process the recording and extract the samples from the audio
		this.updater = updater;
		// Calculate the window start indices
		LinkedList<Integer> window_start_indices_list = new LinkedList<Integer>();
		int this_start = 0;
		while (this_start < samples.length) {
			window_start_indices_list.add(new Integer(this_start));
			this_start += window_size - window_overlap_offset;
		}
		Integer[] window_start_indices_I = window_start_indices_list
				.toArray(new Integer[1]);
		int[] window_start_indices = new int[window_start_indices_I.length];

		// if were using a progress bar, set its max update
		if (updater != null) {
			updater.setFileLength(window_start_indices.length);
		}

		for (int i = 0; i < window_start_indices.length; i++)
			window_start_indices[i] = window_start_indices_I[i].intValue();

		// Extract the feature values from the samples
		double[][][] window_feature_values = getFeatures(samples,
				window_start_indices);

                
                return window_feature_values;
	}

        //useless method
        private double[][] aggregateFeatures(double[][][] windowFeatureValues, int aggregator){
            // Find the feature averages and standard deviations if appropriate
            double[][] overall_feature_values = null;
		
		overall_feature_values = getOverallRecordingFeatures(
		windowFeatureValues, true,true);
            
            return overall_feature_values;
        }


	public FeatureDefinition[] getFeatureDefinitions(){
        FeatureDefinition[] defs = new FeatureDefinition[feature_extractors.length];

        for(int i= 0; i<feature_extractors.length; i++){
            defs[i]=feature_extractors[i].getFeatureDefinition();
        }
            return defs;
    }

	/* PRIVATE METHODS ******************************************************** */

	/**
	 * Fills the feature_extractors, feature_extractor_dependencies,
	 * max_feature_offsets and features_to_save fields. This involves finding
	 * which features need to be extracted and in what order and finding the
	 * indices of dependencies and the maximum offsets for each feature.
	 * <p>
	 * Daniel McEnnis 05-07-05 added feature offset of dependancies to
	 * max_offset
	 *
	 * @param all_feature_extractors
	 *            All features that can be extracted.
	 * @param features_to_save_among_all
	 *            Which features are to be saved. Entries correspond to the
	 *            all_feature_extractors parameter.
	 */
	private void findAndOrderFeaturesToExtract(
			FeatureExtractor[] all_feature_extractors,
			boolean[] features_to_save_among_all) {
		// Find the names of all features
		String[] all_feature_names = new String[all_feature_extractors.length];
		for (int feat = 0; feat < all_feature_extractors.length; feat++){
                    all_feature_names[feat] = all_feature_extractors[feat]
					.getFeatureDefinition().name;
                }
			

		// Find dependencies of all features marked to be extracted.
		// Mark as null if features are not to be extracted. Note that will also
		// be null if there are no dependencies.
		String[][] dependencies = new String[all_feature_extractors.length][];
		for (int feat = 0; feat < all_feature_extractors.length; feat++) {
			if (features_to_save_among_all[feat])
				dependencies[feat] = all_feature_extractors[feat]
						.getDepenedencies();
			else
				dependencies[feat] = null;
		}

		// Add dependencies to dependencies and if any features are not marked
		// for
		// saving but are marked as a dependency of a feature that is marked to
		// be
		// saved. Also fill features_to_extract in order to know what features
		// to
		// extract(but not necessarily save).
		boolean done = false;
		boolean[] features_to_extract = new boolean[dependencies.length];
		for (int feat = 0; feat < features_to_extract.length; feat++) {
			if (features_to_save_among_all[feat])
				features_to_extract[feat] = true;
			else
				features_to_extract[feat] = false;
		}
		while (!done) {
			done = true;
			for (int feat = 0; feat < dependencies.length; feat++)
				if (dependencies[feat] != null)
					for (int i = 0; i < dependencies[feat].length; i++) {
						String name = dependencies[feat][i];
						for (int j = 0; j < all_feature_names.length; j++) {
							if (name.equals(all_feature_names[j])) {
								if (!features_to_extract[j]) {
									features_to_extract[j] = true;
									dependencies[j] = all_feature_extractors[j]
											.getDepenedencies();
									if (dependencies[j] != null)
										done = false;
								}
								j = all_feature_names.length;
							}
						}
					}
		}

		// Find the correct order to extract features in by filling the
		// feature_extractors field
		int number_features_to_extract = 0;
		for (int i = 0; i < features_to_extract.length; i++)
			if (features_to_extract[i])
				number_features_to_extract++;
		feature_extractors = new FeatureExtractor[number_features_to_extract];
		features_to_save = new boolean[number_features_to_extract];
		for (int i = 0; i < features_to_save.length; i++)
			features_to_save[i] = false;
		boolean[] feature_added = new boolean[dependencies.length];
		for (int i = 0; i < feature_added.length; i++)
			feature_added[i] = false;
		int current_position = 0;
		done = false;
		while (!done) {
			done = true;

			// Add all features that have no remaining dependencies and remove
			// their dependencies from all unadded features
			for (int feat = 0; feat < dependencies.length; feat++) {
				if (features_to_extract[feat] && !feature_added[feat])
					if (dependencies[feat] == null) // add feature if it has no
					// dependencies
					{
						feature_added[feat] = true;
						feature_extractors[current_position] = all_feature_extractors[feat];
						features_to_save[current_position] = features_to_save_among_all[feat];
						current_position++;
						done = false;

						// Remove this dependency from all features that have
						// it as a dependency and are marked to be extracted
						for (int i = 0; i < dependencies.length; i++)
							if (features_to_extract[i]
									&& dependencies[i] != null) {
								int num_defs = dependencies[i].length;
								for (int j = 0; j < num_defs; j++) {
									if (dependencies[i][j]
											.equals(all_feature_names[feat])) {
										if (dependencies[i].length == 1) {
											dependencies[i] = null;
											j = num_defs;
										} else {
											String[] temp = new String[dependencies[i].length - 1];
											int m = 0;
											for (int k = 0; k < dependencies[i].length; k++) {
												if (k != j) {
													temp[m] = dependencies[i][k];
													m++;
												}
											}
											dependencies[i] = temp;
											j--;
											num_defs--;
										}
									}
								}
							}
					}
			}
		}

		// Find the indices of the feature extractor dependencies for each
		// feature
		// extractor
		feature_extractor_dependencies = new int[feature_extractors.length][];
		String[] feature_names = new String[feature_extractors.length];
		for (int feat = 0; feat < feature_names.length; feat++) {
			feature_names[feat] = feature_extractors[feat].getFeatureDefinition().name;
		}
		String[][] feature_dependencies_str = new String[feature_extractors.length][];
		for (int feat = 0; feat < feature_dependencies_str.length; feat++)
			feature_dependencies_str[feat] = feature_extractors[feat]
					.getDepenedencies();
		for (int i = 0; i < feature_dependencies_str.length; i++)
			if (feature_dependencies_str[i] != null) {
				feature_extractor_dependencies[i] = new int[feature_dependencies_str[i].length];
				for (int j = 0; j < feature_dependencies_str[i].length; j++)
					for (int k = 0; k < feature_names.length; k++)
						if (feature_dependencies_str[i][j]
								.equals(feature_names[k]))
							feature_extractor_dependencies[i][j] = k;
			}

		// Find the maximum offset for each feature
		// Daniel McEnnis 5-07-05 added feature offset of dependancies to
		// max_offset
		max_feature_offsets = new int[feature_extractors.length];
		for (int i = 0; i < max_feature_offsets.length; i++) {
			if (feature_extractors[i].getDepenedencyOffsets() == null)
				max_feature_offsets[i] = 0;
			else {
				int[] these_offsets = feature_extractors[i]
						.getDepenedencyOffsets();
				max_feature_offsets[i] = Math
						.abs(these_offsets[0]
								+ max_feature_offsets[feature_extractor_dependencies[i][0]]);
				for (int k = 0; k < these_offsets.length; k++) {
					int val = Math.abs(these_offsets[k])
							+ max_feature_offsets[feature_extractor_dependencies[i][k]];
					if (val > max_feature_offsets[i]) {
						max_feature_offsets[i] = val;
					}
				}
			}
		}
	}

	/**
	 * Breaks the given samples into the appropriate windows and extracts
	 * features from each window.
	 *
	 * @param samples
	 *            The samples to extract features from. Sample values should
	 *            generally be between -1 and +1.
	 * @param window_start_indices
	 *            The indices of samples that correspond to where each window
	 *            should start.
	 * @return The extracted feature values for this recording. The first indice
	 *         identifies the window, the second identifies the feature and the
	 *         third identifies the feature value. The third dimension will be
	 *         null if the given feature could not be extracted for the given
	 *         window.
	 * @throws Exception
	 *             Throws an exception if a problem occurs.
	 */
	private double[][][] getFeatures(double[] samples,
			int[] window_start_indices) throws Exception {
		// The extracted feature values for this recording. The first indice
		// identifies the window, the second identifies the feature and the
		// third identifies the feature value.
		double[][][] results = new double[window_start_indices.length][feature_extractors.length][];

		// Calculate how frequently to make updates to the updater;
		int updateThreshold = 1;
		if (window_start_indices.length > 100) {
			updateThreshold = window_start_indices.length / 100;
		}

		// Extract features from each window one by one and add save the
		// results.
		// The last window is zero-padded at the end if it falls off the edge of
		// the
		// provided samples.
		for (int win = 0; win < window_start_indices.length; win++) {
			// Do we need to update the progress bar or not
			if ((updater != null) && (win % updateThreshold == 0)) {
				updater.announceUpdate(win);
				/*if(cancel.isCancel()){
					throw new ExplicitCancel("Killed while processing features");
				}*/
			}

			// Find the samples in this window and zero-pad if necessary
			double[] window = new double[window_size];
			int start_sample = window_start_indices[win];
			int end_sample = start_sample + window_size - 1;
			if (end_sample < samples.length)
				for (int samp = start_sample; samp <= end_sample; samp++)
					window[samp - start_sample] = samples[samp];
			else
				for (int samp = start_sample; samp <= end_sample; samp++) {
					if (samp < samples.length)
						window[samp - start_sample] = samples[samp];
					else
						window[samp - start_sample] = 0.0;
				}

			// Extract the features one by one
			for (int feat = 0; feat < feature_extractors.length; feat++) {
				// Only extract this feature if enough previous information
				// is available to extract this feature
				if (win >= max_feature_offsets[feat]) {
					// Find the correct feature
					FeatureExtractor feature = feature_extractors[feat];

					// Find previously extracted feature values that this
					// feature
					// needs
					double[][] other_feature_values = null;
					if (feature_extractor_dependencies[feat] != null) {
						other_feature_values = new double[feature_extractor_dependencies[feat].length][];
						for (int i = 0; i < feature_extractor_dependencies[feat].length; i++) {
							int feature_indice = feature_extractor_dependencies[feat][i];
							int offset = feature.getDepenedencyOffsets()[i];
							other_feature_values[i] = results[win + offset][feature_indice];
						}
					}

					// Store the extracted feature values
					results[win][feat] = feature.extractFeature(window,
							sampling_rate, other_feature_values);
				} else
					results[win][feat] = null;
			}
		}

		// Return the results
		return results;
	}

	public void calcOverallRecordingFeatures(
			double[][][] window_feature_values, Aggregator[] aggList) throws Exception{

		aggContainer = new AggregatorContainer();

		aggContainer.add(feature_extractors, features_to_save);
		aggContainer.add(aggList);
		aggContainer.aggregate(window_feature_values);

	}

	public double[][] getOverallRecordingFeatureValues(){
		return aggContainer.getFeatureValues();
	}

	public FeatureDefinition[] getOverallRecordingFeatureDefinitions(){
		return aggContainer.getFeatureDefinitions();
	}

        private double[][] getOverallFeaturesMean(double[][][] window_feature_values){
            
            //initializes the array with the number of features
            double[][] overallFeatures = new double[window_feature_values[0].length][];
            
            for (int feat =0; feat<window_feature_values[0].length; feat++){
                //initializes the 2nd array with the feature number of dimensions
                overallFeatures[feat] = new double[window_feature_values[0][feat].length];
                //iterates through the windows and get the overall
                for(int w=0; w<window_feature_values.length; w++){
                    
                }
                
                
            }
            
            return null;
        }
        
	/**
	 * Calculates the averages and standard deviations over a whole recording of
	 * each of the windows-based features. Generates a feature definition for
	 * each such feature.
	 *
	 * @param window_feature_values
	 *            The extracted window feature values for this recording. The
	 *            first indice identifies the window, the second identifies the
	 *            feature and the third identifies the feature value. The third
	 *            dimension will be null if the given feature could not be
	 *            extracted for the given window.
	 * @return The extracted overall average and standard deviations of the
	 *         window feature values that were passed to this method. The first
	 *         indice identifies the feature and the second iddentifies the
	 *         feature value. The order of the features correspond to the
	 *         FeatureDefinitions that the overall_feature_definitions parameter
	 *         is filled with.
	 */
	public double[][] getOverallRecordingFeatures(
			double[][][] window_feature_values, boolean mean, boolean stdev) {

		LinkedList<double[]> values = new LinkedList<double[]>();
        LinkedList<FeatureDefinition> definitions = new LinkedList<FeatureDefinition>();

		for (int feat = 0; feat < feature_extractors.length; feat++)
			if (window_feature_values[window_feature_values.length - 1][feat] != null
					&& features_to_save[feat]) {

                // Make the definitions
                FeatureDefinition this_def = feature_extractors[feat]
                        .getFeatureDefinition();
                FeatureDefinition average_definition = new FeatureDefinition(
                        this_def.name + " Overall Average",
                        this_def.description
                                + "\nThis is the overall average over all windows.",
                        this_def.is_sequential,
                        window_feature_values[window_feature_values.length - 1][feat].length);
                FeatureDefinition stdv_definition = new FeatureDefinition(
                        this_def.name + " Overall Standard Deviation",
                        this_def.description
                                + "\nThis is the overall standard deviation over all windows.",
                        this_def.is_sequential,
                        window_feature_values[window_feature_values.length - 1][feat].length);


				// Find the averages and standard deviations
				double[] averages = new double[window_feature_values[window_feature_values.length - 1][feat].length];
				double[] stdvs = new double[window_feature_values[window_feature_values.length - 1][feat].length];
				for (int val = 0; val < window_feature_values[window_feature_values.length - 1][feat].length; val++) {
					// Find the number of windows that have values for this
					// value feature
					int count = 0;
					for (int win = 0; win < window_feature_values.length; win++)
						if (window_feature_values[win][feat] != null)
							count++;

					// Find the values to find the average and standard
					// deviations of
					double[] values_to_process = new double[count];
					int current = 0;
					for (int win = 0; win < window_feature_values.length; win++)
						if (window_feature_values[win][feat] != null) {
							values_to_process[current] = window_feature_values[win][feat][val];
							current++;
						}

					// Calculate the averages and standard deviations
                                        if(mean){
                                            averages[val] = fastclassifier.GeneralTools.Statistics
							.getAverage(values_to_process);
                                        }
                                        if(stdev){
                                            stdvs[val] = fastclassifier.GeneralTools.Statistics
							.getStandardDeviation(values_to_process);
                                        }
					
					
				}
                                if(mean){
                                    values.add(averages);
                                    definitions.add(average_definition);


                                }
                                if(stdev){
                                    values.add(stdvs);
                                    definitions.add(stdv_definition);
                                }
				
				
                                
			}

        overall_definitions = definitions.toArray(new FeatureDefinition[1]);
		return values.toArray(new double[1][]);
	}

//	/**
//	 * Writes the given feature values extracted from a recording to the
//	 * feature_vector_file XML file referred to by the values_writer field.
//	 * Writes both the individual window features and the overall recording
//	 * features to disk.
//	 *
//	 * @param feature_values
//	 *            The extracted feature values for this recording. The first
//	 *            indice identifies the window, the second identifies the
//	 *            feature and the third identifies the feature value. The third
//	 *            dimension will be null if the given feature could not be
//	 *            extracted for the given window.
//	 * @param window_start_indices
//	 *            The indices of samples that correspond to where each window
//	 *            should start.
//	 * @param identifier
//	 *            A string to use for identifying this recording. Often a file
//	 *            path.
//	 * @param overall_feature_values
//	 *            The extracted overall average and standard deviations of the
//	 *            window feature values. The first indice identifies the feature
//	 *            and the second identifies the feature value. The order of the
//	 *            features correspond to the overall_feature_definitions
//	 *            parameter. This value is null if overall feature values were
//	 *            not extracted.
//	 * @param overall_feature_definitions
//	 *            The feature definitions of the features that are in the
//	 *            overall_feature_values parameter. Will be null if no overall
//	 *            features were extracted.
//	 * @throws Exception
//	 *             Throws an exception if cannot write.
//	 */
	/*private void saveACEFeatureVectorsForARecording(
			double[][][] feature_values, int[] window_start_indices,
			String identifier, AggregatorContainer aggContainer)
			throws Exception {
		// Start the entry for the recording
		values_writer.writeBytes("\t<data_set>\n");
		values_writer.writeBytes("\t\t<data_set_id>" + identifier
				+ "</data_set_id>\n");

		// Write the features for individual windows
		if (save_features_for_each_window)
			for (int win = 0; win < feature_values.length; win++) {
				double start_time = ((double) window_start_indices[win])
						/ sampling_rate;
				double end_time = ((double) (window_start_indices[win]
						+ window_size - 1))
						/ sampling_rate;
				values_writer.writeBytes("\t\t<section start=\"" + start_time
						+ "\" stop=\"" + end_time + "\">\n");
				for (int feat = 0; feat < feature_values[win].length; feat++) {
					if (features_to_save[feat])
						if (feature_values[win][feat] != null) {
							String feature_name = feature_extractors[feat]
									.getFeatureDefinition().name;
							values_writer.writeBytes("\t\t\t<feature>\n");
							values_writer.writeBytes("\t\t\t\t<name>"
									+ feature_name + "</name>\n");
							for (int val = 0; val < feature_values[win][feat].length; val++) {
								String value = fastclassifier.GeneralTools.StringMethods
										.getDoubleInScientificNotation(
												feature_values[win][feat][val],
												4);
								values_writer.writeBytes("\t\t\t\t<v>" + value
										+ "</v>\n");
							}
							values_writer.writeBytes("\t\t\t</feature>\n");
						}
				}
				values_writer.writeBytes("\t\t</section>\n");
			}

		// Write the features for the file
		if (save_overall_recording_features)
			// for (int feat = 0; feat < overall_feature_values.length; feat++)
			// {
			// values_writer.writeBytes("\t\t<feature>\n");
			// values_writer.writeBytes("\t\t\t<name>"
			// + overall_feature_definitions[feat].name + "</name>\n");
			// for (int val = 0; val < overall_feature_values[feat].length;
			// val++) {
			// String value = jAudioFeatureExtractor.GeneralTools.StringMethods
			// .getDoubleInScientificNotation(
			// overall_feature_values[feat][val], 4);
			// values_writer.writeBytes("\t\t\t<v>" + value + "</v>\n");
			// }
			// values_writer.writeBytes("\t\t</feature>\n");
			// }
			aggContainer.outputACEValueEntries(values_writer);
		// End the entry for the recording
		values_writer.writeBytes("\t</data_set>\n\n");
	}
*/

//	/**
//	 * Writes feature definitions to the XML file referred to by the
//	 * definitions_writer field. Writes both overall and individual feature
//	 * definitions.
//	 *
//	 * @param feature_values
//	 *            The extracted feature values for a recording. The first indice
//	 *            identifies the window, the second identifies the feature and
//	 *            the third identifies the feature value.
//	 * @param overall_feature_definitions
//	 *            The feature definitions of the features that are in the
//	 *            features for the recording. Will be null if no overallfeatures
//	 *            were extracted.
//	 * @throws Exception
//	 *             Throws an exception if cannot write.
//	 */
/*
	private void saveFeatureDefinitions(double[][][] feature_values,
			AggregatorContainer aggContainer) throws Exception {
		String feature_key_header = new String(
				"<?xml version=\"1.0\"?>\n"
						+ "<!DOCTYPE feature_key_file [\n"
						+ "   <!ELEMENT feature_key_file (comments, feature+)>\n"
						+ "   <!ELEMENT comments (#PCDATA)>\n"
						+ "   <!ELEMENT feature (name, description?, is_sequential, parallel_dimensions)>\n"
						+ "   <!ELEMENT name (#PCDATA)>\n"
						+ "   <!ELEMENT description (#PCDATA)>\n"
						+ "   <!ELEMENT is_sequential (#PCDATA)>\n"
						+ "   <!ELEMENT parallel_dimensions (#PCDATA)>\n"
						+ "]>\n\n" + "<feature_key_file>\n\n"
						+ "   <comments></comments>\n\n");
		definitions_writer.writeBytes(feature_key_header);

		double[][] last_window_features = feature_values[feature_values.length - 1];

		// Write the window functions
		if (save_features_for_each_window)
			for (int feat = 0; feat < feature_extractors.length; feat++)
				if (features_to_save[feat])
					if (last_window_features[feat] != null) {
						FeatureDefinition def = feature_extractors[feat]
								.getFeatureDefinition();
						definitions_writer.writeBytes("   <feature>\n");
						definitions_writer.writeBytes("      <name>" + def.name
								+ "</name>\n");
						definitions_writer.writeBytes("      <description>"
								+ def.description + "</description>\n");
						definitions_writer.writeBytes("      <is_sequential>"
								+ def.is_sequential + "</is_sequential>\n");
						definitions_writer
								.writeBytes("      <parallel_dimensions>"
										+ last_window_features[feat].length
										+ "</parallel_dimensions>\n");
						definitions_writer.writeBytes("   </feature>\n\n");
					}

		// Write the overall file functions
		if (save_overall_recording_features) {
			// for (int feat = 0; feat < overall_feature_definitions.length;
			// feat++) {
			// FeatureDefinition def = overall_feature_definitions[feat];
			// definitions_writer.writeBytes(" <feature>\n");
			// definitions_writer.writeBytes(" <name>" + def.name
			// + "</name>\n");
			// definitions_writer.writeBytes(" <description>"
			// + def.description + "</description>\n");
			// definitions_writer.writeBytes(" <is_sequential>"
			// + def.is_sequential + "</is_sequential>\n");
			// definitions_writer.writeBytes(" <parallel_dimensions>"
			// + def.dimensions + "</parallel_dimensions>\n");
			// definitions_writer.writeBytes(" </feature>\n\n");
			aggContainer.outputACEFeatureKeyEntries(definitions_writer);
		}

		definitions_writer.writeBytes("</feature_key_file>");

		definitions_writer.close();

		definitions_written = true;
	}
*/
    public FeatureDefinition[] getOverallDefinitions() {
        return overall_definitions;
    }
}