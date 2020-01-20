package org.mikitabaltrushevich.fc.jstools.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class JSToolCodeEditor extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JMenuBar toolbar = new JMenuBar();

    private final JMenu fileMenu = new JMenu("File");
    private final JMenuItem newFileBtn = new JMenuItem("New");
    private final JMenuItem openFileBtn = new JMenuItem("Open");
    private final JMenuItem saveFileBtn = new JMenuItem("Save");
    private final JMenuItem saveAsFileBtn = new JMenuItem("Save As");
    private final JMenuItem exitFileBtn = new JMenuItem("Exit");

    private final JMenu editMenu = new JMenu("Edit");
    private final JMenuItem copyEditBtn = new JMenuItem("Copy");
    private final JMenuItem cutEditBtn = new JMenuItem("Cut");
    private final JMenuItem pasteEditBtn = new JMenuItem("Paste");
    private final JMenuItem deleteEditBtn = new JMenuItem("Delete");
    private final JMenuItem undoEditBtn = new JMenuItem("Undo");
    private final JMenuItem redoEditBtn = new JMenuItem("Redo");
    private final JMenuItem findEditBtn = new JMenuItem("Find");
    private final JMenuItem replaceEditBtn = new JMenuItem("Replace");
    private final JMenuItem selectAllEditBtn = new JMenuItem("Select All");

    private final JMenu viewMenu = new JMenu("View");
    private final JMenuItem themeViewBtn = new JMenuItem("Theme");

    private final JMenu compilerMenu = new JMenu("Compiler");
    private final JMenuItem compileCompilerBtn = new JMenuItem("Compile");

    private final JMenu helpMenu = new JMenu("Help");
    private final JMenuItem apiHelpBtn = new JMenuItem("Robocode API");
    private final JMenuItem aboutHelpBtn = new JMenuItem("About");

    private final RSyntaxTextArea codeArea = new RSyntaxTextArea();
    private final RTextScrollPane scroller = new RTextScrollPane(codeArea);

    private File savedFile = null;

    private String readFromFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String buf = null;
            while ((buf = br.readLine()) != null) {
                sb.append("\n").append(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br.close();
        }
        return sb.toString();
    }

    private void writeToFile(File file, String strToWrite) throws Exception {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(strToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bw.close();
        }
    }

    private void saveFile() {
        final JFileChooser fileChooser = new JFileChooser();

        int ret = fileChooser.showDialog(null, "Save");
        if (ret == JFileChooser.APPROVE_OPTION) {
            savedFile = fileChooser.getSelectedFile();

            try {
                writeToFile(savedFile, codeArea.getText());
            } catch (Exception t) {
                t.printStackTrace();
                JOptionPane.showMessageDialog(getParent(), t.getMessage());
            }
        }
    }

    public JSToolCodeEditor() {

        this.setTitle("JSToolEditor");

        final Container container = this.getContentPane();
        container.setLayout(new BorderLayout());

        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setAutoIndentEnabled(true);
        codeArea.setCodeFoldingEnabled(true);

        newFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int reply = JOptionPane.showConfirmDialog(getParent(), "Are you sure? All your changes will be lost.",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (reply != JOptionPane.YES_OPTION) {
                    return;
                }
                codeArea.setText("");
            }
        });

        openFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int reply = JOptionPane.showConfirmDialog(getParent(), "Are you sure? All your changes will be lost. ",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (reply != JOptionPane.YES_OPTION) {
                    return;
                }
                JFileChooser fileChooser = new JFileChooser();

                int ret = fileChooser.showOpenDialog(null);

                if (ret == JFileChooser.APPROVE_OPTION) {
                    savedFile = new File(fileChooser.getSelectedFile().getAbsolutePath());
                    try {
                        String data = readFromFile(savedFile);
                        codeArea.setText(data);
                    } catch (Exception t) {
                        t.printStackTrace();
                        JOptionPane.showMessageDialog(getParent(), t.getMessage());
                    }
                }
            }
        });

        saveFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (savedFile == null) {
                    saveFile();
                } else {
                    try {
                        writeToFile(savedFile, codeArea.getText());
                    } catch (Exception t) {
                        t.printStackTrace();
                        JOptionPane.showMessageDialog(getParent(), t.getMessage());
                    }
                }
            }
        });

        saveAsFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });
        
        exitFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int reply = JOptionPane.showConfirmDialog(getParent(), "Are you sure? All your changes will be lost. ",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (reply != JOptionPane.YES_OPTION) {
                    return;
                }
                dispose();
            }
        });

        fileMenu.add(newFileBtn);
        fileMenu.add(openFileBtn);
        fileMenu.add(saveFileBtn);
        fileMenu.add(saveAsFileBtn);
        fileMenu.add(exitFileBtn);

        editMenu.add(copyEditBtn);
        editMenu.add(cutEditBtn);
        editMenu.add(pasteEditBtn);
        editMenu.add(deleteEditBtn);
        editMenu.add(undoEditBtn);
        editMenu.add(redoEditBtn);
        editMenu.add(findEditBtn);
        editMenu.add(replaceEditBtn);
        editMenu.add(selectAllEditBtn);

        viewMenu.add(themeViewBtn);

        compilerMenu.add(compileCompilerBtn);

        helpMenu.add(apiHelpBtn);
        helpMenu.add(aboutHelpBtn);

        toolbar.add(fileMenu);
        toolbar.add(editMenu);
        toolbar.add(viewMenu);
        toolbar.add(compilerMenu);
        toolbar.add(helpMenu);

        container.add(toolbar, BorderLayout.NORTH);
        container.add(scroller, BorderLayout.CENTER);
    }
}
