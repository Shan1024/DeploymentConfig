import org.testng.annotations.Test;

import java.io.*;

/**
 * Created by shan on 6/6/16.
 */
public class ConfigUtil2Test {

    @Test
    public void getConfig() {

        File file = new File("carbon.yml");
        String config = ConfigUtil2.getConfig(file, ConfigFileFormat.YML);
        System.out.println("Config1: \n" + config);

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
            String config = ConfigUtil2.getConfig(fileInputStream, file.getName(), ConfigFileFormat.XML);
            System.out.println("Config2: \n" + config);

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

    }

}