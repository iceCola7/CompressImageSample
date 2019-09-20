package com.cxz.compresslib;

import android.content.Context;
import android.text.TextUtils;

import com.cxz.compresslib.bean.Photo;
import com.cxz.compresslib.config.CompressConfig;
import com.cxz.compresslib.core.CompressImageUtil;
import com.cxz.compresslib.listener.CompressImage;
import com.cxz.compresslib.listener.CompressResultListener;

import java.io.File;
import java.util.ArrayList;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc
 */
public class CompressImageManager implements CompressImage {

    private CompressImageUtil compressImageUtil; // 压缩工具类
    private ArrayList<Photo> images; // 需要压缩的图片集合
    private CompressListener listener; // 压缩的监听
    private CompressConfig config; // 压缩配置

    private CompressImageManager(Context context, CompressConfig compressConfig,
                                 ArrayList<Photo> images, CompressListener listener) {
        this.images = images;
        this.listener = listener;
        this.config = compressConfig;
        this.compressImageUtil = new CompressImageUtil(context, config);
    }

    public static CompressImage build(Context context, CompressConfig compressConfig,
                                      ArrayList<Photo> images, CompressListener listener) {
        return new CompressImageManager(context, compressConfig, images, listener);
    }

    @Override
    public void compress() {
        if (images == null || images.isEmpty()) {
            listener.onCompressFailed(images, "images 为空");
            return;
        }
        for (Photo image : images) {
            if (image == null) {
                listener.onCompressFailed(images, "image 为空");
                return;
            }
        }
        compress(images.get(0));
    }

    // 从需要压缩的图片集合中，第一张开始压缩
    private void compress(final Photo image) {
        // 如果原始图片有问题
        if (TextUtils.isEmpty(image.getOriginalPath())) {
            continueCompress(image, false);
            return;
        }
        // 如果图片文件不存在或者不是文件
        File file = new File(image.getOriginalPath());
        if (!file.exists() || !file.isFile()) {
            continueCompress(image, false);
            return;
        }

        // 如果图片文件的大小不在压缩的最小值之内，不用压缩了
        if (file.length() < config.getMaxSize()) {
            continueCompress(image, true);
            return;
        }

        // 开始压缩
        compressImageUtil.compress(image.getOriginalPath(), new CompressResultListener() {
            @Override
            public void onCompressSuccess(String imgPath) {
                // 设置压缩成功的图片路径
                image.setCompressPath(imgPath);
                continueCompress(image, true);
            }

            @Override
            public void onCompressFailed(String imgPath, String error) {
                continueCompress(image, false, error);
            }
        });

    }

    // 开始递归压缩，不管成功或者失败，都进入集合的下一张需要压缩的图片对象
    private void continueCompress(Photo image, boolean b, String... error) {
        image.setCompressed(b);
        // 获取当前索引
        int index = images.indexOf(image);
        // 判断是否为压缩图片的最后一张
        if (index == images.size() - 1) {
            // 已经压缩完了
            handleCallback(error);
        } else {
            // 递归
            compress(images.get(index + 1));
        }
    }

    private void handleCallback(String... error) {
        if (error.length > 0) {
            listener.onCompressFailed(images, error[0]);
            return;
        }
        for (Photo image : images) {
            if (!image.isCompressed()) {
                listener.onCompressFailed(images, "");
                return;
            }
        }
        listener.onCompressSuccess(images);
    }

}
