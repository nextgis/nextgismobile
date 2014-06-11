/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2012-2014 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.mobile;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nextgis.mobile.map.NGMapView;
import com.nextgis.mobile.services.TrackerService;
import com.nextgis.mobile.services.TrackerService.RecordedGeoPoint;
import com.nextgis.mobile.services.TrackerService.TSBinder;


public class MainActivity extends SherlockActivity{
	public final static String TAG = "nextgismobile";	
	public final static String LOACTION_HINT = "com.nextgis.gis.location";	
	
	protected TrackerService m_oTrackerService;
	protected Handler m_oTrakAddPointHandler;
	
	protected NGMapView m_oMap;
	
	public final static int MENU_MARK = 0;
	public final static int MENU_RECORD_GPX = 1;
	public final static int MENU_INFO = 2;
	public final static int MENU_PAN = 3;
	public final static int MENU_SETTINGS = 4;
	public final static int MENU_ABOUT = 5;
	public final static int MENU_COMPASS = 6;
	public final static int MENU_LAYERS = 7;	
	
	protected boolean m_bGpxRecord;
	
	//protected ProgressDialog m_oProgressDlg;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
		boolean bInfoOn = prefs.getBoolean(NGMConstants.PREFS_SHOW_INFO, false);
		m_bGpxRecord = prefs.getBoolean(NGMConstants.KEY_PREF_SW_TRACKGPX_SRV, false);
		boolean bCompassOn = prefs.getBoolean(NGMConstants.PREFS_SHOW_COMPASS, false);	
		int nTileSize = 256;//prefs.getInt(NGMConstants.KEY_PREF_TILE_SIZE + "_int", 256);
		int nZoom = prefs.getInt(NGMConstants.PREFS_ZOOM_LEVEL, 1);
		int nScrollX = prefs.getInt(NGMConstants.PREFS_SCROLL_X, 0);
		int nScrollY = prefs.getInt(NGMConstants.PREFS_SCROLL_Y, 0);
		
		m_oMap = new NGMapView(this);
		m_oMap.initMap(nTileSize, nZoom, nScrollX, nScrollY);
		m_oMap.showInfo(bInfoOn);
		m_oMap.showCompass(bCompassOn);
	}
	
	@Override
	public void onPause() {
		final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		if(m_oMap != null){
			m_oMap.onPause(edit);
		}
		
		edit.putBoolean(NGMConstants.KEY_PREF_SW_TRACKGPX_SRV, m_bGpxRecord);

		if(m_bGpxRecord){			
			unbindService(m_oConnection);
		}
	
		edit.commit();			
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if(m_oMap != null){
			m_oMap.onResume(prefs);
		}
		
		m_bGpxRecord = prefs.getBoolean(NGMConstants.KEY_PREF_SW_TRACKGPX_SRV, false);
		if (m_bGpxRecord) {
			startGPXRecord();
		}
	}
	
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getSupportMenuInflater().inflate(R.menu.main, menu);
        //menu.add(Menu.NONE, MENU_MARK, Menu.NONE, R.string.sMark)
        //.setIcon(R.drawable.ic_location_place)
        menu.add(Menu.NONE, MainActivity.MENU_LAYERS, Menu.NONE, R.string.sLayers)
        .setIcon(R.drawable.ic_layers)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MainActivity.MENU_RECORD_GPX, Menu.NONE, R.string.sGPXRecord)
        .setIcon(R.drawable.ic_gpx_record_start)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MainActivity.MENU_INFO, Menu.NONE, R.string.sInfo)
        .setIcon(R.drawable.ic_action_about)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);		
        menu.add(Menu.NONE, MainActivity.MENU_PAN, Menu.NONE, R.string.sPan)
        .setIcon(R.drawable.ic_pan2)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);		
        
        menu.add(Menu.NONE, MainActivity.MENU_COMPASS, Menu.NONE, R.string.sCompass)
        //.setIcon(R.drawable.ic_action_about)
        .setIcon(R.drawable.ic_menu_compass)
		//.setCheckable(true)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu.add(Menu.NONE, MainActivity.MENU_SETTINGS, Menu.NONE, R.string.sSettings)
        .setIcon(R.drawable.ic_action_settings)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
        menu.add(Menu.NONE, MainActivity.MENU_ABOUT, Menu.NONE, R.string.sAbout)
        .setIcon(R.drawable.ic_action_about)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);	
        
		//mOsmv.getOverlayManager().onCreateOptionsMenu((android.view.Menu) menu, Menu.FIRST + 1, mOsmv);
       return true;
		//super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case android.R.id.home:
            return false;
        case MainActivity.MENU_SETTINGS:
            // app icon in action bar clicked; go home
            Intent intentSet = new Intent(this, PreferencesActivity.class);
            intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentSet);
            return true;
        case MainActivity.MENU_ABOUT:
            Intent intentAbout = new Intent(this, AboutActivity.class);
            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAbout);
            return true;	  
        case MainActivity.MENU_PAN:
        	m_oMap.panToLocation();   	
        	return true;
        case MainActivity.MENU_INFO:
        	m_oMap.switchInfo();
        	return true;
        case MainActivity.MENU_RECORD_GPX:
        	onRecordGpx();
        	return true;
/*        case MENU_LAYERS:  
        	try {
				unzip();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	//onMark();
            return true;*/
        case MainActivity.MENU_COMPASS:        	
        	m_oMap.switchCompass();
            return true;    
        }
		return super.onOptionsItemSelected(item);
	}
	
	void doBindService() {
		
		m_oTrakAddPointHandler = new Handler() {
            public void handleMessage(Message msg) {
            	super.handleMessage(msg);
            	
            	Bundle resultData = msg.getData();
            	double dfLat = resultData.getDouble("lat");
            	double dfLon = resultData.getDouble("lon");
            	m_oMap.addPointToRouteOverlay(dfLon, dfLat);
            }
        };

		
	    bindService(new Intent(this, TrackerService.class), m_oConnection, 0);
	  }
	
	private ServiceConnection m_oConnection = new ServiceConnection() {

		@Override
	    public void onServiceConnected(ComponentName className, IBinder binder) {
			TSBinder tsBinder = (TSBinder) binder;
			if(tsBinder == null)
				return;
			
			m_oTrackerService = tsBinder.getService();
			if(m_oTrackerService == null)
				return;
			
			m_oTrackerService.setPathHanler(m_oTrakAddPointHandler);
			//fill path
			ArrayList<RecordedGeoPoint> path = m_oTrackerService.GetPath();
			m_oMap.addPointsToRouteOverlay(path);
	    }

		@Override
	    public void onServiceDisconnected(ComponentName className) {
			m_oTrackerService = null;
	    }
	};	
	

	void onRecordGpx(){
		m_bGpxRecord = !m_bGpxRecord;	
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean(NGMConstants.KEY_PREF_SW_TRACKGPX_SRV, m_bGpxRecord);
		edit.commit();
		
        final SharedPreferences.Editor editor1 = getSharedPreferences("preferences", Context.MODE_PRIVATE).edit();
        editor1.putBoolean(NGMConstants.KEY_PREF_SW_TRACKGPX_SRV, m_bGpxRecord);
        editor1.commit();   

		
		if(m_bGpxRecord){
			//start record
			startGPXRecord();
		}
		else{
			//stop record
			stopGPXRecord();
		}
	}    
	
	void startGPXRecord(){
		startService(new Intent(TrackerService.ACTION_START_GPX));
		
	    bindService(new Intent(this, TrackerService.class), m_oConnection, Context.BIND_AUTO_CREATE);
     }
	
	void stopGPXRecord(){
		 startService(new Intent(TrackerService.ACTION_STOP_GPX));
		 unbindService(m_oConnection);
	}

	void onMark(){
    /* TODO:   
		final Location loc = mLocationOverlay.getLastFix();
		
		if(loc == null)
		{
			Toast.makeText(getApplicationContext(), R.string.error_loc_fix, Toast.LENGTH_SHORT).show();
		}
		else
		{	
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			final boolean bAccCoord = prefs.getBoolean(PreferencesActivity.KEY_PREF_ACCURATE_LOC, false);
			if(bAccCoord)
			{				          
	          class AccLocation extends Handler  implements LocationListener{
	        	  int nPointCount;
	        	  double dfXsum, dfYsum, dfXmean, dfYmean, dfXmin, dfYmin, dfXmax, dfYmax;
	        	  double dfAsum, dfAmean, dfAmin, dfAmax;
	        	  double dfXSumSqDev, sdYSumSqDev;
	        	  ArrayList<GeoPoint> GPSRecords = new ArrayList<GeoPoint>();
	        	  
	        	  public AccLocation() {
						dfXsum = dfYsum = dfXmean = dfYmean = dfXmin = dfYmin = dfXmax = dfYmax = 0;
						dfAsum = dfAmean = dfAmin = dfAmax = 0;
						dfXSumSqDev = sdYSumSqDev = 0;
						
						mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	        		  	
						pd = new ProgressDialog(MainActivity.this);
						pd.setTitle(R.string.acc_gather_dlg_title);
						//pd.setMessage("Wait");
						pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						final int nGPSCount = prefs.getInt(PreferencesActivity.KEY_PREF_ACCURATE_GPSCOUNT + "_int", 60);
						pd.setMax(nGPSCount);
						pd.setIndeterminate(true);
						pd.show();									
	        	  }
	        	  
		        public void handleMessage(Message msg) {
		        	pd.setIndeterminate(false);
		        	if (pd.getProgress() < pd.getMax()) {
		        		sendEmptyMessageDelayed(0, 100);
		        	}
		        	else {
		        		mLocationManager.removeUpdates(this);
		        		
						dfXmean = dfXsum / nPointCount;
						dfYmean = dfYsum / nPointCount;	
						dfAmean = dfAsum / nPointCount;		
						
						Location newLoc = new Location("GPS Accurate");
						
		        		newLoc.setSpeed(0);
		        		newLoc.setLatitude(dfYmean);
		        		newLoc.setLongitude(dfXmean);
		        		newLoc.setAltitude(dfAmean);
		        		newLoc.setTime(System.currentTimeMillis());									
						
		        		GeoPoint basept = new GeoPoint(newLoc);
		        		
		        		ArrayList<Integer> GPSDist = new ArrayList<Integer>();
		        		
		        		for (final GeoPoint gp : GPSRecords) {
		        			dfXSumSqDev += ( (gp.getLongitudeE6() - basept.getLongitudeE6()) / 1000000 ) * ( (gp.getLongitudeE6() - basept.getLongitudeE6()) / 1000000 );
		        			sdYSumSqDev += ( (gp.getLatitudeE6() - basept.getLatitudeE6()) / 1000000 ) * ( (gp.getLatitudeE6() - basept.getLatitudeE6()) / 1000000 );
		        			
		        			GPSDist.add(basept.distanceTo(gp));
		    			}
		        		
		        		Collections.sort(GPSDist);
		        		

			        	float dfAcc;
			        	int nIndex = 0;
						final String CE = prefs.getString(PreferencesActivity.KEY_PREF_ACCURATE_CE, "CE50");

			        	if(CE.compareTo("CE50") == 0)
			        		nIndex = (int) (GPSDist.size() * 0.5);
						else if(CE.compareTo("CE90") == 0)
							nIndex = (int) (GPSDist.size() * 0.9);
						else if(CE.compareTo("CE95") == 0)
							nIndex = (int) (GPSDist.size() * 0.95);
						else if(CE.compareTo("CE98") == 0)
							nIndex = (int) (GPSDist.size() * 0.98);

			        	dfAcc = GPSDist.get(nIndex);
		        		newLoc.setAccuracy(dfAcc);
		        		
		        		Intent newIntent = new Intent(MainActivity.this, InputPointActivity.class);
		        		newIntent.putExtra(LOACTION_HINT, newLoc);
		        		startActivity (newIntent);
		        		pd.dismiss();
		        	}	
		        }

				public void onLocationChanged(Location location) {
					GPSRecords.add(new GeoPoint(location.getLatitude(), location.getLongitude()));
					if ( dfXmin == 0 )
					{
						dfXmin = location.getLongitude();
						dfXmax = location.getLongitude();
					}
					else {
						dfXmin = Math.min(dfXmin, location.getLongitude());
						dfXmax = Math.max(dfXmin, location.getLongitude());
					}
					
					if ( dfYmin == 0 )
					{
						dfYmin = location.getLatitude();
						dfYmax = location.getLatitude();
					}
					else {
						dfYmin = Math.min(dfYmin, location.getLatitude());
						dfYmax = Math.max(dfYmin, location.getLatitude());
					}
					
					if ( dfAmin == 0 )
					{
						dfAmin = location.getAltitude();
						dfAmax = location.getAltitude();
					}
					else {
						dfAmin = Math.min(dfAmin, location.getAltitude());
						dfAmax = Math.max(dfAmax, location.getAltitude());
					}								
					
					dfXsum += location.getLongitude();
					dfYsum += location.getLatitude();
					dfAsum += location.getAltitude();
					
					nPointCount++;
					
					//dfXmean = dfXsum / nPointCount;
					//dfYmean = dfYsum / nPointCount;
							
					//pd.setMessage("X: " + (( location.getLongitude() - dfXmean ) * ( location.getLongitude() - dfXmean )) + "Y: " + (( location.getLatitude() - dfYmean ) * ( location.getLatitude() - dfYmean )));
					pd.incrementProgressBy(1);								
				}

				public void onProviderDisabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				public void onStatusChanged(String provider,
						int status, Bundle extras) {
					// TODO Auto-generated method stub
					
				}
	          }
	          
	          AccLocation h = new AccLocation();
	          h.sendEmptyMessageDelayed(0, 2000);
			}
			else
			{
				Toast.makeText(getApplicationContext(), PositionFragment.getLocationText(getApplicationContext(), loc), Toast.LENGTH_SHORT).show();
				Intent newIntent = new Intent(this, InputPointActivity.class);		
				newIntent.putExtra(LOACTION_HINT, loc);
				startActivity (newIntent);
			}
		}*/
	}
	
	/*//////////////////////////////////////////////////////////////////////////////////////////////
	
	
	public void unzip() throws IOException {
		ProgressDialog oDownloadDialog = new ProgressDialog(this);
		oDownloadDialog.setMessage("extracting");//moContext.getResources().getString(R.string.sZipExtractionProcess));
		oDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		oDownloadDialog.setCancelable(true);
		oDownloadDialog.show();
		File oInputFile = new File(org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.OSMDROID_PATH, "sss.zip");
		File oOutputFileDir = new File(org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.OSMDROID_PATH, "layers");
		File oOutputFile = new File(oOutputFileDir, ".test_zip");
    	new UnZipTask(oInputFile, oOutputFile.getAbsolutePath(), oDownloadDialog).execute();
    }
    
    private class UnZipTask extends AsyncTask<String, Void, Boolean> {
    	private File msInputPath;
    	private String msOutputPath;
    	private ProgressDialog moDownloadDialog;
    	private int per = 0;
    	
        public UnZipTask(File sInputPath, String sOutputPath, ProgressDialog oDownloadDialog) {        
            super();
            msInputPath = sInputPath;
            msOutputPath = sOutputPath;
            moDownloadDialog = oDownloadDialog;
        }
        
    	@Override
    	protected Boolean doInBackground(String... params) {
    		
   		try {
    			//DeleteRecursive(new File(msPath));
    			ZipFile zipfile = new ZipFile(msInputPath);
    			moDownloadDialog.setMax(zipfile.size());
    			for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements();) {
    				ZipEntry entry = (ZipEntry) e.nextElement();
    				unzipEntry(zipfile, entry, msOutputPath);
    				per++;
    				moDownloadDialog.setProgress(per);
    			}
    			zipfile.close();
//    			archive.delete();
    		
    			/*JSONObject oJSONRoot = new JSONObject();

            	oJSONRoot.put("name", msName);
				oJSONRoot.put("name_" + Locale.getDefault().getLanguage(), msLocName);
				oJSONRoot.put("ver", mnVer);
				oJSONRoot.put("directed", mbDirected);            
            
	            String sJSON = oJSONRoot.toString();
	            File file = new File(msPath, MainActivity.META);
	            if(MainActivity.writeToFile(file, sJSON)){
	            	//store data
	            	//create sqlite db
	            	//Creating and saving the graph
		            bundle.putBoolean(MainActivity.BUNDLE_ERRORMARK_KEY, false);
	            } 
	            else{
		            bundle.putBoolean(MainActivity.BUNDLE_ERRORMARK_KEY, true);            	
	                bundle.putString(MainActivity.BUNDLE_MSG_KEY, "write failed");
	            }*//*
    		
			} 
    		/*catch (JSONException e) {
				e.printStackTrace();
	            bundle.putBoolean(MainActivity.BUNDLE_ERRORMARK_KEY, true);            	
				bundle.putString(MainActivity.BUNDLE_MSG_KEY, e.getLocalizedMessage());
			}*//*
			catch (Exception e) {
				e.printStackTrace();
	            /*bundle.putBoolean(MainActivity.BUNDLE_ERRORMARK_KEY, true);            	
	            bundle.putString(MainActivity.BUNDLE_MSG_KEY, e.getLocalizedMessage());*//*
				return false;
			}    		
                  
            /*Message msg = new Message();
            msg.setData(bundle);
            if(moEventReceiver != null){
            	moEventReceiver.sendMessage(msg);
            }   	*/	/*
    		return true;
    	}
    	
    	@Override
    	protected void onPostExecute(Boolean result) {
    		moDownloadDialog.dismiss();
    	}
    	
    	private void unzipEntry(ZipFile zipfile, ZipEntry entry, String outputDir) throws IOException {
    		if (entry.isDirectory()) {
    			createDir(new File(outputDir, entry.getName()));
    			return;
    		}
    		File outputFile = new File(outputDir, entry.getName());
    		if (!outputFile.getParentFile().exists()) {
    			createDir(outputFile.getParentFile());
    		}

    		BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
    		BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
    		try {
    			byte[] _buffer = new byte[1024];
    			copyStream(inputStream, outputStream, _buffer, 1024);
    		} finally {
    			outputStream.flush();
    			outputStream.close();
    			inputStream.close();
    		}
    	}
    	
    	private void copyStream( InputStream is, OutputStream os, byte[] buffer, int bufferSize ) throws IOException {
			try {
				for (;;) {
					int count = is.read( buffer, 0, bufferSize );
					if ( count == -1 ) { break; }
					os.write( buffer, 0, count );
				}
			} catch ( IOException e ) {
				throw e;
			}
		}    	
    	
    	private void createDir(File dir) {
    		if (dir.exists()) {
    			return;
    		}
    		if (!dir.mkdirs()) {
    			throw new RuntimeException("Can not create dir " + dir);
    		}
    	}
    	
    	private void DeleteRecursive(File fileOrDirectory) {
    	    if (fileOrDirectory.isDirectory())
    	        for (File child : fileOrDirectory.listFiles())
    	            DeleteRecursive(child);

    	    fileOrDirectory.delete();
    	}
    }*/
}
