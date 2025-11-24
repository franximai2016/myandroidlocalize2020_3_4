package com.ntm.localization.license;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;

/**
 * Startup activity that verifies JetBrains Marketplace license
 * for a Paid plugin (no freemium features).
 */
public class LicenseStartupActivity implements StartupActivity {

    private final Logger log = Logger.getInstance(LicenseStartupActivity.class);

    @Override
    public void runActivity(Project project) {
        log.info("My Android Localize: Starting license verification...");

        // Kiểm tra license trên background thread
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                boolean licenseValid = verifyLicenseSafely();
                if (licenseValid) {
                    log.info("✅ License verified successfully. Plugin is active.");
                } else {
                    handleInvalidLicense(project);
                }
            } catch (Exception e) {
                log.error("License verification failed with unexpected error", e);
                handleInvalidLicense(project);
            }
        });
    }

    /**
     * Gọi CheckLicense.isLicensed() và xử lý null/exception
     */
    private boolean verifyLicenseSafely() {
        try {
            Boolean licensed = CheckLicense.isLicensed();
            return Boolean.TRUE.equals(licensed); // true = hợp lệ, false/null = không hợp lệ
        } catch (Exception ex) {
            log.warn("License verification failed: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Khi license không hợp lệ → hiển thị cảnh báo + mở hộp thoại đăng ký plugin
     */
    private void handleInvalidLicense(Project project) {
        log.warn("❌ Invalid or expired license detected — prompting user to activate...");

        ApplicationManager.getApplication().invokeLater(() -> {
            int result = Messages.showYesNoDialog(
                    project,
                    "The license for My Android Localize is invalid or has expired.\n" +
                            "You need to activate or renew your license via JetBrains Marketplace.\n\n" +
                            "Would you like to open the registration dialog now?",
                    "License Verification Failed",
                    "Open Registration",
                    "Exit Plugin",
                    Messages.getErrorIcon()
            );

            if (result == Messages.YES) {
                try {
                    CheckLicense.requestLicense(
                            "Please activate or renew your My Android Localize license."
                    );
                    log.warn("Open CheckLicense.requestLicense");
                } catch (Exception e) {
                    log.error("Failed to open registration dialog", e);
                }
            }

            disablePluginCompletely();
        });
    }

    /**
     * Chặn hoàn toàn plugin khi license không hợp lệ
     */
    private void disablePluginCompletely() {
        try {
            log.info("Unloading My Android Localize plugin due to invalid license.");

            ApplicationManager.getApplication().invokeLater(() -> {
                // JetBrains không cho phép unload plugin bằng API public,
                // nên cách dễ nhất là ném exception để chặn plugin chạy tiếp.
                throw new RuntimeException("Plugin disabled due to invalid or expired license.");
            });

        } catch (Exception ex) {
            log.error("Error while disabling plugin", ex);
        }
    }
}
