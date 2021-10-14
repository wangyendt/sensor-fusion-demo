package org.hitlabnz.sensor_fusion_demo.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.hitlabnz.sensor_fusion_demo.OnDeviceSearchListener;
import org.hitlabnz.sensor_fusion_demo.pojo.BLEDevice;

import java.util.List;
import java.util.UUID;


/**
 * 作者：witmotion on 2021/08/6 17:47
 * 蓝牙操作辅助类
 */
public class BLEUtils {
    private static final String TAG = "BLEManager";

    private Context mContext;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetooth4Adapter;
    private OnDeviceSearchListener onDeviceSearchListener;  //设备扫描结果监听


    private Handler mHandler = new Handler();

    public BLEUtils() {

    }

    /**
     * 初始化
     *
     * @param context
     */
    public boolean initBle(Context context) {
        mContext = context;
        if (!checkBle(context)) {
            return false;
        } else {
            return true;
        }
    }

    ////////////////////////////////////  扫描设备  ///////////////////////////////////////////////
    //扫描设备回调
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            //在onLeScan()回调中尽量做少的操作，可以将扫描到的设备扔到另一个线程中处理
            if (bluetoothDevice == null)
                return;

            if (bluetoothDevice.getName() != null) {
                Log.d(TAG, bluetoothDevice.getName() + "-->" + bluetoothDevice.getAddress());
            } else {
                Log.d(TAG, "null" + "-->" + bluetoothDevice.getAddress());
            }
            BLEDevice bleDevice = new BLEDevice(bluetoothDevice, rssi);
            if (onDeviceSearchListener != null) {
                onDeviceSearchListener.onDeviceFound(bleDevice);  //扫描到设备回调
            }
        }
    };


    /**
     * 设置时间段 扫描设备
     *
     * @param onDeviceSearchListener 设备扫描监听
     * @param scanTime               扫描时间
     */
    public void startDiscoveryDevice(OnDeviceSearchListener onDeviceSearchListener, long scanTime) {
        if (bluetooth4Adapter == null) {
            Log.e(TAG, "startDiscoveryDevice-->bluetooth4Adapter == null");
            return;
        }

        this.onDeviceSearchListener = onDeviceSearchListener;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "开始扫描设备");
            bluetooth4Adapter.startLeScan(leScanCallback);

        } else {
            return;
        }

        //设定最长扫描时间
        mHandler.postDelayed(stopScanRunnable, scanTime);
    }


    private Runnable stopScanRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if (onDeviceSearchListener != null) {
                onDeviceSearchListener.onDiscoveryOutTime();  //扫描超时回调
            }
            //scanTime之后还没有扫描到设备，就停止扫描。
            stopDiscoveryDevice();
        }
    };

    //////////////////////////////////////  停止扫描  /////////////////////////////////////////////

    /**
     * 停止扫描
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopDiscoveryDevice() {
        mHandler.removeCallbacks(stopScanRunnable);

        if (bluetooth4Adapter == null) {
            Log.e(TAG, "stopDiscoveryDevice-->bluetooth4Adapter == null");
            return;
        }

        if (leScanCallback == null) {
            Log.e(TAG, "stopDiscoveryDevice-->leScanCallback == null");
            return;
        }

        Log.d(TAG, "停止扫描设备");
        bluetooth4Adapter.stopLeScan(leScanCallback);
    }


    ///////////////////////////////////  开启接收蓝牙的数据  ///////////////////////////////////////////////

    /**
     * 开启接收数据
     *
     * @param gatt
     * @param serviceUUID
     * @param characteristicUUID
     * @return
     */
    public boolean enableNotification(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID) {
        boolean success = false;
        BluetoothGattService service = gatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = findNotifyCharacteristic(service, characteristicUUID);
            if (characteristic != null) {
                success = gatt.setCharacteristicNotification(characteristic, true);
                if (success) {
                    // 来源：http://stackoverflow.com/questions/38045294/oncharacteristicchanged-not-called-with-ble
                    for (BluetoothGattDescriptor dp : characteristic.getDescriptors()) {
                        if (dp != null) {
                            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                            gatt.writeDescriptor(dp);
                        }
                    }
                }
            }
        }
        return success;
    }


    private BluetoothGattCharacteristic findNotifyCharacteristic(BluetoothGattService service, UUID characteristicUUID) {
        BluetoothGattCharacteristic characteristic = null;
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }
        if (characteristic != null)
            return characteristic;
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }
        return characteristic;
    }


    /////////////////////////////////////////////////////检查性工作//////////////////////////////////////////////////////////

    /**
     * 检测手机是否支持4.0蓝牙
     *
     * @param context 上下文
     * @return true--支持4.0  false--不支持4.0
     */
    private boolean checkBle(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {  //API 18 Android 4.3
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return false;
            }
            bluetooth4Adapter = bluetoothManager.getAdapter();  //BLUETOOTH权限
            if (bluetooth4Adapter == null) {
                return false;
            } else {
                Log.d(TAG, "该设备支持蓝牙4.0");
                return true;
            }
        } else {
            return false;
        }
    }


    /**
     * 获取蓝牙状态
     */
    public boolean isEnable() {
        if (bluetooth4Adapter == null) {
            return false;
        }
        return bluetooth4Adapter.isEnabled();
    }

    /**
     * 打开蓝牙
     *
     * @param isFast true 直接打开蓝牙  false 提示用户打开
     */
    public void openBluetooth(Context context, boolean isFast) {
        if (!isEnable()) {
            if (isFast) {
                Log.d(TAG, "直接打开手机蓝牙");
                bluetooth4Adapter.enable();  //BLUETOOTH_ADMIN权限
            } else {
                Log.d(TAG, "提示用户去打开手机蓝牙");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableBtIntent);
            }
        } else {
            Log.d(TAG, "手机蓝牙状态已开");
        }
    }


    /**
     * 本地蓝牙是否处于正在扫描状态
     *
     * @return true false
     */
    public boolean isDiscovery() {
        if (bluetooth4Adapter == null) {
            return false;
        }
        return bluetooth4Adapter.isDiscovering();
    }

}
