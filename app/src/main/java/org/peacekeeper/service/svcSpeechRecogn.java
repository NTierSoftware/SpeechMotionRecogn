// http://programmingtolive.blogspot.com/2014/08/using-cmus-pocketsphinx-voice.html
//http://stackoverflow.com/questions/14940657/android-speech-recognition-as-a-service-on-android-4-1-4-2
//http://stackoverflow.com/questions/30281651/continuous-speech-recognition-android-without-gaps

package org.peacekeeper.service;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;

import org.peacekeeper.service.ShakeDetector.*;
import org.slf4j.Logger;
import org.slf4j.*;

import java.io.*;

import ch.qos.logback.classic.*;
import edu.cmu.pocketsphinx.*;
import edu.cmu.pocketsphinx.demo.*;


public class svcSpeechRecogn extends Service implements RecognitionListener, OnShakeListener{
static private final Logger mLog = LoggerFactory.getLogger( svcSpeechRecogn.class );
static private final LoggerContext mLoggerContext = (LoggerContext) LoggerFactory
		.getILoggerFactory();

private Context context;

private static final String KWS_SEARCH = "wakeup"
		, PHONE_SEARCH = "phones"
//		FORECAST_SEARCH = "forecast",
//		DIGITS_SEARCH = "digits",
//		MENU_SEARCH = "menu"
		;

/* Keyword we are looking for to activate menu */
//    private static final String KEYPHRASE = "oh mighty computer";
private SpeechRecognizer recognizer;
private String KEYPHRASE;

// The following are used for the shake detection
private SensorManager mSensorManager;
private Sensor mAccelerometer;
private ShakeDetector mShakeDetector;


//public svcSpeechRecogn(){}//cstr svcSpeechRecogn

@Override public int onStartCommand(Intent intent, int flags, int startId ){
	// The service is starting, due to a call to startService()
	mLog.debug( "\n\n\t\tonStartCommand: svcSpeechRecogn\n" );

//http://developer.android.com/intl/es/guide/components/services.html#Foreground
//Note: Unlike the activity lifecycle callback methods, you are not required to call the superclass implementation of these callback methods.
	//super.onStartCommand( intent, flags, startId );
	context = getApplicationContext();
	KEYPHRASE = context.getResources().getString( R.string.pkAlertPhrase );
	runRecognizerSetup();

	// ShakeDetector initialization
	mSensorManager = (SensorManager) getSystemService( Context.SENSOR_SERVICE );
	mAccelerometer = mSensorManager
			.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	mShakeDetector = new ShakeDetector();
	mShakeDetector.setOnShakeListener( this );
	mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);

/*
http://www.vogella.com/tutorials/AndroidServices/article.html
Service is restarted if it gets terminated.
Intent data passed to the onStartCommand method is null.
Used for services which manages their own state and do not depend on the Intent data.
*/
	return Service.START_STICKY;
}//onStartCommand

@Override public void onCreate(){
	super.onCreate();
	mLog.debug( "onCreate: svcSpeechRecogn" );

}//onCreate()

@Override public void onDestroy(){
	//super.onDestroy();
	mLoggerContext.stop();//flush log
	mLog.debug( "onDestroy: svcSpeechRecogn" );
	if ( recognizer != null ){
		recognizer.cancel();
		recognizer.shutdown();
	}

	mSensorManager.unregisterListener(mShakeDetector);
}//onDestroy(

@Override public void onPartialResult( Hypothesis hypothesis ){}

/* In partial result we get quick updates about current hypothesis. In
keyword spotting mode we can react here, in other modes we need to wait
for final result in onResult. *//*

@Override public void onPartialResult( Hypothesis hypothesis ){
	if ( hypothesis == null ) return;
	String text = hypothesis.getHypstr();
	mLog.debug( "onPartialResult:\t" + text );
	if ( text.equals( KEYPHRASE ) )
		switchSearch( MENU_SEARCH );
	else if ( text.equals( DIGITS_SEARCH ) )
		switchSearch( DIGITS_SEARCH );
	else if ( text.equals( PHONE_SEARCH ) )
		switchSearch( PHONE_SEARCH );
	else if ( text.equals( FORECAST_SEARCH ) )
		switchSearch( FORECAST_SEARCH );
	else mLog.debug( "no text equals");
}//onPartialResult
*/

//This callback is called when we stop the recognizer.
@Override public void onResult( Hypothesis hypothesis ){
	if ( hypothesis == null ) return;

	String text = hypothesis.getHypstr();
	mLog.error("\n\n\t\tonResult!:\t" + text );
	Intent PocketSphinxActivity = new Intent()
		.setClass(this, pkAlert.class )
		.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	startActivity(PocketSphinxActivity);
}//onResult()

@Override public void onBeginningOfSpeech(){}

//We stop recognizer here to get a final result
@Override public void onEndOfSpeech(){
	switchSearch( KWS_SEARCH );
	//if ( !recognizer.getSearchName().equals( KWS_SEARCH ) ) switchSearch( KWS_SEARCH );
}//onEndOfSpeech()

@Override public void onError( final Exception X ){}

@Override public void onTimeout(){ switchSearch( KWS_SEARCH ); }

@Override public IBinder onBind( Intent intent ){ return null; }

@Override public void onShake( final int count ){
	mLog.error( "SHAKE event!\tcount:\t" + count );
	Intent pkShook = new Intent()
			.setClass(this, pkShook.class )
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	startActivity(pkShook);
}//onShake()

private void switchSearch( String searchName ){
	recognizer.stop();
	recognizer.startListening( searchName );
}

private void runRecognizerSetup(){
//Recognizer initialization is a time-consuming and it involves IO, so we execute it in async task.
	new AsyncTask< Void, Void, Exception >(){
		@Override protected Exception doInBackground( Void... params ){
			mLog.debug( "doInBackground: svcSpeechRecogn" );

			try{
				Assets assets = new Assets( svcSpeechRecogn.this );
				File assetDir = assets.syncAssets();
				setupRecognizer( assetDir );
			}catch ( IOException X ){ return X; }
			return null;
		}

		@Override protected void onPostExecute( Exception result ){
			if ( result == null ){ switchSearch( KWS_SEARCH ); }
			else{ mLog.debug( "Failed to init recognizer!" );}
		}//onPostExecute
	}//new AsyncTask
	.execute();
}//runRecognizerSetup

private void setupRecognizer( File assetsDir ) throws IOException{
//The recognizer can be configured to perform multiple searches of different kind and switch between them
	recognizer = SpeechRecognizerSetup.defaultSetup()
              .setAcousticModel( new File( assetsDir, "en-us-ptm" ) )
              .setDictionary( new File( assetsDir, "cmudict-en-us.dict" ) )
// To disable logging of raw audio comment out this call (takes a lot of space on the device)
//              .setRawLogDir( assetsDir )
// Threshold to tune for keyphrase to balance between false alarms and misses
              .setKeywordThreshold( 1e-45f )
// Use context-independent phonetic search, context-dependent is too slow for mobile
              .setBoolean( "-allphone_ci", true )

      .getRecognizer();
	recognizer.addListener( this );

//In your application you might not need to add all those searches. They are added here for demonstration. You can leave just one.

	// Create keyword-activation search.
	recognizer.addKeyphraseSearch( KWS_SEARCH, KEYPHRASE );
/*

	// Create grammar-based search for selection between demos
	File menuGrammar = new File( assetsDir, "menu.gram" );
	recognizer.addGrammarSearch( MENU_SEARCH, menuGrammar );

	// Create grammar-based search for digit recognition
	File digitsGrammar = new File( assetsDir, "digits.gram" );
	recognizer.addGrammarSearch( DIGITS_SEARCH, digitsGrammar );

	// Create language model search
	File languageModel = new File( assetsDir, "weather.dmp" );
	recognizer.addNgramSearch( FORECAST_SEARCH, languageModel );
*/

	// Phonetic search
	File phoneticModel = new File( assetsDir, "en-phone.dmp" );
	recognizer.addAllphoneSearch( PHONE_SEARCH, phoneticModel );
}//setupRecognizer

}//svcSpeechRecogn
