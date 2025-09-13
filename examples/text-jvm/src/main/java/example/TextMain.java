package example;

public final class TextMain {
  public static void main(String[] args) {
    System.out.println("join => " + demo.text.join_fn.join("hello", " world"));
    System.out.println("shout => " + demo.text.shout_fn.shout("Hello"));
    System.out.println(
        "hasPrefix('foobar','foo') => " + demo.text.hasPrefix_fn.hasPrefix("foobar", "foo"));
    System.out.println("find('fooz', 'z') => " + demo.text.find_fn.find("fooz", "z"));
    System.out.println("words('a b c') => " + demo.text.words_fn.words("a b c"));
  }
}

