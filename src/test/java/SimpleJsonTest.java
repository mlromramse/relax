import static org.junit.Assert.*;

import org.junit.Test;
import se.romram.helpers.SimpleJson;
import se.romram.helpers.StopWatch;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

/**
 * Created by micke on 2015-02-12.
 */
public class SimpleJsonTest {

    @Test
    public void testStringJson() throws ParseException {
        String jsonString = "\"string\"";
        SimpleJson simpleJson = new SimpleJson(jsonString);
        assertEquals("String only:", jsonString, simpleJson.toString(0));
    }

    @Test
    public void testNumberJson() throws ParseException {
        SimpleJson simpleJson = new SimpleJson("1234");
        assertEquals("Number only:", "1234", simpleJson.toString(3));
    }

    @Test
    public void testComplexJson() throws IOException, ParseException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/complex.json");
        String json = new String(Files.readAllBytes(path));
        StopWatch stopWatch = new StopWatch().start();
        SimpleJson simpleJson = new SimpleJson(json);
        stopWatch.stop();
        System.out.println("Parsing took " + stopWatch.getTotalTime() + " ms.");
//        System.out.println(simpleJson.toString(4));
//        System.out.println(simpleJson.get("menu").get("popup").toString(4));
//        System.out.println(simpleJson.get("menu").get("popup").get("menuitem").toString(4));
//        System.out.println(simpleJson.get("menu").get("popup").get("menuitem").get(1).toString(4));
//        System.out.println(simpleJson.toString(4));
        assertEquals(false, simpleJson.get("menu").get("popup").get("menuitem").get(1).get("active").toObject());
        assertEquals("another", simpleJson.get("array").get(2).toObject());
    }

    @Test
    public void testArrayJson() throws IOException, ParseException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/array.json");
        String json = new String(Files.readAllBytes(path));
        StopWatch stopWatch = new StopWatch().start();
        SimpleJson simpleJson = new SimpleJson(json);
        stopWatch.stop();
        assertEquals("String: This is sentence[1]. You can {} with this, and this.", simpleJson.get(0).toObject());
        assertEquals(1234L, simpleJson.get(1).toObject());
        assertEquals(true, simpleJson.get(2).toObject());
        assertEquals(false, simpleJson.get(3).toObject());
        assertEquals(null, simpleJson.get(4).toObject());

    }

    @Test
    public void testBrokenJson() throws IOException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/broken.json");
        String json = new String(Files.readAllBytes(path));
        try {
            SimpleJson simpleJson = new SimpleJson(json);
            System.out.println(simpleJson.toString(4));
            fail("Should not go this far!");
        } catch (ParseException e) {

        }

    }

    @Test
    public void testLargeJson() throws IOException, ParseException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/large.json");
        String json = new String(Files.readAllBytes(path));
        StopWatch stopWatch = new StopWatch().start();
        SimpleJson simpleJson = new SimpleJson(json);
        stopWatch.stop();
        System.out.println("Large Json parsing took " + stopWatch.getTotalTime() + " ms.");
        int size = ((List<Object>) simpleJson.toObject()).size();
        assertEquals("The size of the array:", 24, size);
    }

    @Test
    public void testVeryLargeJson() throws IOException, ParseException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/veryLarge.json");
        String json = new String(Files.readAllBytes(path));
        StopWatch stopWatch = new StopWatch().start();
        SimpleJson simpleJson = new SimpleJson(json);
        stopWatch.stop();
        System.out.println("Very large Json parsing took " + stopWatch.getTotalTime() + " ms.");
        int size = ((List<Object>) simpleJson.toObject()).size();
        assertEquals("The size of the array:", 205, size);
        assertEquals("James Weber", simpleJson.get(204).get("friends").get(1).get("name").toObject());
        assertEquals("ee3af8e5-84c3-4162-9eca-595e50b00e6a", simpleJson.get(152).get("guid").toObject());
    }

//    @Test
//    public void testTemp() throws ParseException {
//        String json = "{\n" +
//                "   \"name\": \"James Weber\",\n" +
//                "   \"id\": 1\n" +
//                "}";
//        SimpleJson simpleJson = new SimpleJson(json);
//        System.out.println(simpleJson.toString(3));
//        System.out.println(simpleJson.get("name").toString(1));
//    }

}
