package com.airsaid.localization.license;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.Messages;

public class LicenseStartupActivity implements StartupActivity {

    private static final Logger log = Logger.getInstance(LicenseStartupActivity.class);

    @Override
    public void runActivity(Project project) {
        log.info("Android Localize Plus: Starting license verification...");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                boolean ok = verifyLicenseSafely();
                if (ok) {
                    log.info("✓ License verified successfully");
                } else {
                    handleInvalidLicense(project);
                }
            } catch (Exception e) {
                log.error("License check failed", e);
                handleInvalidLicense(project);
            }
        });
    }

    private boolean verifyLicenseSafely() {
        try {
            Boolean licensed = CheckLicense.isLicensed();
            return Boolean.TRUE.equals(licensed);
        } catch (Exception e) {
            return false;
        }
    }

    private void handleInvalidLicense(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {

            int result = Messages.showYesNoDialog(
                    project,
                    "The license for Android Localize Plus is invalid or has expired.\n" +
                            "You need to activate or renew your license.\n\n" +
                            "Open registration dialog now?",
                    "License Verification Failed",
                    "Open Registration",
                    "Exit Plugin",
                    Messages.getErrorIcon()
            );

            if (result == Messages.YES) {
                try {
                    CheckLicense.requestLicense("Please activate or renew your Android Localize Plus license.");
                } catch (Exception e) {
                    log.error("Failed to open registration dialog", e);
                }
            }

            disablePluginCompletely();
        });
    }

    private void disablePluginCompletely() {
        log.warn("Plugin is disabled due to invalid license.");

        ApplicationManager.getApplication().invokeLater(() -> {
            throw new RuntimeException("Plugin disabled due to invalid or expired license.");
        });
    }
}
