package com.funtowiczmo.spik.network.lan;

import android.net.DhcpInfo;
import com.funtowiczmo.spik.lang.Computer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by momo- on 28/10/2015.
 */
public class AndroidDiscoveryClient{

    private static final int DEFAULT_PORT = 10101;

    private final int port;

    public AndroidDiscoveryClient(){
        this(DEFAULT_PORT);
    }

    public AndroidDiscoveryClient(int port){
        this.port = port;
    }

    public List<Computer> startDiscovery(DhcpInfo dhcp) throws IOException {
        final List<Computer> computers = new ArrayList<>();

        /*try(DatagramSocket socket = new DatagramSocket()) {
            final Gson decoder = new Gson();
            final DatagramPacket rawResponse = new DatagramPacket(new byte[1 << 16], 1 << 16);
            final DiscoveryMessage message = new DiscoveryMessage(DiscoveryMessage.Method.DISCOVERY_REQUEST)
                    .addParam("port", ((InetSocketAddress) socket.getLocalSocketAddress()).getPort());

            final byte[] raw = decoder.toJson(message).getBytes(StandardCharsets.UTF_8);

            socket.setBroadcast(true);
            socket.send(new DatagramPacket(raw, raw.length, InetAddress.getByName("255.255.255.255"), port));

            try {
                final long maxDuration = System.currentTimeMillis() + 1000;
                do {
                    socket.setSoTimeout(Long.valueOf(maxDuration - System.currentTimeMillis()).intValue());
                    socket.receive(rawResponse);

                    if (rawResponse.getLength() > 0) {
                        final String json = new String(
                                rawResponse.getData(),
                                rawResponse.getOffset(),
                                rawResponse.getLength(),
                                StandardCharsets.UTF_8
                        );

                        DiscoveryMessage response = decoder.fromJson(json, DiscoveryMessage.class);
                        if (response.method() != null && response.method() == DiscoveryMessage.Method.DISCOVERY_RESPONSE) {
                            if(!response.params().containsKey("ip"))
                                response.params().put("ip", rawResponse.getAddress().getHostAddress());

                            final Computer computer = getComputer(response.params());
                            if (computer != null)
                                computers.add(computer);
                        }
                    }
                } while (System.currentTimeMillis() < maxDuration);
            } catch (SocketTimeoutException ignored) {}
        }
*/
        return computers;
    }

    private Computer getComputer(Map<String, Object> params) {
        if(!checkParams(params))
            return null;
        else
            return new Computer(
                params.get("name").toString(),
                params.get("os").toString(),
                params.get("ip").toString(),
                (Double.valueOf(params.get("port").toString())).intValue()
            );
    }

    private boolean checkParams(Map<String, Object> params){
        return params.containsKey("name") &&
               params.containsKey("os") &&
               params.containsKey("ip") &&
               params.containsKey("port");
    }

    private InetAddress getBroadcastAddress(DhcpInfo dhcp) throws UnknownHostException {
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
}
