package com.oves.app.util.file;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

import android.content.Context;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

public class FileServer {
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    ChannelFuture channelFuture;
    Context context;
    private ThreadPoolExecutor threadPoolExecutor;


    public void startFileServer(int port, Context ctx) {
        threadPoolExecutor = new ThreadPoolExecutor(
                4,
                6,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000)
        );
        context = ctx;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);
        new Thread() {
            @Override
            public void run() {
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            //http消息解码器
                            ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                            //将消息转为单一的FullHttpRequest或者FullHttpResponse，因为http解码器在每个http消息中会生成多个消息对象
                            ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65535));
                            //对响应消息进行编码
                            ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                            //支持异步发送大大码流，但不占用过多但内存，防止发生Java内存溢出
                            ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            ch.pipeline().addLast(new FileServerHandler());
                        }
                    });
                    channelFuture = b.bind(port).sync();
                    Logger.d("File server started and listening on port " + port);
                    channelFuture.channel().closeFuture().sync();
                    Logger.d("File server Stop!!!");
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.e(e.getMessage());
                } finally {
                    stopFileServer();
                }
            }
        }.start();
    }


    public void stopFileServer() {
        try {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (channelFuture != null && channelFuture.channel().isOpen()) {
                channelFuture.channel().close();
            }
            if (threadPoolExecutor != null) threadPoolExecutor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class FileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            if (!req.decoderResult().isSuccess()) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            String uri = req.uri();
            if (uri.contains(".") && req.method() == HttpMethod.GET) {
                String path = URLDecoder.decode(uri.substring(1), StandardCharsets.UTF_8.name());
                File internalDir = context.getFilesDir();
                File file = new File(internalDir, path);
                synchronized (file.getAbsolutePath().intern()) {
                    if (file.exists() && file.isFile()) {
                        Logger.d("=====cache=====");
                        handleGetRequest(ctx, path);
                        return;
                    }
                }
//                if (req.method() == HttpMethod.GET) {
//                    handleGetRequest(ctx, path);
//                } else if (req.method() == HttpMethod.POST) {
//                    handlePostRequest(ctx, path, req);
//                } else if (req.method() == HttpMethod.DELETE) {
//                    handleDeleteRequest(ctx, path);
//                } else {
//                    sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
//                }
            }
            Logger.d("=====forwardRequest=====");
            try {
                forwardRequest(ctx, req);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            Logger.d("==================channelActive===================");
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            Logger.d("==================channelInactive===================");
        }

        private void handleGetRequest(ChannelHandlerContext ctx, String path) throws IOException {
            File internalDir = context.getFilesDir();
            File file = new File(internalDir, path);
            if (!file.exists() || file.isDirectory()) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[(int) file.length()];
                fis.read(buffer);
                response.content().writeBytes(buffer);
            }
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void handlePostRequest(ChannelHandlerContext ctx, String path, FullHttpRequest req) throws IOException {
            File internalDir = context.getFilesDir();
            File file = new File(internalDir, path);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] content = new byte[req.content().readableBytes()];
                req.content().readBytes(content);
                fos.write(content);
                fos.flush();
            }
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void handleDeleteRequest(ChannelHandlerContext ctx, String path) {
            File internalDir = context.getFilesDir();
            File file = new File(internalDir, path);
            if (!file.exists() || file.isDirectory()) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
            if (file.delete()) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }

        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, io.netty.buffer.Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", StandardCharsets.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }


    private void forwardRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
        String url = "https://wvapp.omnivoltaic.com:" + 443 + request.uri();
        Request.Builder okHttpRequestBuilder = new Request.Builder()
                .url(url);

        Logger.d(url);

        request.headers().forEach(entry -> {
            okHttpRequestBuilder.addHeader(entry.getKey(), entry.getValue());
        });
        okHttpRequestBuilder.addHeader("host", "wvapp.omnivoltaic.com");
        MediaType mediaType = MediaType.parse(request.headers().get(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream"));
        RequestBody requestBody;
        if (request.content().readableBytes() > 0) {
            byte[] contentBytes = new byte[request.content().readableBytes()];
            request.content().readBytes(contentBytes);
            requestBody = RequestBody.create(mediaType, contentBytes);
        } else {
            requestBody = RequestBody.create(mediaType, new byte[0]);
        }
        HttpMethod nettyMethod = request.method();
        if (nettyMethod.equals(HttpMethod.GET)) {
            okHttpRequestBuilder.get();
        } else if (nettyMethod.equals(HttpMethod.POST)) {
            okHttpRequestBuilder.post(requestBody);
        } else if (nettyMethod.equals(HttpMethod.PUT)) {
            okHttpRequestBuilder.put(requestBody);
        } else if (nettyMethod.equals(HttpMethod.DELETE)) {
            if (requestBody.contentLength() > 0) {
                okHttpRequestBuilder.delete(requestBody);
            } else {
                okHttpRequestBuilder.delete();
            }
        } else if (nettyMethod.equals(HttpMethod.HEAD)) {
            okHttpRequestBuilder.head();
        } else if (nettyMethod.equals(HttpMethod.OPTIONS)) {
            okHttpRequestBuilder.method("OPTIONS", null);
        } else if (nettyMethod.equals(HttpMethod.PATCH)) {
            okHttpRequestBuilder.patch(requestBody);
        }
        Request okHttpRequest = okHttpRequestBuilder.build();
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().
                addInterceptor(new GzipRequestInterceptor())
                .build();
        okHttpClient.newCall(okHttpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (request.refCnt() > 0) {
                    request.release();
                }
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                ctx.writeAndFlush(response);
            }

            @Override
            public void onResponse(Call call, Response okHttpResponse) throws IOException {
                try (ResponseBody body = okHttpResponse.body()) {
                    FullHttpResponse nettyResponse = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.valueOf(okHttpResponse.code())
                    );
                    if (body != null) {
                        String string = body.string();
                        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
                        nettyResponse.content().writeBytes(bytes);
                        nettyResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
                        System.out.println(new String(bytes, StandardCharsets.UTF_8));
                        threadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                String uri = request.uri();
                                if (uri.contains(".")) {
                                    String path = null;
                                    try {
                                        path = URLDecoder.decode(uri.substring(1), StandardCharsets.UTF_8.name());
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    File cachedFile = new File(context.getFilesDir(), path);
                                    synchronized (cachedFile.getAbsolutePath().intern()) {
                                        if (cachedFile.exists() && cachedFile.isFile())
                                            cachedFile.delete();
                                        cachedFile.getParentFile().mkdirs();
                                        FileOutputStream fos = null;
                                        try {
                                            fos = new FileOutputStream(cachedFile);
                                            fos.write(bytes);
                                            fos.flush();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            cachedFile.delete();
                                        } finally {
                                            if (fos != null) {
                                                try {
                                                    fos.close();
                                                } catch (IOException e) {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                    okHttpResponse.headers().names().forEach(header -> {
                        nettyResponse.headers().set(header, okHttpResponse.headers().get(header));
                    });
                    nettyResponse.headers().remove("content-encoding");
                    ctx.writeAndFlush(nettyResponse);


                } finally {
                    if (request.refCnt() > 0) {
                        request.release();
                    }
                }
            }
        });
    }


    // 自定义日志拦截器
    private class GzipRequestInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            String contentEncoding = originalResponse.header("Content-Encoding");
            if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                return originalResponse.newBuilder()
                        .body(new GzipResponseBody(originalResponse.body()))
                        .build();
            }
            return originalResponse;
        }
    }

    class GzipResponseBody extends ResponseBody {
        private final ResponseBody delegate;

        GzipResponseBody(ResponseBody delegate) {
            this.delegate = delegate;
        }

        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() {
            return -1; // 解压后长度未知
        }

        @Override
        public BufferedSource source() {
            try {
                InputStream inputStream = new GZIPInputStream(delegate.byteStream());
                return Okio.buffer(Okio.source(inputStream));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }



}
