package com.yiyang.reactnativebaidumap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.os.Bundle;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ReadableMap;

/**
 * Created by yiyang on 16/3/1.
 */
public class ReactMapMarker {
    private Marker mMarker;
    private MarkerOptions mOptions;

    private String id;

    private Context mContext;

    public static BitmapDescriptor defaultIcon = BitmapDescriptorFactory.fromResource(R.drawable.location);


    private BitmapDescriptor iconBitmapDescriptor;
    private final DraweeHolder mLogoHolder;
    private DataSource<CloseableReference<CloseableImage>> dataSource;

    private final ControllerListener<ImageInfo> mLogoControllerListener =
            new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    CloseableReference<CloseableImage> imageReference = null;
                    try {
                        imageReference = dataSource.getResult();
                        if (imageReference != null) {
                            CloseableImage image = imageReference.get();
                            if (image != null && image instanceof CloseableStaticBitmap) {
                                CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap)image;
                                Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                                if (bitmap != null) {
                                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                    iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                                }
                            }
                        }
                    } finally {
                        dataSource.close();
                        if (imageReference != null) {
                            CloseableReference.closeSafely(imageReference);
                        }
                    }
                    update();
                }
            };

    public ReactMapMarker(Context context) {
        this.mContext = context;
        mLogoHolder = DraweeHolder.create(createDraweeHierarchy(), null);
        mLogoHolder.onAttach();
    }

    public void buildMarker(ReadableMap annotation) throws Exception{
        if (annotation == null) {
            throw new Exception("marker annotation must not be null");
        }
        id = annotation.getString("id");

        int count = annotation.getInt("count");
       // TextView textView = new TextView(mContext);//

        RelativeLayout relativeLayout = new RelativeLayout(mContext);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
        relativeLayout.setLayoutParams(layoutParams);



        ImageView imageView = new ImageView(mContext);
        imageView.setId(new Integer(2));
        imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.icon));
        RelativeLayout.LayoutParams layoutParamsIMG = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParamsIMG.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
       // layoutParams.addRule(RelativeLayout.);
        relativeLayout.addView(imageView,layoutParamsIMG);
       // TextView textView =(TextView) view.findViewById(R.id.tv_count);

        //view.setDrawingCacheEnabled(true);
       // Bitmap bitmap111 = getViewFromBitmap(view);
        TextView textView = new TextView(mContext);
        textView.setId(new Integer(1));
        textView.setText(count+"");
        textView.setTextSize(12);
        RelativeLayout.LayoutParams layoutParamsTV = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
        //layoutParams.addRule(RelativeLayout.ABOVE,2);
        layoutParamsTV.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
       // layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM,2);
       // layoutParams.bottomMargin=28;
        relativeLayout.addView(textView,layoutParamsTV);
        MarkerOptions options = new MarkerOptions();
        double latitude = annotation.getDouble("latitude");
        double longtitude = annotation.getDouble("longtitude");

        Bundle bundle = new Bundle();
        bundle.putString("id",id);
        bundle.putDouble("latitude",latitude);
        bundle.putDouble("longtide",longtitude);
        options.extraInfo(bundle);


        options.position(new LatLng(latitude, longtitude));
        if (annotation.hasKey("draggable")) {

            boolean draggable = annotation.getBoolean("draggable");
            options.draggable(draggable);
        }

        if (annotation.hasKey("title")) {
            options.title(annotation.getString("title"));
        }

        defaultIcon = BitmapDescriptorFactory.fromView(relativeLayout);
      //  defaultIcon = BitmapDescriptorFactory.fromBitmap(bitmap111);

        options.icon(defaultIcon);
        this.mOptions = options;

        if (annotation.hasKey("image")) {
            String imgUri = annotation.getMap("image").getString("uri");
            if (imgUri != null && imgUri.length() > 0) {
                if (imgUri.startsWith("http://") || imgUri.startsWith("https://")) {
                    ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imgUri)).build();
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    dataSource = imagePipeline.fetchDecodedImage(imageRequest,this);
                    DraweeController controller = Fresco.newDraweeControllerBuilder()
                            .setImageRequest(imageRequest)
                            .setControllerListener(mLogoControllerListener)
                            .setOldController(mLogoHolder.getController())
                            .build();
                    mLogoHolder.setController(controller);
                } else {
                    this.mOptions.icon(getBitmapDescriptorByName(imgUri));
                }
            }
        } else {
            options.icon(defaultIcon);
        }


    }

    private GenericDraweeHierarchy createDraweeHierarchy() {
        return new GenericDraweeHierarchyBuilder(this.mContext.getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .setFadeDuration(0)
                .build();
    }

    public String getId() {return this.id;}
    public Marker getMarker() {return this.mMarker;}
    public MarkerOptions getOptions() {return this.mOptions;}

    public void addToMap(BaiduMap map) {
        if (this.mMarker == null) {
            this.mMarker = (Marker)map.addOverlay(this.getOptions());
        }
    }

    private int getDrawableResourceByName(String name) {
        return this.mContext.getResources().getIdentifier(name, "drawable", this.mContext.getPackageName());
    }

    private BitmapDescriptor getBitmapDescriptorByName(String name) {
        return BitmapDescriptorFactory.fromResource(getDrawableResourceByName(name));
    }

    private BitmapDescriptor getIcon() {
        if (iconBitmapDescriptor != null) {
            return iconBitmapDescriptor;
        } else {
            return defaultIcon;
        }
    }

    public void update() {
        if (this.mMarker != null) {
            this.mMarker.setIcon(getIcon());
        } else if (this.mOptions != null){
            this.mOptions.icon(getIcon());
        }
    }



}
