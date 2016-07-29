package com.yiyang.reactnativebaidumap;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yiyang on 16/2/29.
 */
public class ReactMapView {

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

    public ReactMapView(MapView mapView) {
        this.mMapView = mapView;
        mMapView.getMap().setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                System.out.println(marker.getPosition());
                return false;
            }
        });
    }

    public BaiduMap getMap() {
        return this.mMapView.getMap();
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
            for (ReactMapOverlay overlay:
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
            for (ReactMapMarker marker:
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

                        float radius = 0;
                        if (mConfiguration != null && mConfiguration.isShowAccuracyCircle()) {
                            radius = bdLocation.getRadius();
                        }
                        MyLocationData locData = new MyLocationData.Builder()
                                .accuracy(radius)
                                .latitude(bdLocation.getLatitude())
                                .longitude(bdLocation.getLongitude())
                                .build();
                        //getMap().setMyLocationData(locData);
                       // mMapView.getMap().setMyLocationData(locData);
                        if (getMap().isMyLocationEnabled()) {
//                            /*LatLng cenpt = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
//                            //定义地图状态
//                            MapStatus mMapStatus = new MapStatus.Builder()
//                                    .target(cenpt)
//                                    .zoom(18)
//                                    .build();
//                            //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
//
//
//                            MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
//                            //改变地图状态
//                            getMap().setMapStatus(mMapStatusUpdate);*/
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
}
