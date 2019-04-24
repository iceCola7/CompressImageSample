package com.cxz.compresslib.listener;

import com.cxz.compresslib.bean.Photo;

import java.util.ArrayList;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc 图片集合的压缩返回监听
 */
public interface CompressImage {

    // 开始压缩
    void compress();

    interface CompressListener {

        // 成功
        void onCompressSuccess(ArrayList<Photo> images);

        // 失败
        void onCompressFailed(ArrayList<Photo> images, String error);
    }

}
