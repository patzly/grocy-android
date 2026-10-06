package xyz.zedler.patrick.grocy.test;

import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.robolectric.Shadows;

public class TestServer implements AutoCloseable {
  public final List<Request> requests = new CopyOnWriteArrayList<>();
  private final ServerSocket server;
  private final Thread worker;

  public TestServer(Handler handler) throws IOException {
    server = new ServerSocket(0, 10, java.net.InetAddress.getByName("127.0.0.1"));
    worker = new Thread(() -> {
      while (!server.isClosed()) {
        try (Socket socket = server.accept()) {
          socket.setSoTimeout(5000);
          Request request = new Request(socket);
          requests.add(request);
          handler.handle(request);
        } catch (IOException error) {
          if (!server.isClosed()) {
            throw new AssertionError(error);
          }
        }
      }
    });
    worker.setDaemon(true);
    worker.start();
  }

  public String url() {
    return "http://127.0.0.1:" + server.getLocalPort();
  }

  public static void await(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.nanoTime() + 10_000_000_000L;
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      Thread.sleep(10);
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    if (!condition.getAsBoolean()) {
      throw new AssertionError("Timed out waiting for network callback");
    }
  }

  @Override public void close() throws IOException {
    server.close();
  }

  public interface Handler {
    void handle(Request request) throws IOException;
  }

  public static class Request {
    public final String method;
    public final String path;
    public final String body;
    public final String query;
    private final Socket socket;

    private Request(Socket socket) throws IOException {
      this.socket = socket;
      ByteArrayOutputStream headers = new ByteArrayOutputStream();
      String header;
      do {
        int value = socket.getInputStream().read();
        if (value < 0) throw new IOException("Incomplete HTTP request");
        headers.write(value);
        header = headers.toString(StandardCharsets.UTF_8);
      } while (!header.endsWith("\r\n\r\n"));
      String[] lines = header.split("\r\n");
      String[] first = lines[0].split(" ");
      method = first[0];
      URI uri = URI.create(first[1]);
      path = uri.getPath();
      query = uri.getQuery();
      int length = 0;
      for (String line : lines) {
        if (line.toLowerCase().startsWith("content-length:")) {
          length = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
        }
      }
      body = new String(socket.getInputStream().readNBytes(length), StandardCharsets.UTF_8);
    }

    public void respond(int status, String json) throws IOException {
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      String headers = "HTTP/1.1 " + status + " Test\r\nContent-Type: application/json\r\n"
          + "Connection: close\r\nContent-Length: " + bytes.length + "\r\n\r\n";
      socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));
      socket.getOutputStream().write(bytes);
      socket.getOutputStream().flush();
    }
  }
}
