package edu.cmu.pocketsphinx.demo;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.hardware.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v4.content.*;

import org.peacekeeper.service.*;
import org.peacekeeper.service.ShakeDetector.*;
import org.slf4j.Logger;
import org.slf4j.*;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.util.*;
import ch.qos.logback.core.joran.spi.*;


public class PocketSphinxActivity extends Activity{
static private final Logger				mLog	= LoggerFactory.getLogger( PocketSphinxActivity.class );
static private final LoggerContext mLoggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
static private final ContextInitializer mContextInitializer = new ContextInitializer( mLoggerContext );


/* Used to handle permission request */
private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

@Override public void onCreate( Bundle state ){
	super.onCreate( state );
	mLog.debug("OnCreate");

	Intent intent = new Intent( this, svcSpeechRecogn.class);
	ComponentName svc = startService(intent);
	if (svc == null) mLog.error( "\n\n\t\tonCreate: CANNOT START SERVICE!\n\n\t\t" );
	else mLog.info( "\n\n\t\tonCreate: svc started:" + svc.getClassName() + "\n\n\t\t" );


	setContentView( R.layout.main );
	//( (TextView) findViewById( R.id.caption_text ) ).setText( "Preparing the recognizer" );

	// Check if user has given permission to record audio
	int permissionCheck = ContextCompat.checkSelfPermission( getApplicationContext(),
	                                                         Manifest.permission.RECORD_AUDIO );
	if ( permissionCheck != PackageManager.PERMISSION_GRANTED){
		ActivityCompat.requestPermissions( this, new String[]{ Manifest.permission.RECORD_AUDIO },
		                                   PERMISSIONS_REQUEST_RECORD_AUDIO );
		return;
	}


}//onCreate


@Override protected void onRestart(){
	super.onRestart();
	// Reload Logback log: http://stackoverflow.com/questions/3803184/setting-logback-appender-path-programmatically/3810936#3810936
	mLoggerContext.reset();

//I prefer autoConfig() over JoranConfigurator.doConfigure() so I don't need to find the file myself.
	try{ mContextInitializer.autoConfig(); }
	catch ( JoranException X ){ X.printStackTrace(); }
}//onRestart()


@Override
public void onRequestPermissionsResult( int requestCode,
                                        String[] permissions, int[] grantResults ){
	super.onRequestPermissionsResult( requestCode, permissions, grantResults );

	if ( requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO ){
		if ( grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ){

		}
		else{ finish(); }
	}
}



@Override public void onDestroy(){
	super.onDestroy();
	mLog.debug("OnCreate");
	mLoggerContext.stop();//flush log
}//onDestroy()

}
