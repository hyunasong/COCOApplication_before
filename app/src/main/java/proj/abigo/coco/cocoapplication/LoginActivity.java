package proj.abigo.coco.cocoapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kakao.auth.ErrorCode;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.LoginButton;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;

import proj.abigo.coco.cocoapplication.Bluetooth.BluetoothService;


public class LoginActivity extends AppCompatActivity {

    /**
     * 로그인 버튼을 클릭 했을시 access token을 요청하도록 설정한다.
     *
     * @param savedInstanceState 기존 session 정보가 저장된 객체
     */

    SessionCallback callback;

    LoginButton com_kakao_login;
    Button btnBtConnect;

    private static final String TAG = "Login";

    public static final String NICKNAME = "nick";
    public static final String USER_ID = "id";
    public static final String PROFILE_IMG ="img";

    private static final boolean D = true;

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final int MESSAGE_STATE_CHANGE = 3; // 블루투스 연결 상태 check

    BluetoothService btService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();

        if(btService == null){
            btService = new BluetoothService(this, handler);
        }
        //getHashKey();
/*
        UserManagement.requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                //로그아웃
            }
        });
 */
//        com_kakao_login = (LoginButton)findViewById(R.id.com_kakao_login);
//
//        com_kakao_login.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });

        //callback class 초기화
        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);

        setEvent();
    }

    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case MESSAGE_STATE_CHANGE :
                    if(D) Log.d(TAG, "MESSAGE_STATE_CHANGE" + msg.arg1);

                    switch (msg.arg1){
                        case BluetoothService.STATE_CONNECTED :
                            Toast.makeText(getApplicationContext(), "블루투스 연결에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            break;
                        case BluetoothService.STATE_FAIL:
                            Toast.makeText(getApplicationContext(), "블루투스 연결에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
            }
        }
    };

    private void setEvent() {

        btnBtConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btService.getDeviceState()){
                    // 블루투스 지원 가능 기기
                    btService.enableBluetooth();
                }else{
                    finish();
                }
            }
        });
    }


    private void initView() {

        btnBtConnect = (Button)findViewById(R.id.btnBtConnect);

    }

    //재로그인요청
    private void redirectLoginActivity(){
        final Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    //간편로그인 시 호출되는 부분
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_ENABLE_BT :
                if(resultCode == Activity.RESULT_OK){
                    // 기기 접속 요청
                    btService.scanDevice();
                }
                else{
                    Log.d(TAG, "Bluetooth is not enabled");
                }
                break;

            case REQUEST_CONNECT_DEVICE :
                if(resultCode == Activity.RESULT_OK){
                    // 검색된 기기에 접속
                    btService.getDeviceInfo(data);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }

    //SessionCallback 클래스 구현
    private class SessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() {
            UserManagement.requestMe(new MeResponseCallback() {
                @Override
                public void onFailure(ErrorResult errorResult){
                    String message = "failed to get user info. msg=" + errorResult;
                    Logger.d(message);

                    ErrorCode result = ErrorCode.valueOf(errorResult.getErrorCode());
                    if(result == ErrorCode.CLIENT_ERROR_CODE){
                        finish();
                    }else{
                        redirectLoginActivity();//재로그인
                    }
                }
                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                }

                @Override
                public void onNotSignedUp() {

                }

                @Override
                public void onSuccess(UserProfile result) {
                    if(result != null){
                        result.saveUserToCache();
                    }
                    Logger.e("succeeded to update user profile", result, "\n");

                    final String nickName = result.getNickname();
                    final long userID = result.getId();
                    final String pImage = result.getProfileImagePath();//사용자 프로필 경로
                    Log.e("UserProfile", result.toString());

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);

                    Log.d("nickname", nickName);
                    Log.d("USER_ID", String.valueOf(userID));
                    Log.d("PROFILE_IMG", pImage);


//                    intent.putExtra(NICKNAME,nickName);
//                    intent.putExtra(USER_ID,String.valueOf(userID));
//                    intent.putExtra(PROFILE_IMG,pImage);
                    startActivity(intent);
                    finish();
                }
            });
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if (exception != null) {
                Logger.e(exception);
            }
        }

    }


}



