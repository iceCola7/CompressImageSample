package com.cxz.compresslib.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import com.cxz.compresslib.config.CompressConfig;
import com.cxz.compresslib.listener.OnSingleCompressImageListener;
import com.cxz.compresslib.utils.CachePathUtil;
import com.cxz.compresslib.utils.ThreadPoolManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc
 */
public class CompressImageCore {

    private CompressConfig config;

    public CompressImageCore(CompressConfig config) {
        this.config = config == null ? CompressConfig.getDefaultConfig() : config;
    }

    public void compress(String imagePath, OnSingleCompressImageListener listener) {
        if (config.isEnablePixelCompress()) {
            try {
                compressImageByPixel(imagePath, listener);
            } catch (Exception e) {
                listener.onCompressFailed(imagePath, String.format("image compress failed %s", e.toString()));
                e.printStackTrace();
            }
        } else {
            compressImageByQuality(imagePath, listener);
        }
    }

    /**
     * 多线程压缩图片的质量
     */
    private void compressImageByQuality(final String imagePath, final OnSingleCompressImageListener listener) {
        // TODO: 2019/4/21
        ThreadPoolManager.getInstance().runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File targetFile = compressRealImageByQ(imagePath);
                    ThreadPoolManager.getInstance().runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onCompressSuccess(targetFile.getAbsolutePath());
                        }
                    });
                } catch (final Exception e) {
                    ThreadPoolManager.getInstance().runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onCompressFailed(imagePath, e.getMessage());
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }

    private void compressImageByPixel(final String imagePath, final OnSingleCompressImageListener listener) {
        // TODO: 2019/4/21
        ThreadPoolManager.getInstance().runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File targetFile = compressRealImageByP(imagePath);
                    ThreadPoolManager.getInstance().runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onCompressSuccess(targetFile.getAbsolutePath());
                        }
                    });
                } catch (final Exception e) {
                    ThreadPoolManager.getInstance().runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onCompressFailed(imagePath, e.getMessage());
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }

    private File compressRealImageByQ(String srcPath) throws Exception {
        Bitmap srcBitmap = BitmapFactory.decodeFile(srcPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 将图片旋转正确的角度
        srcBitmap = rotatingImage(srcBitmap, getBitmapDegree(srcPath));

        // 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        int options = 100;
        while (baos.toByteArray().length / 1024 > 200) { //循环判断如果压缩后图片是否大于200kb,大于继续压缩
            // 重置baos即清空baos
            baos.reset();
            // 这里压缩options%，把压缩后的数据存放到baos中
            srcBitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
            // 每次都减少10
            options -= 10;
        }
        return writeToLocal(baos);
    }

    private File compressRealImageByP(String srcPath) throws Exception {
        FileInputStream srcIs = new FileInputStream(srcPath);

        BitmapFactory.Options options = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;
        BitmapFactory.decodeStream(srcIs, null, options);
        options.inJustDecodeBounds = false;
        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;

        // 关闭输入流
        srcIs.close();

        options.inSampleSize = computeSize(srcWidth, srcHeight);

        FileInputStream newSrcIs = new FileInputStream(srcPath);

        Bitmap targetBitmap = BitmapFactory.decodeStream(newSrcIs, null, options);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // 将图片旋转正确的角度
        targetBitmap = rotatingImage(targetBitmap, getBitmapDegree(srcPath));

        targetBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        targetBitmap.recycle();

        // 关闭输入流
        newSrcIs.close();

        return writeToLocal(stream);
    }

    private File writeToLocal(ByteArrayOutputStream stream) throws Exception {
        File targetFile = new File(config.getCacheDir(), CachePathUtil.getImageCacheFileName());
        FileOutputStream fos = new FileOutputStream(targetFile);
        fos.write(stream.toByteArray());
        fos.flush();
        fos.close();
        stream.close();
        return targetFile;
    }

    private int computeSize(int srcWidth, int srcHeight) {
        srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
        srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

        int longSide = Math.max(srcWidth, srcHeight);
        int shortSide = Math.min(srcWidth, srcHeight);

        float scale = ((float) shortSide / longSide);
        if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                return 1;
            } else if (longSide < 4990) {
                return 2;
            } else if (longSide > 4990 && longSide < 10240) {
                return 4;
            } else {
                return longSide / 1280 == 0 ? 1 : longSide / 1280;
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            return longSide / 1280 == 0 ? 1 : longSide / 1280;
        } else {
            return (int) Math.ceil(longSide / (1280.0 / scale));
        }
    }

    /**
     * 读取图片的旋转的角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    private Bitmap rotatingImage(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();

        matrix.postRotate(angle);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    /**********************************************************************************************/
    /**
     * 质量压缩法
     */
    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) { //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    /**
     * 图片按比例大小压缩方法（根据路径获取图片并压缩）
     */
    private Bitmap getImage(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);//此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

    /**
     * 图片按比例大小压缩方法（根据Bitmap图片压缩）
     */
    private Bitmap comp(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        if (baos.toByteArray().length / 1024 > 1024) {//判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 50, baos);//这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

}
