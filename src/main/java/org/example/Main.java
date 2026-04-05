package org.example;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.view.mxGraph;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JFrame {
    private mxGraphComponent graphComponent;
    private mxGraph graph;
    private Object parent;
    private int totalPlayers = 2;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final List<String> changeHistory = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public Main() {
        super("Редактор динамических игр (Теория игр)");

        graph = new mxGraph();
        parent = graph.getDefaultParent();


        graph.setCellsEditable(true);
        graph.setCellsMovable(true);
        graph.setCellsResizable(false);
        graph.setAllowDanglingEdges(false);

        graph.getModel().beginUpdate();
        try {
            graph.insertVertex(parent, null, "P1", 400, 20, 80, 40, "shape=ellipse;fillColor=#C3D9FF");
        } finally {
            graph.getModel().endUpdate();
        }

        graph.getModel().addListener(mxEvent.CHANGE, (sender, evt) -> {
            // Чтобы не спамить в логи техническими изменениями,
            // записываем только когда меняется текст (Label)
            logAction("Изменена структура или данные графа");
        });


        graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent, BorderLayout.CENTER);

        new mxKeyboardHandler(graphComponent);

        JPanel panel = new JPanel();
        JButton solveBtn = new JButton("РАССЧИТАТЬ");
        JButton playersBtn = new JButton("Игроков: " + totalPlayers);
        JButton logBtn = new JButton("СОХРАНИТЬ ЛОГ");
        JButton saveBtn = new JButton("СОХРАНИТЬ JSON");
        JButton loadBtn = new JButton("ЗАГРУЗИТЬ JSON");

        panel.add(new JLabel("ПКМ - добавить ход | Del - Удалить |"));
        panel.add(playersBtn);
        panel.add(solveBtn);
        panel.add(saveBtn);
        panel.add(loadBtn);
        panel.add(logBtn);

        getContentPane().add(panel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> saveToJson());
        loadBtn.addActionListener(e -> loadFromJson());
        logBtn.addActionListener(e -> saveHistoryToFile());

        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                    showContextMenu(e, (mxCell) cell);
                }
            }
        });

        playersBtn.addActionListener(e -> {
            String val = JOptionPane.showInputDialog("Введите количество игроков:", totalPlayers);
            if (val != null) {
                totalPlayers = Integer.parseInt(val);
                playersBtn.setText("Игроков: " + totalPlayers);
            }
        });

        solveBtn.addActionListener(e -> calculateGame());

        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
    private void logAction(String action) {
        String time = sdf.format(new Date());
        changeHistory.add("[" + time + "] " + action);
    }

    private void saveHistoryToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("game_history.txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                writer.println("ИСТОРИЯ ИЗМЕНЕНИЙ ГРАФА:");
                for (String line : changeHistory) writer.println(line);
                JOptionPane.showMessageDialog(this, "Лог сохранен успешно!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при сохранении лога!");
            }
        }
    }

    static class SaveModel {
        int players;
        List<NodeDto> nodes = new ArrayList<>();
        List<EdgeDto> edges = new ArrayList<>();
    }

    static class NodeDto {
        String id, value;
        double x, y;
    }

    static class EdgeDto {
        String sourceId, targetId, label;
    }

    private void saveToJson() {
        SaveModel model = new SaveModel();
        model.players = totalPlayers;

        Object[] vertices = graph.getChildVertices(parent);
        for (Object v : vertices) {
            mxCell cell = (mxCell) v;
            NodeDto n = new NodeDto();
            n.id = cell.getId();
            n.value = cell.getValue().toString();
            n.x = cell.getGeometry().getX();
            n.y = cell.getGeometry().getY();
            model.nodes.add(n);
        }

        Object[] edges = graph.getChildEdges(parent);
        for (Object e : edges) {
            mxCell edge = (mxCell) e;
            EdgeDto ed = new EdgeDto();
            ed.sourceId = edge.getSource().getId();
            ed.targetId = edge.getTarget().getId();
            ed.label = edge.getValue() != null ? edge.getValue().toString() : "";
            model.edges.add(ed);
        }

        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                gson.toJson(model, writer);
                JOptionPane.showMessageDialog(this, "Игра сохранена!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    private void loadFromJson() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileReader reader = new FileReader(fileChooser.getSelectedFile())) {
                SaveModel model = gson.fromJson(reader, SaveModel.class);

                graph.getModel().beginUpdate();
                try {
                    graph.removeCells(graph.getChildCells(parent, true, true));

                    totalPlayers = model.players;
                    Map<String, Object> idMap = new HashMap<>();

                    for (NodeDto n : model.nodes) {
                        String style = n.value.startsWith("P") ? "shape=ellipse;fillColor=#C3D9FF" : "fillColor=#D5E8D4";
                        Object vertex = graph.insertVertex(parent, n.id, n.value, n.x, n.y, 80, 40, style);
                        idMap.put(n.id, vertex);
                    }

                    for (EdgeDto ed : model.edges) {
                        graph.insertEdge(parent, null, ed.label, idMap.get(ed.sourceId), idMap.get(ed.targetId));
                    }
                } finally {
                    graph.getModel().endUpdate();
                }
                JOptionPane.showMessageDialog(this, "Игра загружена! Игроков: " + totalPlayers);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Ошибка при чтении JSON!");
            }
        }
    }


    private void showContextMenu(MouseEvent e, mxCell cell) {
        JPopupMenu menu = new JPopupMenu();

        if (cell != null && cell.isVertex()) {
            JMenuItem addMove = new JMenuItem("Добавить ход");
            addMove.addActionListener(al -> addNextStep(cell));
            menu.add(addMove);
        }

        if (cell != null) {
            JMenuItem deleteItem = new JMenuItem("Удалить выбранное");
            deleteItem.addActionListener(al -> {
                graph.removeCells(new Object[]{cell});
            });
            menu.add(deleteItem);
        }

        // Если нажали на пустое место
        if (cell == null) {
            JMenuItem addRoot = new JMenuItem("Создать новый узел P1");
            addRoot.addActionListener(al -> {
                graph.getModel().beginUpdate();
                try {
                    graph.insertVertex(parent, null, "P1", e.getX(), e.getY(), 80, 40, "shape=ellipse;fillColor=#C3D9FF");
                } finally {
                    graph.getModel().endUpdate();
                }
            });
            menu.add(addRoot);
        }

        menu.show(graphComponent.getGraphControl(), e.getX(), e.getY());
    }

    private void addNextStep(mxCell parentCell) {
        String moveName = JOptionPane.showInputDialog("Название хода:", "Ход");
        if (moveName == null) return;

        graph.getModel().beginUpdate();
        try {
            String defaultPayoffs = "0" + ",0".repeat(totalPlayers - 1);
            Object child = graph.insertVertex(parent, null, defaultPayoffs, 0, 0, 80, 40, "fillColor=#D5E8D4");
            graph.insertEdge(parent, null, moveName, parentCell, child);

            new mxHierarchicalLayout(graph).execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }
    }


    static class GameNode {
        mxCell visualCell;
        Integer player;
        double[] payoffs;
        Map<mxCell, GameNode> children = new HashMap<>();
        List<mxCell> bestEdges = new ArrayList<>();
    }


    private void resetStyles() {
        graph.getModel().beginUpdate();
        try {
            Object[] allCells = graph.getChildCells(parent, true, true);
            for (Object c : allCells) {
                mxCell cell = (mxCell) c;

                graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#6482B9", new Object[]{cell});
                graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1", new Object[]{cell});

                if (cell.isVertex()) {
                    String color = cell.getValue().toString().startsWith("P") ? "#C3D9FF" : "#D5E8D4";
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, color, new Object[]{cell});
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    private void calculateGame() {
        try {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("НАЧАЛО РАСЧЕТА ДИНАМИЧЕСКОЙ ИГРЫ");

            resetStyles();
            System.out.println("\nСТРУКТУРА ГРАФА (ТЕКСТОВЫЙ ВИД):");
            mxCell rootCell = (mxCell) graph.getChildVertices(parent)[0];
            printTextGraph(rootCell, 0);

            System.out.println("\nШАГИ АЛГОРИТМА (ОБРАТНАЯ ИНДУКЦИЯ):");
            GameNode rootLogic = convertToLogic(rootCell);
            double[] res = solveWithTrace(rootLogic,0);

            highlightPath(rootLogic);

            System.out.println("\nИТОГОВОЕ РАВНОВЕСИЕ: " + Arrays.toString(res));
            System.out.println("=".repeat(50));

            JOptionPane.showMessageDialog(this, "Оптимальный исход: " + Arrays.toString(res));

            logAction("Выполнен расчет игры. Результат: " + Arrays.toString(res));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка! Проверьте данные в узлах.\n" + ex.getMessage());
        }
    }
    private void printTextGraph(mxCell cell, int level) {
        String indent = "  ".repeat(level);
        String value = cell.getValue().toString();
        System.out.println(indent + "└── Node: " + value);

        for (int i = 0; i < cell.getEdgeCount(); i++) {
            mxCell edge = (mxCell) cell.getEdgeAt(i);
            if (edge.getSource() == cell) {
                System.out.println(indent + "    Move: [" + edge.getValue() + "]");
                printTextGraph((mxCell) edge.getTarget(), level + 2);
            }
        }
    }

    private GameNode convertToLogic(mxCell cell) {
        GameNode node = new GameNode();
        node.visualCell = cell;
        String val = cell.getValue().toString().trim();

        if (val.toUpperCase().startsWith("P")) {
            node.player = Integer.parseInt(val.substring(1));
            for (int i = 0; i < cell.getEdgeCount(); i++) {
                mxCell edge = (mxCell) cell.getEdgeAt(i);
                if (edge.getSource() == cell) {

                    node.children.put(edge, convertToLogic((mxCell) edge.getTarget()));
                }
            }
        } else {
            String[] parts = val.split(",");
            node.payoffs = new double[totalPlayers];
            for (int i = 0; i < totalPlayers; i++) node.payoffs[i] = Double.parseDouble(parts[i]);
        }
        return node;
    }

    private double[] solveWithTrace(GameNode node, int depth) {
        String indent = "  ".repeat(depth);

        if (node.payoffs != null) {
            System.out.println(indent + "└── [Лист] Достигнут финал. Выигрыши: " + Arrays.toString(node.payoffs));
            return node.payoffs;
        }

        System.out.println(indent + "┌── [Узел " + node.visualCell.getValue() + "] Анализирует Игрок " + node.player);

        double[] bestOutcome = null;
        double max = Double.NEGATIVE_INFINITY;
        int pIdx = node.player - 1;

        for (Map.Entry<mxCell, GameNode> entry : node.children.entrySet()) {
            mxCell edge = entry.getKey();
            System.out.println(indent + "│ Проверка хода: [" + edge.getValue() + "]");

            double[] current = solveWithTrace(entry.getValue(), depth + 3);
            double val = current[pIdx];

            System.out.println(indent + "│ Ход [" + edge.getValue() + "] дает Игроку " + node.player + " профит: " + val);

            if (val > max + 0.0001) {
                max = val;
                bestOutcome = current;
                node.bestEdges.clear();
                node.bestEdges.add(edge);
                System.out.println(indent + "│ +++ Это ЛУЧШИЙ вариант для игрока " + node.player);
            } else if (Math.abs(val - max) < 0.0001) {
                node.bestEdges.add(edge);
                System.out.println(indent + "│ === Еще одно равновесное решение (профит такой же)");
            }
        }

        System.out.println(indent + "└── Игрок " + node.player + " в узле " + node.visualCell.getValue() + " выбирает результат " + Arrays.toString(bestOutcome));
        return bestOutcome;
    }

    private void highlightPath(GameNode node) {
        graph.getModel().beginUpdate();
        try {
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FF0000", new Object[]{node.visualCell});
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "4", new Object[]{node.visualCell});

            for (mxCell edge : node.bestEdges) {
                graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FF0000", new Object[]{edge});
                graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "4", new Object[]{edge});

                highlightPath(node.children.get(edge));
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}