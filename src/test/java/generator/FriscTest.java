package generator;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FriscTest {

    @Test
    public void testFrisc() throws IOException {
        String fileName = "./src/test/resources/example.frisc";

        String[] friscArguments = new String[] {"node",  "./frisc/main.js", fileName};
        Process proc = Runtime.getRuntime().exec(friscArguments);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(proc.getInputStream()));
        while(reader.ready()){
            System.out.println(reader.readLine());
        }
        String myOutput = reader.readLine();

        while(proc.isAlive()) {
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println(myOutput);
    }
}
