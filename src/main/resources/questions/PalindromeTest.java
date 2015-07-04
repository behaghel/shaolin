import org.junit.*;
import static org.junit.Assert.*;

public class PalindromeTest {
    @Test
    public void testIsPalindrome() {
        // assertTrue("Empty string", Palindrome.isPalindrome(""));
        assertTrue("Single word, lower case", Palindrome.isPalindrome("radar"));
        assertTrue("Single word, any case", Palindrome.isPalindrome("RadAr"));
        assertTrue("Sentence", Palindrome.isPalindrome("Max, I stay away at six A.M."));
        assertTrue("Sentence", Palindrome.isPalindrome("Noel, did I not rub Burton? I did, Leon."));
        //FIXME: test non-palindrome words/sentences
    }
}
