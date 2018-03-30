package cz.mendelu.pef.xhasprov.watsonrecognition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ibm.watson.developer_cloud.http.ServiceCallback;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private Button btnSelect;
    //private Button btnUpload;
    private ImageView ivBiowaste;
    private ImageView ivPaper;
    private ImageView ivGeneral;
    private ImageView ivPMD;
    private ImageView ivImage;
    private String userChoosenTask;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSelect = findViewById(R.id.btnSelectPhoto);
        btnSelect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
        ivImage = findViewById(R.id.ivImage);
        txtResult = findViewById(R.id.txtResult);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=Utility.checkPermission(MainActivity.this);
                if (items[item].equals("Take Photo")) {
                    userChoosenTask="Take Photo";
                    if(result)
                        cameraIntent();
                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask="Choose from Library";
                    if(result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
        uploadImage();
        Log.i("ActivityResult: ", "uploading image..");
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ivImage.setImageBitmap(thumbnail);
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ivImage.setImageBitmap(bm);
    }

    public void uploadImage() {

        Bitmap image=((BitmapDrawable)ivImage.getDrawable()).getBitmap();

        VisualRecognition service = new VisualRecognition("2016-05-20");
        service.setApiKey("0502eb7433ec16832c7b914a8e0c160732137027");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] bitmapdata = outputStream.toByteArray();
        InputStream imagesStream = new ByteArrayInputStream(bitmapdata);

        ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                .imagesFile(imagesStream)
                .imagesFilename(getImageName())
                .addClassifierId("DefaultCustomModel_1490079410")
                .build();

        service.classify(classifyOptions).enqueue(new ServiceCallback<ClassifiedImages>() {
            @Override
            public void onResponse(ClassifiedImages response) {

                ArrayList<TrashClass> trashClasses = parseJSON(response.toString());
                if (trashClasses != null){
                    getTrashBin(trashClasses);
                }
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void getTrashBin(ArrayList<TrashClass> trashClasses) {

        //get the item with highest probability
        TrashClass item = new TrashClass("", 0.0);
        for (int i = 0; i < trashClasses.size(); i++) {
            if (trashClasses.get(i).getScore()>item.getScore()){
                item = trashClasses.get(i);
            }
            Log.i("getTrashBin: ", "highest score found");
        }
        //determine the trash bin
            switch(item.getClassName()){
                case "PMC":
                    Log.i("getTrashBin: ", "image set");
                    txtResult.setText("You should throw this into PMC trash bin.");
                    setContentView(R.layout.activity_main);
                    Log.i("getTrashBin: ", "text set");
                    ivImage.setImageResource(R.drawable.bin_pmd);
                    setContentView(R.layout.activity_main);
                    break;

                case "Paper":
                    ivImage.setImageResource(R.drawable.bin_paper);
                    txtResult.setText("You should throw this into Paper trash bin.");
                    break;

                case "Food":
                    ivImage.setImageResource(R.drawable.bin_biowaste);
                    txtResult.setText("You should throw this into the Biological waste trash bin.");
                    break;

                case "Unrecognized as trash":
                    txtResult.setText("You should not throw this into any trash bin.");
                    break;

                default:
                    ivImage.setImageResource(R.drawable.bin_general_waste);
                    txtResult.setText("You should throw this into general trash bin.");
                    break;
        }
    }

    private String getImageName() {
        SimpleDateFormat s = new SimpleDateFormat("dd-MM-yyyy_hh:mm");
        String format = s.format(new Date());
        String fileName =  format + ".jpg";
        return fileName;
    }

    public ArrayList<TrashClass> parseJSON(String result) {
        ArrayList<TrashClass> trashClasses = new ArrayList<>();

        if (result != null) {
            try {
                JSONObject jObject = new JSONObject(result);
                JSONArray images = jObject.getJSONArray("images");

                for (int i = 0; i < images.length(); i++) {
                    JSONObject imagesObj = images.getJSONObject(i);
                    JSONArray classifiers = imagesObj.getJSONArray("classifiers");

                    for (int j = 0; j < classifiers.length(); j++) {
                        JSONObject classifierObj = classifiers.getJSONObject(j);
                        JSONArray classes = classifierObj.getJSONArray("classes");

                        for (int k = 0; k < classes.length(); k++) {
                            JSONObject classObj = classes.getJSONObject(k);
                            trashClasses.add(new TrashClass(
                                    classObj.getString("class"),
                                    classObj.getDouble("score")
                            ));
                            Log.i("TRASH CLASS: ", trashClasses.get(k).toString());
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return trashClasses;
    }
}


