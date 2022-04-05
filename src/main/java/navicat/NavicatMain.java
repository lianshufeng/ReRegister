package navicat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.stream.Collectors;

public class NavicatMain {

    public static void main(String[] args) {
        JFrame jf = new JFrame("Navicat");
        jf.setSize(240, 320);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);

        // 创建内容面板，指定使用 流式布局
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 100, 5));


        JButton button = new JButton();
        button.setText("Reset Navicat");
        button.addActionListener(new ButtonClick());
        panel.add(button);


        jf.setContentPane(panel);
        jf.setVisible(true);        // PS: 最后再设置为可显示(绘制), 所有添加的组件才会显示
    }


    static class ButtonClick implements ActionListener {


        @Override
        public void actionPerformed(ActionEvent e) {
            int option = JOptionPane.showConfirmDialog(null, "remove " + "[Navicat]" + " ? ", "ReRegister", JOptionPane.YES_NO_OPTION);
            if (option == 0) {
                try {
                    String ret = runCmd("reg query  HKEY_CURRENT_USER\\SOFTWARE\\Classes\\CLSID").trim();
                    //寻找所有有info的节点

                    Arrays.stream(ret.split("\r\n")).filter((it) -> {
                        String cmd = "reg query  " + it + "\\info";
                        return runCmd(cmd).length() > 0;
                    }).forEach((it) -> {
                        System.out.println("remove : " + it);
                        runCmd("reg delete \"" + it + "\" /f");
                    });


                    // navicat 15
                    runCmd("reg delete \"HKEY_CURRENT_USER\\SOFTWARE\\PremiumSoft\\NavicatPremium\\Registration15XCS\" /f");

                    // navicat 16
                    runCmd("reg delete \"HKEY_CURRENT_USER\\SOFTWARE\\PremiumSoft\\NavicatPremium\\Registration16XCS\" /f");

                    JOptionPane.showMessageDialog(null, "finish");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "error");
                }
            }
        }
    }

    static String runCmd(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec("cmd /c " + cmd);
            InputStream inputStream = process.getInputStream();
            process.waitFor();
            byte[] bin = copyToByteArray(inputStream);
            inputStream.close();
            return new String(bin);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
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


    static byte[] copyToByteArray(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            copy((InputStream) in, out);
            return out.toByteArray();
        }
    }

    static int copy(InputStream in, OutputStream out) throws IOException {
        int byteCount = 0;

        int bytesRead;
        for (byte[] buffer = new byte[4096]; (bytesRead = in.read(buffer)) != -1; byteCount += bytesRead) {
            out.write(buffer, 0, bytesRead);
        }

        out.flush();
        return byteCount;
    }

}
