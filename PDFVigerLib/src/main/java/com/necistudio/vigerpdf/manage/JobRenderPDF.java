package com.necistudio.vigerpdf.manage;

import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

import com.necistudio.pdfvigerengine.R;
import com.necistudio.vigerpdf.VigerPDFv2;
import com.necistudio.vigerpdf.core.MuPDFCore;
import com.necistudio.vigerpdf.network.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobRenderPDF extends JobService {
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notifBuilder;
    private CompositeDisposable disposable = new CompositeDisposable();
    private byte[] filePath;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        // retrieve extra
        final int type = jobParameters.getExtras().getInt("type");

        if (type == 0){  // from local directory
            File file = new File(Objects.requireNonNull(jobParameters.getExtras().getString("file")));
            MuPDFCore core = new MuPDFCore(file.getAbsolutePath());
            renderPDF(core, jobParameters);
        }else {          // from network
            String endPoint = jobParameters.getExtras().getString("endpoint");
            downloadFile(endPoint, jobParameters);
        }
        return false;
    }

    private void downloadFile(String endPoint, final JobParameters jobParameters) {
        RestClient.ApiInterface client = RestClient.getClient();
        Call<ResponseBody> call = client.streamFile(endPoint);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                writeToDisk(response.body(), jobParameters);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                sendBroadCast("failed", null, t);
                onStopJob(jobParameters);
            }
        });
    }

    private void writeToDisk(final ResponseBody body,final JobParameters jobParameters) {
        disposable.clear();
        disposable.add((Disposable) Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = null;
                try {
                    byte[] fileReader = new byte[4096];
                    long fileSize = body.contentLength();
                    inputStream = body.byteStream();
                    outputStream = new ByteArrayOutputStream((int) fileSize);
                    while (true){
                        int read = inputStream.read(fileReader);
                        if (read == -1) break;
                        outputStream.write(fileReader, 0, read);
                    }
                    outputStream.flush();
                    filePath = outputStream.toByteArray();
                    inputStream.close();
                    outputStream.close();
                } catch (IOException ee) {
                    ee.printStackTrace();
                    sendBroadCast("failed", null, ee);
                    e.onError(ee);
                    onStopJob(jobParameters);
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Integer>() {
                    @Override
                    public void onNext(Integer integer) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        sendBroadCast("failed", null, e);
                    }

                    @Override
                    public void onComplete() {
                        MuPDFCore core = new MuPDFCore(filePath, "pdf");
                        renderPDF(core, jobParameters);
                    }
                }));
    }

    private void renderPDF(final MuPDFCore core, final JobParameters jobParameters){
        disposable.clear();
        showNotification();
        disposable.add(Observable.create(new ObservableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(ObservableEmitter<byte[]> e) throws Exception {
                try {
                    for (int i = 0; i < core.countPages(); i++){
                        int width = (int) core.getPageSize(i).x;
                        int height = (int) core.getPageSize(i).y;
                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, new ByteArrayOutputStream());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                            bitmap.eraseColor(0);

                        core.updatePage(bitmap, i, width, height, 0, 0, width, height, null);

                        boolean rotate;
                        rotate = width > height;

                        if (rotate){
                            Matrix matrix = new Matrix();
                            matrix.postRotate(360);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, baos);
                        byte[] bytes = baos.toByteArray();
                        e.onNext(bytes);
                        bitmap.recycle();

                        notifBuilder.setContentText((i*100/core.countPages())+"%");
                        notifBuilder.setProgress(100, i*100/core.countPages(), false);
                        notificationManager.notify(111, notifBuilder.build());
                    }

                    notifBuilder.setContentText("Render finished").setProgress(100, 100, false);
                    notificationManager.notify(111, notifBuilder.build());
                    e.onComplete();
                    jobFinished(jobParameters, false);
                } catch (Exception ee) {
                    sendBroadCast("failed", null, ee);
                    e.onError(ee);
                    onStopJob(jobParameters);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<byte[]>() {
                    @Override
                    public void onNext(byte[] bytes) {
                        sendBroadCast("data", bytes, null);
                    }

                    @Override
                    public void onError(Throwable e) {
                        sendBroadCast("failed", null, e);
                    }

                    @Override
                    public void onComplete() {
                        sendBroadCast("success", null, null);
                    }
                }));
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private void sendBroadCast(String extra, byte[] pageData, Throwable t){
        Intent intent = new Intent(VigerPDFv2.BROAD_FILTER);
        switch (extra){
            case "data" : {
                intent.putExtra("data", pageData);
                break;
            }
            case "success" : {
                intent.putExtra("success", true);
                break;
            }
            case "failed" : {
                intent.putExtra("failed", t);
                break;
            }
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void showNotification(){
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifBuilder = new NotificationCompat.Builder(getBaseContext());
        notifBuilder.setContentTitle("Rendering");
        notifBuilder.setSmallIcon(R.drawable.icmage_search);
        notifBuilder.build();
        notificationManager.notify(111, notifBuilder.build());
    }

}
