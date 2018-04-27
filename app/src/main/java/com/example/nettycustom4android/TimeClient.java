package com.example.nettycustom4android;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class TimeClient {

    private static TimeClient client = new TimeClient();

    private EventLoopGroup group;

    private Channel channel;

    private boolean isConnect = false;

    private int reconnectNum = Integer.MAX_VALUE;

    private long reconnectIntervalTime = 5000;

    public static TimeClient getInstance(){
        return  client;
    }

    public synchronized void connect(int port, String host)  {

        // 配置客户端NIO线程组
        group = new NioEventLoopGroup();
        try{
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 核心在下面两行，加入了LineBasedFrameDecoder和StringDecoder两个解码器
                        // 所以当消息到达我们的业务处理handler即TimerServerHandler，所看到的消息
                        // 都是前面两个解码器经过处理之后的结果
                        ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new TimeClientHandler());
                    }
                });
            // 发起异步连接操作
            // 发起异步连接操作
            ChannelFuture f = b.connect(host, port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        isConnect = true;
                        channel = channelFuture.channel();
                    } else {
                        isConnect = false;
                    }
                }
            }).sync();

            // 等待客户端链路关闭
//            f.channel().closeFuture().sync();
        }catch(Exception e){
            e.printStackTrace();
            reconnect();
        }
    }

    public void disconnect() {
        group.shutdownGracefully();
    }

    public void reconnect() {
        if(reconnectNum >0 && !isConnect){
            reconnectNum--;
            try {
                Thread.sleep(reconnectIntervalTime);
            } catch (InterruptedException e) {}
            disconnect();
            connect(8080, "192.168.1.2");
        }else{
            disconnect();
        }
    }

    public boolean sendMsgToServer(byte[] data, ChannelFutureListener listener) {
        boolean flag = channel != null && isConnect;
        if (flag) {
            ByteBuf buf = Unpooled.buffer(data.length);
            buf.writeBytes(data);
//            ByteBuf buf = Unpooled.copiedBuffer(data);
            channel.writeAndFlush(buf).addListener(listener);
        }
        return flag;
    }

    public void setReconnectNum(int reconnectNum) {
        this.reconnectNum = reconnectNum;
    }

//    public static void main(String[] args) throws Exception {
//        int port = 8080;
//        if(args != null && args.length > 0) {
//            try {
//                port = Integer.valueOf(port);
//            } catch (NumberFormatException e) {
//                // 采用默认值
//            }
//        }
//        new TimeClient().connect(port, "localhost");
//    }
}
