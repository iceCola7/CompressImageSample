package com.cxz.compresslib.listener;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc 单张图片压缩时的监听
 */
public interface OnSingleCompressImageListener {

    // 成功
    void onCompressSuccess(String imgPath);

    // 失败
    void onCompressFailed(String imgPath, String error);
}
