package org.example;


import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import javax.swing.JButton;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JFrame {
    private mxGraphComponent graphComponent;
    private mxGraph graph;
    private Object parent;
    private int totalPlayers = 2;

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

        graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent, BorderLayout.CENTER);

        new mxKeyboardHandler(graphComponent);

        JPanel panel = new JPanel();
        JButton solveBtn = new JButton("РАССЧИТАТЬ");
        JButton playersBtn = new JButton("Игроков: " + totalPlayers);
        panel.add(new JLabel("ПКМ - добавить ход | Del - Удалить |"));
        panel.add(playersBtn);
        panel.add(solveBtn);
        getContentPane().add(panel, BorderLayout.SOUTH);

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
            // По умолчанию создаем "лист" с нулевыми выигрышами
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
            resetStyles();

            mxCell rootCell = (mxCell) graph.getChildVertices(parent)[0];
            GameNode rootLogic = convertToLogic(rootCell);

            double[] res = solve(rootLogic);
            highlightPath(rootLogic);
            JOptionPane.showMessageDialog(this, "Оптимальный исход: " + Arrays.toString(res));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка! Проверьте данные в узлах.\n" + ex.getMessage());
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

    private double[] solve(GameNode node) {
        if (node.payoffs != null) return node.payoffs;
        double[] best = null;
        double max = Double.NEGATIVE_INFINITY;
        int pIdx = node.player - 1;

        for (Map.Entry<mxCell, GameNode> entry : node.children.entrySet()) {
            double[] current = solve(entry.getValue());
            double currentVal = current[pIdx];
            if (currentVal > max + 0.0001) {
                max = currentVal;
                best = current;
                node.bestEdges.clear();
                node.bestEdges.add(entry.getKey());
            }
            else if (Math.abs(currentVal - max) < 0.0001) {
                node.bestEdges.add(entry.getKey());
            }
        }
        return best;
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