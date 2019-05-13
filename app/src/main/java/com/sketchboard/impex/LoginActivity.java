package com.sketchboard.impex;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.sketchboard.impex.helper.FbConnectHelper;
import com.sketchboard.impex.managers.SharedPreferenceManager;
import com.sketchboard.impex.model.UserModel;
import com.facebook.FacebookSdk;
import com.facebook.GraphResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity  implements FbConnectHelper.OnFbSignInListener{

    private static final String TAG = LoginActivity.class.getSimpleName();
    private Button facbookLoginBtn;
    private Button googlePlusLoginBtn;
    private ImageView imgPaintLogo;
    private FbConnectHelper fbConnectHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//will hide the title
        getSupportActionBar().hide(); //hide the title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //show the activity in full screen
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        fbConnectHelper = new FbConnectHelper(this,this);
        assignView();
        setListner();


    }

    private void setListner() {

        facbookLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fbConnectHelper.connect();
               // setBackground(R.color.fb_color);
            }
        });
    }

    private void assignView() {
        facbookLoginBtn = (Button)findViewById(R.id.btnFacebookLog);
        //googlePlusLoginBtn = (Button)findViewById(R.id.btnGoogleLog);
        imgPaintLogo = (ImageView)findViewById(R.id.imgPaint);
    }

    @Override
    public void OnFbSuccess(GraphResponse graphResponse) {
        UserModel userModel = getUserModelFromGraphResponse(graphResponse);
        if(userModel!=null) {
           // SharedPreferenceManager.getSharedInstance().saveUserModel(userModel);
            startHomeActivity(userModel);
        }
    }

    @Override
    public void OnFbError(String errorMessage) {
        resetToDefaultView(errorMessage);
    }

    private void resetToDefaultView(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startHomeActivity(UserModel userModel)
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(UserModel.class.getSimpleName(), userModel);
        startActivity(intent);
        this.finish();
    }

    private UserModel getUserModelFromGraphResponse(GraphResponse graphResponse)
    {
        UserModel userModel = new UserModel();
        try {
            JSONObject jsonObject = graphResponse.getJSONObject();
            userModel.userName = jsonObject.getString("name");
            userModel.userEmail = jsonObject.getString("email");
            String id = jsonObject.getString("id");
            String profileImg = "http://graph.facebook.com/"+ id+ "/picture?type=large";
            userModel.profilePic = profileImg;
            Log.i(TAG,profileImg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return userModel;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fbConnectHelper.onActivityResult(requestCode, resultCode, data);
        /*gSignInHelper.onActivityResult(requestCode, resultCode, data);
        twitterConnectHelper.onActivityResult(requestCode, resultCode, data);*/
    }
}
