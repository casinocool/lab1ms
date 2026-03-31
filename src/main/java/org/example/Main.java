package org.example;
import java.util.*;

public class Main {
    static Scanner sc = new Scanner(System.in);
    static int totalPlayers; // Общее кол-во игроков

    static class Node {
        String path;
        Integer actingPlayer; // Кто делает ход в этом узле
        double[] payoffs;     // Массив выигрышей для всех игроков
        Map<String, Node> moves = new LinkedHashMap<>();

        Node(String path) { this.path = path; }
    }

    public static void main(String[] args) {
        System.out.println("=== Универсальный конструктор динамических игр ===");

        // 1. Определяем количество участников
        System.out.print("Введите количество игроков в игре: ");
        totalPlayers = sc.nextInt();
        while (totalPlayers < 1) {
            System.out.print("Игроков должно быть как минимум 1. Повторите ввод: ");
            totalPlayers = sc.nextInt();
        }

        // 2. Строим дерево игры
        Node root = buildGame("Корень");

        // 3. Выводим структуру
        System.out.println("\n--- ГРАФ ИГРЫ ---");
        printTree(root, "");

        // 4. Решаем игру
        double[] result = solve(root);
        System.out.println("\n--- РЕЗУЛЬТАТ АНАЛИЗА ---");
        System.out.println("Рациональные выигрыши для всех игроков:");
        for (int i = 0; i < totalPlayers; i++) {
            System.out.printf("Игрок %d: %.2f\n", (i + 1), result[i]);
        }
    }

    /**
     * Рекурсивное построение дерева игры пользователем
     */
    static Node buildGame(String path) {
        System.out.println("\n--- Настройка узла: [" + path + "] ---");
        System.out.print("Это конец игры (выигрыши)? (д/н): ");
        String choice = sc.next().toLowerCase();

        Node node = new Node(path);

        if (choice.equals("д") || choice.equals("y")) {
            // Терминальный узел: вводим выигрыш для каждого игрока
            node.payoffs = new double[totalPlayers];
            System.out.println("  Введите выигрыши для " + totalPlayers + " игроков:");
            for (int i = 0; i < totalPlayers; i++) {
                System.out.print("    Выигрыш Игрока " + (i + 1) + ": ");
                node.payoffs[i] = sc.nextDouble();
            }
        } else {
            // Узел принятия решения
            System.out.print("  Какой игрок делает ход в этом узле? (от 1 до " + totalPlayers + "): ");
            int player = sc.nextInt();
            while (player < 1 || player > totalPlayers) {
                System.out.print("  Ошибка! Введите номер игрока от 1 до " + totalPlayers + ": ");
                player = sc.nextInt();
            }
            node.actingPlayer = player;

            System.out.print("  Сколько вариантов хода у Игрока " + player + "?: ");
            int movesCount = sc.nextInt();

            for (int i = 1; i <= movesCount; i++) {
                System.out.print("  Назовите ход #" + i + ": ");
                String moveName = sc.next();
                // Рекурсивный вызов для следующего уровня
                node.moves.put(moveName, buildGame(path + " -> " + moveName));
            }
        }
        return node;
    }

    /**
     * Рекурсивный расчет методом обратной индукции
     */
    static double[] solve(Node node) {
        if (node.payoffs != null) {
            return node.payoffs;
        }

        double[] optimalPayoffs = null;
        double maxBenefitForActivePlayer = Double.NEGATIVE_INFINITY;

        // Игрок, делающий ход в этом узле, просматривает все варианты впереди
        for (Node child : node.moves.values()) {
            double[] potentialOutcome = solve(child);

            // Индекс игрока в массиве (игрок 1 -> индекс 0)
            int pIdx = node.actingPlayer - 1;

            // Если этот вариант лучше для ТЕКУЩЕГО игрока, выбираем его
            if (potentialOutcome[pIdx] > maxBenefitForActivePlayer) {
                maxBenefitForActivePlayer = potentialOutcome[pIdx];
                optimalPayoffs = potentialOutcome;
            }
        }
        return optimalPayoffs;
    }

    /**
     * Красивый вывод структуры дерева
     */
    static void printTree(Node node, String indent) {
        if (node.payoffs != null) {
            System.out.print(indent + "└── [ВЫПЛАТЫ: ");
            for (double p : node.payoffs) System.out.print(p + " ");
            System.out.println("]");
        } else {
            System.out.println(indent + "├── [Узел: " + node.path + " | Ходит Игрок: " + node.actingPlayer + "]");
            for (Map.Entry<String, Node> entry : node.moves.entrySet()) {
                System.out.println(indent + "    │ ход: " + entry.getKey());
                printTree(entry.getValue(), indent + "    │");
            }
        }
    }
}