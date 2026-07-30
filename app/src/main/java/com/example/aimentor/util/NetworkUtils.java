package com.example.aimentor.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Small, side-effect-free network availability check for the question queue. */
public final class NetworkUtils {

    private NetworkUtils() { }

    public static boolean isOnline(Context context) {
        if (context == null) return true;
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
