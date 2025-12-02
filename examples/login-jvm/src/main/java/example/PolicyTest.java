package example;

import demo.policy.Action;

public final class PolicyTest {
  public static void main(String[] args) {
    System.out.println("=== Policy Engine Demonstration ===");
    
    // Test basic policy functions
    try {
      // Test canAccess function
      boolean adminAccess = demo.policy.canAccess_fn.canAccess("admin", Action.Read, "user123", "user456");
      System.out.println("Admin read access: " + (adminAccess ? "GRANTED" : "DENIED"));
      
      boolean userOwnAccess = demo.policy.canAccess_fn.canAccess("user", demo.policy.Action.Read, "user123", "user123");
      System.out.println("User read own resource: " + (userOwnAccess ? "GRANTED" : "DENIED"));
      
      boolean userOtherAccess = demo.policy.canAccess_fn.canAccess("user", demo.policy.Action.Read, "user123", "user456");
      System.out.println("User read other resource: " + (userOtherAccess ? "GRANTED" : "DENIED"));
      
      // Test PolicyContext and evaluateUserReadRule
      demo.policy.PolicyContext context1 = new demo.policy.PolicyContext(
          "user123", "user", "user123", demo.policy.Resource.Document, aster.runtime.Primitives.number(14.5), "office");
      boolean result1 = demo.policy.evaluateUserReadRule_fn.evaluateUserReadRule(context1);
      System.out.println("User reads own document: " + (result1 ? "GRANTED" : "DENIED"));
      
      demo.policy.PolicyContext context2 = new demo.policy.PolicyContext(
          "user456", "user", "user123", demo.policy.Resource.Document, aster.runtime.Primitives.number(14.5), "office");
      boolean result2 = demo.policy.evaluateUserReadRule_fn.evaluateUserReadRule(context2);
      System.out.println("User reads other's document: " + (result2 ? "GRANTED" : "DENIED"));
      
      // Test policy demo functions
      // NOTE: demo.policy_demo module removed - these tests are commented out
      // System.out.println("\n=== Policy Demo Functions ===");
      // System.out.println("Demo result: " + demo.policy_demo.demonstratePolicyEngine_fn.demonstratePolicyEngine());
      // System.out.println("Test 1: " + demo.policy_demo.runPolicyTest1_fn.runPolicyTest1());
      // System.out.println("Test 2: " + demo.policy_demo.runPolicyTest2_fn.runPolicyTest2());
      // System.out.println("Test 3: " + demo.policy_demo.runPolicyTest3_fn.runPolicyTest3());

    } catch (Exception e) {
      System.err.println("Error running policy tests: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
