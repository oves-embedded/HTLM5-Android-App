package com.oves.app.util;

import android.os.Environment;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtil {
    private static DownloadUtil downloadUtil;
    private final OkHttpClient okHttpClient;

    public static DownloadUtil getInstance() {
        if (downloadUtil == null) {

            Logger.d("DownloadUtil get new DownloadUtil");
            downloadUtil = new DownloadUtil();
        }
        return downloadUtil;
    }

    private DownloadUtil() {
        okHttpClient = new OkHttpClient();
    }

    /**
     * @param url      下载连接
     * @param filePath 储存下载文件的SDCard目录
     * @param listener 下载监听
     */
    public void download(final String url, final String filePath, final OnDownloadListener listener) {
        Logger.d("DownloadUtil download start");
        Request request = new Request.Builder().url(url).build();
        Logger.d("DownloadUtil request:" + request.toString());
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.d("DownloadUtil onFailure e:" + e.getMessage());
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logger.d("DownloadUtil onResponse start");
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录
                //String savePath = isExistDir(saveDir);
                Logger.d("DownloadUtil filePath:" + filePath);
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    // 从响应头中获取 Content-Disposition 字段
                    String contentDisposition = response.header("Content-Disposition");
                    String fileName = null;
                    if (contentDisposition != null) {
                        int startIndex = contentDisposition.indexOf("filename=");
                        if (startIndex != -1) {
                            startIndex += 9; // "filename=" 的长度
                            int endIndex = contentDisposition.indexOf(";", startIndex);
                            if (endIndex == -1) {
                                endIndex = contentDisposition.length();
                            }
                            fileName = contentDisposition.substring(startIndex, endIndex).replace("\"", "");
                        }
                    } else {
                        fileName = getFileNameFromUrl(url);
                    }
                    File file = new File(filePath, fileName);
                    if (file.exists() && file.isFile()) {
                        file.delete();
                    }
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onDownloading(progress);
                        Logger.d("====onDownloading====" + progress);
                    }
                    fos.flush();
                    // 下载完成
                    listener.onDownloadSuccess(file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.d("DownloadUtil onResponse e1:" + e.getMessage());
                    listener.onDownloadFailed();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        Logger.d("DownloadUtil onResponse e2:" + e.getMessage());
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        Logger.d("DownloadUtil onResponse e3:" + e.getMessage());
                    }
                }
            }

        });
    }

    /**
     * @param saveDir
     * @return
     * @throws IOException 判断下载目录是否存在
     */
    private String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }


    private String getFileNameFromUrl(String url) {
        String fileName = null;
        if (url != null) {
            int lastIndex = url.lastIndexOf("/");
            if (lastIndex != -1) {
                fileName = url.substring(lastIndex + 1);
            }
        }
        return fileName;
    }


    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess(String filePath);

        /**
         * @param progress 下载进度
         */
        void onDownloading(int progress);

        /**
         * 下载失败
         */
        void onDownloadFailed();
    }


}


