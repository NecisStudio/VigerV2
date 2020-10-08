package com.necistudio.vigerpdf;

import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.necistudio.pdfvigerengine.R;
import com.necistudio.vigerpdf.manage.JobRenderPDF;
import com.necistudio.vigerpdf.manage.OnResultListener;
import com.necistudio.vigerpdf.manage.RenderingPDF;
import com.necistudio.vigerpdf.manage.RenderingPDFNetwork;

import java.io.File;

/**
 * Created by Vim on 1/31/2017.
 */

public class VigerPDF {
    static OnResultListener onResultListener;
    static RenderingPDF renderingPDF;
    static RenderingPDFNetwork renderingPDFNetwork;
    private Context context;

    public VigerPDF(Context context) {
        this.context = context;
    }

    public static void cancel() {
        if (renderingPDF != null) {
            renderingPDF.onDestroy();
        }
        if (renderingPDFNetwork != null) {
            renderingPDFNetwork.onDestory();
        }
    }

    public static void setData(Bitmap itemData) {
        onResultListener.resultData(itemData);
    }

    public static void failedData(Throwable t) {
        onResultListener.failed(t);
    }

    public static void progressData(int progress) {
        onResultListener.progressData(progress);
    }

    public static void onComplete() {
        onResultListener.onComplete();
    }

    public void initFromNetwork(String endpoint, OnResultListener resultListener) {
        onResultListener = resultListener;
        renderingPDFNetwork = new RenderingPDFNetwork(context, endpoint);
    }

    public void initFromFile(File file, OnResultListener resultListener) {
        onResultListener = resultListener;
        renderingPDF = new RenderingPDF(context, file, 0);
    }
}