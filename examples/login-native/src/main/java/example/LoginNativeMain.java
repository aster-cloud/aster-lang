package example;

public final class LoginNativeMain {
  public static void main(String[] args) {
    var ok = app.service.login_fn.login("bob", "secret");
    var err = app.service.login_fn.login("bob", "nope");
    System.out.println("OK => " + ok);
    System.out.println("ERR => " + err);
  }
}

