package com.powerwheelino;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class ControlRemoteCar extends Activity {
    
	//Settings related variables
	public static final String PREFS_NAME = "Preferences";
	private String serverIP;
	private int serverPort;
	private int messageRate;
	private boolean debugMode;
	
	//View Variables for later access
	private JoystickView joystick;
	private jMoveListener jmove;
	
	//Udp Client
	private UdpClient udp;
	
	//Thread Related
	private Handler handler;
	private Thread commThread;
	private boolean  paused = false;
	private static long MSG_INTERVAL = 20; //Milliseconds
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }
    
    //Handle Hardware Menu Button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settingsmenu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.item1:
            	stopComm();
                setContentView(R.layout.settingslayout);
                
                EditText ipTextBox = (EditText)findViewById(R.id.editText1);
                ipTextBox.setText(serverIP, TextView.BufferType.EDITABLE);
                
                EditText portTextBox = (EditText)findViewById(R.id.serverPort);
                portTextBox.setText(String.valueOf(serverPort));
                
                SeekBar msgRateBar = (SeekBar)findViewById(R.id.rateBar1);
                msgRateBar.setProgress(messageRate);
                
                ToggleButton dbgBtn = (ToggleButton)findViewById(R.id.toggleDbgBtn);
                dbgBtn.setChecked(debugMode);
                
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    //Handle Settings Page Buttons
    public void saveSettings(View view){
    	EditText ipAddr = (EditText)findViewById(R.id.editText1);
    	String ipAddrString = ipAddr.getText().toString();
    	
    	EditText srvPrt = (EditText)findViewById(R.id.serverPort);
    	int srvPortInt = Integer.parseInt(srvPrt.getText().toString());
    	
    	SeekBar msgRateBar = (SeekBar)findViewById(R.id.rateBar1);
    	int msgRate = msgRateBar.getProgress();
    	
    	ToggleButton dbgBtn = (ToggleButton)findViewById(R.id.toggleDbgBtn);
    	boolean debugYN = dbgBtn.isChecked();
    	
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	
    	editor.putInt("messageRate", msgRate);
    	editor.putBoolean("debugMode", debugYN);
    	editor.putString("serverIP", ipAddrString);
        editor.putInt("serverPort", srvPortInt);
        editor.commit();
        
        Log.d("powerwheelino", "Server Info Saved: "+ ipAddrString + "@" + srvPortInt);
        
        init();
        
    }
    public void cancelSettings(View view){init();}
    
    private void init(){
    	// Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        serverIP = settings.getString("serverIP", "192.168.1.1");
        serverPort = settings.getInt("serverPort", 50000);
        messageRate = settings.getInt("messageRate", 50);
        debugMode = settings.getBoolean("debugMode", false);
        
        MSG_INTERVAL = messageRate;
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        joystick = new JoystickView(this);
        //jmove = new jMoveListener(serverIP, serverPort);
        //joystick.setOnJoystickMovedListener(jmove);
        handler = new Handler();
        udp = new UdpClient(serverIP, serverPort);
        if (udp.isConnected()){
        	CharSequence tMsg = serverIP + "@" + serverPort + " - Connected Successfully !!!";
        	Toast.makeText(this.getApplicationContext(),tMsg , Toast.LENGTH_SHORT).show();
        } else {
        	CharSequence tMsg = "Server " + serverIP + "@" + serverPort + " not connected !!!";
        	Toast.makeText(this.getApplicationContext(),tMsg , Toast.LENGTH_SHORT).show();
        }
        setContentView(joystick);
        startComm();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	stopComm();
    }
    
    @Override
    protected void onRestart(){
    	super.onRestart();
    	startComm();
    }
    @Override
    protected void onPause(){
    	super.onPause();
    	stopComm();
    }
    @Override
    protected void onResume(){
    	super.onResume();
    	startComm();
    }
    
    // Start UDP Communication thread
    public void startComm(){
//    	if (commThread != null){
//    		commThread.stop();
//    		commThread.destroy();
//    		commThread = null;
//    	}
    	paused = false;
    	if (commThread == null){
    		Runnable commRun = new Runnable(){
				@Override
				public void run() {
					while (true){
						if (!paused) {
							try {
								Thread.sleep(MSG_INTERVAL);
								//Log.d("powerwheelino", "udp comm thread looper iterated");
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							handler.post(new Runnable() {
								
								@Override
								public void run() {
									int x = joystick.getUserX();
									int y = joystick.getUserY();
									boolean xInBounds = (x > -127 && x < 127);
									boolean yInBounds = (y > -127 && y < 127);
									if (xInBounds && yInBounds){
										byte drive = (byte) (y & 0x000000FF);
										byte steering = (byte) (x & 0x000000FF);
										udp.sendData(drive, steering);
										//Log.d("powerwheelino","coordinates: X: " + x + " Y: " + y);
									}
									
								}
								
							});
						}
					}
						
				}
				
	    		
	    	};
    	commThread = new Thread(commRun);
    	commThread.start();
    	}
    }
    // Stop UDP Communication thread
    public void stopComm(){
    	paused = true;
    }
}
