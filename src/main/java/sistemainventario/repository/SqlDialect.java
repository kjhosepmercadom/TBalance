package sistemainventario.repository;

import sistemainventario.model.DatabaseEngine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Lo que cambia entre PostgreSQL y Oracle.
 *
 * <p>Las consultas de los repositorios son SQL estándar y sirven para los dos
 * motores; aquí quedan solo las diferencias: dónde están los scripts, cómo se
 * pregunta si una tabla existe, cómo se separan las sentencias de un script
 * (los bloques PL/SQL de Oracle terminan en "/" y las funciones de PostgreSQL
 * van entre "$$") y cómo llega el mensaje de un disparador.</p>
 */
public interface SqlDialect {

    /** @return motor al que corresponde este dialecto */
    DatabaseEngine motor();

    /** @return carpeta de los scripts dentro de los recursos (por ejemplo {@code sql/oracle}) */
    String carpetaScripts();

    /** @return nombre del driver JDBC, para los mensajes de error */
    String nombreDriver();

    /**
     * @param conexion conexión abierta
     * @param tabla    nombre de la tabla
     * @return {@code true} si la tabla existe en el esquema del usuario
     * @throws SQLException si falla la consulta al diccionario de datos
     */
    boolean tablaExiste(Connection conexion, String tabla) throws SQLException;

    /**
     * Dice si una tabla tiene una columna. Por defecto responde {@code true} (no se revisa):
     * solo PostgreSQL lo usa, para aplicar el cambio de los tipos de negocio según lo que
     * tiene la base y no solo según el número de versión.
     *
     * @param conexion conexión abierta
     * @param tabla    nombre de la tabla
     * @param columna  nombre de la columna
     * @return {@code true} si la tabla tiene esa columna
     * @throws SQLException si la base no responde
     */
    default boolean columnaExiste(Connection conexion, String tabla, String columna) throws SQLException {
        return true;
    }

    /**
     * @param script contenido de un archivo .sql
     * @return sentencias listas para enviar una por una por JDBC
     */
    List<String> separarSentencias(String script);

    /**
     * @param error error devuelto por la base
     * @return el mensaje para el usuario si el error lo lanzó un disparador de TBalance
     */
    Optional<String> mensajeDeDisparador(SQLException error);

    /**
     * @param motor PostgreSQL u Oracle
     * @return el dialecto de ese motor
     * @throws IllegalArgumentException si el motor no es relacional
     */
    static SqlDialect para(DatabaseEngine motor) {
        return switch (motor) {
            case POSTGRESQL -> new PostgresDialect();
            case ORACLE -> new OracleDialect();
            case JSON -> throw new IllegalArgumentException("El modo JSON no usa SQL");
        };
    }
}
