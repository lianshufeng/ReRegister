package proxycap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyCapMain {

    private static final String PROXYCAP_INSTALL_URL = "https://proxy.jpy.wang/www.proxycap.com/download/pcap542_x64.msi";
    private static final File PROXYCAP_WORK = new File(System.getenv("SystemDrive") + "/ProxyCapReset/");
    private static final File PROXYCAP_FILE = new File(PROXYCAP_WORK.getAbsolutePath() + "/" + PROXYCAP_INSTALL_URL.substring(PROXYCAP_INSTALL_URL.lastIndexOf("/")));
    private static final File PROXYCAP_BACKUP_CONFIG = new File(System.getenv("SystemDrive") + "/ProxyCapReset/backup/");

    // 所有中文常量改成 Unicode 转义，避免源文件编码差异导致的乱码
    private static final String TXT_RESET_SERVICE       = "\u91cd\u7f6e ProxyCap \u670d\u52a1";                 // 重置 ProxyCap 服务
    private static final String TXT_UNINSTALL_SERVICE   = "\u5378\u8f7d ProxyCap \u670d\u52a1";                 // 卸载 ProxyCap 服务
    private static final String TXT_STOP_SERVICE        = "\u505c\u6b62 ProxyCap \u670d\u52a1";                 // 停止 ProxyCap 服务
    private static final String TXT_RESTART_SERVICE     = "\u91cd\u542f ProxyCap \u670d\u52a1";                 // 重启 ProxyCap 服务

    private static final String MSG_CONFIRM_RESET       = "\u91cd\u7f6e " + "[ProxyCap]" + "  ? ";              // 重置 [ProxyCap]  ?
    private static final String MSG_CONFIRM_UNINSTALL   = "\u5378\u8f7d " + "[ProxyCap]" + "  ? ";              // 卸载 [ProxyCap]  ?
    private static final String MSG_STOP_DONE           = "\u670d\u52a1\u505c\u6b62\u5b8c\u6210";               // 服务停止完成
    private static final String MSG_RESTART_DONE        = "\u670d\u52a1\u91cd\u542f\u5b8c\u6210";               // 服务重启完成
    private static final String MSG_AFTER_REPAIR        = "\u8bf7\u542f\u52a8\u670d\u52a1\uff0c\u5e76\u6062\u590d\u914d\u7f6e\u6587\u4ef6: \n"; // 请启动服务，并恢复配置文件:
    private static final String MSG_AFTER_UNINSTALL     = "\u5378\u8f7d\u5b8c\u6210,\u6062\u590d\u914d\u7f6e\u6587\u4ef6: \n";                   // 卸载完成,恢复配置文件:

    public static void main(String[] args) {
        initFile();

        // 可选：设置一个常见中文字体，确保不同 LAF 下也能显示中文
        UIManager.put("Button.font", new Font("Microsoft YaHei", Font.PLAIN, 12));
        UIManager.put("Label.font",  new Font("Microsoft YaHei", Font.PLAIN, 12));

        SwingUtilities.invokeLater(() -> {
            JFrame jf = new JFrame("ProxyCap");
            jf.setSize(260, 320);
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jf.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

            JButton resetProxyCap = new JButton(TXT_RESET_SERVICE);
            resetProxyCap.addActionListener((e) -> {
                int option = JOptionPane.showConfirmDialog(null, MSG_CONFIRM_RESET, "ReRegister", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    try {
                        downloadProxyCap();
                        backupConfig();
                        resetRegInfo();
                        repairInstall();
                        JOptionPane.showMessageDialog(null, MSG_AFTER_REPAIR + PROXYCAP_BACKUP_CONFIG.getAbsolutePath());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            panel.add(resetProxyCap);

            JButton uninstallProxyCap = new JButton(TXT_UNINSTALL_SERVICE);
            uninstallProxyCap.addActionListener((e) -> {
                int option = JOptionPane.showConfirmDialog(null, MSG_CONFIRM_UNINSTALL, "ReRegister", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    try {
                        downloadProxyCap();
                        backupConfig();
                        resetRegInfo();
                        unInstall();
                        JOptionPane.showMessageDialog(null, MSG_AFTER_UNINSTALL + PROXYCAP_BACKUP_CONFIG.getAbsolutePath());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            panel.add(uninstallProxyCap);

            JButton stopProxyCap = new JButton(TXT_STOP_SERVICE);
            stopProxyCap.addActionListener((ActionEvent e) -> {
                stopService();
                JOptionPane.showMessageDialog(null, MSG_STOP_DONE);
            });
            panel.add(stopProxyCap);

            JButton reStartProxyCap = new JButton(TXT_RESTART_SERVICE);
            reStartProxyCap.addActionListener((ActionEvent e) -> {
                stopService();
                runCmd("net start pcapsvc");
                runCmd("powershell -Command \"$Key = 'HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run' ; $Name = 'ProxyCap' ; $result = (Get-ItemProperty -Path \\\"Registry::$Key\\\" -ErrorAction Stop).$Name; & $result\"");
                JOptionPane.showMessageDialog(null, MSG_RESTART_DONE);
            });
            panel.add(reStartProxyCap);

            jf.setLayout(new BorderLayout());
            jf.add(panel, BorderLayout.CENTER);
            jf.setVisible(true);
        });
    }

    private static void initFile() {
        if (!PROXYCAP_WORK.exists()) {
            PROXYCAP_WORK.mkdirs();
        }
        if (!PROXYCAP_BACKUP_CONFIG.exists()) {
            PROXYCAP_BACKUP_CONFIG.mkdirs();
        }
    }

    private static void stopService() {
        runCmd("net stop pcapsvc");
        runCmd("taskkill /im pcapui.exe /f");
    }

    /**
     * 卸载
     */
    private static void unInstall() {
        stopService();
        runCmd("msiexec /quiet /uninstall " + PROXYCAP_INSTALL_URL + " /norestart");
    }

    private static void repairInstall() {
        runCmd('"' + PROXYCAP_FILE.getAbsolutePath() + '"' + " /quiet /norestart");
    }

    // 清除注册信息
    private static void resetRegInfo() {
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\Software\\WOW6432Node\\Proxy Labs\" /f");
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\Software\\WOW6432Node\\SB\" /f");
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\System\\ControlSet001\\Services\\pcapsvc\" /f");
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\System\\ControlSet001\\Services\\Tcpip\\Parameters\\Arp\" /f");
    }

    // 备份配置文件
    private static void backupConfig() {
        File machinePrs = new File(System.getenv("ProgramData") + "/ProxyCap/machine.prs");
        if (!machinePrs.exists()) {
            System.err.println("config : " + machinePrs.getAbsolutePath() + " not exist");
            return;
        }
        try {
            File target = new File(PROXYCAP_BACKUP_CONFIG.getAbsolutePath() + "/"
                    + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(System.currentTimeMillis()))
                    + "_machine.prs");
            try (FileOutputStream fos = new FileOutputStream(target)) {
                Files.copy(machinePrs.toPath(), fos);
            }
            System.out.println("config backup success : " + target.getName());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("config backup error: " + e.getMessage());
        }
    }

    // 判断并下载安装包
    private static void downloadProxyCap() throws Exception {
        if (PROXYCAP_FILE.exists() && PROXYCAP_FILE.length() > 0) {
            return;
        }
        HttpRequest req = HttpRequest.newBuilder().uri(new URI(PROXYCAP_INSTALL_URL)).GET().build();
        HttpResponse<InputStream> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = resp.body(); FileOutputStream out = new FileOutputStream(PROXYCAP_FILE)) {
            byte[] buf = new byte[102400];
            int n;
            long downloaded = 0;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;
                System.out.println("download : " + downloaded);
            }
            out.flush();
        }
    }

    /**
     * 运行命令（JDK11/21 通用，无需改启动参数）
     * 传入一整行 Windows 命令，本方法内部用 "cmd /c" 执行。
     */
    static int runCmd(String cmdLine) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmdLine);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (InputStream in = p.getInputStream()) {
                in.transferTo(System.out); // 打印输出，避免阻塞
            }
            return p.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    // -------- 下面这段你原来就有，如无需可保留 --------

    static String subText(String source, String startText, String endText, int offSet) {
        int start = source.indexOf(startText, offSet) + 1;
        if (start == 0) {
            return null;
        }
        int end = source.indexOf(endText, start + offSet + startText.length() - 1);
        if (end == -1) {
            end = source.length();
        }
        return source.substring(start + startText.length() - 1, end);
    }
}
