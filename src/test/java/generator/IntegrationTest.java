package generator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTest {

    @ParameterizedTest
    @MethodSource("provideTestDirectoryNames")
    public void integrationTest(String directoryName) throws IOException {
        String pathPrefix = "./src/test/resources/" + directoryName;

        String inFileName = pathPrefix + "/test.in";
        String outFileName = pathPrefix + "/test.out";
        String myFileName = pathPrefix + "/test.my";
        String myOutFileName = pathPrefix + "/test.my.out";
        String myErrFileName = pathPrefix + "/test.my.err";

        //run generator
        try(InputStream input = new FileInputStream(inFileName);
            PrintStream output = new PrintStream(new FileOutputStream(myFileName))){

            GeneratorKoda sa = new GeneratorKoda(input, output);

            sa.analyzeInputAndGenerateCode();
        }

        ProcessBuilder builder = new ProcessBuilder("node", "./frisc/main.js", myFileName);
        builder.redirectOutput(new File(myOutFileName));
        builder.redirectError(new File(myErrFileName));
        Process p = builder.start();

        while(p.isAlive()) {
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        String correctOutput = Files.readString(Path.of(outFileName));
        String myOutput = Files.readString(Path.of(myOutFileName));

        assertEquals(normalizeString(correctOutput), normalizeString(myOutput));
    }


    private static Stream<Arguments> provideTestDirectoryNames() {

        File[] officialExamplesDirectories
                = new File("./src/test/resources/test-examples/official-test-examples").listFiles(File::isDirectory);
        File[] exampleDirectories1112
                = new File("./src/test/resources/test-examples/11-12-test-examples").listFiles(File::isDirectory);
        File[] exampleDirectories2021
                = new File("./src/test/resources/test-examples/20-21-test-examples").listFiles(File::isDirectory);
        File[] exampleDirectories2122
                = new File("./src/test/resources/test-examples/21-22-test-examples").listFiles(File::isDirectory);


        List<String> args = new ArrayList<>();

        for(File file: exampleDirectories1112){
            args.add("test-examples/11-12-test-examples/" +file.getName());
        }

        for(File file: officialExamplesDirectories){
            args.add("test-examples/official-test-examples/" +file.getName());
        }

        for(File file: exampleDirectories2021){
            args.add("test-examples/20-21-test-examples/" +file.getName());
        }

        for(File file: exampleDirectories2122) {
            args.add("test-examples/21-22-test-examples/" + file.getName());
        }

        return args.stream().map(Arguments::of);
    }

    public static String normalizeString(String s){
        return s.replace("\r\n", "\n")
                .replace("\r", "\n").trim();
    }
}
