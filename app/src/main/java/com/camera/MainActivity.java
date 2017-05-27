package com.camera;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.R.attr.path;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final String TAG = "TAG";
    private SurfaceView sfvBack;
    private SurfaceView sfvFront;
    private SurfaceHolder mSurfaceHolderBack;
    private SurfaceHolder mSurfaceHolderFront;
    private RelativeLayout rl;
    private Button tackPicture;
    private int lastX;
    private int lastY;
    private int screenWidth;
    private int screenHeight;
    private Camera mcamera = null;
    private Camera mcamera2 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels - 50;
        getSupportActionBar().hide();
        sfvBack = (SurfaceView) findViewById(R.id.sv);
        rl = (RelativeLayout) findViewById(R.id.rl);
        rl.setOnTouchListener(this);
        tackPicture = (Button) findViewById(R.id.btn_tack_picture);
        tackPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Camera.Parameters parameters = mcamera.getParameters();
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);//自动识别打开闪光灯
                    // 设置对焦方式，这里设置自动对焦
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    mcamera2.setParameters(parameters);
                    mcamera2.takePicture(shutterCallback, null, jpeg);

                    mcamera.takePicture(shutterCallback, null, jpeg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mSurfaceHolderBack = sfvBack.getHolder();
        //对 surfaceView 进行操作
        mSurfaceHolderBack.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mcamera2 != null) {
                    mcamera2.stopPreview();
                    mcamera2.release();
                    mcamera2 = null;
                } else {
                    try {
                        mcamera2 = Camera.open(0);//后置
                        mcamera2.setDisplayOrientation(90);
                        mcamera2.setPreviewDisplay(mSurfaceHolderBack);
//                        setPreviewSize();
                        mcamera2.startPreview();
                        return;
                    } catch (IOException localIOException) {
                        localIOException.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mcamera2.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initCamera();//实现相机的参数初始化
                            camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
                        }
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mcamera2.stopPreview();
                mcamera2.release();
                mcamera2 = null;
            }
        });
        this.mSurfaceHolderBack.setType(3);
        sfvFront = (SurfaceView) findViewById(R.id.sv_mini);
        //这两个方法差不多，设置了就会浮现到顶部，但是，后面的看不见，要像下面设置为透明
        sfvFront.setZOrderOnTop(true);
        sfvFront.setZOrderMediaOverlay(true);
        mSurfaceHolderFront = sfvFront.getHolder();
        //设置透明
        mSurfaceHolderFront.setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolderBack.setFormat(PixelFormat.TRANSPARENT);

        mSurfaceHolderFront.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mcamera != null) {
                    mcamera.stopPreview();
                    mcamera.release();
                    mcamera = null;
                }
                mcamera = Camera.open(1);//前置
                mcamera.setDisplayOrientation(270);
                mcamera.setFaceDetectionListener(faceDetectionListener);

                try {
                    mcamera.setPreviewDisplay(mSurfaceHolderFront);
                    mcamera.startPreview();
                    mcamera.startFaceDetection();
                    return;
                } catch (IOException localIOException) {
                    localIOException.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //设置参数并开始预览
                //实现自动对焦
                SystemClock.sleep(500);
                Camera.Parameters params = mcamera.getParameters();
                params.setPictureFormat(PixelFormat.JPEG);
                params.setPreviewSize(640, 480);
                // 设置预览照片时每秒显示多少帧的最小值和最大值
                params.setPreviewFpsRange(4, 10);
                // 设置图片格式
                params.setPictureFormat(ImageFormat.JPEG);
                // 设置JPG照片的质量
                params.set("jpeg-quality", 85);
                mcamera.setParameters(params);
                mcamera.startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mcamera.stopPreview();
                mcamera.release();
                mcamera = null;
            }
        });
        this.mSurfaceHolderFront.setType(3);
    }

    Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Toast.makeText(MainActivity.this, "识别成功", Toast.LENGTH_SHORT).show();
            }

        }
    };

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int ea = event.getAction();
        switch (ea) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                break;
            /**
             * layout(l,t,r,b)
             * l  Left position, relative to parent
             t  Top position, relative to parent
             r  Right position, relative to parent
             b  Bottom position, relative to parent
             * */
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getRawX() - lastX;
                int dy = (int) event.getRawY() - lastY;

                int left = v.getLeft() + dx;
                int top = v.getTop() + dy;
                int right = v.getRight() + dx;
                int bottom = v.getBottom() + dy;

                if (left < 0) {
                    left = 0;
                    right = left + v.getWidth();
                }

                if (right > screenWidth) {
                    right = screenWidth;
                    left = right - v.getWidth();
                }

                if (top < 0) {
                    top = 0;
                    bottom = top + v.getHeight();
                }

                if (bottom > screenHeight) {
                    bottom = screenHeight;
                    top = bottom - v.getHeight();
                }

                v.layout(left, top, right, bottom);

                Log.i("", "position：" + left + ", " + top + ", " + right + ", " + bottom);

                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();

                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    //相机参数的初始化设置
    private void initCamera() {
        Camera.Parameters parameters = mcamera2.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
//        parameters.setPictureSize(sfvBack.getWidth(), sfvBack.getHeight());
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        mcamera2.setParameters(parameters);
        mcamera2.startPreview();
        mcamera2.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上

    }

    private void setPreviewSize() {
        Camera.Parameters params = mcamera2.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            Log.d("previewSize", "width:" + size.width + " height " + size.height);
        }
        for (Camera.Size size : sizes) {
            if (size.width / 5 == size.height / 3) {
                params.setPreviewSize(size.width, size.height);
                Log.d("previewSize", "SET width:" + size.width + " height " + size.height);
                break;
            }
        }
        // params一定要记得写回Camera
        mcamera2.setParameters(params);
        mcamera2.startPreview();
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
        }
    };

    int i = 0;
    //创建jpeg图片回调数据对象
    Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            // 首先保存图片
            File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            String fileName = System.currentTimeMillis() + ".jpg";
            File file = new File(appDir, fileName);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 其次把文件插入到系统图库
            try {
                MediaStore.Images.Media.insertImage(MainActivity.this.getContentResolver(),
                        file.getAbsolutePath(), fileName, null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            // 最后通知图库更新
            MainActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));

            if (i == 1) {
                mcamera2.stopPreview();
                mcamera2.startPreview();
                mcamera.stopPreview();
                mcamera.startPreview();
            } else {
                i += 1;
            }

        }
    };
}
