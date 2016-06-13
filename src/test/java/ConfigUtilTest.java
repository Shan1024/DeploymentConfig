import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.kernel.core.util.ConfigUtil;
import org.wso2.carbon.kernel.core.util.configfiletypes.Properties;
import org.wso2.carbon.kernel.core.util.configfiletypes.XML;
import org.wso2.carbon.kernel.core.util.configfiletypes.YAML;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * This class is to demonstrate the sample uses of the ConfigUtil class
 */
public class ConfigUtilTest {

    private static Logger logger = Logger.getLogger(ConfigUtilTest.class.getName());

    @Test
    public void getConfig1Test_Yaml() {
        setUpEnvironment();
        File file = new File("carbon.yml");
        YAML configYaml = ConfigUtil.getConfig(file, YAML.class);
        logger.log(Level.INFO, "New YML: \n" + configYaml.getValue());
    }

    @Test
    public void getConfig2Test_Xml() {
        setUpEnvironment();
        File file = new File("log4j2.xml");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            XML configXml = ConfigUtil.getConfig(fileInputStream, file.getName(), XML.class);
            logger.log(Level.INFO, "New XML: \n" + configXml.getValue());
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, e.toString());
        }
    }

    @Test
    public void getConfig2Test_Properties() {
        setUpEnvironment();
        File file = new File("launch.properties");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            Properties configProperties = ConfigUtil.getConfig(fileInputStream, file.getName(), Properties.class);
            logger.log(Level.INFO, "New Properties: \n" + configProperties.getValue());
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, e.toString());
        }
    }

    @Test
    public void getConfigs3Test() {
        setUpEnvironment();

        String newTenant = ConfigUtil.getConfig("[carbon.yml]/tenant");
        Assert.assertEquals(newTenant, "new_tenant");

        String newTransportPort = ConfigUtil.getConfig("[carbon.yml]/transports/transport/port");
        Assert.assertEquals(newTransportPort, "9090");

        String newTransportPortXYZ = ConfigUtil.getConfig("[carbon.yml]/transports/transport[name='xyz']/port");
        Assert.assertEquals(newTransportPortXYZ, "8080");
    }

    @Test
    public void jaxbExample() {
        setUpEnvironment();
        try {
            File file = new File("Example.xml");

            JAXBContext jaxbContext = JAXBContext.newInstance(Configurations.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Configurations configurations = (Configurations) unmarshaller.unmarshal(file);

            Assert.assertEquals(configurations.getTenant(), "tenant");
            Assert.assertEquals(configurations.getTransport().getPort(), "8000");

            XML configXml = ConfigUtil.getConfig(file, XML.class);

            Source xmlInput = new StreamSource(new StringReader(configXml.getValue()));
            jaxbContext = JAXBContext.newInstance(Configurations.class);
            unmarshaller = jaxbContext.createUnmarshaller();
            configurations = (Configurations) unmarshaller.unmarshal(xmlInput);

            Assert.assertEquals(configurations.getTenant(), "new_tenant");
            Assert.assertEquals(configurations.getTransport().getPort(), "8080");

        } catch (JAXBException e) {
            logger.log(Level.SEVERE, e.toString());
        }

    }

    private void setUpEnvironment() {
        System.setProperty("https.port", "8080");
        Map<String, String> envVarMap = new HashMap<>();
        envVarMap.put("HTTP_PORT", "9090");
        setEnv(envVarMap);
        System.setProperty("osgi.instance.area", "file:profiles/${profile}/workspace_new");
        System.setProperty("file.pattern", "NewPattern");
        System.setProperty("abc.http.port", "8080");
    }

    private static void setEnv(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                logger.log(Level.INFO, e2.toString());
            }
        } catch (Exception e1) {
            logger.log(Level.INFO, e1.toString());
        }
    }
}
