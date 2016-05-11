
package edu.cmu.pocketsphinx.demo;

import android.app.*;
import android.os.*;

import org.slf4j.Logger;
import org.slf4j.*;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.util.*;
import ch.qos.logback.core.joran.spi.*;


public class pkShook extends Activity{
static private final Logger mLog = LoggerFactory.getLogger( pkShook.class );
static private final LoggerContext mLoggerContext = (LoggerContext) LoggerFactory
		.getILoggerFactory();
static private final ContextInitializer mContextInitializer = new ContextInitializer(
		mLoggerContext );



@Override public void onCreate( Bundle state ){
	super.onCreate( state );
	mLog.debug( "OnCreate" );


	setContentView( R.layout.main );
	//( (TextView) findViewById( R.id.caption_text ) ).setText( "Preparing the recognizer" );

	// Check if user has given permission to record audio
/*
	int permissionCheck = ContextCompat.checkSelfPermission( getApplicationContext(),
	                                                         Manifest.permission.RECORD_AUDIO );
	if ( permissionCheck != PackageManager.PERMISSION_GRANTED){
		ActivityCompat.requestPermissions( this, new String[]{ Manifest.permission.RECORD_AUDIO },
		                                   PERMISSIONS_REQUEST_RECORD_AUDIO );
		return;
	}
*/

}


@Override public void onDestroy(){
	super.onDestroy();
	mLog.debug( "OnCreate" );
	mLoggerContext.stop();//flush log
}//onDestroy()

@Override protected void onRestart(){
	super.onRestart();
	// Reload Logback log: http://stackoverflow.com/questions/3803184/setting-logback-appender-path-programmatically/3810936#3810936
	mLoggerContext.reset();

//I prefer autoConfig() over JoranConfigurator.doConfigure() so I don't need to find the file myself.
	try{ mContextInitializer.autoConfig(); }catch ( JoranException X ){ X.printStackTrace(); }
}//onRestart()


}
