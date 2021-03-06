package uni.mobile.mobileapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

import uni.mobile.mobileapp.rest.RestLocalMethods;
import uni.mobile.mobileapp.rest.callbacks.ScanAllBooksCallbacks;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    private static final int STORAGE_PERMISSION_CODE = 101;
    private int bottomNavigationSelectedItem, navigationSelectedItem;
    private Toolbar toolbar;
    private Button scanButton;
    private DrawerLayout dl;
    private TextView titleToolbar;
    private ActionBarDrawerToggle t;
    private NavigationView nv;
    private FirebaseAuth firebaseAuth;
    private StorageReference storageReference;
    private ImageView profilePic;
    private TextView navTitle;
    private Context context;
    private boolean isConnected;
    private SharedPreferences preferenceManager;
    private BookFragment bf;


    private HomeFragment homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        context = this;
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");

        setSupportActionBar(toolbar);

        // Permission
        if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)  == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(HomeActivity.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, STORAGE_PERMISSION_CODE);
        }

        // Check internet connection
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null && activeNetwork.isConnected();
        checkInternetConnection();

        titleToolbar = findViewById(R.id.toolbar_title);
        dl = findViewById(R.id.activity_home);

        scanButton = findViewById(R.id.scanButton);
        scanButton.setClickable(false);

        //Connection check
        new CheckNetwork(this);

        t = new ActionBarDrawerToggle(this, dl, toolbar, R.string.Open, R.string.Close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                titleToolbar.setText("Menu");
                invalidateOptionsMenu();
            }
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                titleToolbar.setText("MobileApp");
                invalidateOptionsMenu();
            }
        };

        t.setDrawerIndicatorEnabled(true);
        dl.addDrawerListener(t);
        t.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        nv = findViewById(R.id.navigation);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // No item selected on bottom navbar
        for(int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setCheckable(false);
        }

        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                for(int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                    bottomNavigationView.getMenu().getItem(i).setCheckable(false);
                }
                switch (id) {
                    case R.id.nav_home:
                        if (item.getItemId() == navigationSelectedItem) {
                            bottomNavigationSelectedItem = -1;
                            dl.closeDrawers();
                            break;
                        }
                        navigationSelectedItem = R.id.nav_home;
                        bottomNavigationSelectedItem = -1;
                        homeFragment = new HomeFragment();
                        openFragment(homeFragment);
                        dl.closeDrawers();
                        return true;
                    case R.id.nav_profile:
                        if (item.getItemId() == navigationSelectedItem) {
                            bottomNavigationSelectedItem = -1;
                            dl.closeDrawers();
                            break;
                        }
                        navigationSelectedItem = R.id.nav_profile;
                        bottomNavigationSelectedItem = -1;
                        openFragment(new ProfileFragment());
                        dl.closeDrawers();
                        return true;
                    case R.id.nav_settings:
                        if (item.getItemId() == navigationSelectedItem) {
                            bottomNavigationSelectedItem = -1;
                            dl.closeDrawers();
                            break;
                        }
                        navigationSelectedItem = R.id.nav_settings;
                        bottomNavigationSelectedItem = -1;
                        openFragment(new SettingsFragment());
                        dl.closeDrawers();
                        return true;
                    case R.id.nav_info:
                        dl.closeDrawers();
                        new AlertDialog.Builder(context)
                                .setTitle("App information")
                                .setMessage("This app is created by Giuseppe Capaldi and Marco Costa.\nIt was developed for Mobile Application and Cloud Computing course.\nCopyright \u00a9 2020, La Sapienza Università di Roma")
                                .setCancelable(false) // disallow cancel of AlertDialog on click of back button and outside touch
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                        return true;
                    case R.id.nav_logout:
                        userLogout();
                        return true;
                }

                return false;
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                for(int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                    bottomNavigationView.getMenu().getItem(i).setCheckable(true);
                }
                switch (item.getItemId()) {
                    case R.id.navigation_house:
                        if (item.getItemId() == bottomNavigationSelectedItem) break;
                        navigationSelectedItem = -1;
                        bottomNavigationSelectedItem = R.id.navigation_house;
                        openFragment(new HouseFragment());
                        return true;
                    case R.id.navigation_room:
                        if (item.getItemId() == bottomNavigationSelectedItem) break;
                        navigationSelectedItem = -1;
                        bottomNavigationSelectedItem = R.id.navigation_room;
                        openFragment(new RoomFragment(bottomNavigationView));
                        return true;
                    case R.id.navigation_wall:
                        if (item.getItemId() == bottomNavigationSelectedItem) break;
                        navigationSelectedItem = -1;
                        bottomNavigationSelectedItem = R.id.navigation_wall;
                        openFragment(new WallFragment(bottomNavigationView));
                        return true;
                    case R.id.navigation_shelf:
                        if (item.getItemId() == bottomNavigationSelectedItem) break;
                        navigationSelectedItem = -1;
                        bottomNavigationSelectedItem = R.id.navigation_shelf;
                        openFragment(new ShelfFragment(bottomNavigationView));
                        return true;
                    case R.id.navigation_book:
                        if (item.getItemId() == bottomNavigationSelectedItem) break;
                        navigationSelectedItem = -1;
                        bottomNavigationSelectedItem = R.id.navigation_book;
                        bf =new BookFragment(bottomNavigationView);
                        openFragment(bf);
                        return true;
                }
                return false;
            }
        });

        profilePic = nv.getHeaderView(0).findViewById(R.id.nav_profile_pic);
        navTitle = nv.getHeaderView(0).findViewById(R.id.nav_header_textView);

        firebaseAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        final FirebaseUser user = firebaseAuth.getCurrentUser();

        String idToken =user.getUid();
        //Do whatever
        Log.d("User", "GetTokenResult result = " + idToken);

        Activity act = this;
        RestLocalMethods.initRetrofit(context, idToken);
        scanButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(getApplicationContext(),"scan",Toast.LENGTH_SHORT).show();
                RestLocalMethods.scanAllBooks(act, scanButton, new ScanAllBooksCallbacks() {
                    @Override
                    public void onScanned() {
                        if(bf!=null)
                            bf.getUserBooks();
                    }
                }, null);
            }
        });

        scanButton.setClickable(true);



        if (getUserProvider(user).equals("GOOGLE")) {
            Uri imageUri = Uri.parse(user.getPhotoUrl().toString().replace("s96-c", "s400-c"));
            Picasso.get().load(imageUri).fit().centerInside().into(profilePic);
        }
        else if (getUserProvider(user).equals("FACEBOOK")) {
            Uri imageUri = Uri.parse(user.getPhotoUrl().toString() + "?height=500");
            Picasso.get().load(imageUri).fit().centerInside().into(profilePic);
        }
        else {
            // Get the image stored on Firebase via "User id/Images/Profile Pic.jpg".
            storageReference.child(firebaseAuth.getUid()).child("Images").child("Profile Pic").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).fit().centerInside().into(profilePic);
                }
            });
        }
        navTitle.setText(user.getDisplayName());
        Toast.makeText(HomeActivity.this, "Welcome, " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

        if (getUserProvider(user).equals("FIREBASE") && !user.isEmailVerified()) {
            new AlertDialog.Builder(context)
                    .setTitle("Email verification")
                    .setMessage("Your account is not activated since your email address is not verified. Activate your account clicking on the activation link sent at " + user.getEmail() + ". If you didn't receive the email click on 'Send' button to send another email.")
                    .setCancelable(false) // disallow cancel of AlertDialog on click of back button and outside touch
                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            user.sendEmailVerification();
                            Toast.makeText(getApplicationContext(),"Email verification sent!" ,Toast.LENGTH_SHORT).show();
                            userLogout();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            userLogout();
                        }
                    })
                    .show();
        }

        //loading the default fragment
        bottomNavigationSelectedItem = -1;
        navigationSelectedItem = R.id.nav_home;
        homeFragment = new HomeFragment();
        openFragment(homeFragment);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean rememberMe = preferenceManager.getBoolean("RememberMe", false);
        if (FirebaseAuth.getInstance().getCurrentUser() != null && !rememberMe) userLogout();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(HomeActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(HomeActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        t.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        t.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(t.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        homeFragment.onActivityResultFragment(requestCode, resultCode, data);
    }

    private void openFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private String getUserProvider(FirebaseUser user) {
        List<? extends UserInfo> infos = user.getProviderData();
        String provider = "FIREBASE";
        for (UserInfo ui : infos) {
            if (ui.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) provider = "GOOGLE";
            else if (ui.getProviderId().equals(FacebookAuthProvider.PROVIDER_ID)) provider = "FACEBOOK";
        }
        return provider;
    }

    private void userLogout() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>(){

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putBoolean("UseBiometrics", false);
                editor.apply();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finishAffinity();
            }
        });
    }
    private void checkInternetConnection() {
        if (!isConnected) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("You are disconnected!")
                    .setMessage("Please activate internet connection")
                    .setCancelable(false) // disallow cancel of AlertDialog on click of back button and outside touch
                    .setPositiveButton("Activate", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            bottomNavigationView.setSelectedItemId(R.id.navigation_shelf);
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragmentContainer, new ShelfFragment(bottomNavigationView));
                            transaction.commit();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                                checkInternetConnection();
                        }
                    })
                    .show();
        }
    }


}
