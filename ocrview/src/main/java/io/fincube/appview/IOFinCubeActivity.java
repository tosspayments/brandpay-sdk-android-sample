package io.fincube.appview;
/**
 * IOFinCubeActivity.java
 **/

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static android.text.Html.fromHtml;

import io.fincube.creditcard.DevConfig;


import io.fincube.creditcard.CameraUnavailableException;
import io.fincube.creditcard.DetectionInfo;

// import for using OcrScanner
import io.fincube.ocr.OcrScanner;
import io.fincube.ocr.OcrConfig;
import io.fincube.ocr.OverlayView;
import io.fincube.ocr.listener.OcrScannerListener;
import io.fincube.ocrsdk.OcrConfigSDK;

public final class IOFinCubeActivity extends AppCompatActivity {

    public static final int SCANNER_TYPE_CREDIT_CARD    = 0;
    public static final int SCANNER_TYPE_ID_CARD        = 1;
    public final static int SCANNER_TYPE_ID_ALIEN       = 4;
    public final static int SCANNER_TYPE_ID_PASSPORT    = 5;
    public final static int SCANNER_TYPE_ID_ALIEN_BACK  = 11;
    
    public static final String SCANCONFIG_SCANNER_TYPE          = "fincube.appview.scannertype";
    public static final String SCANCONFIG_SCAN_EXPIRY           = "fincube.appview.scan_expiry";
    public static final String SCANCONFIG_VALIDATE_NUMBER       = "fincube.appview.validate_number";
    public static final String SCANCONFIG_VALIDATE_EXPIRY       = "fincube.appview.validate_expiry";


    private static int lastResult = 0xca8d10; // arbitrary. chosen to be well above

    private static final String TAG = "IOFinCubeActivityLOG";

    private static final int TOAST_OFFSET_Y = -75;
    private static final int PERMISSION_REQUEST_ID = 11;

    private boolean requestPermissionGranted = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    static private int numActivityAllocations;

    private OcrScanner mOCRScanner;
    private OcrConfig ocrConfig = new OcrConfig();;
    Button negativeDialogButton;
    Button positiveDialogButton;

    OverlayView overlayView = null;

    // ------------------------------------------------------------------------
    // ACTIVITY LIFECYCLE
    // ------------------------------------------------------------------------

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.xxlayout);
        numActivityAllocations++;
        // NOTE: java native asserts are disabled by default on Android.
        if (numActivityAllocations != 1) {
            Log.i(TAG, String.format(
                    "INTERNAL WARNING: There are %d (not 1) IOFinCubeActivity allocations!",
                    numActivityAllocations));
        }

        final Intent clientData = this.getIntent();

        // Validate app's manifest is correct.
        ResolveInfo resolveInfo;
        String errorMsg;

        //ocrConfig.licenseKeyFile    = "fincubelicense_AOS.flk";

        ocrConfig.scannerType       = clientData.getIntExtra(SCANCONFIG_SCANNER_TYPE, OcrConfig.ScannerType.CREDITCARD.getValue());
        ocrConfig.scanExpiry        = clientData.getBooleanExtra(SCANCONFIG_SCAN_EXPIRY, false);
        ocrConfig.validateNumber    = clientData.getBooleanExtra(SCANCONFIG_VALIDATE_NUMBER, false);
        ocrConfig.validateExpiry    = clientData.getBooleanExtra(SCANCONFIG_VALIDATE_EXPIRY, false);
        ocrConfig.cameraIdx = OcrConfig.USE_BACK_CAMERA;
        ocrConfig.changeOverlayColor = true;
        ocrConfig.context = this.getApplicationContext();
        ocrConfig.autoReleaseCamera = false;

        // check hardwareInfo
        ocrConfig.useMultiThread = checkHardwareInfo();


        getDisplayInfo( ocrConfig );

        try {
            checkPermission();
        } catch (Exception e) {
            handleGeneralExceptionError(e);
        }
    }

    private boolean checkHardwareInfo() {
        return is64Bit();
    }

    public static boolean is64Bit() {
        String arch = System.getProperty("os.arch");
        return arch.contains("64");
    }

    private void getDisplayInfo(OcrConfig config)
    {
        if( config == null )
        {
            Log.e(TAG, "OcrConfig is null");
            return;
        }

        Display display = getWindowManager().getDefaultDisplay();

        int pixels = 0;

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ) {
            Point size = new Point();
            display.getSize(size);
            pixels = Math.min(size.x, size.y);
        }
        else
        {
            pixels = Math.min(display.getWidth(), display.getHeight());
        }

        if( pixels <= 720 ) {
            ocrConfig.cameraPreviewWidth = 1280;
            ocrConfig.cameraPreviewHeight = 720;
        }
        else
        {
            ocrConfig.cameraPreviewWidth = 1920;
            ocrConfig.cameraPreviewHeight = 1080;
        }
        return;
    }


    private void checkPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCamera = checkSelfPermission(Manifest.permission.CAMERA);

            if( permissionCamera == PackageManager.PERMISSION_DENIED /*|| permissionWriteExternalStorage == PackageManager.PERMISSION_DENIED*/)
            {
                String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, PERMISSION_REQUEST_ID);
            }
            else
            {
                requestPermissionGranted = true;
                checkCamera();;
                showCameraScannerOverlay();
            }
        } else {
            Log.e(TAG, "checkPermission not going this way");
            requestPermissionGranted = true;
            checkCamera();
            showCameraScannerOverlay();
        }
    }

    private void checkCamera() {
        try {
            if (!OcrScanner.canReadCardWithCamera()) {
                Log.e(TAG, "error : Camera open failed" );
            }
        } catch (CameraUnavailableException e) {
            Log.e(TAG, "error : Camera open failed");
            Toast toast = Toast.makeText(this, "Camera open failed", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, TOAST_OFFSET_Y);
            toast.show();
        }
    }

    private void showCameraScannerOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getSupportActionBar();

            if (null != actionBar) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(fromHtml("<font color=\"#10C0FF\">" + getResources().getString(R.string.str_demo_app_name) + "</font>"));
            }
        }

        try {
            setPreviewLayout();
        } catch (Exception e) {
            handleGeneralExceptionError(e);
        }
    }

    private void handleGeneralExceptionError(Exception e) {
        Log.e(TAG,"Unknown exception - please send the stack trace", e);
        Toast toast = Toast.makeText(this, "Camera unexcpeted fail", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, TOAST_OFFSET_Y);
        toast.show();
    }

    /**
     * Suspend/resume camera preview as part of the {@link android.app.Activity} life cycle (side note: we reuse the
     * same buffer for preview callbacks to greatly reduce the amount of required GC).
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(requestPermissionGranted) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            if( ocrConfig != null )
            {
                int systemOrientation = getWindowManager().getDefaultDisplay().getRotation();

                switch(systemOrientation)
                {
                    case 0 :
                        ocrConfig.orientation = 1;
                        break;
                    case 1 :
                        ocrConfig.orientation = 2;
                        break;
                    case 3 :
                        ocrConfig.orientation = 0;
                        break;
                }
            }

            if (!restartPreview()) {
                Log.e(TAG, "Could not connect to camera.");
            }
        }
    }



    @Override
    protected void onPause() {
        Log.d(TAG, "[FinCube] onPause");
        super.onPause();

        if( mOCRScanner != null )
            mOCRScanner.pauseScan();
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "[FinCube] onDestroy");
        numActivityAllocations--;
        if( mOCRScanner != null )
            mOCRScanner.endScan();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        Log.e(TAG, "requestCode = " + requestCode);
        switch (requestCode) {
            case PERMISSION_REQUEST_ID:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    requestPermissionGranted = true;
                }
                break;
        }

        if( requestPermissionGranted )
        {
            checkCamera();
            showCameraScannerOverlay();
            onResume();
        }
        else
        {
            checkPermission();
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }



    private void showCreditCardResult(DetectionInfo info)
    {
        Log.d(TAG, "[FinCube] showCreditCardResult");
        AlertDialog.Builder builder = new AlertDialog.Builder(IOFinCubeActivity.this);
        builder.setTitle("Card Scan Result");

        View dialogView = this.getLayoutInflater().inflate(R.layout.creditcard_result_dialog, null);
        builder.setView(dialogView);

        final EditText cardnum = (EditText)dialogView.findViewById(R.id.scanResultCardNumberTextEditor);
        final EditText cardexpiry = (EditText)dialogView.findViewById(R.id.scanResultCardExpiryTextEditor);
 
        final ImageView cardView = (ImageView)dialogView.findViewById(R.id.scanResultImage);
       

        if( info != null )
        {
            Log.d(TAG, "[FinCube] mDetectedCard is OK");
            cardnum.setText("" + info.getCardNumber());
            cardexpiry.setGravity(Gravity.RIGHT);
            if (info.expiry_month < 0 || info.expiry_year < 0)
                cardexpiry.setText("No expiry");
            else
                cardexpiry.setText("" + info.expiry_month + "/" + info.expiry_year);

            if( info.cardImage != null )
            {
                if( ocrConfig.changeGuideRectOrientation == 1 ) {
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(90);
                    info.cardImage = Bitmap.createBitmap(info.cardImage, 0, 0, info.cardImage.getWidth(), info.cardImage.getHeight(), rotateMatrix, false);
                }
                cardView.setImageBitmap(info.cardImage);
            }
           
        }
        else
        {
            Log.i(TAG, "[FinCube] mDetectedCard is null");
        }

        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        builder.setCancelable(false);

        Log.d(TAG, "[FinCube] builder.show");
        builder.create().show();
        if (DevConfig.USE_AUTO_RUNNING_TEST) {
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    finish();
                }
            }, DevConfig.AUTO_RUNNING_DELAY);
        } else {

        }
        info.clearAllPrivacyData();
    }


    void onCardDetectedResult(final DetectionInfo dInfo) {
        Log.d(TAG, "onCardDetected()");


        if( dInfo.cardScannerType == OcrConfig.ScannerType.CREDITCARD.getValue() ) {
            showCreditCardResult(dInfo);
        }
        else {
            Log.d(TAG, "showResult Failed");
        }
    }

    private boolean restartPreview() {
        if( mOCRScanner != null ) {
            try {
                return mOCRScanner.startScan();
            }
            catch( RuntimeException e )
            {
                // TODO open camera failed
            }
        }
        return false;
    }

    void onFailure(int error_code) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( this);
        alertDialogBuilder.setTitle("ERROR");

        switch (error_code) {
            case OcrConfigSDK.ERR_CODE_EXPIRED:
                alertDialogBuilder.setMessage("EXPIRED");
                break;
            case OcrConfigSDK.ERR_CODE_INVALID_PACKAGE:
                alertDialogBuilder.setMessage("INVALID PACKAGE");
                break;
            case OcrConfigSDK.ERR_CODE_ALLOC_FAILED:
                alertDialogBuilder.setMessage("ALLOC FAILED");
                break;
            case OcrConfigSDK.ERR_CODE_FAILED_TO_LOAD_DATA:
                alertDialogBuilder.setMessage("FAILED TO LOAD DATA");
                break;
            default:
                alertDialogBuilder.setMessage("UNKNOWN");
                break;
        }


        alertDialogBuilder.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int id) {
                        IOFinCubeActivity.this.finish();
                    }
                });
        alertDialogBuilder.show();
    }

    /**
     * Manually set up the layout for this {@link android.app.Activity}. It may be possible to use the standard xml
     * layout mechanism instead, but to know for sure would require more work
     */
    private void setPreviewLayout() {
        this.setContentView(R.layout.xxlayout);

        FrameLayout mMainLayout = (FrameLayout)findViewById(R.id.iofincubemainlayout);

        ocrConfig.guideColor = 0xFF000000;

        mOCRScanner = new OcrScanner(this);
        mOCRScanner.setOcrScannerListener(new OcrScannerListener() {
            @Override
            public void onFailure(final int error_code) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        IOFinCubeActivity.this.onFailure(error_code);
                    }
                });
            }

            @Override
            public void onCardDetected(final DetectionInfo dinfo) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (ocrConfig.autoReleaseCamera) {
                            onCardDetectedResult(dinfo);
                        } else {
                            mOCRScanner.pauseScan();
                            onCardDetectedResult(dinfo);

                        }
                    }
                });
            }

            @Override
            public void onCardScannerInit() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        overlayView.onCardScannerInit();
                    }
                });
            }

            @Override
            public void onCardScannerReady() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        overlayView.onCardScannerReady();
                    }
                });
            }
        });

        overlayView = new CustomOverlayView(this, null, ocrConfig);

        mOCRScanner.initView(this, ocrConfig, overlayView);
        mOCRScanner.changeGuideRect(0.5f, 0.5f, 1.0f, 0);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mOCRScanner.setLayoutParams(layoutParams);

        mMainLayout.addView(mOCRScanner);

        Button btn = (Button)findViewById(R.id.Key);
        btn.setText(getResources().getString(R.string.str_btn_stoptscan));
        btn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !ocrConfig.autoReleaseCamera ) {
                    mOCRScanner.pauseScan();
                }
                onBackPressed();
            }
        });

        final ToggleButton btnRotate = (ToggleButton) findViewById(R.id.rotateToggleButton);
        if( ocrConfig.scannerType == OcrConfig.ScannerType.CREDITCARD.getValue() ) {
            btnRotate.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (btnRotate.isChecked()) {
                        mOCRScanner.changeGuideRect(0.5f, 0.5f, 1.0f, 1);
                    } else {
                        mOCRScanner.changeGuideRect(0.5f, 0.5f, 1.0f, 0);
                    }
                }
            });
            btnRotate.setVisibility(View.VISIBLE);
        }
        else {
            btnRotate.setVisibility(View.GONE);
        }
    }

 


}