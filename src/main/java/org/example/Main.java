package org.example;


import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main extends JFrame {

    private mxGraph graph;
    private Object parent;
    private int totalPlayers = 2;

    public Main() {
        super("Редактор динамических игр (Теория игр)");

        graph = new mxGraph();
        parent = graph.getDefaultParent();

        // Настройки редактора
        graph.setCellsEditable(true); // 2 клика для редактирования текста
        graph.setCellsMovable(true);
        graph.setCellsResizable(false);

        // Создаем первый узел
        graph.getModel().beginUpdate();
        try {
            graph.insertVertex(parent, null, "P1", 400, 20, 80, 40, "shape=ellipse;fillColor=#C3D9FF");
        } finally {
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent, BorderLayout.CENTER);

        // Кнопки управления
        JPanel panel = new JPanel();
        JButton solveBtn = new JButton("РАССЧИТАТЬ");
        JButton playersBtn = new JButton("Игроков: " + totalPlayers);
        panel.add(new JLabel("ПКМ - добавить ход | "));
        panel.add(playersBtn);
        panel.add(solveBtn);
        getContentPane().add(panel, BorderLayout.SOUTH);

        // Добавление ходов через правую кнопку мыши
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                    if (cell instanceof mxCell && ((mxCell) cell).isVertex()) {
                        addNextStep((mxCell) cell);
                    }
                }
            }
        });

        // Настройка кол-ва игроков
        playersBtn.addActionListener(e -> {
            String val = JOptionPane.showInputDialog("Введите количество игроков:", totalPlayers);
            if (val != null) {
                totalPlayers = Integer.parseInt(val);
                playersBtn.setText("Игроков: " + totalPlayers);
            }
        });

        // Запуск алгоритма
        solveBtn.addActionListener(e -> calculateGame());

        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void addNextStep(mxCell parentCell) {
        String moveName = JOptionPane.showInputDialog("Название хода:", "Ход");
        if (moveName == null) return;

        graph.getModel().beginUpdate();
        try {
            // По умолчанию создаем "лист" с нулевыми выигрышами
            String defaultPayoffs = "0" + ",0".repeat(totalPlayers - 1);
            Object child = graph.insertVertex(parent, null, defaultPayoffs, 0, 0, 80, 40, "fillColor=#D5E8D4");
            graph.insertEdge(parent, null, moveName, parentCell, child);

            // Авто-выравнивание дерева
            new mxHierarchicalLayout(graph).execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }
    }

    // --- ЛОГИКА РАСЧЕТА ---

    static class GameNode {
        Integer player;
        double[] payoffs;
        List<GameNode> next = new ArrayList<>();
    }

    private void calculateGame() {
        try {
            mxCell rootCell = (mxCell) graph.getChildVertices(parent)[0];
            GameNode rootLogic = convertToLogic(rootCell);
            double[] res = solve(rootLogic);
            JOptionPane.showMessageDialog(this, "Оптимальный исход: " + Arrays.toString(res));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка! Проверьте данные в узлах.\n" + ex.getMessage());
        }
    }

    private GameNode convertToLogic(mxCell cell) {
        GameNode node = new GameNode();
        String val = cell.getValue().toString().trim();

        if (val.toUpperCase().startsWith("P")) {
            node.player = Integer.parseInt(val.substring(1));
            for (int i = 0; i < cell.getEdgeCount(); i++) {
                mxCell edge = (mxCell) cell.getEdgeAt(i);
                if (edge.getSource() == cell) {
                    node.next.add(convertToLogic((mxCell) edge.getTarget()));
                }
            }
        } else {
            String[] parts = val.split(",");
            node.payoffs = new double[totalPlayers];
            for (int i = 0; i < totalPlayers; i++) node.payoffs[i] = Double.parseDouble(parts[i]);
        }
        return node;
    }

    private double[] solve(GameNode node) {
        if (node.payoffs != null) return node.payoffs;
        double[] best = null;
        double max = Double.NEGATIVE_INFINITY;
        int pIdx = node.player - 1;

        for (GameNode child : node.next) {
            double[] cur = solve(child);
            if (cur[pIdx] > max) {
                max = cur[pIdx];
                best = cur;
            }
        }
        return best;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}