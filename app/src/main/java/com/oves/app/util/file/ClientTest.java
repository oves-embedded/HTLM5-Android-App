//package com.oves.app.util.file;
//
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
//import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.handler.codec.http.DefaultFullHttpRequest;
//import io.netty.handler.codec.http.FullHttpResponse;
//import io.netty.handler.codec.http.HttpClientCodec;
//import io.netty.handler.codec.http.HttpContentDecompressor;
//import io.netty.handler.codec.http.HttpHeaderNames;
//import io.netty.handler.codec.http.HttpHeaderValues;
//import io.netty.handler.codec.http.HttpMethod;
//import io.netty.handler.codec.http.HttpObjectAggregator;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.HttpVersion;
//import io.netty.handler.logging.LogLevel;
//import io.netty.handler.logging.LoggingHandler;
//import io.netty.handler.ssl.SslContext;
//import io.netty.handler.ssl.SslContextBuilder;
//import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
//import io.netty.util.CharsetUtil;
//
//public class ClientTest {
//
//    private String urlStr;
//
//
//    public ClientTest(String urlStr) {
//        this.urlStr = urlStr;
//    }
//
//
//    public void start() throws InterruptedException {
//
//        //线程组
//        EventLoopGroup group = new NioEventLoopGroup();
//        //启动类
//        Bootstrap bootstrap = new Bootstrap();
//        try {
//            InetSocketAddress inetAddress = null;
//            URI uri = new URI(urlStr);
//            if (Objects.isNull(uri)) {
//                return;
//            }
//            boolean isSSL = urlStr.contains("https");
//            try {
//                URL url = new URL(urlStr);
//                String host = url.getHost();
//                InetAddress address = InetAddress.getByName(host);
//                if (!host.equalsIgnoreCase(address.getHostAddress())) {
//                    //域名连接,https默认端口是443，http默认端口是80
//                    inetAddress = new InetSocketAddress(address, isSSL ? 443 : 80);
//                } else {
//                    //ip+端口连接
//                    int port = url.getPort();
//                    inetAddress = InetSocketAddress.createUnresolved(host, port);
//                }
//            } catch (Throwable e) {
//                return;
//            }
//            bootstrap.group(group)
//                    .remoteAddress(inetAddress)
//                    .channel(NioSocketChannel.class)
//                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
//                    .option(ChannelOption.TCP_NODELAY, true)
//                    //长连接
//                    .option(ChannelOption.SO_KEEPALIVE, true)
//                    .handler(new LoggingHandler(LogLevel.ERROR))
//
//                    .handler(new ChannelInitializer<Channel>() {
//                        @Override
//                        protected void initChannel(Channel channel) throws Exception {
//                            System.out.println("channelCreated. Channel ID：" + channel.id());
//                            SocketChannel socketChannel = (SocketChannel) channel;
//                            socketChannel.config().setKeepAlive(true);
//                            socketChannel.config().setTcpNoDelay(true);
//                            if (isSSL) { //配置Https通信
//                                SslContext context = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//                                channel.pipeline().addLast(context.newHandler(channel.alloc()));
//                            }
//                            socketChannel.pipeline()
//                                    //包含编码器和解码器
//                                    .addLast(new HttpClientCodec())
//                                    //聚合
//                                    .addLast(new HttpObjectAggregator(1024 * 10 * 1024))
//                                    //解压
//                                    .addLast(new HttpContentDecompressor())
//                                    //添加ChannelHandler
//                                    .addLast(new ChannelInboundHandlerAdapter(){
//                                        /**
//                                         * 客户端与服务端建立连接时执行
//                                         * @param ctx
//                                         * @throws Exception
//                                         */
//                                        @Override
//                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
//                                            //发送请求至服务端
//                                            Map<String,String> header = new HashMap<>();
//                                            header.put("Cache-Control","no-cache");
//                                            header.put("User-Agent","PostmanRuntime/7.43.3");
//                                            header.put("Accept","*/*");
//                                            header.put("Accept-Encoding","gzip, deflate, br");
//                                            String  url ="https://wvapp.omnivoltaic.com/_next/static/chunks/app/page-6daa79ca68680d6d.js";
//                                            //配置HttpRequest的请求数据和一些配置信息
//                                            HttpRequest request = buildRequest("",url,true,header);
//                                            ChannelFuture future = ctx.writeAndFlush(request);
//
//                                        }
//
//                                        @Override
//                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//                                            FullHttpResponse response = (FullHttpResponse) msg;
//                                            ByteBuf content = response.content();
//                                            System.out.println(": content:"+content.toString(CharsetUtil.UTF_8));
//                                        }
//
//                                    });
//
//                        }
//                    });
//
//            ChannelFuture channelFuture = bootstrap.connect().sync();
//            channelFuture.channel().closeFuture().sync();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            group.shutdownGracefully();
//        }
//    }
//
//
//    public  HttpRequest buildRequest(String msg, String url, boolean isKeepAlive, Map<String,String> headers) throws Exception {
//        URL netUrl = new URL(url);
//        URI uri = new URI(netUrl.getPath());
//        //构建http请求
//        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
//                HttpMethod.POST,
//                uri.toASCIIString(),
//                Unpooled.wrappedBuffer(msg.getBytes(StandardCharsets.UTF_8)));
//
//        //设置请求的host(这里可以是ip,也可以是域名)
//        request.headers().set(HttpHeaderNames.HOST, netUrl.getHost());
//        //其他头部信息
//        if (headers != null && !headers.isEmpty()) {
//            for (Map.Entry<String, String> entry : headers.entrySet()) {
//                request.headers().set(entry.getKey(), entry.getValue());
//            }
//        }
//        //设置返回Json
//        request.headers().set(HttpHeaderNames.CONTENT_TYPE ,"text/json;charset=UTF-8");
//        //发送的长度
//        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
//        //是否是长连接
//        if (isKeepAlive){
//            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        }
//
//        return request;
//    }
//
//    public static void main(String[] args) throws InterruptedException {
//        ClientTest client = new ClientTest("https://wvapp.omnivoltaic.com/_next/static/chunks/app/page-6daa79ca68680d6d.js");
//        client.start();
//    }
//}
