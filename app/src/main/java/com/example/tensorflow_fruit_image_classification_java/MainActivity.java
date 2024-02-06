package com.example.tensorflow_fruit_image_classification_java;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.example.tensorflow_fruit_image_classification_java.ml.Detect;
import com.example.tensorflow_fruit_image_classification_java.ml.MobilenetClassification;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Button camera, gallery, help;
    ImageView imageView;

    TextView result;
    //Compulsory 224x224 pixel for tensor input
    Bitmap bitmap2, bitmap;

    int imageSize = 224;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                }else{
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(cameraIntent, 1);
            }
        });
        setSupportActionBar(findViewById(R.id.toolbar));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.guideline){
            Toast.makeText(this, "Item 1 selected", Toast.LENGTH_SHORT).show();
            Intent guidelineIntent = new Intent(this, GuidelineActivity.class);
            startActivity(guidelineIntent);
            return true;
        }
        else if(item.getItemId() == R.id.setting){
            Toast.makeText(this, "Item 2 selected", Toast.LENGTH_SHORT).show();
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }

    }
    public void classifyImage(Bitmap image){
        try {
            MobilenetClassification model = MobilenetClassification.newInstance(getApplicationContext());
            // Creates inputs for reference.
            int imageSize = 224;
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++];
                    // extract R,G,B by bitwise
                    // /1 because preprocessing scaled from 0 (fresh) to 1 (spoiled)
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f/ 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f/ 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f/ 1));
                }
            }
            inputFeature0.loadBuffer(byteBuffer);
            // Runs model inference and gets result.
            MobilenetClassification.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            float[] confidences = outputFeature0.getFloatArray();

            int maxPos = 0;
            float maxConfidence = 0;
            for(int i = 0; i < confidences.length; i++){
                System.out.println(confidences[i] + " ");
                Log.d("tagString", (confidences[i] + " "));
                if(confidences[i] > maxConfidence){
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            String[] classes = {"fresh", "spoiled"};
            Log.d("confidences", (confidences[0] + " " + Math.round(confidences[0]) + " " + classes[Math.round(confidences[0])]));

            float confidence_per = confidences[0] * 100;
            if(classes[Math.round(confidences[0])].equals("fresh")){
                confidence_per = (1-confidences[0]) * 100;
            }
            result.setText(classes[Math.round(confidences[0])]
                    + "\nConfidence:" + (confidence_per+ "%"));


            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }
    public void detectImage(Bitmap image){
        try {
            Detect model = Detect.newInstance(getApplicationContext());
            int imageSize = 320;
            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 320, 320, 3}, DataType.INT8);
            TensorImage tensorImage = TensorImage.fromBitmap(image);
            //ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            /*
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    // extract R,G,B by bitwise
                    // /1 because preprocessing scaled from 0 (fresh) to 1 (spoiled)
                    int val = intValues[pixel++];
                    byteBuffer.putInt(val);
                    //byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f/ 1));
                    //byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f/ 1));
                    //byteBuffer.putFloat((val & 0xFF) * (1.f/ 1));
                }
            }
            */

            inputFeature0.loadBuffer(byteBuffer);
            // Runs model inference and gets result.
            /*
            Detect.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            TensorBuffer outputFeature1 = outputs.getOutputFeature1AsTensorBuffer();
            TensorBuffer outputFeature2 = outputs.getOutputFeature2AsTensorBuffer();
            TensorBuffer outputFeature3 = outputs.getOutputFeature3AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            float[] output1 = outputFeature1.getFloatArray();
            float[] output2 = outputFeature2.getFloatArray();

            float[] output3 = outputFeature3.getFloatArray();

            Log.d("confidences ", Arrays.toString(confidences) + "");
            Log.d("boxes ", Arrays.toString(output1) + "");
            Log.d("len ", Arrays.toString(output2) + "");
            Log.d("classes ", Arrays.toString(output3) + "");
            Bitmap bitmapWithBoundingBoxes = image.copy(Bitmap.Config.ARGB_8888, true);

            // Create a canvas from the new bitmap
            Canvas canvas = new Canvas(bitmapWithBoundingBoxes);

            // Get the dimensions of the original image
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            // Get the dimensions of the model's input size
            int modelInputSize = 320; // Assuming the model input size is 320x320

            // Calculate the scaling factors to map the model's coordinates to the image's coordinates
            float scaleX = (float) imageWidth / modelInputSize;
            float scaleY = (float) imageHeight / modelInputSize;
            // Iterate through each bounding box
            for(int i = 0,j = 0; j < confidences.length; j++,i+=4) {
                if(confidences[j] <= 0.95) continue;
                Log.d("curcon", confidences[j]+" ");

                float ymin = output1[i] * modelInputSize * scaleY;
                float xmin = output1[i + 1] * modelInputSize * scaleX;
                float ymax = output1[i + 2] * modelInputSize * scaleY;
                float xmax = output1[i + 3] * modelInputSize * scaleX;

                ymin = Math.max(1f,ymin);
                xmin = Math.max(1f,xmin);
                ymax = Math.max(1f,ymax);
                xmax = Math.max(1f,xmax);
                // Create a Paint object for drawing bounding boxes
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);
                Log.d("draw", ymin+" "+xmin+" "+ymax+" "+xmax);
                // Draw the bounding box on the canvas
                canvas.drawRect(xmin, ymin, xmax, ymax, paint);

            }

            imageView.setImageBitmap(bitmapWithBoundingBoxes);
//            /*
//            int maxPos = 0;
//            float maxConfidence = 0;
//            for(int i = 0; i < confidences.length; i++){
//                System.out.println(confidences[i] + " ");
//                Log.d("tagString", (confidences[i] + " "));
//                if(confidences[i] > maxConfidence){
//                    maxConfidence = confidences[i];
//                    maxPos = i;
//                }
//            }
//            */
//            String[] classes = {"fresh", "spoiled"};
//            Log.d("confidences", (confidences[0] + " "));
//            if (classes[Math.round(confidences[0])].equals("fresh")) {
//                confidences[0] = 1 - confidences[0];
//            }
//            result.setText(classes[Math.round(confidences[0])] + "\nconfidences:" + (confidences[0] * 100 + ""));


            // Releases model resources if no longer used.

            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(resultCode == 3){
                // Permission of Camera result code received
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);

                Bitmap displayImage = Bitmap.createScaledBitmap(image, 200, 200, true);
                imageView.setImageBitmap(displayImage);
                //image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }else{
                //Attempt to solve the bug

                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                }catch (IOException e) {
                    e.printStackTrace();
                }
                Bitmap displayImage = Bitmap.createScaledBitmap(image, 1000, 1000, false);
                imageView.setImageBitmap(displayImage);
                Log.d("Image Info", "Width: " + image.getWidth() + ", Height: " + image.getHeight());
                Log.d("Image Info", "Width: " + displayImage.getWidth() + ", Height: " + displayImage.getHeight());
                imageSize = 320;
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}