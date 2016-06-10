
import org.testng.Assert;
import org.testng.annotations.Test;

import org.wso2.carbon.kernel.core.util.ConfigUtil;

import org.wso2.carbon.kernel.core.util.configfiletypes.Properties;
import org.wso2.carbon.kernel.core.util.configfiletypes.XML;
import org.wso2.carbon.kernel.core.util.configfiletypes.YAML;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Logger;





/**
 * Created by shan on 6/6/16.
 */
public class ConfigUtilTest {

    private static Logger logger = Logger.getLogger(ConfigUtilTest.class.getName());

    @Test
    public void getConfigYaml() {
        File file = new File("carbon.yml");
        YAML configYaml = ConfigUtil.getConfig(file, YAML.class);
        logger.info("New YML: \n" + configYaml.getValue());

    }

    @Test
    public void getConfigXml() {

        File file = new File("log4j2.xml");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            XML configXml = ConfigUtil.getConfig(fileInputStream, file.getName(), XML.class);
            logger.info("New XML: \n" + configXml.getValue());
        } catch (FileNotFoundException e) {
            logger.warning(e.toString());
        }
    }

    @Test
    public void getConfigProperties() {
        File file = new File("launch.properties");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            Properties configProperties = ConfigUtil.getConfig(fileInputStream, file.getName(), Properties.class);
            logger.info("New Properties: \n" + configProperties);
        } catch (FileNotFoundException e) {
            logger.warning(e.toString());
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
