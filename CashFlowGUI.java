import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.chart.*;

import java.util.*;

public class CashFlowGUI extends Application {

    Map<String, List<Edge>> graph = new HashMap<>();
    Map<String, Integer> netAmount = new HashMap<>();
    Stack<Edge> undoStack = new Stack<>();
    Stack<Edge> redoStack = new Stack<>();
    List<String> people = new ArrayList<>();

    TextArea outputArea = new TextArea();

    static class Edge {
        String from;
        String to;
        int amount;

        Edge(String from, String to, int amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }
    }

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Cash Flow Minimizer - JavaFX");

        // UI Elements
        TextField personInput = new TextField();
        personInput.setPromptText("Enter person name");

        TextField fromInput = new TextField();
        fromInput.setPromptText("From");

        TextField toInput = new TextField();
        toInput.setPromptText("To");

        TextField amountInput = new TextField();
        amountInput.setPromptText("Amount");

        Button addPersonBtn = new Button("Add Person");
        Button addTransactionBtn = new Button("Add Transaction");
        Button undoBtn = new Button("Undo");
        Button redoBtn = new Button("Redo");
        Button optimizeBtn = new Button("Minimize Cash Flow");
        Button chartBtn = new Button("Show Bar Chart");

        outputArea.setPrefHeight(300);

        // Layout
        VBox layout = new VBox(10);
        layout.setPadding(new javafx.geometry.Insets(10));

        HBox personBox = new HBox(5, personInput, addPersonBtn);
        HBox transactionBox = new HBox(5, fromInput, toInput, amountInput, addTransactionBtn);
        HBox buttonsBox = new HBox(10, undoBtn, redoBtn, optimizeBtn, chartBtn);

        layout.getChildren().addAll(
                new Label("Cash Flow Minimizer"),
                personBox,
                transactionBox,
                buttonsBox,
                new Label("Output:"),
                outputArea
        );

        // Actions

        addPersonBtn.setOnAction(e -> {
            String name = personInput.getText().trim();
            if (!name.isEmpty() && !graph.containsKey(name)) {
                graph.put(name, new ArrayList<>());
                netAmount.put(name, 0);
                people.add(name);
                outputArea.appendText("Added person: " + name + "\n");
                personInput.clear();
            }
        });

        addTransactionBtn.setOnAction(e -> {
            try {
                String from = fromInput.getText().trim();
                String to = toInput.getText().trim();
                int amount = Integer.parseInt(amountInput.getText().trim());

                if (graph.containsKey(from) && graph.containsKey(to) && amount > 0) {
                    Edge e1 = new Edge(from, to, amount);
                    graph.get(from).add(e1);

                    netAmount.put(from, netAmount.get(from) - amount);
                    netAmount.put(to, netAmount.get(to) + amount);

                    undoStack.push(e1);
                    redoStack.clear();

                    outputArea.appendText(from + " → " + to + " : Rs." + amount + "\n");

                    fromInput.clear();
                    toInput.clear();
                    amountInput.clear();
                } else {
                    outputArea.appendText("Invalid transaction!\n");
                }
            } catch (Exception ex) {
                outputArea.appendText("Invalid amount!\n");
            }
        });

        undoBtn.setOnAction(e -> {
            if (!undoStack.isEmpty()) {
                Edge last = undoStack.pop();
                graph.get(last.from).removeIf(edge -> edge.to.equals(last.to) && edge.amount == last.amount);
                netAmount.put(last.from, netAmount.get(last.from) + last.amount);
                netAmount.put(last.to, netAmount.get(last.to) - last.amount);
                redoStack.push(last);
                outputArea.appendText("Undid transaction: " + last.from + " → " + last.to + " : Rs." + last.amount + "\n");
            } else {
                outputArea.appendText("Nothing to undo.\n");
            }
        });

        redoBtn.setOnAction(e -> {
            if (!redoStack.isEmpty()) {
                Edge last = redoStack.pop();
                graph.get(last.from).add(last);
                netAmount.put(last.from, netAmount.get(last.from) - last.amount);
                netAmount.put(last.to, netAmount.get(last.to) + last.amount);
                undoStack.push(last);
                outputArea.appendText("Redid transaction: " + last.from + " → " + last.to + " : Rs." + last.amount + "\n");
            } else {
                outputArea.appendText("Nothing to redo.\n");
            }
        });

        optimizeBtn.setOnAction(e -> {
            PriorityQueue<String> creditors = new PriorityQueue<>((a, b) -> netAmount.get(b) - netAmount.get(a));
            PriorityQueue<String> debtors = new PriorityQueue<>((a, b) -> netAmount.get(a) - netAmount.get(b));

            for (String person : people) {
                int amt = netAmount.get(person);
                if (amt > 0) creditors.offer(person);
                else if (amt < 0) debtors.offer(person);
            }

            outputArea.appendText("\n--- Optimized Transactions ---\n");

            while (!creditors.isEmpty() && !debtors.isEmpty()) {
                String creditor = creditors.poll();
                String debtor = debtors.poll();

                int settleAmount = Math.min(netAmount.get(creditor), -netAmount.get(debtor));
                netAmount.put(creditor, netAmount.get(creditor) - settleAmount);
                netAmount.put(debtor, netAmount.get(debtor) + settleAmount);

                outputArea.appendText(debtor + " pays Rs." + settleAmount + " to " + creditor + "\n");

                if (netAmount.get(creditor) > 0) creditors.offer(creditor);
                if (netAmount.get(debtor) < 0) debtors.offer(debtor);
            }

            outputArea.appendText("-------------------------------\n");
        });

        chartBtn.setOnAction(e -> {
            showBarChart();
        });

        Scene scene = new Scene(layout, 700, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showBarChart() {
        Stage stage = new Stage();
        stage.setTitle("Net Amounts - Bar Chart");

        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);

        barChart.setTitle("Net Balances per Person");
        xAxis.setLabel("Person");
        yAxis.setLabel("Amount (Rs)");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Net Amount");

        for (Map.Entry<String, Integer> entry : netAmount.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);

        Scene scene = new Scene(barChart, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
