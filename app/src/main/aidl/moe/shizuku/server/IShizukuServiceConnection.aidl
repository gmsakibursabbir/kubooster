/*
 * Shizuku API AIDL, from RikkaApps/Shizuku-API (Apache License 2.0).
 * https://github.com/RikkaApps/Shizuku-API
 */
package moe.shizuku.server;

interface IShizukuServiceConnection {

    oneway void connected(IBinder service);

    oneway void died();
}
