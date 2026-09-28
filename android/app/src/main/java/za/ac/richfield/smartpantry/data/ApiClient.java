package za.ac.richfield.smartpantry.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import za.ac.richfield.smartpantry.util.Prefs;

/**
 * Talks to the pantry REST API.
 *
 * <p>Android kills any app that performs network work on the main thread, so
 * every call here runs on a small background pool and posts its result back to
 * the main thread through a {@link Handler}. Doing that in one place means the
 * repositories and activities never have to think about threads: they hand over
 * a {@link Callback} and are called back where it is safe to touch views.
 *
 * <p>Errors arrive as readable messages rather than exceptions. A user seeing
 * "Cannot reach the pantry API" can act on it; a user seeing
 * {@code java.net.ConnectException} cannot.
 */
public class ApiClient {

    /**
     * Two-outcome callback, always delivered on the main thread.
     *
     * @param <T> the parsed response type
     */
    public interface Callback<T> {
        void onSuccess(T result);

        void onFailure(String message);
    }

    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * Three threads is enough: the app never has more than a couple of requests
     * in flight, and an unbounded pool would let a flaky connection spawn threads
     * without limit.
     */
    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static ApiClient instance;

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final Prefs prefs;

    private ApiClient(Context context) {
        this.prefs = new Prefs(context);
        // Short timeouts. A local API either answers immediately or is not
        // running, and making the user wait 30 seconds to be told that is poor.
        this.http = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }

    public String getBaseUrl() {
        return prefs.getApiBaseUrl();
    }

    // ---------------------------------------------------------------- verbs

    public <T> void get(String path, Type type, Callback<T> callback) {
        execute(new Request.Builder().url(getBaseUrl() + path).get(), type, callback);
    }

    public <T> void post(String path, Object body, Type type, Callback<T> callback) {
        execute(new Request.Builder().url(getBaseUrl() + path).post(jsonBody(body)), type, callback);
    }

    public <T> void put(String path, Object body, Type type, Callback<T> callback) {
        execute(new Request.Builder().url(getBaseUrl() + path).put(jsonBody(body)), type, callback);
    }

    public <T> void delete(String path, Type type, Callback<T> callback) {
        execute(new Request.Builder().url(getBaseUrl() + path).delete(), type, callback);
    }

    private RequestBody jsonBody(Object body) {
        return RequestBody.create(gson.toJson(body), JSON);
    }

    // -------------------------------------------------------------- plumbing

    private <T> void execute(final Request.Builder builder, final Type type,
                             final Callback<T> callback) {
        IO.execute(new Runnable() {
            @Override
            public void run() {
                try (Response response = http.newCall(builder.build()).execute()) {
                    ResponseBody body = response.body();
                    String text = body == null ? "" : body.string();

                    if (!response.isSuccessful()) {
                        postFailure(callback, readServerMessage(text, response.code()));
                        return;
                    }

                    final T parsed = gson.fromJson(text, type);
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(parsed);
                        }
                    });
                } catch (IOException exception) {
                    // Thrown when the server is not running or the URL is wrong,
                    // which during development is the overwhelmingly likely cause.
                    Log.w(TAG, "request failed", exception);
                    postFailure(callback, "Cannot reach the pantry API at " + getBaseUrl()
                            + ". Check that the server is running.");
                } catch (Exception exception) {
                    Log.e(TAG, "unexpected failure", exception);
                    postFailure(callback, "Unexpected error: " + exception.getMessage());
                }
            }
        });
    }

    /**
     * The API reports problems as {"error": "..."} with a helpful sentence.
     * Surfacing that beats showing a bare status code, but it must not be trusted
     * to be well-formed, hence the fallback.
     */
    private String readServerMessage(String text, int code) {
        try {
            JsonObject object = JsonParser.parseString(text).getAsJsonObject();
            if (object.has("error")) {
                return object.get("error").getAsString();
            }
        } catch (Exception ignored) {
            // Fall through to the generic message below.
        }
        return "The server returned an error (HTTP " + code + ").";
    }

    private <T> void postFailure(final Callback<T> callback, final String message) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(message);
            }
        });
    }
}
