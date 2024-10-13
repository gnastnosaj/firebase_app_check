package io.flutter.plugins.firebase.appcheck;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.AppCheckProvider;
import com.google.firebase.appcheck.AppCheckProviderFactory;
import com.google.firebase.appcheck.AppCheckToken;

import org.json.JSONObject;

public class CustomProvider {
    public class CustomAppCheckProvider implements AppCheckProvider {
        public CustomAppCheckProvider(FirebaseApp firebaseApp) {
        }

        @NonNull
        @Override
        public Task<AppCheckToken> getToken() {
            TaskCompletionSource<AppCheckToken> taskCompletionSource = new TaskCompletionSource<>();

            new Thread(() -> {
                try {
                    URL url = new URL("https://tidytech.evozic.com/appCheck");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject data = new JSONObject();
                    data.put("debug", true);

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(data.toString());

                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        String responseString = readStream(conn.getInputStream());
                        JSONObject result = new JSONObject(responseString);
                        String tokenFromServer = result.getString("token");
                        long expirationFromServer = result.getLong("ttlMillis");
                        long expMillis = expirationFromServer - 60000L;
                        AppCheckToken appCheckToken = new CustomAppCheckToken(tokenFromServer, expMillis);
                        taskCompletionSource.setResult(appCheckToken);
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    taskCompletionSource.setException(e);
                }
            }).start();

            return taskCompletionSource.getTask();
        }
    }

    public class CustomAppCheckToken extends AppCheckToken {
        private String token;
        private long expiration;

        CustomAppCheckToken(String token, long expiration) {
            this.token = token;
            this.expiration = expiration;
        }

        @NonNull
        @Override
        public String getToken() {
            return token;
        }

        @Override
        public long getExpireTimeMillis() {
            return expiration;
        }
    }

    public class CustomAppCheckProviderFactory implements AppCheckProviderFactory {
        @NonNull
        @Override
        public AppCheckProvider create(@NonNull FirebaseApp firebaseApp) {
            // Create and return an AppCheckProvider object.
            return new CustomAppCheckProvider(firebaseApp);
        }
    }

    public CustomAppCheckProviderFactory getProviderFactory() {
        return new CustomAppCheckProviderFactory();
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }
}