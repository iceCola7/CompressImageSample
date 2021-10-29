package com.cxz.compresslib.utils;

import android.content.Context;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc
 */
public class CachePathUtil {

    private static final String DEFAULT_DISK_CACHE_DIR = "image_cache";

    /**
     * Returns a directory with a default name in the private cache directory of the application to
     * use to store retrieved audio.
     *
     * @param context A context.
     * @see #getImageCacheDir(Context, String)
     */
    public static File getImageCacheDir(Context context) {
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
    private static File getImageCacheDir(Context context, String cacheName) {
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

    /**
     * Returns a file
     * @param context Context
     * @return File
     */
    public static File getImageCacheFile(Context context) {
        return new File(getImageCacheDir(context), getImageCacheFileName());
    }

    /**
     * Returns a file name
     *
     * @return String
     */
    public static String getImageCacheFileName() {
        return "compress_" + getBaseFileName() + ".jpg";
    }

    private static String getBaseFileName() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
    }
}
