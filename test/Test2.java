import java.io.FileNotFoundException;
import easyIO.*;

/** A simple test program for Scanner. */
public class Test2 {

	public static void main(String[] args) throws FileNotFoundException {
		
		Scanner s = new Scanner(args[0]);
			
		while (true) {
			s.whitespace();
			try {
				double n = s.nextDouble();
				System.out.println("Read number: " + n);
			} catch (UnexpectedInput e) {	
				System.out.println("Unexpected input at: " + s.location());
				break;
			}
		}
	}
}
