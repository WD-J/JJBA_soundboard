package soundboys.wm.jojosbizarreadventuresoundboard_goldenwindpart1_4;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;

import jp.alessandro.android.iab.BillingApi;
import jp.alessandro.android.iab.BillingContext;
import jp.alessandro.android.iab.BillingException;
import jp.alessandro.android.iab.BillingProcessor;
import jp.alessandro.android.iab.PurchaseType;
import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.logger.SystemLogger;
import jp.alessandro.android.iab.response.PurchaseResponse;
import ru.noties.sbv.ScrollingBackgroundView;

public class SoundboardActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<SoundObject>> {
    // Define a tag that is used to log any kind of error or comment
    private static final String LOG_TAG = SoundboardActivity.class.getSimpleName();

    private final PurchaseHandler mPurchaseHandler = new PurchaseHandler() {
        @Override
        public void call(PurchaseResponse response) {
            if (response.isSuccess()) {
                Intent mailIntent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:?subject=" + "Sound Request"+ "&body=" + "I want to add the sound:  " + "&to=" + "dcdude120@gmail.com");
                mailIntent.setData(data);
                startActivity(Intent.createChooser(mailIntent, "Send mail..."));
                // Do your stuff with the purchased item
            } else {
                // Handle the error
            }
        }
    };

    private BillingProcessor mBillingProcessor;

    // Declare a mToolbar to use instead of the system standard mToolbar
    private Toolbar mToolbar;

    // Declare an ArrayList that you fill with SoundObjects
    private ArrayList<SoundObject> mSoundList = new ArrayList<>();

    // Declare a RecyclerView and its components
    // You can assign the RecyclerView.Adapter right away
    private RecyclerView mRecyclerView;
    private SoundboardRecyclerAdapter mRecyclerAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    // Declare a View that will contain the layout of the activity and serves as the parent of a Snackbar
    private View mLayout;

    // Declare a DatabaseHandler to support database usage
    private DatabaseHandler mDatabaseHandler;

    private AdView mAdView;

    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soundboard);

        initBillingProcessor();

        // Application context
        Context context = getApplicationContext();

// Public key generated on the Google Play Console
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxHqWmq5whKrJyH+W//DynCpoBryjcmQTsqjZoWFoWgowb2FLhFGiw+u57FFeUHznrUORnFI8Hf7MWUDZlJy8y0hnX3wpZKop4G0DkPWhLAx3y8VR2GoGajc4AAdfe2mNXcB4asinIaEc6Fbw+1LajW+j4JuRpsVJUbWVBgCHYfsILIdEWPyaJZ7frHnNJHQ+ZPJENkDm8hSWH6nBBwfoigrA1U1wTWEsacuoKrwYgeV9HQATr0uDUW4QMnlmi3XDp4Lw+Hs0jBAkovjaYV2S7dkMB59e+4T9Cz8TAnA4jfi8obtZc+6DZy03ulveuIst3yhcgInhYzQIo0SLXn6sHQIDAQAB";

        BillingContext.Builder builder = new BillingContext.Builder()
                .setContext(getApplicationContext()) // App context
                .setPublicKeyBase64(base64EncodedPublicKey) // Public key generated on the Google Play Console
                .setApiVersion(BillingApi.VERSION_3) // It also supports version 5
                .setLogger(new SystemLogger()); // This is optional

        builder.build();

        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-4612423874107732/8564370325");
       // MobileAds.initialize(this, "ca-app-pub-4612423874107732~8279808609");

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-4612423874107732/3663067863");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        // Assign an instance to the DatabaseHandler
        mDatabaseHandler = DatabaseHandler.getInstance(this);

        // If the gets an update or runs for the first time fill the database with all SoundObjects
        if (appUpdate()) {

            mDatabaseHandler.createSoundCollection();

            mDatabaseHandler.updateFavorites();
        }

        // Assign layout view
        // Take a look at activity_soundboard.xml change the id
        mLayout = findViewById(R.id.activity_soundboard);

        // Assign mToolbar to the Toolbar item declared in activity_soundboard.xml
        mToolbar = (Toolbar) findViewById(R.id.soundboard_toolbar);
        mToolbar.setTitle("");

        // Set mToolbar as new action bar
        setSupportActionBar(mToolbar);

        // Assign mRecyclerView to the RecyclerView item declared in activity_soundboard.xml
        mRecyclerView = (RecyclerView) findViewById(R.id.soundboardRecyclerView);

        // Define the RecyclerView.LayoutManager to have 3 columns
        mLayoutManager = new GridLayoutManager(this, 4);

        // Set the RecyclerView.LayoutManager
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Initialize recycler adapter
        mRecyclerAdapter = new SoundboardRecyclerAdapter(this, mSoundList);

        // Set the RecyclerView.Adapter
        mRecyclerView.setAdapter(mRecyclerAdapter);

        final ScrollingBackgroundView scrollingBackgroundView = findViewById(R.id.scrolling_background_view);

        scrollingBackgroundView.setOnSizeChangedListener(new ScrollingBackgroundView.OnSizeChangedListener() {
            @Override
            public void onSizeChanged(int width, int height) {
            }
        });

        final RecyclerView recyclerView = findViewById(R.id.soundboardRecyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                scrollingBackgroundView.scrollBy(dx, dy/8);
            }
        });

        // actual scrolling methods
        scrollingBackgroundView.scrollBy(0, 0);
        scrollingBackgroundView.scrollTo(155, 0);

        scrollingBackgroundView.scrollX(); // use this method to retrieve current scroll x value
        scrollingBackgroundView.scrollY(); // use this method to retrieve current scroll y value

        // Calls a method that handles all permission events
        requestPermissions();

        // Calls a method that adds data from a database to the mSoundList
        getSupportLoaderManager().initLoader(R.id.common_soundlist_loader_id, null, this);
    }

    private void initBillingProcessor() {
        // Your public key
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxHqWmq5whKrJyH+W//DynCpoBryjcmQTsqjZoWFoWgowb2FLhFGiw+u57FFeUHznrUORnFI8Hf7MWUDZlJy8y0hnX3wpZKop4G0DkPWhLAx3y8VR2GoGajc4AAdfe2mNXcB4asinIaEc6Fbw+1LajW+j4JuRpsVJUbWVBgCHYfsILIdEWPyaJZ7frHnNJHQ+ZPJENkDm8hSWH6nBBwfoigrA1U1wTWEsacuoKrwYgeV9HQATr0uDUW4QMnlmi3XDp4Lw+Hs0jBAkovjaYV2S7dkMB59e+4T9Cz8TAnA4jfi8obtZc+6DZy03ulveuIst3yhcgInhYzQIo0SLXn6sHQIDAQAB";

        BillingContext.Builder builder = new BillingContext.Builder()
                .setContext(getApplicationContext()) // App context
                .setPublicKeyBase64(base64EncodedPublicKey) // Public key generated on the Google Play Console
                .setApiVersion(BillingApi.VERSION_3); // This is optional

        mBillingProcessor = new BillingProcessor(builder.build(), mPurchaseHandler);
    }

    public void b1(View v)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/jjbapp"));
        startActivity(browserIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBillingProcessor.onActivityResult(requestCode, resultCode, data)) {
            // The response will be sent through PurchaseHandler
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Create an options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the layout
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // Define a SearchView to provide a search function and define its behaviour
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                /*
                 * Searches for sound objects that begin with the
                 * entered string and displays them on submit of the search.
                 */
                mRecyclerAdapter.queryData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {

                getSupportLoaderManager()
                        .restartLoader(R.id.common_soundlist_loader_id,
                                null,
                                SoundboardActivity.this);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

      /*  if (item.getItemId() == R.id.pause_sound) {
            EventHandlerClass.releaseMediaPlayer();
        } */

            if (item.getItemId() == R.id.buy_sound) {

            Activity activity = this;
            int requestCode = 10; // YOUR REQUEST CODE
            String itemId = "addsound";
            PurchaseType purchaseType = PurchaseType.IN_APP; // or PurchaseType.SUBSCRIPTION for subscriptions
            String developerPayload = "Buy and request a sound to be added in the next update of the app!";

            mBillingProcessor.startPurchase(activity, requestCode, itemId, purchaseType, developerPayload,
                    new StartActivityHandler() {
                        @Override
                        public void onSuccess() {
                            // Billing activity started successfully
                            // Do nothing
                        }

                        @Override
                        public void onError(BillingException e) {
                            // Handle the error
                        }
                    });

            mBillingProcessor.consumePurchase(itemId, new ConsumeItemHandler() {
                @Override
                public void onSuccess() {
                    // Item was consumed successfully
                }

                @Override
                public void onError(BillingException e) {
                    // Handle the error
                }
            });
            }

        if (item.getItemId() == R.id.action_favorite_show) {

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    // Load the next interstitial.
                    mInterstitialAd.loadAd(new AdRequest.Builder().build());
                }
            });

            this.startActivity(new Intent(this, FavoriteActivity.class));
            }

        switch (item.getItemId()) {

            case R.id.action_favorite:
                startActivity(new Intent(this, FavoriteActivity.class));
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    // Takes care of some things when the user closes the activity
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Calls a method that releases all data from the used MediaPlayer instance
        EventHandlerClass.releaseMediaPlayer();
    }

    /**
     * Handles all permission events on startup.
     */
    private void requestPermissions() {

        // Check if the users Android version is equal to or higher than Android 6 (Marshmallow)
        // Since Android 6 you have to request permissions at runtime to provide a better security
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Check if the permission to write and read the users external storage is not granted
            // You need this permission if you want to share sounds via WhatsApp or the like
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // If the permission is not granted request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        0);
            }

            // Check if the permission to write the users settings is not granted
            // You need this permission to set a sound as ringtone or the like
            if (!Settings.System.canWrite(this)) {

                Snackbar.make(mLayout, "Enable this to share & set a sound as your ringtone/notification sound.", Snackbar.LENGTH_LONG)
                        .setCallback(new Snackbar.Callback() {
                            @Override
                            public void onShown(Snackbar snackbar) {
                                super.onShown(snackbar);
                            }

                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                super.onDismissed(snackbar, event);
                                if (event != DISMISS_EVENT_TIMEOUT) {
                                }
                            }
                        })
                        .setAction("OK",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Context context = v.getContext();
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                }).show();
            }
        }
    }

    /**
     * Checks if the app has been updated by looking at the versionCode defined in the modules build.gradle.
     *
     * @return True if the app has been updated.
     */
    private boolean appUpdate() {



        /*
         * We are saving the current app version into a preference file.
         * There are two ways to get a handle to a SharedPreferences,
         * we are creating a unique preference file that is not bound to a context.
         * Check the android developer documentation if you want to find out more.
         */

        // Define a name for the preference file and a key name to save the version code to it
        final String prefsName = "VersionPref";
        final String prefVersionCodeKey = "version_code";
        // Define a value that is set if the key does not exist
        final int doesntExist = -1;

        // Get the current version code from the package
        int currentVersionCode = 0;
        try {

            currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

        } catch (PackageManager.NameNotFoundException e) {

            Log.e(LOG_TAG, e.getMessage());
        }

        // Get the SharedPreferences from the preference file
        // Creates the preference file if it does not exist
        SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        // Get the saved version code or set it if it does not exist
        int savedVersionCode = prefs.getInt(prefVersionCodeKey, doesntExist);

        // Create an editor to edit the shared preferences on app update
        SharedPreferences.Editor edit = prefs.edit();

        //Check for updates
        if (savedVersionCode == doesntExist) {

            mDatabaseHandler.appUpdate();
            // First run of the app
            // Set the saved version code to the current version code
            edit.putInt(prefVersionCodeKey, currentVersionCode);
            edit.apply();
            return true;
        } else if (currentVersionCode > savedVersionCode) {

            // App update
            mDatabaseHandler.appUpdate();
            edit.putInt(prefVersionCodeKey, currentVersionCode);
            edit.apply();
            return true;
        }

        return false;
    }

    @NonNull
    @Override
    public Loader<ArrayList<SoundObject>> onCreateLoader(int id, @Nullable Bundle args) {
        return new SoundListLoader(getApplicationContext()) {

            @Override
            public ArrayList<SoundObject> loadInBackground() {
                return DatabaseHandler.getInstance(SoundboardActivity.this).getSoundCollection();
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<SoundObject>> loader, ArrayList<SoundObject> data) {

        mRecyclerAdapter.swapData(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<SoundObject>> loader) {
        mRecyclerAdapter.swapData(new ArrayList<SoundObject>());
    }
}