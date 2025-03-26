package com.oves.app.util;

import android.content.Context;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedWriteHandler;

public class FileServer {
    static EventLoopGroup bossGroup;
    static EventLoopGroup workerGroup;
    static ChannelFuture channelFuture;
    static Context context;

    public static void startFileServer(int port, Context ctx) {
        context = ctx;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);
        new Thread() {
            @Override
            public void run() {
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
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


    public static void stopFileServer() {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static class FileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            Logger.d("FileServerHandler===>channelRead0");
            if (!req.decoderResult().isSuccess()) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            String uri = req.uri();
            String path = URLDecoder.decode(uri.substring(1), StandardCharsets.UTF_8.name());
            if (req.method() == HttpMethod.GET) {
                handleGetRequest(ctx, path);
            } else if (req.method() == HttpMethod.POST) {
                handlePostRequest(ctx, path, req);
            } else if (req.method() == HttpMethod.DELETE) {
                handleDeleteRequest(ctx, path);
            } else {
                sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
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
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status,
                    io.netty.buffer.Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", StandardCharsets.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

}
