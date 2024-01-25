package jetbrains;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JetBrainsMain2 {


    public static void main(String[] args) {


        JFrame jf = new JFrame("jetbrains");
        jf.setSize(240, 360);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);


        JPanel panel = new JPanel(new GridLayout(2, 0, 0, 0));
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5)) {{
            add(new JLabel() {{
                setText("1.install chrome extension");
                setHorizontalAlignment(SwingConstants.LEFT);
            }});


            add(new JLabel() {{
                setText("2.start chrome");
                setHorizontalAlignment(SwingConstants.LEFT);
            }});


            add(new JLabel() {{
                setText("3.open chrome extension");
                setHorizontalAlignment(SwingConstants.LEFT);
            }});

            add(new JLabel() {{
                setText("4.clean cache");
                setHorizontalAlignment(SwingConstants.LEFT);
            }});
        }});


        panel.add(new JPanel(new GridLayout(4, 0, 0, 0)) {{
            add(new JButton() {{
                setText("start");
                setSize(100, 200);
                addActionListener(new ButtonClick());
            }});
        }});

        jf.setContentPane(panel);
        jf.setVisible(true);        // PS: 最后再设置为可显示(绘制), 所有添加的组件才会显示
    }

    static File findChromePath() {
        File file = null;
        file = new File(System.getenv("appdata") + "\\..\\Local\\Google\\Chrome\\Application\\chrome.exe");
        if (file.exists()) {
            return file;
        }
        file = new File(System.getenv("programfiles") + "\\Google\\Chrome\\Application\\chrome.exe");
        if (file.exists()) {
            return file;
        }
        return null;
    }

    static class ButtonClick implements ActionListener {


        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                run();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void run() throws Exception {
            final File chromeFile = findChromePath();
            if (chromeFile == null) {
                JOptionPane.showMessageDialog(null, "not found chrome");
            }
            System.out.println("chrome : " + chromeFile.getAbsolutePath());

            //工作空间
            final File workFile = File.createTempFile("work_", "tmp");
            workFile.delete();
            workFile.mkdirs();
            System.out.println("work : " + workFile.getAbsolutePath());

            //扩展
            final File extensionFile = new File(workFile.getAbsolutePath() + "/jetbrains_account-master");

            // user data
            final File udFile = new File(workFile.getAbsolutePath() + "/ud");

            //download and unzip
            final InputStream inputStream = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder().uri(new URI("https://github.jpy.wang/lianshufeng/jetbrains_account/archive/refs/heads/master.zip")).GET().build()
                    , HttpResponse.BodyHandlers.ofInputStream()
            ).body();
            unZipFile(inputStream, workFile);
            inputStream.close();


            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(
                    chromeFile.getAbsolutePath(),
                    "--load-extension=" + extensionFile.getAbsolutePath(),
                    "--user-data-dir=" + udFile.getAbsolutePath()
            );
            Process process = processBuilder.start();
            process.waitFor();


//            ProcessBuilder processBuilder = new ProcessBuilder()

//            String cmd = String.format(
//                    "\"%s\" --load-extension=\"%s\" --user-data-dir=\"%s\"",
//                    chromeFile.getAbsolutePath(),
//                    extensionFile.getAbsolutePath(),
//                    udFile.getAbsolutePath()
//            );
//            System.out.println(cmd);
            //start chrome
//            runCmd(
//                    "\"" + chromeFile.getAbsolutePath() + "\"" + "--load-extension="
//            );

//            JOptionPane.showMessageDialog(null, "finish");
            runCmd("rd /s /q " + workFile.getAbsolutePath());
        }
    }

    static void runCmd(String cmd) {
        try {
            Runtime.getRuntime().exec("cmd /c " + cmd).waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * 字符串
     *
     * @param source
     * @param startText
     * @param endText
     * @return
     */
    static String subText(String source, String startText, String endText, int offSet) {
        int start = source.indexOf(startText, offSet) + 1;
        if (start == -1) {
            return null;
        }
        int end = source.indexOf(endText, start + offSet + startText.length() - 1);
        if (end == -1) {
            end = source.length();
        }
        return source.substring(start + startText.length() - 1, end);
    }


    static java.util.List<String> unZipFile(final InputStream inputStream, final File targetDirectory) throws IOException {
        List<String> list = new ArrayList<String>();
        // 创建或清空目录
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            try {
                final File file = new File(targetDirectory.getAbsolutePath() + "/" + zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File pFile = new File(file.getParent());
                    if (!pFile.exists()) {
                        pFile.mkdirs();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    copyStream(zipInputStream, fileOutputStream);
                    fileOutputStream.close();
                    list.add(zipEntry.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            zipEntry.clone();
            zipInputStream.closeEntry();
        }

        return list;
    }


    public static int copyStream(InputStream in, OutputStream out) throws IOException {

        int byteCount = 0;
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        out.flush();
        return byteCount;
    }

}
