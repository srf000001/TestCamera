package com.arno.testsystemcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/*
 Developed by Srf,Solving the problem that photo of fullsize will filp 90°;
 using 2 function: readPictureDegree &  rotaingImageView
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();
    // request_code
    private final int REQ_THUMB_NAIL_CAPTURE = 1;
    private final int REQ_FULL_SIZE_CAPTURE = 2;
    // permission_request_code
    private final int REQ_PERMISSION_THUMB = 1;
    private final int REQ_PERMISSION_FULL = 2;
    // bundle_key
    private final String IMG_PATH_KEY = "img_path_key";

    private String curImgPath;
    private ImageView mImageView;
    private int mImgHeight;
    private int mImgWidth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: " + savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_1 = (Button) findViewById(R.id.btn_l);
        Button btn_2 = (Button) findViewById(R.id.btn_r);
        btn_1.setOnClickListener(this);
        btn_2.setOnClickListener(this);

        mImageView = ((ImageView) findViewById(R.id.img_show));
        ViewGroup.LayoutParams layoutParams = mImageView.getLayoutParams();
        mImgHeight = layoutParams.height;
        mImgWidth = layoutParams.width;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: ");
    }

    /**
     * 使用相机时的权限请求
     * @param REQ_PERMISSION
     */
    private void requestPermission(int REQ_PERMISSION) {
        /* 如果没有权限则申请权限*/
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISSION);
        }else{
            switch (REQ_PERMISSION) {
                case REQ_PERMISSION_THUMB:
                    Intent thumbIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(thumbIntent, REQ_THUMB_NAIL_CAPTURE);
                    break;
                case REQ_PERMISSION_FULL:
                    takeFullSizePhoto();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION_THUMB:
                if (grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                    Intent thumbIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(thumbIntent, REQ_THUMB_NAIL_CAPTURE);
                }
                break;
            case REQ_PERMISSION_FULL:
                if (grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                    takeFullSizePhoto();
                }
                break;
        }
    }

    /**
     * 拍摄全尺寸图片
     */
    public void takeFullSizePhoto(){
        File photoFile = createPhotoFile();
        Uri uri = performUri(photoFile);
        Log.d(TAG, "uri: " + uri);
        Intent fullIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fullIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        fullIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(fullIntent, REQ_FULL_SIZE_CAPTURE);
    }

    @Override
    public void onClick(View v) {
        // 判断是否有相机设备
        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            Toast.makeText(this, "未检测到相机设备", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (v.getId()) {
            case R.id.btn_l:
                requestPermission(REQ_PERMISSION_THUMB);
                break;
            case R.id.btn_r:
                requestPermission(REQ_PERMISSION_FULL);
                break;
        }
    }

    /**
     * 创建保存图片路径
     */
    private File createPhotoFile() {
        String timeTip = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        // 保存当前图片路径
        curImgPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + timeTip + ".jpg";
        Log.d(TAG, "picPath:" + curImgPath);
        return new File(curImgPath);
    }

    /**
     * 根据版本生成 uri
     * @param file
     * @return
     */
    private Uri performUri(File file) {
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(MainActivity.this, "com.test.fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult: requestCode-" + requestCode + ",resultCode-" + resultCode + ",data:" + data + ",curImgPath-" + curImgPath);
        switch (requestCode) {
            case REQ_THUMB_NAIL_CAPTURE:
                if (resultCode != RESULT_OK) return;
                Bundle b = data.getExtras();
                Bitmap thumbBitmap = (Bitmap) b.get("data");
                mImageView.setImageBitmap(thumbBitmap);
                break;
            case REQ_FULL_SIZE_CAPTURE:
                if (resultCode != RESULT_OK) return;

                Bitmap bitmap = BitmapFactory.decodeFile(curImgPath);
                bitmap = compressBitmap(bitmap);

                mImageView.setImageBitmap(rotaingImageView(readPictureDegree(curImgPath),bitmap));
                break;
        }

    }

    /**
     * 压缩图片
     * @param bitmap
     * @return
     */
    private Bitmap compressBitmap(Bitmap bitmap) {
        long start = System.currentTimeMillis(), end;
        if (bitmap != null) {
            int quality = 100;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            while (baos.toByteArray().length / 1024 > 0.4 * 1024) {
                if (11 > quality) break;
                else quality -= 10;

                baos.reset();

                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                Log.i(TAG, "compressBitmap: size - " + baos.toByteArray().length);

            }
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray(), 0, baos.toByteArray().length);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = calSampleSize(opt, mImgWidth, mImgHeight);
            opt.inPurgeable = true;// api > 21
            opt.inInputShareable = true;
            bitmap = BitmapFactory.decodeStream(bais, null, opt);
            close(bais, baos);
        }
        end = System.currentTimeMillis();
        Log.d(TAG, "compressBitmap: cost - " + (end - start) / 1000);
        return bitmap;
    }

    /**
     * 获取合适的采样率
     * @param opt
     * @param mImgWidth
     * @param mImgHeight
     * @return
     */
    private int calSampleSize(BitmapFactory.Options opt, int mImgWidth, int mImgHeight) {
        int outWidth = opt.outWidth;
        int outHeight = opt.outHeight;
        int radio_w = 1, radio_h = 1;
        int sampleSize = 1;
        if (outWidth > mImgWidth || outHeight > mImgHeight) {
            radio_w = outWidth / mImgWidth;
            radio_h = outWidth / mImgHeight;
            sampleSize = Math.min(radio_w, radio_h);
        }
        Log.i("calSampleSize", "sampleSize: " + sampleSize);
        return sampleSize;
    }

    /**
     * 关闭流
     * @param cList
     */
    private void close(Closeable... cList) {
        for (Closeable c : cList) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取图片属性：旋转的角度
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }


    /*
     * 旋转图片
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    public Bitmap rotaingImageView(int angle , Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
