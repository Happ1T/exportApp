package com.example.script3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class PeriodSelectorApp extends Application {

    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private TextArea logArea;
    private Label statusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Экспорт данных по периоду");

        // Установка иконки приложения
        primaryStage.getIcons().add(new Image("file:res\\icon.png"));

        // UI elements
        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();
        Button runButton = new Button("Запустить выгрузку");
        logArea = new TextArea();
        logArea.setEditable(false);
        statusLabel = new Label(); // Статусный индикатор

        runButton.setOnAction(e -> runScript());

        // Организация интерфейса
        VBox startBox = new VBox(5, new Label("Начало:"), startDatePicker);
        VBox endBox = new VBox(5, new Label("Конец:"), endDatePicker);
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(startBox, endBox, runButton, statusLabel, logArea);
        layout.setAlignment(Pos.TOP_CENTER);
        Scene scene = new Scene(layout, 400, 350);
        scene.getStylesheets().add("style.css"); // Добавление CSS файла для стилизации

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void runScript() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showAlert("Ошибка", "Пожалуйста, выберите обе даты.", Alert.AlertType.WARNING);
            return;
        }

        if (endDate.isBefore(startDate)) {
            showAlert("Ошибка", "Конечная дата должна быть позже начальной.", Alert.AlertType.WARNING);
            return;
        }

        // Форматирование дат в формате yyyyMMdd
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedStartDate = startDate.format(formatter);
        String formattedEndDate = endDate.format(formatter);

        try {

            statusLabel.setText("Идет обработка...");

            // Запись дат в скрипт
            FileWriter writer = new FileWriter("exportMC.ps1");
            writer.write("$ENV:NLS_LANG=\"AMERICAN_AMERICA.CL8MSWIN1251\"\n\n");
            writer.write("$ENV:PATH1=\"C:\\Scripts\\Export\"\n\n");
            writer.write("$ENV:CONN1=\"fbdata/micros@mcv8\"\n\n");
            writer.write("$ENV:SCRIPT=\"@D:\\MC\\Interfaces\\1C\\Period\\ALLRECEIVINGoneday.SQL\"\n\n");
            writer.write("$ENV:SUPPLIER=\"\"\"\"\n\n");

            writer.write("$ENV:DATE1=\"" + formattedStartDate + "\"\n");
            writer.write("$ENV:DATE2=\"" + formattedEndDate + "\"\n\n");
            writer.write("SQLPLUS $ENV:CONN1 $ENV:SCRIPT $ENV:DATE1 $ENV:DATE2 $ENV:PATH1 $ENV:SUPPLIER\n");
            writer.close();

            // Запуск таймера на 10 секунд
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // Обновление UI должно происходить на главном потоке JavaFX
                    Platform.runLater(() -> {
                        logArea.appendText("Выгрузка успешна, Файл называется: Ap" + formattedStartDate + ".\n");
                        statusLabel.setText("Обработка завершена."); // Смена статуса после завершения
                        showAlert("Успех", "Выгрузка завершена успешно!", Alert.AlertType.INFORMATION);
                    });
                }
            }, 10000); // 10000 миллисекунд = 10 секунд

        } catch (IOException ex) {
            logArea.appendText("Ошибка: " + ex.getMessage() + "\n");
            statusLabel.setText("Ошибка при обработке.");
            showAlert("Ошибка", "Произошла ошибка при записи в файл: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
