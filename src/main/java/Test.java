import com.fasterxml.jackson.xml.XmlMapper;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.Map;

/**
 * Created by shan on 6/9/16.
 */

//    public void test() {
//                ObjectMapper wrapping = rootMapper();
//
//                String json = wrapping.writer().withRootName("something").writeValueAsString(new Bean());
//                //        assertEquals("{\"something\":{\"a\":3}}", json);
//                json = wrapping.writer().withRootName("").writeValueAsString(new Bean());
//                //        assertEquals("{\"a\":3}", json);
//
////        String TEST_XML_STRING = "<?xml version=\"1.0\" ?><test attrib=\"moretest\">Turn this to JSON</test>";
////
////        try {
////            JSONObject xmlJSONObj = ConfigFileTypes.XML.toJSONObject(TEST_XML_STRING);
////            String jsonPrettyPrintString = xmlJSONObj.toString(4);
////
////            System.out.println(jsonPrettyPrintString);
////        } catch (JSONException je) {
////            System.out.println(je.toString());
////        }
//
//    }
//
//    private ObjectMapper rootMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
//        return mapper;
//    }

public class Test {
    public static void main(String[] args) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            //            Map entries = xmlMapper.readValue(new File("log4j2.xml"), Map.class);

            String xmlString = "<Configuration>\n" + "    <Appenders>\n" + "\n"
                    + "        <RandomAccessFile name=\"RandomAccessFile1\" fileName=\"${sys:carbon.home}/logs/carbon.log\"\n"
                    + "                          immediateFlush=\"false\" append=\"false\">\n"
                    + "            <PatternLayout>\n" + "                <Pattern>[%d] %5p {%c} - %m%ex%n</Pattern>\n"
                    + "            </PatternLayout>\n" + "        </RandomAccessFile>\n" + "\n"
                    + "        <RandomAccessFile name=\"RandomAccessFile2\" fileName=\"${sys:carbon.home}/logs/carbon.log\"\n"
                    + "                          immediateFlush=\"false\" append=\"false\">\n"
                    + "            <PatternLayout>\n" + "                <Pattern>[%d] %5p {%c} - %m%ex%n</Pattern>\n"
                    + "            </PatternLayout>\n" + "        </RandomAccessFile>\n" + "\n"
                    + "        <RandomAccessFile name=\"RandomAccessFile3\" fileName=\"${sys:carbon.home}/logs/carbon.log\"\n"
                    + "                          immediateFlush=\"false\" append=\"false\">\n"
                    + "            <PatternLayout>\n" + "                <Pattern>[%d] %5p {%c} - %m%ex%n</Pattern>\n"
                    + "            </PatternLayout>\n" + "        </RandomAccessFile>\n" + "\n"
                    + "        <Console name=\"CARBON_CONSOLE\" target=\"SYSTEM_OUT\">\n"
                    + "            <PatternLayout pattern=\"[%d] %5p {%c} - %m%ex%n\"/>\n" + "        </Console>\n"
                    + "        \n" + "    </Appenders>\n" + "    <Loggers>\n" + "\n"
                    + "        <AsyncLogger name=\"com.foo.Bar\" level=\"trace\" includeLocation=\"true\">\n"
                    + "            <AppenderRef ref=\"RandomAccessFile\"/>\n" + "        </AsyncLogger>\n" + "\n"
                    + "        <Root level=\"info\">\n" + "            <AppenderRef ref=\"RandomAccessFile\"/>\n"
                    + "            <AppenderRef ref=\"CARBON_CONSOLE\"/>\n" + "        </Root>\n" + "    </Loggers>\n"
                    + "</Configuration>";

            Map map = null;

            System.out.println("hello "+null);
            try {
                JSONObject xmlJSONObj = XML.toJSONObject(xmlString);
                String jsonPrettyPrintString = xmlJSONObj.toString();
                System.out.println(jsonPrettyPrintString);

                Yaml yaml = new Yaml();

                //            Object yamlObj = yaml.load(jsonPrettyPrintString);
                //            System.out.println(yamlObj);

                map = yaml.loadAs(jsonPrettyPrintString, Map.class);

                map = (Map) map.get("Configuration");

                String yamlString = yaml.dumpAsMap(map);
                System.out.println("yamlString:\n" + yamlString);
                ObjectMapper jsonMapper = new ObjectMapper();
                String json = jsonMapper.writeValueAsString(map);
                System.out.println(json);

            } catch (JSONException e) {

            }

            //            ObjectMapper jsonMapper = new ObjectMapper();
            //            String json = jsonMapper.writeValueAsString(entries);
            //            System.out.println(json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
