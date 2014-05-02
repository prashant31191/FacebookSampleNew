
package com.nexercise.client.android.activities;

import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.LoggingBehavior;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.UiLifecycleHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;
import com.nexercise.client.android.R;
import com.nexercise.client.android.base.BaseActivity;
import com.nexercise.client.android.constants.APIConstants;
import com.nexercise.client.android.constants.APIConstants.APIJsonKeys;
import com.nexercise.client.android.constants.ApplicationConstants;
import com.nexercise.client.android.constants.DisplayConstants;
import com.nexercise.client.android.constants.FacebookConstants;
import com.nexercise.client.android.constants.MessagesConstants;
import com.nexercise.client.android.constants.PushNotificationConstants;
import com.nexercise.client.android.constants.UserPreferencesConstants;
import com.nexercise.client.android.helpers.FacebookHelper;
import com.nexercise.client.android.helpers.GplusHelper;
import com.nexercise.client.android.helpers.GsonHelper;
import com.nexercise.client.android.helpers.NxrActionBarMenuHelper;
import com.nexercise.client.android.helpers.PreferenceHelper;
import com.nexercise.client.android.helpers.WebServiceHelper;
import com.nexercise.client.android.model.Factory;
import com.nexercise.client.android.model.Model;
import com.nexercise.client.android.task.GooglePlusConnect;
import com.nexercise.client.android.utils.Funcs;
import com.nexercise.client.android.utils.Logger;
import com.nexercise.client.android.utils.Logger.ACTIVITY_STATUS;
import com.nexercise.client.android.utils.TextViewWatcher;
import com.socialize.api.SocializeSession;
import com.socialize.error.SocializeException;
import com.socialize.listener.SocializeAuthListener;
import com.socialize.networks.facebook.FacebookUtils;

public class FacebookLoginActivity extends BaseActivity implements OnClickListener,  PlusClient.ConnectionCallbacks, PlusClient.OnConnectionFailedListener{

	ImageButton btnFbLogin;
	//Button btnCancel;
	private Session.StatusCallback statusCallback = new SessionStatusCallback();

	TextView  btnRestoreAccount;
	ImageButton btnCreateAccount;
	/**  Navigation Drawer menu  changes  starts*/
	private NxrActionBarMenuHelper mActionBarHelper;
	/**  Navigation Drawer menu  changes  ends*/

	boolean closeActivity= false;
	private boolean newPermissionRequest = false;
	private UiLifecycleHelper uiHelper;
    private static final int DIALOG_GET_GOOGLE_PLAY_SERVICES = 1;

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES = 2;
    
    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;
    ImageButton gplaySignInButton;
    private boolean saveUserConnection = false;
    
	Handler handler= new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (msg.what == 1) {
				Funcs.showShortToast(MessagesConstants.USER_RESTORE_CHECK_EMAIL,
						FacebookLoginActivity.this);
				closeActivity= true;
			}
			else if (msg.what == 2) {
				Funcs.showShortToast(MessagesConstants.ERROR_USER_NOT_RESTORED,
						FacebookLoginActivity.this);
			}

		}
	};

	@Override
	public void fetchData() {

	}

	@Override
	public void initComponents() {

		Logger.activityStatusChanged(this.getLocalClassName(), ACTIVITY_STATUS.STARTED);
		Logger.context= this.getApplicationContext();
		setContentView(R.layout.facebook_login_activity);
		btnFbLogin= (ImageButton) findViewById(R.id.facebookLoginBtn);

		btnCreateAccount=(ImageButton)findViewById(R.id.createAccountBtn);
		btnRestoreAccount=(TextView)findViewById(R.id.btnrestoreAccount);
		//btnRestoreAccount.setText(Html.fromHtml("<u>Restore my account</u>"));
		gplaySignInButton = (ImageButton)findViewById(R.id.gplaySignInButton); 
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if(status == ConnectionResult.SUCCESS) {
		    //Success! Do what you want
			gplaySignInButton.setVisibility(View.VISIBLE);
		}
		else
		{
			gplaySignInButton.setVisibility(View.GONE);
		}
		
		Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
		uiHelper = new UiLifecycleHelper(this, statusCallback);
		
        mPlusClient = new PlusClient.Builder(this, this, this).build();
        
		/**  Navigation Drawer menu  changes  starts*/
		mActionBarHelper = new NxrActionBarMenuHelper(this,DisplayConstants.FACEBOOK_LOGIN_HEADING);
		/**  Navigation Drawer menu  changes  ends*/
	}

	public void initFaceBookLogin(){
		try{
			Session session = Session.getActiveSession();
			if (session == null) {
				session = new Session(this);
			}
			Session.setActiveSession(session);
			if (!session.isOpened() && !session.isClosed()) {
				session.openForRead(new Session.OpenRequest(FacebookLoginActivity.this).setCallback(statusCallback).setPermissions(Arrays.asList(FacebookConstants.READ_PERMISSIONS)));
			} else {
				Session.openActiveSession(FacebookLoginActivity.this, true, statusCallback);
			}

		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void loadData() {

		// TODO Auto-generated method stub

	}

	@Override
	public void setListeners() {

		btnFbLogin.setOnClickListener(this);
		//btnCancel.setOnClickListener(this);
		btnCreateAccount.setOnClickListener(this);
		btnRestoreAccount.setOnClickListener(this);
		gplaySignInButton.setOnClickListener(this);
	}

	public class FacebookUserCreationTask extends AsyncTask<String, Void, HashMap<String, Object>> {

		Activity activity;
		String accessToken= "";
		EditText textEmailId;
		HashMap<String, Object> userValues;

		public FacebookUserCreationTask(Activity activity) {

			this.activity= activity;
			try {
				if (mActionBarHelper != null) {
					mActionBarHelper.showProgressbar();
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		@Override
		protected HashMap<String, Object> doInBackground(String... params) {

			String response= "";
			String url= params[0];
			accessToken= params[2];
			HashMap<String, Object> tempMap= new HashMap<String, Object>();
			try {
				response= WebServiceHelper.INSTANCE.getFromWebServiceForFacebook(url);
				HashMap<String, Object> hashMap= GsonHelper.INSTANCE.parse(response);


				tempMap.put(APIJsonKeys.FB_ID.getValue(), hashMap.get("id").toString());
				tempMap.put(APIJsonKeys.CLIENT_APPLICATION_SETTINGS.getValue(), "y");
				tempMap.put(APIJsonKeys.REWARD_OPTIONS.getValue(), "y");
				tempMap.put(APIJsonKeys.FIRST_NAME.getValue(), hashMap.get("first_name").toString());
				tempMap.put(APIJsonKeys.LAST_NAME.getValue(), hashMap.get("last_name").toString());
				String usr_email="";
				try {
					usr_email=hashMap.get("email").toString();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
				}
				tempMap.put(APIJsonKeys.FB_EMAIL_ADDRESS.getValue(), usr_email);
				if(hashMap.get("picture").toString().contains("is_silhouette")){
					String pictureUrl = null;
					try{
						JSONObject jsonPicture  = new JSONObject(response);
						if(jsonPicture.has("picture")){
							if(jsonPicture.getJSONObject("picture").has("data")){
								pictureUrl = jsonPicture.getJSONObject("picture").getJSONObject("data").getString("url");
							}
						}
					}catch(Exception e){

					}
					tempMap.put(APIJsonKeys.FB_IMAGE_URL.getValue(), pictureUrl);
				}else{
					tempMap.put(APIJsonKeys.FB_IMAGE_URL.getValue(), hashMap.get("picture").toString());
				}
				tempMap.put(APIJsonKeys.CLIENT_VERSION.getValue(), ApplicationConstants.APP_VERSION);
				tempMap.put(APIJsonKeys.OS_VERSION.getValue(), Funcs.getOSVersion());
				tempMap.put(APIJsonKeys.DEVICE_MODEL.getValue(), Funcs.getDeviceName());
				tempMap.put(APIJsonKeys.LOCALE.getValue(), Funcs.getLocale());                
				if (hashMap.get(APIJsonKeys.GENDER.getValue()) != null) {
					if (((String) hashMap.get(APIJsonKeys.GENDER.getValue())).contentEquals("male"))
						tempMap.put(APIJsonKeys.GENDER.getValue(), "m");
					else
						tempMap.put(APIJsonKeys.GENDER.getValue(), "f");
				}

			} catch (Exception e) {

			}
			return tempMap;
		}

		@Override
		protected void onPostExecute(HashMap<String, Object> response) {
			if(response != null){
				String fb_email = null;
				if(response.containsKey(APIJsonKeys.FB_EMAIL_ADDRESS.getValue())){
					fb_email = response.get(APIJsonKeys.FB_EMAIL_ADDRESS.getValue()).toString();
				}
				userValues = response;
				if( fb_email == null || fb_email.equals("") ){
					final Dialog dlgFBEmail = new Dialog(activity, R.style.AdDialog);
					dlgFBEmail.setTitle("Update email");
					dlgFBEmail.setContentView(R.layout.dialog_facebook_email);
					dlgFBEmail.setCancelable(true);
					dlgFBEmail.setCanceledOnTouchOutside(false);
	
					textEmailId = (EditText) dlgFBEmail
							.findViewById(R.id.fbEmailId);	
	
					Button fbEmailUpdate = (Button) dlgFBEmail
							.findViewById(R.id.fbEmailUpdate);
	
					View.OnClickListener fbDialogListener = new View.OnClickListener() {
	
						@Override
						public void onClick(View v) {
	
							switch (v.getId()) {
							case R.id.fbEmailUpdate:
								if(verifyData(textEmailId)){
									String fb_id = "";
									try {
										fb_id = userValues.get(APIJsonKeys.FB_ID.getValue()).toString();
									} catch (Exception e) {
									}
									if(fb_id.equals("") || fb_id == null){
										userValues.remove(APIJsonKeys.EMAIL_ADDRESS.getValue());
										userValues.put(APIJsonKeys.EMAIL_ADDRESS.getValue(), textEmailId.getText().toString());
									}
									else{
										userValues.remove(APIJsonKeys.FB_EMAIL_ADDRESS.getValue());
										userValues.put(APIJsonKeys.FB_EMAIL_ADDRESS.getValue(), textEmailId.getText().toString());
									}					
									dlgFBEmail.cancel();
								}
								else{
									Funcs.showShortToast("Please enter email!", FacebookLoginActivity.this);
								}
								break;
							default:
								dlgFBEmail.cancel();
								break;
							}
	
						}
					};
	
					dlgFBEmail.setOnDismissListener(new OnDismissListener() {
	
						@Override
						public void onDismiss(DialogInterface dialog) {
							// TODO Auto-generated method stub
							new FacebookUserUpdateTask(activity,accessToken, userValues).execute();
						}
					});
	
					fbEmailUpdate.setOnClickListener(fbDialogListener);
					if(!FacebookLoginActivity.this.isFinishing()){
						dlgFBEmail.show();
					}
				}
				else{
					new FacebookUserUpdateTask(activity,accessToken, response).execute();
				}
			}

			try {
				if (mActionBarHelper != null) {
					mActionBarHelper.hideProgressBar();
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
	}

	private boolean verifyData(EditText textEmailId) {

		boolean flag= true;

		if (!TextViewWatcher.isValidEmail(textEmailId.getText().toString())) {
			textEmailId.setError(getResources().getString(R.string.msg_error_invalid_email));
			textEmailId.requestFocus();
			flag= false;
		}

		return flag;
	}

	public class FacebookUserUpdateTask extends AsyncTask<String, Void, String> {

		Activity activity;
		ProgressDialog progressDialog;
		String accessToken= "";
		HashMap<String, Object> tempMap;
		public FacebookUserUpdateTask(Activity activity, String accessToken,HashMap<String, Object> response) {

			this.activity= activity;
			this.tempMap = response;
			this.accessToken = accessToken;


		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			try {
				if (mActionBarHelper != null) {
					mActionBarHelper.showProgressbar();
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		@Override
		protected String doInBackground(String... params) {

			String response= "";
			try{
				String uuid= PreferenceHelper.getStringPreference(activity,
						UserPreferencesConstants.USER_PREFERENCES, UserPreferencesConstants.USER_UUID);

				response= new Model().createFacebookUser(tempMap,uuid);

			} catch (Exception e) {
				response= "";
				e.printStackTrace();
			}
			return response;
		}

		@Override
		protected void onPostExecute(String response) {

			if (!response.equals("")) {
				Model dataModel= new Model();
				Factory.setUser(dataModel.parseUser(response));
				if (Factory.getUser() != null) {
					activity.getSharedPreferences(PushNotificationConstants.PREF_NAME,
							Activity.MODE_WORLD_WRITEABLE).edit().clear().commit();
					PreferenceHelper.putStringPreference(activity,
							UserPreferencesConstants.USER_PREFERENCES,
							UserPreferencesConstants.USER_UUID, Factory.getUser().getUserInfo().userID);
					PreferenceHelper.putStringPreference(activity,
							UserPreferencesConstants.USER_PREFERENCES,
							UserPreferencesConstants.USER_FBID,
							Factory.getUser().getUserInfo().fbID);
					PreferenceHelper.putBooleanPreference(activity,
							UserPreferencesConstants.USER_PREFERENCES,
							UserPreferencesConstants.NAME_SYNC_WITH_FB, true);
					PreferenceHelper.putBooleanPreference(activity,
							UserPreferencesConstants.USER_PREFERENCES,
							UserPreferencesConstants.EMAIL_SYNC_WITH_FB, true);
					PreferenceHelper.putBooleanPreference(activity,
							UserPreferencesConstants.USER_PREFERENCES,
							UserPreferencesConstants.GENDER_SYNC_WITH_FB, true);
					FacebookHelper.saveAccessToken(activity, accessToken);
					FacebookUtils.linkForWrite(activity, accessToken, false, new SocializeAuthListener() {

						@Override
						public void onError(SocializeException error) {
							// TODO Auto-generated method stub
						}

						@Override
						public void onCancel() {
							// TODO Auto-generated method stub
						}

						@Override
						public void onAuthSuccess(SocializeSession session) {
							// TODO Auto-generated method stub
						}

						@Override
						public void onAuthFail(SocializeException error) {
							// TODO Auto-generated method stub
						}
					},FacebookConstants.PUBLISH_PERMISSIONS);
				}

				Intent intent= new Intent(activity, SplashActivity.class);
				intent.putExtra(SplashActivity.STATE, SplashActivity.STATE_CREATE_FROM_FACEBOOK);
				intent.putExtra("SHOWTUTORIAL", "yes");
				activity.startActivity(intent);
				activity.finish();
			}
			else {
				Funcs.showAlertDialog(MessagesConstants.ERROR_USER_NOT_CREATED_DESCRIPTION,
						MessagesConstants.ERROR_USER_NOT_CREATED_TITLE, activity);
			}
			try {
				if (mActionBarHelper != null) {
					mActionBarHelper.hideProgressBar();
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}



	@Override
	public void onClick(View v) {

		int id= v.getId();
		switch (id) {
		case R.id.facebookLoginBtn:
			onFacebookLogin();
			break;
			//case R.id.cancelBtn:
			// onNoThanks();
			//    break;
		case R.id.createAccountBtn:
			if (Funcs.isInternetReachable(this.getApplicationContext()))
				createAccount();
			else
				Funcs.showAlertDialog(MessagesConstants.ERROR_INTERNET_NOT_FOUND, "Error", this);
			break;
		case R.id.btnrestoreAccount:
			if (Funcs.isInternetReachable(this.getApplicationContext()))
				RestoreAccount();
			else
				Funcs.showAlertDialog(MessagesConstants.ERROR_INTERNET_NOT_FOUND, "Error", this);
			break;
        case R.id.gplaySignInButton:
        	saveUserConnection = true;
            int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            if (available != ConnectionResult.SUCCESS) {
                showDialog(DIALOG_GET_GOOGLE_PLAY_SERVICES);
                return;
            }

            try {
            	if(mConnectionResult != null){
            		mConnectionResult.startResolutionForResult(this, REQUEST_CODE_SIGN_IN);
            	}else{
                    mPlusClient.connect();
            	}
            } catch (IntentSender.SendIntentException e) {
                // Fetch a new result to start.
                mPlusClient.connect();
            }
            break;
		default:
			break;
		}
	}

	private void onFacebookLogin() {

		// facebook.authorize(this, FacebookConstants.PERMISSIONS, this);
		// facebook.authorize(this,this);


		PackageManager pm = getPackageManager();
		try {
			PackageInfo pinfo=pm.getPackageInfo("com.facebook.katana", PackageManager.GET_CONFIGURATIONS);
			String versionName=pinfo.versionName;

			char first=versionName.charAt(0);
			if(first=='1'||first=='2')
			{

				//---------------------
				AlertDialog.Builder builder1 = new AlertDialog.Builder(FacebookLoginActivity.this);
				builder1.setTitle("Update Facebook App");
				builder1.setMessage("You are using an old version of Facebook.");
				builder1.setCancelable(true);
				builder1.setPositiveButton("Upgrade Facebook",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//----------------------
						String appName="com.facebook.katana";
						try {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
						} catch (android.content.ActivityNotFoundException anfe) {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
						}
						dialog.cancel();
					}
				});
				builder1.setNegativeButton("Sign up with email instead",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (Funcs.isInternetReachable(FacebookLoginActivity.this.getApplicationContext()))
							createAccount();
						else
							Funcs.showAlertDialog(MessagesConstants.ERROR_INTERNET_NOT_FOUND, "Error", FacebookLoginActivity.this);
						dialog.cancel();
					}
				});

				AlertDialog alert11 = builder1.create();
				alert11.setCanceledOnTouchOutside(true);
				alert11.show();

			}
			else{
				initFaceBookLogin();
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// if facebook app not installed on device!
			initFaceBookLogin();
		}
	}



	private void createAccount() {
		Intent intent= new Intent(FacebookLoginActivity.this, CreateAccountActivity.class);
		startActivity(intent);
		FacebookLoginActivity.this.finish();
	}

	private void RestoreAccount() {
		Intent intent= new Intent(FacebookLoginActivity.this, SignUpExistingUserActivity.class);
		startActivity(intent);
		FacebookLoginActivity.this.finish();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN
                || requestCode == REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES) {
            if (resultCode == RESULT_OK && !mPlusClient.isConnected()
                    && !mPlusClient.isConnecting()) {
                // This time, connect should succeed.
                mPlusClient.connect();
            }
        }
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		mPlusClient.connect();
	}

	@Override
	public void onPause() {

		Logger.activityStatusChanged(this.getLocalClassName(), ACTIVITY_STATUS.PAUSED);
		super.onPause();
		uiHelper.onPause();
		if (closeActivity) {
			this.finish();
		}
	}

	@Override
	public void onResume() {

		Logger.activityStatusChanged(this.getLocalClassName(), ACTIVITY_STATUS.RESUMED);
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	public void onStop() {
		 mPlusClient.disconnect();
		 super.onStop();
	}

	@Override
	public void onDestroy() {

		Logger.activityStatusChanged(this.getLocalClassName(), ACTIVITY_STATUS.FINISHED);
		super.onDestroy();
		uiHelper.onDestroy();

	}

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id != DIALOG_GET_GOOGLE_PLAY_SERVICES) {
            return super.onCreateDialog(id);
        }

        int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (available == ConnectionResult.SUCCESS) {
            return null;
        }
        if (GooglePlayServicesUtil.isUserRecoverableError(available)) {
            return GooglePlayServicesUtil.getErrorDialog(
                    available, this, REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES);
        }
        return new AlertDialog.Builder(this)
                .setMessage(R.string.plus_generic_error)
                .setCancelable(true)
                .create();
    }



	private class SessionStatusCallback implements Session.StatusCallback {
		String accessToken= "";
		private boolean createUser = false;
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			if (state.isOpened()&& !newPermissionRequest) {
				try {
					accessToken= session.getAccessToken();
					// Save access token in shared preferences ..
					FacebookHelper.saveAccessToken(FacebookLoginActivity.this, accessToken);
					if(hasFacebookPublishPermission()){
						newPermissionRequest = false;
						createUser = true;
					}else{
						newPermissionRequest = true;
						createUser = true;
						//session.requestNewPublishPermissions(new Session.NewPermissionsRequest(FacebookLoginActivity.this, Arrays.asList(FacebookConstants.PUBLISH_PERMISSIONS)));
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (state == SessionState.OPENED_TOKEN_UPDATED && newPermissionRequest) {
				newPermissionRequest = false;
				createUser = true;
				accessToken= session.getAccessToken();
			}
			else if (exception instanceof FacebookOperationCanceledException ||
					exception instanceof FacebookAuthorizationException)
			{
				if(state.isOpened() && !createUser && newPermissionRequest)
				{
					newPermissionRequest = true;
					createUser = true;
					accessToken= session.getAccessToken();

				}

			}
			if(createUser){
				try {
					if(accessToken != null){
						String fbGetUserInfoURl= Funcs.concactString(FacebookConstants.FACEBOOK_GRAPH_API,
								"me?fields=first_name,gender,last_name,email,picture&access_token=",
								accessToken);
						String createUserFromFbUrl= Funcs.concactString(APIConstants.API_ENDPOINT,
								APIConstants.API_CREATE_FB_USER_CALL);
						new FacebookUserCreationTask(FacebookLoginActivity.this).execute(fbGetUserInfoURl, createUserFromFbUrl,
								accessToken);
					}

				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
			if(exception != null){
				try{
					String error= exception.getMessage();
					Funcs.showShortToast(error, FacebookLoginActivity.this);
				}catch(Exception e){

				}
			}
		}

	}
	private boolean hasFacebookPublishPermission() {
		Session session = Session.getActiveSession();
		return session != null && session.getPermissions().contains("publish_actions");
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		 mConnectionResult = result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
        int gender   = -1;  
       if (mPlusClient.getCurrentPerson() != null && saveUserConnection) {
           Person currentPerson = mPlusClient.getCurrentPerson();
           Person.Name personName = currentPerson.getName();
           String firstName = personName.getGivenName();
           String lastName = personName.getFamilyName();
           String middleName = personName.getMiddleName();
           if(currentPerson.hasGender()){
        	   gender = currentPerson.getGender();
           }
           String Id = currentPerson.getId();
           String accountName = mPlusClient.getAccountName();
           Person.Image profilePic = currentPerson.getImage();
           String personGooglePlusProfile = profilePic.getUrl();
           
           HashMap<String, Object> hashMap = new HashMap<String, Object>();
           hashMap.put("googleID", Id);
           hashMap.put("first_name", firstName);
           hashMap.put("last_name", lastName);
           hashMap.put("googleImageUrl", personGooglePlusProfile);
           if(gender != -1){
        	   if(gender == Person.Gender.MALE){
        		   hashMap.put(APIJsonKeys.GENDER.getValue(),"m");
        	   }else if(gender == Person.Gender.FEMALE){
        		   hashMap.put(APIJsonKeys.GENDER.getValue(),"f");
        	   }
           }else{
        	   hashMap.put(APIJsonKeys.GENDER.getValue(),null);
           }
           hashMap.put("email", accountName);   
           GplusHelper.saveAccountName(FacebookLoginActivity.this, accountName);
          new GooglePlusConnect(FacebookLoginActivity.this,mActionBarHelper).execute(hashMap);
       }
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		if(mPlusClient != null){
		  mPlusClient.connect();
		}
	}
}
