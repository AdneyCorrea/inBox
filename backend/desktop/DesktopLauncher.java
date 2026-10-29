// Inicia a versão desktop com Java incluído, dados locais e abertura automática do navegador.
import java.awt.Desktop;
import java.net.URI;
import java.net.http.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

public class DesktopLauncher {
    public static void main(String[] args) {
        try { launch(java.util.Arrays.asList(args).contains("--smoke-test")); }
        catch (Exception e) {
            if (java.util.Arrays.asList(args).contains("--smoke-test")) e.printStackTrace();
            else JOptionPane.showMessageDialog(null, e.getMessage(), "inBox", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    private static void launch(boolean smokeTest) throws Exception {
        Path data = Path.of(System.getProperty("inbox.desktop.data", Path.of(System.getenv("LOCALAPPDATA"), "inBox").toString()));
        Files.createDirectories(data);
        int port = 18080;
        if (smokeTest) {
            try (var socket = new java.net.ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) { port = socket.getLocalPort(); }
        }
        URI home = URI.create("http://127.0.0.1:" + port);
        try (FileChannel channel = FileChannel.open(data.resolve("desktop.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                if (!healthy(home)) throw new IllegalStateException("O inBox está iniciando. Aguarde alguns segundos e abra novamente.");
                Desktop.getDesktop().browse(home);
                return;
            }
            try (lock) {
                // Não reutiliza uma porta ocupada sem possuir o bloqueio desta instalação.
                try (var socket = new java.net.ServerSocket()) {
                    socket.bind(new java.net.InetSocketAddress("127.0.0.1", port));
                } catch (java.io.IOException e) {
                    throw new IllegalStateException("A porta 18080 está ocupada. Encerre a outra instância antes de abrir o inBox.");
                }
                Path app = Path.of(System.getProperty("java.home")).getParent().resolve("app");
                Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java.exe");
                Process server = new ProcessBuilder(javaExe.toString(), "-jar", app.resolve("inbox.jar").toString(),
                    "--spring.profiles.active=default", "--server.address=127.0.0.1", "--server.port=" + port,
                    "--spring.datasource.url=jdbc:h2:file:./data/inbox;DB_CLOSE_ON_EXIT=FALSE",
                    "--logging.file.name=server.log")
                    .directory(data.toFile()).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(data.resolve("desktop.log").toFile())).start();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    server.destroy();
                    try { if (!server.waitFor(15, TimeUnit.SECONDS)) server.destroyForcibly(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }));
                boolean ready = false;
                for (int i = 0; i < 120 && server.isAlive(); i++) {
                    if (healthy(home)) { ready = true; break; }
                    Thread.sleep(500);
                }
                if (!ready) {
                    server.destroy();
                    throw new IllegalStateException("Não foi possível iniciar. Consulte os logs em " + data);
                }
                if (smokeTest) { server.destroy(); server.waitFor(20, TimeUnit.SECONDS); System.out.println("Desktop startup OK"); return; }
                if (java.awt.SystemTray.isSupported()) {
                    var image = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    var g = image.createGraphics();
                    g.setColor(new java.awt.Color(50, 115, 100)); g.fillRoundRect(0, 0, 32, 32, 8, 8);
                    g.setColor(java.awt.Color.WHITE); g.drawString("in", 10, 21); g.dispose();
                    var menu = new java.awt.PopupMenu();
                    var open = new java.awt.MenuItem("Abrir inBox");
                    open.addActionListener(e -> { try { Desktop.getDesktop().browse(home); } catch (Exception ignored) {} });
                    var exit = new java.awt.MenuItem("Encerrar inBox");
                    exit.addActionListener(e -> System.exit(0));
                    menu.add(open); menu.add(exit);
                    var tray = new java.awt.TrayIcon(image, "inBox — abrir ou encerrar", menu);
                    tray.setImageAutoSize(true);
                    java.awt.SystemTray.getSystemTray().add(tray);
                }
                Desktop.getDesktop().browse(home);
                server.waitFor();
                System.exit(server.exitValue());
            }
        }
    }
    private static boolean healthy(URI home) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            var response = client.send(HttpRequest.newBuilder(home.resolve("/api/health")).timeout(Duration.ofSeconds(2)).build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("\"app\":\"inbox\"");
        } catch (Exception e) { return false; }
    }
}
