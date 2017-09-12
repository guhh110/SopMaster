package com.danikula.videocache;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.danikula.videocache.file.MyLog;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.danikula.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;
import static com.danikula.videocache.ProxyCacheUtils.LOG_TAG;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;

/**
 * {@link Source} that uses http resource as source for {@link ProxyCache}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 * 
 *  从URL 获取数据
 */
public class HttpUrlSource implements Source {

    private static final int MAX_REDIRECTS = 5;
    public final String url;
    private static OkHttpClient okHttpClient = new OkHttpClient();
    private Call requestCall = null;
    private InputStream inputStream;
    private volatile int length = Integer.MIN_VALUE;
    private volatile String mime;
    private Map<String, String> headers;

    public HttpUrlSource(String url) {
        this(url, ProxyCacheUtils.getSupposablyMime(url));
    }

    public HttpUrlSource(String url, Map<String, String> headers) {
        this(url, ProxyCacheUtils.getSupposablyMime(url));
        this.headers = headers;
    }

    public HttpUrlSource(String url, String mime) {
        this.url = Preconditions.checkNotNull(url);
        this.mime = mime;
    }

    public HttpUrlSource(HttpUrlSource source) {
        this.url = source.url;
        this.mime = source.mime;
        this.length = source.length;
    }

    @Override
    public synchronized int length() throws ProxyCacheException {
        if (length == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return length;
    }

    @Override
    public void open(int offset) throws ProxyCacheException {
        try {
            Response response = openConnection(offset, -1);
            mime = response.header("Content-Type");
            inputStream = new BufferedInputStream(response.body().byteStream(), DEFAULT_BUFFER_SIZE);
            length = readSourceAvailableBytes(response, offset, response.code());
        } catch (IOException e) {
            throw new ProxyCacheException("Error opening okHttpClient for " + url + " with offset " + offset, e);
        }
    }

    private int readSourceAvailableBytes(Response response, int offset, int responseCode) throws IOException {
        int contentLength = Integer.valueOf(response.header("Content-Length", "-1"));

        return responseCode == HTTP_OK ? contentLength
                : responseCode == HTTP_PARTIAL ? contentLength + offset : length;
    }

    @Override
    public void close() throws ProxyCacheException {
        if (okHttpClient != null && inputStream != null && requestCall != null) {
            try {
                inputStream.close();
                requestCall.cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        if (inputStream == null) {
            throw new ProxyCacheException("Error reading data from " + url + ": okHttpClient is absent!");
        }
        try {
            return inputStream.read(buffer, 0, buffer.length);
        } catch (InterruptedIOException e) {
            throw new InterruptedProxyCacheException("Reading source " + url + " is interrupted", e);
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading data from " + url, e);
        }
    }

    private void fetchContentInfo() throws ProxyCacheException {
        MyLog.d(LOG_TAG, "Read content info from " + url);
        Response response = null;
        InputStream inputStream = null;
        try {
            response = openConnection(0, 20000);
            length = Integer.valueOf(response.header("Content-Length", "-1"));
            mime = response.header("Content-Type");
            inputStream = response.body().byteStream();
            MyLog.i(LOG_TAG, "Content info for `" + url + "`: mime: " + mime + ", content-length: " + length);
        } catch (IOException e) {
            MyLog.e(LOG_TAG, "Error fetching info from " + url, e);
        } finally {
            ProxyCacheUtils.close(inputStream);
            if (response != null) {
                requestCall.cancel();
            }
        }
    }

    private Response openConnection(int offset, int timeout) throws IOException, ProxyCacheException {
        boolean redirected;
        int redirectCount = 0;
        String url = this.url;
        Request request = null;
        //do {
            MyLog.d(LOG_TAG, "Open okHttpClient " + (offset > 0 ? " with offset " + offset : "") + " to " + url);
//            okHttpClient = (HttpURLConnection) new URL(url).openConnection();
            Request.Builder builder = new Request.Builder();
            builder.url(url);
            //flac
            if(headers != null) {
                //设置请求头
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    MyLog.i(LOG_TAG, "请求头信息 key:" + entry.getKey() +" Value" + entry.getValue());
//                    okHttpClient.setRequestProperty(entry.getKey(), entry.getValue());
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            if (offset > 0) {
                builder.addHeader("Range", "bytes=" + offset + "-");
            }

            request = builder.build();
            requestCall = okHttpClient.newCall(request);
            /*if (redirected) {
                url = okHttpClient.getHeaderField("Location");
                redirectCount++;
                okHttpClient.disconnect();
            }
            if (redirectCount > MAX_REDIRECTS) {
                throw new ProxyCacheException("Too many redirects: " + redirectCount);
            }*/
        //} while (redirected);

        return requestCall.execute();
    }

    public synchronized String getMime() throws ProxyCacheException {
        if (TextUtils.isEmpty(mime)) {
            fetchContentInfo();
        }
        return mime;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{url='" + url + "}";
    }
}
