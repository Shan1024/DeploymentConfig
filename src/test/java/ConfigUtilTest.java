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

        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(new File("carbon_new.yml"));
            fileWriter.write(configYaml.getValue());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    @Test
    public void getConfigXml() {

        File file = new File("log4j2.xml");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            XML configXml = ConfigUtil.getConfig(fileInputStream, file.getName(), XML.class);
            System.out.println("New XML: \n" + configXml.getValue());

            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(new File("log4j_new.xml"));
                fileWriter.write(configXml.getValue());
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println(e);
            }

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

            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(new File("launch_new.properties"));
                fileWriter.write(configProperties.getValue());
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println(e);
            }

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