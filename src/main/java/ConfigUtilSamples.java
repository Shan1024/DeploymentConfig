import ConfigFileTypes.Properties;
import ConfigFileTypes.XML;
import ConfigFileTypes.YAML;
import org.testng.Assert;

import java.io.*;

/**
 * Created by shan on 6/9/16.
 */
public class ConfigUtilSamples {

    //Using File
    public void getConfig() {

        File file = new File("carbon.yml");
        YAML configYaml = ConfigUtil.getConfig(file, YAML.class);

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

    //Using FileStream
    public void getConfig1() {

        File file = new File("log4j2.xml");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            XML configXml = ConfigUtil.getConfig(fileInputStream, file.getName(), XML.class);

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

    public void getConfig2() {

        File file = new File("launch.properties");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            Properties configProperties = ConfigUtil.getConfig(fileInputStream, file.getName(), Properties.class);

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

    public void getConfig3() {
        String newTenant = ConfigUtil.getConfig("[carbon.yml]/tenant");
        Assert.assertEquals(newTenant, "shanm");
    }
}
