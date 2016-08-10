package com.yiyang.reactnativebaidumap;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yiyang on 16/2/29.
 */
public class ReactMapView implements OnGetRoutePlanResultListener {

    private MapView mMapView;

    private LocationClient mLocationClient;

    private ReactMapMyLocationConfiguration mConfiguration;

    private boolean autoZoomToSpan;
    boolean isFirstLoc = true;

    public boolean isAutoZoomToSpan() {
        return autoZoomToSpan;
    }

    public void setAutoZoomToSpan(boolean autoZoomToSpan) {
        this.autoZoomToSpan = autoZoomToSpan;
    }

    private List<ReactMapMarker> mMarkers = new ArrayList<ReactMapMarker>();
    private List<String> mMarkerIds = new ArrayList<String>();

    private List<ReactMapOverlay> mOverlays = new ArrayList<ReactMapOverlay>();
    private List<String> mOverlayIds = new ArrayList<String>();

    // 搜索相关
    RoutePlanSearch mSearch = null;
    TransitRouteResult nowResult = null;
    DrivingRouteResult nowResultd = null;
    boolean useDefaultIcon = false;
    RouteLine route = null;
    OverlayManager routeOverlay = null;
    int nodeIndex = -1; // 节点索引,供浏览节点时使用

    public ReactMapView(MapView mapView) {
        super();
        this.mMapView = mapView;
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
    }

    public BaiduMap getMap() {
        return this.mMapView.getMap();
    }

    public int getWidth() {
        return this.mMapView.getWidth();
    }

    public int getHeight() {
        return this.mMapView.getHeight();
    }

    public void navi(MapView mapView, String endCity) {
        PlanNode stNode = PlanNode.withCityNameAndPlaceName("成都", "红牌楼广场2号写字楼");
        PlanNode enNode = PlanNode.withCityNameAndPlaceName("成都", "四川大学江安校区");
        mSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(stNode).to(enNode));

    }

    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        this.mMapView.addView(view, layoutParams);
    }


    public void setOverlays(List<ReactMapOverlay> overlays) {
        List<String> newOverlayIds = new ArrayList<String>();
        List<ReactMapOverlay> overlaysToDelete = new ArrayList<ReactMapOverlay>();
        List<ReactMapOverlay> overlaysToAdd = new ArrayList<ReactMapOverlay>();

        for (ReactMapOverlay overlay :
                overlays) {
            if (overlay instanceof ReactMapOverlay == false) {
                continue;
            }

            newOverlayIds.add(overlay.getId());

            if (!mOverlayIds.contains(overlay.getId())) {
                overlaysToAdd.add(overlay);
            }
        }

        for (ReactMapOverlay overlay :
                this.mOverlays) {
            if (overlay instanceof ReactMapOverlay == false) {
                continue;
            }

            if (!newOverlayIds.contains(overlay.getId())) {
                overlaysToDelete.add(overlay);
            }
        }

        if (!overlaysToDelete.isEmpty()) {
            for (ReactMapOverlay overlay :
                    overlaysToDelete) {
                overlay.getPolyline().remove();
                this.mOverlays.remove(overlay);
            }
        }

        if (!overlaysToAdd.isEmpty()) {
            for (ReactMapOverlay overlay :
                    overlaysToAdd) {
                if (overlay.getOptions() != null) {
                    overlay.addToMap(this.getMap());
                    this.mOverlays.add(overlay);
                }
            }
        }

        this.mOverlayIds = newOverlayIds;

    }

    public void setMarker(List<ReactMapMarker> markers) {

        List<String> newMarkerIds = new ArrayList<String>();
        List<ReactMapMarker> markersToDelete = new ArrayList<ReactMapMarker>();
        List<ReactMapMarker> markersToAdd = new ArrayList<ReactMapMarker>();

        for (ReactMapMarker marker :
                markers) {
            if (marker instanceof ReactMapMarker == false) {
                continue;
            }

            newMarkerIds.add(marker.getId());

            if (!mMarkerIds.contains(marker.getId())) {
                markersToAdd.add(marker);
            }
        }

        for (ReactMapMarker marker :
                this.mMarkers) {
            if (marker instanceof ReactMapMarker == false) {
                continue;
            }

            if (!newMarkerIds.contains(marker.getId())) {
                markersToDelete.add(marker);
            }
        }

        if (!markersToDelete.isEmpty()) {
            for (ReactMapMarker marker :
                    markersToDelete) {
                marker.getMarker().remove();
                this.mMarkers.remove(marker);
            }
        }

        if (!markersToAdd.isEmpty()) {
            for (ReactMapMarker marker :
                    markersToAdd) {
                if (marker.getOptions() != null) {
                    marker.addToMap(this.getMap());
                    this.mMarkers.add(marker);
                }
            }
        }

        this.mMarkerIds = newMarkerIds;
    }


    public void onMapLoaded() {
        if (this.autoZoomToSpan) {
            this.zoomToSpan();
        }
    }

    public void zoomToSpan(List<ReactMapMarker> markers, List<ReactMapOverlay> overlays) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasBuilded = false;
        if (markers != null && markers.size() > 0) {
            for (ReactMapMarker marker :
                    markers) {
                if (marker != null && marker.getOptions() != null) {
                    LatLng location = marker.getOptions().getPosition();
                    builder.include(location);
                    hasBuilded = true;
                }
            }
        }
        if (overlays != null && overlays.size() > 0) {
            for (ReactMapOverlay overlay :
                    overlays) {
                if (overlay != null && overlay.getOptions() != null) {
                    for (LatLng location :
                            overlay.getOptions().getPoints()) {
                        builder.include(location);
                        hasBuilded = true;
                    }
                }
            }
        }
        if (hasBuilded) {
            this.getMap().animateMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()));
        }
    }

    public void zoomToSpan() {
        this.zoomToSpan(this.mMarkers, this.mOverlays);
    }

    public void setShowsUserLocation(boolean showsUserLocation) {
        if (getMap() == null) {
            return;
        }
        if (showsUserLocation != getMap().isMyLocationEnabled()) {
            getMap().setMyLocationEnabled(showsUserLocation);
            if (showsUserLocation && mLocationClient == null) {
                mLocationClient = new LocationClient(mMapView.getContext());
                BaiduLocationListener listener = new BaiduLocationListener(mLocationClient, new BaiduLocationListener.ReactLocationCallback() {
                    @Override
                    public void onSuccess(BDLocation bdLocation) {
                        //定位成功之后即返回经纬度和比例尺
                        WritableMap event = Arguments.createMap();
                        event.putDouble("latitude", bdLocation.getLatitude());
                        event.putDouble("longitude", bdLocation.getLongitude());
                        event.putInt("scale", mMapView.getMapLevel());
                        sendEvent((ReactContext) mMapView.getContext(), "OnLocationSuccess", event);
                        float radius = 0;
                        if (mConfiguration != null && mConfiguration.isShowAccuracyCircle()) {
                            radius = bdLocation.getRadius();
                        }
                        MyLocationData locData = new MyLocationData.Builder()
                                .accuracy(radius)
                                .latitude(bdLocation.getLatitude())
                                .longitude(bdLocation.getLongitude())
                                .build();
                        if (getMap().isMyLocationEnabled()) {
                            getMap().setMyLocationData(locData);
                            if (isFirstLoc) {
                                isFirstLoc = false;
                                LatLng ll = new LatLng(bdLocation.getLatitude(),
                                        bdLocation.getLongitude());
                                MapStatus.Builder builder = new MapStatus.Builder();
                                builder.target(ll).zoom(18.0f);
                                getMap().animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                            }
                        }
                    }

                    @Override
                    public void onFailure(BDLocation bdLocation) {
                        Log.e("RNBaidumap", "error: " + bdLocation.getLocType());
                    }
                });
                LocationClientOption option = new LocationClientOption();
                option.setOpenGps(true); // 打开gps
                option.setCoorType("bd09ll"); // 设置坐标类型
                option.setScanSpan(1000);
                mLocationClient.setLocOption(option);
                mLocationClient.registerLocationListener(listener);
                mLocationClient.start();
            } else if (showsUserLocation) {
                if (mLocationClient.isStarted()) {
                    mLocationClient.requestLocation();
                } else {
                    mLocationClient.start();
                }
            } else if (mLocationClient != null) {
                if (mLocationClient.isStarted()) {
                    mLocationClient.stop();
                }
            }
        }
    }

    public void zoomBack() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(mMapView.getContext());
            BaiduLocationListener listener = new BaiduLocationListener(mLocationClient, new BaiduLocationListener.ReactLocationCallback() {
                @Override
                public void onSuccess(BDLocation bdLocation) {
                    System.out.println("================================="+"定位成功"+"============"+"================================");
                    //定位成功之后即返回经纬度和比例尺
                    WritableMap event = Arguments.createMap();
                    event.putDouble("latitude", bdLocation.getLatitude());
                    event.putDouble("longitude", bdLocation.getLongitude());
                    event.putInt("scale", mMapView.getMapLevel());
                    sendEvent((ReactContext) mMapView.getContext(), "OnLocationSuccess", event);
                    float radius = 0;
                    //if (mConfiguration != null && mConfiguration.isShowAccuracyCircle()) {
                        radius = bdLocation.getRadius();
                   // }
                    MyLocationData locData = new MyLocationData.Builder()
                            .accuracy(radius)
                            .latitude(bdLocation.getLatitude())
                            .longitude(bdLocation.getLongitude())
                            .build();
                   // if (getMap().isMyLocationEnabled()) {
                        getMap().setMyLocationData(locData);
                       // if (isFirstLoc) {
                            isFirstLoc = false;
                            LatLng ll = new LatLng(bdLocation.getLatitude(),
                                    bdLocation.getLongitude());
                    System.out.println("================================="+ll.toString()+"============"+"================================");
                            MapStatus.Builder builder = new MapStatus.Builder();
                            builder.target(ll).zoom(18.0f);
                            getMap().animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                       // }
                 //   }
                }

                @Override
                public void onFailure(BDLocation bdLocation) {
                    Log.e("RNBaidumap", "error: " + bdLocation.getLocType());
                }
            });
            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true); // 打开gps
            option.setCoorType("bd09ll"); // 设置坐标类型
           // option.setScanSpan(1000);
            mLocationClient.setLocOption(option);
            mLocationClient.registerLocationListener(listener);
            mLocationClient.start();
        }else {
            LatLng ll = new LatLng(mLocationClient.getLastKnownLocation().getLatitude(),
                    mLocationClient.getLastKnownLocation().getLongitude());
            System.out.println("================================="+ll.toString()+"============"+"================================");
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(ll).zoom(18.0f);
            getMap().animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        try {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void setConfiguration(ReactMapMyLocationConfiguration configuration) {
        this.mConfiguration = configuration;
        this.mConfiguration.setConfigurationUpdateListener(new ReactMapMyLocationConfiguration.ConfigurationUpdateListener() {
            @Override
            public void onConfigurationUpdate(ReactMapMyLocationConfiguration aConfiguration) {
                if (getMap() != null) {
                    getMap().setMyLocationConfigeration(aConfiguration.getConfiguration());
                }
            }
        });
        if (getMap() != null) {
            getMap().setMyLocationConfigeration(configuration.getConfiguration());
        }
    }

    private LocationClientOption getLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(1000);
        option.setCoorType("bd09ll");
        return option;
    }

    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

    }

    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(mMapView.getContext(), "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;

            if (result.getRouteLines().size() > 1) {
                nowResultd = result;

                MyTransitDlg myTransitDlg = new MyTransitDlg(mMapView.getContext(),
                        result.getRouteLines(),
                        RouteLineAdapter.Type.DRIVING_ROUTE);
                myTransitDlg.setOnItemInDlgClickLinster(new OnItemInDlgClickListener() {
                    public void onItemClick(int position) {
                        route = nowResultd.getRouteLines().get(position);
                        DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mMapView.getMap());
                        mMapView.getMap().setOnMarkerClickListener(overlay);
                        routeOverlay = overlay;
                        overlay.setData(nowResultd.getRouteLines().get(position));
                        overlay.addToMap();
                        overlay.zoomToSpan();
                    }

                });
                myTransitDlg.show();

            } else if (result.getRouteLines().size() == 1) {
                route = result.getRouteLines().get(0);
                DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mMapView.getMap());
                routeOverlay = overlay;
                mMapView.getMap().setOnMarkerClickListener(overlay);
                overlay.setData(result.getRouteLines().get(0));
                overlay.addToMap();
                overlay.zoomToSpan();
            }

        }
    }

    // 定制RouteOverly
    private class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
            }
            return null;
        }
    }

    @Override
    public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

    }

    // 响应DLg中的List item 点击
    interface OnItemInDlgClickListener {
        public void onItemClick(int position);
    }

    // 供路线选择的Dialog
    class MyTransitDlg extends Dialog {

        private List<? extends RouteLine> mtransitRouteLines;
        private ListView transitRouteList;
        private RouteLineAdapter mTransitAdapter;

        OnItemInDlgClickListener onItemInDlgClickListener;

        public MyTransitDlg(Context context, int theme) {
            super(context, theme);
        }

        public MyTransitDlg(Context context, List<? extends RouteLine> transitRouteLines, RouteLineAdapter.Type
                type) {
            this(context, 0);
            mtransitRouteLines = transitRouteLines;
            mTransitAdapter = new RouteLineAdapter(context, mtransitRouteLines, type);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_transit_dialog);

            transitRouteList = (ListView) findViewById(R.id.transitList);
            transitRouteList.setAdapter(mTransitAdapter);

            transitRouteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onItemInDlgClickListener.onItemClick(position);
                    dismiss();

                }
            });
        }

        public void setOnItemInDlgClickLinster(OnItemInDlgClickListener itemListener) {
            onItemInDlgClickListener = itemListener;
        }

    }
}

