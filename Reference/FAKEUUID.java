/* Clark Elliott 1.0 2022-12-27

Generate a reasonably useful approximation of a UUID (real UUIDs use hex).

Thanks to Walter Hogan and...
https://www.baeldung.com/java-generating-random-numbers-in-range

Output: Fake UUID is: 19085665-6580-6992-3424-851252305578

*/


import java.io.*;  
import java.util.*;

public class FakeUUID {

  static int getRandomNumber (int min, int max){
    return (int) ((Math.random() * (max - min)) + min);
  }


  public static void main(String a[]) throws IOException {
    String uuid = null;
    Random rn = new Random();

    int int1 = getRandomNumber(10000000, 99999999);
    int int2 = getRandomNumber(1000, 9999);
    int int3 = getRandomNumber(1000, 9999);
    int int4 = getRandomNumber(1000, 9999);
    int int5 = getRandomNumber(100000, 999999);
    int int6 = getRandomNumber(100000, 999999);

    uuid = int1 + "-" + int2 + "-" + int3 + "-" + int4 + "-" + int5 + int6;
    System.out.println("Fake UUID is: " + uuid);
    
  }
}


