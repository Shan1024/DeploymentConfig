import ConfigFileTypes.Properties;
import ConfigFileTypes.XML;
import ConfigFileTypes.YAML;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;

/**
 * Created by shan on 6/6/16.
 */
public class ConfigUtilTest {

    @Test
    public void getConfigYaml() {
        File file = new File("carbon.yml");
        YAML configYaml = ConfigUtil.getConfig(file, YAML.class);
        System.out.println("New YML: \n" + configYaml.getValue());

    }

    @Test
    public void getConfigXml() {

        File file = new File("log4j2.xml");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            XML configXml = ConfigUtil.getConfig(fileInputStream, file.getName(), XML.class);
            System.out.println("New XML: \n" + configXml.getValue());
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    @Test
    public void getConfigProperties() {
        File file = new File("launch.properties");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            Properties configProperties = ConfigUtil.getConfig(fileInputStream, file.getName(), Properties.class);
            System.out.println("New Properties: \n" + configProperties);
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    @Test
    public void getConfigs() {

        String newTenant = ConfigUtil.getConfig("[carbon.yml]/tenant");
        Assert.assertEquals(newTenant, "shanm");

        String newTransportPort = ConfigUtil.getConfig("[carbon.yml]/transports/transport/port");
        Assert.assertEquals(newTransportPort, "1212");

        String newTransportPortXYZ = ConfigUtil.getConfig("[carbon.yml]/transports/transport[name='xyz']/port");
        Assert.assertEquals(newTransportPortXYZ, "8888");

    }

}