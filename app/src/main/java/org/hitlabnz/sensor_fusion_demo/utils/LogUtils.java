package org.hitlabnz.sensor_fusion_demo.utils;

import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {

    private final String FORMAT = "yyyyMMdd-HH-mm-ss";
    private final String PATH = "/sdcard/healthlab/";
    private final String suffix_r = "-rawData-";
    private File rawDataLog;
    private FileOutputStream fos_r;

    public LogUtils(String suffix) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT);
        String timeStamp = sdf.format(new Date());
        String fileName;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.d("wytest", "shit");
            return;
        }

        try {
            File dir = new File(PATH);
            boolean ret;
            if (!dir.exists()) {
                ret = dir.mkdirs();
                if (ret) {
                    Log.d("shit", "shit");
                }
            }

            fileName = PATH + timeStamp + suffix_r + suffix + ".txt";
            rawDataLog = new File(fileName);
            rawDataLog.createNewFile();
            fos_r = new FileOutputStream(rawDataLog, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 生成文件
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }

    public void writeBytes(String content) {
        try {
            byte[] buffer = content.getBytes();
            fos_r.write(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            fos_r.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
