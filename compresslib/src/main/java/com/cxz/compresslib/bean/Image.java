package com.cxz.compresslib.bean;

/**
 * @author chenxz
 * @date 2019/4/21
 * @desc
 */
public class Image {

    /**
     * 图片原始路径
     */
    private String originalPath;
    /**
     * 是否压缩过
     */
    private boolean compressed;
    /**
     * 压缩后路径
     */
    private String compressPath;

    public Image(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public String getCompressPath() {
        return compressPath;
    }

    public void setCompressPath(String compressPath) {
        this.compressPath = compressPath;
    }

    @Override
    public String toString() {
        return "Image{" +
                "originalPath='" + originalPath + '\'' +
                ", compressed=" + compressed +
                ", compressPath='" + compressPath + '\'' +
                '}';
    }
}
