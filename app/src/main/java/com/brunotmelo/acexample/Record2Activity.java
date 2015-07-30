package com.brunotmelo.acexample;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.brunotmelo.acexample.util.ExtAudioRecorder;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import ace.datatypes.TrainedModel;
import fastclassifier.Controller;
import fastclassifier.FastClassifier;


public class Record2Activity extends Activity {

    private String audioOutputPath;
    private ExtAudioRecorder gravador;
    private double[] samples;
    private Button recordingbutton;

    public void recordAudio(View view){
        System.out.println("recording");
        //changes button color
        recordingbutton = (Button) findViewById(R.id.RecordButton);
        recordingbutton.setEnabled(false);
        recordingbutton.setBackgroundColor(Color.GREEN);
        //starts recording
        gravador.start();
    }

    public void stopRecording(View view){
        System.out.println("stopped");
        gravador.stop();

        samples = gravador.getSamples();
        System.out.println(samples.length);

        Button testbutton = (Button) findViewById(R.id.testButton);
        testbutton.setEnabled(true);
        testbutton.setBackgroundColor(Color.GREEN);
        recordingbutton.setBackgroundColor(Color.RED);
    }

    public void classify(View view){
        TextView classification = (TextView) findViewById(R.id.showClassification);
        String awns = "none";
        try {
            TrainedModel model = createTrainedModel();
            awns = FastClassifier.classifyAudio(samples,model);


        } catch (Exception e) {
            e.printStackTrace();
        }
        classification.setText(awns);
    }

    private TrainedModel createTrainedModel(){
        InputStream ins = getResources().openRawResource(R.raw.sopro);
        TrainedModel model = null;
        try {
            ObjectInputStream object_stream = new ObjectInputStream(ins);
            model = (TrainedModel) object_stream.readObject();
            ins.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return model;

    }

    /*public void testAssobio(View view){

        Button treta = (Button)findViewById(R.id.isAssobioButton);

        treta.setText("rolou");
        //double mfccval = getAssobio();

    }*/

    /*public static double getAssobio(){
        Object[] data = null;
        try {
            data = (Object[]) XMLDocumentParser.parseXMLDocument("ftestFV.xml",
                    "feature_vector_file");
        } catch (Exception e) {
            System.out.println("Error encountered parsing the settings file");
            System.out.println(e.getMessage());
            System.exit(3);
        }

        //double
        DataSet dos = (DataSet)data[0];
        double[][] bagda = dos.feature_values;
        //esse é o valor da feature correspondente a
        //Area Method of Moments of MFCCs Overall Average 1

        return bagda[1][1];
    }*/

    private void setOutputPath(){
        audioOutputPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        audioOutputPath += "/command.wav";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record2);
        gravador = new ExtAudioRecorder(true, MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_CONFIGURATION_MONO , AudioFormat.ENCODING_PCM_16BIT);
        setOutputPath();
        gravador.setOutputFile(audioOutputPath);
        gravador.prepare();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_record2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
