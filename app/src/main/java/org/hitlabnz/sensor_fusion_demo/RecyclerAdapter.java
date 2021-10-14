package org.hitlabnz.sensor_fusion_demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.hitlabnz.sensor_fusion_demo.pojo.BLEDevice;

import java.util.ArrayList;


/**
 * 首页列表适配类
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    /**
     * 首页面
     */
    private Context mContext;

    /**
     * 数据
     */
    private ArrayList<BLEDevice> mDatas;
    /**
     * 主页
     */
    private BleconnActivity mainActivity;


    public RecyclerAdapter(Context context, ArrayList<BLEDevice> bleDevices, BleconnActivity activity) {
        mContext = context;
        mDatas = bleDevices;
        mainActivity = activity;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_layout, parent, false);
        return new NormalHolder(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NormalHolder normalHolder = (NormalHolder) holder;
        BLEDevice bleDevice = mDatas.get(position);
        String text = String.format("%s  %s [%s]", bleDevice.getBluetoothDevice().getName(), bleDevice.getBluetoothDevice().getAddress(), bleDevice.getRSSI());
        normalHolder.mTV.setText(text);
        normalHolder.bleDevice = mDatas.get(position);
    }


    @Override
    public int getItemCount() {
        return mDatas.size();
    }


    public class NormalHolder extends RecyclerView.ViewHolder {

        //文本显示
        public TextView mTV;

        //蓝牙设备
        public BLEDevice bleDevice;

        public NormalHolder(View itemView) {
            super(itemView);
            mTV = itemView.findViewById(R.id.item_tv);
            mTV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(mContext, mTV.getText(), Toast.LENGTH_SHORT).show();
                    mainActivity.OpenBleDevive(bleDevice);
                }
            });
        }

    }
}
