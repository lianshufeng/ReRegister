package openai;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class OpenAiMain {

    // 定义表头和数据
    final static String[] columnNames = {"ApiKey", "总共", "剩余"};
    final static Object[][] datas = {};
    // 创建默认表格模型，并将数据传递给它
    final static DefaultTableModel tableModel = new DefaultTableModel(datas, columnNames);


    public static void main(String[] args) {
        JFrame jf = new JFrame("OpenAi - 验ApiKey - 拖拽文本");
        jf.setSize(800, 600);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);

        // 创建内容面板，指定使用 流式布局
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 100, 5));

        // 添加文件拖拽目标监听器
        new DropTarget(panel, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    // 获取拖拽操作中的传输数据
                    Transferable transferable = event.getTransferable();

                    // 检查是否有文件类型的数据被传输
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                        // 获取拖拽操作中的文件列表
                        java.util.List<File> fileList = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        // 在标签中显示第一个文件的路径
                        String filePath = fileList.get(0).getAbsolutePath();
                        loadFile(new File(filePath));

                        event.dropComplete(true);
                    } else {
                        event.rejectDrop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    event.rejectDrop();
                }
            }
        });


        // 创建 JTable 组件，并将表格模型传递给它
        JTable table = new JTable(tableModel);
        // 添加键盘事件监听器以实现复制功能
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    // 检查是否有选定单元格
                    if (table.getSelectedRowCount() > 0 && table.getSelectedColumnCount() > 0) {
                        StringBuilder sb = new StringBuilder();
                        int[] selectedRows = table.getSelectedRows();
                        for (int row : selectedRows) {
                            sb.append(table.getValueAt(row, 0));
                            if (row > 1) {
                                sb.append(System.lineSeparator());
                            }
                        }

                        // 复制选定单元格内容到剪贴板
                        StringSelection selection = new StringSelection(sb.toString());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                    }
                }
            }
        });

        // 阻止默认的复制事件
        InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = table.getActionMap();
        KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        inputMap.put(copyKeyStroke, "none");
        actionMap.put("none", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Do nothing
            }
        });

        // 将 JList 放置在 JScrollPane 中，以便可以滚动查看所有列表项
        JScrollPane scrollPane = new JScrollPane(table);


        // 将 JScrollPane 添加到 JFrame 的内容面板中
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton importButton = new JButton("导入数据");
        importButton.addActionListener((it) -> {
            // 创建文件选择器
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getenv("USERPROFILE") + "/Desktop"));

            // 显示文件选择对话框，并获取用户选择的结果
            int result = fileChooser.showOpenDialog(jf);
            if (result == JFileChooser.APPROVE_OPTION) {
                // 用户选择了一个文件
                java.io.File selectedFile = fileChooser.getSelectedFile();
                loadFile(selectedFile);
            }
        });
        JButton refreshButton = new JButton("刷新数据");
        refreshButton.addActionListener((it) -> {
            refreshDatas();
        });
        panel.add(importButton, BorderLayout.PAGE_START);
        panel.add(refreshButton, BorderLayout.PAGE_END);


        jf.setLayout(new BorderLayout());
        jf.add(panel, BorderLayout.CENTER);
        jf.setVisible(true);
    }


    private static void refreshDatas() {

        //清空
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt("", i, 1);
            tableModel.setValueAt("", i, 2);
        }


        final Map<Integer, String> items = new ConcurrentHashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String apiKey = (String) tableModel.getValueAt(i, 0);
            if (apiKey == null || apiKey.length() == 0) {
                continue;
            }
            items.put(i, apiKey);
        }


        final CountDownLatch countDownLatch = new CountDownLatch(items.size());

        final Map<Integer, Map<String, Object>> updateItems = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        items.entrySet().forEach((entry) -> {
            executorService.execute(() -> {
                String subscription = get("https://openai.jpy.wang/v1/dashboard/billing/subscription", Map.of("Authorization", "Bearer " + entry.getValue()));
                //总余额
                final String system_hard_limit_usd = subText(subscription, "\"system_hard_limit_usd\":", ",", -1).trim();


                //取出当前时间
                Date nowDate = new Date(System.currentTimeMillis());
                // 创建一个日期时间格式化器
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                // 将日期对象格式化为指定格式的字符串
                final String end_date = formatter.format(nowDate);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date(nowDate.getTime()));
                calendar.add(Calendar.DAY_OF_MONTH, -99);
                final String start_date = formatter.format(calendar.getTime());
                final String query = "start_date=" + start_date + "&end_date=" + end_date;
                String usage = get("https://openai.jpy.wang/v1/dashboard/billing/usage?" + query, Map.of("Authorization", "Bearer " + entry.getValue()));
                //当前消费
                final String total_usage = subText(usage, "\"total_usage\":", "}", -1).trim();


                Map<String, Object> item = new HashMap<>();
                item.put("system_hard_limit_usd", system_hard_limit_usd);
                item.put("total_usage", total_usage);
                updateItems.put(entry.getKey(), item);

                countDownLatch.countDown();
            });
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 更新
        updateItems.entrySet().forEach((entry) -> {
            String system_hard_limit_usd = (String) entry.getValue().get("system_hard_limit_usd");
            String total_usage = (String) entry.getValue().get("total_usage");

            tableModel.setValueAt(system_hard_limit_usd, entry.getKey(), 1);
            tableModel.setValueAt(String.valueOf(Double.parseDouble(system_hard_limit_usd) - Double.valueOf(total_usage) / 100), entry.getKey(), 2);
        });

    }

    private static byte[] readFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bin = new byte[((Number) file.length()).intValue()];
            fileInputStream.read(bin);
            fileInputStream.close();
            return bin;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static void loadFile(File file) {
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }

        byte[] bin = readFile(file);
        var lines = Arrays.stream(new String(bin).split("\\r\\n|\\n")).filter(it -> it != null && it.length() > 0).collect(Collectors.toList());
        lines.forEach((it) -> {
            tableModel.addRow(new Object[]{it, "", ""});
        });
    }


    private static String get(String url, Map<String, String> headers) {
        try {
            final URI uri = URI.create(url);
            final HttpClient httpClient = HttpClient.newBuilder()
                    .build();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .GET()
                    .uri(uri);

            headers.entrySet().forEach((entry) -> {
                builder.header(entry.getKey(), entry.getValue());
            });

            final HttpRequest request = builder.build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String subText(String source, String startText, String endText, int offSet) {
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
