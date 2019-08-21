package com.example.ahn.asm_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private MediaProjectionManager mpm;
    private MediaProjection mediaProjection;
    private ImageReader mImageReader;

    public static final int  REQUEST_CODE_MIRROR=3171;

    int width;
    int height;
    int density;

    Connector ct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button)findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                EditText et = (EditText)findViewById(R.id.editText);
                String code = et.getText().toString();
                ct = new Connector("3171");
                Thread getJson= new Thread() {
                    public void run() {
                        ct.GetServerInfo();
                    }
                };
                getJson.start();
            }
        });

        Button btn2 = (Button)findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(ct!=null) {
                    if(ct.isReady)
                        startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CODE_MIRROR);
                }else{
                    Toast toast = Toast.makeText(MainActivity.this, "check your code",
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        //get screen size information
        final DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealMetrics(metrics);

        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_MIRROR) {
            // permission check
            if (resultCode == RESULT_OK) {
                mediaProjection = mpm.getMediaProjection(resultCode, data);

                if (mediaProjection != null) {
                    int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;
                    mediaProjection.createVirtualDisplay("VirtualDisplay", width, height, density, flags, mImageReader.getSurface(), null, null);
                    myThread.start();
                }
            }else{
                Toast toast = Toast.makeText(this, "should allow permission.",
                        Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
            super.onActivityResult(requestCode, resultCode, data);
        }else{
            Toast toast = Toast.makeText(this, "should allow permission.",
                    Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    private Thread myThread = new Thread() {
        public void run() {
            Socket socket=null;
            OutputStream os = null;
            DataOutputStream dos = null;
            InputStream is = null;
            DataInputStream dis=null;

            try {
                socket = new Socket(ct.GetIp(),ct.GetPort());
                os = socket.getOutputStream();
                dos = new DataOutputStream(os);
                is = socket.getInputStream();
                dis = new DataInputStream(is);

                while(true) {
                    Date startTime2 = new Date();
                    byte [] bytes = new byte[4];

                    //PC Server requested screen image
                    dis.read(bytes,0,4);

                    Image image = null;
                    while (true) {
                        image = mImageReader.acquireLatestImage();
                        if (image != null) {
                            break;
                        }
                    }

                    Date startTime = new Date();

                    byte [] imageBytes = ImageToByteArray(image);
                    image.close();
                    image=null;

                    Date endTime = new Date();
                    long lTime = endTime.getTime() - startTime.getTime();
                    Log.e("Image to byte array","TIME : " + lTime + "(ms)");

                    Log.e("size",imageBytes.length+"");
                    bytes = intToByteArray(imageBytes.length);

                    //send image size
                    dos.write(bytes,0,4);

                    //send image byte stream
                    dos.write(imageBytes,0,imageBytes.length);
                    Date endTime2 = new Date();
                    long lTime2 = endTime2.getTime() - startTime2.getTime();
                    Log.e("mirror process","TIME : " + lTime2 + "(ms)");
                }

            }catch(SocketException se){
                se.printStackTrace();
                try {
                    Log.e("Mirroring Log","connection reset");
                    dis.close();
                    is.close();
                    dos.close();
                    os.close();
                    socket.close();
                }catch(IOException ie){
                    ie.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            //screencapture stop
            mediaProjection.stop();
            finish();
        }
    };

    public byte [] ImageToByteArray(Image image){
        try {
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();

            int width= image.getWidth();
            int height = image.getHeight();
            Log.e(width+"*",height+"");

            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            //image to bitmap
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            buffer.clear();

            //bitmap resize
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, width/4, height/4, true);
            bitmap.recycle();
            bitmap = null;

            //bitmap to stream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, out);
            resized.recycle();
            resized=null;

            //stream to byte array
            byte[] bytes = out.toByteArray();
            out.close();

            return bytes;

        }catch(IOException ioe){
            ioe.printStackTrace();
            return null;
        }
    }

    public byte[] intToByteArray(int a)
    {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
}
