package com.sketchboard.impex;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.UUID;

import android.provider.MediaStore;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //record screen pupose
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;

    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private ToggleButton mToggleButton;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSIONS = 10;
    private AlertDialogManager alertDialogManager;
    private ImageView recordScreen;
    private static boolean isRecordingStop = false;

    //painting purpose
    private DrawingView drawView;
    private ImageView currPaint;
    private float smallBrush, mediumBrush, largeBrush;
    private ImageView drawBtn, eraseBtn, newBtn, saveBtn, brushBtn, shapeBtn;

    //private LinearLayout linearLayoutColorChooser;
    PopupWindow popupwindow;
    SeekBar seekBar;
    SeekBar seekBarOpacity;
    TextView txtPixcelPercentage;
    TextView txtOpacityPix;
    boolean isSeekBarClicked = false;
    int progressBarPer = 9;
    int opacityProgress = 0;
    String selectedBrushSize = "";
    int lastPaintAlpha = 0;
    int selectedColor;

    //for zoom in out with touch event
    private float scale = 1f;
    private ScaleGestureDetector detector;
    ImageView imgBitMap;
    ImageView imageCommingSoon;

    private AdView adView;


    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//will hide the title
        getSupportActionBar().hide(); //hide the title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //show the activity in full screen
        setContentView(R.layout.activity_main);

        alertDialogManager = new AlertDialogManager(this);

        ///////////////////////////////// mob ads////////////////////

        MobileAds.initialize(MainActivity.this, "ca-app-pub-4664100260183976~4021641613");//ca-app-pub-3940256099942544~3347511713
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener(){
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.e(MainActivity.class.getName(),"onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.e(MainActivity.class.getName(),""+errorCode);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.e(MainActivity.class.getName(),"onAdOpened");
            }

            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
                Log.e(MainActivity.class.getName(),"onAdClicked");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.e(MainActivity.class.getName(),"onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
                Log.e(MainActivity.class.getName(),"onAdClosed");
            }
        });

        //painting variable initialization/////////////////////////////////////////////////////////////////////////////
        drawView = (DrawingView) findViewById(R.id.drawing);
        drawView.setBrushSize(mediumBrush);


        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);

        drawBtn = (ImageView) findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);

        eraseBtn = (ImageView) findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        newBtn = (ImageView) findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        saveBtn = (ImageView) findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        brushBtn = (ImageView) findViewById(R.id.brush_btn);
        brushBtn.setOnClickListener(this);

        shapeBtn = (ImageView) findViewById(R.id.shape_btn);
        shapeBtn.setOnClickListener(this);

        /*linearLayoutColorChooser = (LinearLayout)findViewById(R.id.linearLayoutColorChooser);
        linearLayoutColorChooser.setOnClickListener(this);*/

        //record screen initialization////////////////////////////////////////////////////////////////////////////////
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        mToggleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (PermissionChecker.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) + PermissionChecker
                        .checkSelfPermission(MainActivity.this,
                                Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale
                                    (MainActivity.this, Manifest.permission.RECORD_AUDIO)) {

                        mToggleButton.setChecked(false);
                         .Dialog("Permissions", "Give Storage And Recording Permission", "ok", "cancel", new AlertDialogManager.onSingleButtonClickListner() {
                            @Override
                            public void onPositiveClick() {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission
                                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                        REQUEST_PERMISSIONS);
                            }
                        }).show();

                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission
                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                REQUEST_PERMISSIONS);
                    }

                } else {
                    onToggleScreenShare(v);
                }*/
            }
        });

        //animation
        final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in

        recordScreen = (ImageView) findViewById(R.id.recordBtn);
        recordScreen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isRecordingStop == true) {
                    animation.cancel();
                    isRecordingStop = false;
                    onToggleScreenShare(isRecordingStop);
                } else {

                    recordScreen.startAnimation(animation);
                    isRecordingStop = true;

                    if (PermissionChecker.checkSelfPermission(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) + PermissionChecker
                            .checkSelfPermission(MainActivity.this,
                                    Manifest.permission.RECORD_AUDIO) + PermissionChecker
                            .checkSelfPermission(MainActivity.this,
                                    Manifest.permission.INTERNET) + PermissionChecker
                            .checkSelfPermission(MainActivity.this,
                                    Manifest.permission.ACCESS_NETWORK_STATE)
                            != PackageManager.PERMISSION_GRANTED) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale
                                (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                                ActivityCompat.shouldShowRequestPermissionRationale
                                        (MainActivity.this, Manifest.permission.RECORD_AUDIO) ||
                                ActivityCompat.shouldShowRequestPermissionRationale
                                        (MainActivity.this, Manifest.permission.INTERNET) ||
                                ActivityCompat.shouldShowRequestPermissionRationale
                                        (MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE)) {

                            mToggleButton.setChecked(false);
                            alertDialogManager.Dialog("Permissions", "Give Storage And Recording Permission", "ok", "cancel", new AlertDialogManager.onTwoButtonClickListner() {
                                @Override
                                public void onPositiveClick() {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission
                                                    .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE},
                                            REQUEST_PERMISSIONS);
                                }

                                @Override
                                public void onNegativeClick() {
                                    Toast.makeText(MainActivity.this, "You must be allow the all permissions", Toast.LENGTH_LONG).show();
                                }

                            }).show();

                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission
                                            .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                    REQUEST_PERMISSIONS);
                        }
                    } else {
                        onToggleScreenShare(isRecordingStop);
                    }
                }

            }
        });

        // permission
        if (PermissionChecker.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) + PermissionChecker
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) + PermissionChecker
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.INTERNET) + PermissionChecker
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.RECORD_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.INTERNET) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE)) {

                mToggleButton.setChecked(false);
                alertDialogManager.Dialog("Permissions", "Give Storage And Recording Permission", "ok", "cancel", new AlertDialogManager.onTwoButtonClickListner() {
                    @Override
                    public void onPositiveClick() {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission
                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE},
                                REQUEST_PERMISSIONS);

                    }

                    @Override
                    public void onNegativeClick() {
                        Toast.makeText(MainActivity.this, "You must be allow the all permissions", Toast.LENGTH_LONG).show();
                    }

                }).show();

            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                        REQUEST_PERMISSIONS);
            }
        } else {
           /* onToggleScreenShare(isRecordingStop);*/
        }


    }

    public void paintClicked(View view) {
        ViewParent id = view.getParent();


        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());
        /* lastPaintAlpha = drawView.getLastPaintAlpha();*/
        //use chosen color
        if (view != currPaint) {
            //update color
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            /* drawView.setPaintAlpha(lastPaintAlpha);*/
            selectedColor = getSelectedColor(imgView.getId());
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint = (ImageButton) view;
        }
    }

    public void brushPaintClicked(View view) {
        ViewParent id = view.getParent();


        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());
        lastPaintAlpha = drawView.getLastPaintAlpha();
        //use chosen color
        if (view != currPaint) {
            //update color
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            drawView.setPaintAlpha(lastPaintAlpha);
            selectedColor = getSelectedColor(imgView.getId());
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint = (ImageButton) view;
        }
    }

    private int getSelectedColor(int id) {

        int colorIndex;
        switch (id) {
            case R.id.color_brown:
                colorIndex = 0;
                break;

            case R.id.color_red:
                colorIndex = 1;
                break;

            case R.id.color_orange:
                colorIndex = 2;
                break;

            case R.id.color_yellow:
                colorIndex = 3;
                break;

            case R.id.color_green:
                colorIndex = 4;
                break;

            case R.id.color_magentaBlue:
                colorIndex = 5;
                break;

            default:
                colorIndex = 0;
                break;
        }
        return colorIndex;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.draw_btn) {
            //draw button clicked
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);

            popupwindow = popupDisplay(R.id.draw_btn);
            popupwindow.showAsDropDown(v, 130, -160);

        } else if (v.getId() == R.id.erase_btn) {
            //switch to erase - choose size

            popupwindow = popupDisplay(R.id.erase_btn);
            popupwindow.showAsDropDown(v, 130, -160);

        } else if (v.getId() == R.id.new_btn) {
            //new button
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
            newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    drawView.startNew();
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            newDialog.show();
        } else if (v.getId() == R.id.save_btn) {
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device Gallery?");
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //save drawing
                    drawView.setDrawingCacheEnabled(true);
                    String imgSaved = MediaStore.Images.Media.insertImage(
                            getContentResolver(), drawView.getDrawingCache(),
                            UUID.randomUUID().toString() + ".png", "drawing");
                    if (imgSaved != null) {
                        Toast savedToast = Toast.makeText(getApplicationContext(),
                                "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                        savedToast.show();
                    } else {
                        Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                        unsavedToast.show();
                    }
                    drawView.destroyDrawingCache();
                }
            });
            saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            saveDialog.show();
        } /*else if(v.getId() == R.id.linearLayoutColorChooser){

            popupwindow = popupDisplay(R.id.linearLayoutColorChooser);
            popupwindow.showAsDropDown(v,150, -250);

        }*/ else if (v.getId() == R.id.brush_btn) {

            popupwindow = popupDisplay(R.id.brush_btn);
            popupwindow.showAsDropDown(v, 150, -250);

            ///////////////////////// for zoom in out with touch event /////////////////////////////
            imgBitMap = new ImageView(getApplicationContext());
            imgBitMap.setImageBitmap(drawView.canvasBitmap);

            // detector = new ScaleGestureDetector(this,new ScaleListener());

        } else if (v.getId() == R.id.shape_btn) {

            popupwindow = popupDisplay(R.id.shape_btn);

            popupwindow.showAsDropDown(v, 500, -400);
        }

    }

    private PopupWindow popupDisplay(int id) {
        final PopupWindow popupWindow = new PopupWindow(MainActivity.this); // inflet your layout or diynamic add view

        View view = null;

        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        /*if(id == R.id.linearLayoutColorChooser)
        {
            view = inflater.inflate(R.layout.color_chooser, null);
            LinearLayout paintLayout = (LinearLayout)view.findViewById(R.id.paint_colors);

            currPaint = (ImageButton)paintLayout.getChildAt(0);
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            popupWindow.dismiss();
        } else */
        if (id == R.id.erase_btn) {

            view = inflater.inflate(R.layout.eraser_chooser, null);
            seekBar = (SeekBar) view.findViewById(R.id.seekbar);
            txtPixcelPercentage = (TextView) view.findViewById(R.id.txtPixcel);
            ImageView imgClearPage = (ImageView) view.findViewById(R.id.clearPage);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    drawView.setErase(true);
                    drawView.setBrushSize(progress);
                    txtPixcelPercentage.setText("" + progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            imgClearPage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.clearAnimation();
                }
            });

        } else if (id == R.id.draw_btn) {

            view = inflater.inflate(R.layout.brush_chooser, null);
            LinearLayout paintLayout = (LinearLayout) view.findViewById(R.id.pencil_paint_colors);
            LinearLayout brushSizeLayout = (LinearLayout) view.findViewById(R.id.linearLayoutBrushChooser);


            final TextView smallTxt = (TextView) view.findViewById(R.id.small_brush);
            final TextView mediumTxt = (TextView) view.findViewById(R.id.medium_brush);
            final TextView largeTxt = (TextView) view.findViewById(R.id.large_brush);

            if (selectedBrushSize.equals("small")) {
                smallTxt.setTextColor(Color.RED);
            } else if (selectedBrushSize.equals("medium")) {
                mediumTxt.setTextColor(Color.RED);
            } else if (selectedBrushSize.equals("large")) {
                largeTxt.setTextColor(Color.RED);
            }

            currPaint = (ImageButton) paintLayout.getChildAt(0);
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));


            smallTxt.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(smallBrush);
                    drawView.setLastBrushSize(smallBrush);
                    drawView.setErase(false);
                    smallTxt.setTextColor(Color.RED);
                    mediumTxt.setTextColor(Color.GRAY);
                    largeTxt.setTextColor(Color.GRAY);
                    selectedBrushSize = "small";
                    //popupWindow.dismiss();
                }
            });

            mediumTxt.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(mediumBrush);
                    drawView.setLastBrushSize(mediumBrush);
                    drawView.setErase(false);
                    smallTxt.setTextColor(Color.GRAY);
                    mediumTxt.setTextColor(Color.RED);
                    largeTxt.setTextColor(Color.GRAY);
                    selectedBrushSize = "medium";
                    // popupWindow.dismiss();
                }
            });

            largeTxt.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(largeBrush);
                    drawView.setLastBrushSize(largeBrush);
                    drawView.setErase(false);
                    smallTxt.setTextColor(Color.GRAY);
                    mediumTxt.setTextColor(Color.GRAY);
                    largeTxt.setTextColor(Color.RED);
                    selectedBrushSize = "large";
                    // popupWindow.dismiss();
                }
            });


        } else if (id == R.id.brush_btn) {

            view = inflater.inflate(R.layout.pencil_chooser, null);
            LinearLayout paintLayout = (LinearLayout) view.findViewById(R.id.paint_colors);


            seekBar = (SeekBar) view.findViewById(R.id.seekbar);
            seekBarOpacity = (SeekBar) view.findViewById(R.id.seekbarOpacity);
            txtPixcelPercentage = (TextView) view.findViewById(R.id.txtPixcel);
            txtOpacityPix = (TextView) view.findViewById(R.id.txtOpacityPer);
            currPaint = (ImageButton) paintLayout.getChildAt(selectedColor);
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));


            if (isSeekBarClicked == false) {
                seekBar.setProgress(progressBarPer);
                txtPixcelPercentage.setText("" + progressBarPer);
                drawView.setBrushSize(progressBarPer);
                drawView.setLastBrushSize(progressBarPer);


                opacityProgress = drawView.getPaintAlpha();
                seekBarOpacity.setProgress(opacityProgress);
                txtOpacityPix.setText("" + opacityProgress);
                drawView.setLastPaintAlpha(opacityProgress);
                lastPaintAlpha = drawView.getLastPaintAlpha();
                drawView.setPaintAlpha(lastPaintAlpha);

            } else {
                seekBar.setProgress(progressBarPer);
                txtPixcelPercentage.setText("" + progressBarPer);
                drawView.setBrushSize(progressBarPer);
                drawView.setLastBrushSize(progressBarPer);

                opacityProgress = drawView.getPaintAlpha();
                seekBarOpacity.setProgress(opacityProgress);
                txtOpacityPix.setText("" + opacityProgress);
                drawView.setLastPaintAlpha(opacityProgress);
                lastPaintAlpha = drawView.getLastPaintAlpha();
                drawView.setPaintAlpha(lastPaintAlpha);

            }

            //for set size of the brush
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    isSeekBarClicked = true;
                    progressBarPer = progress;
                    drawView.setErase(false);
                    drawView.setBrushSize(progress);
                    drawView.setLastBrushSize(progress);
                    txtPixcelPercentage.setText("" + progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            //for set opacity to brush
            seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    isSeekBarClicked = true;
                    drawView.setPaintAlpha(progress);
                    drawView.setLastPaintAlpha(progress);
                    opacityProgress = progress;
                    txtOpacityPix.setText("" + drawView.getPaintAlpha());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        } else if (id == R.id.shape_btn) {
            view = inflater.inflate(R.layout.shape_chooser, null);
            LinearLayout linearLayoutShapeChooser = (LinearLayout) view.findViewById(R.id.linearLayoutShapeChooser);

            imageCommingSoon = (ImageView)view.findViewById(R.id.imgCommingSoon);

        }

        popupWindow.setFocusable(true);
        popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setContentView(view);
        return popupWindow;
    }

    ///////////////////////////////////////////// for zoom in out with touch event ////////////////////////////////////

   /* public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        return true;
    }*/

    /*private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {


        float onScaleBegin = 0;
        float onScaleEnd = 0;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            imgBitMap.setScaleX(scale);
            imgBitMap.setScaleY(scale);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {

            Toast.makeText(getApplicationContext(),"Scale Begin" ,Toast.LENGTH_SHORT).show();
            onScaleBegin = scale;

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

            Toast.makeText(getApplicationContext(),"Scale Ended",Toast.LENGTH_SHORT).show();
            onScaleEnd = scale;

            if (onScaleEnd > onScaleBegin){
                Toast.makeText(getApplicationContext(),"Scaled Up by a factor of  " + String.valueOf( onScaleEnd / onScaleBegin ), Toast.LENGTH_SHORT  ).show();
            }

            if (onScaleEnd < onScaleBegin){
                Toast.makeText(getApplicationContext(),"Scaled Down by a factor of  " + String.valueOf( onScaleBegin / onScaleEnd ), Toast.LENGTH_SHORT  ).show();
            }

            super.onScaleEnd(detector);
        }
    }*/

////////////////////////// Code for Screen Recording/////////////////////////////////////////////////////////////

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }

        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            return;
        }

        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    public void onToggleScreenShare(boolean isRecordingStop) {
        if (isRecordingStop) {
            initRecorder();
            shareScreen();
        } else {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Stopping Recording");
            stopScreenSharing();
        }
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(Environment
                    .getExternalStoragePublicDirectory(Environment
                            .DIRECTORY_DOWNLOADS) + "/video.mp4");
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mToggleButton.isChecked()) {
                mToggleButton.setChecked(false);
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    //onToggleScreenShare(false);
                } else {
                    mToggleButton.setChecked(false);
                    alertDialogManager.Dialog("Permissions", "Permission is not enabled. Do you want to enable?", "ok", "cancel", new AlertDialogManager.onSingleButtonClickListner() {
                        @Override
                        public void onPositiveClick() {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        }

                        @Override
                        public void onNegativeClick() {

                        }
                    }).show();
                }
                return;
            }
        }
    }
}
