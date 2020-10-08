package com.necistudio.vigerpdf.manage;

import android.graphics.Bitmap;

/**
 * Created by Vim on 1/31/2017.
 */

public interface OnResultListenerV2 {
    void resultData(byte[] data);
    void progressData(int progress);
    void failed(Throwable t);
    void onComplete();
}