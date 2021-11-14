package socket;

import java.io.IOException;

public class Testserver {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = new Server(6666);
        new Thread(server).start();
        Thread.sleep(5000);
        server.send(String.format("{\"type\":\"unstake_nft\",\"user_name\":\"%s\",\"asset_id\":\"%s\"}","qv5ag.wam","1337"));
    }
}
