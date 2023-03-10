package jrebel;


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
import java.util.*;

public class JrebelMain {

    private final static String url = "https://headless.zeroturnaround.com/public/api/registrations/add-jrebel-evaluation.php";

    public static void main(String[] args) {

        JFrame jf = new JFrame("JrebelEvaluation");
        jf.setSize(240, 320);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);

        // 创建内容面板，指定使用 流式布局
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 100, 5));


        JButton button = new JButton();
        button.setText("Get License");
        button.addActionListener(new JrebelMain.ButtonClick());
        panel.add(button);


        jf.setContentPane(panel);
        jf.setVisible(true);        // PS: 最后再设置为可显示(绘制), 所有添加的组件才会显示

    }


    public static int randNumber(int max, int min) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String subText(String source, String startText, String endText, int offSet) {
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

    static class ButtonClick implements ActionListener {


        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                action();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void action() throws Exception {
        System.out.println("clean .jrebel");
        File file = new File(System.getProperty("user.home") + "/.jrebel");
        if (file.exists()) {
            Arrays.stream(file.listFiles()).filter(it -> it.canWrite()).forEach(it -> {
                System.out.println("delete : " + it.getAbsolutePath());
                it.delete();
            });
        }


        StringBuffer phone = new StringBuffer();
        for (int i = 0; i < randNumber(6, 11); i++) {
            phone.append(randNumber(0, 9));
        }

        Map<String, Object> body = new HashMap<>() {{
            put("referer_url", "IDE");
            put("email", uuid() + "%40qq.com");
            put("first_name", uuid().substring(0, randNumber(3, 5)));
            put("last_name", uuid().substring(0, randNumber(3, 6)));
            put("phone", phone.toString());
            put("organization", uuid().substring(0, randNumber(1, 5)));
            put("output_format", "json");
            put("client_os", "Windows+11");
            put("guid", uuid());
            put("jrebel-version", "2023.1.2");
            put("ide", "intellij");
            put("ide-product", "IU");
            put("ide-version", "2022.3.3");
            put("jvm.version", "17.0." + randNumber(0, 20));
            put("jvm.vendor", "JetBrains+s.r.o");
            put("os.name", "Windows+11");
        }};


        System.out.println(body);

        String queryText = String.join("&", body.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).toArray(String[]::new));
        final URI uri = URI.create(url + "?" + queryText);


        final HttpClient httpClient = HttpClient.newBuilder()
                .build();


        final HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .GET()
                .uri(uri)
                .build();

        String ret = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        String content = subText(ret, "content\":\"", "\"", 0);
        System.out.println(content);
        byte[] bin = Base64.getMimeDecoder().decode(content);
        File jrebelFile = new File("c:/jrebel.lic");
        FileOutputStream fileOutputStream = new FileOutputStream(jrebelFile);
        fileOutputStream.write(bin);
        fileOutputStream.flush();
        fileOutputStream.close();

        JOptionPane.showMessageDialog(null, "please use license :  \n" + jrebelFile.getAbsolutePath());

    }


}
