package com.example.myapplication;

import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.AppCheckProviderFactory;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {

        super.onCreate();
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        FirebaseAppCheck.getInstance()
                .getAppCheckToken( false)
                .addOnSuccessListener(
                        appCheckTokenResponse -> {
                            // Get the token from the response
                            String appCheckToken = appCheckTokenResponse.getToken();
                            // Use the token in your requests to Firebase services
                            // (e.g., Firebase Storage, Firestore, Realtime Database)
                            // Include the token in the appropriate headers or parameters
                        })

                .addOnFailureListener(
                        e -> {
                            // Handle failure to get the token
                            Log.e("AppCheck", "Failed to retrieve App Check token: " + e.getMessage());
                        });

    }
}
