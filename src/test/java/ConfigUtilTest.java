import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by shan on 6/2/16.
 */
public class ConfigUtilTest {

    private static final String YML_FILENAME = "carbon.yml";
    private static Map carbonYMLMap;

    @BeforeClass
    public void setup() {
        carbonYMLMap = readYAML();
    }

    @Test
    public void getConfigs() {
        System.out.println("OldMap: " + carbonYMLMap);
        Map newMap = ConfigUtil.getConfigs(carbonYMLMap, YML_FILENAME);
        System.out.println("NewMap: " + newMap);
    }

    private static Map readYAML() {

        Map map = null;
        Yaml yaml = new Yaml();

        try {
            InputStream ios = new FileInputStream(new File(YML_FILENAME));

            // Parse the YAML
            map = yaml.loadAs(ios, Map.class);
            System.out.println(map);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}