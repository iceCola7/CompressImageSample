package com.cxz.compress.sample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.cxz.compresslib.CompressImageManager;
import com.cxz.compresslib.bean.Photo;
import com.cxz.compresslib.config.CompressConfig;
import com.cxz.compresslib.listener.CompressImage;
import com.cxz.compresslib.utils.CachePathUtil;
import com.cxz.compresslib.utils.CommonUtil;
import com.cxz.compresslib.utils.Constants;
import com.cxz.compresslib.utils.UriParseUtil;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CompressImage.CompressListener {

    private static final String TAG = "MainActivity";
    private CompressConfig compressConfig; // 压缩配置
    private ProgressDialog dialog; // 压缩进度加载框
    private String cameraCachePath; // 拍照源文件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 运行时权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (checkSelfPermission(perms[0]) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(perms[1]) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(perms, 2000);
            }
        }

        compressConfig = CompressConfig.builder().create();


    }

    // 拍照
    public void camera(View view) {
        // android 7.0 file 路径的变更，需要使用 FileProvider 来做
        Uri outputUri;
        File file = CachePathUtil.getCameraCacheFile();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outputUri = UriParseUtil.getCameraOutPutUri(this, file);
        } else {
            outputUri = Uri.fromFile(file);
        }
        cameraCachePath = file.getAbsolutePath();
        CommonUtil.hasCamera(this, CommonUtil.getCameraIntent(outputUri), Constants.CAMERA_CODE);
    }

    public void album(View view) {
        CommonUtil.openAlbum(this, Constants.ALBUM_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拍照返回
        if (requestCode == Constants.CAMERA_CODE && resultCode == RESULT_OK) {
            // 压缩
            preCompress(cameraCachePath);
        }
        if (requestCode == Constants.ALBUM_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                String path = UriParseUtil.getPath(this, uri);
                preCompress(path);
            }
        }
    }

    private void preCompress(String path) {
        ArrayList<Photo> photos = new ArrayList<>();
        photos.add(new Photo(path));
        if (!photos.isEmpty()) compress(photos);
    }

    private void compress(ArrayList<Photo> photos) {
        if (compressConfig.isShowCompressDialog()) {
            dialog = CommonUtil.showProgressDialog(this, "压缩中...");
        }
        CompressImageManager.build(this, compressConfig, photos, this).compress();
    }

    @Override
    public void onCompressSuccess(ArrayList<Photo> images) {
        Log.e(TAG, "onCompressSuccess: success");
        if (dialog != null) dialog.dismiss();
    }

    @Override
    public void onCompressFailed(ArrayList<Photo> images, String error) {
        Log.e(TAG, "onCompressFailed: " + error);
        if (dialog != null) dialog.dismiss();
    }

}
