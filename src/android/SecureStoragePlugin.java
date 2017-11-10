package com.crypho.plugins;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Map;

import de.adorsys.android.securestoragelibrary.SecurePreferences;

public class SecureStoragePlugin extends CordovaPlugin {

    private static final String TAG = SecureStoragePlugin.class.getSimpleName();

    // For testing SecurePreferences init
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";

    // Plugin method names
    private static final String METHOD_INIT = "init";
    private static final String METHOD_SET_SECURE = "set";
    private static final String METHOD_GET_SECURE = "get";
    private static final String METHOD_DECRYPT_RSA = "decrypt_rsa";
    private static final String METHOD_ENCRYPT_RSA = "encrypt_rsa";
    private static final String METHOD_SECURE_DEVICE = "secureDevice";
    private static final String METHOD_STORE_NON_SECURE = "store";
    private static final String METHOD_FETCH_NON_SECURE = "fetch";
    private static final String METHOD_REMOVE = "remove";
    private static final String METHOD_CLEAR = "clear";
    private static final String METHOD_KEYS = "keys";

    // Escape these keys when returning all keys
    private static final String MIGRATED_TO_NATIVE_KEY = "_SS_MIGRATED_TO_NATIVE";
    private static final String MIGRATED_TO_NATIVE_STORAGE_KEY = "_SS_MIGRATED_TO_NATIVE_STORAGE";

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "Execute: " + action);

        // Plugin is not supported in versions below Kitkat
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.w(TAG, "API 19 (Android 4.4 KitKat) is required. This device is running API " + Build.VERSION.SDK_INT);
            callbackContext.error("API 19 (Android 4.4 KitKat) is required. This device is running API " + Build.VERSION.SDK_INT);
            return true;
        }

        if (action.equals(METHOD_INIT)) {
            initSecureStorage(callbackContext);
            return true;
        } else if (action.equals(METHOD_SET_SECURE)) {
            String service = args.getString(0);
            String key = args.getString(1);
            String value = args.getString(2);
            setInSecureStorage(service, key, value, callbackContext);
            return true;
        } else if (action.equals(METHOD_GET_SECURE)) {
            String service = args.getString(0);
            String key = args.getString(1);
            getFromSecureStorage(service, key, callbackContext);
            return true;
        } else if (action.equals(METHOD_DECRYPT_RSA)) { // Copied over from old code
            final String service = args.getString(0);
            // getArrayBuffer does base64 decoding
            final byte[] decryptMe = args.getArrayBuffer(1);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        if (!RSA.isEntryAvailable(serviceToAlias(service))) {
                            RSA.createKeyPair(cordova.getActivity(), serviceToAlias(service));
                        }
                        byte[] decrypted = RSA.decrypt(decryptMe, serviceToAlias(service));
                        callbackContext.success(new String(decrypted));
                    } catch (Exception e) {
                        Log.e(TAG, "Decrypt (RSA) failed :", e);
                        callbackContext.error(e.getMessage());
                    }
                }
            });
            return true;
        } else if (action.equals(METHOD_ENCRYPT_RSA)) { // Copied over from old code
            final String service = args.getString(0);
            final String encryptMe = args.getString(1);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        if (!RSA.isEntryAvailable(serviceToAlias(service))) {
                            RSA.createKeyPair(cordova.getActivity(), serviceToAlias(service));
                        }
                        byte[] encrypted = RSA.encrypt(encryptMe.getBytes(), serviceToAlias(service));
                        callbackContext.success(Base64.encodeToString(encrypted, Base64.DEFAULT));
                    } catch (Exception e) {
                        Log.e(TAG, "Encrypt (RSA) failed :", e);
                        callbackContext.error(e.getMessage());
                    }
                }
            });
            return true;
        } else if (action.equals(METHOD_SECURE_DEVICE)) {
            Log.w(TAG, "Not clear what this method is supposed to do.");
            callbackContext.success();
            return true;
        } else if (action.equals(METHOD_STORE_NON_SECURE)) {
            final String service = args.getString(0);
            final String key = args.getString(1);
            final String value = args.getString(2);

            if (!(key.equals(MIGRATED_TO_NATIVE_STORAGE_KEY) || key.equals(MIGRATED_TO_NATIVE_KEY))) {
                Log.e(TAG, "Storing secure data in non-secure storage");
            }

            SharedPreferencesUtil.putString(cordova.getActivity(), "", key, value);
            callbackContext.success();
            return true;

        } else if (action.equals(METHOD_FETCH_NON_SECURE)) {
            final String service = args.getString(0);
            final String key = args.getString(1);

            if (!(key.equals(MIGRATED_TO_NATIVE_STORAGE_KEY) || key.equals(MIGRATED_TO_NATIVE_KEY))) {
                Log.e(TAG, "Retrieving secure data from non-secure storage");
            }

            String value = SharedPreferencesUtil.getString(cordova.getActivity(), "", key);
            if (value != null) {
                callbackContext.success(value);
            } else {
                callbackContext.error("Could not get value for key: " + key);
            }

            return true;

        } else if (action.equals(METHOD_REMOVE)) {
            String service = args.getString(0);
            String key = args.getString(1);
            removeFromSecureStorage(service, key, callbackContext);
            return true;

        } else if (action.equals(METHOD_CLEAR)) {
            final String service = args.getString(0);
            removeAllFromSecureStorage(service, callbackContext);
            return true;

        } else if (action.equals(METHOD_KEYS)) {
            String service = args.getString(0);
            getAllKeysFromSecureStorage(service, callbackContext);
            return true;
        }

        Log.d(TAG, "Action " + action + " not recognised");
        return false;
    }

    // Copied from old code
    private String serviceToAlias(String service) {
        return cordova.getActivity().getPackageName() + "." + service;
    }

    // Handle the same key from different services
    private String prependServiceName(String service, String key) {
        if (service.equals("")) {
            return key;
        } else {
            return service + "_" + key;
        }
    }

    // Initialize secure storage
    private void initSecureStorage(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Because there is no explicit init try setting and removing a random value to see
                    // if SecurePreferences work.
                    SecurePreferences.setValue(TEST_KEY, TEST_VALUE);
                    String testValue = SecurePreferences.getStringValue(TEST_KEY, "");

                    if (testValue.equals(TEST_VALUE)) {
                        SecurePreferences.removeValue(TEST_KEY);

                        // Need to indicate in success callback whether native AES is supported.
                        // It's only supported in Lollipop and above.
                        // 0 is falsy in js while 1 is truthy
//                        boolean isNativeAesSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
                        boolean isNativeAesSupported = true; // Cheating here because SecurePreferences lib will handle this
                        callbackContext.success(isNativeAesSupported ? 1 : 0);
                    } else {
                        callbackContext.error("SecurePreferences init failed for an unknown reason.");
                    }

                } catch (Exception e) { // TODO - Exception should be more specific
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    /**
     * Saves a value to secure storage
     * @param service (string) - Instance name
     * @param key (string) - Key
     * @param value (string) - Value
     * @param callbackContext - Cordova callback context
     */
    private void setInSecureStorage(final String service,
                                    final String key,
                                    final String value,
                                    final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SecurePreferences.setValue(prependServiceName(service, key), value);
                    SharedPreferencesUtil.putString(cordova.getActivity(),
                            service,
                            key,
                            "1");
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    /**
     * Get's a value from secure storage. Value is returend through callback
     * @param service (string) - Instance name
     * @param key (string) - Key
     * @param callbackContext - Cordova callback context
     */
    private void getFromSecureStorage(final String service,
                                      final String key,
                                      final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String value = SecurePreferences.getStringValue(prependServiceName(service, key), null);
                    if (value != null) {
                        callbackContext.success(value);
                    } else {
                        callbackContext.error("Could not get value for key: " + key);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    /**
     * Removes a value from secure storage
     * @param service (string) - Instance name
     * @param key (string) - Key
     * @param callbackContext - Cordova callback context
     */
    private void removeFromSecureStorage(final String service,
                                         final String key,
                                         final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (SecurePreferences.contains(prependServiceName(service, key))) {
                        SecurePreferences.removeValue(prependServiceName(service, key));
                        SharedPreferencesUtil.remove(cordova.getActivity(), service, key);
                        callbackContext.success();
                    } else {
                        callbackContext.error("Could not find value for key: " + key);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    /**
     * Removes all values in a given instance
     * @param service (string) - Instance name
     * @param callbackContext - Cordova callback context
     */
    private void removeAllFromSecureStorage(final String service,
                                            final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, ?> map = SharedPreferencesUtil.getAll(cordova.getActivity(), service);
                    for (String key : map.keySet()) {
                        SecurePreferences.removeValue(prependServiceName(service, key));
                    }
                    SharedPreferencesUtil.clear(cordova.getActivity(), service);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    /**
     * Returns all keys in a given instance. Values are returned as a Json array in the callback.
     * @param service (string) - Instance name
     * @param callbackContext - Cordova callback context
     */
    private void getAllKeysFromSecureStorage(final String service,
                                             final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Map<String, ?> map = SharedPreferencesUtil.getAll(cordova.getActivity(), service);
                JSONArray jsonArray = new JSONArray();
                for (String key : map.keySet()) {
                    jsonArray.put(key);
                }
                callbackContext.success(jsonArray);
            }
        });
    }
}
