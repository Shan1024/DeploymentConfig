import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Configurations {

    @XmlRootElement
    static class Transport {

        String name;
        String port;

        @XmlElement
        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @XmlElement
        public void setPort(String port) {
            this.port = port;
        }

        public String getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "Name: " + name + ", Port: " + port;
        }

    }

    String tenant;
    Transport transport;

    @XmlElement
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    @XmlElement
    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    @Override
    public String toString() {
        return "tenant: " + tenant + "\nTransport: \n\t" + transport;
    }
}
