package com.icloudoor.clouddoor;

import com.jauker.widget.BadgeView;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.Volley;
import com.icloudoor.clouddoor.ShakeEventManager;
import com.icloudoor.clouddoor.UartService;
import com.icloudoor.clouddoor.ShakeEventManager.ShakeListener;

@SuppressLint("NewApi")
public class KeyFragment extends Fragment implements ShakeListener {

	private String TAG = "KeyFragment";
	
	private MyDataBaseHelper mKeyDBHelper;
	private SQLiteDatabase mKeyDB;
	private final String DATABASE_NAME = "KeyDB.db";
	private final String TABLE_NAME = "KeyInfoTable";

	private ArrayList<HashMap<String, String>> carDoorList;
	private ArrayList<HashMap<String, String>> manDoorList;

	private RelativeLayout channelSwitch;
	private RelativeLayout keyWidge;

	private TextView TvChooseCar;
	private TextView TvChooseMan;
	private TextView TvDistrictDoor;
	private TextView TvCarNumber;
	private RelativeLayout TvOpenKeyList;

	private ImageView IvChooseCar;
	private ImageView IvChooseMan;
	private RelativeLayout IvSearchKey;
	private ImageView IvOpenDoorLogo;
	private ImageView IvWeatherWidgePush1;
	private ImageView IvWeatherWidgePush2;

	private int COLOR_CHANNEL_CHOOSE = 0xFF010101;
	private int COLOR_CHANNEL_NOT_CHOOSE = 0xFF999999;

	private int isChooseCarChannel;
	private int canDisturb;
	private int haveSound;
	private int canShake;
	private boolean isFindKey;

	private float alpha_transparent = 0.0f;
	private float alpha_opaque = 1.0f;

	private ViewPager mWeatherWidgePager;
	private ArrayList<Fragment> mKeyPageFragmentList;
	private KeyPageAdapter mKeyPageAdapter;
	private WeatherWidgeFragment mWeatherWidgeFragment;
	private WeatherWidgeFragment2 mWeatherWidgeFragment2;
	public FragmentManager mFragmentManager;
	public MyPageChangeListener myPageChangeListener;

	// for BLE
	private static final int REQUEST_ENABLE_BT = 0;
	private static final long SCAN_PERIOD = 1500; // ms
	private BluetoothAdapter mBluetoothAdapter;
	private DeviceAdapter mDeviceAdapter;
	private UartService mUartService = null;
	private ShakeEventManager mShakeMgr;
	private List<BluetoothDevice> mDeviceList;
	private Map<String, Integer> mDevRssiValues;
	private BluetoothGattCharacteristic mNotifyCharacteristic;

	private int deviceIndexToOpen;
	private String NameOfDoorToOpen = null;
	private String IdOfDoorToOpen = null;

	private SoundPool mSoundPool;
		
	private boolean checkForOpenDoor = false;
	
	public KeyFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.key_page, container, false);

		mKeyDBHelper = new MyDataBaseHelper(getActivity(), DATABASE_NAME);
		mKeyDB = mKeyDBHelper.getWritableDatabase();
		
		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		getActivity().registerReceiver(mBluetoothStateReceiver, filter);
		
		channelSwitch = (RelativeLayout) view.findViewById(R.id.channel_switch);
		keyWidge = (RelativeLayout) view.findViewById(R.id.key_widge);

		TvChooseCar = (TextView) view.findViewById(R.id.Tv_choose_car);
		TvChooseMan = (TextView) view.findViewById(R.id.Tv_choose_man);
		TvDistrictDoor = (TextView) view.findViewById(R.id.district_door);
		TvCarNumber = (TextView) view.findViewById(R.id.car_number);
		TvDistrictDoor.setSelected(true);
		TvCarNumber.setSelected(true);	
		TvOpenKeyList = (RelativeLayout) view.findViewById(R.id.open_key_list);

		IvChooseCar = (ImageView) view.findViewById(R.id.Iv_choose_car);
		IvChooseMan = (ImageView) view.findViewById(R.id.Iv_choose_man);
		IvSearchKey = (RelativeLayout) view.findViewById(R.id.Iv_search_key);
		IvOpenDoorLogo = (ImageView) view.findViewById(R.id.Iv_open_door_logo);
		IvWeatherWidgePush1 = (ImageView) view.findViewById(R.id.Iv_weather_widge_push1);
		IvWeatherWidgePush2 = (ImageView) view.findViewById(R.id.Iv_weather_widge_push2);

		mFragmentManager = getChildFragmentManager();
		mWeatherWidgePager = (ViewPager) view.findViewById(R.id.weather_widge_pager);
		myPageChangeListener = new MyPageChangeListener();
	
		InitFragmentViews();
		InitViewPager();
		
//		// BLE
//		if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//			Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//		}
//
//		BluetoothManager mBluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
//		mBluetoothAdapter = mBluetoothManager.getAdapter();
//
//		if (mBluetoothAdapter == null) {
//			if (getActivity() != null)
//				Toast.makeText(getActivity(), R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
//		}

//		checkBlueToothState();
//		service_init();

		mShakeMgr = new ShakeEventManager(getActivity());
		mShakeMgr.setListener(KeyFragment.this);
		mShakeMgr.init(getActivity());
			
		carDoorList = new ArrayList<HashMap<String, String>>();
		manDoorList = new ArrayList<HashMap<String, String>>();
//		if (mKeyDBHelper.tabIsExist(TABLE_NAME)) {
//			if (DBCount() > 0) {
//				
//				BadgeView badge = new com.jauker.widget.BadgeView(getActivity());
//				badge.setTargetView(TvOpenKeyList);
//				badge.setBadgeGravity(Gravity.RIGHT);
//				badge.setBadgeCount((int)DBCount());//TODO
//				
//				Cursor mCursor = mKeyDB.rawQuery("select * from " + TABLE_NAME,
//						null);
//				if (mCursor.moveToFirst()) {
//					int deviceIdIndex = mCursor.getColumnIndex("deviceId");
//					int doorNamemIndex = mCursor.getColumnIndex("doorName");
//					int doorTypeIndex = mCursor.getColumnIndex("doorType");
//
//					do {
//						HashMap<String, String> temp = new HashMap<String, String>();
//						String deviceId = mCursor.getString(deviceIdIndex);
//						String doorName = mCursor.getString(doorNamemIndex);
//						String doorType = mCursor.getString(doorTypeIndex);
//
//						if (doorType.equals("2")) { // ��ͨ�еĳ����б�����deviceId, doorName
//							temp.put("CDdeviceid", deviceId);
//							temp.put("CDdoorName", doorName);
//							carDoorList.add(temp);
//						} else if (doorType.equals("1")) { // ��ͨ�е������б�����deviceId, doorName
//							temp.put("MDdeviceid", deviceId);
//							temp.put("MDdoorName", doorName);
//							manDoorList.add(temp);
//						}
//					} while (mCursor.moveToNext());
//				}
//			}
//		}

		channelSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isChooseCarChannel == 1) {
					TvChooseCar.setTextColor(COLOR_CHANNEL_NOT_CHOOSE);
					TvChooseMan.setTextColor(COLOR_CHANNEL_CHOOSE);
					IvChooseCar.setAlpha(alpha_transparent);
					IvChooseMan.setAlpha(alpha_opaque);
					isChooseCarChannel = 0;
					populateDeviceList();
				} else {
					TvChooseCar.setTextColor(COLOR_CHANNEL_CHOOSE);
					TvChooseMan.setTextColor(COLOR_CHANNEL_NOT_CHOOSE);
					IvChooseCar.setAlpha(alpha_opaque);
					IvChooseMan.setAlpha(alpha_transparent);
					isChooseCarChannel = 1;
					populateDeviceList();
				}
			}
		});

		IvOpenDoorLogo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doOpenDoor();
				if (haveSound == 1) {
					playOpenDoorSound();
				}
			}
		});

		TvOpenKeyList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(getActivity(), KeyList.class);
				startActivity(intent);
			}
		});
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.e("TEST", "keyFragment onResume()");
		
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if(currentapiVersion >= 18){
			// BLE
			if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
				Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			}

			BluetoothManager mBluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = mBluetoothManager.getAdapter();

			if (mBluetoothAdapter == null) {
				if (getActivity() != null)
					Toast.makeText(getActivity(), R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
			}
			checkBlueToothState();
			service_init();
		} else {
			if(getActivity() != null)
				Toast.makeText(getActivity(), R.string.low_android_version, Toast.LENGTH_SHORT).show();
		}
		
		if (mKeyDBHelper.tabIsExist(TABLE_NAME)) {
			if (DBCount() > 0) {
				
				BadgeView badge = new com.jauker.widget.BadgeView(getActivity());
				badge.setTargetView(TvOpenKeyList);
				badge.setBadgeGravity(Gravity.RIGHT);
		        badge.setBadgeCount((int)DBCount());//TODO
				
				Cursor mCursor = mKeyDB.rawQuery("select * from " + TABLE_NAME,
						null);
				if (mCursor.moveToFirst()) {
					int deviceIdIndex = mCursor.getColumnIndex("deviceId");
					int doorNamemIndex = mCursor.getColumnIndex("doorName");
					int doorTypeIndex = mCursor.getColumnIndex("doorType");

					do {
						HashMap<String, String> temp = new HashMap<String, String>();
						String deviceId = mCursor.getString(deviceIdIndex);
						String doorName = mCursor.getString(doorNamemIndex);
						String doorType = mCursor.getString(doorTypeIndex);

						if (doorType.equals("2")) { // ��ͨ�еĳ����б�����deviceId, doorName
							temp.put("CDdeviceid", deviceId);
							temp.put("CDdoorName", doorName);
							carDoorList.add(temp);
						} else if (doorType.equals("1")) { // ��ͨ�е������б�����deviceId, doorName
							temp.put("MDdeviceid", deviceId);
							temp.put("MDdoorName", doorName);
							manDoorList.add(temp);
						}
					} while (mCursor.moveToNext());
				}
			}
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(mBluetoothAdapter.isEnabled())
			scanLeDevice(false);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(mBluetoothStateReceiver);

		try {
			LocalBroadcastManager.getInstance(getActivity())
					.unregisterReceiver(UARTStatusChangeReceiver);
		} catch (Exception e) {

		}
		getActivity().unbindService(mServiceConnection);
		mUartService.stopSelf();
		mUartService = null;
	}

	private void service_init() {
		Intent bindIntent = new Intent(getActivity(), UartService.class);
		getActivity().bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
	}

	private void checkBlueToothState() {
		Log.e("BLE", "checkBlueToothState");
		if (mBluetoothAdapter == null) {
			if (getActivity() != null)
				Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
		} else {
			if (mBluetoothAdapter.isEnabled()) {
				if (mBluetoothAdapter.isDiscovering()) {
				} else{
					populateDeviceList();
					// if(getActivity() != null)
					// Toast.makeText(getActivity(), R.string.bt_enabled,
					// Toast.LENGTH_SHORT).show();
				}
			} else {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	private void populateDeviceList() {
		Log.e("BLE", "populateDeviceList");
		
		IvOpenDoorLogo.setEnabled(false);
		TvDistrictDoor.setText(R.string.searching_key);
		TvDistrictDoor.setTextSize(18);
		TvDistrictDoor.setTextColor(0xFFffffff);
		TvCarNumber.setText(R.string.can_shake_to_open_door);
		TvCarNumber.setTextColor(0xFFffffff);
		IvSearchKey.setBackgroundResource(R.drawable.btn_gray);
		IvOpenDoorLogo.setImageResource(R.drawable.btn_serch_1);
		
		mDeviceList = new ArrayList<BluetoothDevice>();
		mDeviceAdapter = new DeviceAdapter(getActivity(), mDeviceList);
		mDevRssiValues = new HashMap<String, Integer>();
		scanLeDevice(true);

	}

	private void scanLeDevice(final boolean enable) {
		Log.e("BLE", "scanLeDevice");
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					channelSwitch.setEnabled(true);
					
					if (mDeviceList != null && mDeviceList.size() > 0) {

						int maxRssiIndex = 0;
						int maxRssi = -128;

						for (int i = 0; i < mDeviceList.size(); i++) {
							Log.e("TEST", "checking rssi");
							String tempAdd = mDeviceList.get(i).getAddress();
							int tempRssi = mDevRssiValues.get(tempAdd);
							if (tempRssi > maxRssi) {
								maxRssi = tempRssi;
								maxRssiIndex = i;
							}
						}
						deviceIndexToOpen = maxRssiIndex;
					}
					
					if (mDeviceList.size() != 0) {
						if (isChooseCarChannel == 1) {
							for (int i = 0; i < carDoorList.size(); i++) {
								String tempDID = carDoorList.get(i).get("CDdeviceid");
								tempDID = tempDID.toUpperCase();
								char[] data = tempDID.toCharArray();
								String formatDeviceId = String.valueOf(data[0]) + String.valueOf(data[1]) + ":"
										+ String.valueOf(data[2]) + String.valueOf(data[3]) + ":"
										+ String.valueOf(data[4]) + String.valueOf(data[5]) + ":"
										+ String.valueOf(data[6]) + String.valueOf(data[7]) + ":"
										+ String.valueOf(data[8]) + String.valueOf(data[9]) + ":"
										+ String.valueOf(data[10]) + String.valueOf(data[11]);
								Log.e("TEST", "CDdeviceID:" + formatDeviceId);

								if (mDeviceList.get(deviceIndexToOpen).getAddress().equals(formatDeviceId)) {
									IvOpenDoorLogo.setEnabled(true);
									IvOpenDoorLogo.setImageResource(R.drawable.selector_pressed);
									IvSearchKey.setBackgroundResource(R.drawable.btn_background_blue);
									TvDistrictDoor.setText(carDoorList.get(i).get("CDdoorName"));
									TvDistrictDoor.setTextSize(18);
									TvDistrictDoor.setTextColor(0xFFffffff);
									TvCarNumber.setText(carDoorList.get(i).get("CDdeviceid"));
									TvCarNumber.setTextColor(0xFFffffff);
								}
							}
						} else {
							for (int i = 0; i < manDoorList.size(); i++) {
								String tempDID = manDoorList.get(i).get("MDdeviceid");
								tempDID = tempDID.toUpperCase();
								char[] data = tempDID.toCharArray();
								String formatDeviceId = String.valueOf(data[0]) + String.valueOf(data[1]) + ":"
										+ String.valueOf(data[2]) + String.valueOf(data[3]) + ":"
										+ String.valueOf(data[4]) + String.valueOf(data[5]) + ":"
										+ String.valueOf(data[6]) + String.valueOf(data[7]) + ":"
										+ String.valueOf(data[8]) + String.valueOf(data[9]) + ":"
										+ String.valueOf(data[10]) + String.valueOf(data[11]);
								Log.e("TEST", "MDdeviceID:" + formatDeviceId);

								if (mDeviceList.get(deviceIndexToOpen).getAddress().equals(formatDeviceId)) {
									IvOpenDoorLogo.setEnabled(true);
									IvOpenDoorLogo.setImageResource(R.drawable.selector_pressed);
									IvSearchKey.setBackgroundResource(R.drawable.btn_background_blue);
									TvDistrictDoor.setText(manDoorList.get(i).get("MDdoorName"));
									TvDistrictDoor.setTextSize(18);
									TvDistrictDoor.setTextColor(0xFFffffff);
									TvCarNumber.setText(manDoorList.get(i).get("MDdeviceid"));
									TvCarNumber.setTextColor(0xFFffffff);
								}
							}
						}
					}
				}
			}, SCAN_PERIOD);
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			channelSwitch.setEnabled(false);
			if (getActivity() != null)
				Toast.makeText(getActivity(), R.string.scanning, 3000).show();
		} else {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			channelSwitch.setEnabled(true);
			if (mDeviceList.size() != 0) {
				if (isChooseCarChannel == 1) {
					for (int i = 0; i < carDoorList.size(); i++) {
						String tempDID = carDoorList.get(i).get("CDdeviceid");
						tempDID = tempDID.toUpperCase();
						char[] data = tempDID.toCharArray();
						String formatDeviceId = String.valueOf(data[0]) + String.valueOf(data[1]) + ":"
								+ String.valueOf(data[2]) + String.valueOf(data[3]) + ":"
								+ String.valueOf(data[4]) + String.valueOf(data[5]) + ":"
								+ String.valueOf(data[6]) + String.valueOf(data[7]) + ":"
								+ String.valueOf(data[8]) + String.valueOf(data[9]) + ":"
								+ String.valueOf(data[10]) + String.valueOf(data[11]);
						Log.e("TEST", "CDdeviceID:" + formatDeviceId);

						if (mDeviceList.get(deviceIndexToOpen).getAddress().equals(formatDeviceId)) {
							IvOpenDoorLogo.setEnabled(true);
							IvOpenDoorLogo.setImageResource(R.drawable.selector_pressed);
							IvSearchKey.setBackgroundResource(R.drawable.btn_background_blue);
							TvDistrictDoor.setText(carDoorList.get(i).get("CDdoorName"));
							TvDistrictDoor.setTextSize(18);
							TvDistrictDoor.setTextColor(0xFFffffff);
							TvCarNumber.setText(carDoorList.get(i).get("CDdeviceid"));
							TvCarNumber.setTextColor(0xFFffffff);
						}
					}
				} else {
					for (int i = 0; i < manDoorList.size(); i++) {
						String tempDID = manDoorList.get(i).get("MDdeviceid");
						tempDID = tempDID.toUpperCase();
						char[] data = tempDID.toCharArray();
						String formatDeviceId = String.valueOf(data[0]) + String.valueOf(data[1]) + ":"
								+ String.valueOf(data[2]) + String.valueOf(data[3]) + ":"
								+ String.valueOf(data[4]) + String.valueOf(data[5]) + ":"
								+ String.valueOf(data[6]) + String.valueOf(data[7]) + ":"
								+ String.valueOf(data[8]) + String.valueOf(data[9]) + ":"
								+ String.valueOf(data[10]) + String.valueOf(data[11]);
						Log.e("TEST", "MDdeviceID:" + formatDeviceId);

						if (mDeviceList.get(deviceIndexToOpen).getAddress().equals(formatDeviceId)) {
							IvOpenDoorLogo.setEnabled(true);
							IvOpenDoorLogo.setImageResource(R.drawable.selector_pressed);
							IvSearchKey.setBackgroundResource(R.drawable.btn_background_blue);
							TvDistrictDoor.setText(manDoorList.get(i).get("MDdoorName"));
							TvDistrictDoor.setTextSize(18);
							TvDistrictDoor.setTextColor(0xFFffffff);
							TvCarNumber.setText(manDoorList.get(i).get("MDdeviceid"));
							TvCarNumber.setTextColor(0xFFffffff);
						}
					}
				}
			}
		}
	}

	private void addDevice(BluetoothDevice device, int rssi) {
		Log.e("BLE", "addDevice");
		boolean deviceFound = false;

		for (BluetoothDevice listDev : mDeviceList) {
			if (listDev.getAddress().equals(device.getAddress())) {
				deviceFound = true;
				break;
			}
		}

		if (isChooseCarChannel == 1) {
			for (int i = 0; i < carDoorList.size(); i++) {
				String tempDID = carDoorList.get(i).get("CDdeviceid");
				tempDID = tempDID.toUpperCase();
				char[] data = tempDID.toCharArray();
				String formatDeviceId = String.valueOf(data[0]) + String.valueOf(data[1]) + ":"
						+ String.valueOf(data[2]) + String.valueOf(data[3]) + ":" 
						+ String.valueOf(data[4]) + String.valueOf(data[5]) + ":"
						+ String.valueOf(data[6]) + String.valueOf(data[7]) + ":" 
						+ String.valueOf(data[8]) + String.valueOf(data[9]) + ":"
						+ String.valueOf(data[10]) + String.valueOf(data[11]);
				Log.e("TEST", "CDdeviceID:" + formatDeviceId);

				if (device.getAddress().equals(formatDeviceId)) {
					mDevRssiValues.put(device.getAddress(), rssi);
					if (!deviceFound) {
						Log.e("TEST", "add a car door");
						mDeviceList.add(device);
						mDeviceAdapter.notifyDataSetChanged();
					}
				}
			}
		} else {
			for (int i = 0; i < manDoorList.size(); i++) {
				String tempDID = manDoorList.get(i).get("MDdeviceid");
				tempDID = tempDID.toUpperCase();
				char[] data = tempDID.toCharArray();
				String formatDeviceId = String.valueOf(data[0]) + String.valueOf(data[1]) + ":"
						+ String.valueOf(data[2]) + String.valueOf(data[3]) + ":"
						+ String.valueOf(data[4]) + String.valueOf(data[5]) + ":"
						+ String.valueOf(data[6]) + String.valueOf(data[7]) + ":"
						+ String.valueOf(data[8]) + String.valueOf(data[9]) + ":"
						+ String.valueOf(data[10]) + String.valueOf(data[11]);
				Log.e("TEST", "MDdeviceID:" + formatDeviceId);

				if (device.getAddress().equals(formatDeviceId)) {
					mDevRssiValues.put(device.getAddress(), rssi);
					if (!deviceFound) {
						Log.e("TEST", "add a man door");
						mDeviceList.add(device);
						mDeviceAdapter.notifyDataSetChanged();
					}
				}
			}
		}
	}
	
	private void doOpenDoor() {
		Log.e("BLE", "doOpenDoor");
		IvOpenDoorLogo.setEnabled(false);
		// if(getActivity() != null)
		// Toast.makeText(getActivity(), R.string.door_open,
		// Toast.LENGTH_SHORT).show();

		if (mDeviceList != null && mDeviceList.size() > 0) {
			if (mDeviceList.get(deviceIndexToOpen).getAddress() != null) {
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mUartService.connect(mDeviceList.get(deviceIndexToOpen)
						.getAddress());
//				handler.post(task);   //add for new response
//				if (mUartService != null) {
//					mUartService
//							.readRXCharacteristic(mUartService.SIMPLEPROFILE_CHAR2_UUID);
//				}
//				populateDeviceList();
				
				new Handler().postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                    if(!checkForOpenDoor) {
	                    	Toast.makeText(getActivity(), R.string.open_door_fail, Toast.LENGTH_LONG).show();
	                    	Log.e("test for open door", "fail");
	                    }else{
	                    	checkForOpenDoor = true;
	                    }
	                }
	            }, 3000);
			}
		}
		

	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				byte[] scanRecord) {
			Log.e("BLE", "onLeScan");
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addDevice(device, rssi);
							}
						});
					}

				}
			});
		}
	};

	public void InitFragmentViews() {
		isFindKey = false;

		SharedPreferences setting = getActivity().getSharedPreferences(
				"SETTING", 0);
		isChooseCarChannel = setting.getInt("chooseCar", 1);
		canDisturb = setting.getInt("disturb", 1);
		haveSound = setting.getInt("sound", 1);
		canShake = setting.getInt("shake", 0);

		if (isChooseCarChannel == 1) {
			TvChooseCar.setTextColor(COLOR_CHANNEL_CHOOSE);
			TvChooseMan.setTextColor(COLOR_CHANNEL_NOT_CHOOSE);
			IvChooseCar.setAlpha(alpha_opaque);
			IvChooseMan.setAlpha(alpha_transparent);
		} else {
			TvChooseCar.setTextColor(COLOR_CHANNEL_NOT_CHOOSE);
			TvChooseMan.setTextColor(COLOR_CHANNEL_CHOOSE);
			IvChooseCar.setAlpha(alpha_transparent);
			IvChooseMan.setAlpha(alpha_opaque);
		}

		TvDistrictDoor.setText(R.string.searching_key);
		TvDistrictDoor.setTextSize(18);
		TvDistrictDoor.setTextColor(0xFFffffff);
		TvCarNumber.setText(R.string.can_shake_to_open_door);
		TvCarNumber.setTextColor(0xFFffffff);

		IvSearchKey.setBackgroundResource(R.drawable.btn_gray);
		IvOpenDoorLogo.setImageResource(R.drawable.btn_serch_1);
		IvOpenDoorLogo.setEnabled(false);

		IvWeatherWidgePush1.setImageResource(R.drawable.push_current);
		IvWeatherWidgePush2.setImageResource(R.drawable.push_next);
	}

	public void InitViewPager() {
		mKeyPageFragmentList = new ArrayList<Fragment>();

		mWeatherWidgeFragment = new WeatherWidgeFragment();
		mWeatherWidgeFragment2 = new WeatherWidgeFragment2();

		mKeyPageFragmentList.add(mWeatherWidgeFragment);
		mKeyPageFragmentList.add(mWeatherWidgeFragment2);

		mKeyPageAdapter = new KeyPageAdapter(mFragmentManager,
				mKeyPageFragmentList);
		mWeatherWidgePager.setAdapter(mKeyPageAdapter);
		mWeatherWidgePager.setCurrentItem(0);
		mWeatherWidgePager.setOnPageChangeListener(myPageChangeListener);
	}

	public class MyPageChangeListener implements OnPageChangeListener {

		@Override
		public void onPageScrollStateChanged(int arg0) {
			if (arg0 == 2) {
				int index = mWeatherWidgePager.getCurrentItem();
				mWeatherWidgePager.setCurrentItem(index);
				if (index == 0) {
					IvWeatherWidgePush1
							.setImageResource(R.drawable.push_current);
					IvWeatherWidgePush2.setImageResource(R.drawable.push_next);
				} else if (index == 1) {
					IvWeatherWidgePush2
							.setImageResource(R.drawable.push_current);
					IvWeatherWidgePush1.setImageResource(R.drawable.push_next);
				}
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageSelected(int arg0) {

		}

	}

	private final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.e("BLE", "mBluetoothStateReceiver");
			final String action = intent.getAction();
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = intent.getIntExtra(
						BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

				switch (state) {
				case BluetoothAdapter.STATE_OFF:
					Log.e("BLE", "BluetoothAdapter.STATE_OFF");
					break;

				case BluetoothAdapter.STATE_TURNING_OFF:
					Log.e("BLE", "BluetoothAdapter.STATE_TURNING_OFF");
					break;

				case BluetoothAdapter.STATE_ON:
					Log.e("BLE", "BluetoothAdapter.STATE_ON");
					populateDeviceList();
					break;

				case BluetoothAdapter.STATE_TURNING_ON:
					Log.e("BLE", "BluetoothAdapter.STATE_TURNING_ON");
					break;
				}
			}
		}
	};

	private static IntentFilter makeGattUpdateIntentFilter() {
		Log.e("BLE", "makeGattUpdateIntentFilter");
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
		intentFilter.addAction(UartService.ACTION_MAKESURE_DOOROPENED);
		return intentFilter;
	}
	
//	private Handler handler = new Handler();
//
//	private Runnable task = new Runnable() {
//		public void run() {
//			// TODOAuto-generated method stub
//			handler.postDelayed(this, 2 * 100);// �����ӳ�ʱ�䣬�˴���0.2��
//			// ��Ҫִ�еĴ���
//			if (mUartService != null) {
//                mUartService.readRXCharacteristic(mUartService.SIMPLEPROFILE_CHAR2_UUID);
//            }
//			Log.e("TEst for response", "print");
//		}
//	};
	
	private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			Log.e("BLE", "UARTStatusChangeReceiver");
			String action = intent.getAction();

			if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
				Log.e("BLE", "UartService.ACTION_GATT_CONNECTED");
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
					}
				});
			}

			if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
				Log.e("BLE", "UartService.ACTION_GATT_SERVICES_DISCOVERED");
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						if (mUartService != null) {
							mUartService.readRXCharacteristic(mUartService.RX_CHAR_UUID);
						}
					}
				});
			}

			if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
				Log.e("BLE", "UartService.ACTION_GATT_DISCONNECTED");
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						IvOpenDoorLogo.setEnabled(true);
						// mUartService.disconnect();
						mUartService.close();
					}
				});
			}

			if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
				Log.e("BLE", "UartService.ACTION_GATT_SERVICES_DISCOVERED");
				mUartService.enableTXNotification();
			}

			if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
				Log.e("BLE", "UartService.ACTION_DATA_AVAILABLE");

				@SuppressWarnings("unused")
				final byte[] txValue = intent
						.getByteArrayExtra(UartService.EXTRA_DATA);
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						if (mUartService != null) {
							String message = new String(
									Character.toChars(new Random()
											.nextInt(90 - 65) + 65));
							try {
								byte[] value = message.getBytes("UTF-8");
								mUartService.writeRXCharacteristic(value);
							} catch (Exception e) {
							}
						}
					}
				});
			}

			if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
				Log.e("BLE", "UartService.DEVICE_DOES_NOT_SUPPORT_UART");
				mUartService.disconnect();
			}
			
			//new add for response
			if (action.equals(UartService.ACTION_MAKESURE_DOOROPENED)) {
                Log.e("BLE", "UartService.ACTION_MAKESURE_DOOROPENED");
                final byte[] txValue = intent
                        .getByteArrayExtra(UartService.EXTRA_DATA);
//                getActivity().runOnUiThread(new Runnable() {
//					public void run() {
						if (txValue[0] == 0x10) {
		                    // door had opened. go on ...
							
							checkForOpenDoor = true;
							Toast.makeText(getActivity(), R.string.open_door_success, Toast.LENGTH_LONG).show();
		                    Log.e("BLE", "door had opened.");
		                }
//					}
//                });
            }

		}
	};
	
	

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder rawBinder) {
			Log.e("BLE", "onServiceConnected");
			mUartService = ((UartService.LocalBinder) rawBinder).getService();
			if (!mUartService.initialize()) {
				getActivity().finish();
			}

		}

		public void onServiceDisconnected(ComponentName classname) {
			Log.e("BLE", "onServiceDisconnected");
			mUartService = null;

		}
	};

	private class DeviceAdapter extends BaseAdapter {
		private Context context;
		private List<BluetoothDevice> devices;

		public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
			this.context = context;
			this.devices = devices;
		}

		@Override
		public int getCount() {
			int ret = 0;
			if (devices != null && devices.size() > 0) {
				ret = devices.size();
			}
			return ret;
		}

		@Override
		public Object getItem(int position) {
			return devices.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return null;
		}

	}

	@Override
	public void onShake() {
		if (canShake == 1) {
			Log.e("TEST", "shaking");
			doOpenDoor();
		}
	}

	public void playOpenDoorSound() {
		mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		mSoundPool.play(mSoundPool.load(getActivity(), R.raw.ring, 0), 1, 1, 0,
				0, 1);
	}

	// �������ݱ�KeyInfoTable�����¼������
	private long DBCount() {
		String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
		SQLiteStatement statement = mKeyDB.compileStatement(sql);
		long count = statement.simpleQueryForLong();
		return count;
	}
	
	@Override
    public void onDetach() {
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        super.onDetach();

    }
	
	//for test
	public void saveSid(String sid) {
		SharedPreferences savedSid = getActivity().getSharedPreferences("SAVEDSID", 0);
		Editor editor = savedSid.edit();
		editor.putString("SID", sid);
		editor.commit();
	}

	public String loadSid() {
		SharedPreferences loadSid = getActivity().getSharedPreferences("SAVEDSID", 0);
		return loadSid.getString("SID", null);
	}
}
