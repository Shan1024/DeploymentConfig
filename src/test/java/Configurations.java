import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
class Transport {

    String name;
    int port;
    boolean secure;

    @XmlAttribute
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isSecure() {
        return secure;
    }

    @XmlElement
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @XmlElement
    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}

@XmlRootElement
class Transports {

    List<Transport> transport;

    @XmlElement
    public void setTransport(List<Transport> transport) {
        this.transport = transport;
    }

    public List<Transport> getTransport() {
        return transport;
    }

}

@XmlRootElement
public class Configurations {

    String tenant;
    Transports transports;

    @XmlElement
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    @XmlElement
    public void setTransports(Transports transports) {
        this.transports = transports;
    }

    public Transports getTransports() {
        return transports;
    }
}
