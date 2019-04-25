package com.cxz.compresslib.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc
 */
public class CommonUtil {

    /**
     * 许多定制的android系统，并不带相机功能，如果强行调用，程序会崩溃
     *
     * @param activity    上下文
     * @param intent      意图
     * @param requestCode 回调标识码
     */
    public static void hasCamera(Activity activity, Intent intent, int requestCode) {
        if (activity == null) {
            throw new IllegalArgumentException("activity 不能为空");
        }
        PackageManager pm = activity.getPackageManager();
        boolean hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
                || Camera.getNumberOfCameras() > 0;
        if (hasCamera) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            Toast.makeText(activity, "当前设备没有相机", Toast.LENGTH_SHORT).show();
            throw new IllegalArgumentException("当前设备没有相机");
        }
    }

    /**
     * 获取拍照的Intent
     *
     * @param outPutUri 拍照后图片输出URI
     * @return 返回Intent，方便封装跳转
     */
    public static Intent getCameraIntent(Uri outPutUri) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);  // 设置 Action 为拍照
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri); // 设置拍取得照片保存到指定URI
        return intent;
    }

    /**
     * 跳转到图库选择
     *
     * @param activity    上下文
     * @param requestCode 回调码
     */
    public static void openAlbum(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 显示圆形对话框
     *
     * @param activity      上下文
     * @param progressTitle 显示的标题
     */
    public static ProgressDialog showProgressDialog(Activity activity, String... progressTitle) {
        if (activity == null || activity.isFinishing()) return null;
        String title = "提示";
        if (progressTitle != null && progressTitle.length > 0) title = progressTitle[0];
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(title);
        progressDialog.setCancelable(false);
        progressDialog.show();
        return progressDialog;
    }

}
