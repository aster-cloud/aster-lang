package example;

public final class PolicyMain {
  private static void usage() {
    System.out.println("Usage: policy <role> <action> <ownerId> <userId>\n" +
        "Example: policy user Read u123 u123\n" +
        "         policy user Write u123 u456\n" +
        "         policy admin Delete u123 u999");
  }

  public static void main(String[] args) {
    if (args.length < 4) { usage(); return; }
    String role = args[0];
    String actionStr = args[1];
    String ownerId = args[2];
    String userId = args[3];
    try {
      demo.policy.Action action = demo.policy.Action.valueOf(actionStr);
      boolean allowed = demo.policy.canAccess_fn.canAccess(role, action, ownerId, userId);
      System.out.println(allowed ? "ALLOW" : "DENY");
    } catch (IllegalArgumentException ex) {
      System.err.println("Unknown action: " + actionStr + " (expected: Read, Write, Delete)");
      System.exit(2);
    } catch (Throwable t) {
      System.err.println("Policy error: " + t.getMessage());
      t.printStackTrace(System.err);
      System.exit(1);
    }
  }
}

