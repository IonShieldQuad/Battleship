package network;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

public class BroadcastListener extends Thread {
    public final String REQUEST_MESSAGE = "BATTLESHIP_GAME_DISCOVERY_REQUEST";
    public final String RESPONSE_MESSAGE = "BATTLESHIP_GAME_DISCOVERY_RESPONSE";
    
    private int port;
    private boolean receivingRequests;
    private boolean receivingResponses;
    private Collection<InetAddress> addressCollection;
    
    private List<Consumer<DatagramPacket>> requestListeners = new ArrayList<>();
    private List<Consumer<DatagramPacket>> responseListeners = new ArrayList<>();
    
    public BroadcastListener(int port, boolean receivingRequests, boolean receivingResponses) {
        this.port = port;
        this.receivingRequests = receivingRequests;
        this.receivingResponses = receivingResponses;
        
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.setBroadcast(true);
            
            while (true) {
                
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
    
                byte[] buffer = new byte[15000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData()).trim();
                
                if (receivingRequests && message.equals(REQUEST_MESSAGE)) {
                    byte[] sendData = RESPONSE_MESSAGE.getBytes();
    
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                    
                    requestListeners.forEach(l -> l.accept(packet));
                }
                
                if (receivingResponses && message.equals(RESPONSE_MESSAGE)) {
                    if (addressCollection != null) {
                        addressCollection.add(packet.getAddress());
                    }
                    responseListeners.forEach(l -> l.accept(packet));
                }
                
            }
        } catch (InterruptedException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    public void sendRequest() {
        try {
            DatagramSocket socket = new DatagramSocket();
    
            byte[] sendData = REQUEST_MESSAGE.getBytes();
            DatagramPacket sendPacket;
    
            sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
            socket.send(sendPacket);
    
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
    
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
    
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    
                    if (broadcast == null) {
                        continue;
                    }
    
                    sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, port);
                    socket.send(sendPacket);
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isReceivingRequests() {
        return receivingRequests;
    }
    
    public void setReceivingRequests(boolean receivingRequests) {
        this.receivingRequests = receivingRequests;
    }
    
    public Collection<InetAddress> getAddressCollection() {
        return addressCollection;
    }
    
    public void setAddressCollection(Collection<InetAddress> addressCollection) {
        this.addressCollection = addressCollection;
    }
    
    public void addRequestListener(Consumer<DatagramPacket> listener) {
        requestListeners.add(listener);
    }
    
    public void addResponseListener(Consumer<DatagramPacket> listener) {
        responseListeners.add(listener);
    }
    
    public void setReceivingResponses(boolean receivingResponses) {
        this.receivingResponses = receivingResponses;
    }
}
