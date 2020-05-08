package se.vidstige.jadb;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class JadbConnection implements ITransportFactory {

    private final String host;
    private final int port;

    private static final int DEFAULTPORT = 5037;

    public JadbConnection() {
        this("localhost", DEFAULTPORT);
    }

    public JadbConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Transport createTransport() throws IOException {
        return new Transport(new Socket(host, port));
    }

    public String getHostVersion() throws IOException, JadbException {
        try (Transport transport = createTransport()) {
            transport.send("host:version");
            transport.verifyResponse();
            return transport.readString();
        }
    }

    public InetSocketAddress connectToTcpDevice(InetSocketAddress inetSocketAddress)
            throws IOException, JadbException, ConnectionToRemoteDeviceException {
        try (Transport transport = createTransport()) {
            return new HostConnectToRemoteTcpDevice(transport).connect(inetSocketAddress);
        }
    }

    public InetSocketAddress disconnectFromTcpDevice(InetSocketAddress tcpAddressEntity)
            throws IOException, JadbException, ConnectionToRemoteDeviceException {
        try (Transport transport = createTransport()) {
            return new HostDisconnectFromRemoteTcpDevice(transport).disconnect(tcpAddressEntity);
        }
    }

    public List<JadbDevice> getDevices() throws IOException, JadbException {
        try (Transport transport = createTransport()) {
            transport.send("host:devices");
            transport.verifyResponse();
            String body = transport.readString();
            return parseDevices(body);
        }
    }

    public DeviceWatcher createDeviceWatcher(DeviceDetectionListener listener) throws IOException, JadbException {
        Transport transport = createTransport();
        transport.send("host:track-devices");
        transport.verifyResponse();
        return new DeviceWatcher(transport, listener, this);
    }

    public List<JadbDevice> parseDevices(String body) {
        String[] lines = body.split("\n");
        ArrayList<JadbDevice> devices = new ArrayList<>(lines.length);
        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length > 1) {
                devices.add(new JadbDevice(parts[0], this)); // parts[1] is type
            }
        }
        return devices;
    }

    public JadbDevice getAnyDevice() {
        return JadbDevice.createAny(this);
    }
    
    public String listforward(String serial) throws IOException, JadbException {
        Transport devices = createTransport();
        devices.send("host-serial:" + serial + ":list-forward");
        devices.verifyResponse();
        String body = devices.readString();
        devices.close();
        return body;
    }
    
    public void forward(String serial, int port1, int port2) throws IOException, JadbException {
        Transport devices = createTransport();
        devices.send("host-serial:" + serial + ":list-forward");
        devices.verifyResponse();
        String body = devices.readString();
        devices.close();
        System.out.println(body);
        devices = createTransport();
        devices.send("host-serial:" + serial + ":forward:tcp:" + port1 + ";tcp:" + port2);
        devices.verifyResponse();
        devices.close();
    }
}
