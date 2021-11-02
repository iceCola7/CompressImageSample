package com.cxz.compresslib.listener;

import com.cxz.compresslib.bean.Image;

import java.util.ArrayList;

public interface OnCompressImageListener {

    // 成功
    void onCompressSuccess(ArrayList<Image> images);

    // 失败
    void onCompressFailed(ArrayList<Image> images, String error);
}
