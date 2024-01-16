/* 1.0 Example: generating three executable .class files from one .java source code file

Name: Dr. Clark Elliott
Data: 2023-03-25
Java Version: 20 (build 20+36-2344)

Command-line compilation: > javac JokeServerX.java [or > javac *.java]

Running these three resultant processes:

> java JokeServerX
> java JokeClientX
> java JokeClientAdminX

*/
  
import java.io.*;

class JokeClientX {
  public static void main(String argv[]) {
    JokeClientX jc = new JokeClientX(argv);
    jc.run(argv);
  }
  
  public JokeClientX(String argv[]) { // Constructor
    System.out.println("\nThis is the JokeClientX constructor if you want to use it.\n");
  }

  public void run(String argv[]) {
    System.out.println("Hello from inside the JokeClientX process\n");
  }
}

class JokeClientAdminX {
  public static void main(String argv[]) {
    JokeClientAdminX jca = new JokeClientAdminX(argv);
    jca.run(argv);
  }
  
  public JokeClientAdminX(String argv[]) { // Constructor
    System.out.println("\nThis is the JokeClientAdminX constructor if you want to use it.\n");
  }

  public void run(String argv[]) {
    System.out.println("Hello from inside the JokeClientX process\n");
  }
}

class JokeServerX {
  public static void main(String argv[]) {
    JokeServerX js = new JokeServerX(argv);
    js.run(argv);
  }
  
  public JokeServerX(String argv[]) { // Constructor
    System.out.println("\nThis is the JokeServerX constructor if you want to use it.\n");
  }

  public void run(String argv[]) {
    System.out.println("Hello from inside the JokeServerX process\n");
  }
}
/*---------------  MY JOKESERVER POSTINGS ON THE D2L FORUMS: ----------------

[Your two or more postings go here.]

 Be careful: all the plagiarism rules apply to the this section as well. All postings here MUST
 have appeared on the class D2L forums.

*/

/*--------------- MY JOKELOG.TXT File:

[Your JokeLog.txt file is copied here.]

--------------------------------------------------------------------------- */



/* Example runs of each named process for this code:

> java JokeServerX
java JokeServerX

This is the JokeServerX constructor if you want to use it.

Hello from inside the JokeServerX process

> java JokeClientX
java JokeClientX

This is the JokeClientX constructor if you want to use it.

Hello from inside the JokeClientX process


> java JokeClientAdminX
Java JokeClientAdminX

This is the JokeClientAdminX constructor if you want to use it.

Hello from inside the JokeClientX process

>

*/
