package org.hitlabnz.sensor_fusion_demo;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.hitlabnz.sensor_fusion_demo.permission.PermissionListener;
import org.hitlabnz.sensor_fusion_demo.permission.PermissionRequest;
import org.hitlabnz.sensor_fusion_demo.pojo.BLEDevice;
import org.hitlabnz.sensor_fusion_demo.utils.BLEUtils;
import org.hitlabnz.sensor_fusion_demo.utils.ToastUtils;
import org.hitlabnz.sensor_fusion_demo.utils.TypeConversion;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BleconnActivity extends FragmentActivity {
    Button btnClick;
    //日志标签
    private static final String TAG = "MainActivity";
    //蓝牙设备
    private ArrayList<BLEDevice> bleDevices = new ArrayList<>();
    //设备地址列表
    private List<String> macList = new ArrayList<>();
    //蓝牙辅助类
    private BLEUtils bleUtils = new BLEUtils();
    ;
    //本页面
    private Context mContext;
    //当前连接的蓝牙传感器
    private BluetoothGatt bluetoothGatt;

    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权限
    private List<String> deniedPermissionList = new ArrayList<>();
    //动态申请权限
    private String[] requestPermissionArray = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * 这是传感器的服务UUID和读写UUID
     */
    //蓝牙设备的服务的UUID
    public final static UUID UUID_SERVICE = UUID.fromString("0000ffe5-0000-1000-8000-00805f9a34fb");
    //蓝牙设备的读取数据的UUID
    public final static UUID UUID_READ = UUID.fromString("0000ffe4-0000-1000-8000-00805f9a34fb");
    //蓝牙设备的写入数据的UUID
    public final static UUID UUID_WRITE = UUID.fromString("0000ffe9-0000-1000-8000-00805f9a34fb");

    //保存传感器的数据
    public static float[] data = new float[12];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleconn);

        mContext = BleconnActivity.this;

        //申请权限
        initPermissions();

        //初始化视图
        InitRecyclerView();

        //初始化蓝牙
        InitBle();

        //开始搜索事件
        Button button = findViewById(R.id.ScanBLE);
        button.setOnClickListener(StartScanClick);


        btnClick = findViewById(R.id.btnJump);
        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        BleconnActivity.this,
                        SensorSelectionActivity.class
                );
                startActivity(intent);
            }
        });
    }


    /**
     * 初始化蓝牙
     */
    private void InitBle() {

        //初始化ble管理器

        if (!bleUtils.initBle(mContext)) {
            Log.d(TAG, "该设备不支持低功耗蓝牙");
            Toast.makeText(mContext, "该设备不支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            if (!bleUtils.isEnable()) {
                //去打开蓝牙
                bleUtils.openBluetooth(mContext, false);
            }
        }


        if (bleUtils != null) {
            bleUtils.stopDiscoveryDevice();
        }
    }


    /**
     * 初始化权限
     */
    private void initPermissions() {
        //Android 6.0以上动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.requestRuntimePermission(BleconnActivity.this, requestPermissionArray, new PermissionListener() {
                @Override
                public void onGranted() {
                    Log.d(TAG, "所有权限已被授予");
                }

                //用户勾选“不再提醒”拒绝权限后，关闭程序再打开程序只进入该方法！
                @Override
                public void onDenied(List<String> deniedPermissions) {
                    deniedPermissionList = deniedPermissions;
                    for (String deniedPermission : deniedPermissionList) {
                        Log.e(TAG, "被拒绝权限：" + deniedPermission);
                    }
                }
            });
        }
    }


    /**
     * 初始化列表
     */
    private void InitRecyclerView() {
        RecyclerView mRv = findViewById(R.id.DeviceListRecyclerView);
        //线性布局
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRv.setLayoutManager(linearLayoutManager);
        RecyclerAdapter adapter = new RecyclerAdapter(this, bleDevices, this);
        mRv.setAdapter(adapter);
    }


    /**
     * 刷新列表
     */
    public void UpdateList() {
        InitRecyclerView();
    }


    /**
     * 刷新列表并且添加一条数据
     *
     * @param str
     */
    public void UpdateList(BLEDevice str) {
        bleDevices.add(str);
        InitRecyclerView();
    }


    /**
     * 开始扫描实现类
     */
    private View.OnClickListener StartScanClick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            if (bleUtils == null) {
                Log.d(TAG, "searchBtDevice()-->bleUtils == null");
                return;
            }

            if (bleUtils.isDiscovery()) { //当前正在搜索设备...
                bleUtils.stopDiscoveryDevice();
            }

            //如果有当前连接就断开
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
            }

            //清空列表
            macList.clear();
            bleDevices.clear();
            UpdateList();


            //开始搜索
            bleUtils.startDiscoveryDevice(onDeviceSearchListener, 15000);
        }
    };


    /**
     * 扫描结果回调
     */
    private OnDeviceSearchListener onDeviceSearchListener = new OnDeviceSearchListener() {

        @Override
        public void onDeviceFound(BLEDevice bleDevice) {
            //如果找到了WT开头的蓝牙，并且没有添加到设备列表
            if (bleDevice.getBluetoothDevice().getName() != null && bleDevice.getBluetoothDevice().getName().contains("WT") && !macList.contains(bleDevice.getBluetoothDevice().getAddress())) {
                //加入Mac地址列表
                macList.add(bleDevice.getBluetoothDevice().getAddress());
                //更新到设备列表
                UpdateList(bleDevice);
                //提示信息
                ShowToast("一个设备被找到");
            }
        }


        @Override
        public void onDiscoveryOutTime() {


        }
    };


    /**
     * 打开蓝牙设备
     *
     * @param bleDevice
     */
    public void OpenBleDevive(BLEDevice bleDevice) {

        //如果有当前连接就断开
        if (this.bluetoothGatt != null) {
            this.bluetoothGatt.disconnect();
        }


        //停止搜索
        bleUtils.stopDiscoveryDevice();
        SetMsg("开始连接设备");
        //连接蓝牙
        BluetoothGatt bluetoothGatt = bleDevice.getBluetoothDevice().connectGatt(this, false, bluetoothGattCallbackOne);
        bluetoothGatt.connect();


        this.bluetoothGatt = bluetoothGatt;
    }


    /**
     * 第一个设备  蓝牙返回数据函数
     */
    private BluetoothGattCallback bluetoothGattCallbackOne = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    ShowToast("设备连接成功");
                    //搜索Service
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    ShowToast("设备连接断开");
                }
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // 开启接收信息
            boolean isConnect = bleUtils.enableNotification(gatt, UUID_SERVICE, UUID_READ);

            if (isConnect) {
                Log.i(TAG, "onServicesDiscovered: 设备一连接notify成功");

            } else {
                Log.i(TAG, "onServicesDiscovered: 设备一连接notify失败");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {//数据改变
            super.onCharacteristicChanged(gatt, characteristic);
            String data = TypeConversion.bytes2HexString(characteristic.getValue());
            //ShowToast(data);
            Log.i(TAG, "onCharacteristicChanged: " + data);

            dealCallDatas(gatt, characteristic);
        }
    };


    /**
     * 解析传感器的数据
     *
     * @param gatt
     * @param characteristic
     */
    private void dealCallDatas(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        //第一个传感器数据
        byte[] value = characteristic.getValue();

        if (value[0] != 0x55) {
            return; //开头不是0x55的数据删除
        }
        switch (value[1]) {

            case 0x61:
                //加速度数据
                data[3] = ((((short) value[3]) << 8) | ((short) value[2] & 0xff)) / 32768.0f * 16;   //x轴
                data[4] = ((((short) value[5]) << 8) | ((short) value[4] & 0xff)) / 32768.0f * 16;   //y轴
                data[5] = ((((short) value[7]) << 8) | ((short) value[6] & 0xff)) / 32768.0f * 16;   //z轴
                //角速度数据
                data[6] = ((((short) value[9]) << 8) | ((short) value[8] & 0xff)) / 32768.0f * 2000;  //x轴
                data[7] = ((((short) value[11]) << 8) | ((short) value[10] & 0xff)) / 32768.0f * 2000;  //x轴
                data[8] = ((((short) value[13]) << 8) | ((short) value[12] & 0xff)) / 32768.0f * 2000;  //x轴
                //角度
                data[9] = ((((short) value[15]) << 8) | ((short) value[14] & 0xff)) / 32768.0f * 180;   //x轴
                data[10] = ((((short) value[17]) << 8) | ((short) value[16] & 0xff)) / 32768.0f * 180;   //y轴
                data[11] = ((((short) value[19]) << 8) | ((short) value[18] & 0xff)) / 32768.0f * 180;   //z轴
                break;
            case 0x62:


                break;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss.SSS");

        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = "时间：" + formatter.format(curDate);

        str += "\r\n加速度XYZ：" + String.format("%.3f", data[3]) + "\t\t" + String.format("%.3f", data[4]) + "\t\t" + String.format("%.3f", data[5])
                + "\r\n角速度速度XYZ：" + String.format("%.3f", data[6]) + "\t\t" + String.format("%.3f", data[6]) + "\t\t" + String.format("%.3f", data[8])
                + "\r\n角度XYZ：" + String.format("%.3f", data[9]) + "\t\t" + String.format("%.3f", data[10]) + "\t\t" + String.format("%.3f", data[11]);


        //把数据打印到首页
        SetMsg(str);
    }


    /**
     * 首页文本提示信息
     *
     * @param str
     */
    private void SetMsg(final String str) {
        BleconnActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                TextView viewById = findViewById(R.id.MainActivityMsgTextView);
                viewById.setText(str);
            }
        });
    }


    /**
     * 打开Toast提示
     *
     * @param str
     */
    private void ShowToast(String str) {
        Log.i("MainActivity", str);
        ToastUtils.show(BleconnActivity.this, str);
    }

}