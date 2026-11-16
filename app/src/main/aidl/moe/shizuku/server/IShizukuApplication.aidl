/*
 * Shizuku API AIDL, from RikkaApps/Shizuku-API (Apache License 2.0).
 * https://github.com/RikkaApps/Shizuku-API
 */
package moe.shizuku.server;

interface IShizukuApplication {

    oneway void bindApplication(in Bundle data) = 1;

    oneway void dispatchRequestPermissionResult(int requestCode, in Bundle data) = 2;

    // Sui only
    void showPermissionConfirmation(int requestUid, int requestPid, in String requestPackageName, int requestCode) = 10000;
}
