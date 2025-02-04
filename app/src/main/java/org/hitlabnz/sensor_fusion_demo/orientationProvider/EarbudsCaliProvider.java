package org.hitlabnz.sensor_fusion_demo.orientationProvider;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import org.hitlabnz.sensor_fusion_demo.BleconnActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.TimeZone;

public class EarbudsCaliProvider extends OrientationProvider {

    private float gain;
    private float sampleFreq;
    private float qW, qX, qY, qZ; // raw quaternion
    private float qW_, qX_, qY_, qZ_; // transformed quaternion
    private float rW, rX, rY, rZ; // quaternion to be left rotated
    private double SW, SX, SY, SZ;
    private double sW, sX, sY, sZ;
    Deque<Float> deqW = new LinkedList<Float>();
    Deque<Float> deqX = new LinkedList<Float>();
    Deque<Float> deqY = new LinkedList<Float>();
    Deque<Float> deqZ = new LinkedList<Float>();
    private int idx;

    private float acc[] = new float[4];
    private float gyr[] = new float[4];

    public EarbudsCaliProvider(SensorManager sensorManager,
                               float gain,
                               float sampleFreq) {
        super(sensorManager);
        this.gain = gain;
        this.sampleFreq = sampleFreq;
        rW = qW_ = qW = 1.0f;
        rX = qX_ = qX = 0.0f;
        rY = qY_ = qY = 0.0f;
        rZ = qZ_ = qZ = 0.0f;
        idx = 0;
        SW = SX = SY = SZ = sW = sX = sY = sZ = 0.0;
        sensorList.add(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensorList.add(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
    }


    static long time_ = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*获取当前系统时间*/
        long time = System.currentTimeMillis();
        if (time - time_ > 10) {
            time_ = time;

            /*时间戳转换成IOS8601字符串*/
            Date date = new Date(time);
            TimeZone tz = TimeZone.getTimeZone("Asia/Beijing");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            df.setTimeZone(tz);
            String nowAsIOS = df.format(date);

            acc[0] = BleconnActivity.data[3];
            acc[1] = BleconnActivity.data[4];
            acc[2] = BleconnActivity.data[5];
            gyr[0] = (float) (BleconnActivity.data[6] * Math.PI / 180);
            gyr[1] = (float) (BleconnActivity.data[7] * Math.PI / 180);
            gyr[2] = (float) (BleconnActivity.data[8] * Math.PI / 180);
//            Log.d("sensor_data", nowAsIOS + String.format(":ax=%.3f,ay=%.3f,az=%.3f,gx=%.3f,gy=%.3f,gz=%.3f", acc[0], acc[1], acc[2], gyr[0], gyr[1], gyr[2]));
            MadgwickAHRSupdateIMU(gyr[0], gyr[1], gyr[2], acc[0], acc[1], acc[2]);
//            currentOrientationQuaternion.setXYZW(qX, qY, qZ, -qW); //-q for cube rotation inversion
            currentOrientationQuaternion.setXYZW(-qZ_, -qX_, -qY_, -qW_); //-q for cube rotation inversion
        }
    }

    private float invSqrt(float x) {
        return (float) (1.0f / Math.sqrt(x));
    }

    public void MadgwickAHRSupdateIMU(float gx, float gy, float gz, float ax, float ay, float az) {
        float recipNorm;
        float s0, s1, s2, s3;
        float qDot1, qDot2, qDot3, qDot4;
        float _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2, _8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

        // control gain
        idx++;
        if (idx > 50 * 10) {
            gain = 0.033f;
            idx = Math.min(idx, 1000);
        } else {
            gain = 0.2f;
        }

        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5f * (-qX * gx - qY * gy - qZ * gz);
        qDot2 = 0.5f * (qW * gx + qY * gz - qZ * gy);
        qDot3 = 0.5f * (qW * gy - qX * gz + qZ * gx);
        qDot4 = 0.5f * (qW * gz + qX * gy - qY * gx);

        // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
        if (!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

            // Normalise accelerometer measurement
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            _2q0 = 2.0f * qW;
            _2q1 = 2.0f * qX;
            _2q2 = 2.0f * qY;
            _2q3 = 2.0f * qZ;
            _4q0 = 4.0f * qW;
            _4q1 = 4.0f * qX;
            _4q2 = 4.0f * qY;
            _8q1 = 8.0f * qX;
            _8q2 = 8.0f * qY;
            q0q0 = qW * qW;
            q1q1 = qX * qX;
            q2q2 = qY * qY;
            q3q3 = qZ * qZ;

            // Gradient decent algorithm corrective step
            s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
            s1 = _4q1 * q3q3 - _2q3 * ax + 4.0f * q0q0 * qX - _2q0 * ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
            s2 = 4.0f * q0q0 * qY + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
            s3 = 4.0f * q1q1 * qZ - _2q1 * ax + 4.0f * q2q2 * qZ - _2q2 * ay;
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // Apply feedback step
            qDot1 -= gain * s0;
            qDot2 -= gain * s1;
            qDot3 -= gain * s2;
            qDot4 -= gain * s3;
        }

        // Integrate rate of change of quaternion to yield quaternion
        qW += qDot1 * (1.0f / sampleFreq);
        qX += qDot2 * (1.0f / sampleFreq);
        qY += qDot3 * (1.0f / sampleFreq);
        qZ += qDot4 * (1.0f / sampleFreq);


        double tmp;
        deqW.offer(qW);
        sW += qW;
        SW += qW * qW;
        if (deqW.size() > 50) {
            tmp = deqW.poll();
            sW -= tmp;
            SW -= tmp * tmp;
        }
        deqX.offer(qX);
        sX += qX;
        SX += qX * qX;
        if (deqX.size() > 50) {
            tmp = deqX.poll();
            sX -= tmp;
            SX -= tmp * tmp;
        }
        deqY.offer(qY);
        sY += qY;
        SY += qY * qY;
        if (deqY.size() > 50) {
            tmp = deqY.poll();
            sY -= tmp;
            SY -= tmp * tmp;
        }
        deqZ.offer(qZ);
        sZ += qZ;
        SZ += qZ * qZ;
        if (deqZ.size() > 50) {
            tmp = deqZ.poll();
            sZ -= tmp;
            SZ -= tmp * tmp;
        }

        tmp = (SW / 50 - sW * sW / 2500 +
                SX / 50 - sX * sX / 2500 +
                SY / 50 - sY * sY / 2500 +
                SZ / 50 - sZ * sZ / 2500
        );

        // Rotation
        double alpha = 0.9;
        if (tmp < 1e-4) {
            rW = (float) (rW * alpha + qW * (1 - alpha));
            rX = (float) (rX * alpha + -qX * (1 - alpha));
            rY = (float) (rY * alpha + -qY * (1 - alpha));
            rZ = (float) (rZ * alpha + -qZ * (1 - alpha));
        }

        qW_ = rW * qW - rX * qX - rY * qY - rZ * qZ;
        qX_ = rW * qX + rX * qW + rY * qZ - rZ * qY;
        qY_ = rW * qY - rX * qZ + rY * qW + rZ * qX;
        qZ_ = rW * qZ + rX * qY - rY * qX + rZ * qW;

        // Normalise quaternion
        recipNorm = invSqrt(qW * qW + qX * qX + qY * qY + qZ * qZ);
        qW *= recipNorm;
        qX *= recipNorm;
        qY *= recipNorm;
        qZ *= recipNorm;

        // Normalise quaternion
        recipNorm = invSqrt(qW_ * qW_ + qX_ * qX_ + qY_ * qY_ + qZ_ * qZ_);
        qW_ *= recipNorm;
        qX_ *= recipNorm;
        qY_ *= recipNorm;
        qZ_ *= recipNorm;

    }
}
