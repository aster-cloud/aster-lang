package example;

public final class PolicyMain {
  private static void usage() {
    System.out.println("Usage: policy <command> [args...]\n" +
        "Commands:\n" +
        "  basic <role> <action> <ownerId> <userId>  - Basic policy check\n" +
        "  demo                                      - Run policy demonstrations\n" +
        "  complex <userId> <userRole> <resourceOwner> <resourceType> <timeOfDay> <location> - Complex policy check\n" +
        "\n" +
        "Examples:\n" +
        "  policy basic user Read u123 u123\n" +
        "  policy basic admin Delete u123 u999\n" +
        "  policy demo\n" +
        "  policy complex user123 user user123 Document 14.5 office");
  }

  public static void main(String[] args) {
    if (args.length == 0) { 
      usage(); 
      return; 
    }
    
    String command = args[0];
    
    try {
      switch (command) {
        case "basic":
          runBasicPolicy(args);
          break;
        case "demo":
          runPolicyDemo();
          break;
        case "complex":
          runComplexPolicy(args);
          break;
        default:
          System.err.println("Unknown command: " + command);
          usage();
          System.exit(1);
      }
    } catch (Throwable t) {
      System.err.println("Error: " + t.getMessage());
      t.printStackTrace(System.err);
      System.exit(1);
    }
  }
  
  private static void runBasicPolicy(String[] args) {
    if (args.length < 5) {
      System.err.println("Usage: policy basic <role> <action> <ownerId> <userId>");
      System.exit(1);
    }
    
    String role = args[1];
    String actionStr = args[2];
    String ownerId = args[3];
    String userId = args[4];
    
    try {
      demo.policy.Action action = demo.policy.Action.valueOf(actionStr);
      boolean allowed = demo.policy.canAccess_fn.canAccess(role, action, ownerId, userId);
      System.out.println(allowed ? "ALLOW" : "DENY");
    } catch (IllegalArgumentException ex) {
      System.err.println("Unknown action: " + actionStr + " (expected: Read, Write, Delete, Admin)");
      System.exit(2);
    }
  }
  
  private static void runPolicyDemo() {
    System.out.println("=== Policy Engine Demonstration ===");
    String result = demo.policy_demo.demonstratePolicyEngine_fn.demonstratePolicyEngine();
    System.out.println("Policy Demo Result: " + result);
    
    System.out.println("\n=== Policy Tests ===");
    System.out.println("Test 1: " + demo.policy_demo.runPolicyTest1_fn.runPolicyTest1());
    System.out.println("Test 2: " + demo.policy_demo.runPolicyTest2_fn.runPolicyTest2());
    System.out.println("Test 3: " + demo.policy_demo.runPolicyTest3_fn.runPolicyTest3());
    System.out.println("Test 4: " + demo.policy_demo.runPolicyTest4_fn.runPolicyTest4());
    System.out.println("Test 5: " + demo.policy_demo.runPolicyTest5_fn.runPolicyTest5());
  }
  
  
  private static void runComplexPolicy(String[] args) {
    if (args.length < 6) {
      System.err.println("Usage: policy complex <userId> <userRole> <resourceOwner> <resourceType> <timeOfDay>");
      System.exit(1);
    }
    
    String userId = args[1];
    String userRole = args[2];
    String resourceOwner = args[3];
    String resourceType = args[4];
    double timeOfDay = Double.parseDouble(args[5]);
    
    // Create a PolicyContext and test with evaluateUserReadRule
    demo.policy.PolicyContext context = new demo.policy.PolicyContext(
        userId, userRole, resourceOwner, 
        demo.policy.Resource.valueOf(resourceType), 
        timeOfDay, "office");
    
    boolean result = demo.policy.evaluateUserReadRule_fn.evaluateUserReadRule(context);
    System.out.println(result ? "Access granted" : "Access denied");
  }
}

