AndroidClassifier

<b>About:</b>
	Android classifier is a FastClassifier(https://github.com/brunotmelo/FastClassifier) fork intended for android use.</p> 

<b>Testing:</b>
	The tool comes with an Android application for testing. You can download, import into Android Studio and run it in your device.

##How to embed in your application##
  Create a new android library named "androidclassifier" as a module in your application.
  Add everything AndroidClassifier/androidclassifier folder to the root of your module.
  Add the new module as a compile dependency to your app(app module).
  You will have to open a TrainedModel file in your application. Instructions on how to do that are below.
  You will have to get the audio as a double array.
  Run FastClassifier.ClassifyAudio(double[] audio, TrainedModel yourClassifier )

<b>How to open a TrainedModel:</b> 
There are many ways to open a TrainedModel. It is not included in the library because the library has no access to the
application resources.</br>
In my application I have choosen to ship the .model file with the application. You could copy the .model file to the android
filesystem and open it...</br>
A way to do:</br>
1. Copy the yourclassifier.model file to your application's res/raw folder</br>
2. Copy the method com.brunotmelo.acexample.createTrainedModel to one of your app classes</br>
3. Change "R.raw.sopro" in the line InputStream ins = getResources().openRawResource(R.raw.sopro); to "R.raw.yourclassifier"</br>
*notice the R.raw.yourclassifier is without its extension.</br></br>

<b>How to create a double array:</b> 
I changed a existing library(found in this link: http://i-liger.com/article/android-wav-audio-recording) to get a recording as double[].
As it was for example purposes I don't guarantee that the double[] output it gives is correct.</br>
If you want to use my method:</br>
1. Copy the class com.brunotmelo.acexample.util.ExtAudioRecorder.java to your app.</br>
2. Record the audio using that class.</br>
3. Get the double[] output using ExtAudioRecorder.getSamples()</br>

##Classifying your own audios##
To create an audio classifier(the .model file) you should try EasyClassifier http://sourceforge.net/projects/easyclassifier/ </br>
After you create the classifier, change the library run parameters in the fastclassifier.options.Parameters class.</br>
To understand what the parameters means, check the fastClassifier [documentation](https://github.com/brunotmelo/FastClassifier/tree/master/documentation).


