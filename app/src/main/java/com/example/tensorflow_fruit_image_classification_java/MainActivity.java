package com.example.tensorflow_fruit_image_classification_java;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.tensorflow_fruit_image_classification_java.ml.Detect;
import com.example.tensorflow_fruit_image_classification_java.ml.MobilenetClassification;
import com.example.tensorflow_fruit_image_classification_java.ml.Mobilenetv3largeUnet;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String MODE_1 = "Classification";
    public static final String MODE_2 = "Image Segmentation";
    public static final String MODE_3 = "Detection";

    Button camera, gallery, liveCamera;
    ImageView imageView, imageViewCover;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    String selected_mode;
    TextView result, descriptionText;
    //Compulsory 224x224 pixel for tensor input
    Bitmap controlResultBitMap;
    int imageSize = 320;
    private SeekBar seekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if(selected_mode == null){
            preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
            selected_mode = preferences.getString("user", MODE_1);
        }
        //grab main content
        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);
        liveCamera = findViewById(R.id.button4);
        result = findViewById(R.id.result);
        descriptionText = findViewById(R.id.descriptionText);
        imageView = findViewById(R.id.imageView);
        imageViewCover = findViewById(R.id.imageViewCover);
        PackageManager packageManager = getPackageManager();
        boolean hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CAMERA
            },100);
        }

        liveCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasCamera) {
                    Toast.makeText(MainActivity.this, "No camera detected", Toast.LENGTH_SHORT).show();
                    return;
                }else{
                    goToLiveCameraActivity(view);
                }
            }
        });
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    if (!hasCamera) {
                        Toast.makeText(MainActivity.this, "No camera detected", Toast.LENGTH_SHORT).show();

                    }else{
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(intent);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error accessing the camera", Toast.LENGTH_SHORT).show());

                }

            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(galleryIntent);

            }
        });

        //SeekBar
        seekBar = findViewById(R.id.seekBar);
        seekBar.setVisibility(View.INVISIBLE);
        //imageViewCover.setAlpha(0f);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float opacity = (float) progress / 100;
                // Set the opacity on the ImageView
                imageViewCover.setAlpha(opacity);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Action and Setting grabbing
        setSupportActionBar(findViewById(R.id.toolbar));




    }
    private void checkModeFollowUp(){
        switch(selected_mode){
            case MODE_1:{
                descriptionText.setText("Classified as");
                result.setVisibility(View.VISIBLE);
                break;
            }
            case MODE_2:{
                descriptionText.setText("Opacity (0-100)");
                seekBar.setVisibility(View.VISIBLE);
                seekBar.setProgress(40);
                imageViewCover.setAlpha(0.4f);

                break;

            }
            case MODE_3:{
                descriptionText.setText("Detected fruits");
                result.setVisibility(View.VISIBLE);
                break;
            }
            default:break;
        }
        descriptionText.setVisibility(View.VISIBLE);
    }

    private void disabler(){
        View[] list = new View[]{seekBar,result};
        for(View i : list){
            i.setVisibility(View.INVISIBLE);
        }
        imageViewCover.setAlpha(0f);
        imageViewCover.setImageDrawable(null);
    }
    private void createPopUpSettingWindow() {
        Log.d("MainActivity", selected_mode);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popUpViews = inflater.inflate(R.layout.settingpopup, null);
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final List<String> option = Arrays.asList(MODE_1,MODE_2, MODE_3);
        final Spinner spinner = popUpViews.findViewById(R.id.mode_spinner);
        PopupWindow popupWindow = new PopupWindow(popUpViews,width,height,focusable);
        imageView.post(new Runnable(){
            @Override
            public void run(){
                popupWindow.showAtLocation(imageView, Gravity.TOP,0,200);
            }
        });

        //Set state for setting popup
        TextView closeBtn, saveBtn;
        closeBtn = popUpViews.findViewById(R.id.reset_setting);
        saveBtn = popUpViews.findViewById(R.id.save_setting);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
                editor=preferences.edit();
                Spinner mySpinner = (Spinner) popUpViews.findViewById(R.id.mode_spinner);
                String text = mySpinner.getSelectedItem().toString();
                editor.putString("user", text);
                editor.commit();
                selected_mode = preferences.getString("user", "Classification");

                popupWindow.dismiss();
            }
        });
        //Touch close
        popUpViews.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });

        String AndroidVersion = android.os.Build.VERSION.RELEASE;
        TextView apiVersion = popUpViews.findViewById(R.id.get_api);
        apiVersion.setText(AndroidVersion);
        TextView requiredVersion = popUpViews.findViewById(R.id.get_apk);
        int androidVersion = BuildConfig.AndroidVersion;
        requiredVersion.setText(androidVersion+"");
        //Set Spinner
        ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), R.layout.mode_selected_item_layout, option);
        adapter.setDropDownViewResource(R.layout.mode_selected_dropdown_layout);
        spinner.setAdapter(adapter);

        spinner.setSelection(adapter.getPosition(selected_mode));



    }

    private void goToLiveCameraActivity(View view){
        Intent intent = new Intent(this, LiveCameraActivity.class);
        view.getContext().startActivity(intent);
    }
    public static Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float scaleX = newWidth / (float) bitmap.getWidth();
        float scaleY = newHeight / (float) bitmap.getHeight();
        float pivotX = 0;
        float pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    private ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == RESULT_OK){
                        // Permission of Camera result code received
                        disabler();
                        Intent data = result.getData();
                        Bitmap image = (Bitmap) data.getExtras().get("data");
                        int dimension = Math.min(image.getWidth(), image.getHeight());
                        image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                        Bitmap originalMap = scaleBitmap(image, 1000, 1000);
                        imageView.setImageBitmap(originalMap);
                        switch(selected_mode){
                            case MODE_1:{
                                imageSize = 224;
                                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                                classifyImage(image);
                                break;
                            }
                            case MODE_2:{
                                imageSize = 224;
                                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                                segmentImage(image);
                                break;

                            }
                            case MODE_3:{
                                imageSize = 320;
                                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                                detectImage(image);
                                break;
                            }
                            default:break;
                        }
                        checkModeFollowUp();
                    }
                }
            });

    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == RESULT_OK){
                        //Attempt to solve the bug
                        disabler();
                        Intent data = result.getData();
                        Uri dat = data.getData(); //get intent data
                        Bitmap image = null;
                        if(dat != null){
                            try {
                                image = MediaStore.Images.Media.getBitmap(getContentResolver(), dat);
                                Bitmap originalMap = scaleBitmap(image, 1000, 1000);
                                imageView.setImageBitmap(originalMap);
                                switch(selected_mode){
                                    case MODE_1:{
                                        imageSize = 224;
                                        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                                        classifyImage(image);
                                        break;
                                    }
                                    case MODE_2:{
                                        imageSize = 224;
                                        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                                        segmentImage(image);
                                        break;

                                    }
                                    case MODE_3:{
                                        imageSize = 320;
                                        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                                        detectImage(image);
                                        break;
                                    }
                                    default:break;
                                }
                                checkModeFollowUp();
                            }catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            });


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {

        }
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
            Toast.makeText(this, "Back to Guideline", Toast.LENGTH_SHORT).show();
            Intent guidelineIntent = new Intent(this, GuidelineActivity.class);
            startActivity(guidelineIntent);
            return true;
        }
        else if(item.getItemId() == R.id.setting){
            Toast.makeText(this, "Open Setting", Toast.LENGTH_SHORT).show();
            createPopUpSettingWindow();
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }

    }
    public void classifyImage(Bitmap image){
        Log.d("MainActivity", "Execute classifyImage");
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
        }
    }

    public void segmentImage(Bitmap image){
        Log.d("MainActivity", "Execute segmentImage");
        try {
            Mobilenetv3largeUnet model = Mobilenetv3largeUnet.newInstance(getApplicationContext());
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
            Mobilenetv3largeUnet.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Log.d("MainActivity: outputFeature0",outputFeature0.getBuffer()+"");
            int colorObject1 = Color.WHITE;
            int colorObject2 = Color.BLACK;
            Bitmap resultBitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
            float[] data=outputFeature0.getFloatArray();
            Log.d("MainActivity: outputFeature0 data",data+"");
            int index = 0;
            for (int y = 0; y < 224; y++) {
                for (int x = 0; x < 224; x++) {
                    // Get the segmentation class (0 or 1)
                    float class0Score = data[index++];
                    float class1Score = data[index++];
                    int classIndex = class0Score > class1Score ? 0 : 1;

                    // Set pixel color based on class
                    resultBitmap.setPixel(x, y, classIndex == 0 ? colorObject1 : colorObject2);
                }
            }
            controlResultBitMap = Bitmap.createScaledBitmap(resultBitmap, 1000, 1000, true);
            imageViewCover.setImageBitmap(controlResultBitMap);
            seekBar.setVisibility(View.VISIBLE);
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }
    public void detectImage(Bitmap image){
        Log.d("MainActivity", "Execute detectImage");
        try {
            Detect model = Detect.newInstance(getApplicationContext());
            int imageSize = 320;
            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 320, 320, 3}, DataType.FLOAT32);
            TensorImage tensorImage = TensorImage.fromBitmap(image);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);


            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    // extract R,G,B by bitwise
                    // /1 because preprocessing scaled from 0 (fresh) to 1 (spoiled)
                    int val = intValues[pixel++];
                    byteBuffer.putFloat( ((val >> 16) & 0xFF)/ 256.0f + (0.5f/256.0f));
                    byteBuffer.putFloat( ((val >> 8) & 0xFF) /256.0f  + (0.5f/256.0f));
                    byteBuffer.putFloat( (val & 0xFF) /256.0f  + (0.5f/256.0f));
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
            int freshCount = 0,rottenCount =0;
            // Iterate through each bounding box
            for(int i = 0,j = 0; j < confidences.length; j++,i+=4) {
                if(confidences[j] <= 0.5) continue;
                if(classes[j] == 1.0f){
                    rottenCount++;
                }else{
                    freshCount++;
                }
                Log.d("curcon", confidences[j]+" ");

                float ymin = boxes[i] * modelInputSize * scaleY;
                float xmin = boxes[i + 1] * modelInputSize * scaleX;
                float ymax = boxes[i + 2] * modelInputSize * scaleY;
                float xmax = boxes[i + 3] * modelInputSize * scaleX;

                ymin = Math.max(1f,ymin);
                xmin = Math.max(1f,xmin);
                ymax = Math.max(1f,ymax);
                xmax = Math.max(1f,xmax);
                // Create a Paint object for drawing bounding boxes
                Paint paint = new Paint();
                if(classes[j] == 1){
                    paint.setColor(Color.RED);
                }else{
                    paint.setColor(Color.GREEN);
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);
                Log.d("draw", ymin+" "+xmin+" "+ymax+" "+xmax);
                // Draw the bounding box on the canvas
                canvas.drawRect(xmin, ymin, xmax, ymax, paint);

            }
            imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmapWithBoundingBoxes, 1000, 1000, true));

            String text = "";
            if(rottenCount > 0 && freshCount > 0){
                text += String.format("fresh: %d%nrotten: %d",freshCount,rottenCount);
            }
            else if(rottenCount > 0){
                text += "rotten: " + rottenCount;
            }
            else if(freshCount > 0){
                text += "fresh: " + freshCount;
            }else{
                text += "No fruit find";
            }

            result.setText(text);

            // Releases model resources if no longer used.

            model.close();
        } catch (IOException e) {
        }
    }

}