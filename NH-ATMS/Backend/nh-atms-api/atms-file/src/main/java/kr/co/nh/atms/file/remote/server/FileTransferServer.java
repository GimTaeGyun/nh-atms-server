package kr.co.nh.atms.file.remote.server;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import kr.co.nh.atms.file.remote.util.SafeClose;

/**
 * FileTransferServer
 * 구 새마을금고 atmf.lib (atmf.server.Server) 이식.
 * 정적(static) 구조를 Spring SmartLifecycle 인스턴스 빈으로 변환.
 * accept/L4 차단/세션 관리/종료 대기 로직은 구 Server.bgn / Server.end 와 동일(무변경).
 *
 * atmf.server.enabled=true 일 때만 기동 시 포트를 bind 한다 (기본 false).
 */
@Component
public class FileTransferServer implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileTransferServer.class);

    private final FileServerProperties props;

    private ThreadPoolExecutor executor;
    private AsynchronousServerSocketChannel server = null;
    private final Set<Client> clients = new HashSet<>();
    private ServerContext ctx;

    private volatile boolean running = false;

    public FileTransferServer(FileServerProperties props) {
        this.props = props;
    }

    @Override
    public boolean isAutoStartup() {
        return props.isEnabled();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // 웹 컨테이너 이후 기동, 종료 시 먼저 내림
        return Integer.MAX_VALUE;
    }

    /**
     * start (구 Server.bgn 이식)
     */
    @Override
    public void start() {
        if (running) {
            return;
        }
        try {
            LOGGER.info(""
                + "[bgn]\n"
                + "bind={}:{}, session.buf={}, session.max={}, repository={}, l4={}"
                , props.getBindIp(), props.getPort(), props.getBuf(), props.getMax(), props.getRepository(), props.getL4Allowlist()
            );

            {
                int buf_size = props.getBuf();
                int buf_max = props.getMax();

                buf_size = (((buf_size - 1) / 128) + 1) * 128;
                ctx = new ServerContext(buf_size, buf_max, props.getRepository());

                int avg_handling_time_ms = 100;
                int through_put_thread_per_sec = 1000 / avg_handling_time_ms;
                int estimated_threads = buf_max / through_put_thread_per_sec;
                executor = new ThreadPoolExecutor(
                    estimated_threads / 2
                    , estimated_threads
                    , 60L
                    , TimeUnit.SECONDS, new LinkedBlockingQueue<>(buf_max)
                    , Executors.defaultThreadFactory()
                );
                AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);
                server = AsynchronousServerSocketChannel.open(group);
                server.setOption(StandardSocketOptions.SO_REUSEADDR, true);

                InetSocketAddress bind = new InetSocketAddress(props.getBindIp(), props.getPort());

                server.bind(bind);
            }

            final AsynchronousServerSocketChannel _server = server;
            final ServerContext _ctx = ctx;

            final List<String> l4 = props.getL4Allowlist();
            _server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

                @Override
                public void completed(AsynchronousSocketChannel channel, Void attachment) {
                    try {
                        _server.accept(null, this);
                        InetSocketAddress addr = (InetSocketAddress) channel.getRemoteAddress();
                        String ip = addr.getAddress().getHostAddress();
                        for (int i = 0; i < l4.size(); i++) {
                            if (ip.equals(l4.get(i))) {
                                CompletableFuture.runAsync(() -> {
                                    SafeClose.close(channel);
                                });
                                return;
                            }
                        }

                        Client client = Client.attach(channel, _ctx, (_this) -> {
                            int[] clients_size = {0};
                            synchronized (clients) {
                                clients.remove(_this);
                                clients_size[0] = clients.size();
                            }
                            LOGGER.info(String.format("[clients][rem][%05d][%s]", clients_size[0], _this.address()));
                        });
                        {
                            int[] clients_size = {0};
                            synchronized (clients) {
                                clients.add(client);
                                clients_size[0] = clients.size();
                            }
                            LOGGER.info(String.format("[clients][put][%05d][%s]", clients_size[0], client.address()));
                        }
                        client.handshakeAsync();

                    } catch (Exception e) {
                        SafeClose.close(channel);
                    }
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                }
            });

            running = true;
        } catch (Throwable e) {
            SafeClose.close(server);
            server = null;
            LOGGER.error("[runtime exception]", e);
            LOGGER.info("[end]");
        }
    }

    /**
     * stop (구 Server.end 이식)
     */
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            server = SafeClose.close(server);

            executor.shutdown();

            synchronized (clients) {
                for (Client client : clients) {
                    client.close();
                }
            }

            int cnt = 10;
            while (true) {
                synchronized (clients) {
                    if (clients.size() == 0) {
                        break;
                    }
                }
                if (cnt-- <= 0) {
                    break;
                }
                Thread.sleep(500);
            }

            executor.awaitTermination(1000 * 5, TimeUnit.MILLISECONDS);

        } catch (Throwable e) {
            LOGGER.error("[end fail]", e);
        } finally {
            SafeClose.close(server);
            server = null;
            LOGGER.info("[end]");
        }
    }
}
