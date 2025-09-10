package app.service;

public final class AuthRepo {
  private AuthRepo(){}
  public static boolean verify(String user, String pass) {
    return user != null && pass != null && !user.isEmpty() && pass.equals("secret");
  }
}

