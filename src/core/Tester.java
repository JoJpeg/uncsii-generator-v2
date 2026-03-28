package core;

public class Tester {
    public static void main(String[] args) {
        /// Users/jonaseschner/Downloads/image.jpeg
        args = new String[]{"-b", "-png", "-dark", "-s=1", "/Users/jonaseschner/Documents/image.png"};
        test(args); 
    }

    private static void test(String[] args) {
        try {
            App.main(args);
        } catch (Exception e) { 
            e.printStackTrace();
        }
    }
}
