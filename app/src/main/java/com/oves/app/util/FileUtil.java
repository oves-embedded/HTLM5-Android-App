package com.oves.app.util;

import android.content.Context;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class FileUtil {


    public static void delCacheFile(Context context){
        File file=context.getCacheDir();
        deleteChildFolder(file);
    }

    public static void deleteChildFolder(File folder) {
        if (folder.exists()) {
            Queue<File> fileQueue = new LinkedList<>();
            fileQueue.add(folder);
            while (!fileQueue.isEmpty()) {
                File current = fileQueue.poll();
                if (current.isDirectory()) {
                    File[] files = current.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            fileQueue.add(file);
                        }
                    }
                } else {
                    synchronized (current.getAbsolutePath().intern()) {
                        if (!current.delete()) {
                            Logger.d("delete file fail : " + current.getAbsolutePath());
                        } else {
                            Logger.d("delete file success : " + current.getAbsolutePath());
                        }
                    }
                }
            }
        } else {
            Logger.d("folder not exist!");
        }
    }

}
