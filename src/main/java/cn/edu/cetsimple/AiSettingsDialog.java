package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

public class AiSettingsDialog extends JDialog {
    private final JComboBox<AiConfig.Provider> provider = new JComboBox<>(AiConfig.Provider.values());
    private final JComboBox<AiConfig.Protocol> protocol = new JComboBox<>(AiConfig.Protocol.values());
    private final JTextField endpoint = new JTextField();
    private final JTextField model = new JTextField();
    private final JPasswordField apiKey = new JPasswordField();
    private final JTextField authHeader = new JTextField();
    private final JTextField authPrefix = new JTextField();
    private final JLabel presetTip = new JLabel(" ");
    private AiConfig result;

    public AiSettingsDialog(Window owner, AiConfig current) {
        super(owner, "AI 接口设置", ModalityType.APPLICATION_MODAL);
        setSize(680, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createForm(current), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
    }

    public AiConfig getResult() {
        return result;
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Theme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        JLabel title = new JLabel("选择厂商，快速连接大模型");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Theme.DARK);
        JLabel tip = new JLabel("国产主流模型可自动填写参数；API 密钥只保存在本次运行内存中。 ");
        tip.setForeground(Theme.TEXT_GRAY);
        panel.add(title, BorderLayout.NORTH);
        panel.add(tip, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createForm(AiConfig current) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 12, 22));
        provider.setSelectedItem(current.provider());
        protocol.setSelectedItem(current.protocol());
        endpoint.setText(current.endpoint());
        model.setText(current.model());
        apiKey.setText(current.apiKey());
        authHeader.setText(current.authHeader());
        authPrefix.setText(current.authPrefix());
        endpoint.putClientProperty("JTextField.placeholderText", "例如 https://api.example.com/v1/chat/completions");
        model.putClientProperty("JTextField.placeholderText", "填写厂商提供的模型名称");
        apiKey.putClientProperty("JTextField.placeholderText", "本地模型或无需鉴权时可留空");
        authHeader.putClientProperty("JTextField.placeholderText", "通常是 Authorization 或 x-api-key");
        authPrefix.putClientProperty("JTextField.placeholderText", "通常是 Bearer，也可以留空");
        presetTip.setForeground(Theme.TEXT_GRAY);

        int row = 0;
        addRow(panel, row++, "厂商预设", provider);
        GridBagConstraints tipConstraints = new GridBagConstraints();
        tipConstraints.gridx = 1;
        tipConstraints.gridy = row++;
        tipConstraints.anchor = GridBagConstraints.WEST;
        tipConstraints.insets = new Insets(0, 0, 7, 0);
        panel.add(presetTip, tipConstraints);
        addRow(panel, row++, "接口协议", protocol);
        addRow(panel, row++, "完整接口地址", endpoint);
        addRow(panel, row++, "模型名称", model);
        addRow(panel, row++, "API 密钥", apiKey);
        addRow(panel, row++, "鉴权请求头", authHeader);
        addRow(panel, row, "密钥前缀", authPrefix);

        updatePresetTip((AiConfig.Provider) provider.getSelectedItem());
        provider.addActionListener(event -> applyPreset((AiConfig.Provider) provider.getSelectedItem()));
        return panel;
    }

    private void applyPreset(AiConfig.Provider selected) {
        if (selected == null) return;
        updatePresetTip(selected);
        if (selected == AiConfig.Provider.CUSTOM) return;
        protocol.setSelectedItem(selected.protocol());
        endpoint.setText(selected.endpoint());
        model.setText(selected.model());
        authHeader.setText("Authorization");
        authPrefix.setText("Bearer");
    }

    private void updatePresetTip(AiConfig.Provider selected) {
        if (selected == null) return;
        if (selected == AiConfig.Provider.CUSTOM) {
            presetTip.setText("可连接其他厂商、本地模型或学校提供的兼容服务");
        } else if (selected == AiConfig.Provider.OPENAI) {
            presetTip.setText("已填写官方接口，请根据账号权限填写模型名称");
        } else {
            presetTip.setText("已内置官方接口和推荐模型，通常只需填写 API 密钥");
        }
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(7, 0, 7, 14);
        JLabel title = new JLabel(label);
        title.setForeground(Theme.DARK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(title, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(7, 0, 7, 0);
        panel.add(field, right);
    }

    private JPanel createActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        JButton cancel = new JButton("取消");
        JButton save = new JButton("保存设置");
        Theme.styleSecondaryButton(cancel);
        Theme.stylePrimaryButton(save);
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> {
            result = new AiConfig(
                    (AiConfig.Provider) provider.getSelectedItem(),
                    (AiConfig.Protocol) protocol.getSelectedItem(),
                    endpoint.getText().trim(),
                    model.getText().trim(),
                    new String(apiKey.getPassword()).trim(),
                    authHeader.getText().trim(),
                    authPrefix.getText().trim()
            );
            dispose();
        });
        panel.add(cancel);
        panel.add(save);
        return panel;
    }
}
