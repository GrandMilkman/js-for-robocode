package org.mikitabaltrushevich.fc.jstools.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.commons.cli.ParseException;

import org.mikitabaltrushevich.fc.jstools.JSTool;

public class JSToolMainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JPanel statusBarPanel = new JPanel();
    private final JLabel statusBarLabel = new JLabel();

    private final JButton installBtn = new JButton("Install");
    private final JButton uninstallBtn = new JButton("Uninstall");

    private final JLabel pathToRobocodeLabel = new JLabel("Path to Robocode:");
    private final JTextField pathToRobocodeField = new JTextField();
    private final JButton pathToRobocodeBtn = new JButton("Browse");

    private final JButton createBtn = new JButton("Create robot");

    private final JButton checkBtn = new JButton("Check");

    private final JLabel pathToJsRobotLabel = new JLabel("Path to JS Robot");
    private final JTextField pathToJsRobotField = new JTextField();
    private final JButton pathToJsRobotBtn = new JButton("Browse");

    private final JLabel authorLabel = new JLabel("Author name: ");
    private final JTextField authorField = new JTextField();

    private final JLabel robotNameLabel = new JLabel("Robot name: ");
    private final JTextField robotNameField = new JTextField();

    private final JLabel descriptionLabel = new JLabel("Description: ");
    private final JTextField descriptionField = new JTextField();

    private final JLabel versionLabel = new JLabel("Version: ");
    private final JTextField versionField = new JTextField();

    private final JButton editorBtn = new JButton("Source editor");

    public JSToolMainFrame() {

        this.setTitle("JSTool");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final Container container = this.getContentPane();
        container.setLayout(new BorderLayout());

        statusBarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusBarPanel.add(statusBarLabel);
        statusBarPanel.setPreferredSize(new Dimension(getWidth(), 28));

        container.add(new JLabel("toolbar"), BorderLayout.NORTH);
        container.add(statusBarPanel, BorderLayout.SOUTH);

        final JPanel panel = new JPanel();
        final GroupLayout groupLayout = new GroupLayout(panel);
        groupLayout.setAutoCreateContainerGaps(true);
        groupLayout.setAutoCreateGaps(true);
        panel.setLayout(groupLayout);

        pathToRobocodeField.setPreferredSize(new Dimension(280, 20));
        authorField.setPreferredSize(new Dimension(120, 20));
        pathToRobocodeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int ret = fileChooser.showDialog(null, "Browse");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = fileChooser.getSelectedFile();
                    pathToRobocodeField.setText(JSTool.changeSeparatorOrientation(chosenFile.getPath() + "\\"));
                }
            }
        });

        installBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!JSTool.install(pathToRobocodeField.getText())) {
                    setStatusBarText("Error during patch installation", Color.RED);
                    return;
                }
                setStatusBarText("Patch was successfully installed", Color.GREEN);

                enableRobotCreation(true);
            }
        });

        uninstallBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!JSTool.uninstall(pathToRobocodeField.getText())) {
                    setStatusBarText("Error during patch uninstallation", Color.RED);
                    return;
                }
                setStatusBarText("Patch was successfully uninstalled", Color.GREEN);

                enableRobotCreation(false);
            }
        });

        checkBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pathToRobocodeField.getText() == null || pathToRobocodeField.getText().length() == 0) {
                    setStatusBarText("Please, specify path to your Robocode", Color.YELLOW);
                    return;
                }
                if (JSTool.check(pathToRobocodeField.getText())) {
                    enableRobotCreation(true);

                    setStatusBarText("Your robocode is already patched!", Color.GREEN);
                } else {
                    enableRobotCreation(false);

                    setStatusBarText("No patch is found!", Color.RED);
                }
            }
        });

        pathToJsRobotBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();

                final int ret = fileChooser.showDialog(null, "Browse");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = fileChooser.getSelectedFile();
                    pathToJsRobotField.setText(JSTool.changeSeparatorOrientation(chosenFile.getPath()));
                }
            }
        });

        enableRobotCreation(false);

        createBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final File propsFile = JSTool.createPropertyFile(
                            pathToRobocodeField.getText(), 
                            pathToJsRobotField.getText(), 
                            authorField.getText(), 
                            robotNameField.getText(), 
                            descriptionField.getText().split(" "), 
                            versionField.getText()
                    );
                    JSTool.create(
                            pathToRobocodeField.getText(), 
                            pathToJsRobotField.getText(), 
                            propsFile
                    );
                    setStatusBarText("Robot created successfully", Color.GREEN);
                } catch (ParseException e1) {
                    setStatusBarText("JSRobot file does not exist", Color.RED);
                }
            }
        });

        editorBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JSToolCodeEditor codeEditorFrame = new JSToolCodeEditor();
                codeEditorFrame.setVisible(true);

                codeEditorFrame.setSize(640, 480);
                codeEditorFrame.setLocationRelativeTo(null);
            }
        });

        groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
                .addComponent(pathToRobocodeLabel)
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(pathToRobocodeField)
                        .addComponent(pathToRobocodeBtn))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(checkBtn))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(installBtn)
                        .addComponent(uninstallBtn))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(pathToJsRobotLabel)
                        .addComponent(editorBtn))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(pathToJsRobotField)
                        .addComponent(pathToJsRobotBtn))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(authorLabel)
                        .addComponent(authorField))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(robotNameLabel)
                        .addComponent(robotNameField))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(descriptionLabel)
                        .addComponent(descriptionField))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(versionLabel)
                        .addComponent(versionField))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(createBtn))
                );

        groupLayout.linkSize(SwingConstants.HORIZONTAL, uninstallBtn, installBtn, checkBtn);

        groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(pathToRobocodeLabel)
                        .addComponent(pathToRobocodeField)
                        .addComponent(checkBtn)
                        .addComponent(installBtn)
                        .addComponent(uninstallBtn)
                        .addComponent(pathToJsRobotLabel)
                        .addComponent(pathToJsRobotField)
                        .addComponent(authorLabel)
                        .addComponent(robotNameLabel)
                        .addComponent(descriptionLabel)
                        .addComponent(versionLabel)
                        .addComponent(createBtn))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(pathToRobocodeBtn)
                        .addComponent(pathToJsRobotBtn)
                        .addComponent(editorBtn)
                        .addComponent(authorField)
                        .addComponent(robotNameField)
                        .addComponent(descriptionField)
                        .addComponent(versionField))
                );

        container.add(panel, BorderLayout.CENTER);

        this.setSize(640, 480);
        this.setLocationRelativeTo(null);
    }

    private void setStatusBarText(String text, Color c) {
        statusBarLabel.setText(text);
        statusBarPanel.setBackground(c);
    }

    private void enableRobotCreation(boolean flag) {
        installBtn.setVisible(!flag);
        uninstallBtn.setVisible(flag);

        pathToJsRobotLabel.setEnabled(flag);
        pathToJsRobotField.setEnabled(flag);
        pathToJsRobotBtn.setEnabled(flag);

        authorLabel.setEnabled(flag);
        authorField.setEnabled(flag);

        robotNameLabel.setEnabled(flag);
        robotNameField.setEnabled(flag);

        descriptionLabel.setEnabled(flag);
        descriptionField.setEnabled(flag);

        versionLabel.setEnabled(flag);
        versionField.setEnabled(flag);

        createBtn.setEnabled(flag);
        
        editorBtn.setEnabled(!flag);
    }
}
