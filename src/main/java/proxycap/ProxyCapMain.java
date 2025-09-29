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

    // 中文用 Unicode 转义，避免源文件编码差异导致乱码
    private static final String TXT_RESET_SERVICE       = "\u91cd\u7f6e ProxyCap \u670d\u52a1";                 // 重置 ProxyCap 服务
    private static final String TXT_UNINSTALL_SERVICE   = "\u5378\u8f7d ProxyCap \u670d\u52a1";                 // 卸载 ProxyCap 服务
    private static final String TXT_STOP_SERVICE        = "\u505c\u6b62 ProxyCap \u670d\u52a1";                 // 停止 ProxyCap 服务
    private static final String TXT_RESTART_SERVICE     = "\u91cd\u542f ProxyCap \u670d\u52a1";                 // 重启 ProxyCap 服务

    private static final String MSG_CONFIRM_RESET       = "\u91cd\u7f6e " + "[ProxyCap]" + "  ? ";
    private static final String MSG_CONFIRM_UNINSTALL   = "\u5378\u8f7d " + "[ProxyCap]" + "  ? ";
    private static final String MSG_STOP_DONE           = "\u670d\u52a1\u505c\u6b62\u5b8c\u6210";               // 服务停止完成
    private static final String MSG_RESTART_DONE        = "\u670d\u52a1\u91cd\u542f\u5b8c\u6210";               // 服务重启完成
    private static final String MSG_AFTER_REPAIR        = "\u8bf7\u542f\u52a8\u670d\u52a1\uff0c\u5e76\u6062\u590d\u914d\u7f6e\u6587\u4ef6: \n";
    private static final String MSG_AFTER_UNINSTALL     = "\u5378\u8f7d\u5b8c\u6210,\u6062\u590d\u914d\u7f6e\u6587\u4ef6: \n";

    public static void main(String[] args) {
        initFile();

        // 可选：统一字体，防止某些 LAF 下中文回退异常
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

                // 1) 同步启动服务（很快结束）
                runCmd("net start pcapsvc");

                // 2) 异步启动托盘/GUI进程，立即返回，不阻塞事件线程
                String ps = "powershell -NoProfile -WindowStyle Hidden -Command "
                        + "\"$Key='HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run'; "
                        + "$Name='ProxyCap'; "
                        + "$exe=(Get-ItemProperty -Path \\\"Registry::$Key\\\" -ErrorAction Stop).$Name; "
                        + "Start-Process -FilePath $exe\"";
                runCmdAsync(ps);

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

    /** 卸载 */
    private static void unInstall() {
        stopService();
        runCmd("msiexec /quiet /uninstall " + PROXYCAP_INSTALL_URL + " /norestart");
    }

    private static void repairInstall() {
        // 给路径加引号，以防含空格
        runCmd('"' + PROXYCAP_FILE.getAbsolutePath() + '"' + " /quiet /norestart");
    }

    /** 清除注册信息 */
    private static void resetRegInfo() {
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\Software\\WOW6432Node\\Proxy Labs\" /f");
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\Software\\WOW6432Node\\SB\" /f");
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\System\\ControlSet001\\Services\\pcapsvc\" /f");
        runCmd("reg delete \"HKEY_LOCAL_MACHINE\\System\\ControlSet001\\Services\\Tcpip\\Parameters\\Arp\" /f");
    }

    /** 备份配置文件 */
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

    /** 判断并下载安装包 */
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
     * 同步运行命令（等待结束，打印输出），JDK 11/21 通用
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

    /**
     * 异步运行命令（立刻返回，不等待）
     * 用于需要“启动即返回”的场景，例如拉起 GUI/托盘进程
     */
    static void runCmdAsync(String cmdLine) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmdLine);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start(); // 不 wait，不读输出
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 你原本的工具方法，保持不动
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
