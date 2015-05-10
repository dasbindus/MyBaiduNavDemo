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

	// 地图相关
	MapView mMapView = null;
	BaiduMap mBaiduMap = null;
	// 定位相关
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	boolean isFirstLoc = true;// 是否首次定位

	// POI相关
	private PoiSearch mPoiSearch = null;
	private int poiLoad_index = 0;
	private SearchView searchView;

	// 导航相关
	private LatLng startLatLng;
	private LatLng endLatLng;

	// private RoutePlanModel mRoutePlanModel = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 初始化导航引擎，校验key
		BaiduNaviManager.getInstance().initEngine(this, getsdcardDir(),
				mNaviEngineInitListener, new LBSAuthManagerListener() {
					@Override
					public void onAuthResult(int status, String msg) {
						String str = null;
						if (0 == status) {
							str = "校验成功！";
						} else {
							str = "校验失败！" + msg;
						}
						Toast.makeText(MainActivity.this, str,
								Toast.LENGTH_SHORT).show();
					}
				});

		setActionBar();

		// 地图初始化
		mMapView = (MapView) findViewById(R.id.mapView);
		mBaiduMap = mMapView.getMap();
		// 缩放级别3~18{"50m","100m","200m","500m","1km",
		// "2km","5km","10km","20km","25km","50km","100km",
		// "200km","500km","1000km","2000km"}
		mBaiduMap.setMapStatus(MapStatusUpdateFactory
				.newMapStatus(new MapStatus.Builder().zoom(17).build()));

		// ------------------定位模块------------------------------
		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		// 定位初始化
		mLocClient = new LocationClient(getApplicationContext());
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开gps
		option.setCoorType("bd09ll"); // 设置坐标类型
		option.setScanSpan(300);
		mLocClient.setLocOption(option);
		mLocClient.start();

		// -----------------POI搜索模块-----------------------------
		// 初始化搜索模块，注册搜索事件的监听器
		mPoiSearch = PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(this);
	}

	/**
	 * 配置ActionBar
	 */
	private void setActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setIcon(R.drawable.icon);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setTitle("搜索周边");
	}

	/**
	 * 获取Sd卡路径
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
	 * 导航引擎初始化监听
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
	 * 定位SDK监听函数
	 */
	public class MyLocationListenner implements BDLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view 销毁后不在处理新接收的位置
			if (location == null || mMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
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
		// 退出时销毁定位
		mLocClient.stop();
		// 退出时销毁POI搜索
		mPoiSearch.destroy();
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroy();
	}

	/**
	 * 添加Menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		searchView = (SearchView) menu.findItem(R.id.action_search)
				.getActionView();
		searchView.setOnQueryTextListener(MainActivity.this);
		searchView.setQueryHint("查找");
		searchView.setFocusable(false);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * ActionBar的OverFlow单击事件
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// 为actionBar添加单击事件
		switch (item.getItemId()) {
		case R.id.action_search:
//			setSearchView(item);
			break;
		case R.id.action_search_school:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("学校"));
			break;
		case R.id.action_search_4s:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("4s店"));
			break;
		case R.id.action_search_restaurant:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("餐馆"));
			break;
		case R.id.action_search_gas:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("加油站"));
			break;
		case R.id.action_search_hotel:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("宾馆"));
			break;
		case R.id.action_search_mall:
			mBaiduMap.clear();
			mPoiSearch.searchNearby(setOption("商场"));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

//	/**
//	 * 设置SearchView
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
	 * 获取POI结果的详细信息
	 */
	@Override
	public void onGetPoiDetailResult(PoiDetailResult detailResult) {
		System.out.println(detailResult.error);
		if (detailResult.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
					.show();
		} else {
			endLatLng = detailResult.getLocation();// 获取终点坐标
			System.out.println("终点位置坐标：" + endLatLng);

			AlertDialog.Builder builder = new AlertDialog.Builder(
					MainActivity.this);
			builder.setTitle("详细信息")
					.setIcon(R.drawable.ic_menu_search)
					.setMessage(
							detailResult.getName() + ": "
									+ detailResult.getAddress())
					.setPositiveButton("就去这儿", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							// 算路
							// calcRoute(NL_Net_Mode.NL_Net_Mode_OnLine);

							// 开始导航
							// ------真实导航----------
							launchNavigator(true);
							// ------模拟导航----------
							// launchNavigator(false);
						}
					}).setNegativeButton("换个地方", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(MainActivity.this, "取消！",
									Toast.LENGTH_SHORT).show();
						}
					}).create().show();
		}
	}

	/**
	 * 获取POI检索的结果
	 */
	@Override
	public void onGetPoiResult(PoiResult result) {
		if (result == null
				|| result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			Toast.makeText(MainActivity.this, "未找到结果！", Toast.LENGTH_SHORT)
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
			Toast.makeText(MainActivity.this, "检索词有歧义！", Toast.LENGTH_SHORT)
					.show();
		}
	}

	// 重写PoiOverlay的onPoiClick()方法
	private class MyPoiOverlay extends PoiOverlay {

		public MyPoiOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}

		/**
		 * @param index
		 *            被点击的POI在 PoiResult.getAllPoi() 中的索引
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
	 * 设置PoiNearbySearchOption
	 */
	private PoiNearbySearchOption setOption(String keyword) {
		PoiNearbySearchOption searchOption = new PoiNearbySearchOption();
		startLatLng = new LatLng(mLocClient.getLastKnownLocation()
				.getLatitude(), mLocClient.getLastKnownLocation()
				.getLongitude());
		System.out.println("起始位置坐标：" + startLatLng);

		searchOption.keyword(keyword).pageNum(poiLoad_index).radius(5000)
		// 中心坐标可由定位得到
				.location(startLatLng);
		return searchOption;
	}

	public void goNav(View v) {
		// startNavi(true);
		launchNavigator(true);
	}

	/**
	 * 指定导航起终点启动GPS导航.起终点可为多种类型坐标系的地理坐标。 前置条件：导航引擎初始化成功
	 * 
	 * @param isReal
	 *            true:真实导航 false:模拟导航
	 */
	private void launchNavigator(boolean isReal) {
		// 这里给出一个起终点示例，实际应用中可以通过POI检索、外部POI来源等方式获取起终点坐标
		BNaviPoint startPoint = new BNaviPoint(startLatLng.longitude,
				startLatLng.latitude, "起点", BNaviPoint.CoordinateType.BD09_MC);
		BNaviPoint endPoint = new BNaviPoint(endLatLng.longitude,
				endLatLng.latitude, "终点", BNaviPoint.CoordinateType.BD09_MC);
		BaiduNaviManager.getInstance().launchNavigator(this, startPoint, // 起点（可指定坐标系）
				endPoint, // 终点（可指定坐标系）
				NE_RoutePlan_Mode.ROUTE_PLAN_MOD_MIN_TIME, // 算路方式
				isReal, // 真实导航
				BaiduNaviManager.STRATEGY_FORCE_ONLINE_PRIORITY, // 在离线策略
				new OnStartNavigationListener() { // 跳转监听

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
		//隐藏键盘
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);  
		imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS); 
		//清空上一次地图显示
		mBaiduMap.clear();
		mPoiSearch.searchNearby(setOption(query));
		return false;
	}
}
