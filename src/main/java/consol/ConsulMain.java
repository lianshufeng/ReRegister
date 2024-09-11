package consol;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectStreamException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class ConsulMain extends JFrame {

    public ConsulMain(String[] args) {
        // 设置窗口大小
        setSize(1024, 768);
        setTitle("consul 控制台");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 使用BoxLayout来让元素在一行内排列
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 添加一些边距

        // 添加主机名标签和输入框
        JLabel hostLabel = new JLabel("主机名:");
        JTextField hostTextField = new JTextField(15);
        hostTextField.setText(args.length > 0 ? args[0].trim() : "http://127.0.0.1:8500");
        topPanel.add(hostLabel);
        topPanel.add(hostTextField);
        topPanel.add(Box.createHorizontalStrut(10)); // 添加水平空隙

        // 添加服务名标签和输入框
        JLabel serviceLabel = new JLabel("服务名:");
        JTextField serviceTextField = new JTextField(15);
        serviceTextField.setText(args.length > 1 ? args[1].trim() : "");
        topPanel.add(serviceLabel);
        topPanel.add(serviceTextField);


        JButton queryButton = new JButton("查询");
        topPanel.add(queryButton);

        JButton delButton = new JButton("删除");
        topPanel.add(delButton);


        // 将顶部面板添加到JFrame的北边
        add(topPanel, BorderLayout.NORTH);

        // 创建一个JTable实例
        String[] columnNames = {"ServiceID", "Node", "Datacenter"}; // 表头
        Object[][] data = { // 表格数据

        };

        final DefaultTableModel model = new DefaultTableModel(data, columnNames);
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);

        // 将JTable放入ScrollPane中
        JScrollPane scrollPane = new JScrollPane(table);

        // 将ScrollPane添加到JFrame的中心
        add(scrollPane, BorderLayout.CENTER);

        // 居中显示窗口
        setLocationRelativeTo(null);




        // 事件绑定
        queryButton.addActionListener((e) -> {
            String serviceName = serviceTextField.getText().trim();
            if ("".equals(serviceName)) {
                JOptionPane.showConfirmDialog(this, "服务名不能为空", "警告", JOptionPane.DEFAULT_OPTION);
                return;
            }
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }
            //主机名
            final String hostName = hostTextField.getText().trim();
            try {
                String ret = netGet(String.format("%s/v1/catalog/service/%s", hostName, serviceName));
                for (JSONObject item : JSONParse.Parse(ret).list) {
                    String node = item.getString("Node");
                    String datacenter = item.getString("Datacenter");
                    String serviceID = item.getString("ServiceID");
                    model.addRow(new Object[]{serviceID, node, datacenter});
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // 删除
        delButton.addActionListener((e) -> {
            //主机名
            final String hostName = hostTextField.getText().trim();
            if (JOptionPane.showConfirmDialog(this, String.format("删除选中的[%s]项服务?", table.getSelectedRows().length), "删除", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            for (int selectedRow : table.getSelectedRows()) {
                String serviceID = String.valueOf(model.getValueAt(selectedRow, 0));
                String node = String.valueOf(model.getValueAt(selectedRow, 1));
                String datacenter = String.valueOf(model.getValueAt(selectedRow, 2));
                String body = String.format("{\"Node\":\"%s\",\"Datacenter\":\"%s\",\"ServiceID\":\"%s\"}", node, datacenter, serviceID);
                try {
                    String ret = netPut(String.format("%s/v1/catalog/deregister", hostName),body );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // 刷新查询
            queryButton.doClick();
        });

    }

    private static String netPut(String url, String body) throws Exception {
        System.out.println(url);
        HttpClient client = HttpClient.newHttpClient();
        // 创建 HttpRequest 实例
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                // 参数
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                // 请求头
                .header("Content-Type", "application/json")
                .build();
        // 发送请求并获取响应
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String netGet(String url) throws Exception {
        System.out.println(url);
        HttpClient client = HttpClient.newHttpClient();
        // 创建 HttpRequest 实例
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
        // 发送请求并获取响应
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConsulMain frame = new ConsulMain(args);
            frame.setVisible(true);
        });
    }

    public static class ButtonClick implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println(e);
        }
    }


    public static class JSONObject {
        private JSONType type;
        private JSONObject[] list;
        public String string;
        private String number;
        private Boolean bool;
        private HashMap<String, JSONObject> dict;

        public JSONObject(JSONType type, Object value) {
            this.type = type;
            switch (type) {
                case List:
                    this.list = (JSONObject[]) ((JSONObject[]) value);
                    break;
                case Dict:
                    this.dict = (HashMap) value;
                    break;
                case String:
                    this.string = (String) value;
                    break;
                case Number:
                    this.number = (String) value;
                    break;
                case Boolean:
                    this.bool = (Boolean) value;
                case Null:
            }

        }

        public void set(String key, JSONObject value) {
            if (this.type != JSONType.Dict) {
                throw new RuntimeException("Not a dict");
            } else {
                this.dict.put(key, value);
            }
        }

        public JSONObject get(String key) {
            if (this.type != JSONType.Dict) {
                throw new RuntimeException("Not a dict");
            } else {
                return (JSONObject) this.dict.get(key);
            }
        }

        public String getString(String key) {
            if (this.dict.get(key) == null) {
                throw new RuntimeException("Unknown key");
            } else if (((JSONObject) this.dict.get(key)).type == JSONType.String) {
                return ((JSONObject) this.dict.get(key)).string;
            } else {
                throw new RuntimeException("Not a string");
            }
        }

        public String getNumber(String key) {
            if (this.dict.get(key) == null) {
                throw new RuntimeException("Unknown key");
            } else if (((JSONObject) this.dict.get(key)).type == JSONType.Number) {
                return ((JSONObject) this.dict.get(key)).number;
            } else {
                throw new RuntimeException("Not a number");
            }
        }

        public Boolean getBoolean(String key) {
            if (this.dict.get(key) == null) {
                throw new RuntimeException("Unknown key");
            } else if (((JSONObject) this.dict.get(key)).type == JSONType.Boolean) {
                return ((JSONObject) this.dict.get(key)).bool;
            } else {
                throw new RuntimeException("Not a boolean");
            }
        }

        public JSONObject[] getList(String key) {
            if (this.dict.get(key) == null) {
                throw new RuntimeException("Unknown key");
            } else if (((JSONObject) this.dict.get(key)).type == JSONType.List) {
                return ((JSONObject) this.dict.get(key)).list;
            } else {
                throw new RuntimeException("Not a list");
            }
        }

        public Boolean Boolean() {
            if (this.type == JSONType.Boolean) {
                return this.bool;
            } else {
                throw new RuntimeException("Not a boolean");
            }
        }

        public String Number() {
            if (this.type == JSONType.Number) {
                return this.number;
            } else {
                throw new RuntimeException("Not a number");
            }
        }

        public String String() {
            if (this.type == JSONType.String) {
                return this.string;
            } else {
                throw new RuntimeException("Not a string");
            }
        }
    }


    public static class JSONParse {
        public JSONParse() {
        }

        private static int spaceLen(String json) {
            return json.length() - json.trim().length();
        }

        private static ParseResult ParseDict(String json) {
            JSONObject obj = new JSONObject(JSONType.Dict, new HashMap());
            int len = 1;
            len += spaceLen(json.substring(len));

            while (true) {
                if (json.charAt(len) == '}') {
                    ++len;
                    len += spaceLen(json.substring(len));
                    break;
                }

                ParseResult key = ParseString(json.substring(len));
                len += key.len;
                len += spaceLen(json.substring(len));
                if (json.charAt(len) != ':') {
                    throw new RuntimeException("Invalid JSON");
                }

                ++len;
                ParseResult value = parse(json.substring(len));
                len += value.len;
                len += spaceLen(json.substring(len));
                obj.set(key.obj.string, value.obj);
                if (json.charAt(len) == '}') {
                    ++len;
                    len += spaceLen(json.substring(len));
                    break;
                }

                if (json.charAt(len) != ',') {
                    throw new RuntimeException("Invalid JSON");
                }

                ++len;
                len += spaceLen(json.substring(len));
            }

            return new ParseResult(len, obj);
        }

        private static ParseResult ParseString(String json) {
            int len = 1;

            StringBuilder sb;
            for (sb = new StringBuilder(); json.charAt(len) != '"'; ++len) {
                if (json.charAt(len) == '\\') {
                    ++len;
                    switch (json.charAt(len)) {
                        case '"':
                            sb.append('"');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            sb.append((char) Integer.parseInt(json.substring(len + 1, len + 5), 16));
                            len += 4;
                            break;
                        default:
                            throw new RuntimeException("Invalid JSON");
                    }
                } else {
                    sb.append(json.charAt(len));
                }
            }

            ++len;
            return new ParseResult(len, new JSONObject(JSONType.String, sb.toString()));
        }

        private static ParseResult ParseList(String json) {
            JSONObject[] list = new JSONObject[0];
            int len = 1;

            while (true) {
                if (json.charAt(len) == ']') {
                    ++len;
                    len += spaceLen(json.substring(len));
                    break;
                }

                ParseResult value = parse(json.substring(len));
                len += value.len;
                len += spaceLen(json.substring(len));
                JSONObject[] newList = new JSONObject[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = value.obj;
                list = newList;
                if (json.charAt(len) == ']') {
                    ++len;
                    len += spaceLen(json.substring(len));
                    break;
                }

                if (json.charAt(len) != ',') {
                    throw new RuntimeException("Invalid JSON");
                }

                ++len;
                len += spaceLen(json.substring(len));
            }

            return new ParseResult(len, new JSONObject(JSONType.List, list));
        }

        private static ParseResult ParseNumber(String json) {
            int len = 0;
            StringBuilder sb = new StringBuilder();

            while (true) {
                if (json.charAt(len) != '-' && json.charAt(len) != '+' && json.charAt(len) != 'e' && json.charAt(len) != 'E' && json.charAt(len) != '.') {
                    if ('0' > json.charAt(len) || json.charAt(len) > '9') {
                        return new ParseResult(len, new JSONObject(JSONType.Number, sb.toString()));
                    }

                    sb.append(json.charAt(len));
                } else {
                    sb.append(json.charAt(len));
                }

                ++len;
            }
        }

        private static ParseResult ParseNull(String json) {
            return new ParseResult(4, new JSONObject(JSONType.Null, (Object) null));
        }

        private static ParseResult ParseBoolean(String json) {
            return new ParseResult(json.startsWith("true") ? 4 : 5, new JSONObject(JSONType.Boolean, json.startsWith("true")));
        }

        private static ParseResult parse(String json) {
            int slen = spaceLen(json);
            json = json.substring(slen);
            if (json.length() == 0) {
                return null;
            } else {
                ParseResult result;
                if (json.startsWith("{")) {
                    result = ParseDict(json);
                } else if (json.startsWith("[")) {
                    result = ParseList(json);
                } else if (json.startsWith("\"")) {
                    result = ParseString(json);
                } else if ('0' <= json.charAt(0) && json.charAt(0) <= '9') {
                    result = ParseNumber(json);
                } else if (json.startsWith("null")) {
                    result = ParseNull(json);
                } else {
                    if (!json.startsWith("true") && !json.startsWith("false")) {
                        throw new RuntimeException("Invalid JSON");
                    }

                    result = ParseBoolean(json);
                }

                result.AddLen(slen + spaceLen(json.substring(result.len)));
                return result;
            }
        }

        public static JSONObject Parse(String json) {
            json = json.trim().replace("\n", "").replace("\r", "");
            if (json.length() == 0) {
                return null;
            } else {
                ParseResult result = parse(json);
                if (result.len != json.length()) {
                    throw new RuntimeException("Invalid JSON");
                } else {
                    return result.obj;
                }
            }
        }
    }


    public static enum JSONType {
        List,
        Dict,
        String,
        Number,
        Boolean,
        Null;

        private JSONType() {
        }
    }


    public static class ParseResult {
        public int len;
        public JSONObject obj;

        public ParseResult(int len, JSONObject obj) {
            this.len = len;
            this.obj = obj;
        }

        public void AddLen(int len) {
            this.len += len;
        }
    }


}
