import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

/**
 * Created by shan on 6/2/16.
 */
public class ConfigUtilTest {

    private static final String YML_FILENAME = "carbon.yml";

    @BeforeClass
    public void setup() {

    }

    @Test
    public void testGetConfigs1() {
        Map carbonYMLMap = readYAML();
        System.out.println("OldMap: " + carbonYMLMap);
        Map newMap = ConfigUtil.getConfigs(carbonYMLMap, YML_FILENAME);
        System.out.println("NewMap: " + newMap);

        Assert.assertEquals(newMap.get("tenant"), "shanm");
        Assert.assertEquals(((Map) newMap.get("ports")).get("offset"), 12);
    }

    @Test
    public void testGetConfigs2() {
        Map carbonYMLMap = readYAML();
        System.out.println("OldMap: " + carbonYMLMap);
        Map newMap = ConfigUtil.getConfigs(YML_FILENAME);
        System.out.println("Carbon: " + newMap);

        Assert.assertEquals(newMap.get("tenant"), "shanm");
        Assert.assertEquals(newMap.get("ports.offset"), "12");
    }

    @Test
    private void testGetValue1() {
        Object newValue = ConfigUtil.getValue("[carbon.yml]/logging.enable", false);
        //New value from the deployment.properties file is returned
        Assert.assertEquals(newValue, true);
    }

    @Test
    private void testGetValue2() {
        Object newValue = ConfigUtil.getValue("[axis.yml]/logging.enable", true);
        //Default value returns because the property is not in the deployment.properties file
        Assert.assertEquals(newValue, true);
    }

    @Test
    public void convertMapToYamlTest() {
        String expected = "id: ${sec.key}\n" + "name: ${sys.name}\n" + "version: ${env.version}\n" + "tenant: shanm\n"
                + "ports:\n" + "  offset: 12\n" + "  https:\n" + "    port: 4848\n" + "    backup:\n"
                + "      port: 4949\n" + "deployment:\n" + "  mode: scheduled\n" + "transports:\n" + "- transport:\n"
                + "    name: netty\n" + "- transport:\n" + "    name: xyz\n" + "logging:\n" + "  enable: true\n";

        Map carbonYMLMap = readYAML();
        Map newMap = ConfigUtil.getConfigs(carbonYMLMap, YML_FILENAME);
        String yamlString = ConfigUtil.convertMapToYmlString(newMap);

        Assert.assertEquals(yamlString,expected);

        try {
            File newFile = new File("carbon_new.yml");
            FileWriter fileWriter = new FileWriter(newFile);
            fileWriter.write(yamlString);
            fileWriter.flush();

            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map readYAML() {

        Map map = null;
        Yaml yaml = new Yaml();

        try {
            InputStream ios = new FileInputStream(new File(YML_FILENAME));
            map = yaml.loadAs(ios, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}