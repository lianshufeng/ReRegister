package proxycap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProxyCapMain {

    public static void main(String[] args) {
        JFrame jf = new JFrame("ProxyCap");
        jf.setSize(240, 320);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);

        // 创建内容面板，指定使用 流式布局
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 100, 5));


        JButton button = new JButton();
        button.setText("Reset ProxyCap");
        button.addActionListener(new ButtonClick());
        panel.add(button);


        jf.setContentPane(panel);
        jf.setVisible(true);        // PS: 最后再设置为可显示(绘制), 所有添加的组件才会显示
    }


    static class ButtonClick implements ActionListener {


        @Override
        public void actionPerformed(ActionEvent e) {
            int option = JOptionPane.showConfirmDialog(null, "remove " + "[ProxyCap]" + " ? ", "ReRegister", JOptionPane.YES_NO_OPTION);
            if (option == 0) {
                try {
                    String ret = HttpClient.newHttpClient().send(
                            HttpRequest.newBuilder().uri(new URI("https://web.api.jpy.wang/proxycap/Registration.reg")).GET().build()
                            , HttpResponse.BodyHandlers.ofString()
                    ).body();

                    String reg = subText(ret, "<pre>", "</pre>", -1);
                    System.out.println("reg : " + reg);
                    //写到临时文件
                    File file = File.createTempFile("temp", "reg");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(reg.getBytes());
                    fileOutputStream.close();

                    //到注册表里
                    runCmd("regedit /s " + file.getAbsolutePath());

                    file.delete();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }


                JOptionPane.showMessageDialog(null, "finish");
            }
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

}
