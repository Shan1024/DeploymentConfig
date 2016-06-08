import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;

/**
 * Created by shan on 6/6/16.
 */
public class ConfigUtilTest {

    @Test
    public void getConfig() {

        File file = new File("carbon.yml");
        String config = ConfigUtil.getConfig(file, ConfigFileFormat.YML);

        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(new File("carbon_new.xml"));
            fileWriter.write(config);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    @Test
    public void getConfig1() {

        File file = new File("log4j2.xml");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            String config = ConfigUtil.getConfig(fileInputStream, file.getName(), ConfigFileFormat.XML);

            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(new File("log4j_new.xml"));
                fileWriter.write(config);
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
    public void getConfig2() {

        File file = new File("launch.properties");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            String config = ConfigUtil.getConfig(fileInputStream, file.getName(), ConfigFileFormat.PROPERTIES);

            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(new File("launch.xml"));
                fileWriter.write(config);
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
    public void getConfig3() {
        String newTenant = ConfigUtil.getConfig("[carbon.yml]/configurations/tenant");
        Assert.assertEquals(newTenant, "shanm");
        int newPort = Integer.parseInt(ConfigUtil.getConfig("[carbon.yml]/configurations/transports/transport/port"));
        Assert.assertEquals(newPort, 8888);
    }

}