import org.junit.Test;
import se.romram.helpers.SimpleJson;
import se.romram.helpers.StopWatch;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by micke on 2015-02-12.
 */
public class SimpleJsonTest {

    @Test
    public void testStringJson() {
        SimpleJson simpleJson = new SimpleJson("\"string\"");
        System.out.println(simpleJson.toString(3));
    }

    @Test
    public void testNumberJson() {
        SimpleJson simpleJson = new SimpleJson("1234");
        System.out.println(simpleJson.toString(3));
    }

    @Test
    public void testComplexJson() throws IOException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/complex.json");
        String json = new String(Files.readAllBytes(path));
        StopWatch stopWatch = new StopWatch().start();
        SimpleJson simpleJson = new SimpleJson(json);
        stopWatch.stop();
        System.out.println("Parsing took " + stopWatch.getTotalTime() + " ms.");
        System.out.println(simpleJson.toString(4));
        System.out.println(simpleJson.get("menu").get("popup").toString(4));
        System.out.println(simpleJson.get("menu").get("popup").get("menuitem").toString(4));
        System.out.println(simpleJson.get("menu").get("popup").get("menuitem").get(1).toString(4));
        System.out.println(simpleJson.toString(4));

    }

    @Test
    public void testArrayJson() throws IOException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/array.json");
        String json = new String(Files.readAllBytes(path));
        StopWatch stopWatch = new StopWatch().start();
        SimpleJson simpleJson = new SimpleJson(json);
        stopWatch.stop();
        System.out.println("Parsing took " + stopWatch.getTotalTime() + " ms.");
        System.out.println(simpleJson.toString(4));
        System.out.println((Number) simpleJson.get(1).toObject());

    }

}
