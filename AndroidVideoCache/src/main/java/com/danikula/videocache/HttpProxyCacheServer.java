package com.danikula.videocache;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.danikula.videocache.file.MyLog;

import com.danikula.videocache.file.DiskUsage;
import com.danikula.videocache.file.FileNameGenerator;
import com.danikula.videocache.file.Md5FileNameGenerator;
import com.danikula.videocache.file.TotalCountLruDiskUsage;
import com.danikula.videocache.file.TotalSizeLruDiskUsage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static com.danikula.videocache.Preconditions.checkAllNotNull;
import static com.danikula.videocache.Preconditions.checkNotNull;
import static com.danikula.videocache.ProxyCacheUtils.LOG_TAG;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Simple lightweight proxy server with file caching support that handles HTTP requests.
 * Typical usage:
 * <pre><code>
 * public onCreate(Bundle state) {
 *      super.onCreate(state);
 * <p/>
 *      HttpProxyCacheServer proxy = getProxy();
 *      String proxyUrl = proxy.getProxyUrl(VIDEO_URL);
 *      videoView.setVideoPath(proxyUrl);
 * }
 * <p/>
 * private HttpProxyCacheServer getProxy() {
 * // should return single instance of HttpProxyCacheServer shared for whole app.
 * }
 * <code/>
 * 
 *  HttpProxyCacheServer$Builder for {@link Builder}.
 * </pre>
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpProxyCacheServer {

    private static final String PROXY_HOST = "127.0.0.1";
    private static final String PING_REQUEST = "ping";//手动ping
    private static final String PING_RESPONSE = "ping ok";//回应

    private final Object clientsLock = new Object();
    private final ExecutorService socketProcessor = Executors.newFixedThreadPool(8);//创建了1 個可以容纳10個线程任务的线程池
    //key 为地址 对应 1 个 HttpProxyCacheServerClient
    private final Map<String, HttpProxyCacheServerClients> clientsMap = new ConcurrentHashMap<>();
    private final ServerSocket serverSocket;
    private final int port;
    private final Thread waitConnectionThread;
    private final Config config;
    private boolean pinged;//检查与服务器是否连通
    private Map<String, String> headers;

    public HttpProxyCacheServer(Context context) {
        this(new Builder(context).buildConfig());
    }

    private HttpProxyCacheServer(Config config) {
        this.config = checkNotNull(config);
        try {
            InetAddress inetAddress = InetAddress.getByName(PROXY_HOST);
            //0 表示由操作系统分配  backlog指定客户连接请求队列的长度
            this.serverSocket = new ServerSocket(0, 8, inetAddress);
            this.port = serverSocket.getLocalPort();//获取本地端口，且未绑定
            CountDownLatch startSignal = new CountDownLatch(1);//计数器 原子操作
            this.waitConnectionThread = new Thread(new WaitRequestsRunnable(startSignal));
            this.waitConnectionThread.start();
            startSignal.await(); // freeze thread, wait for server starts
            MyLog.i(LOG_TAG, "Proxy cache server started. Ping it...");
            makeSureServerWorks();
        } catch (IOException | InterruptedException e) {
            socketProcessor.shutdown();
            throw new IllegalStateException("Error starting local proxy server", e);
        }
    }

    /**
     * 确保服务工作
     */
    private void makeSureServerWorks() {
        int maxPingAttempts = 5;//重复连接次数
        int delay = 600;//
        int pingAttempts = 0;
        while (pingAttempts < maxPingAttempts) {
            try {
            	// 返回 的  Future 对象可以用于判断 Runnable 是否结束执行
                Future<Boolean> pingFuture = socketProcessor.submit(new PingCallable());
                this.pinged = pingFuture.get(delay, MILLISECONDS);
                if (this.pinged) {
                    return;
                }
                SystemClock.sleep(delay);//不会抛出InterruptedException
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                MyLog.e(LOG_TAG, "Error pinging server [attempt: " + pingAttempts + ", timeout: " + delay + "]. ", e);
            }
            pingAttempts++;
            delay *= 2;//每次倍增 延迟时间 delay
        }
        MyLog.e(LOG_TAG, "Shutdown server… Error pinging server [attempts: " + pingAttempts + ", max timeout: " + delay / 2 + "]. " +
                "If you see this message, please, email me danikula@gmail.com");
        shutdown();
    }

    /**
     * ping to Server
     * @return
     * @throws ProxyCacheException
     */
    private boolean pingServer() throws ProxyCacheException {
        String pingUrl = appendToProxyUrl(PING_REQUEST);
        HttpUrlSource source = new HttpUrlSource(pingUrl);
        try {
            byte[] expectedResponse = PING_RESPONSE.getBytes();
            source.open(0);
            byte[] response = new byte[expectedResponse.length];
            source.read(response);
            boolean pingOk = Arrays.equals(expectedResponse, response);
            MyLog.d(LOG_TAG, "Ping response: `" + new String(response) + "`, pinged? " + pingOk);
            return pingOk;
        } catch (ProxyCacheException e) {
            MyLog.e(LOG_TAG, "Error reading ping response", e);
            return false;
        } finally {
            source.close();
        }
    }
    public String getProxyUrl(String url) {
        return getProxyUrl(url, true);
    }
    public String getProxyUrl(String url,boolean allowCachedFileUri) {
        if (allowCachedFileUri && isCached(url)) {
            File cacheFile = getCacheFile(url);
            touchFileSafely(cacheFile);
            return Uri.fromFile(cacheFile).toString();
        }else if (!pinged) {
            MyLog.e(LOG_TAG, "Proxy server isn't pinged. Caching doesn't work. If you see this message, please, email me danikula@gmail.com");
        }
        return pinged ? appendToProxyUrl(url) : url;
    }
    public boolean isCached(String url) {
        checkNotNull(url, "Url can't be null!");
        return getCacheFile(url).exists();
    }
    private void touchFileSafely(File cacheFile) {
        try {
            config.diskUsage.touch(cacheFile);
        } catch (IOException e) {
            Log.e("sssddd-e","Error touching file " + cacheFile);
        }
    }

    private File getCacheFile(String url) {
        File cacheDir = config.cacheRoot;
        String fileName = config.fileNameGenerator.generate(url);
        return new File(cacheDir, fileName);
    }

    private String appendToProxyUrl(String url) {
        return String.format("http://%s:%d/%s", PROXY_HOST, port, ProxyCacheUtils.encode(url));
    }

	/**
     *
     * @param headers
     * @throws ProxyCacheException
     */
    public void setCacheHeaders(Map<String,String> headers) {
        checkAllNotNull(headers);
        synchronized (clientsLock) {
            this.headers = headers;
        }
    }

    /**
     * 缓存监听器
     * @param cacheListener
     * @param url
     */
    public void registerCacheListener(CacheListener cacheListener, String url) {
        checkAllNotNull(cacheListener, url);
        synchronized (clientsLock) {
            try {
                getClients(url).registerCacheListener(cacheListener);
            } catch (ProxyCacheException e) {
                MyLog.d(LOG_TAG, "Error registering cache listener", e);
            }
        }
    }


    public void unregisterCacheListener(CacheListener cacheListener, String url) {
        checkAllNotNull(cacheListener, url);
        synchronized (clientsLock) {
            try {
                getClients(url).unregisterCacheListener(cacheListener);
            } catch (ProxyCacheException e) {
                MyLog.d(LOG_TAG, "Error registering cache listener", e);
            }
        }
    }

	/**
     * 反注册所有缓存监听器
     */
    public void unregisterCacheListener() {
        MyLog.i(LOG_TAG, "unregisterAllCacheListener");
        synchronized (clientsLock) {
            for (HttpProxyCacheServerClients clients : clientsMap.values()) {
                clients.clearAllCacheListener();
            }
        }
    }
    
    /**
     * 关闭代理
     */
    public void shutdown() {
        MyLog.i(LOG_TAG, "Shutdown proxy server");
        shutdownClients();
        waitConnectionThread.interrupt();
        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error shutting down proxy server", e));
        }
    }
    
    /**
     * 关闭所有Client
     */
    private void shutdownClients() {
        synchronized (clientsLock) {
            for (HttpProxyCacheServerClients clients : clientsMap.values()) {
                clients.shutdown();
            }
            clientsMap.clear();
        }
    }

    /**
     * 等待请求
     */
    private void waitForRequest() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                MyLog.d(LOG_TAG, "Accept new socket " + socket);
                socketProcessor.submit(new SocketProcessorRunnable(socket));
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error during waiting connection", e));
        }
    }

    /**
     * 处理缓存代理 Socket
     * @param socket
     */
    private void processSocket(Socket socket) {
        try {
            GetRequest request = GetRequest.read(socket.getInputStream());
            MyLog.i(LOG_TAG, "Request to cache proxy:" + request);
            String url = ProxyCacheUtils.decode(request.uri);
            if (PING_REQUEST.equals(url)) {
                responseToPing(socket);
            } else {
                HttpProxyCacheServerClients clients = getClients(url);
                clients.processRequest(request, socket);
            }
        } catch (SocketException e) {
            // There is no way to determine that client closed connection http://stackoverflow.com/a/10241044/999458
            // So just to prevent log flooding don't log stacktrace
            MyLog.d(LOG_TAG, "Closing socket… Socket is closed by client.");
        } catch (ProxyCacheException | IOException e) {
            onError(new ProxyCacheException("Error processing request", e));
        } finally {
            releaseSocket(socket);
            MyLog.d(LOG_TAG, "Opened connections: " + getClientsCount());
        }
    }

    private void responseToPing(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write("HTTP/1.1 200 OK\n\n".getBytes());
        out.write(PING_RESPONSE.getBytes());
    }

    private HttpProxyCacheServerClients getClients(String url) throws ProxyCacheException {
        synchronized (clientsLock) {
            HttpProxyCacheServerClients clients = clientsMap.get(url);
            if (clients == null) {
                clients = new HttpProxyCacheServerClients(url, config ,headers);
                if(clientsMap.size() > 3) {
                    clientsMap.clear();
                }
                clientsMap.put(url, clients);
            }
            return clients;
        }
    }


    private int getClientsCount() {
        synchronized (clientsLock) {
            int count = 0;
            for (HttpProxyCacheServerClients clients : clientsMap.values()) {
                count += clients.getClientsCount();
            }
            return count;
        }
    }

    private void releaseSocket(Socket socket) {
        closeSocketInput(socket);
        closeSocketOutput(socket);
        closeSocket(socket);
    }

    private void closeSocketInput(Socket socket) {
        try {
            if (!socket.isInputShutdown()) {
                socket.shutdownInput();
            }
        } catch (SocketException e) {
            // There is no way to determine that client closed connection http://stackoverflow.com/a/10241044/999458
            // So just to prevent log flooding don't log stacktrace
            MyLog.d(LOG_TAG, "Releasing input stream… Socket is closed by client.");
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket input stream", e));
        }
    }

    private void closeSocketOutput(Socket socket) {
        try {
            if (socket.isOutputShutdown()) {
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket output stream", e));
        }
    }

    private void closeSocket(Socket socket) {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket", e));
        }
    }

    private void onError(Throwable e) {
        MyLog.e(LOG_TAG, "HttpProxyCacheServer error", e);
    }

    private final class WaitRequestsRunnable implements Runnable {

        private final CountDownLatch startSignal;

        public WaitRequestsRunnable(CountDownLatch startSignal) {
            this.startSignal = startSignal;
        }

        @Override
        public void run() {
            startSignal.countDown();
            waitForRequest();
        }
    }

    private final class SocketProcessorRunnable implements Runnable {

        private final Socket socket;

        public SocketProcessorRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            processSocket(socket);
        }
    }
    
    /**
     * Callable和Future，它俩很有意思的，一个产生结果，一个拿到结果。
     * <br/>
     * Callable 被线程执行后，可以返回值
     * <br/>
     * Future可以拿到异步执行任务的返回值
     * @author 
     *
     * @create_at:2015年11月27日
     */
    private class PingCallable implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            return pingServer();
        }
    }

    /**
     * Builder for {@link HttpProxyCacheServer}.
     * 
     * 建造者模式 用于构建 缓存目录、 文件名生成器、 磁盘缓存大小管理
     */
    public static final class Builder {

        private static final long DEFAULT_MAX_SIZE = 1024 * 1024 * 1024 * 5;

        private File cacheRoot;
        private FileNameGenerator fileNameGenerator;
        private DiskUsage diskUsage;

        public Builder(Context context) {
            this.cacheRoot = StorageUtils.getIndividualCacheDirectory(context);
            this.diskUsage = new TotalSizeLruDiskUsage(DEFAULT_MAX_SIZE);
//            this.diskUsage = new TotalCountLruDiskUsage(1);
            this.fileNameGenerator = new Md5FileNameGenerator();
        }

        /**
         * Overrides default cache folder to be used for caching files.
         * <p/>
         * By default AndroidVideoCache uses
         * '/Android/data/[app_package_name]/cache/video-cache/' if card is mounted and app has appropriate permission
         * or 'video-cache' subdirectory in default application's cache directory otherwise.
         * <p/>
         * <b>Note</b> directory must be used <b>only</b> for AndroidVideoCache files.
         *
         * @param file a cache directory, can't be null.
         * @return a builder.
         */
        public Builder cacheDirectory(File file) {
            this.cacheRoot = checkNotNull(file);
            return this;
        }

        /**
         * Overrides default cache file name generator {@link Md5FileNameGenerator} .
         *
         * @param fileNameGenerator a new file name generator.
         * @return a builder.
         */
        public Builder fileNameGenerator(FileNameGenerator fileNameGenerator) {
            this.fileNameGenerator = checkNotNull(fileNameGenerator);
            return this;
        }

        /**
         * Sets max cache size in bytes.
         * All files that exceeds limit will be deleted using LRU strategy.
         * Default value is 512 Mb.
         * <p/>
         * Note this method overrides result of calling {@link #maxCacheFilesCount(int)}
         *
         * @param maxSize max cache size in bytes.
         * @return a builder.
         */
        public Builder maxCacheSize(long maxSize) {
            this.diskUsage = new TotalSizeLruDiskUsage(maxSize);
            return this;
        }

        /**
         * Sets max cache files count.
         * All files that exceeds limit will be deleted using LRU strategy.
         * <p/>
         * Note this method overrides result of calling {@link #maxCacheSize(long)}
         *
         * @param count max cache files count.
         * @return a builder.
         */
        public Builder maxCacheFilesCount(int count) {
            this.diskUsage = new TotalCountLruDiskUsage(count);
            return this;
        }

        /**
         * Builds new instance of {@link HttpProxyCacheServer}.
         *
         * @return proxy cache. Only single instance should be used across whole app.
         */
        public HttpProxyCacheServer build() {
            Config config = buildConfig();
            return new HttpProxyCacheServer(config);
        }

        private Config buildConfig() {
            return new Config(cacheRoot, fileNameGenerator, diskUsage);
        }

    }
}
