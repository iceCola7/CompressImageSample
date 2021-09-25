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
 * @desc 压缩图片管理类
 */
public class CompressImageManager implements CompressImage {

    private CompressImageUtil compressImageUtil; // 压缩工具类
    private ArrayList<Photo> images; // 需要压缩的图片集合
    private CompressListener listener; // 压缩的监听
    private CompressConfig config; // 压缩配置

    private static final String DEFAULT_DISK_CACHE_DIR = "image_cache";

    private CompressImageManager(Context context, CompressConfig compressConfig,
                                 ArrayList<Photo> images, CompressListener listener) {
        this.images = images;
        this.listener = listener;
        this.config = compressConfig == null ? CompressConfig.getDefaultConfig() : compressConfig;
        this.compressImageUtil = new CompressImageUtil(context, config);

        if (TextUtils.isEmpty(this.config.getCacheDir())) {
            // 缓存目录为空，设置默认的缓存目录
            this.config.setCacheDir(getImageCacheDir(context).getAbsolutePath());
        }
    }

    public static CompressImage build(Context context, ArrayList<Photo> images, CompressListener listener) {
        return build(context, CompressConfig.getDefaultConfig(), images, listener);
    }

    public static CompressImage build(Context context, CompressConfig compressConfig,
                                      ArrayList<Photo> images, CompressListener listener) {
        return new CompressImageManager(context, compressConfig, images, listener);
    }

    /**
     * Returns a directory with a default name in the private cache directory of the application to
     * use to store retrieved audio.
     *
     * @param context A context.
     * @see #getImageCacheDir(Context, String)
     */
    private File getImageCacheDir(Context context) {
        return getImageCacheDir(context, DEFAULT_DISK_CACHE_DIR);
    }

    /**
     * Returns a directory with the given name in the private cache directory of the application to
     * use to store retrieved media and thumbnails.
     *
     * @param context   A context.
     * @param cacheName The name of the subdirectory in which to store the cache.
     * @see #getImageCacheDir(Context)
     */
    private File getImageCacheDir(Context context, String cacheName) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir != null) {
            File result = new File(cacheDir, cacheName);
            if (!result.mkdirs() && (!result.exists() || !result.isDirectory())) {
                // File wasn't able to create a directory, or the result exists but not a directory
                return null;
            }
            return result;
        }
        return null;
    }

    @Override
    public void compress() {
        if (images == null || images.isEmpty()) {
            listener.onCompressFailed(images, "images are null");
            return;
        }
        for (Photo image : images) {
            if (image == null) {
                listener.onCompressFailed(images, "image is null");
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
