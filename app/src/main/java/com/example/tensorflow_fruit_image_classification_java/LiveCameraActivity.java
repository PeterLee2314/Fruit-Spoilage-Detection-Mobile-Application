package com.example.tensorflow_fruit_image_classification_java;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.tensorflow_fruit_image_classification_java.ml.Detect;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;

public class LiveCameraActivity extends AppCompatActivity {

    private TextureView textureView;
    private ImageView imageView;
    private CameraManager cameraManager;
    private CameraDevice myCameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession myCameraCaptureSession;
    private Paint paint = new Paint();
    private Detect model;
    private String[] classesLabel = {"fresh", "spoiled"};
    private int[] colorText = {Color.RED, Color.GREEN};
    private String stringCameraID;
    Bitmap image;
    private Button startLiveCamera, stopLiveCamera, backButton;
    int imageViewWidth, imageViewHeight;

    @Override
    protected void onCreate(Bundle savedInstanceStace) {
        super.onCreate(savedInstanceStace);
        setContentView(R.layout.activity_live_camera);
        imageView = findViewById(R.id.imageView);
        textureView = findViewById(R.id.textureView);
        startLiveCamera = findViewById(R.id.button5);
        stopLiveCamera = findViewById(R.id.button6);
        backButton = findViewById(R.id.button7);
        try {
            model = Detect.newInstance(getApplicationContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonStopCamera(v);
                goPreviousButton(v);
            }
        });

        startLiveCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonStartCamera();
            }
        });
        stopLiveCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonStopCamera(v);
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            boolean processing;
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                startCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                if (processing) {
                    return;
                }
                processing = true;
                Bitmap image = image = Bitmap.createScaledBitmap(textureView.getBitmap(), 320, 320, false);
                new detectionTask(image, new ImageResponse () {
                    @Override
                    public void processFinished() {
                        processing = false;
                    }
                }).execute();

            }
        });
    }
    private void goPreviousButton(View view){
        Intent intent = new Intent(this, MainActivity.class);
        view.getContext().startActivity(intent);
    }
    private interface ImageResponse {
        void processFinished();
    }

    private class detectionTask  extends AsyncTask<Void, Void, Bitmap> {
        private Bitmap image;
        private ImageResponse  imageResponse;
        detectionTask(Bitmap image, ImageResponse imageResponse) {
            this.image = image;
            this.imageResponse = imageResponse;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                int imageSize = 320;
                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 320, 320, 3}, DataType.FLOAT32);
                TensorImage tensorImage = TensorImage.fromBitmap(image);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                byteBuffer.order(ByteOrder.nativeOrder());

                int[] intValues = new int[imageSize * imageSize];
                image.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize);
                // image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
                int pixel = 0;
                for (int i = 0; i < imageSize; i++) {
                    for (int j = 0; j < imageSize; j++) {
                        // extract R,G,B by bitwise
                        // /1 because preprocessing scaled from 0 (fresh) to 1 (spoiled)
                        int val = intValues[pixel++];
                        byteBuffer.putFloat(((val >> 16) & 0xFF) / 256.0f + (0.5f / 256.0f));
                        byteBuffer.putFloat(((val >> 8) & 0xFF) / 256.0f + (0.5f / 256.0f));
                        byteBuffer.putFloat((val & 0xFF) / 256.0f + (0.5f / 256.0f));
                    }
                }

                inputFeature0.loadBuffer(byteBuffer);
                // Runs model inference and gets result.
                Detect.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                TensorBuffer outputFeature1 = outputs.getOutputFeature1AsTensorBuffer();
                TensorBuffer outputFeature2 = outputs.getOutputFeature2AsTensorBuffer();
                TensorBuffer outputFeature3 = outputs.getOutputFeature3AsTensorBuffer();
                float[] confidences = outputFeature0.getFloatArray();
                float[] boxes = outputFeature1.getFloatArray();
                float[] len = outputFeature2.getFloatArray();

                float[] classes = outputFeature3.getFloatArray();

                Log.d("confidences ", Arrays.toString(confidences) + "");
                Log.d("boxes ", Arrays.toString(boxes) + "");
                Log.d("len ", Arrays.toString(len) + "");
                Log.d("classes ", Arrays.toString(classes) + "");
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                Bitmap blankImage = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(blankImage);
                paint.setTextSize(imageHeight / 15f);
                paint.setStrokeWidth(imageHeight / 85f);
                // Get the dimensions of the model's input size
                int modelInputSize = 320; // Assuming the model input size is 320x320

                // Calculate the scaling factors to map the model's coordinates to the image's coordinates
                float scaleX = (float) imageWidth / modelInputSize;
                float scaleY = (float) imageHeight / modelInputSize;
                for (int i = 0, j = 0; j < confidences.length; j++, i += 4) {
                    if (confidences[j] <= 0.5) continue;
                    float ymin = boxes[i] * modelInputSize * scaleY;
                    float xmin = boxes[i + 1] * modelInputSize * scaleX;
                    float ymax = boxes[i + 2] * modelInputSize * scaleY;
                    float xmax = boxes[i + 3] * modelInputSize * scaleX;

                    ymin = Math.max(1f, ymin);
                    xmin = Math.max(1f, xmin);
                    ymax = Math.max(1f, ymax);
                    xmax = Math.max(1f, xmax);
                    // Create a Paint object for drawing bounding boxes
                    if(classes[j] == 1){
                        paint.setColor(Color.RED);
                    }else{
                        paint.setColor(Color.GREEN);
                    }
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2.0f);
                    //Log.d("draw", ymin+" "+xmin+" "+ymax+" "+xmax);
                    // Draw the bounding box on the canvas
                    canvas.drawRect(xmin, ymin, xmax, ymax, paint);
                    String label = classesLabel[Math.round(classes[j])] + " " + confidences[j];
                    float textHeight = -paint.getFontMetrics().ascent;
                    canvas.drawText(label, xmin, ymin - textHeight, paint);
                }
                Bitmap displayImage = Bitmap.createScaledBitmap(blankImage, imageViewWidth, imageViewHeight, false);
                return displayImage;
            }catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(Bitmap displayImage) {
            super.onPostExecute(displayImage);
            imageView.setImageBitmap(displayImage);
            imageResponse.processFinished();
        }
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            myCameraDevice = cameraDevice;
            //First Initialize for camera
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    buttonStartCamera();
                }
            }, 500);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            myCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            myCameraDevice.close();
            myCameraDevice = null;
        }
    };

    private void startCamera() {
        try {
            //Webcam OR Phone
            if(cameraManager.getCameraIdList().length > 0){
                stringCameraID = cameraManager.getCameraIdList()[0];
            }
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(stringCameraID, stateCallback, null);
        } catch (CameraAccessException e){
            throw new RuntimeException(e);
        }


    }
    public void buttonStartCamera(){
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        Surface surface = new Surface(surfaceTexture);

        try {
            captureRequestBuilder = myCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            int textureViewWidth = textureView.getWidth();
            imageViewWidth = textureViewWidth;
            int textureViewHeight = textureView.getHeight();
            imageViewHeight = textureViewHeight;

            captureRequestBuilder.addTarget(surface);
            OutputConfiguration outputConfiguration = new OutputConfiguration(surface);
            SessionConfiguration sessionConfiguration = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    Collections.singletonList(outputConfiguration),
                    getMainExecutor(),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            myCameraCaptureSession = cameraCaptureSession;
                            captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(0, 0, textureViewWidth, textureViewHeight));
                            /*
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CameraMetadata.CONTROL_MODE_AUTO);

                             */
                            try {
                                myCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            cameraCaptureSession = null;
                        }
                    });

            myCameraDevice.createCaptureSession(sessionConfiguration);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(LiveCameraActivity.this, "Error accessing the camera", Toast.LENGTH_SHORT).show());
            Intent intent = new Intent(this, MainActivity.class);
            this.startActivity(intent);
        }
    }
    public void buttonStopCamera(View view){
        try {
            myCameraCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }



}
