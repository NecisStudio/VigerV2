package com.necistudio.pdfengineme;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.necistudio.vigerpdf.VigerPDF;
import com.necistudio.vigerpdf.VigerPDFv2;
import com.necistudio.vigerpdf.adapter.VigerAdapter;
import com.necistudio.vigerpdf.adapter.VigerAdapterV2;
import com.necistudio.vigerpdf.manage.OnResultListener;
import com.necistudio.vigerpdf.manage.OnResultListenerV2;
import com.necistudio.vigerpdf.utils.ViewPagerZoomHorizontal;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private ArrayList<byte[]> itemDataV2;
    private VigerAdapterV2 adapterV2;
    private Button btnFromFile, btnFromNetwork,btnCancle;
    private VigerPDFv2 vigerPDFV2;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = (ViewPagerZoomHorizontal) findViewById(R.id.viewPager);
        btnFromFile = (Button) findViewById(R.id.btnFile);
        btnFromNetwork = (Button) findViewById(R.id.btnNetwork);
        btnCancle = (Button)findViewById(R.id.btnCancle);
        setupV2();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupV2() {
        vigerPDFV2 = new VigerPDFv2(this);
        btnCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vigerPDFV2.cancel();
            }
        });
        btnFromFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialFilePicker()
                        .withActivity(MainActivity.this)
                        .withRequestCode(100)
                        .withFilter(Pattern.compile(".*\\.pdf$"))
                        .start();
            }
        });

        btnFromNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemDataV2.clear();
                adapterV2.notifyDataSetChanged();
                //fromNetwork("http://www.pdf995.com/samples/pdf.pdf");
            }
        });
        itemDataV2 = new ArrayList<>();
        adapterV2 = new VigerAdapterV2(getApplicationContext(), itemDataV2);
        viewPager.setAdapter(adapterV2);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // v1
            //itemData.clear();
            //adapter.notifyDataSetChanged();
            //String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            //fromFile(filePath);

            // v2
            itemDataV2.clear();
            adapterV2.notifyDataSetChanged();
            String filePathV2 = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            fromFileV2(filePathV2);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void fromNetwork(String endpoint) {
        itemDataV2.clear();
        adapterV2.notifyDataSetChanged();
        vigerPDFV2.cancel();
        vigerPDFV2.initFromNetwork(endpoint, new OnResultListenerV2() {
            @Override
            public void resultData(byte[] data) {
                Log.e("data", "run");
                itemDataV2.add(data);
                adapterV2.notifyDataSetChanged();
            }

            @Override
            public void progressData(int progress) {
                Log.e("data", "" + progress);
            }

            @Override
            public void failed(Throwable t) {

            }

            @Override
            public void onComplete() {

            }

        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void fromFileV2(String path) {
        final ProgressDialog progressDialog =  new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        itemDataV2.clear();
        adapterV2.notifyDataSetChanged();
        File file = new File(path);
        vigerPDFV2.cancel();
        vigerPDFV2.initFromFile(file, new OnResultListenerV2() {
            @Override
            public void resultData(byte[] data) {
                itemDataV2.add(data);
            }

            @Override
            public void progressData(int progress) {
                Log.e("data", "" + progress);

            }

            @Override
            public void failed(Throwable t) {
                Log.e("error", " : " + t.getMessage());
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
                progressDialog.dismiss();
                adapterV2.notifyDataSetChanged();
            }

        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vigerPDFV2 != null) vigerPDFV2.cancel();
    }
}
