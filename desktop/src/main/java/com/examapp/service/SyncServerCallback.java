package com.examapp.service;

public interface SyncServerCallback {
    void onServerStarted(String ipAddress, int port);
    void onServerStopped();
    void onClientConnected();
    void onClientDisconnected();
    void onAuthCodeGenerated(String authCode); // New callback for auth code
    void onSyncProgress(String message);
    void onSyncCompleted();
    void onError(String errorMessage);
}