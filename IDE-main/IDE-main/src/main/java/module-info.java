module com.example.ide {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.ide to javafx.fxml;
    exports com.example.ide;
}