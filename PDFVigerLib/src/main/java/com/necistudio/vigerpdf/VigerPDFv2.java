package com.necistudio.vigerpdf;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

import com.necistudio.vigerpdf.manage.JobRenderPDF;
import com.necistudio.vigerpdf.manage.OnResultListenerV2;
import com.necistudio.vigerpdf.network.RestClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VigerPDFv2 {
    int JOB_ID = 100;
    public static String BROAD_FILTER = "BROAD";
    private OnResultListenerV2 onResultListener;
    private JobScheduler jobScheduler;
    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VigerPDFv2(Context context){
        this.context = context;
        this.jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter(BROAD_FILTER));
    }

    private void setData(byte[] page) {
        onResultListener.resultData(page);
    }

    private void progressData(int progress){
        onResultListener.progressData(progress);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void onComplete(){
        onResultListener.onComplete();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        jobScheduler.cancel(JOB_ID);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void cancel(){
        jobScheduler.cancel(JOB_ID);
    }

    private void onFailed(Throwable t) {
        onResultListener.failed(t);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initFromNetwork(String endpoint, OnResultListenerV2 resultListener){
        onResultListener = resultListener;

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("endpoint", endpoint);
        bundle.putInt("type", 1);

        ComponentName componentName = new ComponentName(context, JobRenderPDF.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName).setExtras(bundle);
        assert jobScheduler != null;
        jobScheduler.schedule(builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initFromFile(File file, OnResultListenerV2 resultListener) {
        onResultListener = resultListener;

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("file", file.getAbsolutePath());
        bundle.putInt("type", 0);

        ComponentName componentName = new ComponentName(context, JobRenderPDF.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName).setExtras(bundle);
        assert jobScheduler != null;
        jobScheduler.schedule(builder.build());
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null){
                if (intent.getByteArrayExtra("data") != null){
                    byte[] bitmap = intent.getByteArrayExtra("data");
                    setData(bitmap);
                }
                if (intent.getBooleanExtra("success", false)){
                    onComplete();
                }
                if (intent.getSerializableExtra("failed") != null){
                    Throwable throwable = (Throwable) intent.getSerializableExtra("failed");
                    onFailed(throwable);
                }
            }
        }
    };
}
