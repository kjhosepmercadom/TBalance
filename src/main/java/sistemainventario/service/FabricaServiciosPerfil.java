package sistemainventario.service;

import sistemainventario.repository.CategoriaRepository;
import sistemainventario.repository.DirectorioPerfiles;
import sistemainventario.repository.EntradaRepository;
import sistemainventario.repository.ProductoRepository;
import sistemainventario.repository.ProveedorRepository;
import sistemainventario.repository.SalidaRepository;
import sistemainventario.repository.VentaRepository;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Construye los repositorios y servicios de un perfil.
 *
 * <p><b>GRASP Creator:</b> es la clase que posee los datos de inicialización
 * (la carpeta del perfil) y por eso es responsable de crear los
 * repositorios JSON y de inyectarlos en los gestores. Así ningún gestor hace
 * {@code new XxxRepository(...)}, lo que reduce el acoplamiento.</p>
 */
public class FabricaServiciosPerfil {

    private final DirectorioPerfiles directorios;

    public FabricaServiciosPerfil(DirectorioPerfiles directorios) {
        this.directorios = Objects.requireNonNull(directorios);
    }

    /**
     * @param perfilId ID del perfil
     * @return servicios listos para operar sobre los datos de ese perfil
     */
    public ServiciosPerfil crear(String perfilId) {
        Path carpeta = directorios.crear(perfilId);
        GestorInventario inventario = new GestorInventario(new ProductoRepository(carpeta));
        return new ServiciosPerfil(
                inventario,
                new GestorCategorias(new CategoriaRepository(carpeta)),
                new GestorProveedores(new ProveedorRepository(carpeta)),
                new GestorMovimientos(inventario, new EntradaRepository(carpeta), new SalidaRepository(carpeta)),
                new GestorVentas(inventario, new VentaRepository(carpeta)));
    }
}
