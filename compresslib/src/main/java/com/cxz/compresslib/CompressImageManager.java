package com.cxz.compresslib;

import android.content.Context;
import android.text.TextUtils;

import com.cxz.compresslib.bean.Image;
import com.cxz.compresslib.config.CompressConfig;
import com.cxz.compresslib.core.CompressImageCore;
import com.cxz.compresslib.listener.CompressImage;
import com.cxz.compresslib.listener.OnCompressImageListener;
import com.cxz.compresslib.listener.OnSingleCompressImageListener;
import com.cxz.compresslib.utils.CachePathUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc 压缩图片管理类
 */
public class CompressImageManager implements CompressImage {

    private CompressImageCore compressImageCore; // 压缩工具类
    private ArrayList<Image> images; // 需要压缩的图片集合
    private OnCompressImageListener onCompressImageListener; // 压缩的监听
    private CompressConfig config; // 压缩配置

    private CompressImageManager(Context context, CompressConfig compressConfig,
                                 ArrayList<Image> images, OnCompressImageListener onCompressImageListener) {
        this.images = images;
        this.onCompressImageListener = onCompressImageListener;
        this.config = compressConfig == null ? CompressConfig.getDefaultConfig() : compressConfig;
        this.compressImageCore = new CompressImageCore(config);
        if (TextUtils.isEmpty(this.config.getCacheDir())) {
            // 缓存目录为空，设置默认的缓存目录
            this.config.setCacheDir(CachePathUtil.getImageCacheDir(context).getAbsolutePath());
        }
    }

    public static CompressImage build(Context context, ArrayList<Image> images, OnCompressImageListener onCompressImageListener) {
        return build(context, CompressConfig.getDefaultConfig(), images, onCompressImageListener);
    }

    public static CompressImage build(Context context, CompressConfig compressConfig,
                                      ArrayList<Image> images, OnCompressImageListener onCompressImageListener) {
        return new CompressImageManager(context, compressConfig, images, onCompressImageListener);
    }

    @Override
    public void compress() {
        if (images == null || images.isEmpty()) {
            onCompressImageListener.onCompressFailed(images, "images are null");
            return;
        }
        for (Image image : images) {
            if (image == null) {
                onCompressImageListener.onCompressFailed(images, "image is null");
                return;
            }
        }
        compress(images.get(0));
    }

    // 从需要压缩的图片集合中，第一张开始压缩
    private void compress(final Image image) {
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
            // 不用压缩的话，压缩的路径设置成原路径
            image.setCompressPath(image.getOriginalPath());
            continueCompress(image, true);
            return;
        }

        // 开始压缩
        compressImageCore.compress(image.getOriginalPath(), new OnSingleCompressImageListener() {
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
    private void continueCompress(Image image, boolean b, String... error) {
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
            onCompressImageListener.onCompressFailed(images, error[0]);
            return;
        }
        for (Image image : images) {
            if (!image.isCompressed()) {
                onCompressImageListener.onCompressFailed(images, "");
                return;
            }
        }
        onCompressImageListener.onCompressSuccess(images);
    }

}
