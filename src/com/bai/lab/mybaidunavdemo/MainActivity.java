package com.bai.lab.mybaidunavdemo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;

import com.baidu.lbsapi.auth.LBSAuthManagerListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.navisdk.BNaviEngineManager.NaviEngineInitListener;
import com.baidu.navisdk.BNaviPoint;
import com.baidu.navisdk.BaiduNaviManager;
import com.baidu.navisdk.BaiduNaviManager.OnStartNavigationListener;
import com.baidu.navisdk.comapi.routeplan.RoutePlanParams.NE_RoutePlan_Mode;

public class MainActivity extends Activity implements
		OnGetPoiSearchResultListener, SearchView.OnQueryTextListener {

	// ��ͼ���
	MapView mMapView = null;
	BaiduMap mBaiduMap = null;
	// ��λ���
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	boolean isFirstLoc = true;// �Ƿ��״ζ�λ

	// POI���
	private PoiSearch mPoiSearch = null;
	private int poiLoad_index = 0;
	private SearchView searchView;

	// �������
	private LatLng startLatLng;
	private LatLng endLatLng;

	// private RoutePlanModel mRoutePlanModel = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ��ʼ���������棬У��key
		BaiduNaviManager.getInstance().initEngine(this, getsdcardDir(),
				mNaviEngineInitListener, new LBSAuthManagerListener() {
					@Override
					public void onAuthResult(int status, String msg) {
						String str = null;
						if (0 == status) {
							str = "У��ɹ���";
						} else {
							str = "У��ʧ�ܣ�" + msg;
						}
						Toast.makeText(MainActivity.this, str,
								Toast.LENGTH_SHORT).show();
					}
				});

		setActionBar();

		// ��ͼ��ʼ��
		mMapView = (MapView) findViewById(R.id.mapView);
		mBaiduMap = mMapView.getMap();
		// ���ż���3~18{"50m","100m","200m","500m","1km",
		// "2km","5km","10km","20km","25km","50km","100km",
		// "200km","500km","1000km","2000km"}
		mBaiduMap.setMapStatus(MapStatusUpdateFactory
				.newMapStatus(new MapStatus.Builder().zoom(17).build()));

		// ------------------��λģ��------------------------------
		// ������λͼ��
		mBaiduMap.setMyLocationEnabled(true);
		// ��λ��ʼ��
		mLocClient = new LocationClient(getApplicationContext());
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// ��gps
		option.setCoorType("bd09ll"); // ������������
		option.setScanSpan(300);
		mLocClient.setLocOption(option);
		mLocClient.start();

		// -----------------POI����ģ��-----------------------------
		// ��ʼ������ģ�飬ע�������¼��ļ�����
		mPoiSearch = PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(this);
	}

	/**
	 * ����ActionBar
	 */
	private void setActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setIcon(R.drawable.icon);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setTitle("�����ܱ�");
	}

	/**
	 * ��ȡSd��·��
	 * 
	 * @return
	 */
	private String getsdcardDir() {
		if (Environment.getExternalStorageState().equalsIgnoreCase(
				Environment.MEDIA_MOUNTED)) {
			return Environment.getExternalStorageDirectory().toString();
		}
		return null;
	}

	/**
	 * ���������ʼ������
	 */
	private NaviEngineInitListener mNaviEngineInitListener = new NaviEngineInitListener() {

		@Override
		public void engineInitSuccess() {
			// mIsEngineInitSuccess = true;
		}

		@Override
		public void engineInitStart() {

		}

		@Override
		public void engineInitFail() {

		}
	};

	/**
	 * ��λSDK��������
	 */
	public class MyLocationListenner implements BDLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view ���ٺ��ڴ����½��յ�λ��
			if (location == null || mMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// �˴����ÿ����߻�ȡ���ķ�����Ϣ��˳ʱ��0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			if (isFirstLoc) {
				isFirstLoc = false;
				LatLng ll = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
				mBaiduMap.animateMapStatus(u);
			}
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// �˳�ʱ���ٶ�λ
		mLocClient.stop();
		// �˳�ʱ����POI����
		mPoiSearch.destroy();
		// �رն�λͼ��
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroy();
	}

	/**
	 * ���Menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		searchView = (SearchView) menu.findItem(R.id.action_search)
				.getActionView();
		searchView.setOnQueryTextListener(MainActivity.this);
		searchView.setQueryHint("����");
		searchView.setFocusable(false);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * ActionBar��OverFlow�����¼�
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// ΪactionBar��ӵ����¼�
		switch (item.getItemId()) {
		case R.id.action_search:
//			setSearchView(item);
			break;
		case R.id.action_search_school:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("ѧУ"));
			break;
		case R.id.action_search_4s:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("4s��"));
			break;
		case R.id.action_search_restaurant:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("�͹�"));
			break;
		case R.id.action_search_gas:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("����վ"));
			break;
		case R.id.action_search_hotel:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("����"));
			break;
		case R.id.action_search_mall:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("�̳�"));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

//	/**
//	 * ����SearchView
//	 */
//	public void setSearchView(MenuItem item) {
//		SearchView searchView = (SearchView) item.getActionView();
//		if (searchView == null)
//			return;
//		searchView.setIconifiedByDefault(true);
//		// SearchManager searchManager = (SearchManager)
//		// getSystemService(Context.SEARCH_SERVICE);
//
//	}

	/**
	 * ��ȡPOI�������ϸ��Ϣ
	 */
	@Override
	public void onGetPoiDetailResult(PoiDetailResult detailResult) {
		System.out.println(detailResult.error);
		if (detailResult.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(MainActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_SHORT)
					.show();
		} else {
			endLatLng = detailResult.getLocation();// ��ȡ�յ�����
			System.out.println("�յ�λ�����꣺" + endLatLng);

			AlertDialog.Builder builder = new AlertDialog.Builder(
					MainActivity.this);
			builder.setTitle("��ϸ��Ϣ")
					.setIcon(R.drawable.ic_menu_search)
					.setMessage(
							detailResult.getName() + ": "
									+ detailResult.getAddress())
					.setPositiveButton("��ȥ���", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							// ��·
							// calcRoute(NL_Net_Mode.NL_Net_Mode_OnLine);

							// ��ʼ����
							// ------��ʵ����----------
							launchNavigator(true);
							// ------ģ�⵼��----------
							// launchNavigator(false);
						}
					}).setNegativeButton("�����ط�", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(MainActivity.this, "ȡ����",
									Toast.LENGTH_SHORT).show();
						}
					}).create().show();
		}
	}

	/**
	 * ��ȡPOI�����Ľ��
	 */
	@Override
	public void onGetPoiResult(PoiResult result) {
		if (result == null
				|| result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			Toast.makeText(MainActivity.this, "δ�ҵ������", Toast.LENGTH_SHORT)
					.show();
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			PoiOverlay poiOverlay = new MyPoiOverlay(mBaiduMap);
			mBaiduMap.setOnMarkerClickListener(poiOverlay);
			poiOverlay.setData(result);
			poiOverlay.addToMap();
			poiOverlay.zoomToSpan();
			return;
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {
			Toast.makeText(MainActivity.this, "�����������壡", Toast.LENGTH_SHORT)
					.show();
		}
	}

	// ��дPoiOverlay��onPoiClick()����
	private class MyPoiOverlay extends PoiOverlay {

		public MyPoiOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}

		/**
		 * @param index
		 *            �������POI�� PoiResult.getAllPoi() �е�����
		 */
		@Override
		public boolean onPoiClick(int index) {
			super.onPoiClick(index);
			PoiInfo poiInfo = getPoiResult().getAllPoi().get(index);
			mPoiSearch.searchPoiDetail(new PoiDetailSearchOption()
					.poiUid(poiInfo.uid));
			return true;
		}

	}

	/**
	 * ����PoiNearbySearchOption
	 */
	private PoiNearbySearchOption setOption(String keyword) {
		PoiNearbySearchOption searchOption = new PoiNearbySearchOption();
		startLatLng = new LatLng(mLocClient.getLastKnownLocation()
				.getLatitude(), mLocClient.getLastKnownLocation()
				.getLongitude());
		System.out.println("��ʼλ�����꣺" + startLatLng);

		searchOption.keyword(keyword).pageNum(poiLoad_index).radius(5000)
		// ����������ɶ�λ�õ�
				.location(startLatLng);
		return searchOption;
	}

	public void goNav(View v) {
		// startNavi(true);
		launchNavigator(true);
	}

	/**
	 * ָ���������յ�����GPS����.���յ��Ϊ������������ϵ�ĵ������ꡣ ǰ�����������������ʼ���ɹ�
	 * 
	 * @param isReal
	 *            true:��ʵ���� false:ģ�⵼��
	 */
	private void launchNavigator(boolean isReal) {
		// �������һ�����յ�ʾ����ʵ��Ӧ���п���ͨ��POI�������ⲿPOI��Դ�ȷ�ʽ��ȡ���յ�����
		BNaviPoint startPoint = new BNaviPoint(startLatLng.longitude,
				startLatLng.latitude, "���", BNaviPoint.CoordinateType.BD09_MC);
		BNaviPoint endPoint = new BNaviPoint(endLatLng.longitude,
				endLatLng.latitude, "�յ�", BNaviPoint.CoordinateType.BD09_MC);
		BaiduNaviManager.getInstance().launchNavigator(this, startPoint, // ��㣨��ָ������ϵ��
				endPoint, // �յ㣨��ָ������ϵ��
				NE_RoutePlan_Mode.ROUTE_PLAN_MOD_MIN_TIME, // ��·��ʽ
				isReal, // ��ʵ����
				BaiduNaviManager.STRATEGY_FORCE_ONLINE_PRIORITY, // �����߲���
				new OnStartNavigationListener() { // ��ת����

					@Override
					public void onJumpToNavigator(Bundle configParams) {
						Intent intent = new Intent(MainActivity.this,
								BNavigatorActivity.class);
						intent.putExtras(configParams);
						startActivity(intent);
					}

					@Override
					public void onJumpToDownloader() {
					}
				});
	}

	@Override
	public boolean onQueryTextChange(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		//���ؼ���
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);  
		imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS); 
		//�����һ�ε�ͼ��ʾ
		mBaiduMap.clear();
		mPoiSearch.searchNearby(setOption(query));
		return false;
	}
}
