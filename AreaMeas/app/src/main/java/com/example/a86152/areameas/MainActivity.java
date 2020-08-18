package com.example.a86152.areameas;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.os.Message;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;

    MapView mMapView;
    BaiduMap mBaiduMap;

    private SensorManager mSensorManager;
    double degree = 0;
    // UI相关
    OnCheckedChangeListener radioButtonListener;
    Button requestLocButton,mStartButton,mStopButton;
    TextView mAreaText,mMu;
    ToggleButton togglebtn = null;
    boolean isFirstLoc = true;// 是否首次定位

    //计算面积
    List<LatLng> latLngs = new ArrayList<LatLng> ();
    ArrayList<Double> edgs = new ArrayList<Double> ();
    private final Timer timer = new Timer();
    private TimerTask task;
    LatLng ll =null;
    double Area =0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        judgePermission();
        requestLocButton = (Button) findViewById(R.id.button1);
        mStartButton=(Button) findViewById(R.id.start);
        mStartButton.setOnClickListener(new ButtonListener());
        mStopButton=(Button) findViewById(R.id.stop);
        mStopButton.setOnClickListener(new ButtonListener());
        mAreaText = (TextView) findViewById(R.id.area);
        mAreaText.setText("面积为：");
        mMu = (TextView) findViewById(R.id.mu);
        mMu.setText("亩数为：");

        mCurrentMode = LocationMode.NORMAL;
        requestLocButton.setText("普通");

        OnClickListener btnClickListener = new OnClickListener() {
            public void onClick(View v) {
                switch (mCurrentMode) {
                    case NORMAL:
                        requestLocButton.setText("跟随");
                        mCurrentMode = LocationMode.FOLLOWING;
                        mBaiduMap
                                .setMyLocationConfigeration(new MyLocationConfiguration(
                                        mCurrentMode, true, mCurrentMarker));
                        break;
                    case COMPASS:
                        requestLocButton.setText("普通");
                        mCurrentMode = LocationMode.NORMAL;
                        mBaiduMap
                                .setMyLocationConfigeration(new MyLocationConfiguration(
                                        mCurrentMode, true, mCurrentMarker));
                        break;
                    case FOLLOWING:
                        requestLocButton.setText("罗盘");
                        mCurrentMode = LocationMode.COMPASS;
                        mBaiduMap
                                .setMyLocationConfigeration(new MyLocationConfiguration(
                                        mCurrentMode, true, mCurrentMarker));
                        break;
                }
            }
        };
        requestLocButton.setOnClickListener(btnClickListener);

        togglebtn = (ToggleButton) findViewById(R.id.togglebutton);
        togglebtn
                .setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        if (isChecked) {
                            // 普通地图
                            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                        } else {
                            // 卫星地图
                            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                        }

                    }
                });

        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);// 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);// 设置发起定位请求的间隔时间为1000ms
        option.setIsNeedAddress(true);
        mLocClient.setLocOption(option);
        mLocClient.start();
        // 隐藏logo
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)){
            child.setVisibility(View.INVISIBLE);
        }

        //指南针
        mSensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor magenticSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor accelerometerSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(listener,magenticSensor,SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(listener,accelerometerSensor,SensorManager.SENSOR_DELAY_GAME);


    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    latLngs.add(ll);
                    if(latLngs.size()>=2)
                    {
                        List<LatLng> points = new ArrayList<LatLng>();
                        points.add(latLngs.get(latLngs.size()-2));
                        points.add(latLngs.get(latLngs.size()-1));
                        //设置折线的属性
                        OverlayOptions mOverlayOptions = new PolylineOptions()
                                .width(10)
                                .color(0xAAFF0000)
                                .points(points);
                      //在地图上绘制折线
                     //mPloyline 折线对象
                        Overlay mPolyline = mBaiduMap.addOverlay(mOverlayOptions);

                    }

//                    Toast.makeText(MainActivity.this,"hello",Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;

            }
        }
    };


    private class ButtonListener implements View.OnClickListener  {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start:
                    Toast.makeText(MainActivity.this, "请开始步行>>>>", Toast.LENGTH_SHORT).show();
                    mAreaText.setText("面积为：");
                    mMu.setText("亩数为：");
                    mBaiduMap.clear();
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            Message message = new Message();
                            message.what = 1;
                            handler.sendMessage(message);
                        }
                    };
                    timer.schedule(task, 500, 4000);
                    break;
                case R.id.stop:
                    task.cancel();
                    int i=0;
                    LinkedHashSet<LatLng> hashSet = new LinkedHashSet<>(latLngs);
                    List<LatLng> listWithoutDuplicates = new ArrayList<>(hashSet);
                    if(listWithoutDuplicates.size()>=3)
                    {
//                        for( i=0;i<latLngs.size()-1;i++)
//                       {
//                          edgs.add(DistanceUtil. getDistance(latLngs.get(i), latLngs.get(i+1)));
//                       }
//                        edgs.add(DistanceUtil. getDistance(latLngs.get(i+1), latLngs.get(0)));

//                        for(i=1;i<listWithoutDuplicates.size()-1;i++ )
//                        {
//                            Area+=calculateArea(latLngs.get(0),latLngs.get(i),latLngs.get(i+1));
//                        }


                       Area=getEnclArea(listWithoutDuplicates);
//                        if(Area<0) Area=0;
                        DecimalFormat df = new DecimalFormat("#.00");
                        DecimalFormat df1= new DecimalFormat("#.0000");
                        mAreaText.setText("面积为："+df.format(Area)+"平方米");
                        mMu.setText("亩数为："+df1.format(Area/666.66667)+"亩");

                    }

                    //构造PolygonOptions
                    PolygonOptions mPolygonOptions = new PolygonOptions()
                            .points(listWithoutDuplicates)
                            .fillColor(0xAAFFFF00) //填充颜色
                            .stroke(new Stroke(5, 0xAA00FF00)); //边框宽度和颜色
                    //在地图上显示多边形
                    mBaiduMap.addOverlay(mPolygonOptions);

                    hashSet.clear();
                    listWithoutDuplicates.clear();
                    latLngs.clear();
                    edgs.clear();
                    Area=0;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 单位：米
     *
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @return
     */
    public static double getDistance(double lon1, double lat1, double lon2, double lat2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);
        double c = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        c = c * 6378.137;// 6378.137赤道半径

        return (c*1000);
//        return (Math.round(c * 10000d) / 10000d) * 1000;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public static double calculateArea(LatLng A, LatLng B, LatLng C) {
        double a = getDistance(A.longitude, A.latitude, B.longitude, B.latitude);
        double b = getDistance(B.longitude, B.latitude, C.longitude, C.latitude);
        double c = getDistance(C.longitude, C.latitude, A.longitude, A.latitude);
        double p = (a + b + c) / 2;
        double area = Math.sqrt(p * Math.abs((p - a)) * Math.abs((p - b)) * Math.abs((p - c)));
        return area;
    }



    //指南针 获取degree值
    private SensorEventListener listener=new SensorEventListener() {
        float[] accelerometerValues=new float[3];
        float[] magenticValues=new float[3];
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            //判断当前是加速度传感器还是地磁传感器
            if(sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
                accelerometerValues=sensorEvent.values.clone();
            }else if(sensorEvent.sensor.getType()== Sensor.TYPE_MAGNETIC_FIELD){
                magenticValues=sensorEvent.values.clone();
            }
            float[] R=new float[9];
            float[] values=new float[3];
            SensorManager.getRotationMatrix(R, null, accelerometerValues, magenticValues);
            SensorManager.getOrientation(R,values);
            Log.d("MainActivity","value[0] is"+Math.toDegrees(values[0]));
            degree = Math.toDegrees(values[0]);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
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
                    .direction((float)degree).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            ll = new LatLng(location.getLatitude(), location.getLongitude());
            if (isFirstLoc) {
                isFirstLoc = false;
                // MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                // 设置缩放比例,更新地图状态
                float f = mBaiduMap.getMaxZoomLevel();// 19.0
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll,
                        f - 2);
                mBaiduMap.animateMapStatus(u);
                //地图位置显示
                Toast.makeText(MainActivity.this, location.getAddrStr(),
                        Toast.LENGTH_SHORT).show();
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
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;

        if(mSensorManager!=null){
            mSensorManager.unregisterListener(listener);
        }
        super.onDestroy();
    }


    //6.0之后要动态获取权限，重要！！！
    protected void judgePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝

            // sd卡权限
            String[] SdCardPermission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (ContextCompat.checkSelfPermission(this, SdCardPermission[0]) != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, SdCardPermission, 100);
            }

            //手机状态权限
            String[] readPhoneStatePermission = {Manifest.permission.READ_PHONE_STATE};
            if (ContextCompat.checkSelfPermission(this, readPhoneStatePermission[0]) != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, readPhoneStatePermission, 200);
            }

            //定位权限
            String[] locationPermission = {Manifest.permission.ACCESS_FINE_LOCATION};
            if (ContextCompat.checkSelfPermission(this, locationPermission[0]) != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, locationPermission, 300);
            }

            String[] ACCESS_COARSE_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION};
            if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION[0]) != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, ACCESS_COARSE_LOCATION, 400);
            }


            String[] READ_EXTERNAL_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE};
            if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE[0]) != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, READ_EXTERNAL_STORAGE, 500);
            }

            String[] WRITE_EXTERNAL_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE[0]) != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, WRITE_EXTERNAL_STORAGE, 600);
            }

        }else{
            //doSdCardResult();
        }
        //LocationClient.reStart();
    }


    /**
     * 获取多边形面积，单位：平方米
     *
     * @return
     */
    public static Double getEnclArea(List<LatLng> lastListBuff) {

        Double Spoints = 0.0;
        Integer lowestIndex = 0;
        LatLng lowestPoint = lastListBuff.get(0);
        for (int i = 0; i < lastListBuff.size(); i++) {
            LatLng point = lastListBuff.get(i);
            if (point.latitude < lowestPoint.latitude) {
                lowestIndex = i;
                lowestPoint = point;
            }
        }
        System.out.println("lowestIndex:" + lowestIndex);
        List<LatLng> lastList = new ArrayList<>();
        for (int i = lowestIndex; i < lastListBuff.size(); i++) {
            lastList.add(lastListBuff.get(i));
        }
        for (int i = 0; i < lowestIndex; i++) {
            lastList.add(lastListBuff.get(i));
        }

        for (int i = 0; i < lastList.size() - 2; i++) {
            LatLng A = lastList.get(0);
            LatLng B = lastList.get(i + 1);
            LatLng C = lastList.get(i + 2);
            if (A.longitude == B.longitude && A.longitude == C.longitude) {
                //k1 、k2不存在
                continue;
            } else if (A.longitude== B.longitude) {
                //k1不存在 k2存在
                Double k2 = (C.latitude - A.latitude) / (C.longitude - A.longitude);
                if (k2 < 0) {//凹
                    Spoints -= calculateArea(A, B, C);
                } else {//凸
                    Spoints += calculateArea(A, B, C);
                }
            } else if (A.longitude == C.longitude) {
                //k1存在 k2不存在
                Double k1 = (B.latitude - A.latitude) / (B.longitude - A.longitude);
                if (k1 > 0) {//凹
                    Spoints -= calculateArea(A, B, C);
                } else {//凸
                    Spoints += calculateArea(A, B, C);
                }
            } else {
                //k1、k2都存在
                Double k1 = (B.latitude - A.latitude) / (B.longitude - A.longitude);
                Double k2 = (C.latitude - A.latitude) / (C.longitude- A.longitude);
                if (k1 > 0 && k2 < 0) {//凹
                    Spoints -= calculateArea(A, B, C);
                } else if (k1 < 0 && k2 < 0) {//同号都为负数
                    if (k1 < k2) {//凹
                        Spoints -= calculateArea(A, B, C);
                    } else {//凸
                        Spoints += calculateArea(A, B, C);
                    }
                } else if (k1 > 0 && k2 > 0) {//同号都为正数
                    if (k1 < k2) {//凹
                        Spoints -= calculateArea(A, B, C);
                    } else {//凸
                        Spoints += calculateArea(A, B, C);
                    }
                } else {//其余情况都为凸
                    Spoints += calculateArea(A, B, C);
                }
            }
        }

        DecimalFormat df = new DecimalFormat("#.00");
        return Double.valueOf(df.format(Math.abs(Spoints)));
    }

}
