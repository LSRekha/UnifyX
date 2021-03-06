package org.edx.mobile.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.inject.Inject;

import org.edx.mobile.BuildConfig;
import org.edx.mobile.R;
import org.edx.mobile.base.BaseFragment;
import org.edx.mobile.base.BaseFragmentActivity;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.event.AccountDataLoadedEvent;
import org.edx.mobile.event.ProfilePhotoUpdatedEvent;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.api.ProfileModel;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.module.facebook.IUiLifecycleHelper;
import org.edx.mobile.module.prefs.PrefManager;
import org.edx.mobile.profiles.UserProfileActivity;
import org.edx.mobile.user.Account;
import org.edx.mobile.user.GetAccountTask;
import org.edx.mobile.user.ProfileImage;
import org.edx.mobile.util.Config;
import org.edx.mobile.util.EmailUtil;
import org.edx.mobile.view.dialog.IDialogCallback;
import org.edx.mobile.view.dialog.NetworkCheckDialogFragment;
import org.edx.mobile.view.my_videos.MyVideosActivity;
import de.greenrobot.event.EventBus;


public class NavigationFragment extends BaseFragment {

    private static final String TAG = "NavigationFragment";

    @Inject
    IEdxEnvironment environment;

    @Inject
    Config config;

    private PrefManager pref;
    private final Logger logger = new Logger(getClass().getName());
    private NetworkCheckDialogFragment newFragment;

    private IUiLifecycleHelper uiLifecycleHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        }
    };

    @Nullable
    private GetAccountTask getAccountTask;

    @Nullable
    private ProfileImage profileImage;

    ProfileModel profile;

    @Nullable
    ImageView imageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiLifecycleHelper = IUiLifecycleHelper.Factory.getInstance(getActivity(), callback);
    uiLifecycleHelper.onCreate(savedInstanceState);
        Context context = getActivity().getBaseContext();
        pref = new PrefManager(context, PrefManager.Pref.LOGIN);
        profile = pref.getCurrentUserProfile();
        if (config.isUserProfilesEnabled() && profile != null && profile.username != null) {
            getAccountTask = new GetAccountTask(getActivity(), profile.username);
            getAccountTask.setTaskProcessCallback(null); // Disable global loading indicator
            getAccountTask.execute();
        }
        EventBus.getDefault().register(this);
    }

    private void loadProfileImage(@NonNull ProfileImage profileImage, @NonNull ImageView imageView) {
        if (profileImage.hasImage()) {
            Glide.with(NavigationFragment.this)
                    .load(profileImage.getImageUrlLarge())
                    .into(imageView);
        } else {
            Glide.with(NavigationFragment.this)
                    .load(R.drawable.xsie)
                    .into(imageView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.drawer_navigation, container, false);
        final TextView name_tv = (TextView) layout.findViewById(R.id.name_tv);
        final TextView email_tv = (TextView) layout.findViewById(R.id.email_tv);
        final FrameLayout nameLayout = (FrameLayout) layout.findViewById(R.id.name_layout);
       imageView = (ImageView) layout.findViewById(R.id.profile_image);
//        if (config.isUserProfilesEnabled()) {
//            if (null != profileImage) {
//                loadProfileImage(profileImage, imageView);
//            }
            if (profile != null && profile.username != null) {
                nameLayout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final BaseFragmentActivity act = (BaseFragmentActivity) getActivity();
                        act.closeDrawer();

                        if (!(act instanceof UserProfileActivity)) {
                            environment.getRouter().showUserProfileWithNavigationDrawer(getActivity(), profile.username);

                            if (!(act instanceof MyCoursesListActivity)) {
                                act.finish();
                            }
                        }
                    }
                });
            }
//        } else {
//            imageView.setVisibility(View.GONE);

            // Disable any on-tap effects
            nameLayout.setClickable(false);
            nameLayout.setForeground(null);
      //  }

        final TextView tvMyCourses = (TextView) layout.findViewById(R.id.drawer_option_my_courses);
        tvMyCourses.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                tvMyCourses.setOnTouchListener(new CustomTouchListener());
                Activity act = getActivity();
                ((BaseFragmentActivity) act).closeDrawer();

                if (!(act instanceof MyCoursesListActivity)) {
                    environment.getRouter().showMyCourses(act);
                    act.finish();
                }
            }
        });

        final TextView tvMyVideos = (TextView) layout.findViewById(R.id.drawer_option_my_videos);
        tvMyVideos.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tvMyVideos.setOnTouchListener(new CustomTouchListener());
                Activity act = getActivity();
                ((BaseFragmentActivity) act).closeDrawer();

                if (!(act instanceof MyVideosActivity)) {
                    environment.getRouter().showMyVideos(act);
                    //Finish need not be called if the current activity is MyCourseListing
                    // as on returning back from FindCourses,
                    // the student should be returned to the MyCourses screen
                    if (!(act instanceof MyCoursesListActivity)) {
                        act.finish();
                    }
                }
            }
        });


        final TextView tvFindCourses = (TextView) layout.findViewById(R.id.drawer_option_find_courses);
        tvFindCourses.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tvFindCourses.setOnTouchListener(new CustomTouchListener());
                ISegment segIO = environment.getSegment();
                segIO.trackUserFindsCourses();
                FragmentActivity act = getActivity();
                ((BaseFragmentActivity) act).closeDrawer();
                if (!(act instanceof WebViewFindCoursesActivity || act instanceof NativeFindCoursesActivity)) {
                    environment.getRouter().showFindCourses(act);

                    //Finish need not be called if the current activity is MyCourseListing
                    // as on returning back from FindCourses,
                    // the student should be returned to the MyCourses screen
                    if (!(act instanceof MyCoursesListActivity)) {
                        act.finish();
                    }
                }
            }
        });

        final TextView tvSettings = (TextView) layout.findViewById(R.id.drawer_option_my_settings);
        tvSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvSettings.setOnTouchListener(new CustomTouchListener());
                Activity act = getActivity();
                ((BaseFragmentActivity) act).closeDrawer();

                if (!(act instanceof SettingsActivity)) {
                    environment.getRouter().showSettings(act);

                    if (!(act instanceof MyCoursesListActivity)) {
                        act.finish();
                    }
                }
            }
        });

//        TextView tvFacebook = (TextView) layout.findViewById(R.id.drawer_option_facebook);
//        tvFacebook.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
                // Activity act = getActivity();
                //((BaseFragmentActivity) act).closeDrawer();


//                if (!(act instanceof FacebookActivity)) {
//                    environment.getRouter().showSettings(act);
//
//                    if (!(act instanceof MyCoursesListActivity)) {
//                        act.finish();
//                    }
//                }
                //Intent intent = new Intent(Intent.ACTION_MAIN);
                //intent.setComponent(new ComponentName("org.edx.mobile","com.facebook.katana.MainActivity"));
               // startActivity(intent);
//                Intent i = new Intent(android.content.Intent.ACTION_VIEW);
//                i.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.facebook.katana"));
//                startActivity(i);
//            }
//        });

        final TextView tvSubmitFeedback = (TextView) layout.findViewById(R.id.drawer_option_submit_feedback);
        tvSubmitFeedback.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String to = environment.getConfig().getFeedbackEmailAddress();
                String subject = getString(R.string.email_subject);

                String osVersionText = String.format("%s %s", getString(R.string.android_os_version), android.os.Build.VERSION.RELEASE);
                String appVersionText = String.format("%s %s", getString(R.string.app_version), BuildConfig.VERSION_NAME);
                String deviceModelText = String.format("%s %s", getString(R.string.android_device_model), Build.MODEL);
                String feedbackText = getString(R.string.insert_feedback);
                String body = osVersionText + "\n" + appVersionText + "\n" + deviceModelText + "\n\n" + feedbackText;
                EmailUtil.openEmailClient(getActivity(), to, subject, body, environment.getConfig());
                tvSubmitFeedback.setOnTouchListener(new CustomTouchListener());
            }
        });


        if (profile != null) {
            if (profile.name != null) {
                name_tv.setText(profile.name);
            }
            if (profile.email != null) {
                email_tv.setText(profile.email);
            }
        }
        final TextView logout_tv=(TextView)layout.findViewById(R.id.drawer_option_logout);
        //Button logout_btn = (Button) layout.findViewById(R.id.logout_button);
        logout_tv.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                environment.getRouter().forceLogout(getActivity(), environment.getSegment(), environment.getNotificationDelegate());
                logout_tv.setOnTouchListener(new CustomTouchListener());
            }
        });

        ((TextView) layout.findViewById(R.id.tv_version_no)).setText(String.format("%s %s %s",
                getString(R.string.label_version), BuildConfig.VERSION_NAME, environment.getConfig().getEnvironmentDisplayName()));

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        uiLifecycleHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiLifecycleHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiLifecycleHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiLifecycleHelper.onDestroy();
        if (null != getAccountTask) {
            getAccountTask.cancel(true);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
      //  imageView = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        uiLifecycleHelper.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiLifecycleHelper.onSaveInstanceState(outState);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void updateWifiSwitch(View layout) {
        final PrefManager wifiPrefManager = new PrefManager(
                getActivity().getBaseContext(), PrefManager.Pref.WIFI);
        Switch wifi_switch = (Switch) layout.findViewById(R.id.wifi_setting);

        wifi_switch.setOnCheckedChangeListener(null);
        wifi_switch.setChecked(wifiPrefManager.getBoolean(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, true));
        wifi_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    wifiPrefManager.put(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, true);
                } else {
                    showWifiDialog();
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(@NonNull ProfilePhotoUpdatedEvent event) {
        if (event.getUsername().equalsIgnoreCase(profile.username)) {
            if (null == event.getUri()) {
                Glide.with(NavigationFragment.this)
                        .load(R.drawable.xsie)
                        .into(imageView);
            } else {
                Glide.with(NavigationFragment.this)
                        .load(event.getUri())
                        .skipMemoryCache(true) // URI is re-used in subsequent events; disable caching
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(imageView);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(@NonNull AccountDataLoadedEvent event) {
        final Account account = event.getAccount();
        if (account.getUsername().equalsIgnoreCase(profile.username)) {
            profileImage = account.getProfileImage();
            if (imageView != null) {
                loadProfileImage(account.getProfileImage(), imageView);
            }
        }
    }

    protected void showWifiDialog() {
        newFragment = NetworkCheckDialogFragment.newInstance(getString(R.string.wifi_dialog_title_help), getString(R.string.wifi_dialog_message_help), new IDialogCallback() {
            @Override
            public void onPositiveClicked() {
                try {
                    PrefManager wifiPrefManager = new PrefManager
                            (getActivity().getBaseContext(), PrefManager.Pref.WIFI);
                    wifiPrefManager.put(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, false);
                    updateWifiSwitch(getView());
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }

            @Override
            public void onNegativeClicked() {
                try {
                    PrefManager wifiPrefManager = new PrefManager(
                            getActivity().getBaseContext(), PrefManager.Pref.WIFI);

                    wifiPrefManager.put(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, true);
                    wifiPrefManager.put(PrefManager.Key.DOWNLOAD_OFF_WIFI_SHOW_DIALOG_FLAG, true);

                    updateWifiSwitch(getView());
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        });
        newFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        newFragment.show(getFragmentManager(), "dialog");
        newFragment.setCancelable(false);
    }
    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap circuleBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getWidth(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(circuleBitmap);

        final int color = Color.BLUE;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getWidth());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return circuleBitmap;
    }
    public class CustomTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch(motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    ((TextView)view).setTextColor(0xff4285f4); //white
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    ((TextView)view).setTextColor(0xFF000000); //black
                    break;
            }
            return false;
        }
    }

}
