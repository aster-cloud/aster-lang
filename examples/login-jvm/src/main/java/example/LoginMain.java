package example;

public final class LoginMain {
  public static void main(String[] args) {
    var r1 = app.service.login_fn.login("bob", "secret");
    System.out.println("login bob/secret => " + r1);
    var r2 = app.service.login_fn.login("bob", "nope");
    System.out.println("login bob/nope => " + r2);
  }
}

