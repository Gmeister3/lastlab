import java.io.Serializable;

/**
 * SPEPacket – the communication envelope exchanged between Client and ConcurrentServer.
 * It must implement Serializable so that Java's ObjectOutputStream / ObjectInputStream
 * can transmit it across a TCP stream without manual marshalling.
 */
public class SPEPacket implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Command identifier (one of the ProtocolConstants values). */
    private String header;

    /** Data or content to be processed by the server. */
    private String payload;

    public SPEPacket(String header, String payload) {
        this.header  = header;
        this.payload = payload;
    }

    public String getHeader()  { return header;  }
    public String getPayload() { return payload; }

    @Override
    public String toString() {
        return "SPEPacket{header='" + header + "', payload='" + payload + "'}";
    }
}
