package com.alemandan.crm.controller;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Categoria;
import com.alemandan.crm.service.ProductoService;
import com.alemandan.crm.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/productos")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoController {

    private static final Logger logger = LoggerFactory.getLogger(ProductoController.class);

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    // Ruta base donde se guardan las subidas (configurable en application.properties)
    @Value("${uploads.path:uploads}")
    private String uploadsPath;

    // Listar todos los productos (activos e inactivos) y pasar categorías
    @GetMapping
    public String listarProductos(Model model) {
        List<Producto> productos = productoService.listarProductos();
        List<Categoria> categorias = categoriaRepository.findAll();
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categorias);
        return "productos/listaprod";
    }

    // Mostrar formulario para agregar producto
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoProducto(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "productos/nuevoprod";
    }

    // Guardar nuevo producto (con manejo de categoría a partir de descripcion y subida de imagen)
    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto,
                                  @RequestParam(value = "imagen", required = false) MultipartFile imagen) {

        logger.info("Entrando a guardarProducto. nombre='{}' descripcion='{}' unidadMedida='{}' iva='{}'",
                producto.getNombre(), producto.getDescripcion(), producto.getUnidadMedida(), producto.getIva());

        // Asegurar IVA no nulo
        if (producto.getIva() == null) producto.setIva(BigDecimal.ZERO);

        // 1) Manejo de categoría (usamos descripcion como nombre de categoria)
        String categoriaNombre = producto.getDescripcion() != null ? producto.getDescripcion().trim() : null;
        if (categoriaNombre != null && !categoriaNombre.isEmpty()) {
            Optional<Categoria> catOpt = categoriaRepository.findByNombreIgnoreCase(categoriaNombre);
            Categoria categoria = catOpt.orElseGet(() -> {
                Categoria c = new Categoria();
                c.setNombre(categoriaNombre);
                return categoriaRepository.save(c);
            });
            producto.setCategoria(categoria);
            producto.setDescripcion(categoriaNombre);
        } else {
            producto.setCategoria(null);
        }

        // 2) Guardar producto para obtener ID
        Producto saved = productoService.saveProducto(producto);
        logger.info("Producto guardado temporal con id={}", saved.getId());

        // 3) Depuración del MultipartFile recibido
        if (imagen == null) {
            logger.warn("MultipartFile 'imagen' es NULL en la petición.");
        } else {
            logger.info("MultipartFile 'imagen' recibido: originalName='{}', size={}, empty={}",
                    imagen.getOriginalFilename(), imagen.getSize(), imagen.isEmpty());
        }

        // 4) Guardar fichero si viene (USANDO Files.copy para evitar Part.write/contendor)
        try {
            if (imagen != null && !imagen.isEmpty() && imagen.getSize() > 0) {
                String original = imagen.getOriginalFilename();
                String safeName = (original != null && !original.trim().isEmpty())
                        ? original.replaceAll("[\\\\/:*?\"<>|]+", "_")
                        : java.util.UUID.randomUUID().toString() + ".img";

                // Usar uploadsPath configurado y convertir a ruta ABSOLUTA
                Path uploadDir = Paths.get(uploadsPath, "products", String.valueOf(saved.getId())).toAbsolutePath();
                Files.createDirectories(uploadDir);

                Path filePath = uploadDir.resolve(safeName);

                // Copiar directamente desde el InputStream del multipart al archivo destino.
                try (InputStream is = imagen.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }

                logger.info("Imagen guardada en disco (absoluta): {}", filePath.toAbsolutePath().toString());

                // Guardamos la ruta pública relativa para Thymeleaf
                String publicPath = "/uploads/products/" + saved.getId() + "/" + safeName;
                saved.setImagePath(publicPath);
                productoService.saveProducto(saved);
                logger.info("Producto {} actualizado con imagePath={}", saved.getId(), saved.getImagePath());
            } else {
                logger.info("No se procesó imagen (null o vacía).");
            }
        } catch (Exception e) {
            logger.error("Error guardando imagen del producto id=" + saved.getId(), e);
        }

        return "redirect:/productos";
    }

    // Mostrar formulario para editar producto
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable Long id, Model model) {
        Optional<Producto> producto = productoService.getProductoById(id);
        if (producto.isPresent()) {
            model.addAttribute("producto", producto.get());
            model.addAttribute("categorias", categoriaRepository.findAll());
            return "productos/editarprod";
        } else {
            return "redirect:/productos";
        }
    }

    // Actualizar producto (acepta imagen nueva opcional y checkbox para eliminar la actual)
    @PostMapping("/actualizar")
    public String actualizarProducto(@ModelAttribute Producto producto,
                                     @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                                     @RequestParam(value = "eliminarImagen", required = false) Boolean eliminarImagen) {

        logger.info("Entrando a actualizarProducto. id='{}' nombre='{}' iva='{}'", producto.getId(), producto.getNombre(), producto.getIva());

        // Asegurar IVA no nulo
        if (producto.getIva() == null) producto.setIva(BigDecimal.ZERO);

        String categoriaNombre = producto.getDescripcion() != null ? producto.getDescripcion().trim() : null;
        if (categoriaNombre != null && !categoriaNombre.isEmpty()) {
            Optional<Categoria> catOpt = categoriaRepository.findByNombreIgnoreCase(categoriaNombre);
            Categoria categoria = catOpt.orElseGet(() -> {
                Categoria c = new Categoria();
                c.setNombre(categoriaNombre);
                return categoriaRepository.save(c);
            });
            producto.setCategoria(categoria);
            producto.setDescripcion(categoriaNombre);
        } else {
            producto.setCategoria(null);
        }

        Producto saved = productoService.saveProducto(producto);

        // Si se pidió eliminar la imagen actual, intentamos borrarla y limpiar el path
        if (Boolean.TRUE.equals(eliminarImagen) && saved.getImagePath() != null) {
            try {
                // imagePath en formato "/uploads/products/{id}/{filename}"
                String imgPath = saved.getImagePath();
                if (imgPath.startsWith("/")) imgPath = imgPath.substring(1); // quitar slash inicial
                Path fsPath = Paths.get(imgPath); // relativa a la raíz del servidor estática, no absoluta
                // Intentar borrar en uploadsPath si existe
                Path candidate = Paths.get(uploadsPath).resolve(fsPath).toAbsolutePath();
                if (Files.exists(candidate)) {
                    Files.deleteIfExists(candidate);
                    logger.info("Imagen borrada de disco: {}", candidate);
                } else {
                    logger.warn("No se encontró archivo para eliminar en: {}", candidate);
                }
            } catch (Exception e) {
                logger.error("Error al eliminar imagen antigua del producto " + saved.getId(), e);
            }
            saved.setImagePath(null);
            productoService.saveProducto(saved);
            logger.info("Producto {} imagePath limpiado por petición de eliminación.", saved.getId());
        }

        try {
            if (imagen != null && !imagen.isEmpty() && imagen.getSize() > 0) {
                String original = imagen.getOriginalFilename();
                String safeName = (original != null && !original.trim().isEmpty())
                        ? original.replaceAll("[\\\\/:*?\"<>|]+", "_")
                        : java.util.UUID.randomUUID().toString() + ".img";

                Path uploadDir = Paths.get(uploadsPath, "products", String.valueOf(saved.getId())).toAbsolutePath();
                Files.createDirectories(uploadDir);

                Path filePath = uploadDir.resolve(safeName);
                try (InputStream is = imagen.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }

                logger.info("Imagen actualizada en disco (absoluta): {}", filePath.toAbsolutePath().toString());

                String publicPath = "/uploads/products/" + saved.getId() + "/" + safeName;
                saved.setImagePath(publicPath);
                productoService.saveProducto(saved);
                logger.info("Producto {} actualizado con nueva imagePath={}", saved.getId(), saved.getImagePath());
            } else {
                logger.info("No se procesó imagen en actualización (null o vacía).");
            }
        } catch (Exception e) {
            logger.error("Error actualizando imagen del producto id=" + saved.getId(), e);
        }
        return "redirect:/productos";
    }

    // Inactivar producto
    @GetMapping("/inactivar/{id}")
    public String inactivarProducto(@PathVariable Long id) {
        productoService.inactivarProducto(id);
        return "redirect:/productos";
    }

    // Activar producto
    @GetMapping("/activar/{id}")
    public String activarProducto(@PathVariable Long id) {
        productoService.activarProducto(id);
        return "redirect:/productos";
    }

    // Endpoint para búsqueda AJAX (empleado/caja)
    @GetMapping("/api/productos/buscar")
    @ResponseBody
    public List<Producto> buscarProductos(@RequestParam(required = false) String term) {
        if (term == null || term.trim().isEmpty()) {
            return productoService.getAllProductos();
        }
        return productoService.buscarPorNombre(term.trim());
    }
}