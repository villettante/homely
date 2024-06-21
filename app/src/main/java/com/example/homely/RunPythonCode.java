package com.example.homely;

import android.os.AsyncTask;
import android.text.PrecomputedText;

import com.google.android.gms.common.api.Result;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunPythonCode extends AsyncTask<String, Void, Void> {
    private Session session;
    private ChannelExec channel;
    @Override
    protected Void doInBackground(String... arguments) {
        String user = "pi";
        String host = "172.20.10.5";

        String pythonScriptPath = arguments[0];
        String arg = arguments[1];

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword("raspberry");
            session.connect();

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("python3 " + pythonScriptPath + " " + arg);
            channel.connect();

            while (channel.isConnected() && !isCancelled()) {
                Thread.sleep(100);
            }

            channel.disconnect();
            session.disconnect();
        } catch (JSchException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
