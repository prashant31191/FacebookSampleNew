package com.nexercise.client.android.activities;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.kiip.sdk.Kiip;
import me.kiip.sdk.Kiip.OnContentListener;
import me.kiip.sdk.Poptart;
//import me.kiip.sdk.Poptart.OnShowListener;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.OpenGraphAction;
import com.mediabrix.android.api.IAdEventsListener;
import com.mediabrix.android.api.MediabrixAPI;
import com.nexercise.client.android.BuildConfig;
import com.nexercise.client.android.NexerciseApplication;
import com.nexercise.client.android.R;
import com.nexercise.client.android.constants.DisplayConstants;
import com.nexercise.client.android.constants.FacebookConstants;
import com.nexercise.client.android.constants.FacebookTwitterSocializePostsConstants;
import com.nexercise.client.android.constants.ImageConstants;
import com.nexercise.client.android.constants.KiipConstants;
import com.nexercise.client.android.constants.MediaBrixConstants;
import com.nexercise.client.android.constants.MessagesConstants;
import com.nexercise.client.android.constants.RewardConstants;
import com.nexercise.client.android.constants.SocializeConstants;
import com.nexercise.client.android.constants.TwitterConstants;
import com.nexercise.client.android.constants.UserPreferencesConstants;
import com.nexercise.client.android.entities.ExerciseSession;
import com.nexercise.client.android.entities.Medal;
import com.nexercise.client.android.entities.PointsEarned;
import com.nexercise.client.android.entities.UserInfo;
import com.nexercise.client.android.entities.Winnings;
import com.nexercise.client.android.helpers.FacebookHelper;
import com.nexercise.client.android.helpers.FlurryHelper;
import com.nexercise.client.android.helpers.KiipHelper;
import com.nexercise.client.android.helpers.PreferenceHelper;
import com.nexercise.client.android.helpers.ScreenHelper;
import com.nexercise.client.android.helpers.TwitterHelper;
import com.nexercise.client.android.interfaces.CompleteAction;
import com.nexercise.client.android.interfaces.EarnAction;
import com.nexercise.client.android.interfaces.NXRMedalGraphicObject;
import com.nexercise.client.android.interfaces.NXRPhysicalActivityGraphObject;
import com.nexercise.client.android.model.Model;
import com.nexercise.client.android.utils.BitmapManager;
import com.nexercise.client.android.utils.Funcs;
import com.nexercise.client.android.utils.NXRRewardsManager;
import com.socialize.CommentUtils;
import com.socialize.SubscriptionUtils;
import com.socialize.api.action.comment.CommentOptions;
import com.socialize.entity.Comment;
import com.socialize.entity.Entity;
import com.socialize.entity.Subscription;
import com.socialize.error.SocializeException;
import com.socialize.listener.comment.CommentAddListener;
import com.socialize.listener.subscription.SubscriptionResultListener;
import com.socialize.notifications.SubscriptionType;

public class WinningsViewActivity extends com.sessionm.api.BaseActivity
implements View.OnClickListener, IAdEventsListener {

	Button continueBtn;
	LinearLayout pointsEarnedListView;
	LinearLayout medalsListView;
	LinearLayout headerForPointsEarned;
	LinearLayout headerForMedals;
	TextView totalPointsEarned;
	RelativeLayout layoutMedalDetail;
	Winnings winning;
	TextView txtMedalTitle;
	TextView txtMedalDescription;
	ImageView imgMedalDetail;
	int totalPoints = 0;
	int baseXP;
	int bonusXP;
	Button btnPostXPToFacebook;
	Button btnPostXPToTwitter;
	Button btnPostXPToSocialize;
	Button btnPostMedalToFacebook;
	Button btnPostMedalToTwitter;
	Button btnPostMedalToSocialize;

	private Twitter twitter;
	// private RequestToken twitterRequestToken;
	// private WebView twitterSite;
	private String webLocationOfImages = "";

	public Activity activity;

	boolean shouldPostXPToFacebook = false;
	boolean shouldPostMedalsToFacebook = false;
	boolean shouldPostXPToTwitter = false;
	boolean shouldPostMedalsToTwitter = false;
	boolean shouldPostXPToSocialize = false;
	boolean shouldPostMedalsToSocialize = false;

	boolean xpPostedToFacebook = false;
	boolean medalsPostedToFacebook = false;
	boolean isNewFacebookPublishPermission = false;
	boolean isMediabrixReadyToShow = false;
	boolean doShowMediabrixAd = false;
	boolean isMediaBrixUnAvailable = false;

	boolean isKiipRedeemed = false;
	String mCountyCode;

	UserInfo userInfo;
	ExerciseSession exerciseSession;
	private PendingAction pendingAction = PendingAction.NONE;

	private final String PENDING_ACTION_BUNDLE_KEY = "com.nexercise.client.android:PendingAction";

	private enum PendingAction {
		NONE, POST_TO_FB
	}

	private UiLifecycleHelper uiHelper;
	private static final List<String> PERMISSIONS = Arrays
			.asList("publish_actions");

	//MediaBrixHelper mMediaBrixHelper;
	private static final int AD_ACTIVITY = 2222;
	private final String TAG = "PaeDaeCall";
	private int PaeDaeRequested = 0;

	NXRRewardsManager nxr;
	private ArrayList<NXRMedalGraphicObject> mMedalsEarned = new ArrayList<NXRMedalGraphicObject>();
	private ArrayList<NXRMedalGraphicObject> mMedalsBonus = new ArrayList<NXRMedalGraphicObject>();
	private ArrayList<Integer> rewardKeys = new ArrayList<Integer>();
	private int rewardExecutionIndex = 0;
	ProgressBar progressBarHorizontal;
	boolean isDismissWinningsViewIfNoReward = false;
	boolean isMediabrixRewardReceived = false;

	TextView txtProgressInfo;

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			String name = savedInstanceState
					.getString(PENDING_ACTION_BUNDLE_KEY);
			pendingAction = PendingAction.valueOf(name);
		}
		getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			activity = this;
			try {
				HashMap<String, Object> hashMap = Funcs
						.castObjectToHashMap(extras.getSerializable("Winnings"));
				winning = new Model().parseWinnings(hashMap);
				webLocationOfImages = ((NexerciseApplication) activity
						.getApplication()).getDataLayerInstance()
						.getAppSettings().webLocationOfImages;

				try {
					exerciseSession = (ExerciseSession) extras
							.getSerializable("ExerciseSession");
				} catch (Exception e) {
					e.printStackTrace();
				}
				userInfo = (UserInfo) extras.getSerializable("userInfo");

				if (exerciseSession != null && winning != null) {
					Entity entity = Funcs.getEntityFromExerciseSessionAndName(
							userInfo, exerciseSession, winning.exerciseName);
					subscribeEntity(entity);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				winning = null;
			}

		}
		initComponents();
		setListeners();
		loadData();
	}

	public void initComponents() {

		setContentView(R.layout.winnings_view_dialog);
		continueBtn = (Button) findViewById(R.id.continueBtn);
		pointsEarnedListView = (LinearLayout) findViewById(R.id.pointsEarnedLayout);
		medalsListView = (LinearLayout) findViewById(R.id.medalsLayout);
		totalPointsEarned = (TextView) findViewById(R.id.totalPointsEarned);
		headerForPointsEarned = (LinearLayout) findViewById(R.id.headerForPointsEarned);
		headerForMedals = (LinearLayout) findViewById(R.id.headerForMedals);
		layoutMedalDetail = (RelativeLayout) findViewById(R.id.layoutMedalDetailForWinnings);
		txtMedalTitle = (TextView) findViewById(R.id.txtMedalDetailTitleForWinnings);
		txtMedalDescription = (TextView) findViewById(R.id.txtMedalDetailForWinnings);
		imgMedalDetail = (ImageView) findViewById(R.id.imgMedalDetailForWinnings);
		btnPostXPToFacebook = (Button) findViewById(R.id.postToFacebookButton);
		btnPostXPToTwitter = (Button) findViewById(R.id.postToTwitterButton);
		btnPostXPToSocialize = (Button) findViewById(R.id.postToSocializeButton);
		btnPostMedalToFacebook = (Button) findViewById(R.id.postToFacebookButtonForMedals);
		btnPostMedalToTwitter = (Button) findViewById(R.id.postToTwitterButtonForMedals);
		btnPostMedalToSocialize = (Button) findViewById(R.id.postToSocializeButtonForMedals);

		txtProgressInfo = (TextView) findViewById(R.id.txtProgressInfo);
		progressBarHorizontal = (ProgressBar) findViewById(R.id.progressBarHorizontal);
		progressBarHorizontal.setVisibility(View.GONE);

		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(TwitterConstants.CONSUMER_KEY,
				TwitterConstants.CONSUMER_SECRET_KEY);

		setSocialIntegrationButtonStates();
		FlurryHelper.startSession(activity);
		nxr = new NXRRewardsManager(activity);
		mCountyCode = Funcs.getCountryCode(activity).toUpperCase();

		if (isInstantRewardsOn()) {
			try {
				MediabrixAPI.getInstance().initialize(WinningsViewActivity.this, MediaBrixConstants.BASE_URL, MediaBrixConstants.APP_ID, this);
			} catch (Exception e) {
				/* *** Flurry changes *** */
				Map<String, String> flurryParams = new HashMap<String, String>();
				flurryParams.put("vendor", "Mb");
				FlurryHelper.logEvent("A:Rewards.Unit.LoadFailed", flurryParams);
				/* *** Flurry changes ends *** */
			}

		}
	}

	public void loadData() {

		loadDataIntoPointsEarned();
		loadDataIntoMedals();
		addKeysToRewardList();
		totalPointsEarned
		.setText(Html
				.fromHtml("Total e<font color='#f68621'>X</font>ercise <font color='#f68621'>P</font>oints (XP) = <font color='#329832'>"
						+ String.valueOf(totalPoints) + "</font>"));
		getWindow().getDecorView().invalidate();
		progressBarHorizontal.setVisibility(View.GONE);
		txtProgressInfo.setVisibility(View.GONE);
	}

	private void loadDataIntoPointsEarned() {

		try {
			ArrayList<PointsEarned> pointsEarnedList = winning.pointsEarnedList;
			if (pointsEarnedList.size() < 1) {
				headerForPointsEarned.setVisibility(View.GONE);
				pointsEarnedListView.setVisibility(View.GONE);
				return;
			}

			for (final PointsEarned pointsEarned : pointsEarnedList) {
				totalPoints = totalPoints + pointsEarned.amount;
				int i = 0;
				View item = ((LayoutInflater) this
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.component_points_earned, null, false);

				TextView txtName = (TextView) item
						.findViewById(R.id.pointsName);
				TextView txtAmount = (TextView) item
						.findViewById(R.id.pointsAmount);
				ImageView pointsImage = (ImageView) item
						.findViewById(R.id.pointsImage);
				TextView txtShortDesc = (TextView) item
						.findViewById(R.id.pointsDesc);

				try {
					if (!pointsEarned.displayName.equals("")) {
						txtName.setText(pointsEarned.displayName);
						if (pointsEarned.shortDescription != null)
							if (pointsEarned.shortDescription.length() > 1) {
								txtShortDesc.setVisibility(View.VISIBLE);
								txtShortDesc
								.setText(pointsEarned.shortDescription);
							}
					} else
						txtName.setText(pointsEarned.shortDescription);
				} catch (Exception e) {
					txtName.setText(pointsEarned.shortDescription);
				}
				if (pointsEarned.shortDescription.equals("Exercise XP")) {
					baseXP = pointsEarned.amount;
				} else {
					if (pointsEarned.amount > 0) {
						bonusXP += pointsEarned.amount;
						String postURL = FacebookConstants.POST_ACTION_URL_MEDAL;
						try {
							if (!pointsEarned.displayName.equals("")) {
								postURL += "&og:title="
										+ pointsEarned.displayName;
							}
						} catch (Exception e) {
							postURL += "&og:title= Medal";
						}
						String link = FacebookConstants.POST_MEDAL_EARNED_LINK
								.replace("#MedalName#",
										pointsEarned.displayName);
						postURL += "&og:url=" + link + "&og:image="
								+ webLocationOfImages + pointsEarned.imageLink;
						postURL += "&og:description="
								+ pointsEarned.shortDescription;
						postURL += "&body=" + pointsEarned.shortDescription;// medal.shortDescription;
						postURL += "&nexercise:times_bonus_achieved=1&fb:explicitly_shared=true";
						NXRMedalGraphicObject medal = GraphObject.Factory
								.create(NXRMedalGraphicObject.class);
						medal.setUrl(postURL);
						medal.setProperty("fb:explicitly_shared", true);

						mMedalsBonus.add(medal);
					}
				}

				txtAmount.setText(String.valueOf(pointsEarned.amount));
				String url = webLocationOfImages + pointsEarned.imageLink;
				BitmapManager.INSTANCE
				.loadBitmap(url, pointsImage,
						ImageConstants.POINTS_EARNED_WIDTH,
						ImageConstants.POINTS_EARNED_HEIGHT, false,
						false, true);

				pointsEarnedListView.addView(item);
				item.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {

						String displayName = pointsEarned.displayName;
						if (displayName == null) {
							displayName = pointsEarned.shortDescription;

						}
						String imageUrl = webLocationOfImages
								+ pointsEarned.imageLink;
						String description = pointsEarned.longDescription;
						showMedalDetail(displayName, imageUrl, description);
					}
				});
			}
			// postBonusMedalsWonToFacebook(true);
		} catch (Exception e) {
			headerForPointsEarned.setVisibility(View.GONE);
			pointsEarnedListView.setVisibility(View.GONE);
		}
	}

	private void showMedalDetail(String title, String imageUrl,
			String description) {

		try {
			txtMedalTitle.setText(title);
			txtMedalDescription.setText(description);

			imgMedalDetail.setImageBitmap(null);
			String largeImageUrl = imageUrl.replace(".png", "_large.png");
			Log.d("Nexercise", "large image : " + largeImageUrl);

			if (ScreenHelper.isLargeScreen(this))
				BitmapManager.INSTANCE.loadBitmap(largeImageUrl,
						imgMedalDetail,
						ImageConstants.MEDAL_DETAIL_WIDTH_LARGE,
						ImageConstants.MEDAL_DETAIL_HEIGHT_LARGE, false, false,
						true);
			else
				BitmapManager.INSTANCE.loadBitmap(largeImageUrl,
						imgMedalDetail, ImageConstants.MEDAL_DETAIL_WIDTH,
						ImageConstants.MEDAL_DETAIL_HEIGHT, false, false, true);

			layoutMedalDetail.setVisibility(View.VISIBLE);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private boolean isMedalDetailVisible() {

		if (layoutMedalDetail.getVisibility() == View.VISIBLE) {
			return true;
		}
		return false;
	}

	private void addKeysToRewardList(){

		if(winning != null){
			if(winning.rewards != null){
				/* *** Flurry changes *** */
				Map<String, String> flurryParams = new HashMap<String, String>();
				flurryParams.put("vendorSequence", winning.rewards);
				FlurryHelper.logEvent("A:Rewards", flurryParams);
				/* *** Flurry changes ends *** */

				int index = 0;
				for(int i=0;i< winning.rewards.length(); i= i+2){
					String sub = winning.rewards.substring(i, i+2);
					if(sub.toLowerCase().equals("kp")){
						rewardKeys.add(index,RewardConstants.KIIP_KEY);
						index ++;
					}else if(sub.toLowerCase().equals("mb")){
						rewardKeys.add(index,RewardConstants.MEDIABRIX_KEY);
						index ++;
					}
				}
			}
			else{
				/* *** Flurry changes *** */
				FlurryHelper.logEvent("T:Rewards.VendorSequenceFailed");
				/* *** Flurry changes ends *** */
			}
		}

	}

	private void loadDataIntoMedals() {

		try {
			ArrayList<Medal> medalsList = winning.medals;
			if (medalsList.size() < 1) {
				headerForMedals.setVisibility(View.GONE);
				medalsListView.setVisibility(View.GONE);
				return;
			}
			for (final Medal medal : medalsList) {
				View item = ((LayoutInflater) this
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.component_points_earned, null, false);

				TextView txtName = (TextView) item
						.findViewById(R.id.pointsName);
				TextView txtShortDesc = (TextView) item
						.findViewById(R.id.pointsDesc);
				ImageView pointsImage = (ImageView) item
						.findViewById(R.id.pointsImage);

				try {
					txtName.setText(medal.displayName);
					if (medal.shortDescription != null)
						if (medal.shortDescription.length() > 2) {
							txtShortDesc.setText(medal.shortDescription);
							txtShortDesc.setVisibility(View.VISIBLE);
						}
					;

				} catch (Exception e) {

				}

				String url = webLocationOfImages + medal.imageLink;
				BitmapManager.INSTANCE
				.loadBitmap(url, pointsImage,
						ImageConstants.POINTS_EARNED_WIDTH,
						ImageConstants.POINTS_EARNED_HEIGHT, false,
						false, true);

				medalsListView.addView(item);

				String postURL = FacebookConstants.POST_ACTION_URL_MEDAL;
				try {
					if (!medal.displayName.equals("")) {
						postURL += "&og:title=" + medal.displayName;
					}
				} catch (Exception e) {
					postURL += "&og:title= Medal";
				}
				String link = FacebookConstants.POST_MEDAL_EARNED_LINK.replace(
						"#MedalName#", medal.name);
				postURL += "&og:url=" + link + "&og:image=" + url;
				postURL += "&og:description=" + medal.shortDescription;
				postURL += "&body=" + medal.shortDescription;// medal.shortDescription;
				postURL += "&nexercise:times_bonus_achieved=1";
				NXRMedalGraphicObject medalsEarnedObject = GraphObject.Factory
						.create(NXRMedalGraphicObject.class);
				medalsEarnedObject.setUrl(postURL);
				mMedalsEarned.add(medalsEarnedObject);
				item.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {

						String displayName = medal.displayName;
						String imageUrl = webLocationOfImages + medal.imageLink;
						String description = medal.longDescription;
						showMedalDetail(displayName, imageUrl, description);
					}
				});

			}
		} catch (Exception e) {
			headerForMedals.setVisibility(View.GONE);
			medalsListView.setVisibility(View.GONE);
		}
	}

	public void setListeners() {

		continueBtn.setOnClickListener(this);
		layoutMedalDetail.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				hideMedalDetail();
			}
		});

		btnPostXPToFacebook.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (shouldPostXPToFacebook) {
					btnPostXPToFacebook
					.setBackgroundResource(R.drawable.post_to_facebook_off);
					shouldPostXPToFacebook = false;
				} else {
					if (!FacebookHelper.isLoggedIn(activity))
						openFacebookLoginDialog();

					btnPostXPToFacebook
					.setBackgroundResource(R.drawable.post_to_facebook_on);
					shouldPostXPToFacebook = true;
				}
			}
		});

		btnPostXPToTwitter.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (shouldPostXPToTwitter) {
					btnPostXPToTwitter
					.setBackgroundResource(R.drawable.post_to_twitter_off);
					shouldPostXPToTwitter = false;
				} else {
					openTwitterLoginActivity();
					btnPostXPToTwitter
					.setBackgroundResource(R.drawable.post_to_twitter_on);
					shouldPostXPToTwitter = true;
				}
			}
		});

		btnPostXPToSocialize.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (shouldPostXPToSocialize) {
					btnPostXPToSocialize
					.setBackgroundResource(R.drawable.post_to_socialize_off);
					shouldPostXPToSocialize = false;
				} else {
					btnPostXPToSocialize
					.setBackgroundResource(R.drawable.post_to_socialize_on);
					shouldPostXPToSocialize = true;
				}
			}
		});

		btnPostMedalToFacebook.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (shouldPostMedalsToFacebook) {
					btnPostMedalToFacebook
					.setBackgroundResource(R.drawable.post_to_facebook_off);
					shouldPostMedalsToFacebook = false;
				} else {
					if (!FacebookHelper.isLoggedIn(activity))
						openFacebookLoginDialog();

					btnPostMedalToFacebook
					.setBackgroundResource(R.drawable.post_to_facebook_on);
					shouldPostMedalsToFacebook = true;
				}
			}
		});

		btnPostMedalToTwitter.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (shouldPostMedalsToTwitter) {
					btnPostMedalToTwitter
					.setBackgroundResource(R.drawable.post_to_twitter_off);
					shouldPostMedalsToTwitter = false;
				} else {
					openTwitterLoginActivity();
					btnPostMedalToTwitter
					.setBackgroundResource(R.drawable.post_to_twitter_on);
					shouldPostMedalsToTwitter = true;
				}

			}
		});

		btnPostMedalToSocialize.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (shouldPostMedalsToSocialize) {
					btnPostMedalToSocialize
					.setBackgroundResource(R.drawable.post_to_socialize_off);
					shouldPostMedalsToSocialize = false;
				} else {
					btnPostMedalToSocialize
					.setBackgroundResource(R.drawable.post_to_socialize_on);
					shouldPostMedalsToSocialize = true;
				}
			}
		});
		setInfoButtonListener();
	}

	public void setInfoButtonListener() {
		ImageView info = (ImageView) findViewById(R.id.info);
		info.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent dialogueIntent = new Intent(WinningsViewActivity.this,
						DialogueActivity.class);
				Bundle values = new Bundle();
				values.putString("title", "What's XP?");
				values.putString(
						"description",
						"eXercise Points (XP for short) are a measure of progress! They are not redeemable like mPOINTS or PocketChange. Tap 'Continue' to see what else you may have won...");
				dialogueIntent.putExtras(values);
				WinningsViewActivity.this.startActivityForResult(
						dialogueIntent, 1);
			}
		});

	}

	private void setSocialIntegrationButtonStates() {

		TwitterHelper.activity = activity;

		if (FacebookHelper.isLoggedIn(activity)) {
			setFacebookButtonStates();
		}

		if (TwitterHelper.isLoggedIn()) {
			setTwitterButtonStates();
		}

		setSocializeButtonStates();
	}

	private void setSocializeButtonStates() {

		boolean postXPToSocialize = PreferenceHelper.getBooleanPreference(
				activity, SocializeConstants.PREF_NAME,
				SocializeConstants.PREF_KEY_POST_NEW_XP);
		boolean postMedalToSocialize = PreferenceHelper.getBooleanPreference(
				activity, SocializeConstants.PREF_NAME,
				SocializeConstants.PREF_KEY_POST_NEW_MEDAL);

		if (postXPToSocialize) {
			btnPostXPToSocialize
			.setBackgroundResource(R.drawable.post_to_socialize_on);
			shouldPostXPToSocialize = true;
		}

		if (postMedalToSocialize) {
			btnPostMedalToFacebook
			.setBackgroundColor(R.drawable.post_to_socialize_on);
			shouldPostMedalsToSocialize = true;
		}
	}

	private void setFacebookButtonStates() {

		boolean postXPToFacebook = PreferenceHelper.getBooleanPreference(
				activity, FacebookConstants.PREF_NAME,
				FacebookConstants.PREF_KEY_POST_NEW_XP);
		boolean postMedalsToFacebook = PreferenceHelper.getBooleanPreference(
				activity, FacebookConstants.PREF_NAME,
				FacebookConstants.PREF_KEY_POST_NEW_MEDAL);

		if (postXPToFacebook) {
			btnPostXPToFacebook
			.setBackgroundResource(R.drawable.post_to_facebook_on);
			shouldPostXPToFacebook = true;
		}

		if (postMedalsToFacebook) {
			btnPostMedalToFacebook
			.setBackgroundResource(R.drawable.post_to_facebook_on);
			shouldPostMedalsToFacebook = true;
		}
	}

	private void setTwitterButtonStates() {

		boolean postXPToTwitter = PreferenceHelper.getBooleanPreference(
				activity, TwitterConstants.PREF_NAME,
				TwitterConstants.PREF_KEY_POST_NEW_XP);
		boolean postMedalsToTwitter = PreferenceHelper.getBooleanPreference(
				activity, TwitterConstants.PREF_NAME,
				TwitterConstants.PREF_KEY_POST_NEW_MEDAL);

		if (postXPToTwitter) {
			btnPostXPToTwitter
			.setBackgroundResource(R.drawable.post_to_twitter_on);
			shouldPostXPToTwitter = true;
		}

		if (postMedalsToTwitter) {
			btnPostMedalToTwitter
			.setBackgroundResource(R.drawable.post_to_twitter_on);
			shouldPostMedalsToTwitter = true;
		}
	}

	private void openTwitterLoginActivity() {

		TwitterHelper.activity = activity;
		if (!TwitterHelper.isLoggedIn()) {
			((NexerciseApplication) activity.getApplication())
			.showTwitterLoginActivity(activity, false, false);
		}
	}

	private void postXPEarnedToTwitter(boolean isSelfReported) {

		String userMessage = TwitterConstants.POST_XP_TWEET;
		userMessage = userMessage.replace("#NumberOfMinutes#",
				String.valueOf(winning.numberOfMinutesExercised));
		userMessage = userMessage.replace("#ExerciseName#",
				winning.exerciseName);
		userMessage = userMessage.replace("#XPEarned#",
				String.valueOf(totalPoints));
		if (isSelfReported)
			userMessage = userMessage.replace("#I_FINISHED#",
					FacebookTwitterSocializePostsConstants.I_REPORTED[0]);
		else
			userMessage = userMessage
			.replace(
					"#I_FINISHED#",
					FacebookTwitterSocializePostsConstants.I_FINISHED[Funcs
					                                                  .getRandomInteger(FacebookTwitterSocializePostsConstants.I_FINISHED.length)]);
		// Funcs.showShortToast("Submitting tweet", this);

		boolean flag = true;

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey(TwitterConstants.CONSUMER_KEY)
		.setOAuthConsumerSecret(TwitterConstants.CONSUMER_SECRET_KEY)
		.setOAuthAccessToken(
				PreferenceHelper.getStringPreference(activity,
						TwitterConstants.PREF_NAME,
						TwitterConstants.PREF_KEY_ACCESS_TOKEN))
						.setOAuthAccessTokenSecret(
								PreferenceHelper.getStringPreference(activity,
										TwitterConstants.PREF_NAME,
										TwitterConstants.PREF_KEY_ACCESS_TOKEN_SECRET));
		;

		try {
			Twitter twitter = new TwitterFactory(cb.build()).getInstance();
			twitter.updateStatus(userMessage);
		} catch (Exception exception) {

			flag = false;
		}

		if (flag) {
			// Funcs.showShortToast("Tweet submitted", activity);
		}
	}

	private void postXPEarnedToSocialize(boolean isSelfReported) {

		String userMessage = SocializeConstants.POST_XP;
		userMessage = userMessage.replace("#NumberOfMinutes#",
				String.valueOf(winning.numberOfMinutesExercised));
		userMessage = userMessage.replace("#ExerciseName#",
				winning.exerciseName);
		userMessage = userMessage.replace("#XPEarned#",
				String.valueOf(totalPoints));
		if (isSelfReported)
			userMessage = userMessage.replace("#I_FINISHED#",
					FacebookTwitterSocializePostsConstants.I_REPORTED[0]);
		else
			userMessage = userMessage
			.replace(
					"#I_FINISHED#",
					FacebookTwitterSocializePostsConstants.I_FINISHED[Funcs
					                                                  .getRandomInteger(FacebookTwitterSocializePostsConstants.I_FINISHED.length)]);
		// Funcs.showShortToast("Submitting tweet", this);

		try {
			Entity entity = new Entity(SocializeConstants.ENTITY_KEY_BASE
					+ "chat/activities", "Activity Feed");
			CommentOptions opts = CommentUtils.getUserCommentOptions(activity);
			opts.setSubscribeToUpdates(false);
			opts.setShowShareDialog(false);
			CommentUtils.addComment(activity, entity, userMessage, opts,
					new CommentAddListener() {
				public void onError(SocializeException e) {
				}

				public void onCreate(Comment comment) {
				}
			});
		} catch (Exception exception) {
		}
	}

	private void openFacebookLoginDialog() {

		((NexerciseApplication) activity.getApplication())
		.showFacebookConnectActivity(activity);
	}

	private void postXPEarnedToFacebook(boolean isSelfReported) {
		if (xpPostedToFacebook)
			return;
		// Run this in a background thread since some of the populate methods
		// may take
		// a non-trivial amount of time.
		AsyncTask<Void, Integer, Response> task = new AsyncTask<Void, Integer, Response>() {
			String progressMessage = "";
			@Override
			protected void onPreExecute() {

			}

			@Override
			protected Response doInBackground(Void... voids) {
				progressMessage = MessagesConstants.WINNINGS_VIEW_SHARING_TO_FB;
				publishProgress(70);
				xpPostedToFacebook = true;
				Bundle params = new Bundle();
				params.putInt("total_xp", totalPoints);
				params.putInt("base_xp", baseXP);
				params.putInt("bonus_xp", bonusXP);
				params.putString("fb:explicitly_shared", "true");

				String activityIconUrl = "";
				try {
					if (exerciseSession.exerciseActivity != null) {
						activityIconUrl = FacebookConstants.POST_ACTION_EXERCISE_IMAGE_PATH
								+ exerciseSession.exerciseActivity + ".png";
					}
				} catch (Exception e) {
					activityIconUrl = FacebookConstants.POST_XP_EARNED_THUMBNAIL_URLS[2];
				}
				String duration = "";
				try {
					duration = Funcs
							.returnTimeCovered(winning.numberOfMinutesExercised);
				} catch (Exception e) {
					e.printStackTrace();
				}
				String postURL = FacebookConstants.POST_ACTION_URL_EXERCISE;
				String distanceAndPaceString = "";

				try {
					if (exerciseSession.distanceInMeters != null
							&& exerciseSession.distanceInMeters > 0) {
						String distance = PreferenceHelper.getStringPreference(
								activity, DisplayConstants.PREF_NAME,
								DisplayConstants.PREF_KEY_DISTANCE,
								DisplayConstants.PREF_VALUE_ENGLISH);
						// String suffix =
						// (distance.equals(DisplayConstants.PREF_VALUE_ENGLISH))
						// ?" mi":" km";
						double modDistance = ((double) exerciseSession.distanceInMeters)
								/ ((distance
										.equals(DisplayConstants.PREF_VALUE_ENGLISH)) ? 1609.344d
												: 1000.0d);
						String unit = PreferenceHelper.getStringPreference(
								activity, DisplayConstants.PREF_NAME,
								DisplayConstants.PREF_KEY_DISTANCE,
								DisplayConstants.PREF_VALUE_ENGLISH);
						StringBuilder strBuilder = new StringBuilder();
						NumberFormat nf = new DecimalFormat("#0.00");

						if (unit.contentEquals(DisplayConstants.PREF_VALUE_ENGLISH)) {
							strBuilder
							.append(nf
									.format((exerciseSession.distanceInMeters / 1000.0d) * 0.621371192d)
									+ " mi ");
						} else if (unit
								.contentEquals(DisplayConstants.PREF_VALUE_METRIC)) {
							strBuilder
							.append(nf
									.format(exerciseSession.distanceInMeters / 1000.0d)
									+ " km ");
						}

						double unitsPerHour = modDistance
								/ (((double) exerciseSession.secondsExercised) / 3600);
						String suffix2 = (distance
								.equals(DisplayConstants.PREF_VALUE_ENGLISH)) ? " mph"
										: " kph";
						String paceString = nf.format(unitsPerHour) + suffix2;
						distanceAndPaceString = "&nexercise:distance="
								+ strBuilder.toString() + "&nexercise:pace="
								+ paceString;
					}
				} catch (Exception e) {

				}
				postURL += "&og:title=" + winning.exerciseName
						+ "&nexercise:duration=" + duration + "&og:image="
						+ activityIconUrl
						+ "&fb:explicitly_shared=true&og:description="
						+ FacebookConstants.POST_ACTION_EXERCISE_DESC
						+ "&body="
						+ FacebookConstants.POST_ACTION_EXERCISE_DESC
						+ distanceAndPaceString;
				CompleteAction completeAction = GraphObject.Factory
						.create(CompleteAction.class);
				completeAction.setProperty("fb:explicitly_shared", true);
				if (mMedalsBonus != null && mMedalsBonus.size() > 0) {
					completeAction.setMedalBonus(mMedalsBonus);
				}
				if (mMedalsEarned != null && mMedalsEarned.size() > 0) {
					completeAction.setMedalsEarned(mMedalsEarned);
				}
				// Bundle bundle = new Bundle();
				// bundle.putString("title", "This is a test content");
				populateOGAction(completeAction, postURL);
				Request request = new Request(Session.getActiveSession(),
						FacebookConstants.POST_ACTION_PATH_COMPLETE, params,
						HttpMethod.POST);
				request.setGraphObject(completeAction);
				//progressMessage = MessagesConstants.WINNINGS_VIEW_SHARING_TO_FB;
				if (!shouldPostMedalsToFacebook) {
					publishProgress(99);
				}
				return request.executeAndWait();
			}
			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
				progressBarHorizontal.setProgress(values[0]);
				//txtProgressInfo.setText(progressMessage);
			}
			@Override
			protected void onPostExecute(Response response) {
				onPostActionResponse(response);
			}
		};

		task.execute();

	}

	protected void populateOGAction(OpenGraphAction action, String postURL) {
		CompleteAction completeAction = action.cast(CompleteAction.class);
		NXRPhysicalActivityGraphObject physical_activity = GraphObject.Factory
				.create(NXRPhysicalActivityGraphObject.class);
		physical_activity.setUrl(postURL);
		completeAction.setPhysicalActivity(physical_activity);

	}

	private void onPostActionResponse(Response response) {
		if (shouldPostMedalsToFacebook) {
			postMedalsWonToFacebook();
		} else {
			//stopMediaBrix();
			finish();
		}
	}

	protected void populateOGMedalAction(OpenGraphAction action, String postURL) {
		EarnAction earnAction = action.cast(EarnAction.class);
		NXRMedalGraphicObject medal = GraphObject.Factory
				.create(NXRMedalGraphicObject.class);
		medal.setUrl(postURL);
		earnAction.setProperty("fb:explicitly_shared", true);
		earnAction.setMedal(medal);

	}

	private void onMedalPostActionResponse(Response response) {
	}

	private void postMedalsWonToFacebook() {
		if (medalsPostedToFacebook)
			return;

		// Run this in a background thread since some of the populate methods
		// may take
		// a non-trivial amount of time.
		AsyncTask<Void, Integer, Integer> task = new AsyncTask<Void, Integer, Integer>() {
			String progressMessage = "";
			@Override
			protected void onPreExecute() {
			}

			@Override
			protected Integer doInBackground(Void... voids) {
				progressMessage = MessagesConstants.WINNINGS_VIEW_SHARING_MEDALS_TO_FB;
				publishProgress(80);
				ArrayList<Medal> medals = winning.medals;
				if (medals != null) {
					for (int i = 0; i < medals.size(); i++) {
						Medal medal = medals.get(i);

						String link = "";
						Bundle params = new Bundle();
						link = FacebookConstants.POST_MEDAL_EARNED_LINK
								.replace("#MedalName#", medal.name);

						String postURL = FacebookConstants.POST_ACTION_URL_MEDAL;
						try {
							if (!medal.displayName.equals("")) {
								postURL += "&og:title=" + medal.displayName;
							}
						} catch (Exception e) {
							postURL += "&og:title=" + medal.name;
						}
						postURL += "&og:url=" + link + "&og:image="
								+ webLocationOfImages + medal.imageLink;
						postURL += "&og:description="
								+ FacebookConstants.POST_MEDAL_EARNED_DESCRIPTION;
						postURL += "&body="
								+ FacebookConstants.POST_MEDAL_EARNED_DESCRIPTION;// medal.shortDescription;
						postURL += "&nexercise:times_bonus_achieved=1";

						EarnAction earnAction = GraphObject.Factory
								.create(EarnAction.class);
						earnAction.setProperty("fb:explicitly_shared", true);
						populateOGMedalAction(earnAction, postURL);
						Request request = new Request(
								Session.getActiveSession(),
								FacebookConstants.POST_ACTION_PATH_EARN,
								params, HttpMethod.POST);
						request.setGraphObject(earnAction);
						Response response = request.executeAndWait();
						onMedalPostActionResponse(response);
					}
				}
				publishProgress(99);
				return 0;
			}
			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
				progressBarHorizontal.setProgress(values[0]);
				//txtProgressInfo.setText(progressMessage);
			}
			@Override
			protected void onPostExecute(Integer result) {
				medalsPostedToFacebook = true;
				//stopMediaBrix();
				finish();
			}
		};

		task.execute();

	}

	private void postMedalsWonToTwitter() {

		ArrayList<Medal> medals = null;
		if(winning != null){
			medals = winning.medals;
		}

		if (medals != null) {

			for (int i = 0; i < medals.size(); i++) {

				Medal medal = medals.get(i);

				String userMessage = TwitterConstants.POST_MEDAL_TWEET;
				String imageUrl = FacebookConstants.POST_MEDAL_EARNED_LINK
						.replace("#MedalName#", medal.name);

				try {
					if (!medal.displayName.equals(""))
						userMessage = userMessage.replace("#MedalName#", "\""
								+ medal.displayName + "\"");
					userMessage = userMessage.replace("#MedalLink#", imageUrl);
				} catch (Exception e) {
					userMessage = userMessage.replace("#MedalName#", "\""
							+ medal.shortDescription + "\"");
					userMessage = userMessage.replace("#MedalLink#", imageUrl);
				}

				// Funcs.showShortToast(MessagesConstants.POST_SUBMITTING_TWITTER,
				// this);

				// boolean flag = true;

				ConfigurationBuilder cb = new ConfigurationBuilder();
				cb.setDebugEnabled(true)
				.setOAuthConsumerKey(TwitterConstants.CONSUMER_KEY)
				.setOAuthConsumerSecret(
						TwitterConstants.CONSUMER_SECRET_KEY)
						.setOAuthAccessToken(
								PreferenceHelper.getStringPreference(activity,
										TwitterConstants.PREF_NAME,
										TwitterConstants.PREF_KEY_ACCESS_TOKEN))
										.setOAuthAccessTokenSecret(
												PreferenceHelper
												.getStringPreference(
														activity,
														TwitterConstants.PREF_NAME,
														TwitterConstants.PREF_KEY_ACCESS_TOKEN_SECRET));
				;

				try {
					Twitter twitter = new TwitterFactory(cb.build())
					.getInstance();
					twitter.updateStatus(userMessage);
				} catch (Exception exception) {
					if (BuildConfig.DEBUG) {
						Log.e("Nexercise", "Exception thrown", exception);
					}
					// Funcs.showShortToast(exception.getMessage(), activity);
				}

			}
		}

	}

	private void postMedalsWonToSocialize() {

		ArrayList<Medal> medals = winning.medals;

		if (medals != null) {

			for (int i = 0; i < medals.size(); i++) {

				Medal medal = medals.get(i);

				String userMessage = SocializeConstants.POST_MEDAL_TWEET;
				String imageUrl = FacebookConstants.POST_MEDAL_EARNED_LINK
						.replace("#MedalName#", medal.name);

				try {
					if (!medal.displayName.equals(""))
						userMessage = userMessage.replace("#MedalName#", "\""
								+ medal.displayName + "\"");
					userMessage = userMessage.replace("#MedalLink#", imageUrl);
				} catch (Exception e) {
					userMessage = userMessage.replace("#MedalName#", "\""
							+ medal.shortDescription + "\"");
					userMessage = userMessage.replace("#MedalLink#", imageUrl);
				}

				try {
					Entity entity = new Entity(
							SocializeConstants.ENTITY_KEY_BASE
							+ "chat/activities", "Activity Feed");
					CommentOptions opts = CommentUtils
							.getUserCommentOptions(activity);
					opts.setSubscribeToUpdates(false);
					opts.setShowShareDialog(false);
					CommentUtils.addComment(activity, entity, userMessage,
							opts, new CommentAddListener() {
						public void onError(SocializeException e) {
						}

						public void onCreate(Comment comment) {
						}
					});
				} catch (Exception exception) {
					if (BuildConfig.DEBUG) {
						Log.e("Nexercise", "exception posting to socialize",
								exception);
					}
				}
			}
		}
	}

	private void hideMedalDetail() {

		layoutMedalDetail.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();
		switch (id) {

		case R.id.continueBtn:
			if (isNewFacebookPublishPermission) {
				finish();
			} else if (mCountyCode != null
					&& (mCountyCode.equals("US") || mCountyCode.equals("CA"))) {
				if (isInstantRewardsOn()) {
					progressBarHorizontal.setVisibility(View.VISIBLE);
					txtProgressInfo.setVisibility(View.VISIBLE);
					txtProgressInfo.setText("Checking Instant Reward partners for you…");
					
					progressBarHorizontal.setProgress(20);
					/*					Funcs.showShortToast(
							MessagesConstants.KIIP_PROGRESS_MESSAGE, activity);*/
					executeRewardsFlow(0);
				} else {/* User from us or Canada turened OFF Kiip rewards */
					continueDialogDismissal();
				}
			} else {
				continueDialogDismissal();
			}
			break;

		default:
			break;
		}

	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(isInstantRewardsOn()){
				if (msg.what == 0) {
					continueDialogDismissal();
				} else if (msg.what == 1) {
					UserPreferencesConstants.MEDIA_BRIX_BONUS = true;
					continueDialogDismissal();
				} else if (msg.what == 2) {
					//displayPaeDaeAdd();
					if(winning.rewards == null && isInstantRewardsOn()){
						if(isDismissWinningsViewIfNoReward){
							continueDialogDismissal();
						}
						else{
							isDismissWinningsViewIfNoReward = true;
						}
					}else{
						executeRewardsFlow(rewardExecutionIndex);
					}
				}
			}
		}
	};

	private boolean shouldPostToSocialNetworks() {
		return (shouldPostMedalsToFacebook || shouldPostMedalsToTwitter
				|| shouldPostMedalsToSocialize || shouldPostXPToFacebook
				|| shouldPostXPToTwitter || shouldPostXPToSocialize);
	}

	public void continueDialogDismissal() {
		progressBarHorizontal.setVisibility(View.GONE);
		txtProgressInfo.setVisibility(View.GONE);
		
		if (PreferenceHelper.getBooleanPreference(this,
				DisplayConstants.PREF_NAME, DisplayConstants.PREF_KEY_MPOINTS)) {
			if (activity != null) {
				if (nxr != null) {
					nxr.logAction("Nexercise");
					try {
						if (exerciseSession != null) {
							nxr.logAction(exerciseSession.exerciseActivity);
						}
					} catch (Exception e) {

					}
					if (winning != null) {
						ArrayList<Medal> medals = winning.medals;
						if (medals != null) {
							for (int i = 0; i < medals.size(); i++) {
								Medal medal = medals.get(i);
								try {
									if (medal.name != null) {
										nxr.logAction(medal.name);
									}
								} catch (Exception e) {

								}
							}
						}
						ArrayList<PointsEarned> pointsEarnedList = winning.pointsEarnedList;
						if (pointsEarnedList != null) {
							for (int i = 0; i < pointsEarnedList.size(); i++) {
								PointsEarned pointsEarned = pointsEarnedList
										.get(i);
								try {
									if (pointsEarned.displayName != null) {
										String medalString = pointsEarned.displayName
												.replaceAll("\\s+", "")
												.replace("&", "And");
										nxr.logAction(medalString);
									}
								} catch (Exception e) {

								}
							}
						}
					}
					nxr.presentActivity();
				}
			}
		}

		Map<String, String> flurryParams = new HashMap<String, String>();
		if (shouldPostToSocialNetworks()) {
			if (shouldPostMedalsToFacebook) {
				flurryParams.put("postMedalsToFB", "Yes");
			} else {
				flurryParams.put("postMedalsToFB", "No");
			}
			if (shouldPostMedalsToTwitter) {
				flurryParams.put("postMedalsToTwitter", "Yes");
			} else {
				flurryParams.put("postMedalsToTwitter", "No");
			}
			if (shouldPostMedalsToSocialize) {
				flurryParams.put("postMedalsToSocialize", "Yes");
			} else {
				flurryParams.put("postMedalsToSocialize", "No");
			}
			if (shouldPostXPToFacebook) {
				flurryParams.put("postToFB", "Yes");
			} else {
				flurryParams.put("postToFB", "No");
			}
			if (shouldPostXPToTwitter) {
				flurryParams.put("postToTwitter", "Yes");
			} else {
				flurryParams.put("postToTwitter", "No");
			}
			if (shouldPostXPToSocialize) {
				flurryParams.put("postToSocialize", "Yes");
			} else {
				flurryParams.put("postToSocialize", "No");
			}

		} else {
			flurryParams.put("postMedalsToFB", "No");
			flurryParams.put("postMedalsToTwitter", "No");
			flurryParams.put("postMedalsToSocialize", "No");
			flurryParams.put("postToFB", "No");
			flurryParams.put("postToTwitter", "No");
			flurryParams.put("postToSocialize", "No");
		}
		FlurryHelper.logEvent("V:PointsPopup", flurryParams);
		if (shouldPostToSocialNetworks()) {
			new SubmitSocialNetworkPostsAsynTask(this).execute();
		} else {
			//stopMediaBrix();
			finish();
		}
	}

	private void postToSocialNetworksIfSelected() {

		if (TwitterHelper.isLoggedIn())
			postToTwitterIfSelected();

		postToSocializeIfSelected();
	}

	private void postToFacebookIfSelected() {
		performPublish(PendingAction.POST_TO_FB);
	}

	private void postToTwitterIfSelected() {

		if (shouldPostXPToTwitter) {
			postXPEarnedToTwitter(winning.isSelfReported);
			if (PreferenceHelper.getBooleanPreference(this,
					DisplayConstants.PREF_NAME,
					DisplayConstants.PREF_KEY_MPOINTS)) {
				if (activity != null) {
					if (nxr != null) {
						nxr.postEvent("TwitterPost");
					}
				}
			}

		}
		if (shouldPostMedalsToTwitter) {
			postMedalsWonToTwitter();
		}

	}

	private void postToSocializeIfSelected() {

		// FIXME: once socialize fixes their Handler issue, this should not be
		// posted to the main thread
		btnPostXPToSocialize.post(new Runnable() {
			public void run() {
				if (shouldPostXPToSocialize) {
					postXPEarnedToSocialize(winning.isSelfReported);
				}
				if (shouldPostMedalsToSocialize) {
					postMedalsWonToSocialize();
				}
			}
		});
	}

	private class SubmitSocialNetworkPostsAsynTask extends
	AsyncTask<Void, Integer, Void> {

		Context context;
		String progressMessage = "";

		public SubmitSocialNetworkPostsAsynTask(Context context) {

			this.context = context;

			try {
				progressBarHorizontal.setVisibility(View.VISIBLE);
				//txtProgressInfo.setVisibility(View.VISIBLE);
			} catch (Exception e) {
				// TODO: handle exception
				finish();
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			progressMessage = MessagesConstants.WINNINGS_VIEW_SUBMITTING_POSTS;
			publishProgress(1);
			postToSocialNetworksIfSelected();
			//progressMessage = MessagesConstants.WINNINGS_VIEW_SUBMITTING_POSTS;
			if (shouldPostXPToFacebook || shouldPostMedalsToFacebook) {
				publishProgress(50);
			}else{
				publishProgress(99);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			progressBarHorizontal.setProgress(values[0]);
			//txtProgressInfo.setText(progressMessage);

		}

		@Override
		protected void onPostExecute(Void param) {
			if (shouldPostXPToFacebook || shouldPostMedalsToFacebook) {
				if (FacebookHelper.isLoggedIn(activity)) {
					postToFacebookIfSelected();
				}
			} else {
				//stopMediaBrix();
				finish();
			}
			try {

			} catch (Exception e) {

			}
		}

	}

	@Override
	public void onBackPressed() {
		if (isMedalDetailVisible()) {
			hideMedalDetail();
		}

		if (isNewFacebookPublishPermission) { /*
		 * if user has no fb publish
		 * permission and share to fb set on
		 * settings
		 */
			//stopMediaBrix();
			finish();
		} else if (shouldPostToSocialNetworks()) {
			new SubmitSocialNetworkPostsAsynTask(this).execute();
		} else {
			//stopMediaBrix();
			finish();
		}
		return;
	}

	public me.kiip.sdk.Kiip.Callback mKiipRequestListener = new me.kiip.sdk.Kiip.Callback() {

		@Override
		public void onFailed(me.kiip.sdk.Kiip arg0, Exception arg1) {			
			
				executeRewardsFlow(rewardExecutionIndex);
		}

		@Override
		public void onFinished(me.kiip.sdk.Kiip kiipInstance, Poptart poptart) {
			kiipInstance.setOnContentListener(new OnContentListener() {

				@Override
				public void onContent(Kiip arg0, String arg1, int arg2, String arg3,
						String arg4) {
					isKiipRedeemed = true;
					/* *** Flurry changes *** */
					Map<String, String> flurryParams = new HashMap<String, String>();
					flurryParams.put("vendor", "Kp");
					FlurryHelper.logEvent("A:Rewards.Unit.Confirmed", flurryParams);
					/* *** Flurry changes ends *** */
				}
			});

			// TODO Auto-generated method stub
			if (poptart != null) {
				/* *** Flurry changes *** */
				Map<String, String> flurryParamsKp = new HashMap<String, String>();
				flurryParamsKp.put("vendor", "Kp");
				FlurryHelper.logEvent("A:Rewards.Unit.Available", flurryParamsKp);
				/* *** Flurry changes ends *** */
				poptart.show(activity);
				poptart.setOnShowListener(new android.content.DialogInterface.OnShowListener() {

					@Override
					public void onShow(DialogInterface dialog) {
						// TODO Auto-generated method stub
						/* *** Flurry changes *** */
						Map<String, String> flurryParamsKpShow = new HashMap<String, String>();
						flurryParamsKpShow.put("vendor", "Kp");
						FlurryHelper.logEvent("A:Rewards.Unit.Presented", flurryParamsKpShow);
						/* *** Flurry changes ends *** */
					}
				});				

				poptart.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						// TODO Auto-generated method stub
						/* *** Flurry changes *** */
						Map<String, String> flurryParamsKp = new HashMap<String, String>();
						flurryParamsKp.put("vendor", "Kp");
						FlurryHelper.logEvent("A:Rewards.Unit.Dismissed", flurryParamsKp);
						/* *** Flurry changes ends *** */

						if(!isKiipRedeemed){
							/* *** Flurry changes *** */
							Map<String, String> flurryParams = new HashMap<String, String>();
							flurryParams.put("vendor", "Kp");
							FlurryHelper.logEvent("A:Rewards.Unit.Rejected", flurryParams);
							/* *** Flurry changes ends *** */
						}
						handler.sendEmptyMessage(0);

						
					}

				});
			} else {
				/* *** Flurry changes *** */
				Map<String, String> flurryParamsKp = new HashMap<String, String>();
				flurryParamsKp.put("vendor", "Kp");
				FlurryHelper.logEvent("A:Rewards.Unit.Unavailable", flurryParamsKp);
				/* *** Flurry changes ends *** */
				executeRewardsFlow(rewardExecutionIndex);
				
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		uiHelper.onResume();
		MediabrixAPI.getInstance().onResume(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);

		outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == AD_ACTIVITY) {
			PaeDaeRequested++;
			if (PaeDaeRequested > 1) {
				handler.sendEmptyMessage(0);
			}
		} else {
			uiHelper.onActivityResult(requestCode, resultCode, data);
		}
	}





	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		FlurryHelper.endSession(this);
	}

	private String getUserEmailId() {
		String kiipDefaultEmail = userInfo.preferredEmail;
		String userEmailAddress = "";
		try {
			if (kiipDefaultEmail.equals("fbEmailAddress"))
				userEmailAddress = userInfo.fbEmailAddress;
			else
				userEmailAddress = userInfo.emailAddress;
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
		return userEmailAddress;
	}

	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (pendingAction != PendingAction.NONE
				&& (exception instanceof FacebookOperationCanceledException || exception instanceof FacebookAuthorizationException)) {
			pendingAction = PendingAction.NONE;
		} else if (state == SessionState.OPENED_TOKEN_UPDATED) {
			handlePendingAction();
		}
	}

	private void handlePendingAction() {
		PendingAction previouslyPendingAction = pendingAction;
		// These actions may re-set pendingAction if they are still pending, but
		// we assume they
		// will succeed.
		pendingAction = PendingAction.NONE;
		if (previouslyPendingAction.equals(PendingAction.POST_TO_FB)) {
			if (shouldPostXPToFacebook) {
				postXPEarnedToFacebook(winning.isSelfReported);
				if (PreferenceHelper.getBooleanPreference(this,
						DisplayConstants.PREF_NAME,
						DisplayConstants.PREF_KEY_MPOINTS)) {
					if (activity != null) {
						if (nxr != null) {
							nxr.postEvent("FacebookPost");
						}
					}
				}
			} else {
				if (shouldPostMedalsToFacebook) {
					postMedalsWonToFacebook();
				}
			}
		}
	}

	private boolean hasPublishPermission() {
		Session session = Session.getActiveSession();
		return session != null
				&& session.getPermissions().contains("publish_actions");
	}

	private void performPublish(PendingAction action) {
		Session session = Session.getActiveSession();
		if (session != null) {
			pendingAction = action;
			if (hasPublishPermission()) {
				// We can do the action right away.
				handlePendingAction();
			} else {
				try {
					// We need to get new permissions, then complete the action
					// when we get called back.
					session.requestNewPublishPermissions(new Session.NewPermissionsRequest(
							this, PERMISSIONS));
					isNewFacebookPublishPermission = true;
				} catch (Exception e) {
					e.printStackTrace();
					//stopMediaBrix();
					finish();
				}
			}
		}
	}

	public void subscribeEntity(final Entity entity) {
		SubscriptionUtils.subscribe(activity, entity,
				SubscriptionType.NEW_COMMENTS,
				new SubscriptionResultListener() {

			@Override
			public void onError(SocializeException error) {
				// TODO Auto-generated method stub
				subscribeEntity(entity);
			}

			@Override
			public void onCreate(Subscription result) {
				// TODO Auto-generated method stub
			}
		});
	}

	private boolean isInstantRewardsOn(){
		if (PreferenceHelper.getBooleanPreference(this,
				DisplayConstants.PREF_NAME,
				DisplayConstants.PREF_KEY_KIIP_REWARDS)) {
			return true;
		}else{
			return false;
		}
	}



	public void executeRewardsFlow(int rewardKeyIndex){
		int rewardKey = 0;
		if(rewardKeys.size() > 0){
			try{
				rewardKey = rewardKeys.get(rewardKeyIndex);
			}catch(IndexOutOfBoundsException e){
				rewardKey = 0;
			}
			switch (rewardKey) {
			case RewardConstants.KIIP_KEY:	
				/* *** Flurry changes *** */
				Map<String, String> flurryParamsKp = new HashMap<String, String>();
				flurryParamsKp.put("vendor", "Kp");
				FlurryHelper.logEvent("A:Rewards.Unit.Requested", flurryParamsKp);
				/* *** Flurry changes ends *** */
				String userEmailAddress = getUserEmailId();
				KiipHelper.INSTANCE.submitScoreToLeaderBoard(activity,
						userEmailAddress, KiipConstants.MOMENT_NEXERCISE,
						100.00, mKiipRequestListener);	
				progressBarHorizontal.setVisibility(View.GONE);
				txtProgressInfo.setVisibility(View.GONE);
				rewardExecutionIndex++;
				break;
			case RewardConstants.MEDIABRIX_KEY:
				doShowMediabrixAd = true;
				rewardExecutionIndex++;

				if(isMediabrixReadyToShow){
					doShowMediabrixAd = false;
					showMediabrixAd();
				}
				else if(isMediaBrixUnAvailable){
					rewardExecutionIndex++;
					executeRewardsFlow(rewardExecutionIndex);
				}
				//else{
				//	rewardExecutionIndex++;
				//	executeRewardsFlow(rewardExecutionIndex);
				//}
				break;
				
			default:
				rewardExecutionIndex=0;
				continueDialogDismissal();
				break;
			}
		}
		else{
			rewardExecutionIndex=0;
			continueDialogDismissal();
		}
	}

	//Mediabrix Activity Life Cycle Events

	@Override
	protected void onPause() {
		MediabrixAPI.getInstance().onPause(this);
		uiHelper.onPause();
		super.onPause();
	}


	@Override
	public void onDestroy() {
		MediabrixAPI.getInstance().onDestroy(this);
		uiHelper.onDestroy();
		super.onDestroy();
	}

	private HashMap<String, String> createRewardsMbrixVars() {
		HashMap<String, String> vars = new HashMap<String, String>();
		String MBBonus = ((NexerciseApplication) activity
				.getApplication()).getDataLayerInstance()
				.getAppSettings().MediaBrixBonus;
		if(MBBonus != null){
			vars.put("rewardText", MBBonus+" XP");
		}
		else{
			vars.put("rewardText", "Nexercise Reward");
		}
		return vars;
	}

	//MediaBrix IAdEventsListener Callbacks

	@Override
	public void onStarted(String status) {

		HashMap<String, String> vars = createRewardsMbrixVars();
		MediabrixAPI.getInstance().load(WinningsViewActivity.this, MediaBrixConstants.ADD_TYPE_REWARD, vars);
		/* *** Flurry changes *** */
		Map<String, String> flurryParams = new HashMap<String, String>();
		flurryParams.put("vendor", "Mb");
		FlurryHelper.logEvent("A:Rewards.Unit.Requested", flurryParams);
		/* *** Flurry changes ends *** */
		progressBarHorizontal.setProgress(60);
	}

	@Override
	public void onAdReady(String target) {

		if (MediaBrixConstants.ADD_TYPE_REWARD.equals(target)) {
			isMediabrixReadyToShow =true;
			/* *** Flurry changes *** */
			Map<String, String> flurryParams = new HashMap<String, String>();
			flurryParams.put("vendor", "Mb");
			FlurryHelper.logEvent("A:Rewards.Unit.Available", flurryParams);
			/* *** Flurry changes ends *** */
			progressBarHorizontal.setProgress(99);
			if(doShowMediabrixAd){
				showMediabrixAd();
			}
		}
	}

	@Override
	public void onAdRewardConfirmation(String target) {

		isMediabrixRewardReceived = true;
		/* *** Flurry changes *** */
		Map<String, String> flurryParams = new HashMap<String, String>();
		flurryParams.put("vendor", "Mb");
		FlurryHelper.logEvent("A:Rewards.Unit.Confirmed", flurryParams);
		/* *** Flurry changes ends *** */
	}

	@Override
	public void onAdClosed(String target) {

		if (isMediabrixRewardReceived){
			handler.sendEmptyMessage(1);
		}else{
			handler.sendEmptyMessage(0);
			/* *** Flurry changes *** */
			Map<String, String> flurryParams = new HashMap<String, String>();
			flurryParams.put("vendor", "Mb");
			FlurryHelper.logEvent("A:Rewards.Unit.Rejected", flurryParams);
			/* *** Flurry changes ends *** */
		}
		/* *** Flurry changes *** */
		Map<String, String> flurryParams = new HashMap<String, String>();
		flurryParams.put("vendor", "Mb");
		FlurryHelper.logEvent("A:Rewards.Unit.Dismissed", flurryParams);
		/* *** Flurry changes ends *** */
	}

	@Override
	public void onAdUnavailable(String target) {	
		/* *** Flurry changes *** */
		Map<String, String> flurryParams = new HashMap<String, String>();
		flurryParams.put("vendor", "Mb");
		FlurryHelper.logEvent("A:Rewards.Unit.Unavailable", flurryParams);
		/* *** Flurry changes ends *** */
		isMediaBrixUnAvailable = true;
		if(doShowMediabrixAd){
			executeRewardsFlow(rewardExecutionIndex);
		}
	}
	
	private void showMediabrixAd(){
		progressBarHorizontal.setVisibility(View.GONE);
		txtProgressInfo.setVisibility(View.GONE);
		
		MediabrixAPI.getInstance().show(WinningsViewActivity.this, MediaBrixConstants.ADD_TYPE_REWARD);
		/* *** Flurry changes *** */
		Map<String, String> flurryParams = new HashMap<String, String>();
		flurryParams.put("vendor", "Mb");
		FlurryHelper.logEvent("A:Rewards.Unit.Presented", flurryParams);
		/* *** Flurry changes ends *** */
	}
}
