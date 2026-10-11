package com.yourstudio.aerolauncher.privacy;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.yourstudio.aerolauncher.R;

/**
 * Prompts for biometrics / device credential before opening a locked app.
 */
public final class AppLockHelper {

    public interface Callback {
        void onAuthenticated();

        void onFailed();
    }

    private AppLockHelper() {
    }

    public static void authenticate(
            @NonNull FragmentActivity activity,
            @NonNull CharSequence appLabel,
            @NonNull Callback callback) {
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.app_lock_prompt_title))
                .setSubtitle(activity.getString(R.string.app_lock_prompt_subtitle, appLabel));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            int can = BiometricManager.from(activity).canAuthenticate(authenticators);
            if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                    || can == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                callback.onAuthenticated();
                return;
            }
            builder.setAllowedAuthenticators(authenticators);
        } else {
            int can = BiometricManager.from(activity).canAuthenticate();
            if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                    || can == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                callback.onAuthenticated();
                return;
            }
            builder.setDeviceCredentialAllowed(true);
        }

        BiometricPrompt prompt = new BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        callback.onAuthenticated();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        callback.onFailed();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Keep the prompt open for another attempt.
                    }
                });
        prompt.authenticate(builder.build());
    }
}
