package example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RestMain {
  private static Map<String, String> parseQuery(URI uri) {
    Map<String, String> m = new LinkedHashMap<>();
    String q = uri.getRawQuery();
    if (q == null || q.isEmpty()) return m;
    for (String part : q.split("&")) {
      int i = part.indexOf('=');
      if (i < 0) m.put(decode(part), "");
      else m.put(decode(part.substring(0, i)), decode(part.substring(i + 1)));
    }
    return m;
  }

  private static String decode(String s) {
    try { return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8); }
    catch (Exception e) { return s; }
  }

  private static void send(HttpExchange ex, int status, String body, String contentType) throws IOException {
    byte[] b = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
    ex.sendResponseHeaders(status, b.length);
    try (OutputStream os = ex.getResponseBody()) { os.write(b); }
  }

  public static void main(String[] args) throws Exception {
    int port = 8080;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

    server.createContext("/greet", new HttpHandler() {
      public void handle(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String name = q.getOrDefault("name", "world");
        String out = demo.rest.greet_fn.greet(name);
        send(ex, 200, out + "\n", "text/plain");
      }
    });

    server.createContext("/exclaim", new HttpHandler() {
      public void handle(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String text = q.getOrDefault("text", "");
        String out = demo.rest.exclaim_fn.exclaim(text);
        send(ex, 200, out + "\n", "text/plain");
      }
    });

    server.createContext("/length", new HttpHandler() {
      public void handle(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI());
        String text = q.getOrDefault("text", "");
        int n = demo.rest.length_fn.length(text);
        send(ex, 200, Integer.toString(n) + "\n", "text/plain");
      }
    });

    server.start();
    System.out.println("REST server listening on http://localhost:" + port);
    System.out.println("Endpoints: /greet?name=Alice, /exclaim?text=wow, /length?text=hello");
  }
}

