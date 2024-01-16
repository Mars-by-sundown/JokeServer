/* Clark Elliott 1.0 2022-04-13

Simple method to change or read a mode, accessible by any other methods running in threads
(e.g., JokeWorker and AdminWorker threads).

Output:

> java ModeChanger

Mode is 0

Mode is 1

*/

import java.io.*;  // Get the Input Output libraries

class ToggleMode{
    int Mode = 0; // The class variable storing the mode
  
    public int SetMode () {    // Method definition to toggle the mode
	if (Mode==0) {Mode = 1;}
	else {Mode = 0;}
	return (Mode);
    }

    public int GetMode () {    // Method definition to read the mode
	return (Mode);
    }
  
}

public class ModeChanger {
  
    public static void main(String a[]) throws IOException {
    
	ToggleMode ModeInstance = new ToggleMode(); // Create the Object
	System.out.println("\nMode is " + ModeInstance.GetMode() + "\n");
	ModeInstance.SetMode(); // Toggle the mode
	System.out.println("Mode is " + ModeInstance.GetMode() + "\n");
    }
}
