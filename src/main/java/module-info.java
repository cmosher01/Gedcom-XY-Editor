import nu.mine.mosher.gedcom.xy.GenXyEditor;

module nu.mine.mosher.gedcom.xy {
    exports nu.mine.mosher.gedcom.xy;
    exports nu.mine.mosher.gedcom.xy.util;
    provides ch.qos.logback.classic.spi.Configurator with GenXyEditor.LogConfig;
    requires gedcom.lib;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires org.apache.commons.logging;
    requires log4j;
    requires jul.to.slf4j;
    requires java.logging;
    requires log.files;
    requires javafx.controls;
    requires javafx.swing;
    requires java.desktop;
    requires java.prefs;
    requires java.xml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
}
