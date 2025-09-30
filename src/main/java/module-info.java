
module com.example.nasa_game {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.xml;
    requires com.fasterxml.jackson.databind;   // for DOM parsing TMX/TSX

    exports com.example.farming;
}
