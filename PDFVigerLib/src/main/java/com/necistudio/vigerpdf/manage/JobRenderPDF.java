package com.necistudio.vigerpdf.manage;

import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.necistudio.pdfvigerengine.R;
import com.necistudio.vigerpdf.VigerPDFv2;

import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.pdfdroid.codec.PdfContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobRenderPDF extends JobService {
    private boolean status = false;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notifBuilder;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        disposable.clear();
        showNotification();
        final File file = new File(Objects.requireNonNull(jobParameters.getExtras().getString("file")));
        final int type = jobParameters.getExtras().getInt("type");

        disposable.add(Observable.create(new ObservableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(ObservableEmitter<byte[]> e) throws Exception {
                try {
                    DecodeServiceBase decodeService = new DecodeServiceBase(new PdfContext());
                    decodeService.setContentResolver(getContentResolver());
                    decodeService.open(Uri.fromFile(file));
                    int pageCount = decodeService.getPageCount();
                    for (int i = 0; i < pageCount; i++) {
                        CodecPage page = decodeService.getPage(i);
                        RectF rectF = new RectF(0, 0, 1, 1);
                        Bitmap bitmap = page.renderBitmap(decodeService.getPageWidth(i), decodeService.getPageHeight(i), rectF);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] bytes = baos.toByteArray();
                        bitmap.recycle();

                        notifBuilder.setContentText((i*100/pageCount)+"%");
                        notifBuilder.setProgress(100, i*100/pageCount, false);
                        notificationManager.notify(111, notifBuilder.build());

                        e.onNext(bytes);
                    }

                    if (type == 1) {
                        file.delete();
                    }

                    notifBuilder.setContentText("Render finished").setProgress(100, 100, false);
                    notificationManager.notify(111, notifBuilder.build());
                    jobFinished(jobParameters, false);
                    e.onComplete();
                } catch (Exception ee) {
                    onStopJob(jobParameters);
                    e.onError(ee);
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

        /*try {
            DecodeServiceBase decodeService = new DecodeServiceBase(new PdfContext());
            decodeService.setContentResolver(context.getContentResolver());
            decodeService.open(Uri.fromFile(file));
            int pageCount = decodeService.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                if (status) {
                    notifBuilder.setContentText("Render finished").setProgress(100, 100, false);
                    notificationManager.notify(111, notifBuilder.build());

                    sendBroadCast("success", null, null);
                    jobFinished(jobParameters, false);
                }
                CodecPage page = decodeService.getPage(i);
                RectF rectF = new RectF(0, 0, 1, 1);
                Bitmap bitmap = page.renderBitmap(decodeService.getPageWidth(i), decodeService.getPageHeight(i), rectF);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] bytes = baos.toByteArray();
                bitmap.recycle();

                notifBuilder.setContentText((i*100/pageCount)+"%");
                notifBuilder.setProgress(100, i*100/pageCount, false);
                notificationManager.notify(111, notifBuilder.build());

                sendBroadCast("data", bytes, null);
            }

            if (type == 1) {
                file.delete();
            }

            notifBuilder.setContentText("Render finished").setProgress(100, 100, false);
            notificationManager.notify(111, notifBuilder.build());

            sendBroadCast("success", null, null);
            jobFinished(jobParameters, false);
        } catch (Exception ee) {
            sendBroadCast("failed", null, ee);
            onStopJob(jobParameters);
        }*/

        return false;
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
